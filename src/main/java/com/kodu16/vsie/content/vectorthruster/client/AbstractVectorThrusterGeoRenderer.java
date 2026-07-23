package com.kodu16.vsie.content.vectorthruster.client;

import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlockEntity;
import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class AbstractVectorThrusterGeoRenderer extends AlwaysRenderGeoBlockRenderer<AbstractVectorThrusterBlockEntity> {

    public AbstractVectorThrusterGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractVectorThrusterModel());
        this.addRenderLayer(new VectorThrusterFlameLayer(this));
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> {
                poseStack.translate(0, 0.5, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
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
    public boolean shouldRenderOffScreen(AbstractVectorThrusterBlockEntity be) {
        return super.shouldRenderOffScreen(be);
    }

    @Override
    public boolean shouldRender(AbstractVectorThrusterBlockEntity be, Vec3 cameraPos) {
        return super.shouldRender(be, cameraPos);
    }
}
