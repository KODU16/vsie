package com.kodu16.vsie.content.aeroie_custom;

/** Maps AeroIE's common turret pitch/yaw state onto the fixed custom bone groups. */
public final class CustomTurretRuntimePose {
    private static final float ROTATION_SMOOTHING = 0.1F;
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = PI * 2.0F;

    private CustomTurretRuntimePose() {
    }

    public static float pitchRadians(String boneId, float turretPitchRadians) {
        // Function: positive AeroIE pitch raises the authored +Z muzzle around the cannon's X axis.
        return "cannon".equals(boneId) ? -turretPitchRadians : 0.0F;
    }

    public static float yawRadians(String boneId, float turretYawRadians) {
        // Function: turn authored +Z forward into AeroIE's neutral -Z before applying the shared yaw state.
        return "turret".equals(boneId) ? PI + turretYawRadians : 0.0F;
    }

    public static float[] defaultBarrelForward() {
        // Function: X-axis cannon pitch is true elevation only when the authored barrel points along local +Z.
        return new float[]{0.0F, 0.0F, 1.0F};
    }

    public static float[] aimedBarrelForward(float turretPitchRadians, float turretYawRadians) {
        // Function: expose the runtime +Z muzzle direction for deterministic targeting regression tests.
        float pitch = pitchRadians("cannon", turretPitchRadians);
        float yaw = yawRadians("turret", turretYawRadians);
        float pitchCos = (float) Math.cos(pitch);
        return new float[]{
                (float) Math.sin(yaw) * pitchCos,
                -(float) Math.sin(pitch),
                (float) Math.cos(yaw) * pitchCos
        };
    }

    public static float smoothAngle(float currentRadians, float targetRadians) {
        if (!Float.isFinite(currentRadians) || !Float.isFinite(targetRadians)) {
            return Float.isFinite(targetRadians) ? targetRadians : 0.0F;
        }
        // Function: interpolate across the shortest angular arc so the +/-PI boundary cannot cause a full turn.
        float delta = wrapRadians(targetRadians - currentRadians);
        return currentRadians + delta * ROTATION_SMOOTHING;
    }

    private static float wrapRadians(float radians) {
        float wrapped = radians % TWO_PI;
        if (wrapped >= PI) {
            wrapped -= TWO_PI;
        } else if (wrapped < -PI) {
            wrapped += TWO_PI;
        }
        return wrapped;
    }
}
