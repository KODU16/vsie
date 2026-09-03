package com.kodu16.vsie.content.aeroie_custom;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Resolves authored firepoint pivots through the fixed custom-turret skeleton. */
public final class CustomTurretFirepointTransform {
    private CustomTurretFirepointTransform() {
    }

    public static float[] localFirepoint(CustomDeviceDefinition definition, int index,
                                         float pitchRadians, float yawRadians) {
        CustomDeviceDefinition.Bone firepoint = definition.findBone("firepoint" + index);
        if (firepoint == null) {
            return new float[]{0.0F, 0.0F, 0.0F};
        }
        Matrix4f transform = parentTransform(definition, firepoint, pitchRadians, yawRadians);
        Vector3f point = transform.transformPosition(
                firepoint.position[0] + firepoint.pivot[0],
                firepoint.position[1] + firepoint.pivot[1],
                firepoint.position[2] + firepoint.pivot[2], new Vector3f());
        return new float[]{point.x, point.y, point.z};
    }

    private static Matrix4f parentTransform(CustomDeviceDefinition definition, CustomDeviceDefinition.Bone bone,
                                            float pitchRadians, float yawRadians) {
        Matrix4f transform = new Matrix4f();
        if (bone.parent.isEmpty()) {
            return transform;
        }
        CustomDeviceDefinition.Bone parent = definition.findBone(bone.parent);
        if (parent == null) {
            return transform;
        }
        transform.set(parentTransform(definition, parent, pitchRadians, yawRadians));
        transform.translate(parent.position[0], parent.position[1], parent.position[2]);
        transform.translate(parent.pivot[0], parent.pivot[1], parent.pivot[2]);
        transform.rotateZ((float) Math.toRadians(parent.rotation[2]));
        transform.rotateY((float) Math.toRadians(parent.rotation[1]));
        transform.rotateX((float) Math.toRadians(parent.rotation[0]));
        transform.rotateY(CustomTurretRuntimePose.yawRadians(parent.id, yawRadians));
        transform.rotateX(CustomTurretRuntimePose.pitchRadians(parent.id, pitchRadians));
        transform.scale(parent.scale[0], parent.scale[1], parent.scale[2]);
        transform.translate(-parent.pivot[0], -parent.pivot[1], -parent.pivot[2]);
        return transform;
    }
}
