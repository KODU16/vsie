package com.kodu16.vsie.content.controlseat.functions;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
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

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

@EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
@SuppressWarnings("removal")
public class WorldMarkerPainter {
    public static final Minecraft mc = Minecraft.getInstance();
    private static final String UNNAMED_SUBLEVEL = "[Unnamed Sublevel]";
    private static final int HUD_MARKER_LIGHT = LightTexture.FULL_BRIGHT;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MAX_MARKER_DISTANCE = 4096;
    private static final float MARKER_POSITION_RESPONSE_PER_SECOND = 14.0F;
    private static final double MARKER_SMOOTHING_SNAP_DISTANCE = 256.0D;
    private static final Map<String, Vec3> smoothedMarkerPositions = new HashMap<>();
    private static long lastMarkerRenderTimeNanos = -1L;

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

        getRenderpos(event.getCamera().getPosition());
        if (shipsData.isEmpty() || playerpos == null) {
            clearMarkerSmoothing();
            return;
        }
        beginHudMarkerRender();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float markerAlpha = computeSmoothingAlpha(computeFrameDeltaSeconds(), MARKER_POSITION_RESPONSE_PER_SECOND);
        Set<String> activeMarkerKeys = new HashSet<>();

        try {
            for (var entry : shipsData.entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?> rawAttr)) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> attr = (Map<String, Object>) rawAttr;
                String markerKey = markerKey(entry.getKey(), attr);
                activeMarkerKeys.add(markerKey);
                renderShipMarker(event.getPoseStack(), buffers, entry.getKey(), attr, markerKey, markerAlpha);
            }
            smoothedMarkerPositions.keySet().removeIf(key -> !activeMarkerKeys.contains(key));

            // Function: flush marker draw calls while depth testing is disabled so blocks cannot hide them.
            buffers.endBatch();
        } finally {
            endHudMarkerRender();
        }
    }

    private static void renderShipMarker(PoseStack pose, MultiBufferSource buffer, String entryKey, Map<String, Object> attr,
                                         String markerKey, float markerAlpha) {
        Vec3 rawTarget = new Vec3(toDouble(attr.get("x")), toDouble(attr.get("y")), toDouble(attr.get("z")));
        Vec3 renderedTarget = resolveRenderedSubLevelCenter(entryKey);
        boolean usesLiveSubLevelPose = renderedTarget != null;
        rawTarget = usesLiveSubLevelPose ? renderedTarget : rawTarget;
        Vec3 delta = rawTarget.subtract(playerpos);
        if (delta.lengthSqr() > (double) MAX_MARKER_DISTANCE * MAX_MARKER_DISTANCE) {
            return;
        }
        Vec3 target = usesLiveSubLevelPose ? rawTarget : smoothMarkerPosition(markerKey, rawTarget, markerAlpha);

        String rawSlug = stringValue(attr.get("slug"));
        String displayName = displayName(rawSlug);
        int targetType = getPriority(enemy, ally, rawSlug);
        int targetIndex = toInt(attr.get("targetIndex"));
        double distance = Vec.Distance(playerpos, target);
        double speed = toDouble(attr.get("speed"));

        ItemStack item = markerItem(targetType);
        ChatFormatting nameColor = markerNameColor(targetType);
        String indexedName = targetIndex > 0 ? "[" + targetIndex + "] " + displayName : displayName;
        Component nameText = Component.literal(indexedName).withStyle(nameColor);
        Component statusText = Component.literal(String.format(Locale.ROOT, "距离 %.1f m  速度 %.1f m/s", distance, speed))
                .withStyle(ChatFormatting.WHITE);

        // Function: locked targets keep a dedicated TGT line above the ship name.
        if (rawSlug.equals(lockedenemyslug) && !rawSlug.isEmpty()) {
            renderText(playerpos, target, mc.getEntityRenderDispatcher(), mc.font, pose, buffer,
                    Component.literal("TGT").withStyle(ChatFormatting.RED), 0.003f, 6.0f);
        }
        // Function: ship markers use a fixed two-line layout so name and status do not crowd into one unreadable row.
        renderText(playerpos, target, mc.getEntityRenderDispatcher(), mc.font, pose, buffer, nameText, 0.003f, 18.0f);
        renderText(playerpos, target, mc.getEntityRenderDispatcher(), mc.font, pose, buffer, statusText, 0.003f, 30.0f);
        if (rawSlug.equals(lockedenemyslug) && !rawSlug.isEmpty()) {
            item = new ItemStack(vsieItems.TARGET_FRAME_ENEMY_LOCKED.get());
        }
        renderIcon(playerpos, target, mc.getEntityRenderDispatcher(), pose, buffer, item);
    }

    private static void getRenderpos(Vec3 cameraPos) {
        var level = mc.level;
        if (level == null) {
            return;
        }

        if (!(mc.getCameraEntity() instanceof Player player)) {
            shipsData = new HashMap<>();
            enemy = "";
            ally = "";
            lockedenemyslug = "";
            playerpos = null;
            clearMarkerSmoothing();
            return;
        }

        ControlSeatClientData data = ClientDataManager.getClientData(player);
        if (!(player.getVehicle() instanceof ControlSeatMountEntity)) {
            // Function: leaving the control seat must clear stale ship scan markers from the previous seated session.
            data.shipsData = new HashMap<>();
            data.lockedenemyslug = "";
            shipsData = new HashMap<>();
            enemy = "";
            ally = "";
            lockedenemyslug = "";
            playerpos = null;
            clearMarkerSmoothing();
            return;
        }
        shipsData = data.shipsData;
        enemy = data.enemy;
        ally = data.ally;
        lockedenemyslug = data.lockedenemyslug;
        // Function: use the render camera position because it is already partial-tick interpolated.
        playerpos = cameraPos;
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
            // Function: keep item markers non-degenerate; near-zero Z scale flickers while sublevels move.
            pose.scale(finalScale * 20, finalScale * 20, finalScale * 20);
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
            // Function: see-through text keeps marker labels visible through blocks.
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
        // Function: world markers are HUD hints and must not be occluded by level geometry.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static void endHudMarkerRender() {
        // Function: restore normal world rendering state after the marker batch is submitted.
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
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

    private static String markerKey(String entryKey, Map<String, Object> attr) {
        // Function: keep smoothing state stable for each marker across render frames.
        String slug = stringValue(attr.get("slug"));
        int targetIndex = toInt(attr.get("targetIndex"));
        return entryKey + "|" + slug + "|" + targetIndex;
    }

    private static Vec3 smoothMarkerPosition(String markerKey, Vec3 target, float alpha) {
        Vec3 current = smoothedMarkerPositions.get(markerKey);
        if (current == null || current.distanceTo(target) > MARKER_SMOOTHING_SNAP_DISTANCE) {
            smoothedMarkerPositions.put(markerKey, target);
            return target;
        }

        // Function: smooth server-updated marker positions so text and icons do not stutter between packets.
        Vec3 smoothed = current.lerp(target, alpha);
        smoothedMarkerPositions.put(markerKey, smoothed);
        return smoothed;
    }

    private static Vec3 resolveRenderedSubLevelCenter(String entryKey) {
        ClientLevel level = mc.level;
        if (level == null) {
            return null;
        }

        UUID subLevelId = parseUuid(entryKey);
        if (subLevelId == null) {
            return null;
        }

        ClientSubLevelContainer container = ClientSubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }

        for (ClientSubLevel subLevel : container.getAllSubLevels()) {
            if (!subLevelId.equals(subLevel.getUniqueId())) {
                continue;
            }

            BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
            if (bounds == null || bounds.volume() <= 0) {
                return null;
            }

            Vec3 localCenter = new Vec3(
                    (bounds.minX() + bounds.maxX() + 1.0D) * 0.5D,
                    (bounds.minY() + bounds.maxY() + 1.0D) * 0.5D,
                    (bounds.minZ() + bounds.maxZ() + 1.0D) * 0.5D
            );
            // Function: marker icons must use the same partial-tick render pose as Sable's visible ship model.
            return subLevel.renderPose().transformPosition(localCenter);
        }
        return null;
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void clearMarkerSmoothing() {
        // Function: reset visual interpolation when markers disappear or the player leaves the seat.
        smoothedMarkerPositions.clear();
        lastMarkerRenderTimeNanos = -1L;
    }

    private static float computeFrameDeltaSeconds() {
        // Function: use real frame time so smoothing remains consistent across FPS changes.
        long now = System.nanoTime();
        if (lastMarkerRenderTimeNanos < 0L) {
            lastMarkerRenderTimeNanos = now;
            return 1f / 60f;
        }
        long deltaNanos = now - lastMarkerRenderTimeNanos;
        lastMarkerRenderTimeNanos = now;
        return Math.max(1f / 240f, Math.min(deltaNanos / 1_000_000_000f, 1f / 15f));
    }

    private static float computeSmoothingAlpha(float deltaSeconds, float responsePerSecond) {
        // Function: convert response speed into an exponential smoothing weight.
        return Math.max(0f, Math.min(1f, 1f - (float) Math.exp(-responsePerSecond * deltaSeconds)));
    }
}
