package com.kodu16.vsie.content.misc.electromagnet_rail.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ElectroMagnetRailCoreGeoRenderer extends GeoBlockRenderer<ElectroMagnetRailCoreBlockEntity> {
    public ElectroMagnetRailCoreGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new ElectroMagnetRailCoreModel());
        // 功能：给 core 渲染器附加双平行光束层，按绑定状态与推进进度动态绘制。
        this.addRenderLayer(new ElectroMagnetRailCoreBeamLayer(this));
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-180));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
            }
            case EAST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            }
        }
    }
}
