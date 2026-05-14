package com.kodu16.vsie.content.bullet.entity;

import com.kodu16.vsie.content.bullet.AbstractBulletEntity;
import com.kodu16.vsie.content.bullet.BulletData;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CenixPlasmaBulletEntity extends AbstractBulletEntity {
    public static final double SPEED = 5.0D;
    private static final int BLOCK_BREAK_RADIUS = 3;
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
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel serverLevel) {
            BlockPos center = result.getBlockPos();
            // Function: destroy blocks in a radius around the impacted block, then play a non-destructive explosion.
            destroyBlocksInRadius(serverLevel, center, BLOCK_BREAK_RADIUS);
            serverLevel.explode(this, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    3.0F, false, Level.ExplosionInteraction.NONE);
            // Function: impact uses the dedicated hit FX once at the exact block-hit position.
            ModNetworking.sendToAll(new FxPositionS2CPacket(
                    CENIX_PLASMA_BULLET_HIT_FX,
                    result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    new Quaternionf(),
                    new Vector3f(1.0F, 1.0F, 1.0F),
                    false
            ));
        }
        discard();
    }

    private void destroyBlocksInRadius(ServerLevel level, BlockPos center, int radius) {
        int radiusSqr = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSqr) {
                        continue;
                    }
                    BlockPos targetPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(targetPos);
                    if (state.isAir() || state.getDestroySpeed(level, targetPos) < 0.0F) {
                        continue;
                    }
                    level.destroyBlock(targetPos, true, this);
                }
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }
}
