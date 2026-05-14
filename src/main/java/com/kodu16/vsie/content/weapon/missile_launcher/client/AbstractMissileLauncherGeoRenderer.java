package com.kodu16.vsie.content.weapon.missile_launcher.client;

import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.kodu16.vsie.content.weapon.missile_launcher.AbstractMissileLauncherBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class AbstractMissileLauncherGeoRenderer extends AlwaysRenderGeoBlockRenderer<AbstractMissileLauncherBlockEntity> {

    public AbstractMissileLauncherGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractMissileLauncherModel());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> {
                poseStack.translate(0, 0.5, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(-270));
            }
            case WEST -> {
                poseStack.translate(-0.5, 0.5, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            }
            case NORTH -> {
                poseStack.translate(0, 0.5, -0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            }
            case EAST -> {
                poseStack.translate(0.5, 0.5, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            }
            case UP -> {
                poseStack.translate(0, 1, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-180));
            }
            case DOWN -> {

            }
        }
    }

}
