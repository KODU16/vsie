package com.kodu16.vsie.content.screen.client;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.screen.server.ScreenContainerMenu;
import com.kodu16.vsie.network.screen.ScreenC2SPacket;
import com.kodu16.vsie.network.screen.ScreentypeC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings({"removal"})
public class ScreenScreen extends AbstractContainerScreen<ScreenContainerMenu> {

    private EditBox editBoxSpinX;
    private EditBox editBoxSpinY;
    private EditBox editBoxOffsetX;
    private EditBox editBoxOffsetY;
    private EditBox editBoxOffsetZ;

    public ScreenScreen(ScreenContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = this.width / 2 - this.imageWidth / 2;
        this.topPos = this.height / 2 - this.imageHeight / 2;

        // ===================== 读取上一次的值 =====================
        // 假设你的 ScreenContainerMenu 能拿到 BlockEntity
        var be = this.menu.getBlockEntity();           // 你要自己实现这个方法
        int spinX  = be.spinx;   // 假设你有这些 getter
        int spinY  = be.spiny;
        int offsetX = be.offsetx;
        int offsetY = be.offsety;
        int offsetZ = be.offsetz;

        // ===================== 创建输入框并设置初始值 =====================
        this.editBoxSpinX = createIntEditBox("SpinX", 28, 68, String.valueOf(spinX));
        this.editBoxSpinY = createIntEditBox("SpinY", 68, 68, String.valueOf(spinY));

        this.editBoxOffsetX = createIntEditBox("offsetX", 28, 88, String.valueOf(offsetX));
        this.editBoxOffsetY = createIntEditBox("offsetY", 68, 88, String.valueOf(offsetY));
        this.editBoxOffsetZ = createIntEditBox("offsetZ", 108, 88, String.valueOf(offsetZ));

        // 可选：让输入框只接受数字（更好体验）
        // setFilter 只允许 负号 + 数字
        setIntegerOnly(this.editBoxSpinX);
        setIntegerOnly(this.editBoxSpinY);
        setIntegerOnly(this.editBoxOffsetX);
        setIntegerOnly(this.editBoxOffsetY);
        setIntegerOnly(this.editBoxOffsetZ);

        // 保存按钮（推荐加上，体验更好）
        int btnX = this.leftPos + 32;
        int btnY = this.topPos + 135;
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
        BlockPos pos = menu.getBlockEntity().getBlockPos(); // 如果有 getBlockEntity() 方法
        this.addRenderableWidget(Button.builder(
                        Component.literal("switch"),
                        button -> ModNetworking.CHANNEL.sendToServer(new ScreentypeC2SPacket(pos,(menu.getBlockEntity().displaytype+1)%2)))
                .pos(this.leftPos + 73, this.topPos + 100)
                .size(30, 15)
                .build());
    }

    // 抽取成方法，方便复用 + 设置初始值
    private EditBox createIntEditBox(String name, int x, int y, String initialValue) {
        EditBox box = new EditBox(this.font,
                this.leftPos + x, this.topPos + y,
                24, 14,
                Component.literal(name));
        box.setMaxLength(8);           // int 范围够用
        box.setValue(initialValue);    // ← 关键！设置初始值
        box.setFocused(false);
        this.addRenderableWidget(box);
        return box;
    }

    // 限制只能输入整数（-可有可无，看需求）
    private void setIntegerOnly(EditBox box) {
        box.setFilter(s -> {
            if (s.isEmpty()) return true;
            if (s.equals("-")) return true;
            try {
                Integer.parseInt(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    private void saveAndClose() {
        try {
            int spinX   = safeParseInt(editBoxSpinX.getValue(), 0);
            int spinY   = safeParseInt(editBoxSpinY.getValue(), 0);
            int offsetX = safeParseInt(editBoxOffsetX.getValue(), 0);
            int offsetY = safeParseInt(editBoxOffsetY.getValue(), 0);
            int offsetZ = safeParseInt(editBoxOffsetZ.getValue(), 0);
            var be = this.menu.getBlockEntity();
            be.spinx = spinX;
            be.spiny = spinY;
            be.offsetx = offsetX;
            be.offsety = offsetY;
            be.offsetz = offsetZ;
            BlockPos pos = menu.getBlockEntity().getBlockPos();
            ModNetworking.CHANNEL.sendToServer(
                    new ScreenC2SPacket(pos, spinX, spinY, offsetX, offsetY, offsetZ)
            );

            this.minecraft.player.closeContainer();

        } catch (Exception e) {
            // 可以选择不做任何事，或者给玩家提示
            // this.minecraft.player.sendSystemMessage(Component.literal("输入格式错误！使用默认值0保存。"));
        }
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


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        AbstractScreenBlockEntity screen = menu.getBlockEntity();
        ResourceLocation texture = new ResourceLocation(vsie.ID, "textures/gui/iff/iff_gui.png");
        ResourceLocation iconradar = new ResourceLocation(vsie.ID, "textures/gui/screen/target_manual.png");
        ResourceLocation iconserverinfo = new ResourceLocation(vsie.ID, "textures/gui/screen/target_auto.png");
        guiGraphics.blit(texture,   // 用实例字段
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight);
        if(screen.displaytype == 0) {
            guiGraphics.blit(iconradar,   // 用实例字段
                    this.leftPos+78, this.topPos+70,
                    0, 0,
                    20, 20,
                    20, 20);
        }
        if(screen.displaytype == 1) {
            guiGraphics.blit(iconserverinfo,   // 用实例字段
                    this.leftPos+78, this.topPos+80,
                    0, 0,
                    20, 20,
                    20, 70);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // 背包标题
        //guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 可选：绘制静态文字标签
        guiGraphics.drawString(this.font, "spinx", this.leftPos+12, this.topPos+68, 0x404040, false);
        guiGraphics.drawString(this.font, "spiny", this.leftPos+52, this.topPos+68, 0x404040, false);
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
        return this.editBoxSpinX.keyPressed(keyCode, scanCode, modifiers) ||
                this.editBoxSpinY.keyPressed(keyCode, scanCode, modifiers) ||
                super.keyPressed(keyCode, scanCode, modifiers);
    }
}
