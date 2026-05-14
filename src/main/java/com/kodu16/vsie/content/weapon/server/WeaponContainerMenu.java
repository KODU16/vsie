package com.kodu16.vsie.content.weapon.server;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.BasicMissileLauncherBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotCoreBlockEntity;
import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class WeaponContainerMenu extends AbstractContainerMenu {
    public static final int INTERNAL_SLOT_COUNT = 9;
    public static final int INTERNAL_SLOT_X = 59;
    public static final int INTERNAL_SLOT_Y = 64;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 128;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;

    private final AbstractWeaponBlockEntity blockEntity;
    private final boolean hasInventorySlots;

    public WeaponContainerMenu(int id, Inventory playerInv, AbstractWeaponBlockEntity be) {
        super(ModMenuTypes.WEAPON_MENU.get(), id);
        this.blockEntity = be;
        this.hasInventorySlots = be instanceof BasicMissileLauncherBlockEntity
                || be instanceof VerticleLaunchingSlotCoreBlockEntity;

        if (be instanceof net.neoforged.neoforge.items.IItemHandlerModifiable missileInventory
                && (be instanceof BasicMissileLauncherBlockEntity || be instanceof VerticleLaunchingSlotCoreBlockEntity)) {
            // Function: missile launch weapons store launchable basic_missile items in a 3x3 buffer.
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int slotIndex = col + row * 3;
                    this.addSlot(new SlotItemHandler(missileInventory, slotIndex,
                            INTERNAL_SLOT_X + col * 18,
                            INTERNAL_SLOT_Y + row * 18));
                }
            }
            addPlayerInventory(playerInv);
        }
    }

    private void addPlayerInventory(Inventory playerInv) {
        // Function: expose player inventory for direct loading and shift-click transfers.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                this.addSlot(new Slot(playerInv, index, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved();
    }

    public AbstractWeaponBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean hasInventorySlots() {
        return hasInventorySlots;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!hasInventorySlots || index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
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
        }
        return original;
    }
}
