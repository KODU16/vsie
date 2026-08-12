package com.kodu16.vsie.content.custom_turret.client;

import com.kodu16.vsie.content.custom_turret.CustomTurretStorage;
import com.kodu16.vsie.content.custom_turret.WavefrontMaterialContract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves an OBJ material's diffuse texture while keeping every referenced file inside the turret root. */
final class CustomObjMaterialResolver {
    private static final long MAX_MATERIAL_SOURCE_BYTES = 1_048_576L;

    private CustomObjMaterialResolver() {
    }

    static String resolveTexture(String modelRelativePath) {
        if (modelRelativePath == null || modelRelativePath.isBlank()) {
            return "";
        }
        try {
            String model = CustomTurretStorage.normalizeRelativePath(modelRelativePath);
            Path objPath = CustomTurretStorage.resolveResource(model);
            String objSource = readSmallText(objPath);
            String materialLibrary = WavefrontMaterialContract.materialLibrary(objSource);
            if (materialLibrary.isBlank()) {
                int extension = model.lastIndexOf('.');
                materialLibrary = (extension >= 0 ? model.substring(0, extension) : model) + ".mtl";
            } else {
                materialLibrary = resolveSibling(model, materialLibrary);
            }

            String diffuseTexture = WavefrontMaterialContract.diffuseTexture(
                    readSmallText(CustomTurretStorage.resolveResource(materialLibrary))
            );
            return diffuseTexture.isBlank() ? "" : resolveSibling(materialLibrary, diffuseTexture);
        } catch (IOException | IllegalArgumentException exception) {
            return "";
        }
    }

    private static String readSmallText(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_MATERIAL_SOURCE_BYTES) {
            return "";
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String resolveSibling(String ownerRelativePath, String referencedPath) {
        Path owner = Path.of(CustomTurretStorage.normalizeRelativePath(ownerRelativePath));
        Path parent = owner.getParent();
        Path relative = parent == null ? Path.of(referencedPath) : parent.resolve(referencedPath);
        return CustomTurretStorage.normalizeRelativePath(relative.normalize().toString());
    }
}
