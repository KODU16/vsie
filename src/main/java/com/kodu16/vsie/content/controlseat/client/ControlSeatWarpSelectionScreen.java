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
    private static final int MAX_VISIBLE_BUTTONS = 7;
    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int HEADER_HEIGHT = 52;
    private static final int RESERVED_VERTICAL_PADDING = 24;
    private static final int DIMENSION_MISMATCH_BUTTON_COLOR = 0xFF3A3A3A;
    private static final int DIMENSION_MISMATCH_BORDER_COLOR = 0xFF5A5A5A;
    private static final int DISABLED_LABEL_COLOR = 0xFF9A9A9A;

    private final BlockPos controlSeatPos;
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

    private void rebuildOptions() {
        options.clear();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        String currentDimensionId = minecraft.player != null
                ? minecraft.player.level().dimension().location().toString()
                : minecraft.level.dimension().location().toString();
        BlockEntity blockEntity = minecraft.level.getBlockEntity(controlSeatPos);
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
                dimensionMismatch = !storedWarpData.dimensionId().equals(currentDimensionId);
                label = String.format("[%02d] %s (%d, %d, %d) - %s%s",
                        slot + 1,
                        storedWarpData.dimensionId(),
                        storedWarpData.pos().getX(),
                        storedWarpData.pos().getY(),
                        storedWarpData.pos().getZ(),
                        chipName,
                        dimensionMismatch ? " [\u7ef4\u5ea6\u4e0d\u5339\u914d]" : "");
                active = !dimensionMismatch;
            } else {
                label = String.format("[%02d] \u672a\u8bb0\u5f55\u5750\u6807 - %s", slot + 1, chipName);
                active = false;
            }
            options.add(new WarpOption(slot, Component.literal(label), active, dimensionMismatch));
        }
        int visibleCount = getVisibleCount();
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, options.size() - visibleCount));
    }

    private void rebuildButtons() {
        clearWidgets();
        int visibleCount = getVisibleCount();
        int startX = (this.width - BUTTON_WIDTH) / 2;
        int startY = getListStartY(visibleCount);
        for (int index = 0; index < visibleCount; index++) {
            WarpOption option = options.get(scrollOffset + index);
            Button button = new WarpOptionButton(option, startX, startY + index * (BUTTON_HEIGHT + BUTTON_GAP));
            button.active = option.active();
            addRenderableWidget(button);
        }
    }

    private void selectWarpTarget(int slot) {
        ModNetworking.sendToServer(new ControlSeatWarpTargetC2SPacket(controlSeatPos, slot));
        onClose();
    }

    private int getVisibleCount() {
        int maxByHeight = Math.max(1, (this.height - HEADER_HEIGHT - RESERVED_VERTICAL_PADDING + BUTTON_GAP) / (BUTTON_HEIGHT + BUTTON_GAP));
        return Math.min(options.size(), Math.min(MAX_VISIBLE_BUTTONS, maxByHeight));
    }

    private int getListStartY(int visibleCount) {
        int listHeight = visibleCount * BUTTON_HEIGHT + Math.max(0, visibleCount - 1) * BUTTON_GAP;
        int contentHeight = HEADER_HEIGHT + listHeight;
        return Math.max(HEADER_HEIGHT + 8, (this.height - contentHeight) / 2 + HEADER_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int visibleCount = getVisibleCount();
        if (options.size() <= visibleCount || scrollY == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int nextOffset = scrollOffset - (scrollY > 0 ? 1 : -1);
        int clampedOffset = Mth.clamp(nextOffset, 0, Math.max(0, options.size() - visibleCount));
        if (clampedOffset != scrollOffset) {
            scrollOffset = clampedOffset;
            rebuildButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft == null) {
            return;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int headerY = getListStartY(getVisibleCount()) - HEADER_HEIGHT;
        guiGraphics.drawCenteredString(this.font, Component.literal("\u9009\u62e9 control seat \u7684\u8dc3\u8fc1\u76ee\u6807"), this.width / 2, headerY, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("\u6eda\u8f6e\u4e0a\u4e0b\u6ed1\u52a8\uff0c\u5de6\u952e\u9009\u5b9a warp data chip"), this.width / 2, headerY + 14, 0xA0E0FF);
        guiGraphics.drawCenteredString(this.font, Component.literal("\u6df1\u7070\u8272\u6309\u94ae\u8868\u793a\u8bb0\u5f55\u7ef4\u5ea6\u4e0e\u5f53\u524d\u7ef4\u5ea6\u4e0d\u540c\uff0c\u5f53\u524d\u4e0d\u53ef\u9009\u62e9"), this.width / 2, headerY + 28, 0x909090);

        if (options.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal("\u63a7\u5236\u6905\u4ed3\u50a8\u5185\u6ca1\u6709 warp data chip"), this.width / 2, headerY + HEADER_HEIGHT + 12, 0xFF8080);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record WarpOption(int slot, Component label, boolean active, boolean dimensionMismatch) {
    }

    private final class WarpOptionButton extends Button {
        private final WarpOption option;

        private WarpOptionButton(WarpOption option, int x, int y) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, option.label(), btn -> selectWarpTarget(option.slot()), DEFAULT_NARRATION);
            this.option = option;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (option.dimensionMismatch()) {
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
