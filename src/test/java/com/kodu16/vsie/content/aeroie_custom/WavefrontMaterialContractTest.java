package com.kodu16.vsie.content.aeroie_custom;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void resolvesEmissiveMaterialsFromExporterMarkerAndKe() {
        String mtl = "newmtl atlas\nmap_Kd atlas.png\n"
                + "newmtl atlas_emissive\n# aeroie_emissive true\nKe 1.000 1.000 1.000\n";

        assertEquals(1, WavefrontMaterialContract.emissiveMaterials(mtl).size());
        assertTrue(WavefrontMaterialContract.emissiveMaterials(mtl).contains("atlas_emissive"));
    }

    @Test
    void resolvesEveryMaterialOwnDiffuseTextureInsteadOfOnlyTheFirst() {
        String mtl = "newmtl body\nmap_Kd body.png\nKa 1.000 1.000 1.000\n"
                + "newmtl trim\nmap_Kd trim.png # diffuse trim\n"
                + "newmtl glow\nKe 1.000 1.000 1.000\n";

        Map<String, String> textures = WavefrontMaterialContract.diffuseTexturesByMaterial(mtl);

        assertEquals(2, textures.size());
        assertEquals("body.png", textures.get("body"));
        assertEquals("trim.png", textures.get("trim"));
        assertFalse(textures.containsKey("glow"));
    }

    @Test
    void collectsEveryReferencedMaterialLibraryInFirstUseOrder() {
        String obj = "mtllib base.mtl trim.mtl\nmtllib extra.mtl\n";

        assertEquals(List.of("base.mtl", "trim.mtl", "extra.mtl"),
                WavefrontMaterialContract.materialLibraries(obj));
    }
}
