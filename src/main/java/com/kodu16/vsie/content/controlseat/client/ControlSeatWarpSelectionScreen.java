package com.kodu16.vsie.content.controlseat.client;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.item.warpdatachip.warp_data_chip;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatWarpTargetC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ControlSeatWarpSelectionScreen extends Screen {
    // 功能：限制选单一次最多显示的按钮数量，配合滚轮形成可滚动列表。
    private static final int MAX_VISIBLE_BUTTONS = 7;
    // 功能：统一按钮尺寸，方便选单纵向排布和鼠标点击命中。
    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    // 功能：为维度不匹配的 warp 目标提供深灰色按钮底色，明确提示该目标当前不可选。
    private static final int DIMENSION_MISMATCH_BUTTON_COLOR = 0xFF3A3A3A;
    // 功能：为维度不匹配的 warp 目标提供略亮的描边，让深灰按钮在深色背景上仍可辨识。
    private static final int DIMENSION_MISMATCH_BORDER_COLOR = 0xFF5A5A5A;
    // 功能：统一不可选按钮的文字颜色，避免使用默认高亮色误导玩家仍可点击。
    private static final int DISABLED_LABEL_COLOR = 0xFF9A9A9A;

    private final BlockPos controlSeatPos;
    // 功能：缓存当前控制椅仓储内可显示的 warp data chip 选项，避免每帧重复解析全部物品。
    private final List<WarpOption> options = new ArrayList<>();
    private int scrollOffset = 0;

    public ControlSeatWarpSelectionScreen(BlockPos controlSeatPos) {
        super(Component.literal("Warp Target Select"));
        this.controlSeatPos = controlSeatPos;
    }

    @Override
    protected void init() {
        super.init();
        rebuildOptions();
        rebuildButtons();
    }

    // 功能：从客户端控制椅方块实体中提取选项文本，按仓位顺序构建 warp 目标列表。
    private void rebuildOptions() {
        options.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        // 功能：记录玩家当前所在维度，用于把跨维度的 warp 目标直接标记为不可选。
        String currentDimensionId = mc.player != null ? mc.player.level().dimension().location().toString() : mc.level.dimension().location().toString();
        BlockEntity blockEntity = mc.level.getBlockEntity(controlSeatPos);
        if (!(blockEntity instanceof ControlSeatBlockEntity controlSeat)) {
            return;
        }
        for (int slot = 0; slot < controlSeat.getWarpChipInventory().getSlots(); slot++) {
            ItemStack stack = controlSeat.getWarpChipInventory().getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            warp_data_chip.StoredWarpData storedWarpData = warp_data_chip.readStoredWarpData(stack);
            String chipName = stack.getHoverName().getString();
            String label;
            boolean active;
            boolean dimensionMismatch = false;
            if (storedWarpData != null) {
                // 功能：当芯片记录维度与玩家当前维度不同步时，禁用对应按钮并追加原因说明。
                dimensionMismatch = !storedWarpData.dimensionId().equals(currentDimensionId);
                label = String.format("[%02d] %s (%d, %d, %d) - %s%s",
                        slot + 1,
                        storedWarpData.dimensionId(),
                        storedWarpData.pos().getX(),
                        storedWarpData.pos().getY(),
                        storedWarpData.pos().getZ(),
                        chipName,
                        dimensionMismatch ? " [维度不匹配]" : "");
                active = !dimensionMismatch;
            } else {
                label = String.format("[%02d] 未记录坐标 - %s", slot + 1, chipName);
                active = false;
            }
            options.add(new WarpOption(slot, Component.literal(label), active, dimensionMismatch));
        }
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, options.size() - MAX_VISIBLE_BUTTONS));
    }

    // 功能：根据当前滚动偏移重建按钮列，让滚轮滚动后只显示对应区间的选项。
    private void rebuildButtons() {
        clearWidgets();
        int visibleCount = Math.min(MAX_VISIBLE_BUTTONS, options.size());
        int startX = (this.width - BUTTON_WIDTH) / 2;
        int startY = (this.height - (visibleCount * BUTTON_HEIGHT + Math.max(0, visibleCount - 1) * BUTTON_GAP)) / 2;
        for (int index = 0; index < visibleCount; index++) {
            WarpOption option = options.get(scrollOffset + index);
            int buttonY = startY + index * (BUTTON_HEIGHT + BUTTON_GAP);
            Button button = new WarpOptionButton(option, startX, buttonY);
            // 功能：禁用未写入坐标或维度不匹配的芯片按钮，避免玩家把非法目标写进 control seat。
            button.active = option.active();
            addRenderableWidget(button);
        }
    }

    // 功能：玩家点选按钮后，把对应仓位发给服务端，由服务端校验并写入控制椅的下一次跃迁目标。
    private void selectWarpTarget(int slot) {
        ModNetworking.CHANNEL.sendToServer(new ControlSeatWarpTargetC2SPacket(controlSeatPos, slot));
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.size() <= MAX_VISIBLE_BUTTONS) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int nextOffset = scrollOffset - (delta > 0 ? 1 : -1);
        int clampedOffset = Mth.clamp(nextOffset, 0, Math.max(0, options.size() - MAX_VISIBLE_BUTTONS));
        if (clampedOffset != scrollOffset) {
            scrollOffset = clampedOffset;
            rebuildButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 功能：防止界面刚打开时 Screen 尚未拿到 minecraft/font 引用就进入渲染，从而触发原版 renderBackground 空指针。
        if (this.minecraft == null) {
            return;
        }

        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 功能：在选单顶部提示玩家滚轮滚动/左键选择当前跃迁目标。
        guiGraphics.drawCenteredString(this.font, Component.literal("选择 control seat 的跃迁目标"), this.width / 2, this.height / 2 - 92, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("滚轮上下滑动，左键选定 warp data chip"), this.width / 2, this.height / 2 - 78, 0xA0E0FF);
        // 功能：补充说明跨维度记录会被锁定，帮助玩家理解深灰色按钮的含义。
        guiGraphics.drawCenteredString(this.font, Component.literal("深灰色按钮表示记录维度与当前维度不同，当前不可选择"), this.width / 2, this.height / 2 - 64, 0x909090);

        if (options.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal("控制椅仓储内没有 warp data chip"), this.width / 2, this.height / 2, 0xFF8080);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // 功能：用轻量结构记录每个按钮对应的仓位、显示文案、是否可选以及是否因维度不匹配被禁用。
    private record WarpOption(int slot, Component label, boolean active, boolean dimensionMismatch) {
    }

    // 功能：为 warp 选项提供自定义按钮渲染，让跨维度禁用项显示为深灰色而不是默认样式。
    private final class WarpOptionButton extends Button {
        private final WarpOption option;

        private WarpOptionButton(WarpOption option, int x, int y) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, option.label(), btn -> selectWarpTarget(option.slot()), DEFAULT_NARRATION);
            this.option = option;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (option.dimensionMismatch()) {
                // 功能：跨维度目标即使被禁用，也额外绘制深灰底色与描边，保证视觉上明确不可选。
                int left = getX();
                int top = getY();
                int right = left + this.width;
                int bottom = top + this.height;
                guiGraphics.fill(left, top, right, bottom, DIMENSION_MISMATCH_BUTTON_COLOR);
                guiGraphics.fill(left, top, right, top + 1, DIMENSION_MISMATCH_BORDER_COLOR);
                guiGraphics.fill(left, bottom - 1, right, bottom, DIMENSION_MISMATCH_BORDER_COLOR);
                guiGraphics.fill(left, top, left + 1, bottom, DIMENSION_MISMATCH_BORDER_COLOR);
                guiGraphics.fill(right - 1, top, right, bottom, DIMENSION_MISMATCH_BORDER_COLOR);
                guiGraphics.drawCenteredString(ControlSeatWarpSelectionScreen.this.font, this.getMessage(), left + this.width / 2, top + (this.height - 8) / 2, DISABLED_LABEL_COLOR);
                return;
            }
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
