package com.kodu16.vsie.content.custom_turret;

import java.util.Optional;
import java.util.regex.Pattern;

/** Defines one folder-scoped OBJ/MTL/PNG trio with matching names and a unique texture. */
public record CustomTurretAssetPackage(String name, String modelPath, String materialPath, String texturePath) {
    private static final Pattern SAFE_NAME = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

    public static CustomTurretAssetPackage named(String name) {
        String normalized = name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
        if (!SAFE_NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid custom turret asset package name");
        }
        String prefix = normalized + '/' + normalized;
        return new CustomTurretAssetPackage(normalized, prefix + ".obj", prefix + ".mtl", prefix + ".png");
    }

    public static Optional<CustomTurretAssetPackage> fromModelPath(String modelPath) {
        if (modelPath == null) {
            return Optional.empty();
        }
        String normalized = modelPath.trim().replace('\\', '/');
        int slash = normalized.indexOf('/');
        if (slash <= 0 || slash != normalized.lastIndexOf('/')) {
            return Optional.empty();
        }
        String folder = normalized.substring(0, slash);
        try {
            CustomTurretAssetPackage candidate = named(folder);
            return candidate.modelPath.equals(normalized) ? Optional.of(candidate) : Optional.empty();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
