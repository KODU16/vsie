package com.kodu16.vsie.content.misc.electromagnet_rail;

import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("removal")
public class ElectroMagnetRailCoreScreen extends AbstractContainerScreen<ElectroMagnetRailCoreContainerMenu> {

    private static final ResourceLocation IFF_BG_TEXTURE =
            new ResourceLocation(vsie.ID, "textures/gui/iff/iff_gui.png");

    public ElectroMagnetRailCoreScreen(ElectroMagnetRailCoreContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        // 复用 IFF 的 GUI 尺寸。
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 复用 IFF 背景贴图，满足“与 IFF 相同 GUI 背景”的需求。
        guiGraphics.blit(IFF_BG_TEXTURE,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        // 在 GUI 上显示当前存储的 rail 数量。
        guiGraphics.drawString(this.font,
                Component.literal("Rail: " + this.menu.getRailCount()),
                32, 18,
                0x404040,
                false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
