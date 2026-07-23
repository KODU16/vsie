package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.TurretContainerMenu;
import com.kodu16.vsie.content.turret.ciws.AbstractCIWSBlockEntity;
import com.kodu16.vsie.foundation.client.GuiTooltipHelper;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
import com.kodu16.vsie.network.turret.TurretDefaultSpinC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("removal")
public class TurretScreen extends AbstractContainerScreen<TurretContainerMenu> {
    private static final int BASE_SCREEN_WIDTH = 176;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/turret_gui.png");
    private static final ResourceLocation AMMO_TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/turret_gui_ammo.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/slot.png");
    private static final int BREAK_BLOCKS_LABEL_X = 40;
    private static final int BREAK_BLOCKS_LABEL_Y = 124;
    private static final int BREAK_BLOCKS_BUTTON_X = 112;
    private static final int BREAK_BLOCKS_BUTTON_Y = 120;
    private static final int AMMO_PANEL_TOP = 154;
    private static final int AMMO_PANEL_BOTTOM = 183;
    private static final int INVENTORY_PANEL_TOP = 190;
    private static final int INVENTORY_PANEL_BOTTOM = 280;
    private static final int AMMO_SCREEN_WIDTH = 228;
    private static final int AMMO_SCREEN_HEIGHT = 288;
    private static final int AIM_LEFT_BOX_X = 38;
    private static final int AIM_RIGHT_BOX_X = 104;
    private static final int AIM_INPUT_TOP_Y = 22;
    private static final int AIM_INPUT_BOTTOM_Y = 44;
    private static final int AIM_INPUT_WIDTH = 34;
    // Function: labels need a full font-height gap so they do not render into compact input boxes.
    private static final int LABEL_GAP_Y = 10;
    private static final int SPIN_Y_BOX_X = 38;
    private static final int SPIN_X_BOX_X = 104;
    private static final int SPIN_INPUT_Y = 68;
    private static final int SPIN_INPUT_WIDTH = 34;
    private static final int TARGET_ICON_HOSTILE_X = 20;
    private static final int TARGET_ICON_PASSIVE_X = 59;
    private static final int TARGET_ICON_PLAYER_X = 98;
    private static final int TARGET_ICON_SHIP_X = 137;
    private static final int TARGET_ICON_Y = 84;
    private static final int TARGET_BUTTON_Y = 104;
    private static final int TARGET_BUTTON_HOSTILE_X = 20;
    private static final int TARGET_BUTTON_PASSIVE_X = 53;
    private static final int TARGET_BUTTON_PLAYER_X = 86;
    private static final int TARGET_BUTTON_SHIP_X = 129;
    private static final int TARGET_BUTTON_HOSTILE_WIDTH = 27;
    private static final int TARGET_BUTTON_PASSIVE_WIDTH = 27;
    private static final int TARGET_BUTTON_PLAYER_WIDTH = 37;
    private static final int TARGET_BUTTON_SHIP_WIDTH = 27;
    private static final int TARGET_BUTTON_HEIGHT = 15;
    // Function: action buttons sit below the break-block row while staying above the optional ammo strip.
    private static final int ACTION_BUTTON_Y = 136;
    private static final int SAVE_BUTTON_X = 46;
    private static final int SAVE_BUTTON_WIDTH = 34;
    private static final int CANCEL_BUTTON_X = 88;
    private static final int CANCEL_BUTTON_WIDTH = 42;
    private static final int ACTION_BUTTON_HEIGHT = 16;

    private EditBox editBoxSpinX;
    private EditBox editBoxSpinY;
    private EditBox editBoxAimLimitMinX;
    private EditBox editBoxAimLimitMaxX;
    private EditBox editBoxAimLimitMinY;
    private EditBox editBoxAimLimitMaxY;
    private Button breakBlocksButton;

