package com.kodu16.vsie.content.weapon.missile_launcher.block;

import com.kodu16.vsie.content.missile.entity.BasicMissileEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieItems;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VerticleLaunchingSlotCoreBlockEntity extends AbstractWeaponBlockEntity implements IItemHandlerModifiable {
    private static final String LINKED_SLOTS_TAG = "LinkedSlots";
    private static final String SLOT_POS_TAG = "Pos";
    private static final String MISSILE_INVENTORY_TAG = "VerticalLaunchMissileInventory";
    private static final String LAUNCH_INTERVAL_TAG = "LaunchIntervalTicks";
    private static final int COOLDOWN_TICKS = 100;
    private static final int DEFAULT_LAUNCH_INTERVAL_TICKS = 20;

    private final List<BlockPos> linkedSlots = new ArrayList<>();
    private int launchIntervalTicks = DEFAULT_LAUNCH_INTERVAL_TICKS;
    private int burstSlotCursor = 0;
    private int burstIntervalTimer = 0;
    private boolean burstInProgress = false;
    private int armedChannelEncode = 0;
    private enum LaunchAttemptResult {
        LAUNCHED,
        NO_READY_SLOT,
        NO_AMMO,
        INVALID_SLOT
    }

    // Function: the launch core stores basic missiles that are consumed by linked vertical launch slots.
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

    public VerticleLaunchingSlotCoreBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.currentTick = COOLDOWN_TICKS;
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return COOLDOWN_TICKS;
    }

    @Override
    public String getweapontype() {
        return "verticle_launching_slot_core";
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("VLS Core");
    }

    @Override
    public void tick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        boolean capActive = isActiveForChannels(armedChannelEncode);
        boolean fireActive = needtofire();
        tickFireCooldown(fireActive);
        syncLinkedSlotActivation(level, capActive);

        if (!isFireCooldownReady()) {
            getData().isfiring = false;
            return;
        }

        if (!fireActive || !hasValidTarget()) {
            cancelBurst();
            getData().isfiring = false;
            return;
        }

        if (!burstInProgress) {
            if (linkedSlots.isEmpty() || !hasMissileAmmo()) {
                getData().isfiring = false;
                return;
            }
            // Function: each active firing cycle walks the linked slots from low number to high number.
            burstInProgress = true;
            burstSlotCursor = 0;
            burstIntervalTimer = 0;
        }

        getData().isfiring = true;
        if (burstIntervalTimer > 0) {
            burstIntervalTimer--;
            return;
        }

        LaunchAttemptResult launchResult = launchNextSlot(level);
        if (launchResult == LaunchAttemptResult.INVALID_SLOT) {
            return;
        }
        if (launchResult == LaunchAttemptResult.NO_AMMO) {
            finishBurst();
            return;
        }

        if (burstSlotCursor >= linkedSlots.size()) {
            finishBurst();
        } else {
            burstIntervalTimer = Math.max(0, launchIntervalTicks - 1);
        }
    }

    @Override
    public void fire() {
        // Function: vertical launch core uses its own burst scheduler instead of AbstractWeaponBlockEntity#tick.
    }

    public void receiveArmedChannels(int encode) {
        // Function: control seat armed channels are separate from the fire input channel used by needtofire().
        this.armedChannelEncode = encode;
    }

    public int addLinkedSlot(BlockPos slotPos) {
        // Function: the stored order is the launcher slot number shown by the linker overlay.
        int existingIndex = linkedSlots.indexOf(slotPos);
        if (existingIndex >= 0) {
            return existingIndex + 1;
        }
        linkedSlots.add(slotPos.immutable());
        setChanged();
        sendData();
        return linkedSlots.size();
    }

    public List<BlockPos> getLinkedSlots() {
        return List.copyOf(linkedSlots);
    }

    public int getLaunchIntervalTicks() {
        return launchIntervalTicks;
    }

    public void setLaunchIntervalTicks(int launchIntervalTicks) {
        // Function: clamp GUI input so zero or negative k cannot lock the burst scheduler.
        this.launchIntervalTicks = Math.max(1, Math.min(launchIntervalTicks, 1200));
        setChanged();
        sendData();
    }

    private LaunchAttemptResult launchNextSlot(Level level) {
        while (burstSlotCursor < linkedSlots.size()) {
            BlockPos slotPos = linkedSlots.get(burstSlotCursor++);
            if (!(level.getBlockEntity(slotPos) instanceof VerticleLaunchingSlotBlockEntity slot)) {
                handleInvalidLinkedSlot(level);
                return LaunchAttemptResult.INVALID_SLOT;
            }
            if (!slot.canLaunch()) {
                continue;
            }
            if (!hasMissileAmmo()) {
                return LaunchAttemptResult.NO_AMMO;
            }
            BasicMissileEntity missile = new BasicMissileEntity(vsieEntities.BASIC_MISSILE.get(), level);
            Vec3 launchDirection = getLaunchWorldDirection(level, slotPos);
            missile.setTarget(getData().targetship);
            missile.setPos(getLaunchWorldPosition(level, slotPos).add(launchDirection.scale(4.0D)));
            // Function: spawn missiles clear of the slot collider so straight-launch movement does not instantly impact itself.
            missile.setInitialDirection(launchDirection);
            if (level.addFreshEntity(missile) && consumeMissileAmmo()) {
                slot.markLaunched();
                return LaunchAttemptResult.LAUNCHED;
            }
            missile.discard();
        }
        return LaunchAttemptResult.NO_READY_SLOT;
    }

    private void handleInvalidLinkedSlot(Level level) {
        // Function: invalid linked slots force a cooldown and compact numbering while preserving remaining slot order.
        rebuildLinkedSlots(level);
        cancelBurst();
        currentTick = 0;
        getData().isfiring = false;
        syncLinkedSlotActivation(level, false);
        setChanged();
        sendData();
    }

    private void rebuildLinkedSlots(Level level) {
        List<BlockPos> validSlots = new ArrayList<>();
        for (BlockPos slotPos : linkedSlots) {
            if (level.getBlockEntity(slotPos) instanceof VerticleLaunchingSlotBlockEntity) {
                validSlots.add(slotPos.immutable());
            }
        }
        linkedSlots.clear();
        linkedSlots.addAll(validSlots);
    }

    private Vec3 getLaunchWorldPosition(Level level, BlockPos slotPos) {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, slotPos);
        return ServerShipUtils.getBlockCenterWorld(subLevel, slotPos);
    }

    private Vec3 getLaunchWorldDirection(Level level, BlockPos slotPos) {
        Vec3 localUp = new Vec3(0, 1, 0);
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, slotPos);
        Vec3 worldUp = subLevel == null ? localUp : subLevel.logicalPose().transformNormal(localUp);
        return worldUp.lengthSqr() > 1.0E-6D ? worldUp.normalize() : localUp;
    }

    private void syncLinkedSlotActivation(Level level, boolean coreActive) {
        // Function: every linked slot gets the live channel-active state and decides its cap animation from local cooldown.
        for (BlockPos slotPos : linkedSlots) {
            if (level.getBlockEntity(slotPos) instanceof VerticleLaunchingSlotBlockEntity slot) {
                slot.setLinkedCoreActive(coreActive);
            }
        }
    }

    public void deactivateLinkedSlots(Level level) {
        syncLinkedSlotActivation(level, false);
    }

    private boolean hasValidTarget() {
        SubLevel target = getData().targetship;
        return target != null && ServerShipUtils.getStructureCenterWorld(target) != null;
    }

    private boolean isActiveForChannels(int encode) {
        // Function: cap opening depends on the intersection of control-seat armed channels and this core's enabled channels.
        return ((encode & 1) != 0 && getData().channel1)
                || ((encode & 2) != 0 && getData().channel2)
                || ((encode & 4) != 0 && getData().channel3)
                || ((encode & 8) != 0 && getData().channel4);
    }

    private void cancelBurst() {
        burstInProgress = false;
        burstSlotCursor = 0;
        burstIntervalTimer = 0;
    }

    private void finishBurst() {
        cancelBurst();
        consumeFireCooldown();
        getData().isfiring = false;
        setChanged();
    }

    private boolean hasMissileAmmo() {
        for (int slot = 0; slot < missileInventory.getSlots(); slot++) {
            if (!missileInventory.extractItem(slot, 1, true).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean consumeMissileAmmo() {
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
        for (int slot = 0; slot < missileInventory.getSlots(); slot++) {
            ItemStack stack = missileInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.copy());
                missileInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ListTag list = new ListTag();
        for (BlockPos slotPos : linkedSlots) {
            CompoundTag posTag = new CompoundTag();
            posTag.putLong(SLOT_POS_TAG, slotPos.asLong());
            list.add(posTag);
        }
        tag.put(LINKED_SLOTS_TAG, list);
        tag.putInt(LAUNCH_INTERVAL_TAG, launchIntervalTicks);
        tag.put(MISSILE_INVENTORY_TAG, missileInventory.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        linkedSlots.clear();
        ListTag list = tag.getList(LINKED_SLOTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag posTag = list.getCompound(i);
            linkedSlots.add(BlockPos.of(posTag.getLong(SLOT_POS_TAG)));
        }
        if (tag.contains(LAUNCH_INTERVAL_TAG, Tag.TAG_INT)) {
            launchIntervalTicks = Math.max(1, tag.getInt(LAUNCH_INTERVAL_TAG));
        }
        if (tag.contains(MISSILE_INVENTORY_TAG)) {
            missileInventory.deserializeNBT(registries, tag.getCompound(MISSILE_INVENTORY_TAG));
        }
    }

    public IItemHandlerModifiable getItemHandler() {
        return this;
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
