package com.kodu16.vsie.content.bullet.entity;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import com.kodu16.vsie.content.bullet.BulletData;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CenixPlasmaBulletEntity extends AbstractBulletEntity {
    public static final double SPEED = 10.0D;
    private static final double BLOCK_BREAK_RADIUS = 10.0D;
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.0F;
    private static final ResourceLocation CENIX_PLASMA_BULLET_HIT_FX = ResourceLocation.fromNamespaceAndPath("vsie", "cenix_plasma_bullet_hit");

    public CenixPlasmaBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level pLevel) {
        super(type, pLevel);
        // Function: bind the plasma trail to the entity lifetime.
        setDataBase(BulletData.createCenixPlasmaBulletDefault());
    }

    @Override
    public double getSpeed() {
        // Function: Cenix plasma bullets currently use a constant tunable speed.
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

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel serverLevel) {
            // Function: plasma bullets break a sphere centered on the exact impact point before the impact FX plays.
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
            // Function: impact uses the dedicated hit FX once at the exact block-hit position.
            ModNetworking.sendToAll(new FxPositionS2CPacket(
                    CENIX_PLASMA_BULLET_HIT_FX,
                    result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    new Quaternionf(),
                    new Vector3f(2.0F, 2.0F, 2.0F),
                    false
            ));
        }
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }
}
