package com.kodu16.vsie.content.misc.enemy_cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnemyCannonBlockEntity extends BlockEntity implements MenuProvider, IItemHandlerModifiable {
    public static final int MIN_CHARGE_COUNT = 1;
    public static final int MAX_CHARGE_COUNT = 64;
    public static final int MIN_COOLDOWN_TICKS = 1;
    public static final int MAX_COOLDOWN_TICKS = 72000;

    private BlockPos linkedEnemyCorePos = BlockPos.ZERO;
    private final ItemStackHandler projectileInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && EnemyCannonCbcCompat.isSupportedProjectile(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            // Function: persist GUI slot edits without dropping the configured projectile on block destruction.
            setChanged();
        }
    };
    private int chargeCount = 7;
    private int cooldownTicks = 200;
    private long nextAllowedFireGameTime;

    public EnemyCannonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public BlockPos getLinkedEnemyCorePos() {
        return linkedEnemyCorePos;
    }

    public void setLinkedEnemyCorePos(BlockPos pos) {
        linkedEnemyCorePos = pos == null ? BlockPos.ZERO : pos.immutable();
        setChanged();
        if (level != null) {
            // Function: keep linker state current on clients without requiring a ticking block entity.
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getChargeCount() {
        return chargeCount;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void applySettings(int chargeCount, int cooldownTicks) {
        this.chargeCount = Math.clamp(chargeCount, MIN_CHARGE_COUNT, MAX_CHARGE_COUNT);
        this.cooldownTicks = Math.clamp(cooldownTicks, MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS);
        setChanged();
    }

    public boolean isReadyToFire(long gameTime) {
        return !projectileInventory.getStackInSlot(0).isEmpty() && gameTime >= nextAllowedFireGameTime;
    }

    public ItemStack getConfiguredProjectile() {
        return projectileInventory.getStackInSlot(0).copyWithCount(1);
    }

    public void markFired(long gameTime) {
        // Function: each cannon owns its firing cadence independently of the enemy core.
        nextAllowedFireGameTime = gameTime + cooldownTicks;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linkedEnemyCorePos = tag.contains("LinkedEnemyCorePos")
                ? BlockPos.of(tag.getLong("LinkedEnemyCorePos"))
                : BlockPos.ZERO;
        if (tag.contains("ProjectileInventory")) {
            projectileInventory.deserializeNBT(registries, tag.getCompound("ProjectileInventory"));
        }
        chargeCount = tag.contains("ChargeCount")
                ? Math.clamp(tag.getInt("ChargeCount"), MIN_CHARGE_COUNT, MAX_CHARGE_COUNT)
                : 7;
        cooldownTicks = tag.contains("CooldownTicks")
                ? Math.clamp(tag.getInt("CooldownTicks"), MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS)
                : 200;
        nextAllowedFireGameTime = Math.max(0L, tag.getLong("NextAllowedFireGameTime"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!linkedEnemyCorePos.equals(BlockPos.ZERO)) {
            tag.putLong("LinkedEnemyCorePos", linkedEnemyCorePos.asLong());
        }
        tag.put("ProjectileInventory", projectileInventory.serializeNBT(registries));
        tag.putInt("ChargeCount", chargeCount);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putLong("NextAllowedFireGameTime", nextAllowedFireGameTime);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vsie.enemy_cannon");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EnemyCannonContainerMenu(id, inventory, this, chargeCount, cooldownTicks);
    }

    @Override
    public int getSlots() {
        return projectileInventory.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return projectileInventory.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return projectileInventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return projectileInventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return projectileInventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return projectileInventory.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        projectileInventory.setStackInSlot(slot, stack);
    }
}
