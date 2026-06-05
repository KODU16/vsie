package com.kodu16.vsie.content.controlseat.client;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlock;
import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AbstractControlSeatGeoRenderer extends AlwaysRenderGeoBlockRenderer<AbstractControlSeatBlockEntity> {
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

    @Override
    public boolean shouldRenderOffScreen(AbstractControlSeatBlockEntity be) {
        return isRenderableControlSeat(be);
    }

    @Override
    public boolean shouldRender(AbstractControlSeatBlockEntity be, Vec3 cameraPos) {
        return isRenderableControlSeat(be);
    }

    private boolean isRenderableControlSeat(AbstractControlSeatBlockEntity be) {
        if (be == null || be.isRemoved()) {
            return false;
        }

        Level level = be.getLevel();
        if (level == null) {
            return false;
        }

        // Function: stale Sable client block entities must not keep drawing a ghost control-seat model.
        return level.getBlockState(be.getBlockPos()).getBlock() instanceof AbstractControlSeatBlock
                && level.getBlockEntity(be.getBlockPos()) == be;
    }
}
