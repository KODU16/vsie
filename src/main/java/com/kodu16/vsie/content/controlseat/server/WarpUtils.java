package com.kodu16.vsie.content.controlseat.server;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.warpprojectile.WarpProjecTileEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.registries.vsieEntities;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import rbasamoyai.ritchiesprojectilelib.effects.screen_shake.ScreenShakeEffect;
import rbasamoyai.ritchiesprojectilelib.network.ClientboundShakeScreenPacket;
import rbasamoyai.ritchiesprojectilelib.network.RPLNetwork;

import javax.annotation.Nullable;

public final class WarpUtils {
    private static final double AXIS_EPSILON = 1.0E-8D;
    // Function: keep warp launch tolerances authored in degrees; vector acos still compares in radians.
    private static final double WARP_LAUNCH_ALIGNMENT_ANGLE_DEGREES = 0.35D;
    private static final double WARP_LAUNCH_ROLL_ANGLE_DEGREES = 0.50D;
    private static final double WARP_LAUNCH_ALIGNMENT_ANGLE_RADIANS = Math.toRadians(WARP_LAUNCH_ALIGNMENT_ANGLE_DEGREES);
    private static final double WARP_LAUNCH_ROLL_ANGLE_RADIANS = Math.toRadians(WARP_LAUNCH_ROLL_ANGLE_DEGREES);
    static final double WARP_SETTLE_ANGULAR_SPEED = 0.8D;
    private static final double WARP_PROJECTILE_DISTANCE_SCALE = 1.5D;
    private static final int WARP_TELEPORT_EXTRA_DELAY_TICKS = 100;
    private static final int WARP_COMPLETE_SCREEN_SHAKE_TICKS = 10;
    
