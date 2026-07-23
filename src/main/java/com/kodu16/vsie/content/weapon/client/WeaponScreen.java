package com.kodu16.vsie.content.weapon.client;

import com.kodu16.vsie.content.weapon.redstone_relay.RedstoneRelayBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotCoreBlockEntity;
import com.kodu16.vsie.foundation.client.GuiTooltipHelper;
import com.kodu16.vsie.content.weapon.server.WeaponContainerMenu;
import com.kodu16.vsie.network.weapon.WeaponC2SPacket;
import com.kodu16.vsie.network.weapon.WeaponDisplayNameC2SPacket;
import com.kodu16.vsie.network.weapon.WeaponLaunchIntervalC2SPacket;
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
public class WeaponScreen extends AbstractContainerScreen<WeaponContainerMenu> {
    private static final int BASE_SCREEN_WIDTH = 176;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/weapon_gui.png");
    private static final ResourceLocation AMMO_TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/weapon_gui_ammo.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/slot.png");
    private static final int DISPLAY_NAME_PANEL_TOP = 94;
    private static final int DISPLAY_NAME_PANEL_BOTTOM = 122;
    private static final int AMMO_PANEL_TOP = 110;
    private static final int AMMO_PANEL_BOTTOM = 139;
    private static final int INVENTORY_PANEL_TOP = 146;
    private static final int INVENTORY_PANEL_BOTTOM = 236;
    private static final int BREAK_BLOCKS_LABEL_X = 12;
    private static final int BREAK_BLOCKS_LABEL_Y = 68;
    private static final int BREAK_BLOCKS_BUTTON_X = 94;
    private static final int BREAK_BLOCKS_BUTTON_Y = 64;
    private static final int AMMO_SCREEN_WIDTH = 228;
    private static final int AMMO_SCREEN_HEIGHT = 246;
    private static final int[] CHANNEL_XS = {20, 59, 98, 137};
    private static final int CHANNEL_ICON_Y = 20;
    private static final int CHANNEL_BUTTON_Y = 40;
    private static final int CHANNEL_SIZE = 20;
    private static final int CHANNEL_BUTTON_HEIGHT = 10;
    private EditBox launchIntervalBox;
    private EditBox displayNameBox;
    private Button breakBlocksButton;

    public WeaponScreen(WeaponContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // Function: ammo screens are wider while channel controls stay centered in the original weapon panel.
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
        if (menu.hasAmmoSlots()) {
            // Function: ammo weapons use a taller baked background so all item slots share one visual frame.
            guiGraphics.blit(AMMO_TEXTURE, this.leftPos, this.topPos, 0, 0,
                    this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        } else {
            guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, 166);
        }
        if (isRedstoneRelayScreen()) {
            // Function: redstone relay reuses the standard weapon menu and reserves the lower strip for HUD short-name editing.
            guiGraphics.fill(this.leftPos + 4, this.topPos + DISPLAY_NAME_PANEL_TOP, this.leftPos + this.imageWidth - 4, this.topPos + DISPLAY_NAME_PANEL_BOTTOM, 0x70505050);
        }
        if (menu.hasAmmoSlots()) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            drawAmmoSlots(guiGraphics);
            drawPlayerInventorySlots(guiGraphics);
        }
        ResourceLocation iconChannel1 = menu.getBlockEntity().getData().channel1
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel1_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel1_off.png");
        ResourceLocation iconChannel2 = menu.getBlockEntity().getData().channel2
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel2_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel2_off.png");
        ResourceLocation iconChannel3 = menu.getBlockEntity().getData().channel3
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel3_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel3_off.png");
        ResourceLocation iconChannel4 = menu.getBlockEntity().getData().channel4
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel4_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel4_off.png");

