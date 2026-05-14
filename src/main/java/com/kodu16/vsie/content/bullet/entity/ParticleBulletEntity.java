package com.kodu16.vsie.content.bullet.entity;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import com.kodu16.vsie.content.bullet.BulletData;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ParticleBulletEntity extends AbstractBulletEntity {
    public static final double SPEED = 5.0D;

    public ParticleBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level pLevel) {
        super(type, pLevel);
        // Bind the bullet trail to the entity lifetime instead of the firing cannon event.
        setDataBase(BulletData.createParticleBulletDefault());
    }

    @Override
    public double getSpeed() {
        // Function: particle bullets currently use a constant tunable speed.
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }
}
