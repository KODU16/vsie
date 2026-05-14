package com.kodu16.vsie.content.weapon.cenix_plasma_cannon;

import com.kodu16.vsie.content.bullet.entity.CenixPlasmaBulletEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import static com.kodu16.vsie.content.weapon.AbstractWeaponBlock.FACING;

public class CenixPlasmaCannonBlockEntity extends AbstractWeaponBlockEntity {
    public CenixPlasmaCannonBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return 40;
    }

    @Override
    public void fire() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        Direction weaponFacing = getBlockState().getValue(FACING);
        Vector3d direction = directionToVector(weaponFacing);
        Vec3 spawnPos = Vec3.atCenterOf(getBlockPos());
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        if (subLevel != null) {
            // Function: convert the cannon facing and muzzle position from sublevel space to world space.
            direction = subLevel.logicalPose()
                    .transformNormal(direction, new Vector3d())
                    .normalize();
            spawnPos = subLevel.logicalPose().transformPosition(Vec3.atCenterOf(getBlockPos()));
        } else {
            direction.normalize();
        }

        Vec3 velocity = new Vec3(direction.x, direction.y, direction.z).normalize().scale(CenixPlasmaBulletEntity.SPEED);
        CenixPlasmaBulletEntity bullet = new CenixPlasmaBulletEntity(vsieEntities.CENIX_PLASMA_BULLET.get(), level);
        bullet.setPos(spawnPos.add(velocity.normalize().scale(1.2D)));
        bullet.setDeltaMovement(velocity);
        level.addFreshEntity(bullet);
        //LogUtils.getLogger().warn("adding cenix bullet to:"+spawnPos);
    }

    private static Vector3d directionToVector(Direction direction) {
        return new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("CNXP");
    }

    @Override
    public String getweapontype() {
        return "cenix_plasma_cannon";
    }
}