    private static final double WARP_COMPLETE_SCREEN_SHAKE_RADIUS = 96.0D;
    private static final float WARP_COMPLETE_SCREEN_SHAKE_YAW = 5F;
    private static final float WARP_COMPLETE_SCREEN_SHAKE_PITCH = 5F;
    private static final float WARP_COMPLETE_SCREEN_SHAKE_ROLL = 0.25F;
    private static final float WARP_COMPLETE_SCREEN_SHAKE_JITTER = 1F;
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);

    private WarpUtils() {
    }

    public static Vec3 calculatePreparationSeatControl(
            ControlSeatServerData data,
            ServerSubLevel subLevel,
            Vec3 aimForward,
            Vec3 aimUp,
            Vec3 aimRight,
            Vec3 controlForward,
            Vec3 controlUp,
            Vec3 controlRight
    ) {
        Vec3 worldControl = calculatePreparationWorldControl(data, subLevel, aimForward, aimUp, aimRight);
        if (!isUsableAxis(worldControl) || !isUsableAxis(controlForward) || !isUsableAxis(controlUp) || !isUsableAxis(controlRight)) {
            return Vec3.ZERO;
        }

        // Function: warp alignment must enter the same seat-local torque path as pilot mouse input.
        return new Vec3(
                worldControl.dot(controlForward.normalize()),
                worldControl.dot(controlUp.normalize()),
                worldControl.dot(controlRight.normalize())
        );
    }

    private static Vec3 calculatePreparationWorldControl(
            ControlSeatServerData data,
            ServerSubLevel subLevel,
            Vec3 worldForward,
            Vec3 worldUp,
            Vec3 worldRight
    ) {
        if (data.warpTargetName == null || data.warpTargetName.isEmpty() || data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return Vec3.ZERO;
        }

        Vec3 targetDirection = getWarpPreparationDirection(data, subLevel);
        if (targetDirection == null) {
            return Vec3.ZERO;
        }

        Vec3 currentForward = worldForward.normalize();
        double forwardAngleRadians = angleBetweenNormalized(currentForward, targetDirection);
        Vec3 correctionAxis = axisToRotate(currentForward, targetDirection, worldUp, forwardAngleRadians);

        Vec3 desiredHorizonUp = projectWorldUpToForwardPlane(currentForward);
        Vec3 currentHorizonUp = rejectAlongAxis(worldUp.normalize(), currentForward);
        if (isUsableAxis(desiredHorizonUp) && isUsableAxis(currentHorizonUp)) {
            Vec3 desiredUp = desiredHorizonUp.normalize();
            Vec3 currentUp = currentHorizonUp.normalize();
            double rollAngleRadians = signedAngleAroundAxis(currentUp, desiredUp, currentForward);
            correctionAxis = correctionAxis.add(currentForward.scale(rollAngleRadians));
        }

        // Function: warp alignment is pure proportional control; torque magnitude follows angle error only.
        return correctionAxis;
    }

    public static void tryLaunchWarpProjectile(
            ControlSeatServerData data,
            ServerSubLevel subLevel,
            Vector3d omega,
            Vec3 worldForward,
            Vec3 worldUp,
            Vec3 worldRight
    ) {
        if (data.hasPendingWarpTeleport) {
            return;
        }
        if (data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return;
        }

        Vec3 launchDirection = getWarpPreparationDirection(data, subLevel);
        if (launchDirection == null) {
            return;
        }

        if (!isWarpAlignedForLaunch(data, launchDirection, omega, worldForward, worldUp, worldRight)) {
            return;
        }
        Level level = data.level;
        Vec3 shipPos = ServerShipUtils.getStructureCenterWorld(subLevel);
        if (level == null || shipPos == null) {
            return;
        }

        double structureMaxDimension = ServerShipUtils.getStructureMaxDimension(subLevel);
        if (structureMaxDimension <= 0.0D) {
            return;
        }
        double projectileTravelDistance = structureMaxDimension * WARP_PROJECTILE_DISTANCE_SCALE;

        ControlSeatBlockEntity controlSeat = getControlSeatBlockEntity(data);
        if (controlSeat == null) {
            return;
        }
        int e710Cost = data.warpE710CostMb > 0 ? data.warpE710CostMb : controlSeat.calculateWarpE710CostMb(data.warpTargetPos);
        // Function: consume E-710 at the actual jump launch so refueling or draining during alignment is respected.
        if (!controlSeat.consumeE710ForWarp(e710Cost)) {
            data.rejectWarpForInsufficientE710(e710Cost);
            syncWarpPreparationState(data);
            return;
        }

        Vec3 targetWorldPos = getWarpTargetWorldPos(data);
        spawnWarpProjectile(level, shipPos, launchDirection, projectileTravelDistance, structureMaxDimension);
        // Function: mirror the launch visual at the warp destination with the same velocity direction.
        spawnWarpProjectile(level, targetWorldPos, launchDirection, projectileTravelDistance, structureMaxDimension);

        long executeGameTime = level.getGameTime()
                + WarpProjecTileEntity.lifeTicksForDistance(projectileTravelDistance)
                + WARP_TELEPORT_EXTRA_DELAY_TICKS;
        data.schedulePendingWarpTeleport(new Vector3d(targetWorldPos.x, targetWorldPos.y, targetWorldPos.z), executeGameTime);
        data.transitionWarpPreparationToPendingTeleport();
        syncWarpPreparationState(data);
    }

    public static void processPendingWarpTeleport(ControlSeatServerData data, ServerSubLevel subLevel) {
        Level level = data.level;
        if (level == null || level.isClientSide() || !data.hasPendingWarpTeleport) {
            return;
        }
        if (level.getGameTime() < data.pendingWarpTeleportGameTime) {
            return;
        }
        // Function: copy the pending target before clearing state, otherwise clearPendingWarpTeleport resets it to zero.
        Vector3d pendingTeleportPos = new Vector3d(data.pendingWarpTeleportPos);
        Vec3 completionCenter = calculateWarpCompletionCenter(subLevel, pendingTeleportPos);
        Player seatedPlayer = data.getPlayer();
        if (ServerShipUtils.teleportKeepOrientation(subLevel, pendingTeleportPos)) {
            restoreSeatPassengerAfterWarp(data, seatedPlayer);
            shakeScreenAtWarpCompletion(data, completionCenter);
            data.clearPendingWarpTeleport();
        }
    }

    private static boolean isWarpAlignedForLaunch(ControlSeatServerData data, Vec3 launchDirection, Vector3d omega, Vec3 worldForward, Vec3 worldUp, Vec3 worldRight) {
        double forwardAngleRadians = angleBetweenNormalized(worldForward.normalize(), launchDirection);
        if (forwardAngleRadians > WARP_LAUNCH_ALIGNMENT_ANGLE_RADIANS) {
            return false;
        }

        Vec3 currentForward = worldForward.normalize();
        Vec3 desiredHorizonUp = projectWorldUpToForwardPlane(currentForward);
        Vec3 currentHorizonUp = rejectAlongAxis(worldUp.normalize(), currentForward);
        if (isUsableAxis(desiredHorizonUp) && isUsableAxis(currentHorizonUp)) {
            double rollAngleRadians = Math.abs(signedAngleAroundAxis(currentHorizonUp.normalize(), desiredHorizonUp.normalize(), currentForward));
            if (rollAngleRadians > WARP_LAUNCH_ROLL_ANGLE_RADIANS) {
                return false;
            }
        }

        Vec3 angularVelocity = Vec.toVec3(omega);
        Vec3 lateralAngularVelocity = rejectAlongAxis(angularVelocity, currentForward);
        double rollRate = Math.abs(angularVelocity.dot(currentForward));
        // Function: projectile launch waits for both forward aim settle and horizon roll settle.
        return lateralAngularVelocity.length() <= WARP_SETTLE_ANGULAR_SPEED
                && rollRate <= WARP_SETTLE_ANGULAR_SPEED;
    }

    private static Vec3 getWarpPreparationDirection(ControlSeatServerData data, ServerSubLevel subLevel) {
        if (data.hasWarpStartSnapshot && isFiniteVector(data.warpLaunchDirection)) {
            Vec3 cachedDirection = Vec.toVec3(data.warpLaunchDirection);
            if (cachedDirection.lengthSqr() >= AXIS_EPSILON) {
                return cachedDirection.normalize();
            }
        }

        Vec3 startWorldPos = getWarpStartWorldPos(subLevel);
        Vec3 targetWorldPos = getWarpTargetWorldPos(data);
        Vec3 targetDirection = targetWorldPos.subtract(startWorldPos);
        if (targetDirection.lengthSqr() < 1.0E-6D) {
            return null;
        }

        Vec3 normalizedDirection = targetDirection.normalize();
        data.warpStartSubLevelWorldPos = Vec.toVector3d(startWorldPos);
        data.warpLaunchDirection = Vec.toVector3d(normalizedDirection);
        data.hasWarpStartSnapshot = true;
        return normalizedDirection;
    }

    private static void spawnWarpProjectile(Level level, Vec3 position, Vec3 launchDirection, double projectileTravelDistance, double structureMaxDimension) {
        if (level == null || level.isClientSide()) {
            return;
        }
        WarpProjecTileEntity warpProjectile = new WarpProjecTileEntity(vsieEntities.WARP_PROJECTILE.get(), level);
        // Function: keep projectile flight range and FX scale identical for source and target-side launch visuals.
        warpProjectile.setPos(position.x, position.y, position.z);
        warpProjectile.configureLaunch(launchDirection, projectileTravelDistance, structureMaxDimension);
        level.addFreshEntity(warpProjectile);
    }

    private static void restoreSeatPassengerAfterWarp(ControlSeatServerData data, @Nullable Player seatedPlayer) {
        if (!(seatedPlayer instanceof ServerPlayer serverPlayer) || !serverPlayer.isAlive() || serverPlayer.isRemoved()) {
            return;
        }

        if (serverPlayer.getVehicle() instanceof ControlSeatMountEntity mount
                && mount.getBoundBlockPos().equals(data.controlSeatPos)) {
            return;
        }

        ControlSeatBlockEntity controlSeat = getControlSeatBlockEntity(data);
        if (controlSeat == null) {
            return;
        }

        // Function: warp teleport can occasionally drop the rider, so force a fresh seat mount before later control packets arrive.
        controlSeat.sit(serverPlayer, true);
    }

    private static Vec3 calculateWarpCompletionCenter(ServerSubLevel subLevel, Vector3d targetPoseWorld) {
        Vec3 targetPose = new Vec3(targetPoseWorld.x, targetPoseWorld.y, targetPoseWorld.z);
        Vec3 currentCenter = ServerShipUtils.getStructureCenterWorld(subLevel);
        if (currentCenter == null) {
            return targetPose;
        }

        var currentPose = subLevel.logicalPose().position();
        Vec3 currentPoseWorld = new Vec3(currentPose.x(), currentPose.y(), currentPose.z());
        // Function: teleport moves the sublevel pose, so preserve the model-center offset from that pose.
        return targetPose.add(currentCenter.subtract(currentPoseWorld));
    }

    private static void shakeScreenAtWarpCompletion(ControlSeatServerData data, Vec3 centerWorldPos) {
        if (!(data.level instanceof ServerLevel serverLevel) || centerWorldPos == null) {
            return;
        }

        double radiusSqr = WARP_COMPLETE_SCREEN_SHAKE_RADIUS * WARP_COMPLETE_SCREEN_SHAKE_RADIUS;
        for (ServerPlayer player : serverLevel.players()) {
            double distanceSqr = player.distanceToSqr(centerWorldPos);
            if (distanceSqr > radiusSqr) {
                continue;
            }

            double distanceFactor = 1.0D - Math.sqrt(distanceSqr) / WARP_COMPLETE_SCREEN_SHAKE_RADIUS;
            float scale = (float) Math.max(0.15D, distanceFactor);
            // Function: use the same shake parameters and attenuation rule as heavy_electromagnet_turret firing.
            ScreenShakeEffect effect = new ScreenShakeEffect(
                    WARP_COMPLETE_SCREEN_SHAKE_TICKS,
                    WARP_COMPLETE_SCREEN_SHAKE_YAW * scale,
                    WARP_COMPLETE_SCREEN_SHAKE_PITCH * scale,
                    WARP_COMPLETE_SCREEN_SHAKE_ROLL * scale,
                    WARP_COMPLETE_SCREEN_SHAKE_JITTER * scale,
                    WARP_COMPLETE_SCREEN_SHAKE_JITTER * scale,
                    WARP_COMPLETE_SCREEN_SHAKE_JITTER * scale,
                    centerWorldPos.x,
                    centerWorldPos.y,
                    centerWorldPos.z
            );
            RPLNetwork.sendToClientPlayer(new ClientboundShakeScreenPacket(effect), player);
        }
    }

    private static Vec3 getWarpStartWorldPos(ServerSubLevel subLevel) {
        Vec3 structureCenter = ServerShipUtils.getStructureCenterWorld(subLevel);
        if (structureCenter != null) {
            return structureCenter;
        }

        // Function: fall back to the sublevel pose only when bounds are unavailable.
        var posePosition = subLevel.logicalPose().position();
        return new Vec3(posePosition.x(), posePosition.y(), posePosition.z());
    }

    private static Vec3 getWarpTargetWorldPos(ControlSeatServerData data) {
        return new Vec3(
                data.warpTargetPos.getX() + 0.5D,
                data.warpTargetPos.getY() + 0.5D,
                data.warpTargetPos.getZ() + 0.5D
        );
    }

    private static ControlSeatBlockEntity getControlSeatBlockEntity(ControlSeatServerData data) {
        if (data.level == null || data.controlSeatPos == null) {
            return null;
        }
        if (data.level.getBlockEntity(data.controlSeatPos) instanceof ControlSeatBlockEntity controlSeat) {
            return controlSeat;
        }
        return null;
    }

    private static void syncWarpPreparationState(ControlSeatServerData data) {
        ControlSeatBlockEntity controlSeat = getControlSeatBlockEntity(data);
        if (controlSeat != null) {
            controlSeat.setChanged();
        }
    }

    private static double dotWorldVector(Vector3d vector, Vec3 axis) {
        return vector.x * axis.x + vector.y * axis.y + vector.z * axis.z;
    }

    private static boolean isFiniteVector(Vector3d vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static boolean isUsableAxis(Vec3 axis) {
        return axis != null
                && Double.isFinite(axis.x)
                && Double.isFinite(axis.y)
                && Double.isFinite(axis.z)
                && axis.lengthSqr() >= AXIS_EPSILON;
    }

    private static double angleBetweenNormalized(Vec3 from, Vec3 to) {
        return Math.acos(Mth.clamp(from.dot(to), -1.0D, 1.0D));
    }

    private static Vec3 axisToRotate(Vec3 from, Vec3 to, Vec3 fallbackAxis, double angleRadians) {
        if (angleRadians <= 0.0D) {
            return Vec3.ZERO;
        }

        Vec3 axis = from.cross(to);
        if (axis.lengthSqr() < AXIS_EPSILON) {
            axis = from.dot(to) < 0.0D && isUsableAxis(fallbackAxis) ? fallbackAxis : Vec3.ZERO;
        }
        return scaleUsableAxis(axis, angleRadians);
    }

    private static Vec3 scaleUsableAxis(Vec3 axis, double scale) {
        if (!isUsableAxis(axis) || scale <= 0.0D) {
            return Vec3.ZERO;
        }
        return axis.normalize().scale(scale);
    }

    private static Vec3 rejectAlongAxis(Vec3 vector, Vec3 axis) {
        if (!isUsableAxis(vector) || !isUsableAxis(axis)) {
            return Vec3.ZERO;
        }
        Vec3 normalizedAxis = axis.normalize();
        return vector.subtract(normalizedAxis.scale(vector.dot(normalizedAxis)));
    }

    private static Vec3 projectWorldUpToForwardPlane(Vec3 forward) {
        return rejectAlongAxis(WORLD_UP, forward);
    }

    private static double signedAngleAroundAxis(Vec3 from, Vec3 to, Vec3 axis) {
        if (!isUsableAxis(from) || !isUsableAxis(to) || !isUsableAxis(axis)) {
            return 0.0D;
        }
        Vec3 normalizedAxis = axis.normalize();
        Vec3 fromNormalized = from.normalize();
        Vec3 toNormalized = to.normalize();
        double dot = Mth.clamp(fromNormalized.dot(toNormalized), -1.0D, 1.0D);
        double angle = Math.acos(dot);
        double sign = Math.signum(normalizedAxis.dot(fromNormalized.cross(toNormalized)));
        return sign == 0.0D ? angle : angle * sign;
    }

    private static Vec3 clampVectorLength(Vec3 vector, double maxLength) {
        if (!isUsableAxis(vector) || maxLength <= 0.0D) {
            return Vec3.ZERO;
        }

        double length = vector.length();
        return length <= maxLength ? vector : vector.scale(maxLength / length);
    }

}
