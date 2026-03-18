package com.kodu16.vsie.content.controlseat.gui;

import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("removal")
public class ControlSeatWarpScreen extends AbstractContainerScreen<ControlSeatWarpContainerMenu> {

    private static final ResourceLocation BG_TEXTURE =
            new ResourceLocation(vsie.ID, "textures/gui/controlseat/control_seat.png");

    public ControlSeatWarpScreen(ControlSeatWarpContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        // 功能：复用控制椅 GUI 背景，显示 27 格 warp data chip 专用仓位。
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        gg.blit(BG_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTicks);
        this.renderTooltip(gg, mouseX, mouseY);
    }
}
