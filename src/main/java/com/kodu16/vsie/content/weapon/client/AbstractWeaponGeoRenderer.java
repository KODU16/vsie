package com.kodu16.vsie.content.weapon.client;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AbstractWeaponGeoRenderer extends GeoBlockRenderer<AbstractWeaponBlockEntity> {
    public AbstractWeaponGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractWeaponModel());
        this.addRenderLayer(new WeaponLaserLayer(this));
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
}
