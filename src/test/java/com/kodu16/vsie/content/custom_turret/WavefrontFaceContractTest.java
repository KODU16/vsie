package com.kodu16.vsie.content.custom_turret;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Reproduces the former cross-face triangle grouping caused by emitting three vertices into a QUADS render type. */
class WavefrontFaceContractTest {
    @Test
    void quadFaceProducesTwoIndependentDegenerateQuads() {
        assertArrayEquals(new int[]{0, 1, 2, 2, 0, 2, 3, 3},
                WavefrontFaceContract.quadCompatibleTriangleFan(4));
    }
}