        int channelOffsetX = controlOffsetX();
        guiGraphics.blit(iconChannel1, this.leftPos + channelOffsetX + CHANNEL_XS[0], this.topPos + CHANNEL_ICON_Y, 0, 0, CHANNEL_SIZE, CHANNEL_SIZE, CHANNEL_SIZE, CHANNEL_SIZE);
        guiGraphics.blit(iconChannel2, this.leftPos + channelOffsetX + CHANNEL_XS[1], this.topPos + CHANNEL_ICON_Y, 0, 0, CHANNEL_SIZE, CHANNEL_SIZE, CHANNEL_SIZE, CHANNEL_SIZE);
        guiGraphics.blit(iconChannel3, this.leftPos + channelOffsetX + CHANNEL_XS[2], this.topPos + CHANNEL_ICON_Y, 0, 0, CHANNEL_SIZE, CHANNEL_SIZE, CHANNEL_SIZE, CHANNEL_SIZE);
        guiGraphics.blit(iconChannel4, this.leftPos + channelOffsetX + CHANNEL_XS[3], this.topPos + CHANNEL_ICON_Y, 0, 0, CHANNEL_SIZE, CHANNEL_SIZE, CHANNEL_SIZE, CHANNEL_SIZE);
    }

    private void drawAmmoSlots(GuiGraphics guiGraphics) {
        // Function: non-energy weapons display exactly one 9-slot ammo row.
        int slotStartX = this.leftPos + WeaponContainerMenu.INTERNAL_SLOT_X - 1;
        int slotStartY = this.topPos + WeaponContainerMenu.INTERNAL_SLOT_Y - 1;
        for (int col = 0; col < WeaponContainerMenu.INTERNAL_SLOT_COUNT; col++) {
            guiGraphics.blit(SLOT_TEXTURE, slotStartX + col * 18, slotStartY, 0, 0, 18, 18, 18, 18);
        }
    }

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                guiGraphics.blit(SLOT_TEXTURE,
                        this.leftPos + WeaponContainerMenu.PLAYER_INVENTORY_X + col * 18 - 1,
                        this.topPos + WeaponContainerMenu.PLAYER_INVENTORY_Y + row * 18 - 1,
                        0, 0, 18, 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            guiGraphics.blit(SLOT_TEXTURE,
                    this.leftPos + WeaponContainerMenu.PLAYER_INVENTORY_X + col * 18 - 1,
                    this.topPos + WeaponContainerMenu.PLAYER_HOTBAR_Y - 1,
                    0, 0, 18, 18, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        if (menu.getBlockEntity().supportsBlockDestructionToggle()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.vsie.common.break_blocks.label"),
                    BREAK_BLOCKS_LABEL_X, BREAK_BLOCKS_LABEL_Y, 0x404040, false);
        }
        if (menu.hasAmmoSlots()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.vsie.common.ammo"), 8, 110, 0x404040, false);
            guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 8, 146, 0x404040, false);
        }
        if (menu.getBlockEntity() instanceof RedstoneRelayBlockEntity relay) {
            guiGraphics.drawString(this.font, Component.translatable("gui.vsie.weapon.display_name.label"), 8, 86, 0x404040, false);
            guiGraphics.drawString(this.font, Component.translatable("gui.vsie.weapon.display_name.current", relay.getResolvedHudDisplayName()), 8, 112, 0x404040, false);
        }
        if (menu.getBlockEntity() instanceof VerticleLaunchingSlotCoreBlockEntity core) {
            // Function: keep the adjustable launch interval on a dedicated lower row under the shared ammo strip.
            guiGraphics.drawString(this.font, Component.translatable("gui.vsie.weapon.launch_interval.label"), 8, 82, 0x404040, false);
            guiGraphics.drawString(this.font, Component.translatable("gui.vsie.weapon.launch_interval.current", core.getLaunchIntervalTicks()), 8, 98, 0x404040, false);
        }
    }

    @Override
    protected void init() {
        super.init();
        BlockPos pos = menu.getBlockEntity().getBlockPos();
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.vsie.weapon.channel_1.label"),
                btn -> ModNetworking.sendToServer(new WeaponC2SPacket(pos, 1))
        ).bounds(leftPos + controlOffsetX() + CHANNEL_XS[0], topPos + CHANNEL_BUTTON_Y, CHANNEL_SIZE, CHANNEL_BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.vsie.weapon.channel_2.label"),
                btn -> ModNetworking.sendToServer(new WeaponC2SPacket(pos, 2))
        ).bounds(leftPos + controlOffsetX() + CHANNEL_XS[1], topPos + CHANNEL_BUTTON_Y, CHANNEL_SIZE, CHANNEL_BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.vsie.weapon.channel_3.label"),
                btn -> ModNetworking.sendToServer(new WeaponC2SPacket(pos, 3))
        ).bounds(leftPos + controlOffsetX() + CHANNEL_XS[2], topPos + CHANNEL_BUTTON_Y, CHANNEL_SIZE, CHANNEL_BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.vsie.weapon.channel_4.label"),
                btn -> ModNetworking.sendToServer(new WeaponC2SPacket(pos, 4))
        ).bounds(leftPos + controlOffsetX() + CHANNEL_XS[3], topPos + CHANNEL_BUTTON_Y, CHANNEL_SIZE, CHANNEL_BUTTON_HEIGHT).build());
        if (menu.getBlockEntity().supportsBlockDestructionToggle()) {
            this.breakBlocksButton = this.addRenderableWidget(Button.builder(
                    breakBlocksButtonLabel(),
                    btn -> {
                        ModNetworking.sendToServer(new WeaponC2SPacket(pos, 5));
                        menu.getBlockEntity().toggleBreaksBlocksEnabled();
                        updateBreakBlocksButtonLabel();
                    }
            ).bounds(leftPos + BREAK_BLOCKS_BUTTON_X, topPos + BREAK_BLOCKS_BUTTON_Y, 20, 14).build());
        }

        if (menu.getBlockEntity() instanceof RedstoneRelayBlockEntity relay) {
            this.displayNameBox = new EditBox(this.font, leftPos + 8, topPos + 96, 124, 14, Component.translatable("gui.vsie.weapon.display_name.input"));
            // Function: the relay HUD short name is intentionally short so the control-seat overlay stays compact.
            this.displayNameBox.setMaxLength(12);
            this.displayNameBox.setValue(relay.getCustomHudDisplayName());
            this.addRenderableWidget(this.displayNameBox);
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.vsie.common.save"),
                    btn -> saveDisplayName()
            ).bounds(leftPos + 136, topPos + 96, 28, 14).build());
        }

        if (menu.getBlockEntity() instanceof VerticleLaunchingSlotCoreBlockEntity core) {
            this.launchIntervalBox = new EditBox(this.font, leftPos + 104, topPos + 80, 28, 14, Component.translatable("gui.vsie.weapon.launch_interval.input"));
            // Function: the VLS burst interval is sent as an integer tick count to the server.
            this.launchIntervalBox.setMaxLength(5);
            this.launchIntervalBox.setValue(String.valueOf(core.getLaunchIntervalTicks()));
            this.addRenderableWidget(this.launchIntervalBox);
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.vsie.common.save"),
                    btn -> saveLaunchInterval()
            ).bounds(leftPos + 136, topPos + 80, 28, 14).build());
        }
    }

    private boolean isRedstoneRelayScreen() {
        return menu.getBlockEntity() instanceof RedstoneRelayBlockEntity;
    }

    private void saveDisplayName() {
        if (!(menu.getBlockEntity() instanceof RedstoneRelayBlockEntity relay) || this.displayNameBox == null) {
            return;
        }
        String displayName = this.displayNameBox.getValue();
        relay.setCustomHudDisplayName(displayName);
        ModNetworking.sendToServer(new WeaponDisplayNameC2SPacket(relay.getBlockPos(), displayName));
    }

    private void saveLaunchInterval() {
        if (!(menu.getBlockEntity() instanceof VerticleLaunchingSlotCoreBlockEntity core)) {
            return;
        }
        int interval = safeParseInt(launchIntervalBox.getValue(), core.getLaunchIntervalTicks());
        core.setLaunchIntervalTicks(interval);
        ModNetworking.sendToServer(new WeaponLaunchIntervalC2SPacket(core.getBlockPos(), interval));
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

    private void renderControlTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.breakBlocksButton != null
                && GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + BREAK_BLOCKS_LABEL_X, this.topPos + BREAK_BLOCKS_BUTTON_Y, 96, 16,
                Component.translatable("gui.vsie.common.break_blocks.tooltip"))) {
            return;
        }
        String[] channelKeys = {
                "gui.vsie.weapon.channel_1.tooltip",
                "gui.vsie.weapon.channel_2.tooltip",
                "gui.vsie.weapon.channel_3.tooltip",
                "gui.vsie.weapon.channel_4.tooltip"
        };
        for (int i = 0; i < CHANNEL_XS.length; i++) {
            if (GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + controlOffsetX() + CHANNEL_XS[i], this.topPos + CHANNEL_BUTTON_Y, CHANNEL_SIZE, CHANNEL_BUTTON_HEIGHT,
                    Component.translatable(channelKeys[i]))) {
                return;
            }
        }

        if (menu.hasAmmoSlots() && (this.hoveredSlot == null || !this.hoveredSlot.hasItem())
                && GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 4, this.topPos + AMMO_PANEL_TOP, this.imageWidth - 8, AMMO_PANEL_BOTTOM - AMMO_PANEL_TOP,
                Component.translatable("gui.vsie.weapon.ammo_slots.tooltip"))) {
            return;
        }

        if (this.launchIntervalBox != null
                && GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.launchIntervalBox.getX(), this.launchIntervalBox.getY(), this.launchIntervalBox.getWidth(), this.launchIntervalBox.getHeight(),
                Component.translatable("gui.vsie.weapon.launch_interval.input.tooltip"))) {
            return;
        }

        if (this.launchIntervalBox != null) {
            GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 136, this.topPos + 80, 28, 14,
                    Component.translatable("gui.vsie.weapon.launch_interval.save.tooltip"));
            return;
        }

        if (this.displayNameBox != null
                && GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                this.displayNameBox.getX(), this.displayNameBox.getY(), this.displayNameBox.getWidth(), this.displayNameBox.getHeight(),
                Component.translatable("gui.vsie.weapon.display_name.input.tooltip"))) {
            return;
        }

        if (this.displayNameBox != null) {
            GuiTooltipHelper.renderTooltipIfHovered(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 136, this.topPos + 96, 28, 14,
                    Component.translatable("gui.vsie.weapon.display_name.save.tooltip"));
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

    private int controlOffsetX() {
        return (this.imageWidth - BASE_SCREEN_WIDTH) / 2;
    }
}
