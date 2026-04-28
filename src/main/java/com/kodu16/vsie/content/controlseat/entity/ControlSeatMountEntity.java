package com.kodu16.vsie.content.controlseat.entity;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ControlSeatMountEntity extends Entity {

    private BlockPos boundBlockPos = BlockPos.ZERO;

    public ControlSeatMountEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setBoundBlockPos(BlockPos pos) {
        this.boundBlockPos = pos.immutable();
    }

    public BlockPos getBoundBlockPos() {
        return this.boundBlockPos;
    }

    @Override
    public void tick() {
        super.tick();

        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);

        if (!this.level().isClientSide) {
            SubLevel subLevel = Sable.HELPER.getContaining(this.level(), this.boundBlockPos);

            if (subLevel == null || subLevel.isRemoved()) {
                this.discard();
                return;
            }

            if (this.level().getBlockState(this.boundBlockPos).isAir()) {
                this.discard();
                return;
            }

            // 关键：这是 sublevel local / plot 坐标，不是 world 坐标。
            Vec3 localSeatPos = Vec3.atLowerCornerOf(this.boundBlockPos)
                    .add(0.5, 0.65, 0.5);

            this.setPos(localSeatPos);
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (!this.hasPassenger(passenger)) {
            return;
        }

        // 仍然使用 local / plot 坐标。
        moveFunction.accept(
                passenger,
                this.getX(),
                this.getY() + 0.25,
                this.getZ()
        );
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
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BoundBlockPos")) {
            this.boundBlockPos = BlockPos.of(tag.getLong("BoundBlockPos"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("BoundBlockPos", this.boundBlockPos.asLong());
    }
}