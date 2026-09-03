package com.kodu16.vsie.content.aeroie_custom;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Protects model-center pivot calculations across negative and positive OBJ coordinates. */
class WavefrontBoundsContractTest {
    @Test
    void findsAxisAlignedBoundsCenter() {
        float[] center = WavefrontBoundsContract.center(List.of(
                new float[]{-4.0F, -2.0F, 3.0F},
                new float[]{2.0F, 6.0F, 11.0F},
                new float[]{0.0F, 0.0F, 4.0F}
        ));

        assertArrayEquals(new float[]{-1.0F, 2.0F, 7.0F}, center, 0.0001F);
    }
}
