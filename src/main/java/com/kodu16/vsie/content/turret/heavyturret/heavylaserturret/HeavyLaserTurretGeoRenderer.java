package com.kodu16.vsie.content.turret.heavyturret.heavylaserturret;

import com.kodu16.vsie.content.turret.client.TurretLaserLayer;
import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class HeavyLaserTurretGeoRenderer extends AlwaysRenderGeoBlockRenderer<HeavyLaserTurretBlockEntity> {
    public HeavyLaserTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new HeavyLaserTurretModel());
        this.addRenderLayer(new HeavyLaserTurretFirePointLayer(this));
        this.addRenderLayer(new TurretLaserLayer<>(this));
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
    public boolean shouldRenderOffScreen(HeavyLaserTurretBlockEntity be) {
        return super.shouldRenderOffScreen(be);
    }

    @Override
    public boolean shouldRender(HeavyLaserTurretBlockEntity be, Vec3 cameraPos) {
        return super.shouldRender(be, cameraPos);
    }
}
