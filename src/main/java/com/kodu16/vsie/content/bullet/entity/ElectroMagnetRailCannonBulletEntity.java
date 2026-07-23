package com.kodu16.vsie.content.bullet.entity;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ElectroMagnetRailCannonBulletEntity extends HeavyElectroMagnetBulletEntity {
    private static final EntityDataAccessor<Integer> RAIL_COUNT =
            SynchedEntityData.defineId(ElectroMagnetRailCannonBulletEntity.class, EntityDataSerializers.INT);
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.0F;

    public ElectroMagnetRailCannonBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
    }

    public void configureRailCount(int railCount) {
        this.entityData.set(RAIL_COUNT, Math.max(0, railCount));
    }

    @Override
    protected float getBlockBreakTntChance() {
        return BLOCK_BREAK_TNT_CHANCE;
    }

    @Override
    protected double getBlockBreakRadius() {
        return Math.max(0.0D, getRailCount() / 15.0D);
    }

    @Override
    protected int getPiercingDurationTicks() {
        return Math.max(1, getRailCount() / 10);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.level() instanceof ServerLevel serverLevel) {
            // Function: rail cannon bullets break a sphere centered on the exact contact point before continuing through blocks.
            destroyBlocksInSphere(serverLevel, result.getLocation(), getBlockBreakRadius());
        }
        super.onHitBlock(result);
    }

    @Override
    protected void afterServerBulletMove(HitResult hitResult) {
        if (hitResult.getType() != HitResult.Type.BLOCK && this.level() instanceof ServerLevel serverLevel && isPiercingStarted()) {
            destroyBlocksInSphere(serverLevel, this.position(), getBlockBreakRadius());
        }
        super.afterServerBulletMove(hitResult);
    }

    private int getRailCount() {
        return this.entityData.get(RAIL_COUNT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAIL_COUNT, 0);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RailCount", getRailCount());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RailCount")) {
            configureRailCount(tag.getInt("RailCount"));
        }
    }
}
