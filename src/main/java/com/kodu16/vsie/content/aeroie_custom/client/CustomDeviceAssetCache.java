package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceStorage;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

/** Reloads external OBJ and PNG files when their modification timestamp changes. */
final class CustomDeviceAssetCache {
    private static final Map<Path, MeshEntry> MESHES = new HashMap<>();
    private static final Map<Path, TextureEntry> TEXTURES = new HashMap<>();
    private static final Map<String, String> MATERIAL_TEXTURES = new HashMap<>();
    private static final Map<String, Map<String, String>> MATERIAL_TEXTURE_MAPS = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    private CustomDeviceAssetCache() {
    }

    static CustomObjMesh mesh(String relativePath) {
        return mesh("turret", relativePath);
    }

    static CustomObjMesh mesh(String deviceType, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return CustomObjMesh.EMPTY;
        }
        try {
            Path path = CustomDeviceStorage.resolveResource(deviceType, relativePath);
            FileTime modified = Files.getLastModifiedTime(path);
            MeshEntry cached = MESHES.get(path);
            if (cached == null || !cached.modified.equals(modified)) {
                LOGGER.info("[AEROIE-OBJ-DIAG] phase=OBJ_CACHE_RELOAD deviceType={} relativePath={} path={} modified={}",
                        deviceType, relativePath, path, modified);
                cached = new MeshEntry(modified, CustomObjMesh.load(path));
                MESHES.put(path, cached);
            }
            return cached.mesh;
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("[AEROIE-OBJ-DIAG] phase=OBJ_CACHE_FAILED deviceType={} relativePath={} error={}",
                    deviceType, relativePath, exception.toString());
            return CustomObjMesh.EMPTY;
        }
    }

    /** Resolves a fresh copy of the cached OBJ-local bounds center. */
    static Vector3f modelCenter(String relativePath) {
        return modelCenter("turret", relativePath);
    }

    static Vector3f modelCenter(String deviceType, String relativePath) {
        return mesh(deviceType, relativePath).center();
    }

    static ResourceLocation texture(String relativePath, ResourceLocation fallback) {
        return texture("turret", relativePath, fallback);
    }

    static ResourceLocation texture(String deviceType, String relativePath, ResourceLocation fallback) {
        if (relativePath == null || relativePath.isBlank()) {
            return fallback;
        }
        try {
            Path path = CustomDeviceStorage.resolveResource(deviceType, relativePath);
            FileTime modified = Files.getLastModifiedTime(path);
            TextureEntry cached = TEXTURES.get(path);
            if (cached == null || !cached.modified.equals(modified)) {
                if (cached != null) {
                    Minecraft.getInstance().getTextureManager().release(cached.location);
                }
                try (InputStream stream = Files.newInputStream(path)) {
                    DynamicTexture texture = new DynamicTexture(NativeImage.read(stream));
                    ResourceLocation location = Minecraft.getInstance().getTextureManager()
                            .register("vsie_custom_turret", texture);
                    cached = new TextureEntry(modified, location);
                    TEXTURES.put(path, cached);
                }
            }
            return cached.location;
        } catch (IOException | IllegalArgumentException exception) {
            return fallback;
        }
    }

    /** Uses the OBJ material only when the definition has no explicit texture override. */
    static String texturePath(String modelPath, String explicitTexturePath) {
        return texturePath("turret", modelPath, explicitTexturePath);
    }

    static String texturePath(String deviceType, String modelPath, String explicitTexturePath) {
        if (explicitTexturePath != null && !explicitTexturePath.isBlank()) {
            return explicitTexturePath;
        }
        if (modelPath == null || modelPath.isBlank()) {
            return "";
        }
        return MATERIAL_TEXTURES.computeIfAbsent(deviceType + ":" + modelPath,
                ignored -> CustomObjMaterialResolver.resolveTexture(deviceType, modelPath));
    }

    /** Resolves each OBJ material's diffuse texture for multi-material rendering, cached per model. */
    static Map<String, String> materialTextures(String deviceType, String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            return Map.of();
        }
        return MATERIAL_TEXTURE_MAPS.computeIfAbsent(deviceType + ":" + modelPath,
                ignored -> CustomObjMaterialResolver.resolveTextures(deviceType, modelPath));
    }

    static void invalidateMeshes() {
        MESHES.clear();
        MATERIAL_TEXTURES.clear();
        MATERIAL_TEXTURE_MAPS.clear();
    }

    private record MeshEntry(FileTime modified, CustomObjMesh mesh) {
    }

    private record TextureEntry(FileTime modified, ResourceLocation location) {
    }
}
