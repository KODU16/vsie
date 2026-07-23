package com.kodu16.vsie.content.bullet.entity;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import com.kodu16.vsie.content.bullet.BulletData;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class HeavyElectroMagnetBulletEntity extends AbstractBulletEntity {
    public static final double SPEED = 21.0D;
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.0F;
    private static final int PIERCING_DURATION_TICKS = 10;
    private static final double BLOCK_BREAK_RADIUS = 5.0D;
    private int configuredMaxLifeTimeTicks = -1;
    private boolean lifetimeExpireExplosionEnabled = true;
    private boolean piercingStarted = false;
    private int piercingTicks = 5;

    public HeavyElectroMagnetBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level pLevel) {
        super(type, pLevel);
        // Function: reuse particle bullet behavior while binding the heavy electromagnetic trail FX.
        setDataBase(BulletData.createHeavyElectroMagnetBulletDefault());
    }

    @Override
    public double getSpeed() {
        // Function: heavy electromagnetic bullets currently use a constant tunable speed.
        return SPEED;
    }

    @Override
    public int startemitticks() {
        return 1;
    }

    @Override
    public int stopemitticks() {
        return getMaxLifeTime();
    }

    @Override
    protected float getBlockBreakTntChance() {
        return BLOCK_BREAK_TNT_CHANCE;
    }

    @Override
    protected double getBlockBreakRadius() {
        return BLOCK_BREAK_RADIUS;
    }

    protected int getPiercingDurationTicks() {
        return PIERCING_DURATION_TICKS;
    }

    public void configureMaxLifeTimeTicks(int maxLifeTimeTicks) {
        // Function: heavy-turret shots can override the shared bullet lifetime to prevent long-lived misses from piling up.
        this.configuredMaxLifeTimeTicks = maxLifeTimeTicks > 0 ? maxLifeTimeTicks : -1;
    }

    public void configureLifetimeExpireExplosion(boolean enabled) {
        // Function: timeout explosions are optional so cleanup can stay cheap when a weapon emits many rounds.
        this.lifetimeExpireExplosionEnabled = enabled;
    }

    protected boolean isPiercingStarted() {
        return piercingStarted;
    }

    @Override
    protected boolean shouldDiscardAfterEntityHit(EntityHitResult result) {
        return false;
    }

    @Override
    protected boolean shouldDiscardAfterBlockHit(BlockHitResult result) {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        this.piercingStarted = true;
        Entity target = result.getEntity();
        if (target != null) {
            target.hurt(this.level().damageSources().onFire(), 15);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        this.piercingStarted = true;
        if (breaksBlocksEnabled() && this.level() instanceof ServerLevel serverLevel) {
            // Function: heavy electromagnetic rounds break a sphere at the first impact point, then keep boring forward.
            destroyBlocksInSphere(serverLevel, result.getLocation(), getBlockBreakRadius());
        }
    }

    @Override
    protected void afterServerBulletMove(HitResult hitResult) {
        if (!this.piercingStarted) {
            return;
        }

        if (breaksBlocksEnabled() && hitResult.getType() != HitResult.Type.BLOCK && this.level() instanceof ServerLevel serverLevel) {
            BlockState state = serverLevel.getBlockState(this.blockPosition());
            if (!state.isAir() && state.isCollisionShapeFullBlock(serverLevel, this.blockPosition())) {
                destroyBlocksInSphere(serverLevel, this.position(), getBlockBreakRadius());
            }
        }

        this.piercingTicks++;
        if (this.piercingTicks >= getPiercingDurationTicks()) {
            this.discard();
        }
    }

    @Override
    protected int getMaxLifeTime() {
        return configuredMaxLifeTimeTicks > 0 ? configuredMaxLifeTimeTicks : super.getMaxLifeTime();
    }

    @Override
    protected void explodeAndDiscardAfterLifetime() {
        if (!lifetimeExpireExplosionEnabled) {
            this.discard();
            return;
        }
        super.explodeAndDiscardAfterLifetime();
    }

    @Override
    protected void startLifecycleFx(FX fx) {
        var effect = new EntityEffectExecutor(fx, this.level(), this, EntityEffectExecutor.AutoRotate.NONE);
        Vec3 velocity = this.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0E-6D) {
            Vec3 direction = velocity.normalize();
            // Function: heavy electromagnetic FX uses local +Y as its forward axis, so align +Y to bullet velocity.
            effect.setRotation(new Quaternionf().rotationTo(
                    0.0F, 1.0F, 0.0F,
                    (float) direction.x,
                    (float) direction.y,
                    (float) direction.z
            ));
        }
        effect.setForcedDeath(false);
        effect.start();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ConfiguredMaxLifeTimeTicks", this.configuredMaxLifeTimeTicks);
        tag.putBoolean("LifetimeExpireExplosionEnabled", this.lifetimeExpireExplosionEnabled);
        tag.putBoolean("PiercingStarted", this.piercingStarted);
        tag.putInt("PiercingTicks", this.piercingTicks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ConfiguredMaxLifeTimeTicks")) {
            this.configuredMaxLifeTimeTicks = tag.getInt("ConfiguredMaxLifeTimeTicks");
        }
        if (tag.contains("LifetimeExpireExplosionEnabled")) {
            this.lifetimeExpireExplosionEnabled = tag.getBoolean("LifetimeExpireExplosionEnabled");
        }
        if (tag.contains("PiercingStarted")) {
            this.piercingStarted = tag.getBoolean("PiercingStarted");
        }
        if (tag.contains("PiercingTicks")) {
            this.piercingTicks = tag.getInt("PiercingTicks");
        }
    }
}
