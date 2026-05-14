package com.kodu16.vsie.content.weapon.missile_launcher.block;

import com.kodu16.vsie.content.weapon.missile_launcher.AbstractMissileLauncherBlockEntity;
import com.kodu16.vsie.registries.vsieItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class BasicMissileLauncherBlockEntity extends AbstractMissileLauncherBlockEntity implements IItemHandlerModifiable {
    private static final String MISSILE_INVENTORY_TAG = "BasicMissileInventory";

    // Function: internal 3x3 ammo buffer; only basic_missile items are valid launch ammo.
    private final ItemStackHandler missileInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(vsieItems.BASIC_MISSILE_ITEM.get());
        }
    };

    public BasicMissileLauncherBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return 20;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("MSL");
    }

    @Override
    public String getmissilelaunchertype() {
        return "basic_missile_launcher";
    }

    @Override
    public String getweapontype() {
        return "basic_missile_launcher";
    }

    @Override
    protected boolean hasMissileAmmo() {
        // Function: check ammo availability before creating the missile entity.
        for (int slot = 0; slot < missileInventory.getSlots(); slot++) {
            if (!missileInventory.extractItem(slot, 1, true).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean consumeMissileAmmo() {
        // Function: each fired missile consumes one stored basic_missile item.
        for (int slot = 0; slot < missileInventory.getSlots(); slot++) {
            ItemStack extracted = missileInventory.extractItem(slot, 1, false);
            if (!extracted.isEmpty()) {
                setChanged();
                return true;
            }
        }
        return false;
    }

    public void dropStoredMissiles(Level level, BlockPos pos) {
        // Function: preserve loaded missiles when the launcher block is broken.
        for (int slot = 0; slot < missileInventory.getSlots(); slot++) {
            ItemStack stack = missileInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack.copy());
                missileInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public IItemHandlerModifiable getItemHandler() {
        return this;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        // Function: save launcher ammo so loaded missiles survive world reloads.
        tag.put(MISSILE_INVENTORY_TAG, missileInventory.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(MISSILE_INVENTORY_TAG)) {
            // Function: restore the internal missile buffer for firing and GUI sync.
            missileInventory.deserializeNBT(registries, tag.getCompound(MISSILE_INVENTORY_TAG));
        }
    }

    @Override
    public int getSlots() {
        return missileInventory.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return missileInventory.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return missileInventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return missileInventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return missileInventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return missileInventory.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        missileInventory.setStackInSlot(slot, stack);
        setChanged();
    }
}
