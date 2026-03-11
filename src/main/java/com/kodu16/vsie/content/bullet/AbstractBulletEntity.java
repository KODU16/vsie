package com.kodu16.vsie.content.bullet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import software.bernie.example.registry.EntityRegistry;

import java.util.List;
import java.util.Optional;

import static net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel;

public abstract class AbstractBulletEntity extends Projectile {

    private int lifeTime = 0;
    private double damage = 1.0;

    public AbstractBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;           // 保持高速、无重力
        this.setNoGravity(true);         // 推荐一起设置
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // 可选：客户端插值表现可以保留原逻辑
            this.setPos(this.position().add(this.getDeltaMovement()));
            return;
        }

        Vec3 start = this.position();           // 本tick开始位置（上一tick结束位置）
        Vec3 movement = this.getDeltaMovement();
        Vec3 end = start.add(movement);         // 本tick理论结束位置

        // 1. 先做标准的射线检测（方块 + 实体）
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(
                this,
                this::canHitEntity
                // ← 重要：把检测距离传进去
        );

        // 2. 如果没有命中，再做一个保守的AABB扫描（防漏）
        if (hitResult.getType() == HitResult.Type.MISS) {
            // 用稍微大一点的膨胀来补救
            List<Entity> entities = this.level().getEntities(
                    this,
                    this.getBoundingBox().expandTowards(movement).inflate(1.8, 1.8, 1.8)
            );

            Entity closest = null;
            double closestDistSq = Double.MAX_VALUE;

            for (Entity entity : entities) {
                if (!this.canHitEntity(entity)) continue;

                // 计算子弹路径与实体AABB的最近交点距离
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
            this.discard();
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) hitResult);
            this.discard();
        }

        // 最后才真正移动
        this.setPos(end);

        lifeTime++;
        if (lifeTime > 3 * 20) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        BlockState state = (level().getBlockState(pResult.getBlockPos()));
        if(state.isCollisionShapeFullBlock(level(), pResult.getBlockPos())) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity target = pResult.getEntity();
        target.hurt(this.level().damageSources().onFire(),15);
        this.discard();
    }

    @Override
    protected void defineSynchedData() {

    }
}
