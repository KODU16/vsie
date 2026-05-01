package com.kodu16.vsie.content.controlseat.entity;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class ControlSeatMountEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> BOUND_BLOCK_POS = SynchedEntityData.defineId(ControlSeatMountEntity.class, EntityDataSerializers.BLOCK_POS);

    public ControlSeatMountEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BOUND_BLOCK_POS, BlockPos.ZERO);
    }

    public void setBoundBlockPos(BlockPos pos) {
        this.entityData.set(BOUND_BLOCK_POS, pos.immutable());
    }

    public BlockPos getBoundBlockPos() {
        return this.entityData.get(BOUND_BLOCK_POS);
    }

    public static Vec3 getSeatMountPosition(BlockPos pos, BlockState state) {
        Direction facing = getFacing(state);
        return switch (facing) {
            case NORTH -> new Vec3(pos.getX() + 0.5D, pos.getY() + 0.35D, pos.getZ());
            case SOUTH -> new Vec3(pos.getX() + 0.5D, pos.getY() + 0.35D, pos.getZ() + 1.0D);
            case EAST -> new Vec3(pos.getX() + 1.0D, pos.getY() + 0.35D, pos.getZ() + 0.5D);
            case WEST -> new Vec3(pos.getX(), pos.getY() + 0.35D, pos.getZ() + 0.5D);
            default -> Vec3.atCenterOf(pos).add(0.0D, -0.15D, 0.0D);
        };
    }

    public static float getSeatYaw(BlockState state) {
        Direction facing = getFacing(state);
        return switch (facing) {
            case NORTH -> 0.0F;
            case SOUTH -> 180.0F;
            case EAST -> 90.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static Direction getFacing(BlockState state) {
        return state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.NORTH;
    }

    @Override
    public void tick() {
        super.tick();

        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);

        BlockPos boundPos = this.getBoundBlockPos();
        BlockState state = this.level().getBlockState(boundPos);
        SubLevel subLevel = Sable.HELPER.getContaining(this.level(), boundPos);

        if (!this.level().isClientSide) {
            if (state.isAir() || subLevel != null && subLevel.isRemoved()) {
                this.discard();
                return;
            }
        }

        Vec3 seatPos = getSeatMountPosition(boundPos, state);
        float yaw = getSeatYaw(state);
        this.setPos(seatPos);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(0.0F);
        this.xRotO = 0.0F;
        EntitySubLevelUtil.setOldPosNoMovement(this);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (!this.hasPassenger(passenger)) {
            return;
        }

        moveFunction.accept(passenger, this.getX(), this.getY() + 0.25D, this.getZ());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BoundBlockPos")) {
            this.setBoundBlockPos(BlockPos.of(tag.getLong("BoundBlockPos")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("BoundBlockPos", this.getBoundBlockPos().asLong());
    }
}
