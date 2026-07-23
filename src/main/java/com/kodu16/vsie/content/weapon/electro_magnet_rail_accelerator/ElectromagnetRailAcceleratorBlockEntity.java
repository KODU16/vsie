package com.kodu16.vsie.content.weapon.electro_magnet_rail_accelerator;

import com.kodu16.vsie.content.cooldown.FireCooldown;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreBlock;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class ElectromagnetRailAcceleratorBlockEntity extends AbstractWeaponBlockEntity {
    private static final int FIRE_INTERVAL_TICKS = 1;
    private static final int MAX_COOLDOWN_VALUE = 50;
    private static final double IDLE_RECOVERY_PER_TICK = 0.1D;
    private static final double MAX_CORE_DISTANCE = 128.0D;
    private static final double FORCE_PER_MASS = 50.0D;
    private static final String LINKED_CORE_POS_TAG = "LinkedElectroMagnetRailCorePos";
    private BlockPos linkedCorePos = BlockPos.ZERO;
    private long lastAccelerationGameTime = Long.MIN_VALUE;

    public ElectromagnetRailAcceleratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getmaxrange() {
        return 0;
    }

    @Override
    public int getcooldown() {
        return FIRE_INTERVAL_TICKS;
    }

    @Override
    public boolean isEnergyWeapon() {
        // Function: the rail accelerator only applies ship force, so it never consumes item ammo.
        return true;
    }

    @Override
    public Item getAmmoItem() {
        return null;
    }

    @Override
    public FireCooldown getFireCooldown() {
        // Function: the accelerator uses cool2 charge storage so holding fire drains charge and idling recovers it slowly.
        return FireCooldown.cool2(FIRE_INTERVAL_TICKS, MAX_COOLDOWN_VALUE, IDLE_RECOVERY_PER_TICK);
    }

    public BlockPos getLinkedCorePos() {
        return linkedCorePos;
    }

    public void setLinkedCorePos(BlockPos linkedCorePos) {
        this.linkedCorePos = linkedCorePos == null ? BlockPos.ZERO : linkedCorePos.immutable();
        markBindingDirty();
    }

    // Function: expose whether this accelerator should hard-disable force assist for the current firing state.
    public boolean shouldSuppressForceAssist() {
        AccelerationContext context = resolveAccelerationContext();
        return context != null && context.coreControlSeat().hasLinkedBatteryEnergy(context.energyCostFe());
    }

    // Function: expose successful force application across either block-entity tick order with one tick of tolerance.
    public boolean wasAcceleratingRecently() {
        Level level = getLevel();
        return level != null && lastAccelerationGameTime != Long.MIN_VALUE
                && level.getGameTime() - lastAccelerationGameTime <= 1L;
    }

    @Override
    public void fire() {
        AccelerationContext context = resolveAccelerationContext();
        if (context == null || !context.coreControlSeat().consumeLinkedBatteryEnergy(context.energyCostFe())) {
            return;
        }
        Vector3d worldForceDirection = context.worldForceDirection();
        worldForceDirection.normalize().mul(context.massData().getMass() * FORCE_PER_MASS);
        // Function: apply at most one COM impulse per game tick so continuous fire does not stack faster than the server tick rate.
        ServerShipUtils.applyWorldForceAndTorqueAtCenterOfMass(context.serverSubLevel(), worldForceDirection, new Vector3d());
        // Function: paired rail interactions must conserve momentum across the accelerator-core sublevel pair.
        ServerShipUtils.applyWorldForceAndTorqueAtCenterOfMass(context.coreServerSubLevel(), new Vector3d(worldForceDirection).negate(), new Vector3d());
        lastAccelerationGameTime = getLevel().getGameTime();
    }

    private Vector3d resolveAccelerationDirection() {
        AccelerationContext context = resolveAccelerationContext();
        return context == null ? null : new Vector3d(context.worldForceDirection());
    }

    private AccelerationContext resolveAccelerationContext() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return null;
        }

        SubLevel selfSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        if (!(selfSubLevel instanceof ServerSubLevel serverSubLevel)) {
            return null;
        }

        ElectroMagnetRailCoreBlockEntity core = resolveLinkedCore(level);
        if (core == null || !core.hasValidTerminalBinding()) {
            return null;
        }

        SubLevel coreSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, core.getBlockPos());
        if (!(coreSubLevel instanceof ServerSubLevel coreServerSubLevel)) {
            return null;
        }
        if (isSameSubLevel(selfSubLevel, coreSubLevel)) {
            return null;
        }

        Vec3 acceleratorWorldPos = ServerShipUtils.getBlockCenterWorld(selfSubLevel, getBlockPos());
        Vec3 coreWorldPos = ServerShipUtils.getBlockCenterWorld(coreSubLevel, core.getBlockPos());
        if (acceleratorWorldPos == null || coreWorldPos == null) {
            return null;
        }
        if (acceleratorWorldPos.distanceTo(coreWorldPos) > MAX_CORE_DISTANCE) {
            return null;
        }

        MassData massData = serverSubLevel.getMassTracker();
        if (massData == null || massData.isInvalid()) {
            return null;
        }

        ControlSeatBlockEntity coreControlSeat = resolveCoreControlSeat(level, core);
        if (coreControlSeat == null) {
            return null;
        }
        int energyCostFe = getEnergyCostFe(massData);
        if (energyCostFe <= 0) {
            return null;
        }

        Vector3d worldForceDirection = new Vector3d(
                core.getBlockState().getValue(ElectroMagnetRailCoreBlock.FACING).getStepX(),
                core.getBlockState().getValue(ElectroMagnetRailCoreBlock.FACING).getStepY(),
                core.getBlockState().getValue(ElectroMagnetRailCoreBlock.FACING).getStepZ()
        );
        if (coreSubLevel != null) {
            // Function: core facing is local to the core carrier and must be rotated into world space before applying thrust.
            coreSubLevel.logicalPose().transformNormal(worldForceDirection, worldForceDirection);
        }
        if (worldForceDirection.lengthSquared() <= 1.0E-6D) {
            return null;
        }

        return new AccelerationContext(serverSubLevel, coreServerSubLevel, coreControlSeat, massData, worldForceDirection, energyCostFe);
    }

    private ElectroMagnetRailCoreBlockEntity resolveLinkedCore(Level level) {
        if (linkedCorePos == null || linkedCorePos.equals(BlockPos.ZERO)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(linkedCorePos);
        if (!(blockEntity instanceof ElectroMagnetRailCoreBlockEntity core)) {
            return null;
        }
        if (!core.getBlockState().hasProperty(ElectroMagnetRailCoreBlock.FACING)) {
            return null;
        }
        return core;
    }

    private boolean isSameSubLevel(SubLevel selfSubLevel, SubLevel otherSubLevel) {
        if (selfSubLevel == null || otherSubLevel == null) {
            return false;
        }
        return selfSubLevel == otherSubLevel || selfSubLevel.hashCode() == otherSubLevel.hashCode();
    }

    private ControlSeatBlockEntity resolveCoreControlSeat(Level level, ElectroMagnetRailCoreBlockEntity core) {
        BlockPos controlSeatPos = core.getLinkedControlSeatPos();
        if (controlSeatPos == null || controlSeatPos.equals(BlockPos.ZERO)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(controlSeatPos);
        if (!(blockEntity instanceof ControlSeatBlockEntity controlSeat)) {
            return null;
        }
        return controlSeat;
    }

    private int getEnergyCostFe(MassData massData) {
        double energyCost = massData.getMass() / 20.0D;
        if (!Double.isFinite(energyCost) || energyCost <= 0.0D) {
            return 0;
        }
        // Function: FE is integral, so even light accelerator sublevels still pay at least 1 FE per powered tick.
        return energyCost >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, (int) Math.ceil(energyCost));
    }

    private void markBindingDirty() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong(LINKED_CORE_POS_TAG, linkedCorePos.asLong());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(LINKED_CORE_POS_TAG)) {
            linkedCorePos = BlockPos.of(tag.getLong(LINKED_CORE_POS_TAG));
        } else {
            linkedCorePos = BlockPos.ZERO;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("EMRA");
    }

    @Override
    public String getweapontype() {
        return "electro_magnet_rail_accelerator";
    }

    // Function: caches all validated server-side acceleration inputs so fire and assist-lock use the same rules.
    private record AccelerationContext(ServerSubLevel serverSubLevel, ServerSubLevel coreServerSubLevel,
                                       ControlSeatBlockEntity coreControlSeat, MassData massData,
                                       Vector3d worldForceDirection, int energyCostFe) {
    }
}
