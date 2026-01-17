package com.kodu16.vsie.content.controlseat.client;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.turret.client.AbstractTurretModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AbstractControlSeatGeoRenderer extends GeoBlockRenderer<AbstractControlSeatBlockEntity> {
    public AbstractControlSeatGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractControlSeatModel());
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
