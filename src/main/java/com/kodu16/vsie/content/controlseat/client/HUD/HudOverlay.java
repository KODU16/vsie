package com.kodu16.vsie.content.controlseat.client.HUD;

import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.controlseat.functions.ShipAnglePainter;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.kodu16.vsie.registries.vsieKeyMappings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class HudOverlay {
    private static final int TEXT_ALPHA = 10;

    public static final int MAIN_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0xFF, 0x99);
    public static final int SUB_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0x66, 0x33);
    private static final int WARP_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x33, 0xAA, 0xFF);
    private static final int WARP_WARNING_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0xFF, 0x22, 0x33);
    private static final int POWER_WARNING_COLOR = FastColor.ARGB32.color(0xC0, 0xFF, 0x44, 0x44);
    private static final int KEY_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0xFF, 0xFF, 0xFF);
    private static final int ASSIST_LOCK_COLOR = FastColor.ARGB32.color(0x90, 0xFF, 0x22, 0x33);
    private static final int TURRET_MARKER_COLOR = FastColor.ARGB32.color(0xE0, 0x00, 0xFF, 0x99);
    private static final int VELOCITY_MARKER_COLOR = FastColor.ARGB32.color(0xFF, 0xFF, 0xFF, 0xFF);
    private static final int MODE_BUTTON_WIDTH = 36;
    private static final int MODE_BUTTON_HEIGHT = 10;
    private static final float HUD_TEXT_SCALE = 0.7f;
    private static final float TURRET_MARKER_LABEL_SCALE = 0.7f;
    private static final float TURRET_MARKER_DISTANCE = 256.0f;
    private static final float VELOCITY_MARKER_DISTANCE = 256.0f;
    private static final int TURRET_MARKER_HALF_SIZE = 5;
    private static final int VELOCITY_MARKER_HALF_SIZE = 4;
    private static final int TURRET_MARKER_LABEL_OFFSET_X = 8;
    private static final int TURRET_MARKER_LABEL_OFFSET_Y = -4;
    private static final float SCREEN_MARKER_PADDING = 6.0f;

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Map<KeyMapping, CachedKeyText> KEY_TEXT_CACHE = new IdentityHashMap<>();
    private static long lastHudRenderTimeNanos = -1L;

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName()) && isControlSeatPassenger(mc.player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayEvent(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }
        // Function: control-seat HUD follows Minecraft's F1 HUD visibility toggle.
        if (mc.options.hideGui) {
            return;
        }
        Player player = mc.player;
        if (player == null || player.getVehicle() == null) {
            return;
        }
        if (!(player.getVehicle() instanceof ControlSeatMountEntity mountEntity)) {
            return;
        }

        BlockPos controlSeatPos = mountEntity.getBoundBlockPos();
        if (controlSeatPos == null || mc.level == null) {
            return;
        }

        BlockEntity blockEntity = mc.level.getBlockEntity(controlSeatPos);
        if (!(blockEntity instanceof ControlSeatBlockEntity controlSeat)) {
            return;
        }

        ControlSeatClientData data = ClientDataManager.getClientDataForSeat(player, controlSeatPos);
        if (data == null) {
            return;
        }
        GuiGraphics gg = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        float markerAlpha = computeSmoothingAlpha(computeFrameDeltaSeconds(), 18f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Function: heavy turret fire-vector markers stay on the screen overlay so they remain visible even when the player looks away from the seat HUD plane.
        drawHeavyTurretMarkers(gg, controlSeat, data, sw, sh, markerAlpha);
        drawVelocityVectorMarker(gg, data, sw, sh, markerAlpha);
        RenderSystem.disableBlend();
        return;
    }

    private static boolean isControlSeatPassenger(Player player) {
        return player != null && player.getVehicle() instanceof ControlSeatMountEntity;
    }

    private static void drawHeavyTurretMarkers(GuiGraphics gg, ControlSeatBlockEntity controlSeat, ControlSeatClientData data, int sw, int sh, float markerAlpha) {
        if (mc.level == null) {
            return;
        }

        List<BlockPos> retainedMarkers = new ArrayList<>();
        List<BlockPos> turretPositions = controlSeat.getLinkedTurretPositionsInOrder();
        for (int i = 0; i < turretPositions.size(); i++) {
            BlockPos turretPos = turretPositions.get(i);
            BlockEntity blockEntity = mc.level.getBlockEntity(turretPos);
            if (!(blockEntity instanceof AbstractHeavyTurretBlockEntity heavyTurret)) {
                continue;
            }
            if (!shouldShowHeavyTurretMarker(heavyTurret, data)) {
                continue;
            }

            retainedMarkers.add(turretPos);
            Vec3 origin = heavyTurret.getHudAimOriginWorld();
            Vec3 direction = heavyTurret.getRenderedBarrelDirectionWorld();
            if (direction == null || direction.lengthSqr() < 1.0E-6D) {
                continue;
            }

            double projectionDistance = Math.max(TURRET_MARKER_DISTANCE, heavyTurret.getTargetDistance());
            ScreenPoint projectedPoint = projectWorldToScreen(origin.add(direction.scale(projectionDistance)), sw, sh, true);
            if (projectedPoint == null) {
                continue;
            }

            ControlSeatClientData.TurretHudMarkerState markerState = data.getTurretHudMarkerState(turretPos);
            if (!markerState.initialized) {
                markerState.screenX = projectedPoint.x();
                markerState.screenY = projectedPoint.y();
                markerState.initialized = true;
            } else {
                markerState.screenX = smoothExp(markerState.screenX, projectedPoint.x(), markerAlpha);
                markerState.screenY = smoothExp(markerState.screenY, projectedPoint.y(), markerAlpha);
            }

            int markerX = Math.round(markerState.screenX);
            int markerY = Math.round(markerState.screenY);
            // Function: manual heavy turret cues need their own opaque color because the rest of the HUD is intentionally faint.
            drawPlusMarker(gg, markerX, markerY, TURRET_MARKER_HALF_SIZE, TURRET_MARKER_COLOR);
            drawLeftTextScaled(gg, "#" + (i + 1), markerX + TURRET_MARKER_LABEL_OFFSET_X, markerY + TURRET_MARKER_LABEL_OFFSET_Y, TURRET_MARKER_COLOR, TURRET_MARKER_LABEL_SCALE);
        }
        data.retainTurretHudMarkers(retainedMarkers);
    }

    private static void drawVelocityVectorMarker(GuiGraphics gg, ControlSeatClientData data, int sw, int sh, float markerAlpha) {
        Vector3d center = data.structureCenterWorld;
        Vector3d velocity = data.structureVelocityWorld;
        if (!isFiniteVector(center) || !isFiniteVector(velocity) || velocity.lengthSquared() <= 1.0E-6D) {
            return;
        }

        Vector3d endpoint = new Vector3d(velocity).normalize().mul(VELOCITY_MARKER_DISTANCE).add(center);
        ScreenPoint projectedPoint = projectWorldToScreen(new Vec3(endpoint.x, endpoint.y, endpoint.z), sw, sh);
        if (projectedPoint == null) {
            return;
        }

        drawPlusMarker(gg, Math.round(projectedPoint.x()), Math.round(projectedPoint.y()), VELOCITY_MARKER_HALF_SIZE, VELOCITY_MARKER_COLOR);
    }

    private static void drawPlusMarker(GuiGraphics gg, int x, int y, int halfSize, int color) {
        // Function: draw the velocity cue as two immediate quads so it does not ghost through font blending.
        gg.fill(x - halfSize, y, x + halfSize + 1, y + 1, color);
        gg.fill(x, y - halfSize, x + 1, y + halfSize + 1, color);
    }

    private static boolean shouldShowHeavyTurretMarker(AbstractHeavyTurretBlockEntity heavyTurret, ControlSeatClientData data) {
        if (!isTurretInAnyActiveSeatChannel(heavyTurret.getData(), data)) {
            return false;
        }
        int fireType = heavyTurret.getData().fireType;
        // Function: only manual mode and smart-mode's manual branch show the current fire vector marker.
        return fireType == 0 || (fireType == 2 && !data.isViewLocked());
    }

    private static boolean isTurretInAnyActiveSeatChannel(TurretData turretData, ControlSeatClientData data) {
        int activeSeatChannelMask = 0;
        if (data.channel1) activeSeatChannelMask |= turretData.CHANNEL_1;
        if (data.channel2) activeSeatChannelMask |= turretData.CHANNEL_2;
        if (data.channel3) activeSeatChannelMask |= turretData.CHANNEL_3;
        if (data.channel4) activeSeatChannelMask |= turretData.CHANNEL_4;
        boolean activeSeatChannelMatches = activeSeatChannelMask != 0 && (turretData.getChannelStatus() & activeSeatChannelMask) != 0;
        // Function: fall back to the turret-synced firing channel so HUD markers survive short seat channel packet delays.
        return activeSeatChannelMatches || (turretData.getChannelStatus() & turretData.channelOfCtrl) != 0;
    }

    private static ScreenPoint projectWorldToScreen(Vec3 worldPoint, int sw, int sh) {
        return projectWorldToScreen(worldPoint, sw, sh, false);
    }

    private static ScreenPoint projectWorldToScreen(Vec3 worldPoint, int sw, int sh, boolean clampToScreen) {
        if (mc.gameRenderer == null) {
            return null;
        }

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 relative = worldPoint.subtract(cameraPos);
        Quaternionf inverseCameraRotation = new Quaternionf(mc.getEntityRenderDispatcher().cameraOrientation()).conjugate();
        Vector3f cameraSpace = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);
        inverseCameraRotation.transform(cameraSpace);
        if (cameraSpace.z >= -0.05f) {
            return null;
        }

        float halfWidth = sw * 0.5f;
        float halfHeight = sh * 0.5f;
        float fovDegrees = mc.options.fov().get().floatValue();
        float focalLength = (float) (halfHeight / Math.tan(fovDegrees * 0.5f * Mth.DEG_TO_RAD));
        float screenX = halfWidth - cameraSpace.x * focalLength / cameraSpace.z;
        float screenY = halfHeight + cameraSpace.y * focalLength / cameraSpace.z;
        if (clampToScreen) {
            // Function: keep manual turret markers visible at the screen edge when the barrel aim point is just off-screen.
            return new ScreenPoint(
                    Mth.clamp(screenX, SCREEN_MARKER_PADDING, sw - SCREEN_MARKER_PADDING),
                    Mth.clamp(screenY, SCREEN_MARKER_PADDING, sh - SCREEN_MARKER_PADDING)
            );
        }
        if (screenX < 0.0f || screenX > sw || screenY < 0.0f || screenY > sh) {
            return null;
        }
        return new ScreenPoint(screenX, screenY);
    }

    private static boolean isFiniteVector(Vector3d vector) {
        return vector != null
                && Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
    }

    private static float ratio(int available, int total) {
        if (total <= 0) {
            return 0f;
        }
        return Mth.clamp((float) available / (float) total, 0f, 1f);
    }

    private static float computeFrameDeltaSeconds() {
        long now = System.nanoTime();
        if (lastHudRenderTimeNanos < 0L) {
            lastHudRenderTimeNanos = now;
            return 1f / 60f;
        }
        long deltaNanos = now - lastHudRenderTimeNanos;
        lastHudRenderTimeNanos = now;
        return Mth.clamp(deltaNanos / 1_000_000_000f, 1f / 240f, 1f / 15f);
    }

    private static float computeSmoothingAlpha(float deltaSeconds, float responsePerSecond) {
        return Mth.clamp(1f - (float) Math.exp(-responsePerSecond * deltaSeconds), 0f, 1f);
    }

    private static float smoothExp(float current, float target, float alpha) {
        return Mth.lerp(alpha, current, target);
    }

    private static String formatSpeedText(double shipSpeed) {
        return "SPD " + formatFixed(shipSpeed, 1);
    }

    private static String formatCoordinatesText(Vector3d coordinates) {
        return "XYZ " + Math.round(coordinates.x) + " " + Math.round(coordinates.y) + " " + Math.round(coordinates.z);
    }

    private static String formatGForceText(double seatGForce) {
        return "G " + formatFixed(seatGForce, 2);
    }

    private static String formatFixed(double value, int decimals) {
        double clampedValue = Double.isFinite(value) ? value : 0.0D;
        long scale = decimals == 1 ? 10L : 100L;
        long rounded = Math.round(clampedValue * scale);
        boolean negative = rounded < 0;
        long abs = Math.abs(rounded);
        long whole = abs / scale;
        long fraction = abs % scale;
        String fractionText = decimals == 1
                ? Long.toString(fraction)
                : (fraction < 10 ? "0" + fraction : Long.toString(fraction));
        return (negative ? "-" : "") + whole + "." + fractionText;
    }

    public static void drawCenteredText(GuiGraphics gg, String text, int x, int y, int color) {
        drawCenteredTextScaled(gg, text, x, y, color, HUD_TEXT_SCALE);
    }

    private static void drawLeftText(GuiGraphics gg, String text, int x, int y, int color) {
        drawLeftTextScaled(gg, text, x, y, color, HUD_TEXT_SCALE);
    }

    private static void drawRightText(GuiGraphics gg, String text, int x, int y, int color) {
        int scaledWidth = Math.round(mc.font.width(text) * HUD_TEXT_SCALE);
        drawLeftTextScaled(gg, text, x - scaledWidth, y, color, HUD_TEXT_SCALE);
    }

    private static void drawCenteredTextScaled(GuiGraphics gg, String text, int x, int y, int color, float scale) {
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1);
        float inv = 1.0f / scale;
        // Function: control-seat HUD text should use the same drawString path as channel and weapon labels, while keeping centered layout.
        int scaledTextX = (int) ((x - (mc.font.width(text) * scale) / 2.0f) * inv);
        gg.drawString(mc.font, text, scaledTextX, (int) (y * inv), color, false);
        gg.pose().popPose();
    }

    private static void drawLeftTextScaled(GuiGraphics gg, String text, int x, int y, int color, float scale) {
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1);
        float inv = 1.0f / scale;
        gg.drawString(mc.font, text, (int) (x * inv), (int) (y * inv), color, false);
        gg.pose().popPose();
    }

    private static void drawSwitch(GuiGraphics gg, String label, int x, int y, boolean active, int recwidth, int recheight) {
        int color = active ? MAIN_COLOR : SUB_COLOR;
        drawCenteredText(gg, label, x, y, color);
        DrawShape.drawHollowRectangle(gg, x, y + 2, recwidth, recheight, 1, color);
    }

    private static void drawKeyedSwitch(GuiGraphics gg, String label, KeyMapping keyMapping, int x, int y, boolean active, boolean forcedDisabled, int recwidth, int recheight) {
        int color = forcedDisabled ? ASSIST_LOCK_COLOR : (active ? MAIN_COLOR : SUB_COLOR);
        int keyColor = forcedDisabled ? ASSIST_LOCK_COLOR : KEY_COLOR;
        CachedKeyText cachedKeyText = getCachedKeyText(keyMapping);
        String keyText = cachedKeyText.text();
        String separator = keyText.isEmpty() ? "" : " ";
        int keyWidth = cachedKeyText.width();
        int totalWidth = keyWidth + mc.font.width(separator + label);
        int leftX = Math.round(x - totalWidth * HUD_TEXT_SCALE / 2f);

        drawLeftText(gg, keyText, leftX, y, keyColor);
        drawLeftText(gg, separator + label, Math.round(leftX + keyWidth * HUD_TEXT_SCALE), y, color);
        DrawShape.drawHollowRectangle(gg, x, y + 2, recwidth, recheight, 1, color);
    }

    private static void drawWarpSwitch(GuiGraphics gg, ControlSeatClientData data, int x, int y) {
        boolean active = data.isWarpPreparing || data.hasPendingWarpTeleport;
        if (!active) {
            drawKeyedSwitch(gg, "Warp", vsieKeyMappings.KEY_START_WARP, x, y, false, false, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT);
            if (data.warpE710Insufficient) {
                drawCenteredText(gg, "NO E-710", x, y + 13, WARP_WARNING_COLOR);
            }
            return;
        }

        String label = data.hasPendingWarpTeleport ? "JUMP" : "ALIGN";
        drawCenteredText(gg, label, x, y, WARP_COLOR);
        DrawShape.drawHollowRectangle(gg, x, y + 2, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, 1, WARP_COLOR);
    }

    private static void drawActiveWeaponCooldowns(GuiGraphics gg, ControlSeatClientData data, int centerX, int centerY, float hudAlpha) {
        int rightStatusArcRight = centerX + centerX / 20 + 68;
        int startX = rightStatusArcRight + 10;
        int startY = centerY - 18;
        int lineHeight = 14;
        int barWidth = 52;
        int barHeight = 4;
        int textBarGap = 6;

        while (data.smoothWeaponCooldownRatios.size() < data.activeWeaponHudInfos.size()) {
            data.smoothWeaponCooldownRatios.add(0f);
        }
        while (data.smoothWeaponCooldownRatios.size() > data.activeWeaponHudInfos.size()) {
            data.smoothWeaponCooldownRatios.remove(data.smoothWeaponCooldownRatios.size() - 1);
        }

        for (int i = 0; i < data.activeWeaponHudInfos.size(); i++) {
            ActiveWeaponHudInfo info = data.activeWeaponHudInfos.get(i);
            int rowY = startY + i * lineHeight;
            int weaponStatusColor = info.fireReady ? MAIN_COLOR : WARP_WARNING_COLOR;
            drawLeftText(gg, info.displayName, startX, rowY, weaponStatusColor);

            int nameWidth = Math.round(mc.font.width(info.displayName) * HUD_TEXT_SCALE);
            int barCenterX = startX + nameWidth + textBarGap + barWidth / 2;
            int barCenterY = rowY + 1;
            int safeMaxCooldown = Math.max(1, info.maxCooldown);
            float targetProgress = Mth.clamp((float) Math.max(0, info.currentTick) / (float) safeMaxCooldown, 0f, 1f);
            float progress = smoothExp(data.smoothWeaponCooldownRatios.get(i), targetProgress, hudAlpha);
            data.smoothWeaponCooldownRatios.set(i, progress);
            float readyProgress = info.remainingCooldown ? 1.0f - progress : progress;

            DrawShape.drawHollowRectangle(gg, barCenterX, barCenterY, barWidth, barHeight + 2, 1, weaponStatusColor);

            int red = Mth.floor(Mth.lerp(readyProgress, 0xFF, 0x00));
            int green = Mth.floor(Mth.lerp(readyProgress, 0x33, 0xFF));
            int dynamicColor = FastColor.ARGB32.color(TEXT_ALPHA, red, green, 0x33);

            int fillWidth = Mth.floor((barWidth - 2) * progress);
            if (fillWidth > 0) {
                gg.fill(
                        barCenterX - barWidth / 2 + 1,
                        barCenterY - barHeight / 2 + 1,
                        barCenterX - barWidth / 2 + 1 + fillWidth,
                        barCenterY + barHeight / 2,
                        dynamicColor
                );
            }
        }
    }

    private static CachedKeyText getCachedKeyText(KeyMapping keyMapping) {
        String resolvedText = keyMapping.getTranslatedKeyMessage().getString();
        CachedKeyText cached = KEY_TEXT_CACHE.get(keyMapping);
        if (cached != null && cached.text().equals(resolvedText)) {
            return cached;
        }

        CachedKeyText updated = new CachedKeyText(resolvedText, mc.font.width(resolvedText));
        KEY_TEXT_CACHE.put(keyMapping, updated);
        return updated;
    }

    private record ScreenPoint(float x, float y) {
    }

    private record CachedKeyText(String text, int width) {
    }
}
