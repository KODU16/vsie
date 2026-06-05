package com.kodu16.vsie.content.turret.block;

import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SmallLaserTurretBlockEntity extends MediumLaserTurretBlockEntity {
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.0F;

    public SmallLaserTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public String getturrettype() {
        return "small_laser";
    }

    @Override
    public boolean isEnergyTurret() {
        // Function: small laser turrets follow the same energy-only loading rules as the medium laser family.
        return true;
    }

    @Override
    public Item getAmmoItem() {
        return null;
    }

    @Override
    protected float getBlockBreakTntChance() {
        return BLOCK_BREAK_TNT_CHANCE;
    }

    @Override
    public void shootship() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockPos hitPos = this.getLastShipShotHitBlockPos();
        if (hitPos.equals(BlockPos.ZERO)) {
            return;
        }

        BlockPos bodyHitPos = resolveSubLevelBodyHitPos(level, hitPos);
        if (breaksBlocksEnabled()) {
            // Function: small laser copies medium laser impact behavior but only clears the exact hit block.
            breakTurretTargetBlockAsMined(level, bodyHitPos);
        }

        level.explode(
                null,
                hitPos.getX() + 0.5D,
                hitPos.getY() + 0.5D,
                hitPos.getZ() + 0.5D,
                3.0F,
                false,
                Level.ExplosionInteraction.NONE
        );
    }

    private BlockPos resolveSubLevelBodyHitPos(Level level, BlockPos hitPos) {
        SubLevel hitSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, hitPos);
        if (hitSubLevel != null) {
            return hitPos;
        }

        SubLevel targetShip = getSelectedTargetShip();
        if (targetShip == null) {
            return hitPos;
        }

        // Function: world-space impact positions must be converted to the target sublevel's stored block position.
        Vec3 bodyHitCenter = targetShip.logicalPose().transformPositionInverse(Vec3.atCenterOf(hitPos));
        return BlockPos.containing(bodyHitCenter);
    }
}
