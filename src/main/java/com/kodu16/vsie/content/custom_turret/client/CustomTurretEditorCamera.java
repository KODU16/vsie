package com.kodu16.vsie.content.custom_turret.client;

/** Defines the editor's initial camera projection independently from Minecraft rendering state. */
public final class CustomTurretEditorCamera {
    public static final float INITIAL_YAW_DEGREES = 35.0F;
    public static final float INITIAL_PITCH_DEGREES = 22.0F;

    private CustomTurretEditorCamera() {
    }

    public static float[] projectInitialDirection(float x, float y, float z) {
        double yaw = Math.toRadians(INITIAL_YAW_DEGREES);
        double pitch = Math.toRadians(INITIAL_PITCH_DEGREES);
        // Function: mirror the editor's Y-then-X model-view rotation and its inverted screen Y axis.
        double yawX = Math.cos(yaw) * x + Math.sin(yaw) * z;
        double yawZ = -Math.sin(yaw) * x + Math.cos(yaw) * z;
        double pitchY = Math.cos(pitch) * y - Math.sin(pitch) * yawZ;
        return new float[]{(float) yawX, (float) -pitchY};
    }

    public static float[] pan(float currentX, float currentY, float dragX, float dragY) {
        // Function: screen-space panning follows the held left mouse button one-to-one in GUI pixels.
        return new float[]{currentX + dragX, currentY + dragY};
    }
}
