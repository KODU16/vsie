package com.kodu16.vsie.content.custom_turret;

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

/** Restricts all custom-turret file access to AeroIE/Custom_turret under the active game directory. */
public final class CustomTurretStorage {
    public static final String DEFINITION_EXTENSION = ".turret.json";
    private static final int MAX_SCAN_DEPTH = 8;
    private static final long MAX_DEFINITION_BYTES = CustomTurretDefinition.MAX_JSON_LENGTH;

    private CustomTurretStorage() {
    }

    public static Path root() {
        return FMLPaths.GAMEDIR.get().resolve("AeroIE").resolve("Custom_turret").toAbsolutePath().normalize();
    }

    public static Path ensureRoot() throws IOException {
        Path root = root();
        Files.createDirectories(root);
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
        Path root = root();
        Path resolved = root.resolve(normalized).toAbsolutePath().normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Custom turret path escapes its root");
        }
        return resolved;
    }

    public static List<String> scanResources(String extension) {
        String lowerExtension = extension.toLowerCase(Locale.ROOT);
        try {
            Path root = ensureRoot();
            try (Stream<Path> files = Files.walk(root, MAX_SCAN_DEPTH)) {
                return files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(lowerExtension))
                        .map(root::relativize)
                        .map(Path::toString)
                        .map(path -> path.replace('\\', '/'))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
            }
        } catch (IOException exception) {
            return List.of();
        }
    }

    /** Lists only complete folder packages whose OBJ, MTL and unique PNG use the folder name. */
    public static List<CustomTurretAssetPackage> scanAssetPackages() {
        try {
            Path root = ensureRoot();
            try (Stream<Path> directories = Files.list(root)) {
                return directories.filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .map(CustomTurretStorage::packageOrNull)
                        .filter(java.util.Objects::nonNull)
                        .filter(assetPackage -> Files.isRegularFile(resolveResource(assetPackage.modelPath()))
                                && Files.isRegularFile(resolveResource(assetPackage.materialPath()))
                                && Files.isRegularFile(resolveResource(assetPackage.texturePath())))
                        .sorted(Comparator.comparing(CustomTurretAssetPackage::name, String.CASE_INSENSITIVE_ORDER))
                        .toList();
            }
        } catch (IOException exception) {
            return List.of();
        }
    }

    public static List<CustomTurretDefinition> loadAllDefinitions() {
        try {
            Path root = ensureRoot();
            try (Stream<Path> files = Files.walk(root, MAX_SCAN_DEPTH)) {
                return files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(DEFINITION_EXTENSION))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                        .map(CustomTurretStorage::loadDefinitionOrNull)
                        .filter(java.util.Objects::nonNull)
                        .toList();
            }
        } catch (IOException exception) {
            return List.of();
        }
    }

    /** Lists valid definition files directly inside one root-confined browser folder. */
    public static List<DefinitionFile> loadDefinitionFiles(String folderPath) {
        try {
            Path root = ensureRoot();
            String normalizedFolder = normalizeRelativePath(folderPath);
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

    public record DefinitionFile(String fileName, CustomTurretDefinition definition) {
    }

    public static void saveDefinition(CustomTurretDefinition definition) throws IOException {
        saveDefinition(definition, "", definition.id);
    }

    public static void saveDefinition(CustomTurretDefinition definition, String folderPath, String fileName) throws IOException {
        String json = definition.toJson();
        Path root = ensureRoot();
        String normalizedFolder = normalizeRelativePath(folderPath);
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
        Path temporary = Files.createTempFile(directory, normalizedName + "_", ".tmp");
        Files.writeString(temporary, json, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveUnsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
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
            Path root = ensureRoot();
            try (Stream<Path> paths = Files.walk(root, MAX_SCAN_DEPTH)) {
                List<String> directories = new java.util.ArrayList<>();
                directories.add("");
                directories.addAll(paths.filter(Files::isDirectory)
                        .filter(path -> !path.equals(root))
                        .map(root::relativize)
                        .map(Path::toString)
                        .map(path -> path.replace('\\', '/'))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList());
                return List.copyOf(directories);
            }
        } catch (IOException exception) {
            return List.of("");
        }
    }

    private static CustomTurretDefinition loadDefinitionOrNull(Path path) {
        try {
            if (Files.size(path) > MAX_DEFINITION_BYTES) {
                return null;
            }
            return CustomTurretDefinition.fromJson(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static CustomTurretAssetPackage packageOrNull(String name) {
        try {
            return CustomTurretAssetPackage.named(name);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
