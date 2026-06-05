package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class TurretContainerMenu extends AbstractContainerMenu {
    public static final int INTERNAL_SLOT_COUNT = 9;
    public static final int INTERNAL_SLOT_X = 33;
    public static final int INTERNAL_SLOT_Y = 162;
    public static final int PLAYER_INVENTORY_X = 33;
    public static final int PLAYER_INVENTORY_Y = 198;
    public static final int PLAYER_HOTBAR_Y = 256;
    private static final int PLAYER_INVENTORY_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_INVENTORY_START_SLOT = 9;
    private static final int PLAYER_HOTBAR_START_SLOT = 0;

    private final AbstractTurretBlockEntity blockEntity;
    private final boolean hasAmmoSlots;

    public TurretContainerMenu(int id, Inventory playerInv, AbstractTurretBlockEntity be) {
        super(ModMenuTypes.TURRET_MENU.get(), id);
        this.blockEntity = be;
        this.hasAmmoSlots = be.hasAmmoInventorySlots();

        if (hasAmmoSlots) {
            // Function: non-energy turrets expose one horizontal 9-slot ammo row in the shared turret GUI.
            for (int col = 0; col < INTERNAL_SLOT_COUNT; col++) {
                this.addSlot(new SlotItemHandler(be, col, INTERNAL_SLOT_X + col * 18, INTERNAL_SLOT_Y));
            }
            addPlayerInventorySlots(playerInv);
        }
    }

    private void addPlayerInventorySlots(Inventory playerInv) {
        // Function: ammo-fed turrets expose the player inventory so ammo can be moved without closing the GUI.
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
            for (int col = 0; col < PLAYER_INVENTORY_COLUMNS; col++) {
                this.addSlot(new Slot(playerInv, PLAYER_INVENTORY_START_SLOT + col + row * PLAYER_INVENTORY_COLUMNS,
                        PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < PLAYER_INVENTORY_COLUMNS; col++) {
            this.addSlot(new Slot(playerInv, PLAYER_HOTBAR_START_SLOT + col,
                    PLAYER_INVENTORY_X + col * 18, PLAYER_HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved();
    }

    public AbstractTurretBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean hasAmmoSlots() {
        return hasAmmoSlots;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!hasAmmoSlots || index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INTERNAL_SLOT_COUNT) {
            if (!moveItemStackTo(stack, INTERNAL_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, INTERNAL_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
