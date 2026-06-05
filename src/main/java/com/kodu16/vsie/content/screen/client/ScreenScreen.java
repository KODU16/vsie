package com.kodu16.vsie.content.screen.client;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.screen.server.ScreenContainerMenu;
import com.kodu16.vsie.foundation.client.GuiTooltipHelper;
import com.kodu16.vsie.network.screen.ScreenC2SPacket;
import com.kodu16.vsie.network.screen.ScreentypeC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("removal")
public class ScreenScreen extends AbstractContainerScreen<ScreenContainerMenu> {
    private static final int MODE_BUTTON_X = 56;
    private static final int MODE_BUTTON_Y = 46;
    private static final int MODE_BUTTON_WIDTH = 64;
    private static final int MODE_BUTTON_HEIGHT = 16;
    private static final int MODE_ICON_X = 78;
    private static final int MODE_ICON_Y = 18;
    private static final int ACTION_BUTTON_X = 32;
    private static final int ACTION_BUTTON_Y = 136;
    private static final int ACTION_BUTTON_WIDTH = 40;
    private static final int ACTION_BUTTON_HEIGHT = 20;

    private EditBox editBoxSpinX;
    private EditBox editBoxSpinY;
    private EditBox editBoxOffsetX;
    private EditBox editBoxOffsetY;
    private EditBox editBoxOffsetZ;
    private Button modeButton;

    public ScreenScreen(ScreenContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = this.width / 2 - this.imageWidth / 2;
        this.topPos = this.height / 2 - this.imageHeight / 2;

        var be = this.menu.getBlockEntity();
        this.editBoxSpinX = createIntEditBox("spinx", 28, 88, String.valueOf(be.spinx));
        this.editBoxSpinY = createIntEditBox("spiny", 68, 88, String.valueOf(be.spiny));
        this.editBoxOffsetX = createIntEditBox("offsetx", 28, 108, String.valueOf(be.offsetx));
        this.editBoxOffsetY = createIntEditBox("offsety", 68, 108, String.valueOf(be.offsety));
        this.editBoxOffsetZ = createIntEditBox("offsetz", 108, 108, String.valueOf(be.offsetz));

        setIntegerOnly(this.editBoxSpinX);
        setIntegerOnly(this.editBoxSpinY);
        setIntegerOnly(this.editBoxOffsetX);
        setIntegerOnly(this.editBoxOffsetY);
        setIntegerOnly(this.editBoxOffsetZ);

        int btnX = this.leftPos + ACTION_BUTTON_X;
        int btnY = this.topPos + ACTION_BUTTON_Y;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.vsie.common.save"),
                        button -> saveAndClose())
                .bounds(btnX, btnY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.vsie.common.cancel"),
                        button -> this.minecraft.player.closeContainer())
                .bounds(btnX + 72, btnY, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
                .build());

