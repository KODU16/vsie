package com.kodu16.vsie.content.misc.enemy_autocannon;

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

public class EnemyAutocannonContainerMenu extends AbstractContainerMenu {
    private static final int AUTOCANNON_SLOT_COUNT = 1;
    private final BlockPos blockPosition;
    private final double spreadAngle;
    private final int shotIntervalTicks;
    private final int burstShots;

    public EnemyAutocannonContainerMenu(
            int id,
            Inventory playerInventory,
            EnemyAutocannonBlockEntity autocannon,
            double spreadAngle,
            int shotIntervalTicks,
            int burstShots
    ) {
        super(ModMenuTypes.ENEMY_AUTOCANNON_MENU.get(), id);
        this.blockPosition = autocannon.getBlockPos();
        this.spreadAngle = spreadAngle;
        this.shotIntervalTicks = shotIntervalTicks;
        this.burstShots = burstShots;
        addSlot(new SlotItemHandler(autocannon, 0, 24, 38));
        addPlayerSlots(playerInventory);
    }

    private void addPlayerSlots(Inventory inventory) {
        // Function: use vanilla slot synchronization so the CBC round retains all item components.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + column + row * 9, 8 + column * 18, 106 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 164));
        }
    }

    public BlockPos getBlockPosition() { return blockPosition; }
    public double getSpreadAngle() { return spreadAngle; }
    public int getShotIntervalTicks() { return shotIntervalTicks; }
    public int getBurstShots() { return burstShots; }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(blockPosition) instanceof EnemyAutocannonBlockEntity)) {
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
        if (index < AUTOCANNON_SLOT_COUNT) {
            if (!moveItemStackTo(moving, AUTOCANNON_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(moving, 0, AUTOCANNON_SLOT_COUNT, false)) {
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
