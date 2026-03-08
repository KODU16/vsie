package com.kodu16.vsie.content.controlseat.client.HUD;

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
            float throttleRatio = Mth.clamp((data.throttle + 100f) / 200f, 0f, 1f);

            data.smoothEnergyRatio = smooth(data.smoothEnergyRatio, energyRatio, partialTick);
            data.smoothFuelRatio = smooth(data.smoothFuelRatio, fuelRatio, partialTick);
            data.smoothShieldRatio = smooth(data.smoothShieldRatio, shieldRatio, partialTick);
            data.smoothThrottle = smooth(data.smoothThrottle, throttleRatio, partialTick);
            int visualThrottle = Mth.floor(Mth.lerp(data.smoothThrottle, -100f, 100f));

            // 标题 - 粗体 + 青色
            //drawCenteredText(gg, "§l§b控制座椅", centerX, baseY, MAIN_COLOR);

            // 坐标
            Vec3 pos = controlseat.getBlockPos().getCenter();
            String coord = String.format("§a%.1f §b%.1f §c%.1f", pos.x, pos.y, pos.z);
            //drawCenteredText(gg, coord, centerX, baseY + 18, SUB_COLOR);

            //武器频道
            drawCenteredText(gg,"§e"+data.channel1, centerX-60, baseY + 50, SUB_COLOR);
            drawCenteredText(gg,"§e"+data.channel2, centerX-20, baseY + 50, SUB_COLOR);
            drawCenteredText(gg,"§e"+data.channel3, centerX+20, baseY + 50, SUB_COLOR);
            drawCenteredText(gg,"§e"+data.channel4, centerX+60, baseY + 50, SUB_COLOR);

            //绘制电量条，护盾条，油条（大雾），热量条（未实装），油门，鼠标控制条
            StatusIndicator.renderDecorative(gg,
                    data.smoothEnergyRatio,
                    data.smoothFuelRatio,
                    data.smoothShieldRatio,
                    visualThrottle,
                    (int) data.accumulatedmousex, (int) data.accumulatedmousey);
            gg.drawCenteredString(mc.font, visualThrottle+"%", centerX-(3*centerX/8)+40, centerY+((centerY/2)-5), MAIN_COLOR);

            //绘制护盾开关
            if(data.shieldon) {
                drawCenteredText(gg,"Shield", centerX+(3*centerX/8), centerY+(centerY/2), MAIN_COLOR);
                DrawShape.drawHollowRectangle(gg,centerX+(3*centerX/8), centerY+(centerY/2), 20, 10, 1, MAIN_COLOR);
            }
            else {
                drawCenteredText(gg,"Shield", centerX+(3*centerX/8), centerY+(centerY/2), SUB_COLOR);
                DrawShape.drawHollowRectangle(gg,centerX+(3*centerX/8), centerY+(centerY/2), 20, 10, 1, SUB_COLOR);
            }

            //绘制水平和竖直方位条
            var interpolatedFacing = data.getInterpolatedShipFacing(partialTick);
            var interpolatedUp = data.getInterpolatedShipUp(partialTick);
            double[] angles = ShipAnglePainter.getDirectedAnglesToAxes(VectorConversionsMCKt.toMinecraft(interpolatedFacing));
            ShipAnglePainter.drawAngleLine(gg, interpolatedFacing, centerX, baseY+10, MAIN_COLOR);
            drawCenteredText(gg, "§l§b"+(int)angles[0], centerX, baseY+5, MAIN_COLOR);
            double[] anglesUp = ShipAnglePainter.getDirectedAnglesToAxes(VectorConversionsMCKt.toMinecraft(interpolatedUp));
            //ShipAnglePainter.drawRotatingItem(gg, new ItemStack(vsieItems.HORIZONTAL_MARK), centerX, centerY-20, (float) -anglesUp[1]);

            RenderSystem.disableBlend();
        }
    }


    private static float ratio(int available, int total) {
        if (total <= 0) return 0f;
        return Mth.clamp((float) available / (float) total, 0f, 1f);
    }

    private static float smooth(float current, float target, float factor) {
        return Mth.lerp(Mth.clamp(factor, 0f, 1f), current, target);
    }

    // 方便的居中绘制方法（不带辉光）
    public static void drawCenteredText(GuiGraphics gg, String text, int x, int y, int color) {
        float scale = 0.7f;
        /*if (scale == 1.0f) {
            gg.drawCenteredString(mc.font, Component.literal(text), x, y, color);
            return;
        }*/
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1);
        float inv = 1 / scale;
        gg.drawCenteredString(mc.font, Component.literal(text), (int)(x * inv), (int)(y * inv), color);
        gg.pose().popPose();
    }

}
