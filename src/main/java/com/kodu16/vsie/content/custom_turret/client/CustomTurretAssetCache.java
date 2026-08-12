package com.kodu16.vsie.content.custom_turret.client;

import com.kodu16.vsie.content.custom_turret.CustomTurretStorage;
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

/** Reloads external OBJ and PNG files when their modification timestamp changes. */
final class CustomTurretAssetCache {
    private static final Map<Path, MeshEntry> MESHES = new HashMap<>();
    private static final Map<Path, TextureEntry> TEXTURES = new HashMap<>();
    private static final Map<String, String> MATERIAL_TEXTURES = new HashMap<>();

    private CustomTurretAssetCache() {
    }

    static CustomObjMesh mesh(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return CustomObjMesh.EMPTY;
        }
        try {
            Path path = CustomTurretStorage.resolveResource(relativePath);
            FileTime modified = Files.getLastModifiedTime(path);
            MeshEntry cached = MESHES.get(path);
            if (cached == null || !cached.modified.equals(modified)) {
                cached = new MeshEntry(modified, CustomObjMesh.load(path));
                MESHES.put(path, cached);
            }
            return cached.mesh;
        } catch (IOException | IllegalArgumentException exception) {
            return CustomObjMesh.EMPTY;
        }
    }

    /** Resolves a fresh copy of the cached OBJ-local bounds center. */
    static Vector3f modelCenter(String relativePath) {
        return mesh(relativePath).center();
    }

    static ResourceLocation texture(String relativePath, ResourceLocation fallback) {
        if (relativePath == null || relativePath.isBlank()) {
            return fallback;
        }
        try {
            Path path = CustomTurretStorage.resolveResource(relativePath);
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
        if (explicitTexturePath != null && !explicitTexturePath.isBlank()) {
            return explicitTexturePath;
        }
        if (modelPath == null || modelPath.isBlank()) {
            return "";
        }
        return MATERIAL_TEXTURES.computeIfAbsent(modelPath, CustomObjMaterialResolver::resolveTexture);
    }

    static void invalidateMeshes() {
        MESHES.clear();
        MATERIAL_TEXTURES.clear();
    }

    private record MeshEntry(FileTime modified, CustomObjMesh mesh) {
    }

    private record TextureEntry(FileTime modified, ResourceLocation location) {
    }
}