    public TurretScreen(TurretContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // Function: ammo screens are wider while turret controls stay centered in the original 176 px panel.
        this.imageWidth = menu.hasAmmoSlots() ? AMMO_SCREEN_WIDTH : 176;
        this.imageHeight = menu.hasAmmoSlots() ? AMMO_SCREEN_HEIGHT : 166;
        this.inventoryLabelY = 1000;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderControlTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        AbstractTurretBlockEntity turret = menu.getBlockEntity();
        if (menu.hasAmmoSlots()) {
            // Function: ammo turrets use a taller baked background so the player inventory fits without ad-hoc fills.
            guiGraphics.blit(AMMO_TEXTURE, this.leftPos, this.topPos, 0, 0,
                    this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        } else {
            guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, 166);
        }
        if (menu.hasAmmoSlots()) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            drawAmmoSlots(guiGraphics);
            drawPlayerInventorySlots(guiGraphics);
        }

        ResourceLocation iconHostile = turret.getData().isTargetsHostile()
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_hostile_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_hostile_off.png");
        ResourceLocation iconPassive = turret.getData().isTargetsPassive()
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_passive_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_passive_off.png");
        ResourceLocation iconPlayer = turret.getData().isTargetsPlayers()
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_players_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_players_off.png");
        ResourceLocation iconShip = turret.getData().isTargetsShip()
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_ship_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_ship_off.png");
        ResourceLocation iconCiws = turret.getData().isTargetsShip()
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_ciws_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/target_ciws_off.png");

