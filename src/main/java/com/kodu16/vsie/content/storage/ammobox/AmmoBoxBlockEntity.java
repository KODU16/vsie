package com.kodu16.vsie.content.storage.ammobox;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.network.storage.AmmoBoxRefillMarkerS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
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

import java.util.ArrayList;
import java.util.List;

public class AmmoBoxBlockEntity extends BlockEntity implements MenuProvider, IItemHandlerModifiable {
    private static final String LINKED_CONTROL_SEAT_POS_TAG = "LinkedControlSeatPos";
    private static final int REFILL_RETRY_INTERVAL_TICKS = 5;
    private static final int REFILL_SUCCESS_INTERVAL_TICKS = 20;
    private static final int REFILL_MARKER_DURATION_TICKS = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private @Nullable BlockPos linkedControlSeatPos;
    private int refillCooldownTicks = 0;
    private int refillIntervalTicks = REFILL_RETRY_INTERVAL_TICKS;
    private int refillCursor = 0;

    public AmmoBoxBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public IItemHandlerModifiable getItemHandler() {
        return this;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++refillCooldownTicks < refillIntervalTicks) {
            return;
        }
        refillCooldownTicks = 0;
        refillIntervalTicks = refillNextLinkedWeaponOrTurret() ? REFILL_SUCCESS_INTERVAL_TICKS : REFILL_RETRY_INTERVAL_TICKS;
    }

    public void setLinkedControlSeatPos(@Nullable BlockPos linkedControlSeatPos) {
        this.linkedControlSeatPos = linkedControlSeatPos;
        this.refillCursor = 0;
        setChanged();
    }

    public @Nullable BlockPos getLinkedControlSeatPos() {
        return linkedControlSeatPos;
    }

    private boolean refillNextLinkedWeaponOrTurret() {
        AbstractControlSeatBlockEntity controlSeat = getLinkedControlSeat();
        if (controlSeat == null) {
            return false;
        }

        List<BlockPos> targets = new ArrayList<>();
        targets.addAll(controlSeat.getLinkedWeaponPositionsInOrder());
        targets.addAll(controlSeat.getLinkedTurretPositionsInOrder());
        if (targets.isEmpty()) {
            refillCursor = 0;
            return false;
        }

        refillCursor = Math.floorMod(refillCursor, targets.size());
        int targetIndex = refillCursor + 1;
        BlockPos targetPos = targets.get(refillCursor);
        refillCursor = (refillCursor + 1) % targets.size();
        return tryRefillTargetAt(targetPos, targetIndex);
    }

    private @Nullable AbstractControlSeatBlockEntity getLinkedControlSeat() {
        if (linkedControlSeatPos == null || level == null || !level.isLoaded(linkedControlSeatPos)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(linkedControlSeatPos);
        return blockEntity instanceof AbstractControlSeatBlockEntity controlSeat ? controlSeat : null;
    }

    private boolean tryRefillTargetAt(BlockPos targetPos, int targetIndex) {
        if (level == null || !level.isLoaded(targetPos)) {
            return false;
        }

        BlockEntity target = level.getBlockEntity(targetPos);
        if (target instanceof AbstractWeaponBlockEntity weapon && weapon.hasAmmoInventorySlots()) {
            return transferOneAmmoTo(weapon, targetIndex, weapon.getDisplayName().getString());
        }
        if (target instanceof AbstractTurretBlockEntity turret && turret.hasAmmoInventorySlots()) {
            return transferOneAmmoTo(turret, targetIndex, turret.getDisplayName().getString());
        }
        return false;
    }

    private boolean transferOneAmmoTo(IItemHandlerModifiable targetInventory, int targetIndex, String targetDisplayName) {
        for (int sourceSlot = 0; sourceSlot < inventory.getSlots(); sourceSlot++) {
            ItemStack sourceStack = inventory.getStackInSlot(sourceSlot);
            if (sourceStack.isEmpty()) {
                continue;
            }

            ItemStack oneAmmo = sourceStack.copyWithCount(1);
            int targetSlot = findEmptyTargetSlotFor(targetInventory, oneAmmo);
            if (targetSlot < 0) {
                continue;
            }

            int transferCount = getTransferCount(targetInventory, targetSlot, sourceStack);
            if (transferCount <= 0) {
                continue;
            }

            ItemStack extracted = inventory.extractItem(sourceSlot, transferCount, false);
            if (extracted.isEmpty()) {
                return false;
            }
            ItemStack markerStack = extracted.copy();
            ItemStack remainder = targetInventory.insertItem(targetSlot, extracted, false);
            if (!remainder.isEmpty()) {
                inventory.insertItem(sourceSlot, remainder, false);
                return false;
            }
            setChanged();
            if (targetInventory instanceof BlockEntity targetBlockEntity) {
                targetBlockEntity.setChanged();
            }
            sendRefillMarker(markerStack, targetIndex, targetDisplayName, markerStack.getCount());
            return true;
        }
        return false;
    }

    private void sendRefillMarker(ItemStack ammoStack, int targetIndex, String targetDisplayName, int amount) {
        if (ammoStack.isEmpty()) {
            return;
        }
        // Function: clients render the last successful refill above this ammo box for one refill interval.
        ModNetworking.sendToAll(new AmmoBoxRefillMarkerS2CPacket(
                worldPosition,
                ammoStack.copyWithCount(1),
                amount,
                targetIndex,
                targetDisplayName,
                REFILL_MARKER_DURATION_TICKS
        ));
    }

    private int findEmptyTargetSlotFor(IItemHandlerModifiable targetInventory, ItemStack oneAmmo) {
        for (int targetSlot = 0; targetSlot < targetInventory.getSlots(); targetSlot++) {
            if (!targetInventory.getStackInSlot(targetSlot).isEmpty()) {
                continue;
            }
            ItemStack remainder = targetInventory.insertItem(targetSlot, oneAmmo, true);
            if (remainder.isEmpty()) {
                return targetSlot;
            }
        }
        return -1;
    }

    private int getTransferCount(IItemHandlerModifiable targetInventory, int targetSlot, ItemStack sourceStack) {
        int slotLimit = targetInventory.getSlotLimit(targetSlot);
        int itemLimit = sourceStack.getMaxStackSize();
        return Math.min(sourceStack.getCount(), Math.min(slotLimit, itemLimit));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        linkedControlSeatPos = readBlockPos(tag, LINKED_CONTROL_SEAT_POS_TAG);
        refillCursor = tag.getInt("RefillCursor");
        refillIntervalTicks = Math.max(REFILL_RETRY_INTERVAL_TICKS, tag.getInt("RefillIntervalTicks"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        if (linkedControlSeatPos != null) {
            tag.putIntArray(LINKED_CONTROL_SEAT_POS_TAG, new int[]{
                    linkedControlSeatPos.getX(),
                    linkedControlSeatPos.getY(),
                    linkedControlSeatPos.getZ()
            });
        }
        tag.putInt("RefillCursor", refillCursor);
        tag.putInt("RefillIntervalTicks", refillIntervalTicks);
    }

    private @Nullable BlockPos readBlockPos(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return null;
        }
        int[] coords = tag.getIntArray(key);
        return coords.length >= 3 ? new BlockPos(coords[0], coords[1], coords[2]) : null;
    }

    @Override
    public Component getDisplayName() {
        // Function: reuse the mod namespace block translation so the ammo box title stays aligned with the item name.
        return Component.translatable("block.vsie.ammo_box");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AmmoBoxContainerMenu(containerId, playerInventory, this);
    }

    @Override
    public int getSlots() {
        return inventory.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return inventory.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
        setChanged();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }
}
