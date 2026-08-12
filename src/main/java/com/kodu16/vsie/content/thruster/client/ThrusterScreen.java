package com.kodu16.vsie.content.thruster.client;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.thruster.ThrusterContainerMenu;
import com.kodu16.vsie.foundation.client.GuiTooltipHelper;
import com.kodu16.vsie.network.thruster.ThrusterLimitC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ThrusterScreen extends AbstractContainerScreen<ThrusterContainerMenu> {
    private EditBox forcePercentBox;
    private EditBox torquePercentBox;

    public ThrusterScreen(ThrusterContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 116;
        this.inventoryLabelY = 1000;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Function: avoid a second container background pass that softens the finished GUI.
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderControlTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // Function: use a compact vanilla-style panel because thrusters only need two numeric controls.
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(this.leftPos + 3, this.topPos + 3, this.leftPos + this.imageWidth - 3, this.topPos + this.imageHeight - 3, 0xFFBDBDBD);
        guiGraphics.fill(this.leftPos + 8, this.topPos + 22, this.leftPos + this.imageWidth - 8, this.topPos + 78, 0xFFE0E0E0);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        AbstractThrusterBlockEntity thruster = menu.getBlockEntity();
        int forcePercent = readPercent(forcePercentBox, thruster.getForceLimitPercent());
        int torquePercent = readPercent(torquePercentBox, thruster.getTorqueLimitPercent());

        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.thruster.force_percent.label"), 14, 30, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.thruster.torque_percent.label"), 14, 52, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.thruster.force_output.label", coefficientText(thruster.getMaxThrust(), forcePercent)), 94, 30, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.thruster.torque_output.label", coefficientText(thruster.getMaxThrust(), torquePercent)), 94, 52, 0x404040, false);
    }

    @Override
    protected void init() {
        super.init();
        AbstractThrusterBlockEntity thruster = menu.getBlockEntity();
        this.forcePercentBox = createPercentBox(this.leftPos + 64, this.topPos + 28, thruster.getForceLimitPercent(), "gui.vsie.thruster.force_percent.tooltip");
        this.torquePercentBox = createPercentBox(this.leftPos + 64, this.topPos + 50, thruster.getTorqueLimitPercent(), "gui.vsie.thruster.torque_percent.tooltip");
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.common.save"), button -> saveAndClose())
                .bounds(this.leftPos + 38, this.topPos + 86, 42, 16)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.common.cancel"), button -> close())
                .bounds(this.leftPos + 88, this.topPos + 86, 52, 16)
                .build());
    }

    private EditBox createPercentBox(int x, int y, int value, String tooltipKey) {
        EditBox box = new EditBox(this.font, x, y, 24, 12, Component.translatable(tooltipKey));
        // Function: percent values are clamped on save, but short inputs keep editing predictable.
        box.setMaxLength(3);
        box.setValue(String.valueOf(value));
        this.addRenderableWidget(box);
        return box;
    }

    private void saveAndClose() {
        AbstractThrusterBlockEntity thruster = menu.getBlockEntity();
        int forcePercent = readPercent(forcePercentBox, thruster.getForceLimitPercent());
        int torquePercent = readPercent(torquePercentBox, thruster.getTorqueLimitPercent());
        thruster.setOutputLimitPercents(forcePercent, torquePercent);
        ModNetworking.sendToServer(new ThrusterLimitC2SPacket(thruster.getBlockPos(), forcePercent, torquePercent));
        close();
    }

    private void close() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
    }

    private int readPercent(EditBox box, int defaultValue) {
        if (box == null || box.getValue() == null || box.getValue().trim().isEmpty()) {
            return clampPercent(defaultValue);
        }
        try {
            return clampPercent(Integer.parseInt(box.getValue().trim()));
        } catch (NumberFormatException ignored) {
            return clampPercent(defaultValue);
        }
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String coefficientText(float maxThrust, int percent) {
        return String.format("%.0f", maxThrust * (percent / 100.0D));
    }

    private void renderControlTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.forcePercentBox.getX(), this.forcePercentBox.getY(), this.forcePercentBox.getWidth(), this.forcePercentBox.getHeight(),
                Component.translatable("gui.vsie.thruster.force_percent.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.torquePercentBox.getX(), this.torquePercentBox.getY(), this.torquePercentBox.getWidth(), this.torquePercentBox.getHeight(),
                Component.translatable("gui.vsie.thruster.torque_percent.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 38, this.topPos + 86, 42, 16,
                Component.translatable("gui.vsie.thruster.save.tooltip"))) {
            return;
        }
        GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 88, this.topPos + 86, 52, 16,
                Component.translatable("gui.vsie.thruster.cancel.tooltip"));
    }
}
