package com.kodu16.vsie.content.storage.energybattery;

import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class AbstractEnergyBatteryGeoRenderer extends AlwaysRenderGeoBlockRenderer<AbstractEnergyBatteryBlockEntity> {
    public AbstractEnergyBatteryGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractEnergyBatteryModel());
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
