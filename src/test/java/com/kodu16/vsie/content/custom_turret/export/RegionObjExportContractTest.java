package com.kodu16.vsie.content.custom_turret.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Guards inclusive selection semantics and the four-vertices-per-quad OBJ layout. */
class RegionObjExportContractTest {
    @Test
    void selectionVolumeIncludesBothSavedCorners() {
        assertEquals(1L, RegionObjExportContract.inclusiveVolume(4, 8, 15, 4, 8, 15));
        assertEquals(36L, RegionObjExportContract.inclusiveVolume(3, 5, 7, 0, 3, 5));
    }

    @Test
    void eachQuadAdvancesFourObjVertexIndices() {
        assertEquals(1, RegionObjExportContract.vertexIndexForQuad(0));
        assertEquals(5, RegionObjExportContract.vertexIndexForQuad(1));
        assertEquals(401, RegionObjExportContract.vertexIndexForQuad(100));
        assertThrows(IllegalArgumentException.class, () -> RegionObjExportContract.vertexIndexForQuad(-1));
    }
}
