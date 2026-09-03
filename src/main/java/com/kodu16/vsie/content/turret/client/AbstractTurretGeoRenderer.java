package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class AbstractTurretGeoRenderer extends AlwaysRenderGeoBlockRenderer<AbstractTurretBlockEntity> {

    public AbstractTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractTurretModel());
        // Function: keep turret effect layers attached after the base distance culling change.
        this.addRenderLayer(new TurretFirePointLayer(this));
        this.addRenderLayer(new TurretLaserLayer<>(this));
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        TurretMountTransform.apply(facing, poseStack);
    }

    @Override
    public boolean shouldRenderOffScreen(AbstractTurretBlockEntity be) {
        return super.shouldRenderOffScreen(be);
    }

    @Override
    public boolean shouldRender(AbstractTurretBlockEntity be, Vec3 cameraPos) {
        return super.shouldRender(be, cameraPos);
    }
}
