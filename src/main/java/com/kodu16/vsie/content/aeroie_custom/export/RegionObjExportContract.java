package com.kodu16.vsie.content.aeroie_custom.export;

/** Dependency-free contract seam for selection volume and OBJ index progression. */
public final class RegionObjExportContract {
    private RegionObjExportContract() {
    }

    public static long inclusiveVolume(int x1, int y1, int z1, int x2, int y2, int z2) {
        return (long) (Math.abs(x2 - x1) + 1)
                * (Math.abs(y2 - y1) + 1)
                * (Math.abs(z2 - z1) + 1);
    }

    public static int vertexIndexForQuad(int zeroBasedQuad) {
        if (zeroBasedQuad < 0) {
            throw new IllegalArgumentException("Quad index cannot be negative");
        }
        return zeroBasedQuad * 4 + 1;
    }
}
