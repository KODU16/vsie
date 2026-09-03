package com.kodu16.vsie.content.aeroie_custom;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Restricts custom-device file access to AeroIE custom roots under the active game directory. */
public final class CustomDeviceStorage {
    public static final String DEFINITION_EXTENSION = ".turret.json";
    private static final int MAX_SCAN_DEPTH = 8;
    private static final long MAX_DEFINITION_BYTES = CustomDeviceDefinition.MAX_JSON_LENGTH;

    private CustomDeviceStorage() {
    }

    public static Path root() {
        return FMLPaths.GAMEDIR.get().resolve("AeroIE_custom").toAbsolutePath().normalize();
    }

    public static Path turretRoot() {
        return root().resolve("turret");
    }

    public static Path weaponRoot() {
        return root().resolve("weapon");
    }

    public static Path thrusterRoot() {
        return root().resolve("thruster");
    }

    public static Path decorationRoot() {
        return root().resolve("decoration");
    }

    public static Path componentsRoot() {
        return root().resolve("components");
    }

    public static Path rootForDeviceType(String deviceType) {
        return switch (deviceType) {
            case "weapon" -> weaponRoot();
            case "thruster" -> thrusterRoot();
            case "decoration" -> decorationRoot();
            default -> turretRoot();
        };
    }

    public static Path ensureRoot() throws IOException {
        Path root = root();
        Files.createDirectories(root);
        return root;
    }

    public static Path ensureRootForDeviceType(String deviceType) throws IOException {
        Path root = rootForDeviceType(deviceType);
        Files.createDirectories(root);
        copyLegacyFilesIfMissing(deviceType, root);
        return root;
    }

