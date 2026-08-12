package com.kodu16.vsie.content.controlseat.client.HUD;

import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.controlseat.functions.ShipAnglePainter;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.integration.deepspace.DeepSpaceHudBridge;
import com.kodu16.vsie.integration.deepspace.DeepSpaceHudRenderer;
import com.kodu16.vsie.registries.vsieKeyMappings;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3dc;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;

import java.util.IdentityHashMap;
import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
@SuppressWarnings("removal")
public class ControlSeatWorldHudRenderer {
    private static final Minecraft mc = Minecraft.getInstance();

    private static final float HUD_VIEW_DISTANCE = 0.9F;
    private static final float HUD_EYE_HEIGHT_FROM_MOUNT = 1.87F;
    private static final int HUD_MARKER_LIGHT = LightTexture.FULL_BRIGHT;
    // Function: world-space HUD needs substantially higher alpha than the old GUI overlay, otherwise most elements become nearly invisible.
    private static final int TEXT_ALPHA = 0xD8;
    private static final int STATUS_TEXT_ALPHA = 0xC0;

    private static final int MAIN_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0xFF, 0x99);
    private static final int SUB_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0x66, 0x33);
    private static final int WARP_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x33, 0xAA, 0xFF);
    private static final int WARP_WARNING_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0xFF, 0x22, 0x33);
    private static final int POWER_WARNING_COLOR = FastColor.ARGB32.color(0xC0, 0xFF, 0x44, 0x44);
    private static final int KEY_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0xFF, 0xFF, 0xFF);
    private static final int ASSIST_LOCK_COLOR = FastColor.ARGB32.color(0x90, 0xFF, 0x22, 0x33);
    private static final int WHITE = FastColor.ARGB32.color(STATUS_TEXT_ALPHA, 0xBB, 0xBB, 0xBB);
    private static final int MAIN_COLOR_FUEL = FastColor.ARGB32.color(STATUS_TEXT_ALPHA, 0xFF, 0xAA, 0x11);
    private static final int MAIN_COLOR_E710 = FastColor.ARGB32.color(STATUS_TEXT_ALPHA, 0x66, 0x11, 0x88);
    private static final int MAIN_COLOR_WARP_E710_COST = FastColor.ARGB32.color(STATUS_TEXT_ALPHA, 0xFF, 0x22, 0x33);
    private static final int MAIN_COLOR_SHIELD = FastColor.ARGB32.color(STATUS_TEXT_ALPHA, 0x00, 0x55, 0xFF);

    private static final int MODE_BUTTON_WIDTH = 36;
    private static final int MODE_BUTTON_HEIGHT = 10;
    private static final int SIDEARC_RADIUS = 60;
    private static final int SIDEARC_THICKNESS = 2;
    private static final int FUEL_BAR_RADIUS = SIDEARC_RADIUS + 4;
    private static final float HUD_TEXT_SCALE = 0.7f;
    private static final float ANGLE_TEXT_SCALE = 0.55F;
    private static final float SMALL_ANGLE_TEXT_SCALE = 0.5F;
    private static final double CONTROL_LINE_MOUSE_X_RANGE = 2560.0D;
    private static final double CONTROL_LINE_MOUSE_Y_RANGE = 1440.0D;
    private static final Map<KeyMapping, CachedKeyText> KEY_TEXT_CACHE = new IdentityHashMap<>();
    private static long lastHudRenderTimeNanos = -1L;

    private record HudSeatContext(ControlSeatBlockEntity controlSeat, BlockPos seatPos) {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        if (mc.options.hideGui) {
            return;
        }

        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        ControlSeatClientData data = ClientDataManager.getClientData(player);
        HudSeatContext hudSeatContext = resolveHudSeatContext(player, data);
        if (hudSeatContext == null) {
            return;
        }
        ControlSeatBlockEntity controlSeat = hudSeatContext.controlSeat();
        BlockPos controlSeatPos = hudSeatContext.seatPos();

        DeltaTracker partialTick = event.getPartialTick();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int screenCenterX = sw / 2;
        int screenCenterY = sh / 2;

        float energyRatio = ratio(data.energyavalible, data.energytotal);
        float fuelRatio = ratio(data.fuelavalible, data.fueltotal);
        float e710Ratio = ratio(data.e710avalible, data.fueltotal);
        boolean showWarpE710Bar = data.isWarpPreparing || data.hasPendingWarpTeleport;
        float warpE710CostRatio = showWarpE710Bar ? ratio(data.warpE710CostMb, data.fueltotal) : 0f;
        float shieldRatio = ratio(data.shieldavalible, data.shieldtotal);
        data.throttleTargetRatio = Mth.clamp((data.throttle + 100f) / 200f, 0f, 1f);

        float frameDeltaSeconds = computeFrameDeltaSeconds();
        float hudAlpha = computeSmoothingAlpha(frameDeltaSeconds, 10f);
        data.smoothEnergyRatio = smoothExp(data.smoothEnergyRatio, energyRatio, hudAlpha);
        data.smoothFuelRatio = smoothExp(data.smoothFuelRatio, fuelRatio, hudAlpha);
        data.smoothE710Ratio = smoothExp(data.smoothE710Ratio, e710Ratio, hudAlpha);
        data.smoothWarpE710CostRatio = smoothExp(data.smoothWarpE710CostRatio, warpE710CostRatio, hudAlpha);
        data.smoothShieldRatio = smoothExp(data.smoothShieldRatio, shieldRatio, hudAlpha);
        data.smoothThrottle = smoothExp(data.smoothThrottle, data.throttleTargetRatio, hudAlpha);
        int visualThrottle = computeVisualThrottle(data);

        Vec3 hudForward = resolveRenderedSeatForward(mc.level, controlSeatPos, mc.level.getBlockState(controlSeatPos));
        Vec3 hudUp = resolveRenderedSeatUp(mc.level, controlSeatPos);
        Vec3 hudRight = resolveRenderedSeatRight(hudForward, hudUp);
        if (hudForward == null || hudUp == null || hudRight == null) {
            return;
        }

        Vec3 hudTranslation;
        if (usesStableFirstPersonTranslation(player, controlSeatPos)) {
            // Function: the seated eye and HUD share one local frame, so avoid subtracting independently interpolated world positions.
            hudTranslation = hudForward.scale(HUD_VIEW_DISTANCE);
        } else {
            Vec3 cameraPos = event.getCamera().getPosition();
            Vec3 hudAnchor = resolveHudAnchor(mc.level, controlSeatPos);
            if (hudAnchor == null) {
                data.clearLastHudSeat();
                return;
            }
            // Function: third-person and remembered-seat HUDs retain their original world-space anchoring.
            hudTranslation = hudAnchor.subtract(cameraPos);
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        float hudWorldScale = computeHudWorldScale(sh);

        beginHudRender();
        poseStack.pushPose();
        try {
            poseStack.translate(hudTranslation.x, hudTranslation.y, hudTranslation.z);
            alignHudToSeatPlane(poseStack, hudRight, hudUp, hudForward);
            poseStack.scale(hudWorldScale, -hudWorldScale, hudWorldScale);
            renderHudPanel(poseStack, buffers, data, screenCenterX, screenCenterY, sw, sh, partialTick, visualThrottle, showWarpE710Bar, hudAlpha);
            buffers.endBatch();
        } finally {
            poseStack.popPose();
            endHudRender();
        }
    }

    private static boolean usesStableFirstPersonTranslation(Player player, BlockPos controlSeatPos) {
        return mc.options.getCameraType().isFirstPerson()
                && player.getVehicle() instanceof ControlSeatMountEntity mountEntity
                && controlSeatPos.equals(mountEntity.getBoundBlockPos());
    }

    private static HudSeatContext resolveHudSeatContext(Player player, ControlSeatClientData data) {
        ClientLevel level = mc.level;
        if (level == null) {
            return null;
        }

        BlockPos seatPos = null;
        if (player.getVehicle() instanceof ControlSeatMountEntity mountEntity) {
            seatPos = mountEntity.getBoundBlockPos();
            data.bindSeat(seatPos);
            data.rememberHudSeat(seatPos);
        } else {
            seatPos = data.getLastHudSeatPos();
        }

        if (seatPos == null) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(seatPos);
        if (!(blockEntity instanceof ControlSeatBlockEntity controlSeat)) {
            data.clearLastHudSeat();
            return null;
        }

        return new HudSeatContext(controlSeat, seatPos);
    }

    private static Vec3 resolveHudAnchor(ClientLevel level, BlockPos seatPos) {
        BlockState state = level.getBlockState(seatPos);
        Vec3 seatOrigin = resolveRenderedSeatMountPosition(level, seatPos, state);
        Vec3 forward = resolveRenderedSeatForward(level, seatPos, state);
        Vec3 up = resolveRenderedSeatUp(level, seatPos);
        if (seatOrigin == null || forward == null || up == null) {
            return null;
        }

        // Function: anchor the HUD at the locked-view eye plane so the world HUD matches the old screen HUD placement.
        return seatOrigin.add(up.scale(HUD_EYE_HEIGHT_FROM_MOUNT)).add(forward.scale(HUD_VIEW_DISTANCE));
    }

    private static Vec3 resolveRenderedSeatMountPosition(ClientLevel level, BlockPos seatPos, BlockState state) {
        Vec3 localMountPos = ControlSeatMountEntity.getSeatMountPosition(seatPos, state);
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, seatPos);
        if (subLevel instanceof ClientSubLevel clientSubLevel) {
            return clientSubLevel.renderPose().transformPosition(localMountPos);
        }
        return subLevel == null ? localMountPos : subLevel.logicalPose().transformPosition(localMountPos);
    }

    private static Vec3 resolveRenderedSeatForward(ClientLevel level, BlockPos seatPos, BlockState state) {
        Direction facing = state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.NORTH;
        // Function: the control-seat state facing is opposite the rider view, so the HUD must use the reversed forward axis.
        Vec3 localForward = Vec3.atLowerCornerOf(facing.getOpposite().getNormal());
        return transformSeatAxis(level, seatPos, localForward);
    }

    private static Vec3 resolveRenderedSeatUp(ClientLevel level, BlockPos seatPos) {
        return transformSeatAxis(level, seatPos, new Vec3(0.0D, 1.0D, 0.0D));
    }

    private static Vec3 resolveRenderedSeatRight(Vec3 forward, Vec3 up) {
        if (forward == null || up == null) {
            return null;
        }
        Vec3 right = forward.cross(up);
        if (right.lengthSqr() < 1.0E-8D) {
            return null;
        }
        return right.normalize();
    }

    private static Vec3 transformSeatAxis(ClientLevel level, BlockPos seatPos, Vec3 localAxis) {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, seatPos);
        if (subLevel instanceof ClientSubLevel clientSubLevel) {
            return normalize(clientSubLevel.renderPose().transformNormal(new Vector3d(localAxis.x, localAxis.y, localAxis.z)));
        }
        if (subLevel != null) {
            return normalize(subLevel.logicalPose().transformNormal(new Vector3d(localAxis.x, localAxis.y, localAxis.z)));
        }
        return localAxis.normalize();
    }

    private static Vec3 normalize(Vector3dc vector) {
        Vector3d normalized = new Vector3d(vector);
        if (normalized.lengthSquared() < 1.0E-8D) {
            return new Vec3(0.0D, 0.0D, 0.0D);
        }
        normalized.normalize();
        return new Vec3(normalized.x, normalized.y, normalized.z);
    }

    private static void alignHudToSeatPlane(PoseStack poseStack, Vec3 right, Vec3 up, Vec3 forward) {
        Vec3 orthoUp = right.cross(forward).normalize();
        Matrix4f orientation = new Matrix4f().identity();
        // Function: keep the HUD fixed to the control-seat plane instead of billboarding toward the player camera.
        orientation.m00((float) right.x).m01((float) right.y).m02((float) right.z);
        orientation.m10((float) orthoUp.x).m11((float) orthoUp.y).m12((float) orthoUp.z);
        orientation.m20((float) -forward.x).m21((float) -forward.y).m22((float) -forward.z);
        poseStack.last().pose().mul(orientation);
    }

    private static float computeHudWorldScale(int guiScaledHeight) {
        float safeHeight = Math.max(guiScaledHeight, 1);
        float fovDegrees = mc.options.fov().get().floatValue();
        double halfFovRadians = 0.5D * fovDegrees * Mth.DEG_TO_RAD;
        // Function: convert old GUI pixel offsets into a fixed seat-space panel that keeps the same apparent size when view-locked.
        return (float) ((2.0D * HUD_VIEW_DISTANCE * Math.tan(halfFovRadians)) / safeHeight);
    }

    private static void renderHudPanel(PoseStack poseStack,
                                       MultiBufferSource.BufferSource buffers,
                                       ControlSeatClientData data,
                                       int screenCenterX,
                                       int screenCenterY,
                                       int sw,
                                       int sh,
                                       DeltaTracker partialTick,
                                       int visualThrottle,
                                       boolean showWarpE710Bar,
                                       float hudAlpha) {
        int centerX = sw / 2;
        int centerY = sh / 2;
        int baseY = sh / 6;

        renderDecorative(poseStack, data, screenCenterX, screenCenterY, sw, sh, visualThrottle, showWarpE710Bar);

        int throttleCenterX = centerX - (3 * centerX / 10);
        int throttleY = centerY + (centerY / 3);
        int throttleBarLeftX = throttleCenterX - 25;
        drawRightText(poseStack, buffers, visualThrottle + "%", throttleBarLeftX - 8, throttleY - 4, MAIN_COLOR, screenCenterX, screenCenterY);

        int switchBaseX = throttleCenterX;
        int switchY = throttleY + 22;
        int switchGapX = 52;
        int switchGapY = 18;
        int leftSwitchX = switchBaseX - switchGapX / 2;
        int rightSwitchX = switchBaseX + switchGapX / 2;
        drawKeyedSwitch(poseStack, buffers, "Shield", vsieKeyMappings.KEY_TOGGLE_SHIELD, leftSwitchX, switchY, data.shieldon, data.isShieldOverloaded, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, screenCenterX, screenCenterY);
        drawKeyedSwitch(poseStack, buffers, "Force", vsieKeyMappings.KEY_TOGGLE_FORCE_ASSIST, rightSwitchX, switchY, data.isforceassiston, data.isForceAssistSuppressedByAccelerator, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, screenCenterX, screenCenterY);
        drawKeyedSwitch(poseStack, buffers, "Torque", vsieKeyMappings.KEY_TOGGLE_TORQUE_ASSIST, leftSwitchX, switchY + switchGapY, data.istorqueassiston, false, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, screenCenterX, screenCenterY);
        drawKeyedSwitch(poseStack, buffers, "AntiG", vsieKeyMappings.KEY_TOGGLE_ANTI_GRAVITY, rightSwitchX, switchY + switchGapY, data.isantigravityon, false, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, screenCenterX, screenCenterY);
        drawWarpSwitch(poseStack, buffers, data, leftSwitchX, switchY + switchGapY * 2, screenCenterX, screenCenterY);
        drawKeyedSwitch(poseStack, buffers, "Level", vsieKeyMappings.KEY_TOGGLE_AUTO_LEVEL, rightSwitchX, switchY + switchGapY * 2, data.isAutoLevelOn, data.isWarpPreparing || data.hasPendingWarpTeleport, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, screenCenterX, screenCenterY);
        if (DeepSpaceHudBridge.available()) {
            drawKeyedSwitch(poseStack, buffers, "DeepSpace", vsieKeyMappings.KEY_TOGGLE_DEEPSPACE_HUD,
                    leftSwitchX, switchY + switchGapY * 3, DeepSpaceHudRenderer.isEnabled(), false,
                    MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, screenCenterX, screenCenterY);
        }

        int rightArcCenterX = centerX + (3 * centerX / 10);
        int rightInfoY = throttleY - 16;
        drawCenteredText(poseStack, buffers, formatSpeedText(data.shipSpeed), rightArcCenterX, rightInfoY, MAIN_COLOR, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
        drawCenteredText(poseStack, buffers, formatCoordinatesText(data.structureCenterWorld), rightArcCenterX, rightInfoY + 10, MAIN_COLOR, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
        int channelY = rightInfoY + 28;
        drawSwitch(poseStack, buffers, "1", rightArcCenterX - 24, channelY, data.channel1, 10, 10, screenCenterX, screenCenterY);
        drawSwitch(poseStack, buffers, "2", rightArcCenterX - 8, channelY, data.channel2, 10, 10, screenCenterX, screenCenterY);
        drawSwitch(poseStack, buffers, "3", rightArcCenterX + 8, channelY, data.channel3, 10, 10, screenCenterX, screenCenterY);
        drawSwitch(poseStack, buffers, "4", rightArcCenterX + 24, channelY, data.channel4, 10, 10, screenCenterX, screenCenterY);
        drawActiveWeaponCooldowns(poseStack, buffers, data, centerX, centerY, hudAlpha, screenCenterX, screenCenterY);

        Vector3d interpolatedFacing = data.getInterpolatedShipFacing(partialTick);
        Vector3d interpolatedUp = data.getInterpolatedShipUp(partialTick);
        double[] angles = ShipAnglePainter.getDirectedAnglesToAxes(new Vec3(interpolatedFacing.x, interpolatedFacing.y, interpolatedFacing.z));
        double pitchDeg = ShipAnglePainter.getPitchDegrees(interpolatedFacing, interpolatedUp);
        drawAngleLine(poseStack, buffers, interpolatedFacing, centerX, baseY + 10, MAIN_COLOR, screenCenterX, screenCenterY);
        drawCenteredText(poseStack, buffers, String.valueOf((int) angles[0]), centerX, baseY + 5, MAIN_COLOR, HUD_TEXT_SCALE, screenCenterX, screenCenterY);

        int leftArcCenterX = centerX - centerX / 20;
        int pitchBarX = leftArcCenterX - 82;
        drawPitchLineCompact(poseStack, buffers, pitchDeg, pitchBarX, centerY, MAIN_COLOR, screenCenterX, screenCenterY);
        // Function: place the G-force readout beside the pitch ladder so it no longer overlaps the right-side weapon cooldown list.
        drawRightText(poseStack, buffers, formatGForceText(data.seatGForce), pitchBarX - 18, centerY - 4, MAIN_COLOR, screenCenterX, screenCenterY);

        if (data.energyavalible <= 0) {
            drawCenteredText(poseStack, buffers, "NO BATTERY POWER", centerX, centerY + 52, POWER_WARNING_COLOR, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
        } else if (!showWarpE710Bar && data.fuelavalible <= 0) {
            // Function: no thruster fuel is a flight warning, but shield-only systems can still run from battery power.
            drawCenteredText(poseStack, buffers, "NO FUEL", centerX, centerY + 52, POWER_WARNING_COLOR, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
        }
    }

    private static void renderDecorative(PoseStack poseStack,
                                         ControlSeatClientData data,
                                         int screenCenterX,
                                         int screenCenterY,
                                         int sw,
                                         int sh,
                                         int visualThrottle,
                                         boolean showWarpE710Bar) {
        int centerX = sw / 2;
        int centerY = sh / 2;

        float energyAngle = -210 + 60 * data.smoothEnergyRatio;
        drawStatusArc(poseStack, centerX - centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR, -210, energyAngle, screenCenterX, screenCenterY);
        drawStatusArc(poseStack, centerX - centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, WHITE, energyAngle, -150, screenCenterX, screenCenterY);

        drawFuelStatusArc(poseStack, centerX - centerX / 20 - 2, centerY, data.smoothFuelRatio, data.smoothE710Ratio, data.smoothWarpE710CostRatio, showWarpE710Bar, screenCenterX, screenCenterY);

        float shieldAngle = 30 - 60 * data.smoothShieldRatio;
        drawStatusArc(poseStack, centerX + centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR_SHIELD, shieldAngle, 30, screenCenterX, screenCenterY);
        drawStatusArc(poseStack, centerX + centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, WHITE, -30, shieldAngle, screenCenterX, screenCenterY);
        drawStatusArc(poseStack, centerX + centerX / 20 + 2, centerY, SIDEARC_RADIUS + 4, SIDEARC_THICKNESS, WHITE, -31, 31, screenCenterX, screenCenterY);

        int throttleY = centerY + (centerY / 3);
        drawThickLine(poseStack, centerX - (3 * centerX / 10) - 25, throttleY, centerX - (3 * centerX / 10) + 25, throttleY, 4, SUB_COLOR, screenCenterX, screenCenterY);
        drawThickLine(poseStack, centerX - (3 * centerX / 10), throttleY, centerX - (3 * centerX / 10) + (int) (0.25 * visualThrottle), throttleY, 4, MAIN_COLOR, screenCenterX, screenCenterY);

        if (data.isWarpPreparing) {
            // Function: warp auto-alignment uses the same center-line shape as mouse control, but is colored blue.
            drawControlLine(poseStack, centerX, centerY,
                    data.warpAlignmentControlX * CONTROL_LINE_MOUSE_X_RANGE,
                    data.warpAlignmentControlY * CONTROL_LINE_MOUSE_Y_RANGE,
                    WARP_COLOR, screenCenterX, screenCenterY);
        } else if (!data.hasPendingWarpTeleport) {
            drawControlLine(poseStack, centerX, centerY, data.accumulatedmousex, data.accumulatedmousey, MAIN_COLOR, screenCenterX, screenCenterY);
        }
    }

    private static void drawControlLine(PoseStack poseStack, int centerX, int centerY, double controlX, double controlY, int color, int screenCenterX, int screenCenterY) {
        double deltax = (controlX < 0 ? -1 : 1) * Math.sqrt(Math.abs(controlX) / 2.0D);
        double deltay = (controlY < 0 ? -1 : 1) * Math.sqrt(Math.abs(controlY) / 2.0D);
        drawThickLine(poseStack, centerX, centerY, (int) (centerX + deltax), (int) (centerY + deltay), 1, color, screenCenterX, screenCenterY);
    }

    private static void drawFuelStatusArc(PoseStack poseStack, int cx, int cy,
                                          float fuelPercent, float e710Percent,
                                          float warpE710CostPercent, boolean showWarpE710Bar,
                                          int screenCenterX, int screenCenterY) {
        float startAngle = -211f;
        float endAngle = -149f;
        float span = endAngle - startAngle;
        if (!showWarpE710Bar) {
            float fuelRatio = Math.max(0f, Math.min(1f, fuelPercent));
            float fuelEndAngle = startAngle + span * fuelRatio;
            drawStatusArc(poseStack, cx, cy, FUEL_BAR_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR_FUEL, startAngle, fuelEndAngle, screenCenterX, screenCenterY);
            drawStatusArc(poseStack, cx, cy, FUEL_BAR_RADIUS, SIDEARC_THICKNESS, WHITE, fuelEndAngle, endAngle, screenCenterX, screenCenterY);
            return;
        }

        float e710Ratio = Math.max(0f, Math.min(1f, e710Percent));
        float e710EndAngle = startAngle + span * e710Ratio;
        drawStatusArc(poseStack, cx, cy, FUEL_BAR_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR_E710, startAngle, e710EndAngle, screenCenterX, screenCenterY);
        drawStatusArc(poseStack, cx, cy, FUEL_BAR_RADIUS, SIDEARC_THICKNESS, WHITE, e710EndAngle, endAngle, screenCenterX, screenCenterY);
        if (warpE710CostPercent > 0f && e710EndAngle > startAngle) {
            float visibleCostRatio = Math.max(0f, Math.min(e710Ratio, warpE710CostPercent));
            float costStartAngle = Math.max(startAngle, e710EndAngle - span * visibleCostRatio);
            drawStatusArc(poseStack, cx, cy, FUEL_BAR_RADIUS + 1, 1, MAIN_COLOR_WARP_E710_COST, costStartAngle, e710EndAngle, screenCenterX, screenCenterY);
        }
    }

    private static void drawStatusArc(PoseStack poseStack, int cx, int cy, int radius, int thickness, int color, float startAngleDeg, float endAngleDeg, int screenCenterX, int screenCenterY) {
        drawPartialArc(poseStack, cx - screenCenterX, cy - screenCenterY, radius, thickness, color, startAngleDeg, endAngleDeg);
    }

    private static void drawAngleLine(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vector3d shipFacing, int centerX, int baseY, int color, int screenCenterX, int screenCenterY) {
        double yawDeg = ShipAnglePainter.getDirectedAnglesToAxes(new Vec3(shipFacing.x, shipFacing.y, shipFacing.z))[0];
        yawDeg = yawDeg % 360;
        if (yawDeg < 0) {
            yawDeg += 360;
        }

        int ticksEachSide = 8;
        double pxPer10Deg = 5.0;
        double fracOffset = (yawDeg % 10.0) * (pxPer10Deg / 10.0);
        double highlightRadius = 5.0;
        int cardinalColor = 0xFF88DDFF;
        int cardinalLength = 2;
        int cardinalThickness = 2;

        int[] cardinalAngles = {0, 90, 180, 270};
        String[] cardinalLabels = {"N", "E", "S", "W"};

        for (int i = 0; i < 4; i++) {
            double targetAngle = cardinalAngles[i];
            double delta = ((targetAngle - yawDeg + 180 + 360) % 360) - 180;
            double screenOffset = delta * pxPer10Deg / 10.0;
            int xPos = centerX + (int) Math.round(screenOffset);
            int lineTop = baseY - cardinalLength;
            int lineBottom = baseY + cardinalLength + 1;
            if (Math.abs(screenOffset) < centerX * 0.15) {
                drawThickLine(poseStack, xPos, lineTop, xPos, lineBottom, cardinalThickness, cardinalColor, screenCenterX, screenCenterY);
                drawCenteredText(poseStack, buffers, cardinalLabels[i], xPos, baseY + 5, 0xFFCCFFFF, 1.0F, screenCenterX, screenCenterY);
            }
        }

        for (int i = -ticksEachSide; i <= ticksEachSide; i++) {
            double angle = yawDeg + i * 10.0;
            double normalized = ((angle % 360) + 360) % 360;
            if (isCardinal(normalized)) {
                continue;
            }

            double xOffset = i * pxPer10Deg - fracOffset;
            int xPos = centerX + (int) Math.round(xOffset);
            double deltaDeg = Math.abs(normalized - yawDeg);
            deltaDeg = Math.min(deltaDeg, 360 - deltaDeg);
            float t = (float) Math.max(0.0, Math.min(1.0, deltaDeg / highlightRadius));
            float smoothT = t * t;
            float strength = 1.0f - smoothT;

            int finalColor = lerpColor(color, cardinalColor, strength);
            int lineLength = Math.round(1 + strength);
            int thickness = Math.round(1 + strength);
            int alpha = Math.round(100 + strength * 155);
            finalColor = (finalColor & 0x00FFFFFF) | (alpha << 24);
            drawThickLine(poseStack, xPos, baseY - lineLength, xPos, baseY + lineLength + 1, thickness, finalColor, screenCenterX, screenCenterY);
        }
    }

    private static void drawPitchLineCompact(PoseStack poseStack, MultiBufferSource.BufferSource buffers, double pitchDeg, int barX, int centerY, int color, int screenCenterX, int screenCenterY) {
        pitchDeg = Math.max(-90.0, Math.min(90.0, pitchDeg));
        int ticksEachSide = 8;
        double pxPer10Deg = 2.5;
        double fracOffset = (pitchDeg % 10.0) * (pxPer10Deg / 10.0);
        int minorHalfLength = 2;
        int majorHalfLength = 4;
        int majorColor = 0xFF88DDFF;

        drawCenteredText(poseStack, buffers, formatSignedAngle(Math.round(pitchDeg)), barX, centerY - 30, color, ANGLE_TEXT_SCALE, screenCenterX, screenCenterY);
        for (int i = -ticksEachSide; i <= ticksEachSide; i++) {
            double angle = pitchDeg + i * 10.0;
            if (angle < -90.0 || angle > 90.0) {
                continue;
            }
            int yPos = centerY + (int) Math.round(i * pxPer10Deg - fracOffset);
            drawThickLine(poseStack, barX - minorHalfLength, yPos, barX + minorHalfLength, yPos, 1, color, screenCenterX, screenCenterY);
        }

        int[] majorAngles = {-90, 0, 90};
        for (int majorAngle : majorAngles) {
            double delta = majorAngle - pitchDeg;
            int yPos = centerY + (int) Math.round(delta * pxPer10Deg / 10.0);
            if (Math.abs(yPos - centerY) > ticksEachSide * pxPer10Deg + 4) {
                continue;
            }
            drawThickLine(poseStack, barX - majorHalfLength, yPos, barX + majorHalfLength, yPos, 1, majorColor, screenCenterX, screenCenterY);
            drawCenteredText(poseStack, buffers, String.valueOf(majorAngle), barX - 12, yPos - 2, 0xFFCCFFFF, SMALL_ANGLE_TEXT_SCALE, screenCenterX, screenCenterY);
        }
    }

    private static void drawActiveWeaponCooldowns(PoseStack poseStack, MultiBufferSource.BufferSource buffers, ControlSeatClientData data, int centerX, int centerY, float hudAlpha, int screenCenterX, int screenCenterY) {
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
            drawLeftText(poseStack, buffers, info.displayName, startX, rowY, weaponStatusColor, HUD_TEXT_SCALE, screenCenterX, screenCenterY);

            int nameWidth = Math.round(mc.font.width(info.displayName) * HUD_TEXT_SCALE);
            int barCenterX = startX + nameWidth + textBarGap + barWidth / 2;
            int barCenterY = rowY + 1;
            int safeMaxCooldown = Math.max(1, info.maxCooldown);
            float targetProgress = Mth.clamp((float) Math.max(0, info.currentTick) / (float) safeMaxCooldown, 0f, 1f);
            float progress = smoothExp(data.smoothWeaponCooldownRatios.get(i), targetProgress, hudAlpha);
            data.smoothWeaponCooldownRatios.set(i, progress);
            float readyProgress = info.remainingCooldown ? 1.0f - progress : progress;

            drawHollowRectangle(poseStack, barCenterX, barCenterY, barWidth, barHeight + 2, 1, weaponStatusColor, screenCenterX, screenCenterY);

            int red = Mth.floor(Mth.lerp(readyProgress, 0xFF, 0x00));
            int green = Mth.floor(Mth.lerp(readyProgress, 0x33, 0xFF));
            int dynamicColor = FastColor.ARGB32.color(TEXT_ALPHA, red, green, 0x33);
            int fillWidth = Mth.floor((barWidth - 2) * progress);
            if (fillWidth > 0) {
                fillRectangle(poseStack, barCenterX - barWidth / 2 + 1, barCenterY - barHeight / 2 + 1, fillWidth, barHeight - 1, dynamicColor, screenCenterX, screenCenterY);
            }
        }
    }

    private static void drawWarpSwitch(PoseStack poseStack, MultiBufferSource.BufferSource buffers, ControlSeatClientData data, int x, int y, int screenCenterX, int screenCenterY) {
        boolean active = data.isWarpPreparing || data.hasPendingWarpTeleport;
        if (!active) {
            drawKeyedSwitch(poseStack, buffers, "Warp", vsieKeyMappings.KEY_START_WARP, x, y, false, false, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, screenCenterX, screenCenterY);
            if (data.warpE710Insufficient) {
                drawCenteredText(poseStack, buffers, "NO E-710", x, y + 13, WARP_WARNING_COLOR, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
            }
            return;
        }

        String label = data.hasPendingWarpTeleport ? "JUMP" : "ALIGN";
        drawCenteredText(poseStack, buffers, label, x, y, WARP_COLOR, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
        drawHollowRectangle(poseStack, x, y + 2, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, 1, WARP_COLOR, screenCenterX, screenCenterY);
    }

    private static void drawSwitch(PoseStack poseStack, MultiBufferSource.BufferSource buffers, String label, int x, int y, boolean active, int width, int height, int screenCenterX, int screenCenterY) {
        int color = active ? MAIN_COLOR : SUB_COLOR;
        drawCenteredText(poseStack, buffers, label, x, y, color, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
        drawHollowRectangle(poseStack, x, y + 2, width, height, 1, color, screenCenterX, screenCenterY);
    }

    private static void drawKeyedSwitch(PoseStack poseStack, MultiBufferSource.BufferSource buffers, String label, KeyMapping keyMapping, int x, int y, boolean active, boolean forcedDisabled, int width, int height, int screenCenterX, int screenCenterY) {
        int color = forcedDisabled ? ASSIST_LOCK_COLOR : (active ? MAIN_COLOR : SUB_COLOR);
        int keyColor = forcedDisabled ? ASSIST_LOCK_COLOR : KEY_COLOR;
        CachedKeyText cachedKeyText = getCachedKeyText(keyMapping);
        String keyText = cachedKeyText.text();
        String separator = keyText.isEmpty() ? "" : " ";
        int keyWidth = cachedKeyText.width();
        int totalWidth = keyWidth + mc.font.width(separator + label);
        int leftX = Math.round(x - totalWidth * HUD_TEXT_SCALE / 2f);

        drawLeftText(poseStack, buffers, keyText, leftX, y, keyColor, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
        drawLeftText(poseStack, buffers, separator + label, Math.round(leftX + keyWidth * HUD_TEXT_SCALE), y, color, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
        drawHollowRectangle(poseStack, x, y + 2, width, height, 1, color, screenCenterX, screenCenterY);
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

    private static void drawCenteredText(PoseStack poseStack, MultiBufferSource.BufferSource buffers, String text, int screenX, int screenY, int color, float scale, int screenCenterX, int screenCenterY) {
        poseStack.pushPose();
        try {
            poseStack.scale(scale, scale, 1.0F);
            float inv = 1.0F / scale;
            float width = mc.font.width(text);
            float localX = ((screenX - screenCenterX) - (width * scale) / 2.0f) * inv;
            float localY = (screenY - screenCenterY) * inv;
            Matrix4f matrix = poseStack.last().pose();
            drawText(buffers, text, localX, localY, color, matrix);
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawLeftText(PoseStack poseStack, MultiBufferSource.BufferSource buffers, String text, int screenX, int screenY, int color, float scale, int screenCenterX, int screenCenterY) {
        poseStack.pushPose();
        try {
            poseStack.scale(scale, scale, 1.0F);
            float inv = 1.0F / scale;
            float localX = (screenX - screenCenterX) * inv;
            float localY = (screenY - screenCenterY) * inv;
            Matrix4f matrix = poseStack.last().pose();
            drawText(buffers, text, localX, localY, color, matrix);
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawRightText(PoseStack poseStack, MultiBufferSource.BufferSource buffers, String text, int screenX, int screenY, int color, int screenCenterX, int screenCenterY) {
        int scaledWidth = Math.round(mc.font.width(text) * HUD_TEXT_SCALE);
        drawLeftText(poseStack, buffers, text, screenX - scaledWidth, screenY, color, HUD_TEXT_SCALE, screenCenterX, screenCenterY);
    }

    private static void drawText(MultiBufferSource.BufferSource buffers, String text, float x, float y, int color, Matrix4f matrix) {
        Font font = mc.font;
        // Function: control-seat HUD text is part of the pilot overlay, so blocks must not occlude it.
        font.drawInBatch(text, x, y, color, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0, HUD_MARKER_LIGHT);
    }

    private static void drawThickLine(PoseStack poseStack, int screenX1, int screenY1, int screenX2, int screenY2, int thickness, int argb, int screenCenterX, int screenCenterY) {
        if (thickness <= 0) {
            return;
        }

        int x1 = screenX1 - screenCenterX;
        int y1 = screenY1 - screenCenterY;
        int x2 = screenX2 - screenCenterX;
        int y2 = screenY2 - screenCenterY;
        if (x1 == x2 && y1 == y2) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        DrawShapeToBuffer.drawThickLine(buffer, poseStack.last().pose(), x1, y1, x2, y2, thickness, argb);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawHollowRectangle(PoseStack poseStack, int screenCenterRectX, int screenCenterRectY, int width, int height, int thickness, int argb, int screenCenterX, int screenCenterY) {
        if (width <= 0 || height <= 0 || thickness <= 0) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        DrawShapeToBuffer.drawHollowRectangle(buffer, poseStack.last().pose(), screenCenterRectX - screenCenterX, screenCenterRectY - screenCenterY, width, height, thickness, argb);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawPartialArc(PoseStack poseStack, int localCenterX, int localCenterY, int radius, int thickness, int argb, float startAngleDeg, float endAngleDeg) {
        if (radius <= 0 || thickness <= 0 || Math.abs(endAngleDeg - startAngleDeg) < 0.0001F) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        DrawShapeToBuffer.drawPartialArc(buffer, poseStack.last().pose(), localCenterX, localCenterY, radius, thickness, argb, startAngleDeg, endAngleDeg);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void fillRectangle(PoseStack poseStack, int screenLeft, int screenTop, int width, int height, int argb, int screenCenterX, int screenCenterY) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float a = (argb >> 24 & 255) / 255f;
        float r = (argb >> 16 & 255) / 255f;
        float g = (argb >> 8 & 255) / 255f;
        float b = (argb & 255) / 255f;
        int left = screenLeft - screenCenterX;
        int top = screenTop - screenCenterY;
        int right = left + width;
        int bottom = top + height;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        buffer.addVertex(matrix, left, top, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, left, bottom, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, right, bottom, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, right, top, 0).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void beginHudRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Function: control-seat HUD is a pilot overlay and must stay visible through level geometry.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static void endHudRender() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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

    private static int computeVisualThrottle(ControlSeatClientData data) {
        int targetThrottle = Mth.clamp(data.throttle, -100, 100);
        if (Math.abs(targetThrottle) == 100) {
            // Function: exponential smoothing approaches endpoints asymptotically, so snap the HUD at real full throttle.
            return targetThrottle;
        }
        return Mth.clamp(Math.round(Mth.lerp(data.smoothThrottle, -100f, 100f)), -99, 99);
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

    private static String formatSignedAngle(long angle) {
        long clamped = Math.max(-99L, Math.min(99L, angle));
        String digits = Math.abs(clamped) < 10 ? "0" + Math.abs(clamped) : Long.toString(Math.abs(clamped));
        return (clamped >= 0 ? "+" : "-") + digits;
    }

    private static boolean isCardinal(double angle) {
        double[] cardinals = {0, 90, 180, 270};
        for (double cardinal : cardinals) {
            double delta = Math.abs(angle - cardinal);
            delta = Math.min(delta, 360 - delta);
            if (delta < 0.5) {
                return true;
            }
        }
        return false;
    }

    private static int lerpColor(int color1, int color2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + t * (a2 - a1));
        int r = (int) (r1 + t * (r2 - r1));
        int g = (int) (g1 + t * (g2 - g1));
        int b = (int) (b1 + t * (b2 - b1));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private record CachedKeyText(String text, int width) {
    }
}
