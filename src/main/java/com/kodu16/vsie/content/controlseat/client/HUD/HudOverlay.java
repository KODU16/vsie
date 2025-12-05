package com.kodu16.vsie.content.controlseat.client.HUD;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.SeatRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HudOverlay {

    // 你可以把颜色做成配置项，这里先写死一个半透明青色
    private static int hudColor = 0xCC00FFFF; // ARGB
    // 透明度配置（建议后面改成 Config）
    private static final int TEXT_ALPHA    = 5;   // 主文字透明度
    private static final int SHADOW_ALPHA  = 110;   // 发光/阴影透明度

    private static final int MAIN_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0xFF, 0xFF);
    private static final int SUB_COLOR  = FastColor.ARGB32.color(TEXT_ALPHA, 0x99, 0xFF, 0xFF);

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

            GuiGraphics gg = event.getGuiGraphics();
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int centerX = sw / 2;
            int baseY = sh / 6; // 稍微再往上提一点，避免挡准心太严重

            // 标题 - 粗体 + 青色
            drawCenteredText(gg, "§l§b控制座椅", centerX, baseY, MAIN_COLOR);

            // 坐标
            Vec3 pos = controlseat.getBlockPos().getCenter();
            String coord = String.format("§a%.1f §b%.1f §c%.1f", pos.x, pos.y, pos.z);
            drawCenteredText(gg, coord, centerX, baseY + 18, SUB_COLOR);

            // 方块名称
            String blockName = controlseat.getBlockState().getBlock().getName().getString();
            drawCenteredText(gg, "§e" + blockName, centerX, baseY + 34, SUB_COLOR);

            //油门
            int throttle = controlseat.getThrottle();
            ThrottleIndicator.renderThrottleIndicator(gg, throttle);

            RenderSystem.disableBlend();
        }
    }


    // 方便的居中绘制方法（不带辉光）
    private static void drawCenteredText(GuiGraphics gg, String text, int x, int y, int color) {
        gg.drawCenteredString(Minecraft.getInstance().font, Component.literal(text), x, y, color);
    }
}
