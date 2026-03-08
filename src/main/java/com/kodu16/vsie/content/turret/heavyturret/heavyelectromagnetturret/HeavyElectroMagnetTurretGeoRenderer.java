package com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HeavyElectroMagnetTurretGeoRenderer extends GeoBlockRenderer<HeavyElectroMagnetTurretBlockEntity> {

    public HeavyElectroMagnetTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new HeavyElectroMagnetTurretModel());

        // 注册 Layer！这是关键，不会触发任何 override
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
    public boolean shouldRenderOffScreen(HeavyElectroMagnetTurretBlockEntity be) {
        return true;   // 或者 return distanceSq < 某个超大值 的平方
    }

    @Override
    public boolean shouldRender(HeavyElectroMagnetTurretBlockEntity be, Vec3 cameraPos) {
        // 自己写距离判断，比如 256 格以内都渲染
        return be.getBlockPos().distSqr(new Vec3i((int) cameraPos.x, (int) cameraPos.y, (int) cameraPos.z)) < 2048 * 2048;
    }

}