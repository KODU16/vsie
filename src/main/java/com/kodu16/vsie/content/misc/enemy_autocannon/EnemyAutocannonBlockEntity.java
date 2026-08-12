package com.kodu16.vsie.content.misc.enemy_autocannon;

import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

public class EnemyAutocannonBlockEntity extends BlockEntity implements MenuProvider, IItemHandlerModifiable {
    public static final double MIN_SPREAD_ANGLE = 0.0D;
    public static final double MAX_SPREAD_ANGLE = 45.0D;
    public static final int MIN_SHOT_INTERVAL_TICKS = 1;
    public static final int MAX_SHOT_INTERVAL_TICKS = 1200;
    public static final int MIN_BURST_SHOTS = 1;
    public static final int MAX_BURST_SHOTS = 1024;

    private BlockPos linkedEnemyCorePos = BlockPos.ZERO;
    private final ItemStackHandler projectileInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && EnemyAutocannonCbcCompat.isSupportedProjectile(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            // Function: retain the configured autocannon round as a non-consumed firing template.
            setChanged();
        }
    };
    private double spreadAngle = 1.0D;
    private int shotIntervalTicks = 2;
    private int burstShots = 10;
    private int remainingBurstShots;
    private long nextAllowedShotGameTime;
    private double burstSpawnForwardDistance;
    private ItemStack burstProjectileStack = ItemStack.EMPTY;

    public EnemyAutocannonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel) || remainingBurstShots <= 0
                || serverLevel.getGameTime() < nextAllowedShotGameTime) {
            return;
        }
        if (!(ServerShipUtils.getSubLevelAtBlockPos(serverLevel, worldPosition) instanceof ServerSubLevel ship)) {
            cancelBurst();
            return;
        }
        boolean fired = EnemyAutocannonCbcCompat.fireConfiguredProjectile(
                serverLevel,
                ship,
                worldPosition,
                burstSpawnForwardDistance,
                burstProjectileStack,
                spreadAngle
        );
        if (!fired) {
            cancelBurst();
            return;
        }
        remainingBurstShots--;
        if (remainingBurstShots <= 0) {
            burstProjectileStack = ItemStack.EMPTY;
        }
        // Function: the final shot also observes one shot interval before another burst becomes ready.
        nextAllowedShotGameTime = serverLevel.getGameTime() + shotIntervalTicks;
        setChanged();
    }

    public boolean isReadyToStartBurst(long gameTime) {
        return remainingBurstShots <= 0
                && gameTime >= nextAllowedShotGameTime
                && !projectileInventory.getStackInSlot(0).isEmpty();
    }

    public boolean startBurst(long gameTime, double spawnForwardDistance) {
        if (!isReadyToStartBurst(gameTime)) {
            return false;
        }
        remainingBurstShots = burstShots;
        nextAllowedShotGameTime = gameTime;
        burstSpawnForwardDistance = Math.max(0.0D, spawnForwardDistance);
        // Function: every shot in one burst uses the same complete item-component snapshot.
        burstProjectileStack = getConfiguredProjectile();
        setChanged();
        return true;
    }

    public boolean hasActiveBurst() {
        return remainingBurstShots > 0;
    }

    private void cancelBurst() {
        remainingBurstShots = 0;
        burstProjectileStack = ItemStack.EMPTY;
        setChanged();
    }

    public BlockPos getLinkedEnemyCorePos() {
        return linkedEnemyCorePos;
    }

    public void setLinkedEnemyCorePos(BlockPos pos) {
        linkedEnemyCorePos = pos == null ? BlockPos.ZERO : pos.immutable();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public double getSpreadAngle() {
        return spreadAngle;
    }

    public int getShotIntervalTicks() {
        return shotIntervalTicks;
    }

    public int getBurstShots() {
        return burstShots;
    }

    public ItemStack getConfiguredProjectile() {
        return projectileInventory.getStackInSlot(0).copyWithCount(1);
    }

    public void applySettings(double spreadAngle, int shotIntervalTicks, int burstShots) {
        this.spreadAngle = Math.clamp(spreadAngle, MIN_SPREAD_ANGLE, MAX_SPREAD_ANGLE);
        this.shotIntervalTicks = Math.clamp(shotIntervalTicks, MIN_SHOT_INTERVAL_TICKS, MAX_SHOT_INTERVAL_TICKS);
        this.burstShots = Math.clamp(burstShots, MIN_BURST_SHOTS, MAX_BURST_SHOTS);
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linkedEnemyCorePos = tag.contains("LinkedEnemyCorePos")
                ? BlockPos.of(tag.getLong("LinkedEnemyCorePos")) : BlockPos.ZERO;
        if (tag.contains("ProjectileInventory")) {
            projectileInventory.deserializeNBT(registries, tag.getCompound("ProjectileInventory"));
        }
        spreadAngle = tag.contains("SpreadAngle")
                ? Math.clamp(tag.getDouble("SpreadAngle"), MIN_SPREAD_ANGLE, MAX_SPREAD_ANGLE) : 1.0D;
        shotIntervalTicks = tag.contains("ShotIntervalTicks")
                ? Math.clamp(tag.getInt("ShotIntervalTicks"), MIN_SHOT_INTERVAL_TICKS, MAX_SHOT_INTERVAL_TICKS) : 2;
        burstShots = tag.contains("BurstShots")
                ? Math.clamp(tag.getInt("BurstShots"), MIN_BURST_SHOTS, MAX_BURST_SHOTS) : 10;
        remainingBurstShots = Math.clamp(tag.getInt("RemainingBurstShots"), 0, MAX_BURST_SHOTS);
        nextAllowedShotGameTime = Math.max(0L, tag.getLong("NextAllowedShotGameTime"));
        burstSpawnForwardDistance = Math.max(0.0D, tag.getDouble("BurstSpawnForwardDistance"));
        burstProjectileStack = tag.contains("BurstProjectile")
                ? ItemStack.parseOptional(registries, tag.getCompound("BurstProjectile")) : ItemStack.EMPTY;
        if (burstProjectileStack.isEmpty()) {
            remainingBurstShots = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!linkedEnemyCorePos.equals(BlockPos.ZERO)) {
            tag.putLong("LinkedEnemyCorePos", linkedEnemyCorePos.asLong());
        }
        tag.put("ProjectileInventory", projectileInventory.serializeNBT(registries));
        tag.putDouble("SpreadAngle", spreadAngle);
        tag.putInt("ShotIntervalTicks", shotIntervalTicks);
        tag.putInt("BurstShots", burstShots);
        tag.putInt("RemainingBurstShots", remainingBurstShots);
        tag.putLong("NextAllowedShotGameTime", nextAllowedShotGameTime);
        tag.putDouble("BurstSpawnForwardDistance", burstSpawnForwardDistance);
        if (!burstProjectileStack.isEmpty()) {
            tag.put("BurstProjectile", burstProjectileStack.saveOptional(registries));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vsie.enemy_autocannon");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EnemyAutocannonContainerMenu(id, inventory, this, spreadAngle, shotIntervalTicks, burstShots);
    }

    @Override public int getSlots() { return projectileInventory.getSlots(); }
    @Override public @NotNull ItemStack getStackInSlot(int slot) { return projectileInventory.getStackInSlot(slot); }
    @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return projectileInventory.insertItem(slot, stack, simulate); }
    @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return projectileInventory.extractItem(slot, amount, simulate); }
    @Override public int getSlotLimit(int slot) { return projectileInventory.getSlotLimit(slot); }
    @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return projectileInventory.isItemValid(slot, stack); }
    @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { projectileInventory.setStackInSlot(slot, stack); }
}
