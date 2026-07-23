package com.kodu16.vsie.content.turret.block;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.content.turret.client.LaserTurretSoundManager;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.kodu16.vsie.registries.vsieBlocks;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.slf4j.Logger;
import software.bernie.geckolib.animation.AnimatableManager;

import javax.annotation.Nonnull;
import java.util.List;

public class MediumLaserTurretBlockEntity extends AbstractTurretBlockEntity {
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.0F;

    public MediumLaserTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    private float raycastDistance = 0.0f;

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
    public boolean isEnergyTurret() {
        // Function: medium laser turrets fire purely from stored energy and never require ammo items.
        return true;
    }

    @Override
    public Item getAmmoItem() {
        return null;
    }

    public String getturrettype() {
        return "medium_laser";
    }

    public double getYAxisOffset() {return 1.7d;}

    @Override
    public double getcannonlength() {
        return 3;
    }

    @Override
    public float getMaxSpinSpeed() {
        return Mth.PI/32;
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

    public void shootentity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide() || targetentity == null || !targetentity.isAlive() || targetentity.isRemoved()) {
            return;
        }

        double distance = Vec.Distance(this.targetPos, currentworldpos);
        double projectionLength = distance;
        turretData.setDistance(projectionLength);
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

        // Function: clear the real sublevel body blocks around the hit point using vanilla break events and no drops.
        if (breaksBlocksEnabled()) {
            for (BlockPos pos : BlockPos.betweenClosed(
                    bodyHitPos.offset(-1, -1, -1),
                    bodyHitPos.offset(1, 1, 1)
            )) {
                breakTurretTargetBlockAsMined(level, pos);
            }
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

        // Function: visual/world hit positions must be projected into the locked sublevel's body coordinate space.
        Vec3 bodyHitCenter = targetShip.logicalPose().transformPositionInverse(Vec3.atCenterOf(hitPos));
        return BlockPos.containing(bodyHitCenter);
    }

    private void performRaycast(@Nonnull Level level) {
        Logger LOGGER = LogUtils.getLogger();
        BlockState state = this.getBlockState();
        //LOGGER.warn(String.valueOf(Component.literal("throttle:"+thrusterData.getThrottle())));
        //LOGGER.warn(String.valueOf(Component.literal("raycastdistance:"+-thrusterData.getThrottle()*getMaxFlameDistance())));
        updateRaycastDistance(level, state, (float) turretData.getDistance());
    }

    private void updateRaycastDistance(@Nonnull Level level, @Nonnull BlockState state, float distance) {
        if (Math.abs(this.raycastDistance - distance) > 0.01f) {
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

