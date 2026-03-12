package com.kodu16.vsie.content.controlseat.client.HUD;

import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.functions.ShipAnglePainter;
import com.kodu16.vsie.content.controlseat.server.SeatRegistry;
import com.kodu16.vsie.registries.vsieItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HudOverlay {

    // 透明度配置（建议后面改成 Config）
    private static final int TEXT_ALPHA    = 10;   // 主文字透明度

    public static final int MAIN_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0xFF, 0x99);
    public static final int SUB_COLOR  = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0x66, 0x33);

    private static final Minecraft mc = Minecraft.getInstance(); // drawGlowText 要用
    // 功能：记录上一帧 HUD 渲染时间，用于把慢包同步数据按真实帧间隔进行平滑插值。
    private static long lastHudRenderTimeNanos = -1L;


    @SubscribeEvent
    public static void onRenderGuiOverlayEvent(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.getVehicle() == null) return;

        // 必须是 VS2 的船骑乘实体
        if (player.getVehicle().getType() != ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE) return;

        BlockPos controlSeatPos = SeatRegistry.SEAT_TO_CONTROLSEAT.get(player.getVehicle().getUUID());
        if (controlSeatPos == null || mc.level == null) return;

        BlockEntity blockEntity = mc.level.getBlockEntity(controlSeatPos);
        if (blockEntity instanceof ControlSeatBlockEntity controlseat) {
            ControlSeatClientData data = ClientDataManager.getClientData(player);
            GuiGraphics gg = event.getGuiGraphics();
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            float partialTick = event.getPartialTick();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int centerX = sw / 2;
            int centerY = sh / 2;
            int baseY = sh / 6; // 稍微再往上提一点，避免挡准心太严重

            float energyRatio = ratio(data.energyavalible, data.energytotal);
            float fuelRatio = ratio(data.fuelavalible, data.fueltotal);
            float shieldRatio = ratio(data.shieldavalible, data.shieldtotal);
            // 功能：把服务器同步油门作为“目标值”，交给本地每帧平滑跟踪，减少慢包时条形跳变。
            data.throttleTargetRatio = Mth.clamp((data.throttle + 100f) / 200f, 0f, 1f);

            // 功能：按真实渲染帧间隔计算插值权重，避免仅依赖 partialTick 导致慢包时视觉卡顿。
            float frameDeltaSeconds = computeFrameDeltaSeconds();
            float hudAlpha = computeSmoothingAlpha(frameDeltaSeconds, 10f);

            data.smoothEnergyRatio = smoothExp(data.smoothEnergyRatio, energyRatio, hudAlpha);
            data.smoothFuelRatio = smoothExp(data.smoothFuelRatio, fuelRatio, hudAlpha);
            data.smoothShieldRatio = smoothExp(data.smoothShieldRatio, shieldRatio, hudAlpha);
            data.smoothThrottle = smoothExp(data.smoothThrottle, data.throttleTargetRatio, hudAlpha);
            int visualThrottle = Mth.floor(Mth.lerp(data.smoothThrottle, -100f, 100f));

            // 标题 - 粗体 + 青色
            //drawCenteredText(gg, "§l§b控制座椅", centerX, baseY, MAIN_COLOR);

            // 坐标
            //绘制电量条，护盾条，油条（大雾），热量条（未实装），油门，鼠标控制条
            StatusIndicator.renderDecorative(gg,
                    data.smoothEnergyRatio,
                    data.smoothFuelRatio,
                    data.smoothShieldRatio,
                    visualThrottle,
                    (int) data.accumulatedmousex, (int) data.accumulatedmousey);
            gg.drawCenteredString(mc.font, visualThrottle+"%", centerX-(3*centerX/8)+40, centerY+((centerY/2)-5), MAIN_COLOR);

            //绘制护盾/飞行辅助/反重力开关/武器频道开关
            int switchBaseX = centerX + (centerX / 6);
            int switchY = centerY + (centerY / 2);
            int switchGap = 24;
            drawSwitch(gg, "Shield", switchBaseX, switchY, data.shieldon,20,10);
            drawSwitch(gg, "Assist", switchBaseX + switchGap, switchY, data.isflightassiston,20,10);
            drawSwitch(gg, "AntiG", switchBaseX + switchGap * 2, switchY, data.isantigravityon,20,10);
            drawSwitch(gg, "1", switchBaseX+5, switchY+15, data.channel1,10,10);
            drawSwitch(gg, "2", switchBaseX+20, switchY+15, data.channel2,10,10);
            drawSwitch(gg, "3", switchBaseX+35, switchY+15, data.channel3,10,10);
            drawSwitch(gg, "4", switchBaseX+50, switchY+15, data.channel4,10,10);

            // 功能：在 HUD 热量条右侧逐行展示“当前控制椅激活频道下可响应武器”的名称与冷却进度条。
            drawActiveWeaponCooldowns(gg, data, centerX, centerY, hudAlpha);

            //绘制水平和竖直方位条
            var interpolatedFacing = data.getInterpolatedShipFacing(partialTick);
            var interpolatedUp = data.getInterpolatedShipUp(partialTick);
            double[] angles = ShipAnglePainter.getDirectedAnglesToAxes(VectorConversionsMCKt.toMinecraft(interpolatedFacing));
            // 功能：基于 shipUp + shipFacing 计算当前俯仰角（-90~90），用于左侧俯仰条滚动。
            double pitchDeg = ShipAnglePainter.getPitchDegrees(interpolatedFacing, interpolatedUp);
            ShipAnglePainter.drawAngleLine(gg, interpolatedFacing, centerX, baseY+10, MAIN_COLOR);
            drawCenteredText(gg, "§l§b"+(int)angles[0], centerX, baseY+5, MAIN_COLOR);

            // 功能：在护盾弧形左侧绘制俯仰条，主粗刻线固定为 -90 / 0 / 90。
            int shieldArcCenterX = centerX + centerX / 20;
            int pitchBarX = shieldArcCenterX - 60 - 12;
            ShipAnglePainter.drawPitchLine(gg, pitchDeg, pitchBarX, centerY, MAIN_COLOR);

            //水平仪（未完成，目前画的是一坨构石）
            //float horizonAngle = ShipAnglePainter.getHorizonAngleDegrees(interpolatedFacing, interpolatedUp);
            //ShipAnglePainter.drawRotatingItem(gg, new ItemStack(vsieItems.HORIZONTAL_MARK), centerX, centerY, -horizonAngle);

            RenderSystem.disableBlend();
        }
    }


    private static float ratio(int available, int total) {
        if (total <= 0) return 0f;
        return Mth.clamp((float) available / (float) total, 0f, 1f);
    }

    // 功能：计算 HUD 两帧之间的真实秒数，为指数平滑提供稳定的时间基准。
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

    // 功能：根据帧时长与响应速度计算指数平滑权重，让慢包下动画依然连续。
    private static float computeSmoothingAlpha(float deltaSeconds, float responsePerSecond) {
        return Mth.clamp(1f - (float) Math.exp(-responsePerSecond * deltaSeconds), 0f, 1f);
    }

    // 功能：统一指数平滑函数，按 alpha 将当前值渐进逼近目标值。
    private static float smoothExp(float current, float target, float alpha) {
        return Mth.lerp(alpha, current, target);
    }

    // 方便的居中绘制方法（不带辉光）
    public static void drawCenteredText(GuiGraphics gg, String text, int x, int y, int color) {
        float scale = 0.7f;
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1);
        float inv = 1 / scale;
        gg.drawCenteredString(mc.font, Component.literal(text), (int)(x * inv), (int)(y * inv), color);
        gg.pose().popPose();
    }

    private static void drawSwitch(GuiGraphics gg, String label, int x, int y, boolean active, int recwidth, int recheight) {
        int color = active ? MAIN_COLOR : SUB_COLOR;
        drawCenteredText(gg, label, x, y, color);
        DrawShape.drawHollowRectangle(gg, x, y+2, recwidth, recheight, 1, color);
    }

    // 功能：把服务端同步来的激活武器名称与冷却进度（currentTick/getcooldown）逐行绘制在热量条右侧。
    private static void drawActiveWeaponCooldowns(GuiGraphics gg, ControlSeatClientData data, int centerX, int centerY, float hudAlpha) {
        int startX = centerX + centerX / 20 + 90;
        int startY = centerY - 18;
        int lineHeight = 14;
        int barWidth = 52;
        int barHeight = 4;

        // 功能：把平滑数组长度对齐武器行数，确保每行冷却条都有独立插值状态。
        while (data.smoothWeaponCooldownRatios.size() < data.activeWeaponHudInfos.size()) {
            data.smoothWeaponCooldownRatios.add(0f);
        }
        while (data.smoothWeaponCooldownRatios.size() > data.activeWeaponHudInfos.size()) {
            data.smoothWeaponCooldownRatios.remove(data.smoothWeaponCooldownRatios.size() - 1);
        }

        for (int i = 0; i < data.activeWeaponHudInfos.size(); i++) {
            ActiveWeaponHudInfo info = data.activeWeaponHudInfos.get(i);
            int rowY = startY + i * lineHeight;

            // 功能：先绘制武器名称，保持原有 HUD 信息可读性。
            drawCenteredText(gg, info.displayName, startX, rowY, MAIN_COLOR);

            int barCenterX = startX + 55;
            int barCenterY = rowY + 1;
            int safeMaxCooldown = Math.max(1, info.maxCooldown);
            float targetProgress = Mth.clamp((float) Math.max(0, info.currentTick) / (float) safeMaxCooldown, 0f, 1f);
            // 功能：对每行武器冷却进度做本地平滑，减少服务端慢包导致的突变感。
            float progress = smoothExp(data.smoothWeaponCooldownRatios.get(i), targetProgress, hudAlpha);
            data.smoothWeaponCooldownRatios.set(i, progress);

            // 功能：绘制冷却进度条外框，作为“油门样式”槽体。
            DrawShape.drawHollowRectangle(gg, barCenterX, barCenterY, barWidth, barHeight + 2, 1, SUB_COLOR);

            // 功能：按 currentTick/getcooldown 线性插值颜色，进度越高越绿，越低越红。
            int red = Mth.floor(Mth.lerp(progress, 0xFF, 0x00));
            int green = Mth.floor(Mth.lerp(progress, 0x33, 0xFF));
            int dynamicColor = FastColor.ARGB32.color(TEXT_ALPHA, red, green, 0x33);

            // 功能：填充进度条，模拟油门推进效果。
            int fillWidth = Mth.floor((barWidth - 2) * progress);
            if (fillWidth > 0) {
                gg.fill(barCenterX - barWidth / 2 + 1, barCenterY - barHeight / 2 + 1,
                        barCenterX - barWidth / 2 + 1 + fillWidth, barCenterY + barHeight / 2,
                        dynamicColor);
            }
        }
    }

}
