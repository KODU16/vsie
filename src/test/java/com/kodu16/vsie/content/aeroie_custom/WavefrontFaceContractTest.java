package com.kodu16.vsie.content.aeroie_custom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Guards OBJ face conversion before vertices are emitted into Minecraft's QUADS render type. */
class WavefrontFaceContractTest {
    @Test
    void quadFaceStaysOneQuad() {
        assertArrayEquals(new int[]{0, 1, 2, 3},
                WavefrontFaceContract.quadCompatibleTriangleFan(4));
    }

    @Test
    void triangleFaceUsesOneDegenerateQuad() {
        assertArrayEquals(new int[]{0, 1, 2, 2},
                WavefrontFaceContract.quadCompatibleTriangleFan(3));
    }
}
