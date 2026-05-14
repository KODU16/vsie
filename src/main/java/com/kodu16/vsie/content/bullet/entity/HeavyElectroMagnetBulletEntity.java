package com.kodu16.vsie.content.bullet.entity;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import com.kodu16.vsie.content.bullet.BulletData;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class HeavyElectroMagnetBulletEntity extends AbstractBulletEntity {
    public static final double SPEED = 5.0D;

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

    }
}
