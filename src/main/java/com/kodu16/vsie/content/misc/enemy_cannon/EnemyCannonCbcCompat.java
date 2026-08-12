package com.kodu16.vsie.content.misc.enemy_cannon;

import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock;

public final class EnemyCannonCbcCompat {
    private static final double MINECRAFT_TICKS_PER_SECOND = 20.0D;

    private EnemyCannonCbcCompat() {
    }

    public static boolean isSupportedProjectile(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ProjectileBlock<?>;
    }

    public static boolean fireConfiguredProjectile(
            ServerLevel level,
            ServerSubLevel ship,
            BlockPos cannonPos,
            double spawnForwardDistance,
            ItemStack projectileStack,
            int chargeCount
    ) {
        BlockState cannonState = level.getBlockState(cannonPos);
        if (!cannonState.hasProperty(HorizontalDirectionalBlock.FACING)
                || !isSupportedProjectile(projectileStack)
                || !(projectileStack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof ProjectileBlock<?> projectileBlock)) {
            return false;
        }

        Direction facing = cannonState.getValue(HorizontalDirectionalBlock.FACING);
        Vector3d worldDirection = ship.logicalPose().orientation().transform(
                new Vector3d(facing.getStepX(), 0.0D, facing.getStepZ())
        ).normalize();
        Vec3 direction = new Vec3(worldDirection.x, worldDirection.y, worldDirection.z);
        Vec3 muzzle = ServerShipUtils.getBlockCenterWorld(ship, cannonPos)
                .add(direction.scale(Math.max(0.0D, spawnForwardDistance)));

        // Function: CBC reconstructs fuze, tracer and other projectile components from this exact item stack.
        AbstractBigCannonProjectile projectile = projectileBlock.getProjectile(level, projectileStack.copyWithCount(1));
        if (projectile == null) {
            return false;
        }

        float chargePower = Math.max(1, chargeCount);
        projectile.setPos(muzzle);
        projectile.setChargePower(chargePower);
        projectile.shoot(direction.x, direction.y, direction.z, chargePower, 0.0F);
        // Function: convert Sable's meters-per-second velocity to Minecraft entity movement per tick.
        projectile.setDeltaMovement(projectile.getDeltaMovement().add(calculateShipLinearVelocityPerTick(ship)));
        if (level.addFreshEntity(projectile)) {
            // Function: initialize interpolation angles exactly as CBC's mounted-cannon firing path does.
            projectile.xRotO = projectile.getXRot();
            projectile.yRotO = projectile.getYRot();
            return true;
        }
        return false;
    }

    public static Vec3 calculateShipLinearVelocityPerTick(ServerSubLevel ship) {
        RigidBodyHandle handle = RigidBodyHandle.of(ship);
        if (handle == null || !handle.isValid()) {
            return Vec3.ZERO;
        }

        SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(ship.getLevel());
        if (physicsSystem == null) {
            return Vec3.ZERO;
        }
        int substepsPerTick = physicsSystem.getConfig().substepsPerTick;
        if (substepsPerTick <= 0) {
            return Vec3.ZERO;
        }

        double physicsTicksPerSecond = MINECRAFT_TICKS_PER_SECOND * substepsPerTick;
        double physicsStepSeconds = 1.0D / physicsTicksPerSecond;
        double entityTickSeconds = physicsStepSeconds * substepsPerTick;
        Vector3d linearVelocity = handle.getLinearVelocity(new Vector3d());
        // Function: accumulate every configured Sable substep covered by one Minecraft entity tick.
        return isFinite(linearVelocity)
                ? new Vec3(linearVelocity.x, linearVelocity.y, linearVelocity.z)
                        .scale(entityTickSeconds)
                : Vec3.ZERO;
    }

    private static boolean isFinite(Vector3d value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
}
