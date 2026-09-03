package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceAnchor;
import com.kodu16.vsie.content.aeroie_custom.CustomTurretBlockEntity;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceGeoModel;
import com.kodu16.vsie.content.aeroie_custom.CustomTurretRuntimePose;
import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.client.TurretMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Runs the passive custom mesh skeleton inside VSIE's GeckoLib block-entity render path. */
public final class CustomTurretGeoRenderer extends AlwaysRenderGeoBlockRenderer<CustomTurretBlockEntity> {
    public CustomTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new CustomDeviceGeoModel<>());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        TurretMountTransform.apply(facing, poseStack);
    }

    @Override
    public void render(CustomTurretBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        // Function: reproduce GeckoLib's centered pre-render transform before drawing the external OBJ skeleton.
        poseStack.translate(0.5F, 0.0F, 0.5F);
        rotateBlock(blockEntity.getBlockState().getValue(AbstractTurretBlock.FACING), poseStack);
        float[] anchorOffset = CustomDeviceAnchor.rootPivotOffset(blockEntity.getDefinition());
        // Function: remove the center already applied above, then anchor the authored root pivot at the mount origin.
        poseStack.translate(anchorOffset[0] - 0.5F, anchorOffset[1], anchorOffset[2] - 0.5F);
        Float pitch = blockEntity.getAnimData(AbstractTurretBlockEntity.XROT);
        Float yaw = blockEntity.getAnimData(AbstractTurretBlockEntity.YROT);
        float targetPitch = pitch == null ? blockEntity.xRot0 : pitch;
        float targetYaw = yaw == null ? blockEntity.yRot0 : yaw;
        // Function: smooth each synchronized server step locally, matching built-in Gecko turret bone motion.
        blockEntity.prevxrot = CustomTurretRuntimePose.smoothAngle(blockEntity.prevxrot, targetPitch);
        blockEntity.prevyrot = CustomTurretRuntimePose.smoothAngle(blockEntity.prevyrot, targetYaw);
        CustomDeviceMeshRenderer.render(blockEntity.getDefinition(), blockEntity, poseStack, bufferSource, packedLight,
                blockEntity.prevxrot, blockEntity.prevyrot);
        poseStack.popPose();
    }
}
