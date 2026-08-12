package com.kodu16.vsie.content.misc.enemy_autocannon;

import com.kodu16.vsie.content.misc.enemy_cannon.EnemyCannonCbcCompat;
import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import rbasamoyai.createbigcannons.config.CBCConfigs;
import rbasamoyai.createbigcannons.index.CBCAutocannonMaterials;
import rbasamoyai.createbigcannons.munitions.autocannon.AbstractAutocannonProjectile;
import rbasamoyai.createbigcannons.munitions.autocannon.AutocannonAmmoItem;

public final class EnemyAutocannonCbcCompat {
    private EnemyAutocannonCbcCompat() {
    }

    public static boolean isSupportedProjectile(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AutocannonAmmoItem;
    }

    public static boolean fireConfiguredProjectile(
            ServerLevel level,
            ServerSubLevel ship,
            BlockPos cannonPos,
            double spawnForwardDistance,
            ItemStack projectileStack,
            double spreadAngle
    ) {
        BlockState state = level.getBlockState(cannonPos);
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)
                || !(projectileStack.getItem() instanceof AutocannonAmmoItem ammoItem)) {
            return false;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Vector3d worldDirection = ship.logicalPose().orientation().transform(
                new Vector3d(facing.getStepX(), 0.0D, facing.getStepZ())
        ).normalize();
        Vec3 direction = new Vec3(worldDirection.x, worldDirection.y, worldDirection.z);
        Vec3 muzzle = ServerShipUtils.getBlockCenterWorld(ship, cannonPos)
                .add(direction.scale(Math.max(0.0D, spawnForwardDistance)));

        ItemStack roundStack = projectileStack.copyWithCount(1);
        AbstractAutocannonProjectile projectile = ammoItem.getAutocannonProjectile(roundStack, level);
        if (projectile == null) {
            return false;
        }

        var material = CBCAutocannonMaterials.CAST_IRON.properties();
        projectile.setPos(muzzle);
        projectile.setChargePower(1.0F);
        projectile.setTracer(CBCConfigs.server().munitions.allAutocannonProjectilesAreTracers.get()
                || ammoItem.isTracer(roundStack));
        projectile.setLifetime(material.projectileLifetime());
        // Function: CBC interprets the final argument as angular divergence in degrees.
        projectile.shoot(direction.x, direction.y, direction.z, material.baseSpeed(), (float) spreadAngle);
        projectile.setDeltaMovement(projectile.getDeltaMovement()
                .add(EnemyCannonCbcCompat.calculateShipLinearVelocityPerTick(ship)));
        if (!level.addFreshEntity(projectile)) {
            return false;
        }
        projectile.xRotO = projectile.getXRot();
        projectile.yRotO = projectile.getYRot();
        return true;
    }
}
