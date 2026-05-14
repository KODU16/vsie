package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.TurretContainerMenu;
import com.kodu16.vsie.content.turret.block.ParticleTurretBlockEntity;
import com.kodu16.vsie.content.turret.ciws.AbstractCIWSBlockEntity;
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

@SuppressWarnings({"removal"})
public class TurretScreen extends AbstractContainerScreen<TurretContainerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/turret/turret_gui.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/slot.png");

    private EditBox editBoxSpinX;
    private EditBox editBoxSpinY;

    public TurretScreen(TurretContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        // Function: Particle Turret needs room for the added player inventory slots.
        this.imageHeight = menu.hasInventorySlots() ? 220 : 166;
        this.inventoryLabelY = menu.hasInventorySlots() ? TurretContainerMenu.PLAYER_INV_Y - 11 : 72;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        AbstractTurretBlockEntity turret = menu.getBlockEntity();
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, Math.min(this.imageHeight, 166));
        if (menu.hasInventorySlots()) {
            // Function: extend the old texture with a neutral panel under the turret controls.
            guiGraphics.fill(this.leftPos, this.topPos + 116, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFBDBDBD);
            guiGraphics.fill(this.leftPos + 3, this.topPos + 119, this.leftPos + this.imageWidth - 3, this.topPos + this.imageHeight - 3, 0xFFC6C6C6);
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

        guiGraphics.blit(iconHostile, this.leftPos + 20, this.topPos + 70, 0, 0, 19, 19, 19, 19);
        guiGraphics.blit(iconPassive, this.leftPos + 59, this.topPos + 70, 0, 0, 19, 19, 19, 19);
        guiGraphics.blit(iconPlayer, this.leftPos + 98, this.topPos + 70, 0, 0, 19, 19, 19, 19);
        guiGraphics.blit(menu.getBlockEntity() instanceof AbstractCIWSBlockEntity ? iconCiws : iconShip,
                this.leftPos + 137, this.topPos + 70, 0, 0, 19, 19, 19, 19);

        if (turret instanceof ParticleTurretBlockEntity) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            drawInternalSlots(guiGraphics);
            drawPlayerInventorySlots(guiGraphics);
        }
    }

    private void drawInternalSlots(GuiGraphics guiGraphics) {
        // Function: draw backgrounds for the Particle Turret's internal 3x3 ammo buffer.
        int slotStartX = this.leftPos + TurretContainerMenu.INTERNAL_SLOT_X - 1;
        int slotStartY = this.topPos + TurretContainerMenu.INTERNAL_SLOT_Y - 1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                guiGraphics.blit(SLOT_TEXTURE, slotStartX + col * 18, slotStartY + row * 18, 0, 0, 18, 18, 18, 18);
            }
        }
    }

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics) {
        // Function: draw backgrounds for the player inventory slots added to this menu.
        int startX = this.leftPos + TurretContainerMenu.PLAYER_INV_X - 1;
        int startY = this.topPos + TurretContainerMenu.PLAYER_INV_Y - 1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                guiGraphics.blit(SLOT_TEXTURE, startX + col * 18, startY + row * 18, 0, 0, 18, 18, 18, 18);
            }
        }
        int hotbarY = this.topPos + TurretContainerMenu.HOTBAR_Y - 1;
        for (int col = 0; col < 9; col++) {
            guiGraphics.blit(SLOT_TEXTURE, startX + col * 18, hotbarY, 0, 0, 18, 18, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        if (menu.hasInventorySlots()) {
            // Function: show the normal player inventory label above the added slots.
            guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        }
    }

    @Override
    protected void init() {
        super.init();
        BlockPos pos = menu.getBlockEntity().getBlockPos();
        var be = this.menu.getBlockEntity();
        this.editBoxSpinX = createIntEditBox("SpinX", this.leftPos + 112, this.topPos + 48, String.valueOf(be.defaultspinx));
        this.editBoxSpinY = createIntEditBox("SpinY", this.leftPos + 48, this.topPos + 48, String.valueOf(be.defaultspiny));

        int targetButtonY = menu.hasInventorySlots() ? 90 : 100;
        int actionButtonY = menu.hasInventorySlots() ? 108 : 140;

        this.addRenderableWidget(Button.builder(Component.literal("HOS"),
                        button -> ModNetworking.sendToServer(new TurretC2SPacket(pos, 1)))
                .pos(this.leftPos + 16, this.topPos + targetButtonY)
                .size(27, 15)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("PAS"),
                        button -> ModNetworking.sendToServer(new TurretC2SPacket(pos, 2)))
                .pos(this.leftPos + 55, this.topPos + targetButtonY)
                .size(27, 15)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Player"),
                        button -> ModNetworking.sendToServer(new TurretC2SPacket(pos, 3)))
                .pos(this.leftPos + 94, this.topPos + targetButtonY)
                .size(37, 15)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Ship"),
                        button -> ModNetworking.sendToServer(new TurretC2SPacket(pos, 4)))
                .pos(this.leftPos + 137, this.topPos + targetButtonY)
                .size(27, 15)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"),
                        button -> saveAndClose())
                .bounds(this.leftPos + 32, this.topPos + actionButtonY, 40, 16)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"),
                        button -> this.minecraft.player.closeContainer())
                .bounds(this.leftPos + 76, this.topPos + actionButtonY, 48, 16)
                .build());
    }

    private EditBox createIntEditBox(String name, int x, int y, String initialValue) {
        EditBox box = new EditBox(this.font, x, y, 24, 10, Component.literal(name));
        // Function: keep the default spin input compact and numeric-friendly.
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
        var be = this.menu.getBlockEntity();
        be.defaultspinx = spinX;
        be.defaultspiny = spinY;
        ModNetworking.sendToServer(new TurretDefaultSpinC2SPacket(menu.getBlockEntity().getBlockPos(), spinX, spinY));
        this.minecraft.player.closeContainer();
    }
}
