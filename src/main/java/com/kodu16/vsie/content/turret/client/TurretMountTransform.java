package com.kodu16.vsie.content.turret.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.kodu16.vsie.content.turret.TurretMountAxes;
import net.minecraft.core.Direction;

/** Applies AeroIE's shared six-face turret mounting transform. */
public final class TurretMountTransform {
    private TurretMountTransform() {
    }

    public static void apply(Direction facing, PoseStack poseStack) {
        // Function: keep model forward/up axes aligned with the targeting basis for every mounting face.
        TurretMountAxes.Mount mount = TurretMountAxes.mount(facing.getSerializedName());
        poseStack.translate(mount.translateX(), mount.translateY(), mount.translateZ());
        if (mount.rotateXDegrees() != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(mount.rotateXDegrees()));
        }
        if (mount.rotateZDegrees() != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(mount.rotateZDegrees()));
        }
    }
}
