package com.kodu16.vsie.content.aeroie_custom;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.json.typeadapter.BakedAnimationsAdapter;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Maps player-authored GeckoLib animation files under AeroIE_custom/turret into GeckoLib's cache. */
public final class CustomTurretAnimationResources {
    public static final String NAMESPACE = "aeroie_custom";
    private static final String RESOURCE_PREFIX = "animations/custom_turret/";
    private static final int MAX_SCAN_DEPTH = 8;
    private static final long MAX_ANIMATION_BYTES = 2L * 1024L * 1024L;

    private CustomTurretAnimationResources() {
    }

    public static String normalizeRelativeAnimationPath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        if (normalized.endsWith(".json") && !normalized.endsWith(".animation.json")) {
            throw new IllegalArgumentException("Custom turret animations must use .animation.json");
        }
        if (!normalized.endsWith(".animation.json")) {
            normalized = normalized + ".animation.json";
        }
        String relative = CustomDeviceStorage.normalizeRelativePath(normalized).toLowerCase(Locale.ROOT);
        if (!relative.matches("[a-z0-9_./-]+\\.animation\\.json")) {
            throw new IllegalArgumentException("Custom turret animation path must use lowercase letters, digits, _, -, /");
        }
        return relative;
    }

    public static ResourceLocation toResourceLocation(String relativeAnimationPath) {
        String normalized = normalizeRelativeAnimationPath(relativeAnimationPath);
        if (normalized.isBlank()) {
            return ResourceLocation.fromNamespaceAndPath("vsie", "animations/block/custom_turret.animation.json");
        }
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, RESOURCE_PREFIX + normalized);
    }

    public static boolean ensureLoaded(String relativeAnimationPath) {
        String normalized = normalizeRelativeAnimationPath(relativeAnimationPath);
        if (normalized.isBlank()) {
            return false;
        }
        ResourceLocation location = toResourceLocation(normalized);
        if (GeckoLibCache.getBakedAnimations().containsKey(location)) {
            return true;
        }
        try {
            BakedAnimations bakedAnimations = readAnimations(resolve(normalized));
            cacheAnimation(location, bakedAnimations);
            return true;
        } catch (Exception ignored) {
            // Invalid user files fail closed; the renderer falls back to the empty bundled animation file.
            return false;
        }
    }

    public static String defaultAnimationName(String relativeAnimationPath) {
        String normalized = normalizeRelativeAnimationPath(relativeAnimationPath);
        if (normalized.isBlank()) {
            return "";
        }
        try {
            JsonObject root = KeyFramesAdapter.GEO_GSON.fromJson(
                    Files.readString(resolve(normalized), StandardCharsets.UTF_8), JsonObject.class);
            JsonObject animations = GsonHelper.getAsJsonObject(root, "animations", new JsonObject());
            return animations.entrySet().stream()
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("fire");
        } catch (Exception ignored) {
            return "fire";
        }
    }

    public static List<String> scanAnimationFiles() {
        try {
            Path root = CustomDeviceStorage.ensureRootForDeviceType("turret");
            try (Stream<Path> files = Files.walk(root, MAX_SCAN_DEPTH)) {
                return files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
                                .endsWith(".animation.json"))
                        .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                        .map(root::relativize)
                        .map(Path::toString)
                        .map(path -> normalizeRelativeAnimationPath(path.replace('\\', '/')))
                        .toList();
            }
        } catch (IOException | IllegalArgumentException exception) {
            return List.of();
        }
    }

    private static Path resolve(String normalizedPath) throws IOException {
        Path root = CustomDeviceStorage.ensureRootForDeviceType("turret");
        Path resolved = root.resolve(normalizedPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(root) || !Files.isRegularFile(resolved) || Files.size(resolved) > MAX_ANIMATION_BYTES) {
            throw new IOException("Missing or oversized custom turret animation file");
        }
        return resolved;
    }

    private static BakedAnimations readAnimations(Path path) throws IOException {
        JsonObject root = KeyFramesAdapter.GEO_GSON.fromJson(
                Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
        var previousCompressionCache = BakedAnimationsAdapter.COMPRESSION_CACHE;
        if (previousCompressionCache == null) {
            BakedAnimationsAdapter.COMPRESSION_CACHE = new ConcurrentHashMap<>();
        }
        try {
            return KeyFramesAdapter.GEO_GSON.fromJson(
                    GsonHelper.getAsJsonObject(root, "animations"), BakedAnimations.class);
        } finally {
            if (previousCompressionCache == null) {
                BakedAnimationsAdapter.COMPRESSION_CACHE = null;
            }
        }
    }

    private static void cacheAnimation(ResourceLocation location, BakedAnimations bakedAnimations) throws Exception {
        try {
            GeckoLibCache.getBakedAnimations().put(location, bakedAnimations);
        } catch (UnsupportedOperationException unsupported) {
            Field field = GeckoLibCache.class.getDeclaredField("ANIMATIONS");
            field.setAccessible(true);
            Map<ResourceLocation, BakedAnimations> mutable = new HashMap<>(GeckoLibCache.getBakedAnimations());
            mutable.put(location, bakedAnimations);
            field.set(null, mutable);
        }
    }
}
