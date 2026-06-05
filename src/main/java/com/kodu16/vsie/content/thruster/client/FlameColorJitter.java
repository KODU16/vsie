package com.kodu16.vsie.content.thruster.client;

public final class FlameColorJitter {
    private static final float HEAT_RED_GAIN = 0.10F;
    private static final float HEAT_GREEN_GAIN = 0.07F;
    private static final float HEAT_BLUE_LOSS = 0.11F;
    private static final float COOL_BLUE_GAIN = 0.08F;

    private FlameColorJitter() {
    }

    public static float[] apply(long seed, float time, float t, float r, float g, float b, float a) {
        float movingBand = smoothNoise(seed, t * 7.0F - time * 0.045F);
        float fineBand = smoothNoise(seed ^ 0x6A09E667F3BCC909L, t * 17.0F + time * 0.085F);
        // Function: blend slow longitudinal bands with finer variation so the flame breathes instead of flickering.
        float turbulence = movingBand * 0.72F + fineBand * 0.28F;
        float hot = Math.max(0.0F, turbulence);
        float cool = Math.max(0.0F, -turbulence);

        return new float[]{
                clamp01(r + hot * HEAT_RED_GAIN - cool * 0.03F),
                clamp01(g + hot * HEAT_GREEN_GAIN + fineBand * 0.025F),
                clamp01(b - hot * HEAT_BLUE_LOSS + cool * COOL_BLUE_GAIN),
                clamp01(a * (1.0F + turbulence * 0.08F))
        };
    }

    private static float smoothNoise(long seed, float x) {
        int base = (int) Math.floor(x);
        float fraction = x - base;
        float smoothFraction = fraction * fraction * (3.0F - 2.0F * fraction);
        return lerp(hashSigned(seed, base), hashSigned(seed, base + 1), smoothFraction);
    }

    private static float hashSigned(long seed, int index) {
        long value = seed + index * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return ((value >>> 40) / (float) 0xFFFFFF) * 2.0F - 1.0F;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        return Math.min(value, 1.0F);
    }
}
