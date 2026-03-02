package com.kodu16.vsie.content.screen;

import com.kodu16.vsie.registries.vsieItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LevelLightEngine;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class AbstractScreenRenderLayer extends GeoRenderLayer<AbstractScreenBlockEntity> {

    public AbstractScreenRenderLayer(GeoRenderer<AbstractScreenBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    private static final String NOZZLE_BONE_NAME = "screen";
    private static Minecraft mc = Minecraft.getInstance();
    ItemRenderer itemRenderer = mc.getItemRenderer();
    Font font = mc.font;

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

        ItemStack stack = animatable.getRenderStack();
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        // 旋转以平躺于表面（针对顶部面）
        poseStack.mulPose(Axis.XP.rotationDegrees(270.0f));  // 对于其他面，使用 Axis.YP 等旋转

        poseStack.scale(0.9f, 0.9f, 0.9f);
        poseStack.translate(0, 0, 0.01f);  // 调整为目标面，例如 NORTH: translate(0.5, 0.5, 1.0)
        itemRenderer.renderStatic(new ItemStack(vsieItems.SCREEN_BG), ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
                level, 0);
        poseStack.translate(0, 0, 0.16f);  // 调整为目标面，例如 NORTH: translate(0.5, 0.5, 1.0)
        // 缩放物品（0.35 为合适大小）
        poseStack.scale(0.15f, 0.15f, 0.15f);
        // 渲染图案（物品）
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
                level, 0);

        poseStack.scale(0.1f, -0.1f, 0.1f);  // 缩放文字，负 Y 翻转正向
        float textWidth = font.width(stack.getCount() + "");
        poseStack.translate(-textWidth / 2, 0, 2);  // 居中并偏移上方
        font.drawInBatch(stack.getCount() + "", 0, 0, 0xECECEC, false,
                poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0,
                LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }
}
