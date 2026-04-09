package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.TurretContainerMenu;
import com.kodu16.vsie.content.turret.ciws.AbstractCIWSBlockEntity;
import com.kodu16.vsie.content.turret.block.ParticleTurretBlockEntity;
import com.kodu16.vsie.network.screen.ScreenC2SPacket;
import com.kodu16.vsie.network.turret.TurretDefaultSpinC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;                  // 新增
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
    private EditBox editBoxSpinX;
    private EditBox editBoxSpinY;
    private static final ResourceLocation TEXTURE = new ResourceLocation(vsie.ID, "textures/gui/turret/turret_gui.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation(vsie.ID, "textures/gui/slot.png");

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
        ResourceLocation iconhostile = turret.getData().isTargetsHostile() ? new ResourceLocation(vsie.ID, "textures/gui/turret/target_hostile_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/turret/target_hostile_off.png");
        ResourceLocation iconpassive = turret.getData().isTargetsPassive() ? new ResourceLocation(vsie.ID, "textures/gui/turret/target_passive_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/turret/target_passive_off.png");
        ResourceLocation iconplayer = turret.getData().isTargetsPlayers() ? new ResourceLocation(vsie.ID, "textures/gui/turret/target_players_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/turret/target_players_off.png");
        ResourceLocation iconship = turret.getData().isTargetsShip() ? new ResourceLocation(vsie.ID, "textures/gui/turret/target_ship_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/turret/target_ship_off.png");
        ResourceLocation iconciws = turret.getData().isTargetsShip() ? new ResourceLocation(vsie.ID, "textures/gui/turret/target_ciws_on.png")
                : new ResourceLocation(vsie.ID, "textures/gui/turret/target_ciws_off.png");

        // 绘制状态图标
        guiGraphics.blit(iconhostile, this.leftPos + 20, this.topPos + 70, 0, 0, 19, 19, 19,19); // 你可以调整位置和大小
        guiGraphics.blit(iconpassive, this.leftPos + 59, this.topPos + 70, 0, 0, 19, 19, 19,19);
        guiGraphics.blit(iconplayer, this.leftPos + 98, this.topPos + 70, 0, 0, 19, 19, 19,19);
        if(menu.getBlockEntity() instanceof AbstractCIWSBlockEntity) {
            guiGraphics.blit(iconciws, this.leftPos + 137, this.topPos + 70, 0, 0, 19, 19, 19,19);
        }
        else {
            guiGraphics.blit(iconship, this.leftPos + 137, this.topPos + 70, 0, 0, 19, 19, 19,19);
        }
        if (turret instanceof ParticleTurretBlockEntity) {
            // 功能：在粒子炮 GUI 中绘制 3x3 弹药槽底图，位置与容器菜单槽位一一对应。
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            int slotStartX = this.leftPos + 59 - 1;
            int slotStartY = this.topPos + 17 - 1;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    guiGraphics.blit(SLOT_TEXTURE,
                            slotStartX + col * 18,
                            slotStartY + row * 18,
                            0, 0, 18, 18, 18, 18);
                }
            }
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
        var be = this.menu.getBlockEntity();
        int spinX  = be.defaultspinx;   // 假设你有这些 getter
        int spinY  = be.defaultspiny;
        this.editBoxSpinX = createIntEditBox("SpinX", this.leftPos+48+16+48, 48, String.valueOf(spinX));
        this.editBoxSpinY = createIntEditBox("SpinY", this.leftPos+48, 48, String.valueOf(spinY));

        this.addRenderableWidget(Button.builder(
                        Component.literal("HOS"),
                        button -> ModNetworking.CHANNEL.sendToServer(new TurretC2SPacket(pos,1)))
                .pos(this.leftPos + 16, this.topPos + 100)
                .size(27, 15)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.literal("PAS"),
                        button -> ModNetworking.CHANNEL.sendToServer(new TurretC2SPacket(pos,2)))
                .pos(this.leftPos + 55, this.topPos + 100)
                .size(27, 15)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.literal("Player"),
                        button -> ModNetworking.CHANNEL.sendToServer(new TurretC2SPacket(pos,3)))
                .pos(this.leftPos + 94, this.topPos + 100)
                .size(27, 15)
                .build());        this.addRenderableWidget(Button.builder(
                        Component.literal("Ship"),
                        button -> ModNetworking.CHANNEL.sendToServer(new TurretC2SPacket(pos,4)))
                .pos(this.leftPos + 133, this.topPos + 100)
                .size(27, 15)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("保存"),
                        button -> saveAndClose()
                )
                .bounds(this.leftPos+32, this.topPos+140, 40, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("取消"),
                        button -> this.minecraft.player.closeContainer()
                )
                .bounds(this.leftPos+72, this.topPos+140, 40, 20)
                .build());
    }

    // 抽取成方法，方便复用 + 设置初始值
    private EditBox createIntEditBox(String name, int x, int y, String initialValue) {
        EditBox box = new EditBox(this.font,
                x, y,
                24, 10,
                Component.literal(name));
        box.setMaxLength(8);           // int 范围够用
        box.setValue(initialValue);    // ← 关键！设置初始值
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
        try {
            int spinX   = safeParseInt(editBoxSpinX.getValue(), 0);
            int spinY   = safeParseInt(editBoxSpinY.getValue(), 0);
            var be = this.menu.getBlockEntity();
            be.defaultspinx = spinX;
            be.defaultspiny = spinY;
            BlockPos pos = menu.getBlockEntity().getBlockPos();
            ModNetworking.CHANNEL.sendToServer(
                    new TurretDefaultSpinC2SPacket(pos, spinX, spinY)
            );
            this.minecraft.player.closeContainer();
        } catch (Exception e) {
            // 可以选择不做任何事，或者给玩家提示
            // this.minecraft.player.sendSystemMessage(Component.literal("输入格式错误！使用默认值0保存。"));
        }

    }
}
