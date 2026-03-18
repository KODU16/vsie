package com.kodu16.vsie.content.controlseat.gui;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.registries.ModMenuTypes;
import com.kodu16.vsie.registries.vsieItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class ControlSeatWarpContainerMenu extends AbstractContainerMenu {

    private static final int CONTAINER_SLOT_COUNT = 27;
    private static final int PLAYER_INVENTORY_START = CONTAINER_SLOT_COUNT;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_START + 27;
    private static final int TOTAL_SLOT_COUNT = PLAYER_HOTBAR_START + 9;

    private final IItemHandler controlSeatInventory;
    private final BlockPos blockPosition;

    public ControlSeatWarpContainerMenu(int id, Inventory playerInventory, IItemHandler controlSeatInventory, BlockPos pos) {
        super(ModMenuTypes.CONTROL_SEAT_WARP_MENU.get(), id);
        this.controlSeatInventory = controlSeatInventory;
        this.blockPosition = pos;

        int startX = 8;
        int startY = 18;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                int x = startX + col * 18;
                int y = startY + row * 18;

                // 功能：控制椅专用 GUI 的 27 个槽位仅接受 warp data chip。
                this.addSlot(new ControlSeatWarpSlot(controlSeatInventory, index, x, y));
            }
        }

        int playerInvY = startY + 4 * 18 - 6;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                int x = startX + col * 18;
                int y = playerInvY + row * 18;
                this.addSlot(new Slot(playerInventory, index, x, y));
            }
        }

        int hotbarY = playerInvY + 3 * 18;
        for (int col = 0; col < 9; col++) {
            int x = startX + col * 18;
            this.addSlot(new Slot(playerInventory, col, x, hotbarY));
        }
    }

    public ControlSeatWarpContainerMenu(int id, Inventory playerInventory, ControlSeatBlockEntity controlSeatBlockEntity) {
        this(id, playerInventory, controlSeatBlockEntity.getWarpChipInventory(), controlSeatBlockEntity.getBlockPos());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            if (index < CONTAINER_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 功能：Shift 点击玩家背包时，只把 warp data chip 快速转移到控制椅专用仓位。
                if (!stack.is(vsieItems.WARP_DATA_CHIP.get())) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stack, 0, CONTAINER_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stack, original);
        }

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.isRemoved()) {
            return false;
        }

        if (!(player.level().getBlockEntity(blockPosition) instanceof ControlSeatBlockEntity)) {
            return false;
        }

        double maxDistSq = 8.0 * 8.0;
        return player.distanceToSqr(blockPosition.getX() + 0.5,
                blockPosition.getY() + 0.5,
                blockPosition.getZ() + 0.5) <= maxDistSq;
    }
}
