package com.kodu16.vsie.content.storage.ammobox;

import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
@SuppressWarnings("removal")
public class AmmoBoxRefillMarkerRenderer {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int MARKER_LIGHT = LightTexture.FULL_BRIGHT;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MAX_MARKER_DISTANCE = 256;
    private static final Map<BlockPos, RefillMarker> MARKERS = new HashMap<>();

    public static void showMarker(BlockPos ammoBoxPos, ItemStack ammoStack, int amount,
                                  int targetIndex, String targetDisplayName, int durationTicks) {
        if (MC.level == null || ammoStack.isEmpty()) {
            return;
        }
        long expiresAt = MC.level.getGameTime() + Math.max(1, durationTicks);
        // Function: each ammo box shows only its newest refill result for one refill interval.
        MARKERS.put(ammoBoxPos.immutable(), new RefillMarker(ammoStack.copy(), amount, targetIndex, targetDisplayName, expiresAt));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || MC.options.hideGui || MC.level == null) {
            return;
        }

        long gameTime = MC.level.getGameTime();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = MC.renderBuffers().bufferSource();

        beginMarkerRender();
        try {
            Iterator<Map.Entry<BlockPos, RefillMarker>> iterator = MARKERS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, RefillMarker> entry = iterator.next();
                if (entry.getValue().expiresAt <= gameTime) {
                    iterator.remove();
                    continue;
                }
                renderMarker(event.getPoseStack(), buffers, cameraPos, entry.getKey(), entry.getValue());
            }
            buffers.endBatch();
        } finally {
            endMarkerRender();
        }
    }

    private static void renderMarker(PoseStack pose, MultiBufferSource buffer, Vec3 cameraPos,
                                     BlockPos ammoBoxPos, RefillMarker marker) {
        Vec3 targetPos = resolveMarkerPosition(ammoBoxPos);
        if (targetPos == null || targetPos.distanceToSqr(cameraPos) > (double) MAX_MARKER_DISTANCE * MAX_MARKER_DISTANCE) {
            return;
        }

        EntityRenderDispatcher dispatcher = MC.getEntityRenderDispatcher();
        renderIcon(cameraPos, targetPos, dispatcher, pose, buffer, marker.ammoStack);
        renderText(cameraPos, targetPos, dispatcher, MC.font, pose, buffer,
                Component.literal("x" + marker.amount).withStyle(ChatFormatting.WHITE), 0.003F, 14.0F);
        renderText(cameraPos, targetPos, dispatcher, MC.font, pose, buffer,
                Component.literal("#" + marker.targetIndex + " " + marker.targetDisplayName).withStyle(ChatFormatting.AQUA),
                0.003F, 28.0F);
    }

    private static Vec3 resolveMarkerPosition(BlockPos ammoBoxPos) {
        if (MC.level == null) {
            return null;
        }
        Vec3 localPos = Vec3.atCenterOf(ammoBoxPos).add(0.0D, 1.0D, 0.0D);
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(MC.level, ammoBoxPos);
        if (subLevel instanceof ClientSubLevel clientSubLevel) {
            return clientSubLevel.renderPose().transformPosition(localPos);
        }
        return subLevel == null ? localPos : subLevel.logicalPose().transformPosition(localPos);
    }

    private static void renderIcon(Vec3 cameraPos, Vec3 targetPos, EntityRenderDispatcher dispatcher,
                                   PoseStack pose, MultiBufferSource buffer, ItemStack item) {
        ItemRenderer itemRenderer = MC.getItemRenderer();
        double distance = cameraPos.distanceTo(targetPos);
        float scale = 0.1F * (float) (distance * 0.05D);
        Vec3 offset = targetPos.subtract(cameraPos);
        Quaternionf cameraRotation = dispatcher.cameraOrientation();

        pose.pushPose();
        try {
            pose.translate(offset.x, offset.y, offset.z);
            pose.mulPose(cameraRotation);
            pose.scale(scale * 20.0F, scale * 20.0F, scale * 20.0F);
            itemRenderer.renderStatic(
                    item,
                    ItemDisplayContext.FIXED,
                    MARKER_LIGHT,
                    OverlayTexture.NO_OVERLAY,
                    pose,
                    buffer,
                    MC.level,
                    0
            );
        } finally {
            pose.popPose();
        }
    }

    private static void renderText(Vec3 cameraPos, Vec3 targetPos, EntityRenderDispatcher dispatcher,
                                   Font font, PoseStack pose, MultiBufferSource buffer,
                                   Component text, float scalePerBlock, float yOffset) {
        Quaternionf cameraRotation = dispatcher.cameraOrientation();
        float scale = scalePerBlock * (float) cameraPos.distanceTo(targetPos);
        float textWidth = font.width(text);

        pose.pushPose();
        try {
            pose.translate(targetPos.x - cameraPos.x, targetPos.y - cameraPos.y, targetPos.z - cameraPos.z);
            pose.mulPose(cameraRotation);
            pose.scale(scale, -scale, -scale);
            pose.translate(0.0F, yOffset, 0.0F);
            Matrix4f matrix = pose.last().pose();
            font.drawInBatch(text, -textWidth / 2.0F, 0.0F, TEXT_COLOR, false, matrix, buffer,
                    Font.DisplayMode.SEE_THROUGH, 0, MARKER_LIGHT);
        } finally {
            pose.popPose();
        }
    }

    private static void beginMarkerRender() {
        // Function: refill markers are HUD hints and should remain visible through nearby blocks.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static void endMarkerRender() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private record RefillMarker(ItemStack ammoStack, int amount, int targetIndex, String targetDisplayName, long expiresAt) {
    }
}
