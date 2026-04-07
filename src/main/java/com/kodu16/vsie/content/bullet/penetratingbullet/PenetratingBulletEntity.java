package com.kodu16.vsie.content.bullet.penetratingbullet;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;

public class PenetratingBulletEntity extends Projectile {
    private int lifeTime = 0;

    // 构造函数：接收一个k参数，代表力量系数
    public PenetratingBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setK(1);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // 可选：客户端插值表现可以保;
        }

        Vec3 start = this.position();           // 本tick开始位置（上一tick结束位置）
        Vec3 movement = this.getDeltaMovement();
        Vec3 end = start.add(movement);         // 本tick理论结束位置

        // 1. 先做标准的射线检测（方块 + 实体）
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(
                this,
                this::canHitEntity
        );

        // 2. 如果没有命中，再做一个保守的AABB扫描（防漏）
        if (hitResult.getType() == HitResult.Type.MISS) {
            List<Entity> entities = this.level().getEntities(
                    this,
                    this.getBoundingBox().expandTowards(movement).inflate(1.8, 1.8, 1.8)
            );

            Entity closest = null;
            double closestDistSq = Double.MAX_VALUE;

            for (Entity entity : entities) {
                if (!this.canHitEntity(entity)) continue;

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

        // 处理命中
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) hitResult);
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) hitResult);
        }

        this.setPos(end);

        lifeTime++;
        if (lifeTime > 10 * 20) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        BlockState state = (level().getBlockState(pResult.getBlockPos()));
        if (state.isCollisionShapeFullBlock(level(), pResult.getBlockPos())) {
            destroyBlockIfPossible(pResult.getBlockPos());
            // 破坏周围k*k的方块
            destroySurroundingBlocks(pResult.getBlockPos());
            this.discard();
        }
    }

    private void destroyBlockIfPossible(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        Block block = state.getBlock();
        this.level().destroyBlock(pos, true, this);
    }

    private void destroySurroundingBlocks(BlockPos center) {
        // 计算周围k*k的区域并破坏方块
        int range = getK() / 2;  // 半径
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    mutablePos.set(center.offset(x, y, z));
                    if (this.level().isLoaded(mutablePos)) {
                        destroyBlockIfPossible(mutablePos);
                    }
                }
            }
        }
    }

    private static final EntityDataAccessor<Integer> DATA_K =
            SynchedEntityData.defineId(PenetratingBulletEntity.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_K, 1);
    }

    public int getK() { return this.entityData.get(DATA_K); }

    public void setK(int k) {
        int kk = (k % 2 == 0) ? (k + 1) : k;   // 偶数向上取奇数
        if (kk < 1) kk = 1;
        this.entityData.set(DATA_K, kk);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("K", this.getK());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("K")) this.setK(tag.getInt("K"));
    }

}
