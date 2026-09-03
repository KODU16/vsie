package com.kodu16.vsie.content.aeroie_custom;

/** Converts OBJ faces to quad-compatible groups for Minecraft render buffers that consume four vertices per primitive. */
public final class WavefrontFaceContract {
    private WavefrontFaceContract() {
    }

    public static int[] quadCompatibleTriangleFan(int vertexCount) {
        if (vertexCount < 3) {
            return new int[0];
        }
        if (vertexCount == 4) {
            return new int[]{0, 1, 2, 3};
        }
        int[] indices = new int[(vertexCount - 2) * 4];
        for (int triangle = 0; triangle < vertexCount - 2; triangle++) {
            int output = triangle * 4;
            indices[output] = 0;
            indices[output + 1] = triangle + 1;
            indices[output + 2] = triangle + 2;
            indices[output + 3] = triangle + 2;
        }
        return indices;
    }
}
