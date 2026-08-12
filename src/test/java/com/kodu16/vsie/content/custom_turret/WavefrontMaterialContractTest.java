package com.kodu16.vsie.content.custom_turret;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Guards the OBJ-to-MTL-to-atlas link relied on by exported structures and legacy empty-texture definitions. */
class WavefrontMaterialContractTest {
    @Test
    void resolvesExporterMaterialAndDiffuseAtlasDirectives() {
        String obj = "# export\nmtllib stoneturret_base.mtl\no stoneturret_base\n";
        String mtl = "newmtl atlas\nmap_Kd stoneturret_base_atlas.png\n";

        assertEquals("stoneturret_base.mtl", WavefrontMaterialContract.materialLibrary(obj));
        assertEquals("stoneturret_base_atlas.png", WavefrontMaterialContract.diffuseTexture(mtl));
    }

    @Test
    void missingDirectivesRemainEmpty() {
        assertEquals("", WavefrontMaterialContract.materialLibrary("o model\n"));
        assertEquals("", WavefrontMaterialContract.diffuseTexture("newmtl plain\n"));
    }
}
