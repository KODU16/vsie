package com.kodu16.vsie.content.weapon.missile_launcher.client;

import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.client.AbstractWeaponModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class VerticleLaunchingSlotGeoRenderer extends AlwaysRenderGeoBlockRenderer<AbstractWeaponBlockEntity> {
    public VerticleLaunchingSlotGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractWeaponModel());
    }


    @Override
    public boolean shouldRenderOffScreen(AbstractWeaponBlockEntity be) {
        return true;   // 或者 return distanceSq < 某个超大值 的平方
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case UP -> {
                poseStack.translate(0, 0, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }
        }
    }

    @Override
    public boolean shouldRender(AbstractWeaponBlockEntity be, Vec3 cameraPos) {
        // 自己写距离判断，比如 256 格以内都渲染
        return true;
    }
}
