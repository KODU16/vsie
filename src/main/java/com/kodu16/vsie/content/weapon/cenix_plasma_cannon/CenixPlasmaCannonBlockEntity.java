package com.kodu16.vsie.content.weapon.cenix_plasma_cannon;

import com.kodu16.vsie.content.bullet.entity.CenixPlasmaBulletEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieItems;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
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
    public boolean isEnergyWeapon() {
        // Function: Cenix plasma consumes particle containers through the shared weapon ammo inventory.
        return false;
    }

    @Override
    public Item getAmmoItem() {
        return vsieItems.PARTICLE_CONTAINER.get();
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

        Vec3 launchDirection = new Vec3(direction.x, direction.y, direction.z).normalize();
        CenixPlasmaBulletEntity bullet = new CenixPlasmaBulletEntity(vsieEntities.CENIX_PLASMA_BULLET.get(), level);
        // Function: sync the full launch axis so the client does not begin from vanilla-clamped velocity direction.
        bullet.setPos(spawnPos.add(launchDirection.scale(1.2D)));
        bullet.setLaunchSubLevel(subLevel);
        bullet.setPreciseLaunchVelocity(launchDirection);
        bullet.setBreaksBlocksEnabled(breaksBlocksEnabled());
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
