package com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret;

import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class HeavyElectroMagnetTurretGeoRenderer extends AlwaysRenderGeoBlockRenderer<HeavyElectroMagnetTurretBlockEntity> {

    public HeavyElectroMagnetTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new HeavyElectroMagnetTurretModel());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> {
                poseStack.translate(0, 0.5, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(270));
            }
            case WEST -> {
                poseStack.translate(-0.5, 0.5, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            }
            case NORTH -> {
                poseStack.translate(0, 0.5, -0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
            }
            case EAST -> {
                poseStack.translate(0.5, 0.5, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            }
            case UP -> {
                poseStack.translate(0, 1, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
            case DOWN -> {
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(HeavyElectroMagnetTurretBlockEntity be) {
        // Function: keep the heavy electromagnetic turret visible while Sable moves the block entity origin.
        return true;
    }

    @Override
    public boolean shouldRender(HeavyElectroMagnetTurretBlockEntity be, Vec3 cameraPos) {
        // Function: heavy electromagnetic turrets can move far from their block origin on ships.
        return true;
    }
}
