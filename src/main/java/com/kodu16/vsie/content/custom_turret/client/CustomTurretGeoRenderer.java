package com.kodu16.vsie.content.custom_turret.client;

import com.kodu16.vsie.content.custom_turret.CustomTurretAnchor;
import com.kodu16.vsie.content.custom_turret.CustomTurretBlockEntity;
import com.kodu16.vsie.content.custom_turret.CustomTurretGeoModel;
import com.kodu16.vsie.content.custom_turret.CustomTurretRuntimePose;
import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Runs the passive custom mesh skeleton inside VSIE's GeckoLib block-entity render path. */
public final class CustomTurretGeoRenderer extends AlwaysRenderGeoBlockRenderer<CustomTurretBlockEntity> {
    public CustomTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new CustomTurretGeoModel());
    }

    @Override
    public void render(CustomTurretBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        float[] anchorOffset = CustomTurretAnchor.rootPivotOffset(blockEntity.getDefinition());
        // Function: the placed block itself anchors the root pivot, without an extra vertical half-block lift.
        poseStack.translate(anchorOffset[0], anchorOffset[1], anchorOffset[2]);
        Float pitch = blockEntity.getAnimData(AbstractTurretBlockEntity.XROT);
        Float yaw = blockEntity.getAnimData(AbstractTurretBlockEntity.YROT);
        float targetPitch = pitch == null ? blockEntity.xRot0 : pitch;
        float targetYaw = yaw == null ? blockEntity.yRot0 : yaw;
        // Function: smooth each synchronized server step locally, matching built-in Gecko turret bone motion.
        blockEntity.prevxrot = CustomTurretRuntimePose.smoothAngle(blockEntity.prevxrot, targetPitch);
        blockEntity.prevyrot = CustomTurretRuntimePose.smoothAngle(blockEntity.prevyrot, targetYaw);
        CustomTurretMeshRenderer.render(blockEntity.getDefinition(), blockEntity, poseStack, bufferSource, packedLight,
                blockEntity.prevxrot, blockEntity.prevyrot);
        poseStack.popPose();
    }
}
