package com.kodu16.vsie.content.storage.ammobox;

import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AmmoBoxContainerMenu extends AbstractContainerMenu {
    public static final int INTERNAL_SLOT_COLUMNS = 9;
    public static final int INTERNAL_SLOT_ROWS = 3;
    public static final int INTERNAL_SLOT_COUNT = INTERNAL_SLOT_COLUMNS * INTERNAL_SLOT_ROWS;
    public static final int INTERNAL_SLOT_X = 8;
    public static final int INTERNAL_SLOT_Y = 18;
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 84;
    public static final int PLAYER_HOTBAR_Y = 142;
    private static final int PLAYER_INVENTORY_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_INVENTORY_START_SLOT = 9;
    private static final int PLAYER_HOTBAR_START_SLOT = 0;

    private final IItemHandler ammoBoxInventory;
    private final BlockPos blockPosition;

    public AmmoBoxContainerMenu(int id, Inventory playerInventory, IItemHandler ammoBoxInventory, BlockPos pos) {
        super(ModMenuTypes.AMMO_BOX_MENU.get(), id);
        this.ammoBoxInventory = ammoBoxInventory;
        this.blockPosition = pos;

        addAmmoBoxSlots();
        addPlayerInventorySlots(playerInventory);
    }

    public AmmoBoxContainerMenu(int id, Inventory playerInventory, AmmoBoxBlockEntity ammoBox) {
        this(id, playerInventory, ammoBox.getInventory(), ammoBox.getBlockPos());
    }

    private void addAmmoBoxSlots() {
        for (int row = 0; row < INTERNAL_SLOT_ROWS; row++) {
            for (int col = 0; col < INTERNAL_SLOT_COLUMNS; col++) {
                int index = col + row * INTERNAL_SLOT_COLUMNS;
                this.addSlot(new SlotItemHandler(
                        ammoBoxInventory,
                        index,
                        INTERNAL_SLOT_X + col * 18,
                        INTERNAL_SLOT_Y + row * 18
                ));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
            for (int col = 0; col < PLAYER_INVENTORY_COLUMNS; col++) {
                int index = PLAYER_INVENTORY_START_SLOT + col + row * PLAYER_INVENTORY_COLUMNS;
                this.addSlot(new Slot(
                        playerInventory,
                        index,
                        PLAYER_INVENTORY_X + col * 18,
                        PLAYER_INVENTORY_Y + row * 18
                ));
            }
        }

        for (int col = 0; col < PLAYER_INVENTORY_COLUMNS; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    PLAYER_HOTBAR_START_SLOT + col,
                    PLAYER_INVENTORY_X + col * 18,
                    PLAYER_HOTBAR_Y
            ));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        original = stack.copy();

        if (index < INTERNAL_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, INTERNAL_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, INTERNAL_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
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
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.isRemoved()) {
            return false;
        }
        return player.level().getBlockEntity(blockPosition) instanceof AmmoBoxBlockEntity
                && player.distanceToSqr(
                blockPosition.getX() + 0.5D,
                blockPosition.getY() + 0.5D,
                blockPosition.getZ() + 0.5D
        ) <= 64.0D;
    }
}
