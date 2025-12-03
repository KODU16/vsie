package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.server.TurretContainerMenu;
import com.kodu16.vsie.network.ModNetworking;
import com.kodu16.vsie.network.TurretC2SPacket;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;                  // 新增
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings({"removal"})
public class TurretScreen extends AbstractContainerScreen<TurretContainerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(vsie.ID, "textures/gui/turret_gui.png");

    public TurretScreen(TurretContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    // 1.20.1 必须重写这个新签名的方法
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);   // 背景（半透明黑）
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);   // 鼠标悬停提示
    }

    // renderBg 也必须改成 GuiGraphics
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // 获取 TurretBlockEntity
        AbstractTurretBlockEntity turret = menu.getBlockEntity();  // 这里 menu.getBlockEntity() 返回 TurretBlockEntity
        // 直接用 GuiGraphics 的 blit 方法绘制纹理
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // 根据状态选择不同的图标
        ResourceLocation icon = turret.getData().targetsHostile ? new ResourceLocation(vsie.ID, "textures/gui/target_hostile_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/target_hostile_off.png");

        // 绘制状态图标
        guiGraphics.blit(icon, this.leftPos + 30, this.topPos + 50, 0, 0, 20, 20, 20,20); // 你可以调整位置和大小
    }

    // 可选：如果你还想显示物品栏标签、玩家背包等文字，也可以重写这个
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
        this.addRenderableWidget(Button.builder(
                        Component.literal("敌对"),
                        button -> ModNetworking.CHANNEL.sendToServer(new TurretC2SPacket(pos,1)))
                .pos(this.leftPos + 30, this.topPos + 80)
                .size(60, 20)
                .build());
    }
}
