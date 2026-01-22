package com.kodu16.vsie.content.weapon.client;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.server.WeaponContainerMenu;
import com.kodu16.vsie.network.ModNetworking;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
import com.kodu16.vsie.network.weapon.WeaponC2SPacket;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings({"removal"})
public class WeaponScreen extends AbstractContainerScreen<WeaponContainerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(vsie.ID, "textures/gui/weapon/weapon_gui.png");

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
        /*guiGraphics.blit(icon_channel1, this.leftPos + 30, this.topPos + 50, 0, 0, 16, 16, 16,16);
        guiGraphics.blit(icon_channel2, this.leftPos + 50, this.topPos + 50, 0, 0, 16, 16, 16,16);
        guiGraphics.blit(icon_channel3, this.leftPos + 70, this.topPos + 50, 0, 0, 16, 16, 16,16);
        guiGraphics.blit(icon_channel4, this.leftPos + 90, this.topPos + 50, 0, 0, 16, 16, 16,16);*/


    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // 玩家背包文字 “物品栏”
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void init() {
        super.init();
        BlockPos pos = menu.getBlockEntity().getBlockPos(); // 如果有 getBlockEntity() 方法
        AbstractWeaponBlockEntity blockEntity = menu.getBlockEntity();
        ResourceLocation icon_channel1 = blockEntity.weaponData.getChannel1() ? new ResourceLocation(vsie.ID, "textures/gui/weapon/channel1_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/weapon/channel1_off.png");
        ResourceLocation icon_channel2 = blockEntity.weaponData.getChannel1() ? new ResourceLocation(vsie.ID, "textures/gui/weapon/channel2_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/weapon/channel2_off.png");
        ResourceLocation icon_channel3 = blockEntity.weaponData.getChannel1() ? new ResourceLocation(vsie.ID, "textures/gui/weapon/channel3_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/weapon/channel3_off.png");
        ResourceLocation icon_channel4 = blockEntity.weaponData.getChannel1() ? new ResourceLocation(vsie.ID, "textures/gui/weapon/channel4_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/weapon/channel4_off.png");
        this.addRenderableWidget(
                new ImageButton(
                        this.leftPos + 30, this.topPos + 50, 16, 16,
                        0, 0,
                        height,
                        icon_channel1,
                        button -> ModNetworking.CHANNEL.sendToServer(new WeaponC2SPacket(pos, 1))
                )
        );
        this.addRenderableWidget(
                new ImageButton(
                        this.leftPos + 50, this.topPos + 50, 16, 16,
                        0, 0,
                        height,
                        icon_channel2,
                        button -> ModNetworking.CHANNEL.sendToServer(new WeaponC2SPacket(pos, 2))
                )
        );
        this.addRenderableWidget(
                new ImageButton(
                        this.leftPos + 70, this.topPos + 50, 16, 16,
                        0, 0,
                        height,
                        icon_channel3,
                        button -> ModNetworking.CHANNEL.sendToServer(new WeaponC2SPacket(pos, 3))
                )
        );
        this.addRenderableWidget(
                new ImageButton(
                        this.leftPos + 90, this.topPos + 50, 16, 16,
                        0, 0,
                        height,
                        icon_channel4,
                        button -> ModNetworking.CHANNEL.sendToServer(new WeaponC2SPacket(pos, 4))
                )
        );
    }
}
