package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceBlockEntity;
import com.kodu16.vsie.content.aeroie_custom.CustomThrusterBlockEntity;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceAnchor;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceGeoModel;
import com.kodu16.vsie.content.aeroie_custom.CustomWeaponBlockEntity;
import com.kodu16.vsie.content.aeroie_custom.CustomDecorationBlock;
import com.kodu16.vsie.content.thruster.AbstractThrusterBlock;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlock;
import com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Renders non-turret custom device OBJ skeletons through the shared custom definition. */
public final class CustomDeviceGeoRenderer<T extends BlockEntity & software.bernie.geckolib.animatable.GeoAnimatable
        & CustomDeviceBlockEntity>
        extends AlwaysRenderGeoBlockRenderer<T> {
    public CustomDeviceGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new CustomDeviceGeoModel<>());
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        applyFacing(blockEntity.getBlockState(), poseStack);
        float[] anchorOffset = CustomDeviceAnchor.rootPivotOffset(blockEntity.getDefinition());
        poseStack.translate(anchorOffset[0] - 0.5F, anchorOffset[1], anchorOffset[2] - 0.5F);
        if (blockEntity instanceof CustomThrusterBlockEntity thruster) {
            CustomDeviceMeshRenderer.render(blockEntity.getDefinition(), thruster, poseStack, bufferSource, packedLight);
        } else if (blockEntity instanceof CustomWeaponBlockEntity weapon) {
            CustomDeviceMeshRenderer.render(blockEntity.getDefinition(), weapon, poseStack, bufferSource, packedLight);
        } else {
            CustomDeviceMeshRenderer.render(blockEntity.getDefinition(), poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }

    private static void applyFacing(BlockState state, PoseStack poseStack) {
        if (!state.hasProperty(BlockStateProperties.FACING)) {
            return;
        }
        Direction facing = state.getValue(BlockStateProperties.FACING);
        if (state.getBlock() instanceof AbstractWeaponBlock || state.getBlock() instanceof CustomDecorationBlock) {
            switch (facing) {
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                default -> {
                }
            }
            return;
        }
        if (state.getBlock() instanceof AbstractThrusterBlock) {
            switch (facing) {
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            }
        }
    }
}
