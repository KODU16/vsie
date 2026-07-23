package com.kodu16.vsie.content.bullet.entity;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class InfraKnifeBulletEntity extends AbstractBulletEntity {
    public static final double SPEED = 8.0D;
    private static final float EXPLOSION_POWER = 1.0F;
    private static final int RENDER_COLOR = 0xC0B33333;
    private static final float RENDER_LENGTH = 9F;
    private static final float RENDER_WIDTH = 0.3F;

    public InfraKnifeBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public double getSpeed() {
        // Function: infra-knife bullets fly twice as fast as the base particle bullet.
        return SPEED;
    }

    @Override
    public int startemitticks() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int stopemitticks() {
        return 0;
    }

    @Override
    public int getRenderColor() {
        return RENDER_COLOR;
    }

    @Override
    public float getRenderLength() {
        return RENDER_LENGTH;
    }

    @Override
    public float getRenderWidth() {
        return RENDER_WIDTH;
    }

    @Override
    public int getRenderStartTick() {
        return 0;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel serverLevel) {
            // Function: preserve the old infra-knife terrain impact: exact block cut plus the same small blast.
            breakBlockAsMined(serverLevel, result.getBlockPos());
            serverLevel.explode(
                    this,
                    result.getLocation().x,
                    result.getLocation().y,
                    result.getLocation().z,
                    EXPLOSION_POWER,
                    true,
                    Level.ExplosionInteraction.TNT
            );
        }
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }
}
