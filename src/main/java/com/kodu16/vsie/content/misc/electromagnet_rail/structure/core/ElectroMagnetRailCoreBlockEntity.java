package com.kodu16.vsie.content.misc.electromagnet_rail.structure.core;

import com.kodu16.vsie.content.misc.electromagnet_rail.structure.top.ElectroMagnetRailTopBlock;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.top.ElectroMagnetRailTopBlockEntity;
import com.kodu16.vsie.registries.vsieBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;

import javax.annotation.Nullable;
import java.util.List;

public class ElectroMagnetRailCoreBlockEntity extends SmartBlockEntity implements MenuProvider, IItemHandlerModifiable, GeoBlockEntity {
    private static final String LINKED_CONTROL_SEAT_POS_TAG = "LinkedControlSeatPos";
    private HolderLookup.Provider nbtRegistries;

    public static final int TERMINAL_STATUS_IDLE = 0;
    public static final int TERMINAL_STATUS_FOUND = 1;
    public static final int TERMINAL_STATUS_FACING_ERROR = 2;
    public static final int TERMINAL_STATUS_NOT_FOUND = 3;
    public static final int TERMINAL_STATUS_BLOCKED = 4;
    public static SerializableDataTicket<Boolean> IS_WORKING;
    public float prevRailOffsetX = 0.0f;
    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(vsieBlocks.ELECTRO_MAGNET_RAIL_BLOCK.asItem());
        }
    };

    private int terminalStatus = TERMINAL_STATUS_IDLE;
    private BlockPos terminalPos = BlockPos.ZERO;
    private float beamRenderDistance = 0.0f;
    // Function: synced model state for moving the core side rails after a valid terminal is found.
    private boolean workingTerminal = false;
    private BlockPos linkedControlSeatPos = BlockPos.ZERO;

    public int getTerminalStatus() {
        return terminalStatus;
    }

    public BlockPos getTerminalPos() {
        return terminalPos;
    }

    public float getBeamRenderDistance() {
        return beamRenderDistance;
    }

    public boolean isWorkingTerminal() {
        return workingTerminal;
    }

    public BlockPos getLinkedControlSeatPos() {
        return linkedControlSeatPos;
    }

    public void setLinkedControlSeatPos(BlockPos linkedControlSeatPos) {
        // Function: the rail core owns the power-source seat link used by cross-sublevel accelerators.
        this.linkedControlSeatPos = linkedControlSeatPos == null ? BlockPos.ZERO : linkedControlSeatPos.immutable();
        setChanged();
    }

    public ElectroMagnetRailCoreBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public void tick(){
        if (this.level == null || this.terminalStatus != TERMINAL_STATUS_FOUND || this.terminalPos.equals(BlockPos.ZERO)) {
            return;
        }

        if (!isTerminalBindingStillValid()) {
            setWorkingTerminal(false);
            updateTopBindingState(this.terminalPos, false);
            clearTerminalBinding();
            this.setChanged();
            this.sendData();
            return;
        }
        setWorkingTerminal(true);

        float maxDistance = (float) Math.sqrt(this.worldPosition.distSqr(this.terminalPos));
        this.beamRenderDistance = Math.min(maxDistance, this.beamRenderDistance + 2.0f);
    }

    public int getStoredRailCount() {
        int total = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            total += inventory.getStackInSlot(i).getCount();
        }
        return total;
    }

    public void detectTerminal() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        Direction facing = this.getBlockState().getValue(ElectroMagnetRailCoreBlock.FACING);
        int maxDistance = this.getStoredRailCount();

        BlockPos previousTerminalPos = this.terminalPos;
        this.terminalStatus = TERMINAL_STATUS_NOT_FOUND;
        this.terminalPos = BlockPos.ZERO;
        this.beamRenderDistance = 0.0f;

        for (int step = 1; step <= maxDistance; step++) {
            BlockPos checkPos = this.worldPosition.relative(facing, step);
            BlockState checkState = this.level.getBlockState(checkPos);

            if (checkState.is(vsieBlocks.ELECTRO_MAGNET_RAIL_TOP_BLOCK.get())) {
                Direction topFacing = checkState.getValue(ElectroMagnetRailTopBlock.FACING);
                if (topFacing == facing) {
                    this.terminalStatus = TERMINAL_STATUS_FOUND;
                    this.terminalPos = checkPos;
                    setWorkingTerminal(true);
                    if (!previousTerminalPos.equals(checkPos)) {
                        updateTopBindingState(previousTerminalPos, false);
                    }
                    updateTopBindingState(checkPos, true);
                    this.beamRenderDistance = 0.0f;
                } else {
                    updateTopBindingState(previousTerminalPos, false);
                    this.terminalStatus = TERMINAL_STATUS_FACING_ERROR;
                    this.terminalPos = checkPos;
                    setWorkingTerminal(false);
                    this.beamRenderDistance = 0.0f;
                }
                this.setChanged();
                this.sendData();
                return;
            }

            if (!isAllowedRailPathBlock(checkState)) {
                updateTopBindingState(previousTerminalPos, false);
                this.terminalStatus = TERMINAL_STATUS_BLOCKED;
                this.terminalPos = checkPos;
                setWorkingTerminal(false);
                this.beamRenderDistance = 0.0f;
                this.setChanged();
                this.sendData();
                return;
            }
        }

        updateTopBindingState(previousTerminalPos, false);
        setWorkingTerminal(false);
        this.setChanged();
        this.sendData();
    }


    public void releaseBoundTop() {
        updateTopBindingState(this.terminalPos, false);
        clearTerminalBinding();
    }

    private void updateTopBindingState(BlockPos topPos, boolean bound) {
        if (this.level == null || topPos == null || topPos.equals(BlockPos.ZERO)) {
            return;
        }
        if (this.level.getBlockEntity(topPos) instanceof ElectroMagnetRailTopBlockEntity topBlockEntity) {
            topBlockEntity.setBoundToCore(bound);
        }
    }

    private void setWorkingTerminal(boolean workingTerminal) {
        this.workingTerminal = workingTerminal;
        setAnimData(IS_WORKING, workingTerminal);
    }

    public boolean hasValidTerminalBinding() {
        return this.terminalStatus == TERMINAL_STATUS_FOUND && !this.terminalPos.equals(BlockPos.ZERO) && isTerminalBindingStillValid();
    }

    public int getEffectiveRailLength() {
        if (!hasValidTerminalBinding()) {
            return -1;
        }

        Direction facing = this.getBlockState().getValue(ElectroMagnetRailCoreBlock.FACING);
        // Function: GUI and rail cannon FX use the active core-to-top distance, not the stored rail item count.
        return getTerminalDistanceAlongFacing(facing, this.terminalPos);
    }

    private boolean isTerminalBindingStillValid() {
        if (this.level == null || this.terminalPos.equals(BlockPos.ZERO)) {
            return false;
        }

        Direction facing = this.getBlockState().getValue(ElectroMagnetRailCoreBlock.FACING);
        BlockState topState = this.level.getBlockState(this.terminalPos);
        if (!topState.is(vsieBlocks.ELECTRO_MAGNET_RAIL_TOP_BLOCK.get())) {
            return false;
        }
        if (!(this.level.getBlockEntity(this.terminalPos) instanceof ElectroMagnetRailTopBlockEntity)) {
            return false;
        }
        if (topState.getValue(ElectroMagnetRailTopBlock.FACING) != facing) {
            return false;
        }

        int distance = getTerminalDistanceAlongFacing(facing, this.terminalPos);
        if (distance <= 0) {
            return false;
        }

        for (int step = 1; step < distance; step++) {
            BlockPos checkPos = this.worldPosition.relative(facing, step);
            BlockState checkState = this.level.getBlockState(checkPos);
            if (!isAllowedRailPathBlock(checkState)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllowedRailPathBlock(BlockState state) {
        // Function: rail scans treat an in-line rail cannon as part of the assembly, not as terminal obstruction.
        return state.isAir()
                || state.is(vsieBlocks.ELECTRO_MAGNET_RAIL_BLOCK.get())
                || state.is(vsieBlocks.ELECTRO_MAGNET_RAIL_CANNON_BLOCK.get());
    }

    private int getTerminalDistanceAlongFacing(Direction facing, BlockPos targetPos) {
        int dx = targetPos.getX() - this.worldPosition.getX();
        int dy = targetPos.getY() - this.worldPosition.getY();
        int dz = targetPos.getZ() - this.worldPosition.getZ();

        return switch (facing.getAxis()) {
            case X -> (dy == 0 && dz == 0 && Integer.signum(dx) == facing.getStepX()) ? Math.abs(dx) : -1;
            case Y -> (dx == 0 && dz == 0 && Integer.signum(dy) == facing.getStepY()) ? Math.abs(dy) : -1;
            case Z -> (dx == 0 && dy == 0 && Integer.signum(dz) == facing.getStepZ()) ? Math.abs(dz) : -1;
        };
    }

    private void clearTerminalBinding() {
        this.terminalStatus = TERMINAL_STATUS_IDLE;
        this.terminalPos = BlockPos.ZERO;
        this.beamRenderDistance = 0.0f;
        setWorkingTerminal(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vsie.electro_magnet_rail_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectroMagnetRailCoreContainerMenu(containerId, playerInventory, this);
    }

    private HolderLookup.Provider currentNbtRegistries() {
        return nbtRegistries != null ? nbtRegistries : this.level.registryAccess();
    }

    private void withNbtRegistries(HolderLookup.Provider registries, Runnable action) {
        HolderLookup.Provider previous = this.nbtRegistries;
        this.nbtRegistries = registries;
        try {
            action.run();
        } finally {
            this.nbtRegistries = previous;
        }
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.read(tag, registries, clientpacket);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("TerminalStatus")) {
            this.terminalStatus = tag.getInt("TerminalStatus");
        }
        if (tag.contains("TerminalPos")) {
            this.terminalPos = BlockPos.of(tag.getLong("TerminalPos"));
        }
        if (tag.contains("BeamRenderDistance")) {
            this.beamRenderDistance = tag.getFloat("BeamRenderDistance");
        }
        if (tag.contains("WorkingTerminal")) {
            this.workingTerminal = tag.getBoolean("WorkingTerminal");
            setAnimData(IS_WORKING, this.workingTerminal);
        }
        if (tag.contains(LINKED_CONTROL_SEAT_POS_TAG)) {
            this.linkedControlSeatPos = BlockPos.of(tag.getLong(LINKED_CONTROL_SEAT_POS_TAG));
        } else {
            this.linkedControlSeatPos = BlockPos.ZERO;
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.write(tag, registries, clientpacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("TerminalStatus", this.terminalStatus);
        tag.putLong("TerminalPos", this.terminalPos.asLong());
        tag.putFloat("BeamRenderDistance", this.beamRenderDistance);
        tag.putBoolean("WorkingTerminal", this.workingTerminal);
        tag.putLong(LINKED_CONTROL_SEAT_POS_TAG, this.linkedControlSeatPos.asLong());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        withNbtRegistries(registries, () -> read(tag, registries, true));
    }


    public IItemHandlerModifiable getItemHandler() {
        return this;
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
