package com.kodu16.vsie.content.aeroie_custom;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Maps user-authored AeroIE/fx Photon files to a stable client-only ResourceLocation namespace. */
public final class CustomFxResources {
    public static final String NAMESPACE = "aeroie_custom";
    private static final int MAX_SCAN_DEPTH = 8;
    private static final long MAX_FX_BYTES = 4L * 1024L * 1024L;

    private CustomFxResources() {
    }

    public static Path root() {
        return FMLPaths.GAMEDIR.get().resolve("AeroIE").resolve("fx").toAbsolutePath().normalize();
    }

    public static Path ensureRoot() throws IOException {
        Path root = root();
        Files.createDirectories(root);
        return root;
    }

    public static String normalizeRelativeFxPath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        if (normalized.endsWith(".fx")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        if (normalized.startsWith("/") || normalized.contains(":") || normalized.contains("\u0000")) {
            throw new IllegalArgumentException("Absolute custom FX paths are not allowed");
        }
        Path relative = Path.of(normalized).normalize();
        String result = relative.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (result.equals("..") || result.startsWith("../")) {
            throw new IllegalArgumentException("Custom FX path escapes its root");
        }
        if (!result.isEmpty() && !result.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Custom FX path must use lowercase letters, digits, _, -, /");
        }
        return result.equals(".") ? "" : result;
    }

    public static ResourceLocation toResourceLocation(String relativeFxPath) {
        String normalized = normalizeRelativeFxPath(relativeFxPath);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Custom FX path is empty");
        }
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, normalized);
    }

    public static InputStream open(ResourceLocation location) throws IOException {
        if (!NAMESPACE.equals(location.getNamespace())) {
            throw new IOException("Not an AeroIE custom FX location");
        }
        Path path = resolve(location.getPath());
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_FX_BYTES) {
            throw new IOException("Missing or oversized custom FX file");
        }
        return Files.newInputStream(path);
    }

    public static Path resolve(String relativeFxPath) {
        String normalized = normalizeRelativeFxPath(relativeFxPath);
        Path root = root();
        Path resolved = root.resolve(normalized + ".fx").toAbsolutePath().normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Custom FX path escapes its root");
        }
        return resolved;
    }

    public static List<String> scanFxFiles() {
        try {
            Path root = ensureRoot();
            try (Stream<Path> files = Files.walk(root, MAX_SCAN_DEPTH)) {
                return files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".fx"))
                        .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                        .map(root::relativize)
                        .map(Path::toString)
                        .map(path -> normalizeRelativeFxPath(path.replace('\\', '/')))
                        .toList();
            }
        } catch (IOException | IllegalArgumentException exception) {
            return List.of();
        }
    }
}
