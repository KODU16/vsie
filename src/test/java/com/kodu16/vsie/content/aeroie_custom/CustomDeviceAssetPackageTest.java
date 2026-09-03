package com.kodu16.vsie.content.aeroie_custom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the one-folder, one-OBJ, one-texture naming contract used by export and editor selection. */
class CustomDeviceAssetPackageTest {
    @Test
    void derivesMatchingFilesFromOnePackageName() {
        CustomDeviceAssetPackage assetPackage = CustomDeviceAssetPackage.named("stone_turret");

        assertEquals("stone_turret/stone_turret.obj", assetPackage.modelPath());
        assertEquals("stone_turret/stone_turret.mtl", assetPackage.materialPath());
        assertEquals("stone_turret/stone_turret.png", assetPackage.texturePath());
        assertEquals(assetPackage, CustomDeviceAssetPackage.fromModelPath(assetPackage.modelPath()).orElseThrow());
    }

    @Test
    void legacyFlatModelsAreNotMistakenForPackages() {
        assertTrue(CustomDeviceAssetPackage.fromModelPath("legacy.obj").isEmpty());
    }
}