        guiGraphics.blit(iconHostile, this.leftPos + controlOffsetX() + TARGET_ICON_HOSTILE_X, this.topPos + TARGET_ICON_Y, 0, 0, 19, 19, 19, 19);
        guiGraphics.blit(iconPassive, this.leftPos + controlOffsetX() + TARGET_ICON_PASSIVE_X, this.topPos + TARGET_ICON_Y, 0, 0, 19, 19, 19, 19);
        guiGraphics.blit(iconPlayer, this.leftPos + controlOffsetX() + TARGET_ICON_PLAYER_X, this.topPos + TARGET_ICON_Y, 0, 0, 19, 19, 19, 19);
        guiGraphics.blit(menu.getBlockEntity() instanceof AbstractCIWSBlockEntity ? iconCiws : iconShip,
                this.leftPos + controlOffsetX() + TARGET_ICON_SHIP_X, this.topPos + TARGET_ICON_Y, 0, 0, 19, 19, 19, 19);
    }

    private void drawAmmoSlots(GuiGraphics guiGraphics) {
        // Function: non-energy turrets display exactly one 9-slot ammo row.
        int slotStartX = this.leftPos + TurretContainerMenu.INTERNAL_SLOT_X - 1;
        int slotStartY = this.topPos + TurretContainerMenu.INTERNAL_SLOT_Y - 1;
        for (int col = 0; col < TurretContainerMenu.INTERNAL_SLOT_COUNT; col++) {
            guiGraphics.blit(SLOT_TEXTURE, slotStartX + col * 18, slotStartY, 0, 0, 18, 18, 18, 18);
        }
    }

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                guiGraphics.blit(SLOT_TEXTURE,
                        this.leftPos + TurretContainerMenu.PLAYER_INVENTORY_X + col * 18 - 1,
                        this.topPos + TurretContainerMenu.PLAYER_INVENTORY_Y + row * 18 - 1,
                        0, 0, 18, 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            guiGraphics.blit(SLOT_TEXTURE,
                    this.leftPos + TurretContainerMenu.PLAYER_INVENTORY_X + col * 18 - 1,
                    this.topPos + TurretContainerMenu.PLAYER_HOTBAR_Y - 1,
                    0, 0, 18, 18, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        if (menu.getBlockEntity().supportsBlockDestructionToggle()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.vsie.common.break_blocks.label"),
                    controlOffsetX() + BREAK_BLOCKS_LABEL_X, BREAK_BLOCKS_LABEL_Y, 0x404040, false);
        }
        drawLabelAboveBox(guiGraphics, this.editBoxAimLimitMinX, Component.translatable("gui.vsie.turret.aim_min_x.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxAimLimitMaxX, Component.translatable("gui.vsie.turret.aim_max_x.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxAimLimitMinY, Component.translatable("gui.vsie.turret.aim_min_y.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxAimLimitMaxY, Component.translatable("gui.vsie.turret.aim_max_y.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxSpinY, Component.translatable("gui.vsie.turret.default_yaw.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxSpinX, Component.translatable("gui.vsie.turret.default_pitch.label"));
        if (menu.hasAmmoSlots()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.vsie.common.ammo"), 8, AMMO_PANEL_TOP, 0x404040, false);
            guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 8, INVENTORY_PANEL_TOP, 0x404040, false);
        }
    }

    @Override
    protected void init() {
        super.init();
        BlockPos pos = menu.getBlockEntity().getBlockPos();
        AbstractTurretBlockEntity be = this.menu.getBlockEntity();
        this.editBoxAimLimitMinX = createIntEditBox("AimLimitMinX", this.leftPos + controlOffsetX() + AIM_LEFT_BOX_X, this.topPos + AIM_INPUT_TOP_Y, AIM_INPUT_WIDTH, String.valueOf(be.getData().aimLimitMinX));
        this.editBoxAimLimitMaxX = createIntEditBox("AimLimitMaxX", this.leftPos + controlOffsetX() + AIM_RIGHT_BOX_X, this.topPos + AIM_INPUT_TOP_Y, AIM_INPUT_WIDTH, String.valueOf(be.getData().aimLimitMaxX));
        this.editBoxAimLimitMinY = createIntEditBox("AimLimitMinY", this.leftPos + controlOffsetX() + AIM_LEFT_BOX_X, this.topPos + AIM_INPUT_BOTTOM_Y, AIM_INPUT_WIDTH, String.valueOf(be.getData().aimLimitMinY));
        this.editBoxAimLimitMaxY = createIntEditBox("AimLimitMaxY", this.leftPos + controlOffsetX() + AIM_RIGHT_BOX_X, this.topPos + AIM_INPUT_BOTTOM_Y, AIM_INPUT_WIDTH, String.valueOf(be.getData().aimLimitMaxY));
        this.editBoxSpinY = createIntEditBox("SpinY", this.leftPos + controlOffsetX() + SPIN_Y_BOX_X, this.topPos + SPIN_INPUT_Y, SPIN_INPUT_WIDTH, String.valueOf(be.defaultspiny));
        this.editBoxSpinX = createIntEditBox("SpinX", this.leftPos + controlOffsetX() + SPIN_X_BOX_X, this.topPos + SPIN_INPUT_Y, SPIN_INPUT_WIDTH, String.valueOf(be.defaultspinx));

        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.turret.hostile.label"),
                        button -> ModNetworking.sendToServer(new TurretC2SPacket(pos, 1)))
                .pos(this.leftPos + controlOffsetX() + TARGET_BUTTON_HOSTILE_X, this.topPos + TARGET_BUTTON_Y)
                .size(TARGET_BUTTON_HOSTILE_WIDTH, TARGET_BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.turret.passive.label"),
                        button -> ModNetworking.sendToServer(new TurretC2SPacket(pos, 2)))
                .pos(this.leftPos + controlOffsetX() + TARGET_BUTTON_PASSIVE_X, this.topPos + TARGET_BUTTON_Y)
                .size(TARGET_BUTTON_PASSIVE_WIDTH, TARGET_BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.turret.player.label"),
                        button -> ModNetworking.sendToServer(new TurretC2SPacket(pos, 3)))
                .pos(this.leftPos + controlOffsetX() + TARGET_BUTTON_PLAYER_X, this.topPos + TARGET_BUTTON_Y)
                .size(TARGET_BUTTON_PLAYER_WIDTH, TARGET_BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.turret.ship.label"),
                        button -> ModNetworking.sendToServer(new TurretC2SPacket(pos, 4)))
                .pos(this.leftPos + controlOffsetX() + TARGET_BUTTON_SHIP_X, this.topPos + TARGET_BUTTON_Y)
                .size(TARGET_BUTTON_SHIP_WIDTH, TARGET_BUTTON_HEIGHT)
                .build());
        if (be.supportsBlockDestructionToggle()) {
            this.breakBlocksButton = this.addRenderableWidget(Button.builder(breakBlocksButtonLabel(),
                            button -> {
                                ModNetworking.sendToServer(new TurretC2SPacket(pos, 5));
                                be.toggleBreaksBlocksEnabled();
                                updateBreakBlocksButtonLabel();
                            })
                    .bounds(this.leftPos + controlOffsetX() + BREAK_BLOCKS_BUTTON_X, this.topPos + BREAK_BLOCKS_BUTTON_Y, 20, 14)
                    .build());
        }
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.common.save"),
                        button -> saveAndClose())
                .bounds(this.leftPos + controlOffsetX() + SAVE_BUTTON_X, this.topPos + ACTION_BUTTON_Y, SAVE_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.common.cancel"),
                        button -> this.minecraft.player.closeContainer())
                .bounds(this.leftPos + controlOffsetX() + CANCEL_BUTTON_X, this.topPos + ACTION_BUTTON_Y, CANCEL_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
                .build());
    }

    private EditBox createIntEditBox(String name, int x, int y, int width, String initialValue) {
        EditBox box = new EditBox(this.font, x, y, width, 10, Component.translatable("gui.vsie.turret." + name.toLowerCase() + ".tooltip"));
        // Function: keep turret setting inputs compact and limited to small signed integers.
        box.setMaxLength(8);
        box.setValue(initialValue);
        box.setFocused(false);
        this.addRenderableWidget(box);
        return box;
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

    private void saveAndClose() {
        int spinX = safeParseInt(editBoxSpinX.getValue(), 0);
        int spinY = safeParseInt(editBoxSpinY.getValue(), 0);
        int aimLimitMinX = safeParseInt(editBoxAimLimitMinX.getValue(), -180);
        int aimLimitMaxX = safeParseInt(editBoxAimLimitMaxX.getValue(), 180);
        int aimLimitMinY = safeParseInt(editBoxAimLimitMinY.getValue(), -180);
        int aimLimitMaxY = safeParseInt(editBoxAimLimitMaxY.getValue(), 180);
        AbstractTurretBlockEntity be = this.menu.getBlockEntity();
        be.defaultspinx = spinX;
        be.defaultspiny = spinY;
        be.setAimLimits(aimLimitMinX, aimLimitMaxX, aimLimitMinY, aimLimitMaxY);
        ModNetworking.sendToServer(new TurretDefaultSpinC2SPacket(
                menu.getBlockEntity().getBlockPos(),
                spinX,
                spinY,
                aimLimitMinX,
                aimLimitMaxX,
                aimLimitMinY,
                aimLimitMaxY
        ));
        this.minecraft.player.closeContainer();
    }

    private void renderControlTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.breakBlocksButton != null
                && GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + controlOffsetX() + BREAK_BLOCKS_LABEL_X, this.topPos + BREAK_BLOCKS_BUTTON_Y, 96, 16,
                Component.translatable("gui.vsie.common.break_blocks.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxAimLimitMinX.getX(), this.editBoxAimLimitMinX.getY(), this.editBoxAimLimitMinX.getWidth(), this.editBoxAimLimitMinX.getHeight(),
                Component.translatable("gui.vsie.turret.aimlimitminx.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxAimLimitMaxX.getX(), this.editBoxAimLimitMaxX.getY(), this.editBoxAimLimitMaxX.getWidth(), this.editBoxAimLimitMaxX.getHeight(),
                Component.translatable("gui.vsie.turret.aimlimitmaxx.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxAimLimitMinY.getX(), this.editBoxAimLimitMinY.getY(), this.editBoxAimLimitMinY.getWidth(), this.editBoxAimLimitMinY.getHeight(),
                Component.translatable("gui.vsie.turret.aimlimitminy.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxAimLimitMaxY.getX(), this.editBoxAimLimitMaxY.getY(), this.editBoxAimLimitMaxY.getWidth(), this.editBoxAimLimitMaxY.getHeight(),
                Component.translatable("gui.vsie.turret.aimlimitmaxy.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxSpinY.getX(), this.editBoxSpinY.getY(), this.editBoxSpinY.getWidth(), this.editBoxSpinY.getHeight(),
                Component.translatable("gui.vsie.turret.spiny.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxSpinX.getX(), this.editBoxSpinX.getY(), this.editBoxSpinX.getWidth(), this.editBoxSpinX.getHeight(),
                Component.translatable("gui.vsie.turret.spinx.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + controlOffsetX() + TARGET_BUTTON_HOSTILE_X, this.topPos + TARGET_BUTTON_Y,
                TARGET_BUTTON_HOSTILE_WIDTH, TARGET_BUTTON_HEIGHT, Component.translatable("gui.vsie.turret.hostile.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + controlOffsetX() + TARGET_BUTTON_PASSIVE_X, this.topPos + TARGET_BUTTON_Y,
                TARGET_BUTTON_PASSIVE_WIDTH, TARGET_BUTTON_HEIGHT, Component.translatable("gui.vsie.turret.passive.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + controlOffsetX() + TARGET_BUTTON_PLAYER_X, this.topPos + TARGET_BUTTON_Y,
                TARGET_BUTTON_PLAYER_WIDTH, TARGET_BUTTON_HEIGHT, Component.translatable("gui.vsie.turret.player.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + controlOffsetX() + TARGET_BUTTON_SHIP_X, this.topPos + TARGET_BUTTON_Y,
                TARGET_BUTTON_SHIP_WIDTH, TARGET_BUTTON_HEIGHT, Component.translatable("gui.vsie.turret.ship.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + controlOffsetX() + SAVE_BUTTON_X, this.topPos + ACTION_BUTTON_Y,
                SAVE_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT, Component.translatable("gui.vsie.turret.save.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + controlOffsetX() + CANCEL_BUTTON_X, this.topPos + ACTION_BUTTON_Y,
                CANCEL_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT, Component.translatable("gui.vsie.turret.cancel.tooltip"))) {
            return;
        }
        if (menu.hasAmmoSlots() && (this.hoveredSlot == null || !this.hoveredSlot.hasItem())) {
            GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 4, this.topPos + AMMO_PANEL_TOP, this.imageWidth - 8, AMMO_PANEL_BOTTOM - AMMO_PANEL_TOP,
                    Component.translatable("gui.vsie.turret.ammo_slots.tooltip"));
        }
    }

    private Component breakBlocksButtonLabel() {
        return Component.literal(menu.getBlockEntity().breaksBlocksEnabled() ? "[x]" : "[ ]");
    }

    private void updateBreakBlocksButtonLabel() {
        if (this.breakBlocksButton != null) {
            this.breakBlocksButton.setMessage(breakBlocksButtonLabel());
        }
    }

    private void drawLabelAboveBox(GuiGraphics guiGraphics, EditBox box, Component label) {
        if (box == null) {
            return;
        }
        int labelX = box.getX() + (box.getWidth() - this.font.width(label)) / 2 - this.leftPos;
        int labelY = box.getY() - this.topPos - LABEL_GAP_Y;
        guiGraphics.drawString(this.font, label, labelX, labelY, 0x404040, false);
    }

    private int controlOffsetX() {
        return (this.imageWidth - BASE_SCREEN_WIDTH) / 2;
    }
}
