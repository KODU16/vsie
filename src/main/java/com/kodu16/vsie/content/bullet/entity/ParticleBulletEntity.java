package com.kodu16.vsie.content.bullet.entity;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ParticleBulletEntity extends AbstractBulletEntity {

    public ParticleBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level pLevel) {
        super(type, pLevel);
    }

    @Override
    public int accelrateticks() {
        return 5;
    }

    @Override
    public int startemitticks() {
        return 1;
    }

    @Override
    public int stopemitticks() {
        return 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }
}
