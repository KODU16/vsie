package com.kodu16.vsie.content.storage.fueltank;

import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class AbstractFuelTankGeoRenderer extends AlwaysRenderGeoBlockRenderer<AbstractFuelTankBlockEntity> {
    public AbstractFuelTankGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractFuelTankModel());
    }
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }
            case EAST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
            }
        }
    }
}
