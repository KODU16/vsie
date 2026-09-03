package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceStorage;
import com.kodu16.vsie.content.aeroie_custom.WavefrontMaterialContract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves an OBJ material's diffuse texture while keeping every referenced file inside the device root. */
final class CustomObjMaterialResolver {
    private static final long MAX_MATERIAL_SOURCE_BYTES = 1_048_576L;

    private CustomObjMaterialResolver() {
    }

    static String resolveTexture(String modelRelativePath) {
        return resolveTexture("turret", modelRelativePath);
    }

    static String resolveTexture(String deviceType, String modelRelativePath) {
        Map<String, String> textures = resolveTextures(deviceType, modelRelativePath);
        return textures.isEmpty() ? "" : textures.values().iterator().next();
    }

    /** Resolves every MTL material's diffuse texture, keyed by material name and kept relative to its MTL file. */
    static Map<String, String> resolveTextures(String deviceType, String modelRelativePath) {
        if (modelRelativePath == null || modelRelativePath.isBlank()) {
            return Map.of();
        }
        try {
            String model = CustomDeviceStorage.normalizeRelativePath(modelRelativePath);
            Path objPath = CustomDeviceStorage.resolveResource(deviceType, model);
            String objSource = readSmallText(objPath);
            List<String> materialLibraries = WavefrontMaterialContract.materialLibraries(objSource);
            if (materialLibraries.isEmpty()) {
                int extension = model.lastIndexOf('.');
                materialLibraries = List.of((extension >= 0 ? model.substring(0, extension) : model) + ".mtl");
            }
            Map<String, String> textures = new LinkedHashMap<>();
            for (String materialLibrary : materialLibraries) {
                String libraryPath = resolveSibling(model, materialLibrary);
                String mtlSource = readSmallText(CustomDeviceStorage.resolveResource(deviceType, libraryPath));
                for (Map.Entry<String, String> entry
                        : WavefrontMaterialContract.diffuseTexturesByMaterial(mtlSource).entrySet()) {
                    // Function: keep the first library's texture so merged MTL files cannot shadow earlier materials.
                    textures.putIfAbsent(entry.getKey(), resolveSibling(libraryPath, entry.getValue()));
                }
            }
            return Collections.unmodifiableMap(textures);
        } catch (IOException | IllegalArgumentException exception) {
            return Map.of();
        }
    }

    private static String readSmallText(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_MATERIAL_SOURCE_BYTES) {
            return "";
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String resolveSibling(String ownerRelativePath, String referencedPath) {
        Path owner = Path.of(CustomDeviceStorage.normalizeRelativePath(ownerRelativePath));
        Path parent = owner.getParent();
        Path relative = parent == null ? Path.of(referencedPath) : parent.resolve(referencedPath);
        return CustomDeviceStorage.normalizeRelativePath(relative.normalize().toString());
    }
}
