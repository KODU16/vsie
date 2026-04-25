package com.kodu16.vsie.content.weapon.client;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.server.WeaponContainerMenu;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.network.weapon.WeaponC2SPacket;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings({"removal"})
public class WeaponScreen extends AbstractContainerScreen<WeaponContainerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/weapon_gui.png");

    public WeaponScreen(WeaponContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);   // 背景（半透明黑）
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);   // 鼠标悬停提示
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        AbstractWeaponBlockEntity blockEntity = menu.getBlockEntity();
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        ResourceLocation icon_channel1= blockEntity.getData().channel1
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel1_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel1_off.png");
        ResourceLocation icon_channel2 = blockEntity.getData().channel2
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel2_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel2_off.png");
        ResourceLocation icon_channel3 = blockEntity.getData().channel3
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel3_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel3_off.png");
        ResourceLocation icon_channel4 = blockEntity.getData().channel4
                ? ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel4_on.png")
                : ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/weapon/channel4_off.png");

        guiGraphics.blit(icon_channel1, this.leftPos + 30, this.topPos + 20, 0, 0, 20, 20, 20,20);
        guiGraphics.blit(icon_channel2, this.leftPos + 60, this.topPos + 20, 0, 0, 20, 20, 20,20);
        guiGraphics.blit(icon_channel3, this.leftPos + 90, this.topPos + 20, 0, 0, 20, 20, 20,20);
        guiGraphics.blit(icon_channel4, this.leftPos + 120, this.topPos + 20, 0, 0, 20, 20, 20,20);

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // 玩家背包文字 “物品栏”
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    protected void init() {
        super.init();
        BlockPos pos = menu.getBlockEntity().getBlockPos();
        AbstractWeaponBlockEntity blockEntity = menu.getBlockEntity();
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

    }

}