    public static String normalizeRelativePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":") || normalized.contains("\u0000")) {
            throw new IllegalArgumentException("Absolute custom turret paths are not allowed");
        }
        Path relative = Path.of(normalized).normalize();
        String result = relative.toString().replace('\\', '/');
        if (result.equals("..") || result.startsWith("../")) {
            throw new IllegalArgumentException("Custom turret path escapes its root");
        }
        return result.equals(".") ? "" : result;
    }

    public static Path resolveResource(String relativePath) {
        String normalized = normalizeRelativePath(relativePath);
        for (String deviceType : CustomDeviceDefinition.DEVICE_TYPES) {
            Path resolved = resolveResource(deviceType, normalized);
            if (Files.exists(resolved)) {
                return resolved;
            }
        }
        return turretRoot().resolve(normalized).toAbsolutePath().normalize();
    }

    public static Path resolveResource(String deviceType, String relativePath) {
        String normalized = normalizeRelativePath(relativePath);
        Path root = rootForDeviceType(deviceType);
        Path resolved = root.resolve(normalized).toAbsolutePath().normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Custom device path escapes its root");
        }
        if (!Files.exists(resolved)) {
            Path legacyRoot = legacyRootForDeviceType(deviceType);
            Path legacyResolved = legacyRoot.resolve(normalized).toAbsolutePath().normalize();
            if (legacyResolved.startsWith(legacyRoot) && Files.exists(legacyResolved)) {
                return legacyResolved;
            }
        }
        return resolved;
    }

    public static List<String> scanResources(String extension) {
        String lowerExtension = extension.toLowerCase(Locale.ROOT);
        try {
            ensureRootForDeviceType("turret");
            ensureRootForDeviceType("weapon");
            ensureRootForDeviceType("thruster");
            ensureRootForDeviceType("decoration");
            List<String> resources = new java.util.ArrayList<>();
            resources.addAll(scanResources(turretRoot(), "", lowerExtension));
            resources.addAll(scanResources(weaponRoot(), "weapon:", lowerExtension));
            resources.addAll(scanResources(thrusterRoot(), "thruster:", lowerExtension));
            resources.addAll(scanResources(decorationRoot(), "decoration:", lowerExtension));
            return resources.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    /** Lists only complete folder packages whose OBJ, MTL and unique PNG use the folder name. */
    public static List<CustomDeviceAssetPackage> scanAssetPackages() {
        return scanAssetPackages("turret");
    }

    public static List<CustomDeviceAssetPackage> scanAssetPackages(String deviceType) {
        try {
            Path root = ensureRootForDeviceType(deviceType);
            try (Stream<Path> directories = Files.list(root)) {
                return directories.filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .map(CustomDeviceStorage::packageOrNull)
                        .filter(java.util.Objects::nonNull)
                        .filter(assetPackage -> Files.isRegularFile(resolveResource(deviceType, assetPackage.modelPath()))
                                && Files.isRegularFile(resolveResource(deviceType, assetPackage.materialPath()))
                                && Files.isRegularFile(resolveResource(deviceType, assetPackage.texturePath())))
                        .sorted(Comparator.comparing(CustomDeviceAssetPackage::name, String.CASE_INSENSITIVE_ORDER))
                        .toList();
            }
        } catch (IOException exception) {
            return List.of();
        }
    }

    public static List<CustomDeviceDefinition> loadAllDefinitions() {
        try {
            ensureRootForDeviceType("turret");
            ensureRootForDeviceType("weapon");
            ensureRootForDeviceType("thruster");
            ensureRootForDeviceType("decoration");
            List<CustomDeviceDefinition> definitions = new java.util.ArrayList<>();
            definitions.addAll(loadDefinitionsFromRoot(turretRoot()));
            definitions.addAll(loadDefinitionsFromRoot(weaponRoot()));
            definitions.addAll(loadDefinitionsFromRoot(thrusterRoot()));
            definitions.addAll(loadDefinitionsFromRoot(decorationRoot()));
            return definitions.stream()
                    .sorted(Comparator.comparing(definition -> definition.id, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    /** Lists valid definition files directly inside one root-confined browser folder. */
    public static List<DefinitionFile> loadDefinitionFiles(String folderPath) {
        try {
            Path root = rootForBrowserFolder(folderPath);
            Files.createDirectories(root);
            String normalizedFolder = normalizeRelativePath(stripFolderRootPrefix(folderPath));
            Path directory = root.resolve(normalizedFolder).normalize();
            if (!directory.startsWith(root) || !Files.isDirectory(directory)) {
                return List.of();
            }
            try (Stream<Path> files = Files.list(directory)) {
                return files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
                                .endsWith(DEFINITION_EXTENSION))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString(),
                                String.CASE_INSENSITIVE_ORDER))
                        .map(path -> new DefinitionFile(path.getFileName().toString(), loadDefinitionOrNull(path)))
                        .filter(entry -> entry.definition() != null)
                        .toList();
            }
        } catch (IOException | IllegalArgumentException exception) {
            return List.of();
        }
    }

    public record DefinitionFile(String fileName, CustomDeviceDefinition definition) {
    }

    public static void saveDefinition(CustomDeviceDefinition definition) throws IOException {
        saveDefinition(definition, "", definition.id);
    }

    public static void saveDefinition(CustomDeviceDefinition definition, String folderPath, String fileName) throws IOException {
        Path root = ensureRootForDeviceType(definition.deviceType);
        String normalizedFolder = normalizeRelativePath(stripFolderRootPrefix(folderPath));
        if (normalizedFolder.isBlank()) {
            normalizedFolder = definitionId(fileName);
        }
        String normalizedName = definitionId(fileName);
        Path directory = root.resolve(normalizedFolder).normalize();
        if (!directory.startsWith(root)) {
            throw new IOException("Invalid custom turret save folder");
        }
        Files.createDirectories(directory);
        Path target = directory.resolve(normalizedName + DEFINITION_EXTENSION).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Invalid custom turret definition id");
        }
        definition.geckoModel = normalizeRelativePath(
                (normalizedFolder.isBlank() ? "" : normalizedFolder + "/") + normalizedName + ".geo.json");
        String jsonWithGeneratedModel = definition.toJson();
        Path geoTarget = directory.resolve(normalizedName + ".geo.json").normalize();
        if (!geoTarget.startsWith(root)) {
            throw new IOException("Invalid custom GeckoLib model path");
        }
        Path temporary = Files.createTempFile(directory, normalizedName + "_", ".tmp");
        Path geoTemporary = Files.createTempFile(directory, normalizedName + "_", ".geo.tmp");
        Files.writeString(temporary, jsonWithGeneratedModel, StandardCharsets.UTF_8);
        Files.writeString(geoTemporary, CustomDeviceGeckoModelExporter.toGeoJson(definition), StandardCharsets.UTF_8);
        try {
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(geoTemporary, geoTarget, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(geoTemporary, geoTarget, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
            Files.deleteIfExists(geoTemporary);
        }
    }

    public static String definitionId(String fileName) {
        String normalized = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(DEFINITION_EXTENSION)) {
            normalized = normalized.substring(0, normalized.length() - DEFINITION_EXTENSION.length());
        }
        if (!normalized.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException("Definition file name must use 1-64 lowercase letters, digits, _ or -");
        }
        return normalized;
    }

    /** Supplies root-relative directories for the in-game Photon-style folder chooser. */
    public static List<String> scanDirectories() {
        try {
            ensureRootForDeviceType("turret");
            ensureRootForDeviceType("weapon");
            ensureRootForDeviceType("thruster");
            ensureRootForDeviceType("decoration");
            List<String> directories = new java.util.ArrayList<>();
            directories.add("");
            directories.add("weapon:");
            directories.add("thruster:");
            directories.add("decoration:");
            directories.addAll(scanDirectories(turretRoot(), ""));
            directories.addAll(scanDirectories(weaponRoot(), "weapon:"));
            directories.addAll(scanDirectories(thrusterRoot(), "thruster:"));
            directories.addAll(scanDirectories(decorationRoot(), "decoration:"));
            return directories.stream().distinct().toList();
        } catch (IOException exception) {
            return List.of("");
        }
    }

    /** Lists root-relative subfolders inside the AeroIE Components folder used by the region OBJ export. */
    public static List<String> scanComponentsDirectories() {
        try {
            Path root = componentsRoot();
            Files.createDirectories(root);
            return scanDirectories(root, "");
        } catch (IOException exception) {
            return List.of("");
        }
    }

    private static List<String> scanDirectories(Path root, String prefix) throws IOException {
        try (Stream<Path> paths = Files.walk(root, MAX_SCAN_DEPTH)) {
            List<String> directories = new java.util.ArrayList<>();
            directories.addAll(paths.filter(Files::isDirectory)
                    .filter(path -> !path.equals(root))
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .map(path -> prefix + path)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList());
            return List.copyOf(directories);
        }
    }

    private static List<String> scanResources(Path root, String prefix, String lowerExtension) throws IOException {
        try (Stream<Path> files = Files.walk(root, MAX_SCAN_DEPTH)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(lowerExtension))
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .map(path -> prefix + path)
                    .toList();
        }
    }

    private static List<CustomDeviceDefinition> loadDefinitionsFromRoot(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root, MAX_SCAN_DEPTH)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(DEFINITION_EXTENSION))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .map(CustomDeviceStorage::loadDefinitionOrNull)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
    }

    private static Path rootForBrowserFolder(String folderPath) {
        if (folderPath != null && folderPath.startsWith("weapon:")) {
            return weaponRoot();
        }
        if (folderPath != null && folderPath.startsWith("thruster:")) {
            return thrusterRoot();
        }
        if (folderPath != null && folderPath.startsWith("decoration:")) {
            return decorationRoot();
        }
        return turretRoot();
    }

    private static String stripFolderRootPrefix(String folderPath) {
        if (folderPath == null) {
            return null;
        }
        if (folderPath.startsWith("weapon:")) {
            return folderPath.substring("weapon:".length());
        }
        if (folderPath.startsWith("thruster:")) {
            return folderPath.substring("thruster:".length());
        }
        if (folderPath.startsWith("decoration:")) {
            return folderPath.substring("decoration:".length());
        }
        return folderPath;
    }

    private static Path legacyRootForDeviceType(String deviceType) {
        Path legacyBase = FMLPaths.GAMEDIR.get().resolve("AeroIE").toAbsolutePath().normalize();
        return switch (deviceType) {
            case "weapon" -> legacyBase.resolve("Custom_weapon");
            case "thruster" -> legacyBase.resolve("Custom_thruster");
            case "decoration" -> legacyBase.resolve("Custom_decoration");
            default -> legacyBase.resolve("Custom_turret");
        };
    }

    private static void copyLegacyFilesIfMissing(String deviceType, Path targetRoot) throws IOException {
        Path legacyRoot = legacyRootForDeviceType(deviceType);
        if (!Files.isDirectory(legacyRoot)) {
            return;
        }
        // Function: copy old per-type folders into AeroIE_custom without deleting or overwriting user assets.
        try (Stream<Path> paths = Files.walk(legacyRoot, MAX_SCAN_DEPTH)) {
            for (Path source : paths.toList()) {
                Path target = targetRoot.resolve(legacyRoot.relativize(source)).normalize();
                if (!target.startsWith(targetRoot)) {
                    continue;
                }
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else if (!Files.exists(target)) {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target);
                }
            }
        }
    }

    private static CustomDeviceDefinition loadDefinitionOrNull(Path path) {
        try {
            if (Files.size(path) > MAX_DEFINITION_BYTES) {
                return null;
            }
            return CustomDeviceDefinition.fromJson(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static CustomDeviceAssetPackage packageOrNull(String name) {
        try {
            return CustomDeviceAssetPackage.named(name);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
