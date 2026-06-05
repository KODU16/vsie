package com.kodu16.vsie.content.bullet.penetratingbullet;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class PenetratingBulletEntity extends Projectile {
    private static final EntityDataAccessor<Integer> DATA_K =
            SynchedEntityData.defineId(PenetratingBulletEntity.class, EntityDataSerializers.INT);
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.0F;
    private static final float BLOCK_BREAK_TNT_POWER = 4.0F;

    private int lifeTime = 0;
    private boolean piercingStarted = false;

    public PenetratingBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setK(1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_K, 1);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 start = this.position();
        Vec3 movement = this.getDeltaMovement();
        Vec3 end = start.add(movement);

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        if (hitResult.getType() == HitResult.Type.MISS) {
            List<Entity> entities = this.level().getEntities(
                    this,
                    this.getBoundingBox().expandTowards(movement).inflate(1.8D, 1.8D, 1.8D)
            );

            Entity closest = null;
            double closestDistSq = Double.MAX_VALUE;

            for (Entity entity : entities) {
                if (!this.canHitEntity(entity)) {
                    continue;
                }

                Optional<Vec3> intercept = entity.getBoundingBox().clip(start, end);
                if (intercept.isPresent()) {
                    double distSq = intercept.get().distanceToSqr(start);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closest = entity;
                    }
                }
            }

            if (closest != null) {
                hitResult = new EntityHitResult(closest, closest.getBoundingBox().clip(start, end).orElse(end));
            }
        }

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) hitResult);
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) hitResult);
        }

        this.setPos(end);

        if (piercingStarted && this.level() instanceof ServerLevel serverLevel) {
            BlockState state = serverLevel.getBlockState(this.blockPosition());
            if (!state.isAir() && state.isCollisionShapeFullBlock(serverLevel, this.blockPosition())) {
                destroyBlocksInSphere(serverLevel, this.position(), getK());
            }
        }

        lifeTime++;
        if (lifeTime > 10 * 20) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        BlockPos hitPos = result.getBlockPos();
        BlockState state = level().getBlockState(hitPos);
        if (state.isCollisionShapeFullBlock(level(), hitPos) && this.level() instanceof ServerLevel serverLevel) {
            piercingStarted = true;
            destroyBlocksInSphere(serverLevel, result.getLocation(), getK());
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        piercingStarted = true;
    }

    public int getK() {
        return this.entityData.get(DATA_K);
    }

    public void setK(int k) {
        this.entityData.set(DATA_K, Math.max(0, k));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("K", this.getK());
        tag.putBoolean("PiercingStarted", this.piercingStarted);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("K")) {
            this.setK(tag.getInt("K"));
        }
        if (tag.contains("PiercingStarted")) {
            this.piercingStarted = tag.getBoolean("PiercingStarted");
        }
    }

    private void destroyBlocksInSphere(ServerLevel level, Vec3 impactPoint, double radius) {
        double safeRadius = Math.max(0.0D, radius);
        int blockRadius = (int) Math.ceil(safeRadius);
        BlockPos center = BlockPos.containing(impactPoint);
        double radiusSqr = safeRadius * safeRadius;

        for (int x = -blockRadius; x <= blockRadius; x++) {
            for (int y = -blockRadius; y <= blockRadius; y++) {
                for (int z = -blockRadius; z <= blockRadius; z++) {
                    if (safeRadius > 0.0D && x * x + y * y + z * z > radiusSqr) {
                        continue;
                    }
                    BlockPos targetPos = center.offset(x, y, z);
                    tryBreakBlock(level, targetPos, impactPoint, safeRadius);
                }
            }
        }
    }

    private void tryBreakBlock(ServerLevel level, BlockPos pos, Vec3 impactPoint, double maxDistance) {
        BlockState state = level.getBlockState(pos);
        if (!canBreakBlock(level, pos, state)) {
            return;
        }
        if (level.random.nextDouble() > computeBlockBreakProbability(level, pos, impactPoint, maxDistance)) {
            return;
        }
        // Function: penetrating bullets use the same distance/hardness break chance as the main bullet hierarchy.
        level.levelEvent(2001, pos, Block.getId(state));
        boolean destroyed = level.destroyBlock(pos, false, this);
        if (destroyed) {
            maybeTriggerBlockBreakTnt(level, pos);
        }
    }

    private boolean canBreakBlock(ServerLevel level, BlockPos pos, BlockState state) {
        return level.isLoaded(pos) && !state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private double computeBlockBreakProbability(ServerLevel level, BlockPos pos, Vec3 impactPoint, double maxDistance) {
        BlockState state = level.getBlockState(pos);
        double hardness = Math.max(0.0D, state.getDestroySpeed(level, pos));
        double distanceRatio = maxDistance <= 1.0E-6D ? 0.0D : Vec3.atCenterOf(pos).distanceTo(impactPoint) / maxDistance;
        return Math.max(0.0D, Math.min(1.0D, (1.2D - distanceRatio) * (1.2D - hardness / 100.0D)));
    }

    private void maybeTriggerBlockBreakTnt(ServerLevel level, BlockPos pos) {
        if (BLOCK_BREAK_TNT_CHANCE <= 0.0F || level.random.nextFloat() >= BLOCK_BREAK_TNT_CHANCE) {
            return;
        }
        // Function: penetrating bullets keep the same optional TNT follow-up hook as the main bullet hierarchy.
        level.explode(
                this,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                BLOCK_BREAK_TNT_POWER,
                false,
                Level.ExplosionInteraction.TNT
        );
    }
}
