package com.kodu16.vsie.content.bullet;

import com.kodu16.vsie.utility.FxData;
import com.kodu16.vsie.utility.vsieFxHelper;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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

import java.util.List;
import java.util.Optional;

import static net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel;

public abstract class AbstractBulletEntity extends Projectile {

    private static final int DEFAULT_MAX_LIFETIME_TICKS = 5 * 20;
    private static final float LIFETIME_EXPIRE_EXPLOSION_POWER = 2.0F;

    private int lifeTime = 0;
    private boolean lifecycleFxStarted = false;
    // BulletData supplies the FX resource used by the lifecycle executor.
    private BulletData dataBase = BulletData.createParticleBulletDefault();

    public BulletData getDataBase() {
        return dataBase;
    }

    public AbstractBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;           // 保持高速、无重力
        this.setNoGravity(true);         // 推荐一起设置
    }

    @Override
    public void tick() {
        // Advance vanilla entity counters so tickCount-based FX windows follow the real entity lifetime.
        super.tick();

        if (!lifecycleFxStarted && this.tickCount >= startemitticks() && this.tickCount <= stopemitticks()) {
            if (this.level().isClientSide()) {
                vsieFxHelper.extractFxUnit(getDataBase().getFxData(), FxData::getAwakeFx)
                        .map(FxData.FxUnit::getId).map(FXHelper::getFX)
                        .ifPresent(this::startLifecycleFx);
                lifecycleFxStarted = true;
            }
        }

        Vec3 movement = applyConstantSpeed();
        updateRotationFromMovement(movement);
        Vec3 start = this.position();
        Vec3 end = start.add(movement);

        // 客户端只负责表现
        if (this.level().isClientSide()) {
            this.setPos(end);
            return;
        }

        // ===== 1 标准射线检测 =====
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(
                this,
                this::canHitEntity
        );

        // ===== 2 防止高速漏判 =====
        if (hitResult.getType() == HitResult.Type.MISS) {

            List<Entity> entities = this.level().getEntities(
                    this,
                    this.getBoundingBox().expandTowards(movement)
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
                hitResult = new EntityHitResult(closest);
            }
        }

        // ===== 处理命中 =====
        if (hitResult.getType() == HitResult.Type.ENTITY) {

            this.onHitEntity((EntityHitResult) hitResult);
            this.discard();
            return;

        } else if (hitResult.getType() == HitResult.Type.BLOCK) {

            this.onHitBlock((BlockHitResult) hitResult);
            this.discard();
            return;
        }

        // ===== 最后移动 =====
        this.setPos(end);

        lifeTime++;

        if (lifeTime >= getMaxLifeTime()) {
            explodeAndDiscardAfterLifetime();
        }
    }

    protected int getMaxLifeTime() {
        // Function: bullets self-destruct after 20 seconds so missed shots cannot accumulate forever.
        return DEFAULT_MAX_LIFETIME_TICKS;
    }

    protected void explodeAndDiscardAfterLifetime() {
        if (this.level() instanceof ServerLevel serverLevel) {
            // Function: timeout explosions are visual/knockback cleanup only and do not destroy terrain.
            serverLevel.explode(this, this.getX(), this.getY(), this.getZ(),
                    LIFETIME_EXPIRE_EXPLOSION_POWER, false, Level.ExplosionInteraction.NONE);
        }
        this.discard();
    }

    // Keep the entity's authoritative rotation aligned with its velocity for hitbox debug and attached FX.
    protected void updateRotationFromMovement(Vec3 movement) {
        if (movement.lengthSqr() < 1.0E-6D) {
            return;
        }

        float yaw = (float) Math.atan2(movement.x, movement.z) * Mth.RAD_TO_DEG;
        float pitch = (float) Math.atan2(movement.y, Math.sqrt(movement.x * movement.x + movement.z * movement.z)) * Mth.RAD_TO_DEG;
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    // Function: subclasses own bullet speed; the base class only enforces constant velocity along current direction.
    public abstract double getSpeed();

    private Vec3 applyConstantSpeed() {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-6D) {
            return movement;
        }

        Vec3 constantMovement = movement.normalize().scale(getSpeed());
        this.setDeltaMovement(constantMovement);
        return constantMovement;
    }

    public abstract int startemitticks();//开始发出粒子的tick数

    public abstract int stopemitticks();//停止发出粒子的tick数

    protected void startLifecycleFx(FX fx) {
        var effect = new EntityEffectExecutor(fx, this.level(), this, EntityEffectExecutor.AutoRotate.XROT);
        // Function: lifecycle bullet FX must stay attached to the entity instead of being force-killed on start.
        effect.setForcedDeath(false);
        effect.start();
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


    // Allow subclasses to override FX data while keeping a particle-bullet fallback.
    public void setDataBase(BulletData dataBase) {
        this.dataBase = dataBase == null ? BulletData.createParticleBulletDefault() : dataBase;
        this.lifecycleFxStarted = false;
    }
}
