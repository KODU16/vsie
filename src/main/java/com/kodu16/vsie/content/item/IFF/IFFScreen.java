package com.kodu16.vsie.content.item.IFF;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。

import com.kodu16.vsie.network.IFF.IFFC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings({"removal"})
public class IFFScreen extends AbstractContainerScreen<IFFContainerMenu> {

    private EditBox editBoxA;
    private EditBox editBoxB;

    public IFFScreen(IFFContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        ItemStack stack = this.menu.itemStack;  // 推荐使用 menu 提供的物品
        if (stack.hasTag()) {
            var tag = stack.getTag();
            if (tag.contains("TextA")) this.editBoxA.setValue(tag.getString("TextA"));
            if (tag.contains("TextB")) this.editBoxB.setValue(tag.getString("TextB"));
        }
        this.leftPos = this.width / 2 - this.imageWidth / 2;
        this.topPos = this.height / 2 - this.imageHeight / 2;
        //LogUtils.getLogger().warn("initing iff screen");
        // 输入框 A
        this.editBoxA = new EditBox(this.font,
                this.leftPos + 68, this.topPos + 28,
                62, 15,
                Component.literal("Enemy"));
        this.editBoxA.setMaxLength(64);
        //this.editBoxA.setFocus(true);           // 默认聚焦第一个框
        this.addRenderableWidget(this.editBoxA);

        // 输入框 B
        this.editBoxB = new EditBox(this.font,
                this.leftPos + 68, this.topPos + 68,
                62, 15,
                Component.literal("Ally"));
        this.editBoxB.setMaxLength(64);
        this.addRenderableWidget(this.editBoxB);

        // 保存按钮（推荐加上，体验更好）
        int btnX = this.leftPos + 32;
        int btnY = this.topPos + 115;
        this.addRenderableWidget(Button.builder(
                        Component.literal("保存"),
                        button -> saveAndClose()
                )
                .bounds(btnX, btnY, 40, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("取消"),
                        button -> this.minecraft.player.closeContainer()
                )
                .bounds(btnX + 72, btnY, 40, 20)
                .build());
    }

    private void saveAndClose() {
        saveToNBT();
        this.minecraft.player.closeContainer();
    }

    private void saveToNBT() {
        String textA = editBoxA.getValue().trim();
        String textB = editBoxB.getValue().trim();

        // 发送给服务器
        ModNetworking.sendToServer(new IFFC2SPacket(textA, textB));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/iff/iff_gui.png");
        guiGraphics.blit(texture,   // 用实例字段
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // 背包标题
        //guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 可选：绘制静态文字标签
        guiGraphics.drawString(this.font, "敌方", titleLabelX+32, this.titleLabelY+25, 0x404040, false);
        guiGraphics.drawString(this.font, "友方", titleLabelX+32, this.titleLabelY+65, 0x404040, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.player.closeContainer();
            return true;
        }

        // 按回车保存并关闭（最常见的习惯）
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveAndClose();
            return true;
        }

        // 让输入框能正常接收输入
        return this.editBoxA.keyPressed(keyCode, scanCode, modifiers) ||
                this.editBoxB.keyPressed(keyCode, scanCode, modifiers) ||
                super.keyPressed(keyCode, scanCode, modifiers);
    }
}
