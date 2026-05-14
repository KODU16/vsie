package com.kodu16.vsie.content.weapon.client;

import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class AbstractWeaponGeoRenderer extends AlwaysRenderGeoBlockRenderer<AbstractWeaponBlockEntity> {
    public AbstractWeaponGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractWeaponModel());
        this.addRenderLayer(new WeaponLaserLayer(this));
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }
            case NORTH -> {
            }
            case EAST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(270));
            }
            case UP -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
            }
            case DOWN -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(AbstractWeaponBlockEntity be) {
        return true;   // 或者 return distanceSq < 某个超大值 的平方
    }

    @Override
    public boolean shouldRender(AbstractWeaponBlockEntity be, Vec3 cameraPos) {
        // 自己写距离判断，比如 256 格以内都渲染
        return true;
    }
}
