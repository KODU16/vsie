package com.kodu16.vsie.content.turret.heavyturret;

import com.kodu16.vsie.network.turret.HeavyTurretC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings({"removal"})
public class HeavyTurretScreen extends AbstractContainerScreen<HeavyTurretContainerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(vsie.ID, "textures/gui/turret/turret_gui.png");

    public HeavyTurretScreen(HeavyTurretContainerMenu menu, Inventory inv, Component title) {
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
        AbstractHeavyTurretBlockEntity turret = menu.getBlockEntity();  // 这里 menu.getBlockEntity() 返回 TurretBlockEntity
        // 直接用 GuiGraphics 的 blit 方法绘制纹理
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // 功能：绘制与武器界面一致的 4 路频道状态图标。
        ResourceLocation iconChannel1 = turret.getData().channel1
                ? new ResourceLocation(vsie.ID, "textures/gui/weapon/channel1_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/weapon/channel1_off.png");
        ResourceLocation iconChannel2 = turret.getData().channel2
                ? new ResourceLocation(vsie.ID, "textures/gui/weapon/channel2_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/weapon/channel2_off.png");
        ResourceLocation iconChannel3 = turret.getData().channel3
                ? new ResourceLocation(vsie.ID, "textures/gui/weapon/channel3_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/weapon/channel3_off.png");
        ResourceLocation iconChannel4 = turret.getData().channel4
                ? new ResourceLocation(vsie.ID, "textures/gui/weapon/channel4_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/weapon/channel4_off.png");

        guiGraphics.blit(iconChannel1, this.leftPos + 30, this.topPos + 20, 0, 0, 20, 20, 20, 20);
        guiGraphics.blit(iconChannel2, this.leftPos + 60, this.topPos + 20, 0, 0, 20, 20, 20, 20);
        guiGraphics.blit(iconChannel3, this.leftPos + 90, this.topPos + 20, 0, 0, 20, 20, 20, 20);
        guiGraphics.blit(iconChannel4, this.leftPos + 120, this.topPos + 20, 0, 0, 20, 20, 20, 20);

        // 根据状态选择不同的图标
        ResourceLocation iconmanual = new ResourceLocation(vsie.ID, "textures/gui/heavyturret/target_manual.png");
        ResourceLocation iconauto = new ResourceLocation(vsie.ID, "textures/gui/heavyturret/target_auto.png");
        ResourceLocation iconsmart = new ResourceLocation(vsie.ID, "textures/gui/heavyturret/target_smart.png");

        // 绘制状态图标
        if (turret.getData().firetype == 0) {
            guiGraphics.blit(iconmanual, this.leftPos + 78, this.topPos + 70, 0, 0, 20, 20, 19, 19);
        }
        if (turret.getData().firetype == 1) {
            guiGraphics.blit(iconauto, this.leftPos + 78, this.topPos + 70, 0, 0, 20, 20, 19, 19);
        }
        if (turret.getData().firetype == 2) {
            guiGraphics.blit(iconsmart, this.leftPos + 78, this.topPos + 70, 0, 0, 20, 20, 19, 19);
        }
    }

    // 可选：如果你还想显示物品栏标签、玩家背包等文字，也可以重写这个
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // 玩家背包文字 “物品栏”
        //guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void init() {
        super.init();
        BlockPos pos = menu.getBlockEntity().getBlockPos(); // 如果有 getBlockEntity() 方法
        // 功能：新增与主武器一致的四个频道切换按键。
        this.addRenderableWidget(Button.builder(
                Component.literal("CH1"),
                btn -> ModNetworking.CHANNEL.sendToServer(new HeavyTurretC2SPacket(pos, 1))
        ).bounds(leftPos + 30, topPos + 40, 20, 10).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("CH2"),
                btn -> ModNetworking.CHANNEL.sendToServer(new HeavyTurretC2SPacket(pos, 2))
        ).bounds(leftPos + 60, topPos + 40, 20, 10).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("CH3"),
                btn -> ModNetworking.CHANNEL.sendToServer(new HeavyTurretC2SPacket(pos, 3))
        ).bounds(leftPos + 90, topPos + 40, 20, 10).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("CH4"),
                btn -> ModNetworking.CHANNEL.sendToServer(new HeavyTurretC2SPacket(pos, 4))
        ).bounds(leftPos + 120, topPos + 40, 20, 10).build());

        // 功能：保留并更新模式切换按钮，使用 100+firetype 编码避免与频道按键冲突。
        this.addRenderableWidget(Button.builder(
                        Component.literal("switch"),
                        button -> ModNetworking.CHANNEL.sendToServer(new HeavyTurretC2SPacket(pos, ((menu.getBlockEntity().getData().firetype + 1) % 3) + 100)))
                .pos(this.leftPos + 73, this.topPos + 100)
                .size(30, 15)
                .build());
    }
}
