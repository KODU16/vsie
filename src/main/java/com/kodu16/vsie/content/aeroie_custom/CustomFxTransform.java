package com.kodu16.vsie.content.aeroie_custom;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Converts pure custom FX config data into Photon transform objects at runtime. */
public final class CustomFxTransform {
    private CustomFxTransform() {
    }

    public static Vector3f scaleVector(CustomFxConfig config) {
        float safeScale = config == null || !Float.isFinite(config.scale)
                ? 1.0F : Math.max(0.001F, Math.min(1000.0F, config.scale));
        return new Vector3f(safeScale, safeScale, safeScale);
    }

    public static Quaternionf rotationForDirection(CustomFxConfig config, Vector3d direction) {
        Vector3f forward = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
        if (!Float.isFinite(forward.x) || !Float.isFinite(forward.y) || !Float.isFinite(forward.z)
                || forward.lengthSquared() < 1.0E-8F) {
            forward.set(0.0F, 0.0F, 1.0F);
        } else {
            forward.normalize();
        }
        // Function: align Photon local +Z to the sampled world point direction, then add authored XYZ rotation.
        return new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), forward)
                .rotateX((float) Math.toRadians(component(config, 0)))
                .rotateY((float) Math.toRadians(component(config, 1)))
                .rotateZ((float) Math.toRadians(component(config, 2)));
    }

    private static float component(CustomFxConfig config, int index) {
        return config != null && config.rotation != null && config.rotation.length == 3
                && Float.isFinite(config.rotation[index]) ? config.rotation[index] : 0.0F;
    }
}
