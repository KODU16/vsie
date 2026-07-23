package com.kodu16.vsie.content.turret.block;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.client.LaserTurretSoundManager;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.List;

public class SmallLaserTurretBlockEntity extends AbstractTurretBlockEntity {
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.0F;
    private float raycastDistance = 0.0F;

    public SmallLaserTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    @Override
    public void tick() {
        Level level = this.getLevel();
        if (level != null && level.isClientSide()) {
            LaserTurretSoundManager.updateTurret(this);
        }
        super.tick();
    }

    @Override
    public Vec3 getShootLocation(Vec3 vec, List<Vector3d> preV, Level lv, Vec3 pos) {
        return vec;
    }

    @Override
    public String getturrettype() {
        return "small_laser";
    }

    @Override
    public float getLaserLayerRadius() {
        // Function: small laser uses the shared beam layer at half the medium laser radius.
        return 0.125F;
    }

    @Override
    public boolean isEnergyTurret() {
        // Function: small laser turrets fire purely from stored energy and never require ammo items.
        return true;
    }

    @Override
    public Item getAmmoItem() {
        return null;
    }

    @Override
    public double getYAxisOffset() {
        return 0.65D;
    }

    @Override
    public double getcannonlength() {
        return 0.3D;
    }

    @Override
    public float getMaxSpinSpeed() {
        return Mth.PI / 32;
    }

    @Override
    public int getCoolDown() {
        return 15;
    }

    @Override
    public int getenergypertick() {
        return 1;
    }

    @Override
    protected float getBlockBreakTntChance() {
        return BLOCK_BREAK_TNT_CHANCE;
    }

    @Override
    public void shootentity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide() || targetentity == null || !targetentity.isAlive() || targetentity.isRemoved()) {
            return;
        }

        double distance = Vec.Distance(this.targetPos, currentworldpos);
        turretData.setDistance(distance);
        performRaycast(level);
        // Function: laser damage is not fire damage, so fire-immune mobs must still take beam hits.
        targetentity.hurt(level.damageSources().generic(), 15.0F);
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

    private void performRaycast(Level level) {
        updateRaycastDistance(level, this.getBlockState(), (float) turretData.getDistance());
    }

    private void updateRaycastDistance(Level level, BlockState state, float distance) {
        if (Math.abs(this.raycastDistance - distance) > 0.01F) {
            this.raycastDistance = distance;
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(this.worldPosition, state, state, 3);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }
}
