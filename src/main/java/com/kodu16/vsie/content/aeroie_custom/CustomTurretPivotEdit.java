package com.kodu16.vsie.content.aeroie_custom;

/** Applies editor pivot drags while preserving the authored OBJ placement. */
public final class CustomTurretPivotEdit {
    private CustomTurretPivotEdit() {
    }

    public static void movePivotKeepingGeometryStable(CustomDeviceDefinition.Bone bone, int axis, float amount) {
        if (axis < 0 || axis >= 3 || !Float.isFinite(amount)) {
            throw new IllegalArgumentException("Invalid pivot drag");
        }
        float[] delta = new float[]{0.0F, 0.0F, 0.0F};
        delta[axis] = amount;
        float[] transformedDelta = transformDirection(bone, delta);
        // Moving a rotation origin requires the inverse position correction so the OBJ and its children stay still.
        for (int component = 0; component < 3; component++) {
            bone.position[component] += transformedDelta[component] - delta[component];
        }
        bone.pivot[axis] += amount;
    }

    public static float[] transformDirection(CustomDeviceDefinition.Bone bone, float[] direction) {
        float[] transformed = new float[]{
                direction[0] * bone.scale[0],
                direction[1] * bone.scale[1],
                direction[2] * bone.scale[2]
        };
        rotateX(transformed, (float) Math.toRadians(bone.rotation[0]));
        rotateY(transformed, (float) Math.toRadians(bone.rotation[1]));
        rotateZ(transformed, (float) Math.toRadians(bone.rotation[2]));
        return transformed;
    }

    static float[] transformPoint(CustomDeviceDefinition.Bone bone, float[] point) {
        float[] local = transformDirection(bone, new float[]{
                point[0] - bone.pivot[0],
                point[1] - bone.pivot[1],
                point[2] - bone.pivot[2]
        });
        return new float[]{
                bone.position[0] + bone.pivot[0] + local[0],
                bone.position[1] + bone.pivot[1] + local[1],
                bone.position[2] + bone.pivot[2] + local[2]
        };
    }

    private static void rotateX(float[] value, float radians) {
        float y = value[1] * (float) Math.cos(radians) - value[2] * (float) Math.sin(radians);
        float z = value[1] * (float) Math.sin(radians) + value[2] * (float) Math.cos(radians);
        value[1] = y;
        value[2] = z;
    }

    private static void rotateY(float[] value, float radians) {
        float x = value[0] * (float) Math.cos(radians) + value[2] * (float) Math.sin(radians);
        float z = -value[0] * (float) Math.sin(radians) + value[2] * (float) Math.cos(radians);
        value[0] = x;
        value[2] = z;
    }

    private static void rotateZ(float[] value, float radians) {
        float x = value[0] * (float) Math.cos(radians) - value[1] * (float) Math.sin(radians);
        float y = value[0] * (float) Math.sin(radians) + value[1] * (float) Math.cos(radians);
        value[0] = x;
        value[1] = y;
    }
}
