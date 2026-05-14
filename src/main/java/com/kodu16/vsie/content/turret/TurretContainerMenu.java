package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.content.turret.block.ParticleTurretBlockEntity;
import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class TurretContainerMenu extends AbstractContainerMenu {
    public static final int INTERNAL_SLOT_COUNT = 9;
    public static final int INTERNAL_SLOT_X = 59;
    public static final int INTERNAL_SLOT_Y = 17;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 128;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;

    private final AbstractTurretBlockEntity blockEntity;
    private final boolean hasInventorySlots;

    public TurretContainerMenu(int id, Inventory playerInv, AbstractTurretBlockEntity be) {
        super(ModMenuTypes.TURRET_MENU.get(), id);
        this.blockEntity = be;
        this.hasInventorySlots = be instanceof ParticleTurretBlockEntity;

        if (be instanceof ParticleTurretBlockEntity particleTurretBlockEntity) {
            // Function: Particle Turret keeps a 3x3 ammo buffer before the player inventory slots.
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int slotIndex = col + row * 3;
                    this.addSlot(new SlotItemHandler(particleTurretBlockEntity, slotIndex,
                            INTERNAL_SLOT_X + col * 18,
                            INTERNAL_SLOT_Y + row * 18));
                }
            }
            addPlayerInventory(playerInv);
        }
    }

    private void addPlayerInventory(Inventory playerInv) {
        // Function: expose the normal player inventory so containers can be shift-click loaded.
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

    public AbstractTurretBlockEntity getBlockEntity() {
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
