package com.kodu16.vsie.content.misc.electromagnet_rail.core;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.screen.client.AbstractScreenRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ElectroMagnetRailCoreGeoRenderer extends GeoBlockRenderer<ElectroMagnetRailCoreBlockEntity> {
    public ElectroMagnetRailCoreGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new ElectroMagnetRailCoreModel());
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
