package com.kodu16.vsie.content.custom_turret;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the one-folder, one-OBJ, one-texture naming contract used by export and editor selection. */
class CustomTurretAssetPackageTest {
    @Test
    void derivesMatchingFilesFromOnePackageName() {
        CustomTurretAssetPackage assetPackage = CustomTurretAssetPackage.named("stone_turret");

        assertEquals("stone_turret/stone_turret.obj", assetPackage.modelPath());
        assertEquals("stone_turret/stone_turret.mtl", assetPackage.materialPath());
        assertEquals("stone_turret/stone_turret.png", assetPackage.texturePath());
        assertEquals(assetPackage, CustomTurretAssetPackage.fromModelPath(assetPackage.modelPath()).orElseThrow());
    }

    @Test
    void legacyFlatModelsAreNotMistakenForPackages() {
        assertTrue(CustomTurretAssetPackage.fromModelPath("legacy.obj").isEmpty());
    }
}
