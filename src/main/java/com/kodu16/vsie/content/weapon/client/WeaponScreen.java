package com.kodu16.vsie.content.weapon.client;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.BasicMissileLauncherBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotCoreBlockEntity;
import com.kodu16.vsie.content.weapon.server.WeaponContainerMenu;
import com.kodu16.vsie.network.weapon.WeaponC2SPacket;
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

@SuppressWarnings({"removal"})
public class WeaponScreen extends AbstractContainerScreen<WeaponContainerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/weapon_gui.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/slot.png");
    private EditBox launchIntervalBox;

    public WeaponScreen(WeaponContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        // Function: missile launchers need extra space for missile storage and player inventory.
        this.imageHeight = menu.hasInventorySlots() ? 220 : 166;
        this.inventoryLabelY = menu.hasInventorySlots() ? WeaponContainerMenu.PLAYER_INV_Y - 11 : 72;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        AbstractWeaponBlockEntity blockEntity = menu.getBlockEntity();
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, Math.min(this.imageHeight, 166));
        if (menu.hasInventorySlots()) {
            // Function: draw a simple panel below the original weapon controls for inventory slots.
            guiGraphics.fill(this.leftPos, this.topPos + 56, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFBDBDBD);
            guiGraphics.fill(this.leftPos + 3, this.topPos + 59, this.leftPos + this.imageWidth - 3, this.topPos + this.imageHeight - 3, 0xFFC6C6C6);
        }

        ResourceLocation iconChannel1 = blockEntity.getData().channel1
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel1_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel1_off.png");
        ResourceLocation iconChannel2 = blockEntity.getData().channel2
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel2_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel2_off.png");
        ResourceLocation iconChannel3 = blockEntity.getData().channel3
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel3_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel3_off.png");
        ResourceLocation iconChannel4 = blockEntity.getData().channel4
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel4_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel4_off.png");

        guiGraphics.blit(iconChannel1, this.leftPos + 30, this.topPos + 20, 0, 0, 20, 20, 20, 20);
        guiGraphics.blit(iconChannel2, this.leftPos + 60, this.topPos + 20, 0, 0, 20, 20, 20, 20);
        guiGraphics.blit(iconChannel3, this.leftPos + 90, this.topPos + 20, 0, 0, 20, 20, 20, 20);
        guiGraphics.blit(iconChannel4, this.leftPos + 120, this.topPos + 20, 0, 0, 20, 20, 20, 20);

        if (blockEntity instanceof BasicMissileLauncherBlockEntity || blockEntity instanceof VerticleLaunchingSlotCoreBlockEntity) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            drawInternalSlots(guiGraphics);
            drawPlayerInventorySlots(guiGraphics);
        }
    }

    private void drawInternalSlots(GuiGraphics guiGraphics) {
        // Function: draw missile buffer slot backgrounds.
        int slotStartX = this.leftPos + WeaponContainerMenu.INTERNAL_SLOT_X - 1;
        int slotStartY = this.topPos + WeaponContainerMenu.INTERNAL_SLOT_Y - 1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                guiGraphics.blit(SLOT_TEXTURE, slotStartX + col * 18, slotStartY + row * 18, 0, 0, 18, 18, 18, 18);
            }
        }
    }

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics) {
        // Function: draw player inventory slot backgrounds for launchers with internal ammo storage.
        int startX = this.leftPos + WeaponContainerMenu.PLAYER_INV_X - 1;
        int startY = this.topPos + WeaponContainerMenu.PLAYER_INV_Y - 1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                guiGraphics.blit(SLOT_TEXTURE, startX + col * 18, startY + row * 18, 0, 0, 18, 18, 18, 18);
            }
        }
        int hotbarY = this.topPos + WeaponContainerMenu.HOTBAR_Y - 1;
        for (int col = 0; col < 9; col++) {
            guiGraphics.blit(SLOT_TEXTURE, startX + col * 18, hotbarY, 0, 0, 18, 18, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        if (menu.hasInventorySlots()) {
            // Function: label the added player inventory section.
            guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        }
        if (menu.getBlockEntity() instanceof VerticleLaunchingSlotCoreBlockEntity core) {
            // Function: show the adjustable launch interval k directly in the weapon inventory GUI.
            guiGraphics.drawString(this.font, "k(tick)", 36, 56, 0x404040, false);
            guiGraphics.drawString(this.font, "current " + core.getLaunchIntervalTicks(), 112, 56, 0x404040, false);
        }
    }

    @Override
    protected void init() {
        super.init();
        BlockPos pos = menu.getBlockEntity().getBlockPos();
        this.addRenderableWidget(Button.builder(
                Component.literal("CH1"),
                btn -> ModNetworking.sendToServer(new WeaponC2SPacket(pos, 1))
        ).bounds(leftPos + 30, topPos + 40, 20, 10).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("CH2"),
                btn -> ModNetworking.sendToServer(new WeaponC2SPacket(pos, 2))
        ).bounds(leftPos + 60, topPos + 40, 20, 10).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("CH3"),
                btn -> ModNetworking.sendToServer(new WeaponC2SPacket(pos, 3))
        ).bounds(leftPos + 90, topPos + 40, 20, 10).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("CH4"),
                btn -> ModNetworking.sendToServer(new WeaponC2SPacket(pos, 4))
        ).bounds(leftPos + 120, topPos + 40, 20, 10).build());

        if (menu.getBlockEntity() instanceof VerticleLaunchingSlotCoreBlockEntity core) {
            this.launchIntervalBox = new EditBox(this.font, leftPos + 72, topPos + 54, 32, 12, Component.literal("k"));
            // Function: the VLS burst interval is sent as an integer tick count to the server.
            this.launchIntervalBox.setMaxLength(5);
            this.launchIntervalBox.setValue(String.valueOf(core.getLaunchIntervalTicks()));
            this.addRenderableWidget(this.launchIntervalBox);
            this.addRenderableWidget(Button.builder(
                    Component.literal("Save"),
                    btn -> saveLaunchInterval()
            ).bounds(leftPos + 112, topPos + 70, 42, 14).build());
        }
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
}
