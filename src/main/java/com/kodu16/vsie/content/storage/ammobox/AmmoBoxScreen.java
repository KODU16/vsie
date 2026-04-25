package com.kodu16.vsie.content.storage.ammobox;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。

import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


@SuppressWarnings("removal")
public class AmmoBoxScreen extends AbstractContainerScreen<AmmoBoxContainerMenu> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/ammo_box/ammo_box.png");

    private static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/slot.png");

    public AmmoBoxScreen(AmmoBoxContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);

        // ⚠ 必须使用和布局匹配的尺寸
        this.imageWidth = 176;
        this.imageHeight = 166;

        this.titleLabelX = 8;
        this.titleLabelY = 6;

        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTicks, int mouseX, int mouseY) {

        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // =========================
        // 绘制背景
        // =========================
        gg.blit(BG_TEXTURE, this.leftPos, this.topPos,
                0, 0, this.imageWidth, this.imageHeight);

        // =========================
        // 绘制所有槽位纹理
        // =========================

        // 容器 3×9
        /*int startX = 8;
        int startY = 18;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = this.leftPos + startX + col * 18;
                int y = this.topPos + startY + row * 18;

                gg.blit(SLOT_TEXTURE, x, y, 0, 0, 18, 18, 18, 18);
            }
        }

        // 玩家背包 3×9
        int playerInvY = startY + 3 * 18 + 14;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = this.leftPos + startX + col * 18;
                int y = this.topPos + playerInvY + row * 18;

                gg.blit(SLOT_TEXTURE, x, y, 0, 0, 18, 18, 18, 18);
            }
        }

        // 快捷栏
        int hotbarY = playerInvY + 3 * 18 + 4;

        for (int col = 0; col < 9; col++) {
            int x = this.leftPos + startX + col * 18;
            int y = this.topPos + hotbarY;

            gg.blit(SLOT_TEXTURE, x, y, 0, 0, 18, 18, 18, 18);
        }*/
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTicks);
        this.renderTooltip(gg, mouseX, mouseY);
    }
}
