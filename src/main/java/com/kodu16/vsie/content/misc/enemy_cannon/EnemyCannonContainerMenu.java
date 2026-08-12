package com.kodu16.vsie.content.misc.enemy_cannon;

import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.ModMenuTypes;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EnemyCannonContainerMenu extends AbstractContainerMenu {
    private static final int CANNON_SLOT_COUNT = 1;
    private final BlockPos blockPosition;
    private final int chargeCount;
    private final int cooldownTicks;

    public EnemyCannonContainerMenu(
            int id,
            Inventory playerInventory,
            EnemyCannonBlockEntity cannon,
            int chargeCount,
            int cooldownTicks
    ) {
        super(ModMenuTypes.ENEMY_CANNON_MENU.get(), id);
        this.blockPosition = cannon.getBlockPos();
        this.chargeCount = chargeCount;
        this.cooldownTicks = cooldownTicks;

        addSlot(new SlotItemHandler(cannon, 0, 24, 38));
        addPlayerSlots(playerInventory);
    }

    private void addPlayerSlots(Inventory inventory) {
        // Function: retain the standard inventory layout so normal slot packets handle the projectile item.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + column + row * 9, 8 + column * 18, 98 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 156));
        }
    }

    public BlockPos getBlockPosition() {
        return blockPosition;
    }

    public int getChargeCount() {
        return chargeCount;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(blockPosition) instanceof EnemyCannonBlockEntity)) {
            return false;
        }
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(player.level(), blockPosition);
        return player.position().distanceToSqr(ServerShipUtils.getBlockCenterWorld(subLevel, blockPosition)) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();
        if (index < CANNON_SLOT_COUNT) {
            if (!moveItemStackTo(moving, CANNON_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(moving, 0, CANNON_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (moving.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (moving.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, moving);
        return original;
    }
}
