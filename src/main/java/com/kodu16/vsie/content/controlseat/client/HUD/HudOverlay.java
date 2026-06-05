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
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private static final int MODE_BUTTON_WIDTH = 36;
    private static final int MODE_BUTTON_HEIGHT = 10;
    private static final float HUD_TEXT_SCALE = 0.7f;
    private static final float TURRET_MARKER_SCALE = 1.0f;
    private static final float TURRET_MARKER_LABEL_SCALE = 0.45f;
    private static final float TURRET_MARKER_DISTANCE = 256.0f;
    private static final int TURRET_MARKER_LABEL_OFFSET_X = 8;
    private static final int TURRET_MARKER_LABEL_OFFSET_Y = -4;

    private static final Minecraft mc = Minecraft.getInstance();
    private static long lastHudRenderTimeNanos = -1L;

    @SubscribeEvent
    public static void onRenderGuiOverlayEvent(RenderGuiLayerEvent.Post event) {
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

        ControlSeatClientData data = ClientDataManager.getClientData(player);
        GuiGraphics gg = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        DeltaTracker partialTick = event.getPartialTick();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int centerX = sw / 2;
        int centerY = sh / 2;
        int baseY = sh / 6;

        float energyRatio = ratio(data.energyavalible, data.energytotal);
        float fuelRatio = ratio(data.fuelavalible, data.fueltotal);
        float e710Ratio = ratio(data.e710avalible, data.fueltotal);
        boolean showWarpE710Bar = data.isWarpPreparing;
        float warpE710CostRatio = showWarpE710Bar ? ratio(data.warpE710CostMb, data.fueltotal) : 0f;
        float shieldRatio = ratio(data.shieldavalible, data.shieldtotal);
        data.throttleTargetRatio = Mth.clamp((data.throttle + 100f) / 200f, 0f, 1f);

        float frameDeltaSeconds = computeFrameDeltaSeconds();
        float hudAlpha = computeSmoothingAlpha(frameDeltaSeconds, 10f);
        float markerAlpha = computeSmoothingAlpha(frameDeltaSeconds, 18f);

        data.smoothEnergyRatio = smoothExp(data.smoothEnergyRatio, energyRatio, hudAlpha);
        data.smoothFuelRatio = smoothExp(data.smoothFuelRatio, fuelRatio, hudAlpha);
        data.smoothE710Ratio = smoothExp(data.smoothE710Ratio, e710Ratio, hudAlpha);
        data.smoothWarpE710CostRatio = smoothExp(data.smoothWarpE710CostRatio, warpE710CostRatio, hudAlpha);
        data.smoothShieldRatio = smoothExp(data.smoothShieldRatio, shieldRatio, hudAlpha);
        data.smoothThrottle = smoothExp(data.smoothThrottle, data.throttleTargetRatio, hudAlpha);
        int visualThrottle = Mth.floor(Mth.lerp(data.smoothThrottle, -100f, 100f));

        StatusIndicator.renderDecorative(
                gg,
                data.smoothEnergyRatio,
                data.smoothFuelRatio,
                data.smoothE710Ratio,
                data.smoothWarpE710CostRatio,
                showWarpE710Bar,
                data.smoothShieldRatio,
                visualThrottle,
                (int) data.accumulatedmousex,
                (int) data.accumulatedmousey
        );

        int throttleCenterX = centerX - (3 * centerX / 10);
        int throttleY = centerY + (centerY / 3);
        gg.drawCenteredString(mc.font, visualThrottle + "%", throttleCenterX, throttleY + 20, MAIN_COLOR);

        int switchBaseX = throttleCenterX;
        int switchY = throttleY + 22;
        int switchGapX = 52;
        int switchGapY = 18;
        int leftSwitchX = switchBaseX - switchGapX / 2;
        int rightSwitchX = switchBaseX + switchGapX / 2;
        drawKeyedSwitch(gg, "Shield", vsieKeyMappings.KEY_TOGGLE_SHIELD, leftSwitchX, switchY, data.shieldon, data.isShieldOverloaded, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT);
        drawKeyedSwitch(gg, "Force", vsieKeyMappings.KEY_TOGGLE_FORCE_ASSIST, rightSwitchX, switchY, data.isforceassiston, data.isForceAssistSuppressedByAccelerator, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT);
        drawKeyedSwitch(gg, "Torque", vsieKeyMappings.KEY_TOGGLE_TORQUE_ASSIST, leftSwitchX, switchY + switchGapY, data.istorqueassiston, false, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT);
        drawKeyedSwitch(gg, "AntiG", vsieKeyMappings.KEY_TOGGLE_ANTI_GRAVITY, rightSwitchX, switchY + switchGapY, data.isantigravityon, false, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT);
        drawWarpSwitch(gg, data, leftSwitchX, switchY + switchGapY * 2);
        drawKeyedSwitch(gg, "Level", vsieKeyMappings.KEY_TOGGLE_AUTO_LEVEL, rightSwitchX, switchY + switchGapY * 2, data.isAutoLevelOn, false, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT);

        int rightArcCenterX = centerX + (3 * centerX / 10);
        int rightInfoY = throttleY - 16;
        drawCenteredText(gg, String.format(Locale.ROOT, "SPD %.1f", data.shipSpeed), rightArcCenterX, rightInfoY, MAIN_COLOR);
        drawCenteredText(
                gg,
                String.format(Locale.ROOT, "XYZ %.0f %.0f %.0f",
                        data.structureCenterWorld.x,
                        data.structureCenterWorld.y,
                        data.structureCenterWorld.z),
                rightArcCenterX,
                rightInfoY + 10,
                MAIN_COLOR
        );
        int channelY = rightInfoY + 28;
        drawSwitch(gg, "1", rightArcCenterX - 24, channelY, data.channel1, 10, 10);
        drawSwitch(gg, "2", rightArcCenterX - 8, channelY, data.channel2, 10, 10);
        drawSwitch(gg, "3", rightArcCenterX + 8, channelY, data.channel3, 10, 10);
        drawSwitch(gg, "4", rightArcCenterX + 24, channelY, data.channel4, 10, 10);
        drawLeftText(gg, String.format(Locale.ROOT, "G %.2f", data.seatGForce), rightArcCenterX + 82, centerY - 4, MAIN_COLOR);

        drawActiveWeaponCooldowns(gg, data, centerX, centerY, hudAlpha);
        drawHeavyTurretMarkers(gg, controlSeat, data, sw, sh, markerAlpha);

        Vector3d interpolatedFacing = data.getInterpolatedShipFacing(partialTick);
        Vector3d interpolatedUp = data.getInterpolatedShipUp(partialTick);
        double[] angles = ShipAnglePainter.getDirectedAnglesToAxes(new Vec3(interpolatedFacing.x, interpolatedFacing.y, interpolatedFacing.z));
        double pitchDeg = ShipAnglePainter.getPitchDegrees(interpolatedFacing, interpolatedUp);
        ShipAnglePainter.drawAngleLine(gg, interpolatedFacing, centerX, baseY + 10, MAIN_COLOR);
        drawCenteredText(gg, "§l§b" + (int) angles[0], centerX, baseY + 5, MAIN_COLOR);

        int leftArcCenterX = centerX - centerX / 20;
        int pitchBarX = leftArcCenterX - 82;
        ShipAnglePainter.drawPitchLineCompact(gg, pitchDeg, pitchBarX, centerY, MAIN_COLOR);

        if (data.energyavalible <= 0) {
            drawCenteredText(gg, "NO BATTERY POWER", centerX, centerY + 52, POWER_WARNING_COLOR);
        }

        RenderSystem.disableBlend();
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
            ScreenPoint projectedPoint = projectWorldToScreen(origin.add(direction.scale(projectionDistance)), sw, sh);
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
            // Function: the fire-direction cue must be a literal plus sign at the projected hit point.
            drawCenteredTextScaled(gg, "+", markerX, markerY, MAIN_COLOR, TURRET_MARKER_SCALE);
            drawLeftTextScaled(gg, "#" + (i + 1), markerX + TURRET_MARKER_LABEL_OFFSET_X, markerY + TURRET_MARKER_LABEL_OFFSET_Y, MAIN_COLOR, TURRET_MARKER_LABEL_SCALE);
        }
        data.retainTurretHudMarkers(retainedMarkers);
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
        return activeSeatChannelMask != 0 && (turretData.getChannelStatus() & activeSeatChannelMask) != 0;
    }

    private static ScreenPoint projectWorldToScreen(Vec3 worldPoint, int sw, int sh) {
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
        if (screenX < 0.0f || screenX > sw || screenY < 0.0f || screenY > sh) {
            return null;
        }
        return new ScreenPoint(screenX, screenY);
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

    public static void drawCenteredText(GuiGraphics gg, String text, int x, int y, int color) {
        drawCenteredTextScaled(gg, text, x, y, color, HUD_TEXT_SCALE);
    }

    private static void drawLeftText(GuiGraphics gg, String text, int x, int y, int color) {
        drawLeftTextScaled(gg, text, x, y, color, HUD_TEXT_SCALE);
    }

    private static void drawCenteredTextScaled(GuiGraphics gg, String text, int x, int y, int color, float scale) {
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1);
        float inv = 1.0f / scale;
        gg.drawCenteredString(mc.font, Component.literal(text), (int) (x * inv), (int) (y * inv), color);
        gg.pose().popPose();
    }

    private static void drawLeftTextScaled(GuiGraphics gg, String text, int x, int y, int color, float scale) {
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1);
        float inv = 1.0f / scale;
        gg.drawString(mc.font, Component.literal(text), (int) (x * inv), (int) (y * inv), color, false);
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
        String keyText = keyMapping.getTranslatedKeyMessage().getString();
        String separator = keyText.isEmpty() ? "" : " ";
        int keyWidth = mc.font.width(keyText);
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
            drawLeftText(gg, info.displayName, startX, rowY, MAIN_COLOR);

            int nameWidth = Math.round(mc.font.width(info.displayName) * HUD_TEXT_SCALE);
            int barCenterX = startX + nameWidth + textBarGap + barWidth / 2;
            int barCenterY = rowY + 1;
            int safeMaxCooldown = Math.max(1, info.maxCooldown);
            float targetProgress = Mth.clamp((float) Math.max(0, info.currentTick) / (float) safeMaxCooldown, 0f, 1f);
            float progress = smoothExp(data.smoothWeaponCooldownRatios.get(i), targetProgress, hudAlpha);
            data.smoothWeaponCooldownRatios.set(i, progress);
            float readyProgress = info.remainingCooldown ? 1.0f - progress : progress;

            DrawShape.drawHollowRectangle(gg, barCenterX, barCenterY, barWidth, barHeight + 2, 1, SUB_COLOR);

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

    private record ScreenPoint(float x, float y) {
    }
}
