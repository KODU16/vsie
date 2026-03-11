package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class AbstractTurretGeoRenderer extends GeoBlockRenderer<AbstractTurretBlockEntity>  {

    public AbstractTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractTurretModel());

        // 注册 Layer！这是关键，不会触发任何 override
        this.addRenderLayer(new TurretLaserLayer(this));
        //this.addRenderLayer(new TurretFlameLayer(this));
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

    @Override
    public boolean shouldRenderOffScreen(AbstractTurretBlockEntity be) {
        return true;   // 或者 return distanceSq < 某个超大值 的平方
    }

    @Override
    public boolean shouldRender(AbstractTurretBlockEntity be, Vec3 cameraPos) {
        // 自己写距离判断，比如 256 格以内都渲染
        return be.getBlockPos().distSqr(new Vec3i((int) cameraPos.x, (int) cameraPos.y, (int) cameraPos.z)) < 2048 * 2048;
    }

}
