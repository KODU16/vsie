package com.kodu16.vsie.content.aeroie_custom;

import net.minecraft.resources.ResourceLocation;

/** Stores optional Photon FX transforms authored for custom device points and projectiles. */
public final class CustomFxConfig {
    public String fx = "";
    public float scale = 1.0F;
    public float[] rotation = new float[]{0.0F, 0.0F, 0.0F};

    public boolean isEnabled() {
        return !fx.isBlank();
    }

    public ResourceLocation resourceLocation() {
        return CustomFxResources.toResourceLocation(fx);
    }

    public void normalizeAndValidate() {
        fx = CustomFxResources.normalizeRelativeFxPath(fx);
        if (!Float.isFinite(scale) || scale < 0.001F || scale > 1000.0F) {
            throw new IllegalArgumentException("Custom FX scale must be 0.001-1000");
        }
        rotation = validateRotation(rotation);
    }

    private static float[] validateRotation(float[] value) {
        if (value == null) {
            return new float[]{0.0F, 0.0F, 0.0F};
        }
        if (value.length != 3) {
            throw new IllegalArgumentException("Custom FX rotation must have three values");
        }
        for (float component : value) {
            if (!Float.isFinite(component) || component < -36000.0F || component > 36000.0F) {
                throw new IllegalArgumentException("Invalid custom FX rotation");
            }
        }
        return value;
    }
}
