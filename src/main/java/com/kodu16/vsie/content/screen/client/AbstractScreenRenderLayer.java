package com.kodu16.vsie.content.screen.client;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.screen.AbstractScreenBlock;
import com.kodu16.vsie.content.screen.client.functions.Radar;
import com.kodu16.vsie.content.screen.client.functions.ServerInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class AbstractScreenRenderLayer extends GeoRenderLayer<AbstractScreenBlockEntity> {
    private static final float HUD_SURFACE_OFFSET = -0.065F;

    public AbstractScreenRenderLayer(GeoRenderer<AbstractScreenBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    private static final String NOZZLE_BONE_NAME = "screen";

    private static final Minecraft mc = Minecraft.getInstance();
    private final Font font = mc.font;

    @Override
    public void render(PoseStack poseStack, AbstractScreenBlockEntity animatable,
                       software.bernie.geckolib.cache.object.BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public void renderForBone(PoseStack poseStack, AbstractScreenBlockEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (!NOZZLE_BONE_NAME.equals(bone.getName())) {
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay);
            return;
        }
        Level level = animatable.getLevel();
        if (level == null) return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-270.0f));
        // Function: orient the HUD upright on the screen bone without changing its anchor point.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        // Function: float the backgroundless HUD slightly in front of the physical screen to avoid surface overlap.
        poseStack.translate(0, 0, HUD_SURFACE_OFFSET);
        beginScreenContentRender();
        try {
            if (animatable.displaytype == 0) {
                Radar.renderRadar(poseStack, animatable, bufferSource, font);
            } else if (animatable.displaytype == 1) {
                poseStack.scale(0.005f, 0.005f, 0.005f);
                ServerInfo.renderServerInfo(poseStack, animatable, bufferSource, font);
            }
            // Function: flush all screen text inside the same render-state scope used by the control-seat HUD.
            if (bufferSource instanceof MultiBufferSource.BufferSource source) {
                source.endBatch();
            }
        } finally {
            endScreenContentRender();
        }
        poseStack.popPose();
    }

    // Function: reject the rear side and screens hidden by another block before using HUD-style rendering.
    private static boolean isVisibleFromScreenFront(AbstractScreenBlockEntity screen, Level level) {
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null || !screen.getBlockState().hasProperty(AbstractScreenBlock.FACING)) {
            return false;
        }

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 screenCenter = Vec3.atCenterOf(screen.getBlockPos());
        Direction facing = screen.getBlockState().getValue(AbstractScreenBlock.FACING);
        Vec3 cameraOffset = cameraPos.subtract(screenCenter);
        double frontSide = cameraOffset.x * facing.getStepX()
                + cameraOffset.y * facing.getStepY()
                + cameraOffset.z * facing.getStepZ();
        if (frontSide <= 0.0D) {
            return false;
        }

        BlockHitResult hit = level.clip(new ClipContext(
                cameraPos,
                screenCenter,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                cameraEntity
        ));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(screen.getBlockPos());
    }

    private static void beginScreenContentRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static void endScreenContentRender() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

}
