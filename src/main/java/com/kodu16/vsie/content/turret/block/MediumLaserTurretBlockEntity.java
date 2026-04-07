package com.kodu16.vsie.content.turret.block;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.kodu16.vsie.registries.vsieBlocks;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import software.bernie.geckolib.core.animation.AnimatableManager;

import javax.annotation.Nonnull;
import java.util.List;

public class MediumLaserTurretBlockEntity extends AbstractTurretBlockEntity {
    public MediumLaserTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    private float raycastDistance = 0.0f;//注意，这就是最重要的核心的raycast距离
    @OnlyIn(Dist.CLIENT)

    public Vector3d getShootLocation(Vector3d vec, List<Vector3d> preV, Level lv, Vector3d pos) {
        return vec;
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
        return 10;
    }

    public void shootentity() {
        double distance = Vec.Distance(this.targetPos, currentworldpos);
        double projectionLength = distance;
        turretData.setDistance(projectionLength);
        performRaycast(this.getLevel());
        targetentity.hurt(level.damageSources().onFire(), 15.0F);
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

        // 功能：若命中方块与炮塔处于同一艘 ship，则直接跳过破坏与爆炸，避免误伤自身舰体。
        if (isHitOnCurrentShip(level, hitPos)) {
            return;
        }

        // 功能：当射线命中方块时，以命中点为中心清空 3*3*3 范围内方块（替换为空气）。
        for (BlockPos pos : BlockPos.betweenClosed(
                hitPos.offset(-1, -1, -1),
                hitPos.offset(1, 1, 1)
        )) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }

        // 功能：在命中点触发一次不破坏方块的爆炸，仅用于伤害/特效。
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

    // 功能：判断射线命中方块是否位于炮塔当前所在 ship，用于过滤自舰命中。
    private boolean isHitOnCurrentShip(Level level, BlockPos hitPos) {
        // 功能：获取炮塔所在 ship（若炮塔不在 ship 上则返回 null）。
        ServerShip turretShip = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
        // 功能：获取命中方块所在 ship（若命中不在 ship 上则返回 null）。
        ServerShip hitShip = VSGameUtilsKt.getShipManagingPos(level, hitPos);

        // 功能：仅当双方都在 ship 上且 shipId 相同，才判定为“命中自身 ship”。
        return turretShip != null && hitShip != null && turretShip.getId() == hitShip.getId();
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
