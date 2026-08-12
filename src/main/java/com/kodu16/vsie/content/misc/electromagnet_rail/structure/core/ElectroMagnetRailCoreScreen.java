package com.kodu16.vsie.content.misc.electromagnet_rail.structure.core;

import com.kodu16.vsie.foundation.client.GuiTooltipHelper;
import com.kodu16.vsie.network.rail.ElectroMagnetRailCoreDetectC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("removal")
public class ElectroMagnetRailCoreScreen extends AbstractContainerScreen<ElectroMagnetRailCoreContainerMenu> {

    private static final ResourceLocation IFF_BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/iff/iff_gui.png");
    private static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/slot.png");

    public ElectroMagnetRailCoreScreen(ElectroMagnetRailCoreContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.rail_core.detect.label"),
                        button -> ModNetworking.sendToServer(new ElectroMagnetRailCoreDetectC2SPacket(this.menu.getBlockPosition())))
                .pos(this.leftPos + 94, this.topPos + 34)
                .size(56, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(IFF_BG_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        int coreStartX = this.leftPos + 28 - 1;
        int coreStartY = this.topPos + 32 - 1;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                guiGraphics.blit(SLOT_TEXTURE, coreStartX + col * 18, coreStartY + row * 18, 0, 0, 18, 18, 18, 18);
            }
        }

        int playerInvStartX = this.leftPos + 8 - 1;
        int playerInvStartY = this.topPos + 84 - 1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                guiGraphics.blit(SLOT_TEXTURE, playerInvStartX + col * 18, playerInvStartY + row * 18, 0, 0, 18, 18, 18, 18);
            }
        }

        int hotbarY = this.topPos + 142 - 1;
        for (int col = 0; col < 9; col++) {
            guiGraphics.blit(SLOT_TEXTURE, playerInvStartX + col * 18, hotbarY, 0, 0, 18, 18, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.rail_core.rail_count.label", this.menu.getRailCount()), this.leftPos + 28, this.topPos + 60, 0x404040, false);
        int effectiveRailLength = this.menu.getEffectiveRailLength();
        String lengthText = effectiveRailLength > 0 ? String.valueOf(effectiveRailLength) : Component.translatable("gui.vsie.common.na").getString();
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.rail_core.length.label", lengthText), this.leftPos + 28, this.topPos + 70, 0x404040, false);
        drawTerminalMessage(guiGraphics);
    }

    private void drawTerminalMessage(GuiGraphics guiGraphics) {
        int status = this.menu.getTerminalStatus();
        if (status == ElectroMagnetRailCoreBlockEntity.TERMINAL_STATUS_IDLE) {
            return;
        }

        Component text = null;
        int color = 0xCC5555;
        if (status == ElectroMagnetRailCoreBlockEntity.TERMINAL_STATUS_FOUND) {
            text = Component.translatable("gui.vsie.rail_core.terminal_found", this.menu.getTerminalPos().toShortString());
            color = 0x00CC77;
        } else if (status == ElectroMagnetRailCoreBlockEntity.TERMINAL_STATUS_FACING_ERROR) {
            text = Component.translatable("gui.vsie.rail_core.terminal_facing_error", this.menu.getTerminalPos().toShortString());
        } else if (status == ElectroMagnetRailCoreBlockEntity.TERMINAL_STATUS_BLOCKED) {
            text = Component.translatable("gui.vsie.rail_core.terminal_blocked", this.menu.getTerminalPos().toShortString());
        } else if (status == ElectroMagnetRailCoreBlockEntity.TERMINAL_STATUS_NOT_FOUND) {
            text = Component.translatable("gui.vsie.rail_core.terminal_not_found", this.menu.getTerminalPos().toShortString());
        }
        if (text == null) {
            return;
        }

        float scale = 0.5F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.drawString(this.font, text, (int) (86 / scale), (int) (58 / scale), color, false);
        guiGraphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Function: avoid rendering the container background twice in one frame.
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderControlTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderControlTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 94, this.topPos + 34, 56, 20,
                Component.translatable("gui.vsie.rail_core.detect.tooltip"))) {
            return;
        }
        if ((this.hoveredSlot == null || !this.hoveredSlot.hasItem())
                && GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 27, this.topPos + 31, 36, 36,
                Component.translatable("gui.vsie.rail_core.core_slots.tooltip"))) {
            return;
        }
        GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 7, this.topPos + 83, 162, 76,
                Component.translatable("gui.vsie.rail_core.player_inventory.tooltip"));
    }
}
