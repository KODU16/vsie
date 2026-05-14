package com.kodu16.vsie.content.controlseat.functions;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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
import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
@SuppressWarnings("removal")
public class WorldMarkerPainter {
    public static final Minecraft mc = Minecraft.getInstance();
    private static final String UNNAMED_SUBLEVEL = "[Unnamed Sublevel]";
    private static final int HUD_MARKER_LIGHT = LightTexture.FULL_BRIGHT;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MAX_MARKER_DISTANCE = 4096;

    public static Map<String, Object> shipsData = new HashMap<>();
    public static String enemy = "";
    public static String ally = "";
    public static String lockedenemyslug = "";
    public static Vec3 playerpos = null;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }


        getRenderpos();
        if (shipsData.isEmpty() || playerpos == null) {
            return;
        }
        beginHudMarkerRender();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        try {
            for (var entry : shipsData.entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?> rawAttr)) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> attr = (Map<String, Object>) rawAttr;
                renderShipMarker(event.getPoseStack(), buffers, attr);
            }

            // 关键：必须在 depth test 仍然关闭时提交
            buffers.endBatch();
        } finally {

        }
        endHudMarkerRender();
    }

    private static void renderShipMarker(PoseStack pose, MultiBufferSource buffer, Map<String, Object> attr) {
        Vec3 target = new Vec3(toDouble(attr.get("x")), toDouble(attr.get("y")), toDouble(attr.get("z")));
        Vec3 delta = target.subtract(playerpos);
        if (delta.lengthSqr() > (double) MAX_MARKER_DISTANCE * MAX_MARKER_DISTANCE) {
            return;
        }

        String rawSlug = stringValue(attr.get("slug"));
        String displayName = displayName(rawSlug);
        int targetType = getPriority(enemy, ally, rawSlug);
        int targetIndex = toInt(attr.get("targetIndex"));
        double distance = Vec.Distance(playerpos, target);

        ItemStack item = markerItem(targetType);
        ChatFormatting nameColor = markerNameColor(targetType);
        String indexedName = targetIndex > 0 ? "[" + targetIndex + "] " + displayName : displayName;
        Component text = Component.literal(indexedName)
                .withStyle(nameColor)
                .append(Component.literal(" " + String.format("%.1f m", distance)).withStyle(ChatFormatting.WHITE));

        renderText(playerpos, target, mc.getEntityRenderDispatcher(), mc.font, pose, buffer, text, 0.003f, 18.0f);
        if (rawSlug.equals(lockedenemyslug) && !rawSlug.isEmpty()) {
            item = new ItemStack(vsieItems.TARGET_FRAME_ENEMY_LOCKED.get());
            renderText(playerpos, target, mc.getEntityRenderDispatcher(), mc.font, pose, buffer,
                    Component.literal("TGT").withStyle(ChatFormatting.RED), 0.005f, -18.0f);
        }
        renderIcon(playerpos, target, mc.getEntityRenderDispatcher(), pose, buffer, item);
    }

    private static void getRenderpos() {
        var level = mc.level;
        if (level == null) {
            return;
        }

        if (!(mc.getCameraEntity() instanceof Player player)) {
            shipsData = new HashMap<>();
            enemy = "";
            ally = "";
            playerpos = null;
            return;
        }

        ControlSeatClientData data = ClientDataManager.getClientData(player);
        shipsData = data.shipsData;
        enemy = data.enemy;
        ally = data.ally;
        lockedenemyslug = data.lockedenemyslug;
        playerpos = player.getEyePosition();
    }

    private static void renderIcon(Vec3 camPos, Vec3 targetPos, EntityRenderDispatcher dispatcher,
                                   PoseStack pose, MultiBufferSource buffer, ItemStack item) {
        ItemRenderer itemRenderer = mc.getItemRenderer();
        double dist = camPos.distanceTo(targetPos);
        float finalScale = 0.1f * (float) (dist * 0.05);
        Vec3 offset = targetPos.subtract(camPos);
        Quaternionf cameraRot = dispatcher.cameraOrientation();

        pose.pushPose();
        try {
            pose.translate(offset.x, offset.y, offset.z);
            pose.mulPose(cameraRot);
            pose.scale(finalScale * 20, finalScale * 20, finalScale * 0.0001f);
            itemRenderer.renderStatic(
                    item,
                    ItemDisplayContext.FIXED,
                    HUD_MARKER_LIGHT,
                    OverlayTexture.NO_OVERLAY,
                    pose,
                    buffer,
                    mc.level,
                    0
            );
        } finally {
            pose.popPose();
        }
    }

    private static void renderText(Vec3 camPos, Vec3 targetPos, EntityRenderDispatcher dispatcher,
                                   Font font, PoseStack pose, MultiBufferSource buffer,
                                   Component text, float scalePerBlock, float yOffset) {
        Quaternionf cameraRot = dispatcher.cameraOrientation();
        float scale = scalePerBlock * (float) camPos.distanceTo(targetPos);
        float textWidth = font.width(text);

        pose.pushPose();
        try {
            pose.translate(targetPos.x - camPos.x, targetPos.y - camPos.y, targetPos.z - camPos.z);
            pose.mulPose(cameraRot);
            pose.scale(scale, -scale, -scale);
            pose.translate(0, yOffset, 0);
            Matrix4f matrix4f = pose.last().pose();
            // 功能：文字使用全亮参数并在关闭深度测试期间提交，保证名称和距离始终可见。
            font.drawInBatch(
                    text,
                    -textWidth / 2f,
                    0,
                    TEXT_COLOR,
                    false,
                    matrix4f,
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    HUD_MARKER_LIGHT
            );
        } finally {
            pose.popPose();
        }
    }

    private static ItemStack markerItem(int targetType) {
        if (targetType == 1) {
            return new ItemStack(vsieItems.TARGET_FRAME_ENEMY.get());
        }
        if (targetType == 2) {
            return new ItemStack(vsieItems.TARGET_FRAME_ALLY.get());
        }
        return new ItemStack(vsieItems.TARGET_FRAME.get());
    }

    private static ChatFormatting markerNameColor(int targetType) {
        if (targetType == 1) {
            return ChatFormatting.RED;
        }
        if (targetType == 2) {
            return ChatFormatting.GREEN;
        }
        return ChatFormatting.AQUA;
    }

    private static void beginHudMarkerRender() {
        /*RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();*/
    }

    private static void endHudMarkerRender() {
        /*RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();*/
    }

    public static int getPriority(String a, String b, String c) {
        if (c == null || c.isEmpty()) {
            return 0;
        }

        boolean hasEnemy = a != null && !a.isEmpty();
        boolean hasAlly = b != null && !b.isEmpty();
        int posA = hasEnemy ? c.indexOf(a) : -1;
        int posB = hasAlly ? c.indexOf(b) : -1;

        if (posA == -1 && posB == -1) {
            return 0;
        }
        if (posA != -1 && posB == -1) {
            return 1;
        }
        if (posA == -1) {
            return 2;
        }
        return posA < posB ? 1 : 2;
    }

    private static String displayName(String rawSlug) {
        return rawSlug == null || rawSlug.isBlank() ? UNNAMED_SUBLEVEL : rawSlug;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }
}
