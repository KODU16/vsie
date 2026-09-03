package com.kodu16.vsie.content.aeroie_custom;

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Definition-configured projectile emitted by non-energy custom turrets. */
public final class CustomTurretProjectileEntity extends AbstractBulletEntity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_SPEED =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> DATA_PROJECTILE_FX =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_PROJECTILE_FX_SCALE =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_PROJECTILE_FX_ROT_X =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_PROJECTILE_FX_ROT_Y =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_PROJECTILE_FX_ROT_Z =
            SynchedEntityData.defineId(CustomTurretProjectileEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private double configuredSpeed = 6.0D;
    private float configuredScale = 1.0F;
    private float configuredDamage = 15.0F;
    private float configuredExplosionRadius;
    private int configuredLifetimeTicks = 20 * 15;
    private String configuredProjectileFx = "";
    private float configuredProjectileFxScale = 1.0F;
    private final float[] configuredProjectileFxRotation = new float[]{0.0F, 0.0F, 0.0F};
    private String clientAppliedProjectileFx = "";

    public CustomTurretProjectileEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
    }

    public void configure(CustomDeviceDefinition definition) {
        // Function: definitions store blocks/second while entities advance in blocks/tick.
        configuredSpeed = definition.projectileSpeedBlocksPerSecond / 20.0D;
        configuredScale = definition.projectileScale;
        configuredDamage = definition.projectileDamage;
        configuredExplosionRadius = definition.projectileExplosionRadius;
        configuredLifetimeTicks = definition.projectileLifetimeTicks;
        configuredProjectileFx = definition.projectileFx.fx;
        configuredProjectileFxScale = definition.projectileFx.scale;
        System.arraycopy(definition.projectileFx.rotation, 0, configuredProjectileFxRotation, 0,
                Math.min(definition.projectileFx.rotation.length, configuredProjectileFxRotation.length));
        entityData.set(DATA_SPEED, (float) configuredSpeed);
        entityData.set(DATA_SCALE, configuredScale);
        entityData.set(DATA_LIFETIME, configuredLifetimeTicks);
        entityData.set(DATA_PROJECTILE_FX, configuredProjectileFx);
        syncProjectileFxData();
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            applySyncedProjectileFx();
        }
        super.tick();
    }

    private void applySyncedProjectileFx() {
        String fx = entityData.get(DATA_PROJECTILE_FX);
        if (fx == null || fx.isBlank() || fx.equals(clientAppliedProjectileFx)) {
            return;
        }
        // Function: custom projectile FX starts from the entity after its spawn data exists on the client.
        setDataBase(BulletData.createWithAwake(CustomFxResources.toResourceLocation(fx)));
        clientAppliedProjectileFx = fx;
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
        builder.define(DATA_LIFETIME, 20 * 15);
        builder.define(DATA_PROJECTILE_FX, "");
        builder.define(DATA_PROJECTILE_FX_SCALE, 1.0F);
        builder.define(DATA_PROJECTILE_FX_ROT_X, 0.0F);
        builder.define(DATA_PROJECTILE_FX_ROT_Y, 0.0F);
        builder.define(DATA_PROJECTILE_FX_ROT_Z, 0.0F);
    }

    @Override
    protected void startLifecycleFx(FX fx) {
        var effect = new EntityEffectExecutor(fx, this.level(), this, EntityEffectExecutor.AutoRotate.XROT);
        float scale = entityData.get(DATA_PROJECTILE_FX_SCALE);
        // Function: projectile-bound custom FX follows the entity and uses definition-authored transform offsets.
        effect.setScale(new Vector3f(scale, scale, scale));
        effect.setRotation(new Quaternionf()
                .rotateX((float) Math.toRadians(entityData.get(DATA_PROJECTILE_FX_ROT_X)))
                .rotateY((float) Math.toRadians(entityData.get(DATA_PROJECTILE_FX_ROT_Y)))
                .rotateZ((float) Math.toRadians(entityData.get(DATA_PROJECTILE_FX_ROT_Z))));
        effect.setForcedDeath(false);
        effect.start();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("CustomSpeed", configuredSpeed);
        tag.putFloat("CustomScale", configuredScale);
        tag.putFloat("CustomDamage", configuredDamage);
        tag.putFloat("CustomExplosionRadius", configuredExplosionRadius);
        tag.putInt("CustomLifetime", configuredLifetimeTicks);
        tag.putString("CustomProjectileFx", configuredProjectileFx);
        tag.putFloat("CustomProjectileFxScale", configuredProjectileFxScale);
        tag.putFloat("CustomProjectileFxRotX", configuredProjectileFxRotation[0]);
        tag.putFloat("CustomProjectileFxRotY", configuredProjectileFxRotation[1]);
        tag.putFloat("CustomProjectileFxRotZ", configuredProjectileFxRotation[2]);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CustomSpeed")) configuredSpeed = tag.getDouble("CustomSpeed");
        if (tag.contains("CustomScale")) configuredScale = tag.getFloat("CustomScale");
        if (tag.contains("CustomDamage")) configuredDamage = tag.getFloat("CustomDamage");
        if (tag.contains("CustomExplosionRadius")) configuredExplosionRadius = tag.getFloat("CustomExplosionRadius");
        if (tag.contains("CustomLifetime")) configuredLifetimeTicks = tag.getInt("CustomLifetime");
        if (tag.contains("CustomProjectileFx")) {
            configuredProjectileFx = tag.getString("CustomProjectileFx");
        }
        if (tag.contains("CustomProjectileFxScale")) configuredProjectileFxScale = tag.getFloat("CustomProjectileFxScale");
        if (tag.contains("CustomProjectileFxRotX")) configuredProjectileFxRotation[0] = tag.getFloat("CustomProjectileFxRotX");
        if (tag.contains("CustomProjectileFxRotY")) configuredProjectileFxRotation[1] = tag.getFloat("CustomProjectileFxRotY");
        if (tag.contains("CustomProjectileFxRotZ")) configuredProjectileFxRotation[2] = tag.getFloat("CustomProjectileFxRotZ");
        entityData.set(DATA_SPEED, (float) configuredSpeed);
        entityData.set(DATA_SCALE, configuredScale);
        entityData.set(DATA_LIFETIME, configuredLifetimeTicks);
        entityData.set(DATA_PROJECTILE_FX, configuredProjectileFx);
        syncProjectileFxData();
    }

    private void syncProjectileFxData() {
        // Function: synced FX transform lets late-spawned or NBT-restored projectiles render consistently on clients.
        entityData.set(DATA_PROJECTILE_FX_SCALE, configuredProjectileFxScale);
        entityData.set(DATA_PROJECTILE_FX_ROT_X, configuredProjectileFxRotation[0]);
        entityData.set(DATA_PROJECTILE_FX_ROT_Y, configuredProjectileFxRotation[1]);
        entityData.set(DATA_PROJECTILE_FX_ROT_Z, configuredProjectileFxRotation[2]);
    }
}
