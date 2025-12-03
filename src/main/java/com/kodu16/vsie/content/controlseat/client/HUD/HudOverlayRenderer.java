package com.kodu16.vsie.content.controlseat.client.HUD;

import com.kodu16.vsie.content.controlseat.server.SeatRegistry;
import com.kodu16.vsie.vsie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

@Mod.EventBusSubscriber(modid = "vsie", value = Dist.CLIENT)
public class HudOverlayRenderer {

    /*@SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player!=null && player.getVehicle().getType() == ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE) {
            // ✅ 获取座椅UUID
            BlockPos controlSeatPos = SeatRegistry.SEAT_TO_CONTROLSEAT.get(player.getVehicle().getUUID());
            if (controlSeatPos != null && mc.level != null) {
                BlockEntity controlseat = mc.level.getBlockEntity(controlSeatPos);
                if (controlseat != null) {
                    renderHUD(event.getGuiGraphics(), mc, controlseat);
                }
            }
        }
    }

    private static void renderHUD(GuiGraphics gui, Minecraft mc, BlockEntity controlseat) {
        int x = 10;
        int y = 10;
        int color = 0xFF00FF00; // ARGB：绿色
        Vec3 pos = Vec3.atCenterOf(controlseat.getBlockPos());
        String text = String.format("位置 [%.1f, %.1f, %.1f]", pos.x, pos.y, pos.z);

        gui.drawString(mc.font, text, x, y, color, true);
    }*/
}