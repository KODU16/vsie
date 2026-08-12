package com.kodu16.vsie.content.turret.heavyturret;

import com.kodu16.vsie.foundation.client.GuiTooltipHelper;
import com.kodu16.vsie.network.turret.HeavyTurretC2SPacket;
import com.kodu16.vsie.network.turret.TurretDefaultSpinC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

@SuppressWarnings("removal")
public class HeavyTurretScreen extends AbstractContainerScreen<HeavyTurretContainerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/turret_gui.png");
    private static final ResourceLocation HEAVY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/heavyturret/heavy_turret_gui.png");
    private static final ResourceLocation HEAVY_AMMO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/heavyturret/heavy_turret_gui_ammo.png");
    private static final ResourceLocation ICON_MANUAL =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/heavyturret/target_manual.png");
    private static final ResourceLocation ICON_AUTO =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/heavyturret/target_auto.png");
    private static final ResourceLocation ICON_SMART =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/heavyturret/target_smart.png");
    private static final Component MANUAL_DISABLED_MESSAGE = Component.translatable("gui.vsie.heavy_turret.mode_disabled");
    private static final int FIRE_TYPE_MANUAL = 0;
    private static final int FIRE_TYPE_AUTO = 1;
    private static final int FIRE_TYPE_SMART = 2;

    private static final int LEFT_PANEL_X = 16;
    private static final int RIGHT_PANEL_X = 136;
    private static final int FIELD_WIDTH = 36;
    private static final int FIELD_HEIGHT = 14;
    private static final int FIELD_LEFT_X = 24;
    private static final int FIELD_RIGHT_X = 88;
    private static final int ROW_X_Y = 38;
    private static final int ROW_Y_Y = 68;
    private static final int ROW_SPIN_Y = 98;
    private static final int LABEL_GAP_Y = 10;
    private static final int CHANNEL_ICON_LEFT_X = 142;
    private static final int CHANNEL_ICON_RIGHT_X = 180;
    private static final int CHANNEL_TOP_ROW_Y = 30;
    private static final int CHANNEL_BOTTOM_ROW_Y = 68;
    private static final int CHANNEL_BUTTON_LEFT_X = 134;
    private static final int CHANNEL_BUTTON_RIGHT_X = 172;
    private static final int CHANNEL_BUTTON_TOP_Y = 48;
    private static final int CHANNEL_BUTTON_BOTTOM_Y = 86;
    private static final int CHANNEL_BUTTON_WIDTH = 28;
    private static final int MODE_LABEL_X = 128;
    private static final int MODE_LABEL_Y = 138;
    private static final int MODE_ICON_X = 152;
    private static final int MODE_ICON_Y = 132;
    private static final int MODE_BUTTON_X = 176;
    private static final int MODE_BUTTON_Y = 134;
    private static final int MODE_BUTTON_WIDTH = 38;
    private static final int AMMO_LABEL_Y = 178;
    private static final int SAVE_BUTTON_X = 74;
    private static final int CANCEL_BUTTON_X = 116;
    private static final int SAVE_BUTTON_WIDTH = 34;
    private static final int CANCEL_BUTTON_WIDTH = 38;
    private static final int ACTION_BUTTON_Y = 160;
    private static final int LABEL_COLOR = 0x404040;
    private static final int NOTICE_COLOR = 0xE02020;
    private static final int SLOT_BORDER_COLOR = 0xFF8B8B8B;
    private static final int SLOT_FILL_COLOR = 0xFF373737;
    private static final int BREAK_BLOCKS_LABEL_X = 18;
    private static final int BREAK_BLOCKS_LABEL_Y = 138;
    private static final int BREAK_BLOCKS_BUTTON_X = 86;
    private static final int BREAK_BLOCKS_BUTTON_Y = 134;
    private static final int PLAYER_INVENTORY_LABEL_Y = 224;
    private static final int NOTICE_WIDTH = 144;
    private static final int HEAVY_SCREEN_WIDTH = 228;
    private static final int AMMO_SCREEN_HEIGHT = 316;

    private EditBox editBoxSpinX;
    private EditBox editBoxSpinY;
    private EditBox editBoxAimLimitMinX;
    private EditBox editBoxAimLimitMaxX;
    private EditBox editBoxAimLimitMinY;
    private EditBox editBoxAimLimitMaxY;
    private Button breakBlocksButton;
    private int manualDisabledNoticeTicks = 0;

    public HeavyTurretScreen(HeavyTurretContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // Function: leave room for the optional shared ammo row and keep the heavy turret controls separated.
        // Function: heavy turret screens need extra width because they combine aim, channel, mode, and ammo controls.
        this.imageWidth = HEAVY_SCREEN_WIDTH;
        this.imageHeight = menu.hasAmmoSlots() ? AMMO_SCREEN_HEIGHT : 238;
        this.inventoryLabelY = this.imageHeight + 1000;
    }

    @Override
    protected void init() {
        super.init();
        BlockPos pos = this.menu.getBlockEntity().getBlockPos();
        AbstractHeavyTurretBlockEntity be = this.menu.getBlockEntity();

        this.titleLabelX = 8;
        this.titleLabelY = 6;

        this.editBoxAimLimitMinX = createIntEditBox("aimlimitminx", this.leftPos + FIELD_LEFT_X, this.topPos + ROW_X_Y, String.valueOf(be.getData().aimLimitMinX));
        this.editBoxAimLimitMaxX = createIntEditBox("aimlimitmaxx", this.leftPos + FIELD_RIGHT_X, this.topPos + ROW_X_Y, String.valueOf(be.getData().aimLimitMaxX));
        this.editBoxAimLimitMinY = createIntEditBox("aimlimitminy", this.leftPos + FIELD_LEFT_X, this.topPos + ROW_Y_Y, String.valueOf(be.getData().aimLimitMinY));
        this.editBoxAimLimitMaxY = createIntEditBox("aimlimitmaxy", this.leftPos + FIELD_RIGHT_X, this.topPos + ROW_Y_Y, String.valueOf(be.getData().aimLimitMaxY));
        this.editBoxSpinY = createIntEditBox("spiny", this.leftPos + FIELD_LEFT_X, this.topPos + ROW_SPIN_Y, String.valueOf(be.defaultspiny));
        this.editBoxSpinX = createIntEditBox("spinx", this.leftPos + FIELD_RIGHT_X, this.topPos + ROW_SPIN_Y, String.valueOf(be.defaultspinx));

        addChannelButton(pos, "ch1", this.leftPos + CHANNEL_BUTTON_LEFT_X, this.topPos + CHANNEL_BUTTON_TOP_Y, 1);
        addChannelButton(pos, "ch2", this.leftPos + CHANNEL_BUTTON_RIGHT_X, this.topPos + CHANNEL_BUTTON_TOP_Y, 2);
        addChannelButton(pos, "ch3", this.leftPos + CHANNEL_BUTTON_LEFT_X, this.topPos + CHANNEL_BUTTON_BOTTOM_Y, 3);
        addChannelButton(pos, "ch4", this.leftPos + CHANNEL_BUTTON_RIGHT_X, this.topPos + CHANNEL_BUTTON_BOTTOM_Y, 4);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.heavy_turret.mode_button.label"),
                        button -> {
                            int nextFireType = findNextSelectableFireType();
                            if (nextFireType < 0) {
                                manualDisabledNoticeTicks = 80;
                                return;
                            }
                            // Function: heavy turret mode cycling skips unsupported modes and only sends a legal selection.
                            ModNetworking.sendToServer(new HeavyTurretC2SPacket(pos, nextFireType + 100));
                            be.getData().fireType = nextFireType;
                        })
                .bounds(this.leftPos + MODE_BUTTON_X, this.topPos + MODE_BUTTON_Y, MODE_BUTTON_WIDTH, 16)
                .build());
        this.breakBlocksButton = this.addRenderableWidget(Button.builder(breakBlocksButtonLabel(),
                        button -> {
                            ModNetworking.sendToServer(new HeavyTurretC2SPacket(pos, 5));
                            be.toggleBreaksBlocksEnabled();
                            updateBreakBlocksButtonLabel();
                        })
                .bounds(this.leftPos + BREAK_BLOCKS_BUTTON_X, this.topPos + BREAK_BLOCKS_BUTTON_Y, 20, 14)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.common.save"),
                        button -> saveAndClose())
                .bounds(this.leftPos + SAVE_BUTTON_X, this.topPos + ACTION_BUTTON_Y, SAVE_BUTTON_WIDTH, 18)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.common.cancel"),
                        button -> this.minecraft.player.closeContainer())
                .bounds(this.leftPos + CANCEL_BUTTON_X, this.topPos + ACTION_BUTTON_Y, CANCEL_BUTTON_WIDTH, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Function: the parent container already renders the background once.
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderControlTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        AbstractHeavyTurretBlockEntity turret = this.menu.getBlockEntity();
        ResourceLocation background = menu.hasAmmoSlots() ? HEAVY_AMMO_TEXTURE : HEAVY_TEXTURE;
        // Function: heavy turrets have a separate baked background because their controls exceed the base turret frame.
        guiGraphics.blit(background, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        if (menu.hasAmmoSlots()) {
            drawAmmoSlots(guiGraphics);
            drawPlayerInventorySlots(guiGraphics);
        }

        drawChannelIcon(guiGraphics, turret.getData().isChannel1(),
                this.leftPos + CHANNEL_ICON_LEFT_X, this.topPos + CHANNEL_TOP_ROW_Y, "channel1");
        drawChannelIcon(guiGraphics, turret.getData().isChannel2(),
                this.leftPos + CHANNEL_ICON_RIGHT_X, this.topPos + CHANNEL_TOP_ROW_Y, "channel2");
        drawChannelIcon(guiGraphics, turret.getData().isChannel3(),
                this.leftPos + CHANNEL_ICON_LEFT_X, this.topPos + CHANNEL_BOTTOM_ROW_Y, "channel3");
        drawChannelIcon(guiGraphics, turret.getData().isChannel4(),
                this.leftPos + CHANNEL_ICON_RIGHT_X, this.topPos + CHANNEL_BOTTOM_ROW_Y, "channel4");

        guiGraphics.blit(getFireTypeIcon(turret.getData().fireType), this.leftPos + MODE_ICON_X, this.topPos + MODE_ICON_Y,
                0, 0, 20, 20, 20, 20);

        if (manualDisabledNoticeTicks > 0) {
            renderWrappedNotice(guiGraphics, MANUAL_DISABLED_MESSAGE, this.leftPos + 16, this.topPos + 202, NOTICE_WIDTH, NOTICE_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.common.break_blocks.label"),
                BREAK_BLOCKS_LABEL_X, BREAK_BLOCKS_LABEL_Y, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.heavy_turret.aim_limits.label"), LEFT_PANEL_X, 18, LABEL_COLOR, false);
        drawLabelAboveBox(guiGraphics, this.editBoxAimLimitMinX, Component.translatable("gui.vsie.turret.aim_min_x.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxAimLimitMaxX, Component.translatable("gui.vsie.turret.aim_max_x.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxAimLimitMinY, Component.translatable("gui.vsie.turret.aim_min_y.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxAimLimitMaxY, Component.translatable("gui.vsie.turret.aim_max_y.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxSpinY, Component.translatable("gui.vsie.turret.default_yaw.label"));
        drawLabelAboveBox(guiGraphics, this.editBoxSpinX, Component.translatable("gui.vsie.turret.default_pitch.label"));

        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.heavy_turret.channels.label"), RIGHT_PANEL_X, 18, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vsie.heavy_turret.mode.label"), MODE_LABEL_X, MODE_LABEL_Y, LABEL_COLOR, false);
        if (menu.hasAmmoSlots()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.vsie.common.ammo"), 8, AMMO_LABEL_Y, LABEL_COLOR, false);
            guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 8, PLAYER_INVENTORY_LABEL_Y, LABEL_COLOR, false);
        }

        if (manualDisabledNoticeTicks > 0) {
            manualDisabledNoticeTicks--;
        }
    }

    private void drawAmmoSlots(GuiGraphics guiGraphics) {
        int slotStartX = this.leftPos + HeavyTurretContainerMenu.INTERNAL_SLOT_X - 1;
        int slotStartY = this.topPos + HeavyTurretContainerMenu.INTERNAL_SLOT_Y - 1;
        // Function: draw an explicit slot frame so the shared ammo row remains readable on the plain footer fill.
        for (int col = 0; col < HeavyTurretContainerMenu.INTERNAL_SLOT_COUNT; col++) {
            int x = slotStartX + col * 18;
            guiGraphics.fill(x, slotStartY, x + 18, slotStartY + 18, SLOT_BORDER_COLOR);
            guiGraphics.fill(x + 1, slotStartY + 1, x + 17, slotStartY + 17, SLOT_FILL_COLOR);
        }
    }

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawPlainSlot(guiGraphics,
                        this.leftPos + HeavyTurretContainerMenu.PLAYER_INVENTORY_X + col * 18 - 1,
                        this.topPos + HeavyTurretContainerMenu.PLAYER_INVENTORY_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawPlainSlot(guiGraphics,
                    this.leftPos + HeavyTurretContainerMenu.PLAYER_INVENTORY_X + col * 18 - 1,
                    this.topPos + HeavyTurretContainerMenu.PLAYER_HOTBAR_Y - 1);
        }
    }

    private void drawPlainSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, SLOT_BORDER_COLOR);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_FILL_COLOR);
    }

    private void addChannelButton(BlockPos pos, String labelKey, int x, int y, int channelIndex) {
        // Function: each fire channel gets its own button under the status icon so the two do not overlap.
        this.addRenderableWidget(Button.builder(Component.translatable("gui.vsie.heavy_turret." + labelKey + ".label"),
                        btn -> ModNetworking.sendToServer(new HeavyTurretC2SPacket(pos, channelIndex)))
                .bounds(x, y, CHANNEL_BUTTON_WIDTH, 14)
                .build());
    }

    private void drawChannelIcon(GuiGraphics guiGraphics, boolean enabled, int x, int y, String channelPath) {
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                vsie.ID,
                "textures/gui/weapon/" + channelPath + (enabled ? "_on.png" : "_off.png")
        );
        guiGraphics.blit(icon, x, y, 0, 0, 20, 20, 20, 20);
    }

    private ResourceLocation getFireTypeIcon(int fireType) {
        return switch (fireType) {
            case FIRE_TYPE_MANUAL -> ICON_MANUAL;
            case FIRE_TYPE_SMART -> ICON_SMART;
            default -> ICON_AUTO;
        };
    }

    private EditBox createIntEditBox(String keySuffix, int x, int y, String initialValue) {
        EditBox box = new EditBox(this.font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.translatable("gui.vsie.heavy_turret." + keySuffix + ".tooltip"));
        // Function: keep heavy turret setting inputs compact and limited to small signed integers.
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
        int spinX = safeParseInt(this.editBoxSpinX.getValue(), 0);
        int spinY = safeParseInt(this.editBoxSpinY.getValue(), 0);
        int aimLimitMinX = safeParseInt(this.editBoxAimLimitMinX.getValue(), -180);
        int aimLimitMaxX = safeParseInt(this.editBoxAimLimitMaxX.getValue(), 180);
        int aimLimitMinY = safeParseInt(this.editBoxAimLimitMinY.getValue(), -180);
        int aimLimitMaxY = safeParseInt(this.editBoxAimLimitMaxY.getValue(), 180);
        AbstractHeavyTurretBlockEntity be = this.menu.getBlockEntity();
        be.defaultspinx = spinX;
        be.defaultspiny = spinY;
        be.setAimLimits(aimLimitMinX, aimLimitMaxX, aimLimitMinY, aimLimitMaxY);
        ModNetworking.sendToServer(new TurretDefaultSpinC2SPacket(
                be.getBlockPos(),
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
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + BREAK_BLOCKS_LABEL_X, this.topPos + BREAK_BLOCKS_BUTTON_Y, 96, 16,
                Component.translatable("gui.vsie.common.break_blocks.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxAimLimitMinX.getX(), this.editBoxAimLimitMinX.getY(), this.editBoxAimLimitMinX.getWidth(), this.editBoxAimLimitMinX.getHeight(),
                Component.translatable("gui.vsie.heavy_turret.aimlimitminx.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxAimLimitMaxX.getX(), this.editBoxAimLimitMaxX.getY(), this.editBoxAimLimitMaxX.getWidth(), this.editBoxAimLimitMaxX.getHeight(),
                Component.translatable("gui.vsie.heavy_turret.aimlimitmaxx.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxAimLimitMinY.getX(), this.editBoxAimLimitMinY.getY(), this.editBoxAimLimitMinY.getWidth(), this.editBoxAimLimitMinY.getHeight(),
                Component.translatable("gui.vsie.heavy_turret.aimlimitminy.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxAimLimitMaxY.getX(), this.editBoxAimLimitMaxY.getY(), this.editBoxAimLimitMaxY.getWidth(), this.editBoxAimLimitMaxY.getHeight(),
                Component.translatable("gui.vsie.heavy_turret.aimlimitmaxy.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxSpinY.getX(), this.editBoxSpinY.getY(), this.editBoxSpinY.getWidth(), this.editBoxSpinY.getHeight(),
                Component.translatable("gui.vsie.heavy_turret.spiny.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.editBoxSpinX.getX(), this.editBoxSpinX.getY(), this.editBoxSpinX.getWidth(), this.editBoxSpinX.getHeight(),
                Component.translatable("gui.vsie.heavy_turret.spinx.tooltip"))) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            int x = i % 2 == 0 ? this.leftPos + CHANNEL_BUTTON_LEFT_X : this.leftPos + CHANNEL_BUTTON_RIGHT_X;
            int y = i < 2 ? this.topPos + CHANNEL_BUTTON_TOP_Y : this.topPos + CHANNEL_BUTTON_BOTTOM_Y;
            if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                    x, y, CHANNEL_BUTTON_WIDTH, 14,
                    Component.translatable("gui.vsie.heavy_turret.ch" + (i + 1) + ".tooltip"))) {
                return;
            }
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + MODE_BUTTON_X, this.topPos + MODE_BUTTON_Y, MODE_BUTTON_WIDTH, 16,
                Component.translatable("gui.vsie.heavy_turret.mode_button.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + SAVE_BUTTON_X, this.topPos + ACTION_BUTTON_Y, SAVE_BUTTON_WIDTH, 18,
                Component.translatable("gui.vsie.heavy_turret.save.tooltip"))) {
            return;
        }
        if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + CANCEL_BUTTON_X, this.topPos + ACTION_BUTTON_Y, CANCEL_BUTTON_WIDTH, 18,
                Component.translatable("gui.vsie.heavy_turret.cancel.tooltip"))) {
            return;
        }
        if (menu.hasAmmoSlots() && (this.hoveredSlot == null || !this.hoveredSlot.hasItem())) {
            GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 6, this.topPos + AMMO_LABEL_Y + 4, this.imageWidth - 12, 24,
                    Component.translatable("gui.vsie.heavy_turret.ammo_slots.tooltip"));
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

    private int findNextSelectableFireType() {
        AbstractHeavyTurretBlockEntity turret = this.menu.getBlockEntity();
        int currentFireType = turret.getData().fireType;
        for (int offset = 1; offset <= 3; offset++) {
            int candidate = Math.floorMod(currentFireType + offset, 3);
            if (candidate != currentFireType && turret.isFireTypeSelectable(candidate)) {
                return candidate;
            }
        }
        return -1;
    }

    private void renderWrappedNotice(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.drawString(this.font, lines.get(i), x, y + i * 9, color, false);
        }
    }

    private void drawLabelAboveBox(GuiGraphics guiGraphics, EditBox box, Component label) {
        if (box == null) {
            return;
        }
        int labelX = box.getX() + (box.getWidth() - this.font.width(label)) / 2 - this.leftPos;
        int labelY = box.getY() - this.topPos - LABEL_GAP_Y;
        guiGraphics.drawString(this.font, label, labelX, labelY, LABEL_COLOR, false);
    }

}