        BlockPos pos = menu.getBlockEntity().getBlockPos();
        this.modeButton = this.addRenderableWidget(Button.builder(
                        getModeLabel(menu.getBlockEntity().displaytype),
                        button -> {
                            int nextMode = (menu.getBlockEntity().displaytype + 1) % 2;
                            ModNetworking.sendToServer(new ScreentypeC2SPacket(pos, nextMode));
                            button.setMessage(getModeLabel(nextMode));
                        })
                .pos(this.leftPos + MODE_BUTTON_X, this.topPos + MODE_BUTTON_Y)
                .size(MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)
                .build());
    }

    private EditBox createIntEditBox(String keySuffix, int x, int y, String initialValue) {
        EditBox box = new EditBox(this.font, this.leftPos + x, this.topPos + y, 24, 14,
                Component.translatable("gui.vsie.screen." + keySuffix + ".tooltip"));
        // Function: screen transform inputs stay integer-only so the renderer reads stable mount offsets.
        box.setMaxLength(8);
        box.setValue(initialValue);
        box.setFocused(false);
        this.addRenderableWidget(box);
        return box;
    }

    private void setIntegerOnly(EditBox box) {
        box.setFilter(s -> {
            if (s.isEmpty() || s.equals("-")) {
                return true;
            }
            try {
                Integer.parseInt(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    private void saveAndClose() {
        int spinX = safeParseInt(editBoxSpinX.getValue(), 0);
        int spinY = safeParseInt(editBoxSpinY.getValue(), 0);
        int offsetX = safeParseInt(editBoxOffsetX.getValue(), 0);
        int offsetY = safeParseInt(editBoxOffsetY.getValue(), 0);
        int offsetZ = safeParseInt(editBoxOffsetZ.getValue(), 0);
        var be = this.menu.getBlockEntity();
        be.spinx = spinX;
        be.spiny = spinY;
        be.offsetx = offsetX;
        be.offsety = offsetY;
        be.offsetz = offsetZ;
        ModNetworking.sendToServer(new ScreenC2SPacket(be.getBlockPos(), spinX, spinY, offsetX, offsetY, offsetZ));
        this.minecraft.player.closeContainer();
    }

    private int safeParseInt(String text, int defaultValue) {
        if (text == null || text.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Function: keep the mode button text aligned with the current synced display mode while this GUI stays open.
        this.modeButton.setMessage(getModeLabel(this.menu.getBlockEntity().displaytype));
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderControlTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        AbstractScreenBlockEntity screen = menu.getBlockEntity();
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/iff/iff_gui.png");
        ResourceLocation iconRadar = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/screen/screentype_radar.png");
        ResourceLocation iconServerInfo = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/screen/screentype_serverinfo.png");
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        guiGraphics.blit(screen.displaytype == 0 ? iconRadar : iconServerInfo, this.leftPos + MODE_ICON_X, this.topPos + MODE_ICON_Y, 0, 0, 20, 20, 20, 20);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.screen.spinx.label"), this.leftPos + 12, this.topPos + 68, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.screen.spiny.label"), this.leftPos + 52, this.topPos + 68, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.screen.offset.label"), this.leftPos + 12, this.topPos + 96, 0x404040, false);
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
        return this.editBoxSpinX.keyPressed(keyCode, scanCode, modifiers)
                || this.editBoxSpinY.keyPressed(keyCode, scanCode, modifiers)
                || this.editBoxOffsetX.keyPressed(keyCode, scanCode, modifiers)
                || this.editBoxOffsetY.keyPressed(keyCode, scanCode, modifiers)
                || this.editBoxOffsetZ.keyPressed(keyCode, scanCode, modifiers)
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderControlTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        EditBox[] boxes = {editBoxSpinX, editBoxSpinY, editBoxOffsetX, editBoxOffsetY, editBoxOffsetZ};
        String[] keys = {
                "gui.vsie.screen.spinx.tooltip",
                "gui.vsie.screen.spiny.tooltip",
                "gui.vsie.screen.offsetx.tooltip",
                "gui.vsie.screen.offsety.tooltip",
                "gui.vsie.screen.offsetz.tooltip"
        };
        for (int i = 0; i < boxes.length; i++) {
            if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                    boxes[i].getX(), boxes[i].getY(), boxes[i].getWidth(), boxes[i].getHeight(),
                    Component.translatable(keys[i]))) {
                return;
            }
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + ACTION_BUTTON_X, this.topPos + ACTION_BUTTON_Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT,
                Component.translatable("gui.vsie.screen.save.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + ACTION_BUTTON_X + 72, this.topPos + ACTION_BUTTON_Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT,
                Component.translatable("gui.vsie.screen.cancel.tooltip"))) {
            return;
        }
        GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + MODE_BUTTON_X, this.topPos + MODE_BUTTON_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT,
                Component.translatable("gui.vsie.screen.switch_mode.tooltip", getModeLabel(this.menu.getBlockEntity().displaytype)));
    }

    private Component getModeLabel(int displayType) {
        return Component.translatable(displayType == 0
                ? "gui.vsie.screen.switch_mode.radar"
                : "gui.vsie.screen.switch_mode.server_info");
    }
}
