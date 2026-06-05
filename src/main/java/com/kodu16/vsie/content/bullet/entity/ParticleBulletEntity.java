package com.kodu16.vsie.content.bullet.entity;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import com.kodu16.vsie.content.bullet.BulletData;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class ParticleBulletEntity extends AbstractBulletEntity {
    public static final double SPEED = 6.0D;
    private static final double SHIP_BLOCK_BREAK_RADIUS = 3.0D;
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.1F;
    private boolean explodesOnBlockHit = false;

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

    public void setExplodesOnBlockHit(boolean explodesOnBlockHit) {
        // Function: particle turrets reuse the same projectile, while ship shots enable block-breaking impact.
        this.explodesOnBlockHit = explodesOnBlockHit;
    }

    @Override
    protected float getBlockBreakTntChance() {
        return BLOCK_BREAK_TNT_CHANCE;
    }

    @Override
    protected double getBlockBreakRadius() {
        return SHIP_BLOCK_BREAK_RADIUS;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!explodesOnBlockHit) {
            super.onHitBlock(result);
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            destroyBlocksInSphere(serverLevel, result.getLocation(), getBlockBreakRadius());
            serverLevel.explode(
                    this,
                    result.getLocation().x,
                    result.getLocation().y,
                    result.getLocation().z,
                    3.0F,
                    false,
                    Level.ExplosionInteraction.NONE
            );
        }
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }
}
