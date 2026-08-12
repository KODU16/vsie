package com.kodu16.vsie.content.item.IFF;

import com.kodu16.vsie.foundation.client.GuiTooltipHelper;
import com.kodu16.vsie.network.IFF.IFFC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.utility.ItemStackNbt;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("removal")
public class IFFScreen extends AbstractContainerScreen<IFFContainerMenu> {

    private EditBox editBoxA;
    private EditBox editBoxB;

    public IFFScreen(IFFContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        ItemStack stack = this.menu.itemStack;
        this.leftPos = this.width / 2 - this.imageWidth / 2;
        this.topPos = this.height / 2 - this.imageHeight / 2;

        this.editBoxA = new EditBox(this.font, this.leftPos + 68, this.topPos + 28, 62, 15,
                Component.translatable("gui.vsie.iff.enemy_prefix.tooltip"));
        this.editBoxA.setMaxLength(64);
        this.addRenderableWidget(this.editBoxA);

        this.editBoxB = new EditBox(this.font, this.leftPos + 68, this.topPos + 68, 62, 15,
                Component.translatable("gui.vsie.iff.ally_prefix.tooltip"));
        this.editBoxB.setMaxLength(64);
        this.addRenderableWidget(this.editBoxB);

        var tag = ItemStackNbt.get(stack);
        if (tag != null) {
            if (tag.contains("enemy")) {
                this.editBoxA.setValue(tag.getString("enemy"));
            }
            if (tag.contains("ally")) {
                this.editBoxB.setValue(tag.getString("ally"));
            }
        }

        int btnX = this.leftPos + 32;
        int btnY = this.topPos + 115;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.vsie.common.save"),
                        button -> saveAndClose())
                .bounds(btnX, btnY, 40, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.vsie.common.cancel"),
                        button -> this.minecraft.player.closeContainer())
                .bounds(btnX + 72, btnY, 40, 20)
                .build());
    }

    private void saveAndClose() {
        saveToNBT();
        this.minecraft.player.closeContainer();
    }

    private void saveToNBT() {
        String textA = editBoxA.getValue().trim();
        String textB = editBoxB.getValue().trim();
        ModNetworking.sendToServer(new IFFC2SPacket(textA, textB));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Function: the container parent draws background, widgets and slots in the correct order.
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderControlTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/iff/iff_gui.png");
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.iff.enemy_prefix.label"), this.titleLabelX + 32, this.titleLabelY + 25, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.iff.ally_prefix.label"), this.titleLabelX + 32, this.titleLabelY + 65, 0x404040, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveAndClose();
            return true;
        }
        if (this.editBoxA.keyPressed(keyCode, scanCode, modifiers)
                || this.editBoxB.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        // Function: once an IFF text box is focused, swallow non-edit keybinds like inventory-close so typing stays isolated.
        if (isEditingText()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.editBoxA.charTyped(codePoint, modifiers) || this.editBoxB.charTyped(codePoint, modifiers)) {
            return true;
        }
        // Function: focused IFF inputs must consume typed characters instead of letting other screen handlers see them.
        if (isEditingText()) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private boolean isEditingText() {
        return this.editBoxA.isFocused() || this.editBoxB.isFocused();
    }

    private void renderControlTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxA.getX(), this.editBoxA.getY(), this.editBoxA.getWidth(), this.editBoxA.getHeight(),
                Component.translatable("gui.vsie.iff.enemy_prefix.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxB.getX(), this.editBoxB.getY(), this.editBoxB.getWidth(), this.editBoxB.getHeight(),
                Component.translatable("gui.vsie.iff.ally_prefix.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 32, this.topPos + 115, 40, 20,
                Component.translatable("gui.vsie.iff.save.tooltip"))) {
            return;
        }
        GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 104, this.topPos + 115, 40, 20,
                Component.translatable("gui.vsie.iff.cancel.tooltip"));
    }
}
