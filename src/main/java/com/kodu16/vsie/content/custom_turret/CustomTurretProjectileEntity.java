package com.kodu16.vsie.content.custom_turret;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Definition-configured projectile emitted by non-energy custom turrets. */
public final class CustomTurretProjectileEntity extends AbstractBulletEntity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_SPEED =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private double configuredSpeed = 6.0D;
    private float configuredScale = 1.0F;
    private float configuredDamage = 15.0F;
    private float configuredExplosionRadius;
    private int configuredLifetimeTicks = 100;

    public CustomTurretProjectileEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
    }

    public void configure(CustomTurretDefinition definition) {
        // Function: definitions store blocks/second while entities advance in blocks/tick.
        configuredSpeed = definition.projectileSpeedBlocksPerSecond / 20.0D;
        configuredScale = definition.projectileScale;
        configuredDamage = definition.projectileDamage;
        configuredExplosionRadius = definition.projectileExplosionRadius;
        configuredLifetimeTicks = definition.projectileLifetimeTicks;
        entityData.set(DATA_SPEED, (float) configuredSpeed);
        entityData.set(DATA_SCALE, configuredScale);
        entityData.set(DATA_LIFETIME, configuredLifetimeTicks);
    }

    @Override
    public double getSpeed() {
        return entityData.get(DATA_SPEED);
    }

    @Override
    protected int getMaxLifeTime() {
        return entityData.get(DATA_LIFETIME);
    }

    @Override
    public int startemitticks() {
        return 1;
    }

    @Override
    public int stopemitticks() {
        return entityData.get(DATA_LIFETIME);
    }

    @Override
    public float getRenderLength() {
        return 4.0F * entityData.get(DATA_SCALE);
    }

    @Override
    public float getRenderWidth() {
        return entityData.get(DATA_SCALE);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        target.hurt(level().damageSources().generic(), configuredDamage);
        explode(result.getLocation());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        explode(result.getLocation());
        discard();
    }

    private void explode(net.minecraft.world.phys.Vec3 position) {
        if (configuredExplosionRadius > 0.0F && level() instanceof ServerLevel serverLevel) {
            serverLevel.explode(this, position.x, position.y, position.z, configuredExplosionRadius,
                    false, Level.ExplosionInteraction.NONE);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // Function: client motion and rendering read the definition-authored speed, scale and lifetime at spawn.
        builder.define(DATA_SPEED, 6.0F);
        builder.define(DATA_SCALE, 1.0F);
        builder.define(DATA_LIFETIME, 100);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("CustomSpeed", configuredSpeed);
        tag.putFloat("CustomScale", configuredScale);
        tag.putFloat("CustomDamage", configuredDamage);
        tag.putFloat("CustomExplosionRadius", configuredExplosionRadius);
        tag.putInt("CustomLifetime", configuredLifetimeTicks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CustomSpeed")) configuredSpeed = tag.getDouble("CustomSpeed");
        if (tag.contains("CustomScale")) configuredScale = tag.getFloat("CustomScale");
        if (tag.contains("CustomDamage")) configuredDamage = tag.getFloat("CustomDamage");
        if (tag.contains("CustomExplosionRadius")) configuredExplosionRadius = tag.getFloat("CustomExplosionRadius");
        if (tag.contains("CustomLifetime")) configuredLifetimeTicks = tag.getInt("CustomLifetime");
        entityData.set(DATA_SPEED, (float) configuredSpeed);
        entityData.set(DATA_SCALE, configuredScale);
        entityData.set(DATA_LIFETIME, configuredLifetimeTicks);
    }
}
