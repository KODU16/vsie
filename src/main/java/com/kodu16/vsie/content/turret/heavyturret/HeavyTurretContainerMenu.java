package com.kodu16.vsie.content.turret.heavyturret;

import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class HeavyTurretContainerMenu extends AbstractContainerMenu {
    public static final int INTERNAL_SLOT_COUNT = 9;
    public static final int INTERNAL_SLOT_X = 33;
    public static final int INTERNAL_SLOT_Y = 190;
    public static final int PLAYER_INVENTORY_X = 33;
    public static final int PLAYER_INVENTORY_Y = 232;
    public static final int PLAYER_HOTBAR_Y = 290;
    private static final int PLAYER_INVENTORY_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_INVENTORY_START_SLOT = 9;
    private static final int PLAYER_HOTBAR_START_SLOT = 0;

    private final AbstractHeavyTurretBlockEntity blockEntity;
    private final boolean hasAmmoSlots;

    public HeavyTurretContainerMenu(int id, Inventory playerInv, AbstractHeavyTurretBlockEntity be) {
        super(ModMenuTypes.HEAVY_TURRET_MENU.get(), id);
        this.blockEntity = be;
        this.hasAmmoSlots = be.hasAmmoInventorySlots();

        if (hasAmmoSlots) {
            addAmmoSlots(be);
            addPlayerInventorySlots(playerInv);
        }
    }

    private void addAmmoSlots(AbstractHeavyTurretBlockEntity be) {
        // Function: non-energy heavy turrets reuse the shared 1x9 ammo row at the bottom of the screen.
        for (int slot = 0; slot < INTERNAL_SLOT_COUNT; slot++) {
            this.addSlot(new SlotItemHandler(be, slot, INTERNAL_SLOT_X + slot * 18, INTERNAL_SLOT_Y));
        }
    }

    private void addPlayerInventorySlots(Inventory playerInv) {
        // Function: ammo-fed heavy turrets expose the player inventory below the turret controls.
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

    public AbstractHeavyTurretBlockEntity getBlockEntity() {
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
