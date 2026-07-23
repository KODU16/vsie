package com.kodu16.vsie.content.controlseat.server;

import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3dc;
import org.joml.Vector3d;

public final class AutoLevelUtils {
    private static final double AXIS_EPSILON = 1.0E-8D;
    private static final double MIN_VALID_MASS = 1.0D;
    private static final double AUTO_LEVEL_ALIGNMENT_TORQUE_SCALE = 2.0D;
    private static final double AUTO_LEVEL_ANGULAR_DAMPING = 0.70D;
    private static final double AUTO_LEVEL_NEAR_TARGET_ANGULAR_DAMPING = 1.20D;
    private static final double AUTO_LEVEL_FULL_TORQUE_ANGLE_RADIANS = Math.toRadians(30.0D);
    private static final double AUTO_LEVEL_NEAR_TARGET_ANGLE_RADIANS = Math.toRadians(10.0D);
    private static final double AUTO_LEVEL_SETTLE_ANGLE_RADIANS = Math.toRadians(0.35D);
    private static final double AUTO_LEVEL_SETTLE_ANGULAR_SPEED = 0.015D;
    private static final double AUTO_LEVEL_REFERENCE_MASS = 256.0D;
    private static final double AUTO_LEVEL_MIN_ALIGNMENT_GAIN = 0.25D;
    private static final double AUTO_LEVEL_HEAVY_MASS_DAMPING_MULTIPLIER = 1.35D;
    private static final double AUTO_LEVEL_MAX_CONTROL = 0.60D;
    private static final double AUTO_LEVEL_NEAR_TARGET_MAX_CONTROL = 0.20D;
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);

    private AutoLevelUtils() {
    }

    public static Vec3 calculateWorldAngularImpulse(
            ControlSeatServerData data,
            ServerSubLevel subLevel,
            Matrix3dc inertia,
            Vector3d omega,
            Vec3 worldForward,
            Vec3 worldUp,
            Vec3 worldRight,
            double rawAverageInertia,
            double effectiveAverageInertia,
            double deltaOmegaScale
    ) {
        if (!isEffective(data) || deltaOmegaScale <= 0.0D) {
            return Vec3.ZERO;
        }

        Vec3 currentUp = worldUp.normalize();
        Vec3 rotationAxisWorld = currentUp.cross(WORLD_UP);
        double alignment = Mth.clamp(currentUp.dot(WORLD_UP), -1.0D, 1.0D);
        if (rotationAxisWorld.lengthSqr() < AXIS_EPSILON) {
            rotationAxisWorld = alignment < 0.0D ? horizontalFallbackAxis(worldForward, worldRight) : Vec3.ZERO;
        }
        if (rotationAxisWorld.lengthSqr() < AXIS_EPSILON) {
            return Vec3.ZERO;
        }

        double angleRadians = Math.acos(alignment);
        Vec3 horizontalOmega = rejectAxisComponent(new Vec3(omega.x, omega.y, omega.z), WORLD_UP);
        if (angleRadians <= AUTO_LEVEL_SETTLE_ANGLE_RADIANS && horizontalOmega.length() <= AUTO_LEVEL_SETTLE_ANGULAR_SPEED) {
            // Function: the rest zone checks only non-yaw spin so auto-level never fights deliberate horizontal heading.
            return Vec3.ZERO;
        }

        double nearTargetBlend = 1.0D - Mth.clamp(angleRadians / AUTO_LEVEL_NEAR_TARGET_ANGLE_RADIANS, 0.0D, 1.0D);
        double massAlignmentGain = calculateMassAlignmentGain(subLevel);
        double dampingGain = Mth.lerp(nearTargetBlend, AUTO_LEVEL_ANGULAR_DAMPING, AUTO_LEVEL_NEAR_TARGET_ANGULAR_DAMPING)
                * Mth.lerp(massAlignmentGain, AUTO_LEVEL_HEAVY_MASS_DAMPING_MULTIPLIER, 1.0D);
        double controlLimit = Mth.lerp(nearTargetBlend, AUTO_LEVEL_MAX_CONTROL, AUTO_LEVEL_NEAR_TARGET_MAX_CONTROL);

        Vec3 worldDeltaOmega = rotationAxisWorld.normalize()
                .scale(Mth.clamp(angleRadians / AUTO_LEVEL_FULL_TORQUE_ANGLE_RADIANS, 0.0D, 1.0D)
                        * AUTO_LEVEL_ALIGNMENT_TORQUE_SCALE
                        * massAlignmentGain)
                .subtract(horizontalOmega.scale(dampingGain));
        worldDeltaOmega = rejectAxisComponent(clampVectorLength(worldDeltaOmega, controlLimit), WORLD_UP);
        return rejectAxisComponent(calculateWorldAngularImpulseForWorldDeltaOmega(
                subLevel,
                inertia,
                new Vector3d(worldDeltaOmega.x, worldDeltaOmega.y, worldDeltaOmega.z).mul(deltaOmegaScale),
                rawAverageInertia,
                effectiveAverageInertia
        ), WORLD_UP);
    }

    public static boolean isEffective(ControlSeatServerData data) {
        return data.isAutoLevelOn && !data.isWarpPreparing && !data.hasPendingWarpTeleport;
    }

    private static double calculateMassAlignmentGain(ServerSubLevel subLevel) {
        MassData massData = subLevel.getMassTracker();
        double mass = massData == null || massData.isInvalid()
                ? MIN_VALID_MASS
                : Math.max(massData.getMass(), MIN_VALID_MASS);
        double lightMassGain = Mth.clamp(Math.sqrt(mass / AUTO_LEVEL_REFERENCE_MASS), AUTO_LEVEL_MIN_ALIGNMENT_GAIN, 1.0D);
        double heavyMassGain = Mth.clamp(Math.sqrt(AUTO_LEVEL_REFERENCE_MASS / mass), AUTO_LEVEL_MIN_ALIGNMENT_GAIN, 1.0D);
        return lightMassGain * heavyMassGain;
    }

    private static Vec3 calculateWorldAngularImpulseForWorldDeltaOmega(
            ServerSubLevel subLevel,
            Matrix3dc inertia,
            Vector3d worldDeltaOmega,
            double rawAverageInertia,
            double effectiveAverageInertia
    ) {
        Vector3d localDeltaOmega = new Vector3d(worldDeltaOmega);
        subLevel.logicalPose().orientation().transformInverse(localDeltaOmega);
        if (!isFiniteVector(localDeltaOmega)) {
            return Vec3.ZERO;
        }

        Vector3d localAngularImpulse = transformWithConservativeInertia(
                inertia,
                localDeltaOmega,
                rawAverageInertia,
                effectiveAverageInertia
        );
        Vector3d worldAngularImpulse = new Vector3d(localAngularImpulse);
        subLevel.logicalPose().orientation().transform(worldAngularImpulse);
        return isFiniteVector(worldAngularImpulse)
                ? new Vec3(worldAngularImpulse.x, worldAngularImpulse.y, worldAngularImpulse.z)
                : Vec3.ZERO;
    }

    private static Vector3d transformWithConservativeInertia(
            Matrix3dc inertia,
            Vector3d localDeltaOmega,
            double rawAverageInertia,
            double effectiveAverageInertia
    ) {
        Vector3d angularImpulse = new Vector3d(localDeltaOmega);
        inertia.transform(angularImpulse);
        if (!isFiniteVector(angularImpulse) || !Double.isFinite(rawAverageInertia) || rawAverageInertia <= AXIS_EPSILON) {
            return new Vector3d();
        }

        return angularImpulse.mul(effectiveAverageInertia / rawAverageInertia);
    }

    private static Vec3 horizontalFallbackAxis(Vec3 worldForward, Vec3 worldRight) {
        Vec3 forwardAxis = rejectAxisComponent(worldForward, WORLD_UP);
        if (isUsableAxis(forwardAxis)) {
            return forwardAxis.normalize();
        }

        Vec3 rightAxis = rejectAxisComponent(worldRight, WORLD_UP);
        if (isUsableAxis(rightAxis)) {
            return rightAxis.normalize();
        }
        return new Vec3(1.0D, 0.0D, 0.0D);
    }

    private static Vec3 clampVectorLength(Vec3 vector, double maxLength) {
        if (!isUsableAxis(vector) || maxLength <= 0.0D) {
            return Vec3.ZERO;
        }

        double length = vector.length();
        return length <= maxLength ? vector : vector.scale(maxLength / length);
    }

    private static Vec3 rejectAxisComponent(Vec3 vector, Vec3 axis) {
        if (!isUsableAxis(axis) || vector.lengthSqr() <= AXIS_EPSILON) {
            return vector;
        }

        Vec3 normalizedAxis = axis.normalize();
        return vector.subtract(normalizedAxis.scale(vector.dot(normalizedAxis)));
    }

    private static boolean isUsableAxis(Vec3 axis) {
        return axis != null
                && Double.isFinite(axis.x)
                && Double.isFinite(axis.y)
                && Double.isFinite(axis.z)
                && axis.lengthSqr() >= AXIS_EPSILON;
    }

    private static boolean isFiniteVector(Vector3d vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }
}
