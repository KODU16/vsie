package com.kodu16.vsie.content.misc.electromagnet_rail.top;

import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class ElectroMagnetRailTopGeoRenderer extends AlwaysRenderGeoBlockRenderer<ElectroMagnetRailTopBlockEntity> {
    public ElectroMagnetRailTopGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new ElectroMagnetRailTopModel());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(270));
            }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            case EAST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }
        }
    }
}
