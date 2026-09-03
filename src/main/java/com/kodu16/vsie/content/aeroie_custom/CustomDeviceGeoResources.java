package com.kodu16.vsie.content.aeroie_custom;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Loads saved custom-device GeckoLib geo files from AeroIE device folders into GeckoLib's model cache. */
public final class CustomDeviceGeoResources {
    public static final String NAMESPACE = "aeroie_custom";
    private static final String RESOURCE_PREFIX = "geo/custom_device/";
    private static final long MAX_GEO_BYTES = 2L * 1024L * 1024L;

    private CustomDeviceGeoResources() {
    }

    public static ResourceLocation toResourceLocation(String deviceType, String relativeGeoPath) {
        String normalized = CustomDeviceStorage.normalizeRelativePath(relativeGeoPath);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Custom GeckoLib model path is empty");
        }
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, RESOURCE_PREFIX + deviceType + "/" + normalized);
    }

    public static boolean ensureLoaded(String deviceType, String relativeGeoPath) {
        String normalized = CustomDeviceStorage.normalizeRelativePath(relativeGeoPath);
        if (normalized.isBlank()) {
            return false;
        }
        ResourceLocation location = toResourceLocation(deviceType, normalized);
        if (GeckoLibCache.getBakedModels().containsKey(location)) {
            return true;
        }
        try {
            BakedGeoModel model = readModel(CustomDeviceStorage.resolveResource(deviceType, normalized));
            cacheModel(location, model);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static BakedGeoModel readModel(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_GEO_BYTES) {
            throw new IOException("Missing or oversized custom GeckoLib model file");
        }
        Model model = KeyFramesAdapter.GEO_GSON.fromJson(
                Files.readString(path, StandardCharsets.UTF_8), Model.class);
        return BakedModelFactory.getForNamespace(NAMESPACE).constructGeoModel(GeometryTree.fromModel(model));
    }

    private static void cacheModel(ResourceLocation location, BakedGeoModel model) throws Exception {
        try {
            GeckoLibCache.getBakedModels().put(location, model);
        } catch (UnsupportedOperationException unsupported) {
            Field field = GeckoLibCache.class.getDeclaredField("MODELS");
            field.setAccessible(true);
            Map<ResourceLocation, BakedGeoModel> mutable = new HashMap<>(GeckoLibCache.getBakedModels());
            mutable.put(location, model);
            field.set(null, mutable);
        }
    }
}
