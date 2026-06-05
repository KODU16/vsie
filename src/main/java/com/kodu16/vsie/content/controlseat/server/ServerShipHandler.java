package com.kodu16.vsie.content.controlseat.server;


import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.controlseat.functions.ScanNearByShips;
import com.kodu16.vsie.content.warpprojectile.WarpProjecTileEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatInputS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatStatusS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.NearbyShipsS2CPacket;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3dc;
import com.kodu16.vsie.registries.vsieEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.kodu16.vsie.registries.ModNetworking;

import org.slf4j.Logger;
import rbasamoyai.ritchiesprojectilelib.effects.screen_shake.ScreenShakeEffect;
import rbasamoyai.ritchiesprojectilelib.network.ClientboundShakeScreenPacket;
import rbasamoyai.ritchiesprojectilelib.network.RPLNetwork;

import javax.annotation.Nullable;


public class ServerShipHandler {
    private static final double KEY_CONTROL_THRUST_EQUIVALENT = 0.50D;
    private static final double KEYBOARD_TORQUE_AXIS_SCALE_AT_TEN_PERCENT = 0.02D;
    private static final double FLIGHT_ASSIST_LINEAR_RESPONSE = 0.60D;
    private static final double FLIGHT_ASSIST_ANGULAR_RESPONSE = 0.45D;
    private static final double ANTI_GRAVITY_VERTICAL_RESPONSE = 0.80D;
    private static final double FLIGHT_ASSIST_LINEAR_THRUST_FRACTION = 0.25D;
    private static final double FLIGHT_ASSIST_ANGULAR_THRUST_FRACTION = 0.18D;
    private static final double ANTI_GRAVITY_DAMPING_THRUST_FRACTION = 0.25D;
    private static final double CONTROL_FORCE_SCALE = 0.25D;
    private static final double CONTROL_TORQUE_SCALE = 0.12D;
    private static final double LINEAR_REFERENCE_SPEED = 10.0D;
    private static final double ANGULAR_REFERENCE_SPEED = 1.5D;
    private static final double ANGULAR_ASSIST_REST_SPEED = 0.02D;
    private static final double MASS_PROPERTY_RESPONSE = 2.0D;
    private static final double MIN_VALID_MASS = 1.0D;
    private static final double MIN_VALID_INERTIA = 1.0D;
    private static final double CONTROL_INPUT_RESPONSE = 14.0D;
    private static final double THROTTLE_INPUT_RESPONSE = 8.0D;
    private static final double FREE_FALL_GRAVITY_IMPULSE_SCALE = 1.0D;
    private static final double STANDARD_GRAVITY = 9.60665D;
    private static final double AXIS_EPSILON = 1.0E-8D;
    private static final double WARP_ALIGNMENT_THRESHOLD_DEGREES = 1.0D;
    private static final double WARP_ALIGNMENT_TORQUE_SCALE = 2.0D;
    private static final double AUTO_LEVEL_ALIGNMENT_TORQUE_SCALE = 2.0D;
    private static final double AUTO_LEVEL_ANGULAR_DAMPING = 0.70D;
    private static final double AUTO_LEVEL_NEAR_TARGET_ANGULAR_DAMPING = 1.20D;
    private static final double AUTO_LEVEL_FULL_TORQUE_ANGLE_RADIANS = Math.toRadians(30.0D);
    private static final double AUTO_LEVEL_NEAR_TARGET_ANGLE_RADIANS = Math.toRadians(10.0D);
    private static final double AUTO_LEVEL_SETTLE_ANGLE_RADIANS = Math.toRadians(2.0D);
    private static final double AUTO_LEVEL_SETTLE_ANGULAR_SPEED = 0.05D;
    private static final double AUTO_LEVEL_REFERENCE_MASS = 256.0D;
    private static final double AUTO_LEVEL_MIN_ALIGNMENT_GAIN = 0.25D;
    private static final double AUTO_LEVEL_HEAVY_MASS_DAMPING_MULTIPLIER = 1.35D;
    private static final double AUTO_LEVEL_MAX_CONTROL = 0.60D;
    private static final double AUTO_LEVEL_NEAR_TARGET_MAX_CONTROL = 0.20D;
    private static final double WARP_PROJECTILE_DISTANCE_SCALE = 1.5D;
    private static final int WARP_TELEPORT_EXTRA_DELAY_TICKS = 100;
    private static final int WARP_COMPLETE_SCREEN_SHAKE_TICKS = 10;
    private static final double WARP_COMPLETE_SCREEN_SHAKE_RADIUS = 96.0D;
    private static final float WARP_COMPLETE_SCREEN_SHAKE_YAW = 5F;
    private static final float WARP_COMPLETE_SCREEN_SHAKE_PITCH = 5F;
    private static final float WARP_COMPLETE_SCREEN_SHAKE_ROLL = 0.25F;
    private static final float WARP_COMPLETE_SCREEN_SHAKE_JITTER = 1F;
    private ControlSeatServerData data;
    public static final Logger LOGGER = LogUtils.getLogger();

    public ServerShipHandler(ControlSeatServerData data){
        this.data = data;
    }

    // Function: expose the keyboard-only control authority so A/D strafing and unlocked-view torque keys stay tunable from one place.
    public static double getKeyControlThrustEquivalent() {
        return KEY_CONTROL_THRUST_EQUIVALENT;
    }

    public static double getTranslationThrottleEquivalent() {
        return KEY_CONTROL_THRUST_EQUIVALENT / CONTROL_FORCE_SCALE;
    }

    public static float getKeyboardTorqueAxisScale() {
        return (float) (KEYBOARD_TORQUE_AXIS_SCALE_AT_TEN_PERCENT * (KEY_CONTROL_THRUST_EQUIVALENT / 0.10D));
    }

    public void resetControlInput() {
        clearManualControlInput();
        hasPreviousMotionSample = false;
        data.setFinaltorque(new Vector3d());
        data.setFinalforce(new Vector3d());
        data.setThrusterVisualForce(new Vector3d());
    }

    public void clearManualControlInput() {
        smoothedControlTorque.set(0.0D, 0.0D, 0.0D);
        smoothedTranslationInput.set(0.0D, 0.0D, 0.0D);
        smoothedThrottle = 0.0D;
    }

    private long lastSendMs = 0;
    private long lastSendStatusMs = 0;
    private long lastSendInputMs = 0;
    private long lastScanShipsMs = 0;
    int lastSentEncode = 0;
    int current=0;
    private volatile Vec3 worldXDirection;
    private volatile Vec3 worldYDirection;
    private volatile Vec3 worldZDirection;
    private final Vector3d smoothedControlTorque = new Vector3d();
    // Function: smooth unlocked WASD translation so force does not step sharply when keys change.
    private final Vector3d smoothedTranslationInput = new Vector3d();
    private double smoothedThrottle = 0.0D;
    private double smoothedMass = Double.NaN;
    private double smoothedAverageInertia = Double.NaN;
    private final Vector3d previousVelocity = new Vector3d();
    private final Vector3d previousOmega = new Vector3d();
    private boolean hasPreviousMotionSample = false;

    public void getandsendshipdata(ServerSubLevel subLevel,BlockPos pos) {
        if (data.getDirectionForward() == null || data.getDirectionUp() == null || data.getDirectionRight() == null) {
            return;
        }

        Vec3 ForwardDirection = transformSeatAxis(subLevel, data.getDirectionForward());
        Vec3 UpDirection = transformSeatAxis(subLevel, data.getDirectionUp());
        Vec3 RightDirection = transformSeatAxis(subLevel, data.getDirectionRight());
        Level level = data.level;
        updateStructureCenterTelemetry(subLevel);
        long now = System.currentTimeMillis();
        if (now - lastScanShipsMs > 500) {
            lastScanShipsMs = now;
            // Function: automatic heavy turrets still need fresh enemy ship targets when no player is seated.
            refreshNearbyShips(pos, level);
            if (data.getPlayer() != null) {
                ModNetworking.sendToPlayer(new NearbyShipsS2CPacket(data.shipsData), (ServerPlayer) data.getPlayer());
            }
        }
        if (data.getPlayer() != null) {
            if (now - lastSendMs > 50) {
                lastSendMs = now;
                ControlSeatS2CPacket packet = new ControlSeatS2CPacket(
                        pos,
                        Vec.toVector3d(ForwardDirection),
                        Vec.toVector3d(UpDirection),
                        data.enemy,
                        data.ally,
                        data.lockedenemyslug,
                        data.getThrottle(),
                        data.isviewlocked,
                        data.shipSpeed,
                        new Vector3d(data.structureCenterWorld),
                        data.seatGForce
                );
                ModNetworking.sendToPlayer(packet, (ServerPlayer) data.getPlayer());
            }

            if(now - lastSendStatusMs > 250) {
                lastSendStatusMs = now;
                boolean shieldOverloaded = data.isshieldon && data.shieldcooldowntime > 0.0D;
                ControlSeatStatusS2CPacket packetstatus = new ControlSeatStatusS2CPacket(pos,
                        data.avalibleenergy,data.totalenergystorage,
                        data.avaliblefuel,data.totalfuelstorage,
                        data.avalibleE710, data.warpE710CostMb, data.warpE710Insufficient,
                        data.isshieldon, (int) data.avalibleshield, (int) data.totalshield, shieldOverloaded,
                        data.isforceassiston, data.istorqueassiston, data.isForceAssistSuppressedByAccelerator,
                        data.isantigravityon, data.isAutoLevelOn,
                        data.isWarpPreparing, data.hasPendingWarpTeleport, data.warpTargetName,
                        data.activeWeaponHudInfos);
                //LogUtils.getLogger().warn("shieldtotal:"+data.totalshield+"avalible:"+data.avalibleshield);
                ModNetworking.sendToPlayer(packetstatus, (ServerPlayer) data.getPlayer());
            }

            if(now - lastSendInputMs > 250) {

                lastSendInputMs = now;
                ControlSeatInputS2CPacket packet = new ControlSeatInputS2CPacket(pos, data.channelencode);
                ModNetworking.sendToPlayer(packet, (ServerPlayer) data.getPlayer());
            }
        }
    }

    private void refreshNearbyShips(BlockPos pos, Level level) {
        if (level == null) {
            data.shipsData.clear();
            data.enemyshipsData.clear();
            data.lockedenemyslug = "";
            data.lockedEnemySubLevel = null;
            data.lockedenemyindex = 0;
            return;
        }


        data.shipsData = ScanNearByShips.withEnemyTargetIndexes(
                ScanNearByShips.scanships(null, pos, level),
                data.enemy,
                data.ally
        );
        data.enemyshipsData = ScanNearByShips.scanenemyships(null, pos, level, data.enemy, data.ally);
        if (data.enemyshipsData.isEmpty()) {
            data.lockedenemyindex = 0;
            data.lockedenemyslug = "";
            data.lockedEnemySubLevel = null;
            return;
        }

        data.lockedenemyindex = Math.floorMod(data.lockedenemyindex, data.enemyshipsData.size());
        data.lockedenemyslug = ScanNearByShips.lockedEnemySlug(data.shipsData, data.lockedenemyindex);
        data.lockedEnemySubLevel = ScanNearByShips.scanEnemySubLevelByIndex(
                null,
                pos,
                level,
                data.enemy,
                data.ally,
                data.lockedenemyindex
        );
    }

    public void applyForceAndTorque(ServerSubLevel subLevel, BlockPos pos, double timeStep) {
        processPendingWarpTeleport(subLevel);
        boolean hasControlAxes = data.getDirectionForward() != null && data.getDirectionUp() != null && data.getDirectionRight() != null;

        Player player = data.getPlayer();
        boolean controlling = true;
        if (player == null || !player.isAlive() || player.isRemoved()) {
            data.clearSeatOccupantState();
            clearManualControlInput();
            controlling = false;
        }
        Entity vehicle = null;
        if (player != null) {
            vehicle = player.getVehicle();
        }
        if (!(vehicle instanceof ControlSeatMountEntity)) {
            data.clearSeatOccupantState();
            clearManualControlInput();
            controlling = false;
        }
        if (controlling && !hasControlAxes) {
            resetControlInput();
            return;
        }

        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid()) {
            resetControlInput();
            return;
        }
        double rawMass = massData.getMass();
        Matrix3dc momentOfInertia = massData.getInertiaTensor();
        double rawAverageInertia = averageInertia(momentOfInertia);
        if (!isUsableMassProperties(rawMass, momentOfInertia, rawAverageInertia)) {
            resetControlInput();
            return;
        }
        updateSmoothedMassProperties(rawMass, rawAverageInertia, timeStep);
        double mass = conservativeMass(rawMass);
        double averageInertia = conservativeAverageInertia(rawAverageInertia);

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || !handle.isValid()) {
            resetControlInput();
            return;
        }
        Vector3d omega = handle.getAngularVelocity(new Vector3d());
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        if (!isFiniteVector(omega) || !isFiniteVector(velocity)) {
            resetControlInput();
            return;
        }

        double totalForceThrust = Math.max(0.0D, data.thruster_force_strength);
        double totalTorqueThrust = Math.max(0.0D, data.thruster_torque_strength);
        if (totalForceThrust <= AXIS_EPSILON && totalTorqueThrust <= AXIS_EPSILON) {
            // Function: no available fueled thruster authority means no ship force or torque, including assists.
            resetControlInput();
            return;
        }
        double linearDampingAlpha = authorityDampingAlpha(
                totalForceThrust * FLIGHT_ASSIST_LINEAR_THRUST_FRACTION,
                mass,
                velocity.length(),
                LINEAR_REFERENCE_SPEED,
                FLIGHT_ASSIST_LINEAR_RESPONSE,
                timeStep
        );
        Vector3d invforce = velocity.negate(new Vector3d()).mul(mass * linearDampingAlpha);

        double angularTorqueAuthority = angularTorqueAuthority(totalTorqueThrust, mass, averageInertia);
        double angularDampingAlpha = authorityDampingAlpha(
                angularTorqueAuthority * FLIGHT_ASSIST_ANGULAR_THRUST_FRACTION,
                averageInertia,
                omega.length(),
                ANGULAR_REFERENCE_SPEED,
                FLIGHT_ASSIST_ANGULAR_RESPONSE,
                timeStep
        );
        Vector3d invtorque = calculateFlightAssistTorque(
                subLevel,
                momentOfInertia,
                omega,
                angularDampingAlpha,
                rawAverageInertia,
                averageInertia
        );

        double deltaOmegaScale = averageInertia > AXIS_EPSILON
                ? (angularTorqueAuthority / averageInertia) * CONTROL_TORQUE_SCALE * timeStep
                : 0.0D;
        boolean hasWorldControlAxes = updateWorldControlAxes(subLevel);
        Vector3d finaltorque = new Vector3d(0,0,0);
        Vector3d finalforce  = new Vector3d(0,0,0);

        if (data.istorqueassiston) {
            finaltorque.add(invtorque);
        }
        if (data.isforceassiston) {
            finalforce.add(invforce);
        }
        if (data.isantigravityon) {
            Vector3d gravity = DimensionPhysicsData.getGravity(
                    subLevel.getLevel(),
                    subLevel.logicalPose().position(),
                    new Vector3d()
            );
            double gravityLength = gravity.length();
            if (gravityLength > 1.0E-6D) {
                Vector3d gravityDirection = gravity.normalize(new Vector3d());
                // Function: use the same conservative mass basis as damping so anti-gravity does not accumulate a net upward bias.
                finalforce.fma(-mass * timeStep * FREE_FALL_GRAVITY_IMPULSE_SCALE, gravity);

                double verticalVelocity = velocity.dot(gravityDirection);
                double verticalDampingAlpha = authorityDampingAlpha(
                        totalForceThrust * ANTI_GRAVITY_DAMPING_THRUST_FRACTION,
                        mass,
                        Math.abs(verticalVelocity),
                        LINEAR_REFERENCE_SPEED,
                        ANTI_GRAVITY_VERTICAL_RESPONSE,
                        timeStep
                );
                finalforce.fma(-mass * verticalVelocity * verticalDampingAlpha, gravityDirection);
            }
        }
        if (data.isAutoLevelOn && hasWorldControlAxes && deltaOmegaScale > 0.0D) {
            // Function: auto-level adds pitch/roll correction even when the seat is empty.
            Vec3 autoLevelTorque = calculateAutoLevelTorque(subLevel, omega);
            Vector3d autoLevelDeltaOmega = Vec.toVector3d(autoLevelTorque).mul(deltaOmegaScale);
            Vec3 autoLevelImpulse = calculateWorldAngularImpulseForControl(
                    subLevel,
                    momentOfInertia,
                    autoLevelDeltaOmega,
                    rawAverageInertia,
                    averageInertia
            );
            finaltorque.add(Vec.toVector3d(autoLevelImpulse));
        }
        Vector3d thrusterVisualForce = calculateVisualForceFromPhysicsForce(subLevel, finalforce);

        double torqueAlpha = smoothingAlpha(CONTROL_INPUT_RESPONSE, timeStep);
        double throttleAlpha = smoothingAlpha(THROTTLE_INPUT_RESPONSE, timeStep);

        if (controlling) {
            boolean warpRotationLocked = data.isWarpPreparing || data.hasPendingWarpTeleport;
            // Function: while warp is active, mouse torque must not rotate the ship; preparation uses auto-alignment only.
            Vec3 torque = warpRotationLocked ? Vec3.ZERO : data.getTorque();
            if (!hasWorldControlAxes) {
                resetControlInput();
                return;
            }

            Vec3 steeringTorque = data.isWarpPreparing ? calculateWarpPreparationTorque(subLevel,pos) : torque;
            Vec3 translationInput = data.getForce();
            if (data.hasPendingWarpTeleport && !data.isWarpPreparing) {
                smoothedControlTorque.set(0.0D, 0.0D, 0.0D);
            } else {
                smoothVector(smoothedControlTorque, steeringTorque.x, steeringTorque.y, steeringTorque.z, torqueAlpha);
            }
            smoothVector(smoothedTranslationInput, translationInput.x, translationInput.y, translationInput.z, torqueAlpha);
            smoothedThrottle += ((data.getThrottle() / 100.0D) - smoothedThrottle) * throttleAlpha;
            if (data.isAutoLevelOn) {
                // Function: stale roll/pitch smoothing must not bleed through after auto-level takes over leveling axes.
                smoothedControlTorque.x = 0.0D;
                smoothedControlTorque.z = 0.0D;
            }
            Vector3d controlDeltaOmega = new Vector3d(smoothedControlTorque).mul(deltaOmegaScale);

            if (data.isWarpPreparing) {

                LogUtils.getLogger().warn("preparing warp...");
                tryLaunchWarpProjectile(subLevel);
            }

            Vec3 Invarianttorque = calculateWorldAngularImpulseForControl(
                    subLevel,
                    momentOfInertia,
                    controlDeltaOmega,
                    rawAverageInertia,
                    averageInertia
            );
            // Function: physics keeps the original seat-forward throttle sign.
            double forcescale = -smoothedThrottle * totalForceThrust * CONTROL_FORCE_SCALE * timeStep;
            Vec3 Invariantforce = new Vec3(worldXDirection.x * forcescale, worldXDirection.y * forcescale, worldXDirection.z * forcescale);
            Vec3 visualThrottleForce = calculateVisualThrottleForce(subLevel);
            double translationForceScale = totalForceThrust * CONTROL_FORCE_SCALE * getTranslationThrottleEquivalent() * timeStep;
            Vec3 translationForce = calculateWorldTorque(new Vector3d(smoothedTranslationInput).mul(translationForceScale), worldXDirection, worldYDirection, worldZDirection);
            Vec3 visualTranslationForce = calculateVisualTranslationForce(subLevel);


            if (Double.isNaN(torque.x()) || Double.isNaN(torque.y()) || Double.isNaN(torque.z())
                    || Double.isNaN(translationInput.x()) || Double.isNaN(translationInput.y()) || Double.isNaN(translationInput.z())) {
                return;
            }
            finaltorque.add(Vec.toVector3d(Invarianttorque));
            finalforce.add(Vec.toVector3d(Invariantforce));
            finalforce.add(Vec.toVector3d(translationForce));
            thrusterVisualForce.add(Vec.toVector3d(visualThrottleForce));
            thrusterVisualForce.add(Vec.toVector3d(visualTranslationForce));
            //LogUtils.getLogger().warn("finaltorque:"+finaltorque+"inverttorque:"+invtorque+"origin:"+Invarianttorque);
        } else {
            // Function: when the pilot leaves, keep assist damping active and only clear stale manual input.
            clearManualControlInput();
        }
        data.setFinaltorque(finaltorque);
        data.setFinalforce(finalforce);
        data.setThrusterVisualForce(thrusterVisualForce);
        updateMotionTelemetry(subLevel, pos, velocity, omega, timeStep, rawMass, finalforce);

        ServerShipUtils.applyWorldForceAndTorqueAtCenterOfMass(subLevel,finalforce,finaltorque);
    }


    private static double smoothingAlpha(double response, double timeStep) {
        return Mth.clamp(1.0D - Math.exp(-response * timeStep), 0.0D, 1.0D);
    }

    private static double authorityDampingAlpha(double authority, double inertia, double speed, double referenceSpeed, double response, double timeStep) {
        if (authority <= AXIS_EPSILON || inertia <= AXIS_EPSILON || timeStep <= 0.0D) {
            return 0.0D;
        }

        double effectiveSpeed = Math.sqrt(speed * speed + referenceSpeed * referenceSpeed);
        double dampingRate = (authority / inertia) * response / effectiveSpeed;
        return smoothingAlpha(dampingRate, timeStep);
    }

    private static double averageInertia(Matrix3dc inertia) {
        if (inertia == null) {
            return AXIS_EPSILON;
        }

        return Math.max((Math.abs(inertia.m00()) + Math.abs(inertia.m11()) + Math.abs(inertia.m22())) / 3.0D, AXIS_EPSILON);
    }

    private static boolean isUsableMassProperties(double mass, Matrix3dc inertia, double averageInertia) {
        return Double.isFinite(mass)
                && mass >= MIN_VALID_MASS
                && inertia != null
                && isFiniteMatrix(inertia)
                && inertia.m00() > AXIS_EPSILON
                && inertia.m11() > AXIS_EPSILON
                && inertia.m22() > AXIS_EPSILON
                && Double.isFinite(averageInertia)
                && averageInertia >= MIN_VALID_INERTIA;
    }

    private static boolean isFiniteMatrix(Matrix3dc matrix) {
        return Double.isFinite(matrix.m00()) && Double.isFinite(matrix.m01()) && Double.isFinite(matrix.m02())
                && Double.isFinite(matrix.m10()) && Double.isFinite(matrix.m11()) && Double.isFinite(matrix.m12())
                && Double.isFinite(matrix.m20()) && Double.isFinite(matrix.m21()) && Double.isFinite(matrix.m22());
    }

    private static boolean isFiniteVector(Vector3d vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private void updateStructureCenterTelemetry(ServerSubLevel subLevel) {
        Vec3 center = ServerShipUtils.getStructureCenterWorld(subLevel);
        data.structureCenterWorld = center == null
                ? new Vector3d()
                : new Vector3d(center.x, center.y, center.z);
    }

    private void updateMotionTelemetry(ServerSubLevel subLevel, BlockPos seatPos, Vector3d velocity, Vector3d omega, double timeStep, double mass, Vector3d appliedLinearImpulse) {
        data.shipSpeed = velocity.length();
        if (timeStep <= AXIS_EPSILON || mass <= AXIS_EPSILON) {
            data.seatGForce = 0.0D;
            return;
        }

        Vector3d linearAcceleration = new Vector3d();
        if (isFiniteVector(appliedLinearImpulse)) {
            // Function: HUD G uses the current non-gravity ship impulse so natural gravity alone does not produce G load.
            linearAcceleration.set(appliedLinearImpulse).div(mass * timeStep);
        }

        Vector3d angularAcceleration = new Vector3d();
        if (!hasPreviousMotionSample) {
            previousVelocity.set(velocity);
            previousOmega.set(omega);
            hasPreviousMotionSample = true;
        } else {
            angularAcceleration.set(omega).sub(previousOmega).div(timeStep);
            previousVelocity.set(velocity);
            previousOmega.set(omega);
        }

        Vec3 centerOfMassWorld = ServerShipUtils.getCenterOfMassWorld(subLevel);
        if (centerOfMassWorld == null) {
            data.seatGForce = Math.max(0.0D, linearAcceleration.length() / STANDARD_GRAVITY);
            return;
        }

        Vector3d seatWorldPos = subLevel.logicalPose().transformPosition(new Vector3d(
                seatPos.getX() + 0.5D,
                seatPos.getY() + 0.5D,
                seatPos.getZ() + 0.5D
        ));
        Vector3d leverArm = seatWorldPos.sub(new Vector3d(centerOfMassWorld.x, centerOfMassWorld.y, centerOfMassWorld.z), new Vector3d());
        Vector3d tangentialAcceleration = new Vector3d(angularAcceleration).cross(leverArm);
        Vector3d centripetalAcceleration = new Vector3d(omega).cross(new Vector3d(omega).cross(leverArm));
        Vector3d seatAcceleration = linearAcceleration.add(tangentialAcceleration).add(centripetalAcceleration);
        data.seatGForce = Math.max(0.0D, seatAcceleration.length() / STANDARD_GRAVITY);
    }

    private Vector3d calculateVisualForceFromPhysicsForce(ServerSubLevel subLevel, Vector3d physicsForce) {
        if (physicsForce == null || physicsForce.lengthSquared() <= AXIS_EPSILON) {
            return new Vector3d();
        }

        // Function: physics impulse sign is opposite of the thrust demand used to choose nozzle flames.
        Vec3 demandDirection = Vec.toVec3(new Vector3d(physicsForce).negate());
        double sameFacingThrust = sameFacingThrustForWorldThrustDirection(subLevel, demandDirection, data.thruster_force_strength);
        double demandRatio = Math.min(1.0D, physicsForce.length() / Math.max(data.thruster_force_strength, AXIS_EPSILON));
        return Vec.toVector3d(demandDirection.normalize().scale(demandRatio * sameFacingThrust));
    }

    private Vec3 calculateVisualThrottleForce(ServerSubLevel subLevel) {
        if (Math.abs(smoothedThrottle) <= AXIS_EPSILON) {
            return Vec3.ZERO;
        }

        Vec3 demandDirection = smoothedThrottle > 0.0D ? worldXDirection : worldXDirection.scale(-1.0D);
        double sameFacingThrust = sameFacingThrustForWorldThrustDirection(subLevel, demandDirection, data.thruster_force_strength);
        double visualForceScale = Math.abs(smoothedThrottle) * sameFacingThrust;
        // Function: visual force uses thrust-demand units, not physics impulse units, so force contribution scales like throttle.
        return demandDirection.normalize().scale(visualForceScale);
    }

    private Vec3 calculateVisualTranslationForce(ServerSubLevel subLevel) {
        if (smoothedTranslationInput.lengthSquared() <= AXIS_EPSILON) {
            return Vec3.ZERO;
        }

        Vec3 demandDirection = calculateWorldTorque(new Vector3d(smoothedTranslationInput), worldXDirection, worldYDirection, worldZDirection);
        if (demandDirection.lengthSqr() <= AXIS_EPSILON) {
            return Vec3.ZERO;
        }

        double sameFacingThrust = sameFacingThrustForWorldThrustDirection(subLevel, demandDirection, data.thruster_force_strength);
        double visualForceScale = Math.min(1.0D, smoothedTranslationInput.length()) * getTranslationThrottleEquivalent() * sameFacingThrust;
        // Function: locked-view translation renders at its 10-percent throttle equivalent without the physics timestep scale.
        return demandDirection.normalize().scale(visualForceScale);
    }

    private double sameFacingThrustForWorldThrustDirection(ServerSubLevel subLevel, Vec3 worldThrustDirection, double fallback) {
        if (worldThrustDirection == null || worldThrustDirection.lengthSqr() <= AXIS_EPSILON) {
            return Math.max(fallback, AXIS_EPSILON);
        }

        Vector3d localThrustDirection = Vec.toVector3d(worldThrustDirection.normalize());
        subLevel.logicalPose().orientation().transformInverse(localThrustDirection);
        Direction thrustDirection = dominantDirection(localThrustDirection);
        // Function: thruster block FACING is the nozzle direction, opposite of the produced thrust direction.
        int facingIndex = getFacingThrustIndex(thrustDirection.getOpposite());
        float[] facingMaxThrustSum = data.facingMaxThrustSum;
        if (facingMaxThrustSum != null && facingIndex >= 0 && facingIndex < facingMaxThrustSum.length && facingMaxThrustSum[facingIndex] > AXIS_EPSILON) {
            return facingMaxThrustSum[facingIndex];
        }
        return Math.max(fallback, AXIS_EPSILON);
    }

    private static Direction dominantDirection(Vector3d vector) {
        double absX = Math.abs(vector.x);
        double absY = Math.abs(vector.y);
        double absZ = Math.abs(vector.z);
        if (absX >= absY && absX >= absZ) {
            return vector.x >= 0.0D ? Direction.EAST : Direction.WEST;
        }
        if (absY >= absZ) {
            return vector.y >= 0.0D ? Direction.UP : Direction.DOWN;
        }
        return vector.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private static int getFacingThrustIndex(Direction direction) {
        return switch (direction) {
            case EAST -> 0;
            case SOUTH -> 1;
            case WEST -> 2;
            case NORTH -> 3;
            case UP -> 4;
            case DOWN -> 5;
        };
    }

    private void updateSmoothedMassProperties(double mass, double averageInertia, double timeStep) {
        double alpha = smoothingAlpha(MASS_PROPERTY_RESPONSE, timeStep);
        smoothedMass = smoothPositiveMetric(smoothedMass, mass, alpha);
        smoothedAverageInertia = smoothPositiveMetric(smoothedAverageInertia, averageInertia, alpha);
    }

    private static double smoothPositiveMetric(double current, double target, double alpha) {
        if (!Double.isFinite(current) || current <= 0.0D) {
            return target;
        }

        return current + (target - current) * alpha;
    }

    private double conservativeMass(double rawMass) {
        if (!Double.isFinite(smoothedMass) || smoothedMass <= 0.0D) {
            return rawMass;
        }

        return Math.max(Math.min(rawMass, smoothedMass), MIN_VALID_MASS);
    }

    private double conservativeAverageInertia(double rawAverageInertia) {
        if (!Double.isFinite(smoothedAverageInertia) || smoothedAverageInertia <= 0.0D) {
            return rawAverageInertia;
        }

        return Math.max(Math.min(rawAverageInertia, smoothedAverageInertia), MIN_VALID_INERTIA);
    }

    private static Vector3d transformWithConservativeInertia(Matrix3dc inertia, Vector3d localDeltaOmega, double rawAverageInertia, double effectiveAverageInertia) {
        Vector3d angularImpulse = new Vector3d(localDeltaOmega);
        inertia.transform(angularImpulse);
        if (!isFiniteVector(angularImpulse) || !Double.isFinite(rawAverageInertia) || rawAverageInertia <= AXIS_EPSILON) {
            return new Vector3d();
        }

        return angularImpulse.mul(effectiveAverageInertia / rawAverageInertia);
    }

    private static Vector3d calculateFlightAssistTorque(
            ServerSubLevel subLevel,
            Matrix3dc inertia,
            Vector3d worldOmega,
            double angularDampingAlpha,
            double rawAverageInertia,
            double effectiveAverageInertia
    ) {
        if (angularDampingAlpha <= 0.0D || worldOmega.length() < ANGULAR_ASSIST_REST_SPEED) {
            return new Vector3d();
        }

        Vector3d localOmega = new Vector3d(worldOmega);
        subLevel.logicalPose().orientation().transformInverse(localOmega);
        if (!isFiniteVector(localOmega)) {
            return new Vector3d();
        }

        Vector3d localDeltaOmega = localOmega.negate(new Vector3d()).mul(angularDampingAlpha);
        Vector3d localAngularImpulse = transformWithConservativeInertia(inertia, localDeltaOmega, rawAverageInertia, effectiveAverageInertia);
        Vector3d worldAngularImpulse = new Vector3d(localAngularImpulse);
        subLevel.logicalPose().orientation().transform(worldAngularImpulse);

        return isFiniteVector(worldAngularImpulse) ? worldAngularImpulse : new Vector3d();
    }

    private Vec3 calculateWorldAngularImpulseForControl(
            ServerSubLevel subLevel,
            Matrix3dc inertia,
            Vector3d controlDeltaOmega,
            double rawAverageInertia,
            double effectiveAverageInertia
    ) {
        // Function: mouse/warp input asks for angular velocity around the control-seat axes; inertia maps that to the COM angular impulse.
        Vec3 worldDeltaOmegaVec = calculateWorldTorque(controlDeltaOmega, worldXDirection, worldYDirection, worldZDirection);
        Vector3d localDeltaOmega = Vec.toVector3d(worldDeltaOmegaVec);
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
        return isFiniteVector(worldAngularImpulse) ? Vec.toVec3(worldAngularImpulse) : Vec3.ZERO;
    }

    private static double angularTorqueAuthority(double totalThrust, double mass, double averageInertia) {
        if (totalThrust <= AXIS_EPSILON) {
            return 0.0D;
        }

        double radiusOfGyration = Math.sqrt(Math.max(averageInertia, AXIS_EPSILON) / Math.max(mass, AXIS_EPSILON));
        return totalThrust * Math.max(radiusOfGyration, 0.5D);
    }

    private static void smoothVector(Vector3d current, double targetX, double targetY, double targetZ, double alpha) {
        current.x += (targetX - current.x) * alpha;
        current.y += (targetY - current.y) * alpha;
        current.z += (targetZ - current.z) * alpha;
    }

    private boolean updateWorldControlAxes(ServerSubLevel subLevel) {
        Vec3 rawForward = transformSeatAxis(subLevel, data.getDirectionForward());
        Vec3 rawUp = transformSeatAxis(subLevel, data.getDirectionUp());
        Vec3 rawRight = transformSeatAxis(subLevel, data.getDirectionRight());
        if (!isUsableAxis(rawForward) || !isUsableAxis(rawUp) || !isUsableAxis(rawRight)) {
            return false;
        }

        // Function: build the control basis from the seat's front and right axes so mouse X always maps to yaw, not roll.
        Vec3 forward = rawForward.normalize();
        Vec3 right = rawRight.subtract(forward.scale(rawRight.dot(forward)));
        if (right.lengthSqr() < AXIS_EPSILON) {
            right = forward.cross(rawUp);
        }
        if (right.lengthSqr() < AXIS_EPSILON) {
            return false;
        }
        right = right.normalize();

        Vec3 up = right.cross(forward);
        if (up.lengthSqr() < AXIS_EPSILON) {
            return false;
        }
        up = up.normalize();

        worldXDirection = forward;
        worldYDirection = up;
        worldZDirection = right;
        return true;
    }

    private static Vec3 transformSeatAxis(ServerSubLevel subLevel, Vec3i localAxis) {
        Vector3d worldAxis = new Vector3d(localAxis.getX(), localAxis.getY(), localAxis.getZ());
        // Function: orientation-only conversion keeps direction axes free from normal/scale effects.
        subLevel.logicalPose().orientation().transform(worldAxis);
        return new Vec3(worldAxis.x, worldAxis.y, worldAxis.z);
    }

    private static boolean isUsableAxis(Vec3 axis) {
        return axis != null
                && Double.isFinite(axis.x)
                && Double.isFinite(axis.y)
                && Double.isFinite(axis.z)
                && axis.lengthSqr() >= AXIS_EPSILON;
    }

    private Vec3 calculateWarpPreparationTorque(ServerSubLevel subLevel,BlockPos pos) {
        if (data.warpTargetName == null || data.warpTargetName.isEmpty() || data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return new Vec3(0, 0, 0);
        }

        Vec3 targetDirection = getNormalizedWarpTargetDirection(pos);
        if (targetDirection == null) {
            return new Vec3(0, 0, 0);
        }

        Vec3 currentForward = worldXDirection.normalize();

        Vec3 rotationAxisWorld = targetDirection.cross(currentForward);
        if (rotationAxisWorld.lengthSqr() < 1.0E-6) {
            return new Vec3(0, 0, 0);
        }

        double alignment = Mth.clamp(currentForward.dot(targetDirection), -1.0D, 1.0D);
        double angleStrength = Mth.clamp((1.0D - alignment) * 2.0D, 0.0D, 1.0D);
        rotationAxisWorld.normalize();
        rotationAxisWorld.scale(angleStrength);
        double factor = subLevel.getMassTracker().getMass();

        double localYawTorque = Mth.clamp(rotationAxisWorld.dot(worldYDirection) * WARP_ALIGNMENT_TORQUE_SCALE, -factor, factor);
        double localPitchTorque = Mth.clamp(rotationAxisWorld.dot(worldZDirection) * WARP_ALIGNMENT_TORQUE_SCALE, -factor, factor);
        return new Vec3(0, localYawTorque, localPitchTorque);
    }


    private Vec3 calculateAutoLevelTorque(ServerSubLevel subLevel, Vector3d omega) {
        Vec3 currentUp = worldYDirection.normalize();
        Vec3 targetUp = new Vec3(0.0D, 1.0D, 0.0D);
        // Function: rotate the current seat-up vector toward world +Y so auto-level cannot settle upside-down.
        Vec3 rotationAxisWorld = currentUp.cross(targetUp);
        double alignment = Mth.clamp(currentUp.dot(targetUp), -1.0D, 1.0D);
        if (rotationAxisWorld.lengthSqr() < 1.0E-6D) {
            // Function: an upside-down seat has no cross-product axis, so pick the local forward axis to recover.
            rotationAxisWorld = alignment < 0.0D ? worldXDirection.normalize() : Vec3.ZERO;
        }
        if (rotationAxisWorld.lengthSqr() < 1.0E-6D) {
            return Vec3.ZERO;
        }

        double angleRadians = Math.acos(alignment);
        double rollRate = dotWorldVector(omega, worldXDirection);
        double pitchRate = dotWorldVector(omega, worldZDirection);
        if (angleRadians <= AUTO_LEVEL_SETTLE_ANGLE_RADIANS
                && Math.abs(rollRate) <= AUTO_LEVEL_SETTLE_ANGULAR_SPEED
                && Math.abs(pitchRate) <= AUTO_LEVEL_SETTLE_ANGULAR_SPEED) {
            // Function: near-perfect alignment enters a small rest zone so light ships do not hunt around level.
            return Vec3.ZERO;
        }

        double angleStrength = Mth.clamp(angleRadians / AUTO_LEVEL_FULL_TORQUE_ANGLE_RADIANS, 0.0D, 1.0D);
        angleStrength *= angleStrength;
        double nearTargetBlend = 1.0D - Mth.clamp(angleRadians / AUTO_LEVEL_NEAR_TARGET_ANGLE_RADIANS, 0.0D, 1.0D);
        MassData massData = subLevel.getMassTracker();
        double mass = massData == null || massData.isInvalid()
                ? MIN_VALID_MASS
                : Math.max(massData.getMass(), MIN_VALID_MASS);
        // Function: large ships need less proportional correction and more damping to avoid roll/pitch hunting.
        double massAlignmentGain = Mth.clamp(Math.sqrt(AUTO_LEVEL_REFERENCE_MASS / mass), AUTO_LEVEL_MIN_ALIGNMENT_GAIN, 1.0D);
        double heavyMassDampingMultiplier = Mth.lerp(massAlignmentGain, AUTO_LEVEL_HEAVY_MASS_DAMPING_MULTIPLIER, 1.0D);
        double dampingGain = Mth.lerp(nearTargetBlend, AUTO_LEVEL_ANGULAR_DAMPING, AUTO_LEVEL_NEAR_TARGET_ANGULAR_DAMPING)
                * heavyMassDampingMultiplier;
        double controlLimit = Mth.lerp(nearTargetBlend, AUTO_LEVEL_MAX_CONTROL, AUTO_LEVEL_NEAR_TARGET_MAX_CONTROL);
        rotationAxisWorld = rotationAxisWorld.normalize().scale(angleStrength);
        double localRollTorque = rotationAxisWorld.dot(worldXDirection) * AUTO_LEVEL_ALIGNMENT_TORQUE_SCALE * massAlignmentGain
                - rollRate * dampingGain;
        double localPitchTorque = rotationAxisWorld.dot(worldZDirection) * AUTO_LEVEL_ALIGNMENT_TORQUE_SCALE * massAlignmentGain
                - pitchRate * dampingGain;

        return new Vec3(
                Mth.clamp(localRollTorque, -controlLimit, controlLimit),
                0.0D,
                Mth.clamp(localPitchTorque, -controlLimit, controlLimit)
        );
    }

    private static double dotWorldVector(Vector3d vector, Vec3 axis) {
        return vector.x * axis.x + vector.y * axis.y + vector.z * axis.z;
    }

    private void tryLaunchWarpProjectile(ServerSubLevel subLevel) {
        if (data.hasPendingWarpTeleport) {
            return;
        }
        if (data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return;
        }

        Vec3 launchDirection = getNormalizedWarpTargetDirection(data.controlSeatPos);
        if (launchDirection == null) {
            return;
        }

        double alignment = Mth.clamp(worldXDirection.dot(launchDirection), -1.0D, 1.0D);
        double angleDegrees = Math.toDegrees(Math.acos(alignment));
        if (Math.abs(angleDegrees-180) >= WARP_ALIGNMENT_THRESHOLD_DEGREES) {
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

        ControlSeatBlockEntity controlSeat = getControlSeatBlockEntity();
        if (controlSeat == null) {
            return;
        }
        int e710Cost = data.warpE710CostMb > 0 ? data.warpE710CostMb : controlSeat.calculateWarpE710CostMb(data.warpTargetPos);
        // Function: consume E-710 at the actual jump launch so refueling or draining during alignment is respected.
        if (!controlSeat.consumeE710ForWarp(e710Cost)) {
            data.rejectWarpForInsufficientE710(e710Cost);
            syncWarpPreparationState();
            return;
        }

        Vec3 targetWorldPos = new Vec3(
                data.warpTargetPos.getX() + 0.5D,
                data.warpTargetPos.getY() + 0.5D,
                data.warpTargetPos.getZ() + 0.5D
        );
        spawnWarpProjectile(level, shipPos, launchDirection, projectileTravelDistance, structureMaxDimension);
        // Function: mirror the launch visual at the warp destination with the same velocity direction.
        spawnWarpProjectile(level, targetWorldPos, launchDirection, projectileTravelDistance, structureMaxDimension);

        // Function: teleport only after the projectile has flown the full bounds-derived distance.
        long executeGameTime = level.getGameTime()
                + WarpProjecTileEntity.lifeTicksForDistance(projectileTravelDistance)
                + WARP_TELEPORT_EXTRA_DELAY_TICKS;
        data.schedulePendingWarpTeleport(new Vector3d(targetWorldPos.x, targetWorldPos.y, targetWorldPos.z), executeGameTime);
        data.clearWarpPreparation();
        syncWarpPreparationState();
    }

    private void spawnWarpProjectile(Level level, Vec3 position, Vec3 launchDirection, double projectileTravelDistance, double structureMaxDimension) {
        WarpProjecTileEntity warpProjectile = new WarpProjecTileEntity(vsieEntities.WARP_PROJECTILE.get(), level);
        // Function: keep projectile flight range and FX scale identical for source and target-side launch visuals.
        warpProjectile.setPos(position.x, position.y, position.z);
        warpProjectile.configureLaunch(launchDirection, projectileTravelDistance, structureMaxDimension);
        level.addFreshEntity(warpProjectile);
    }


    private void processPendingWarpTeleport(ServerSubLevel subLevel) {
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
        if (ServerShipUtils.teleportKeepOrientation(subLevel, pendingTeleportPos)) {
            shakeScreenAtWarpCompletion(completionCenter);
            data.clearPendingWarpTeleport();
        }
    }

    private Vec3 calculateWarpCompletionCenter(ServerSubLevel subLevel, Vector3d targetPoseWorld) {
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


    private void shakeScreenAtWarpCompletion(Vec3 centerWorldPos) {
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

    private Vec3 getNormalizedWarpTargetDirection(BlockPos pos) {
        SubLevel sublevel = ServerShipUtils.getSubLevelAtBlockPos(data.level,pos);
        Vec3 seatWorldPos = ServerShipUtils.getBlockCenterWorld(sublevel, pos);
        Vec3 targetDirection = new Vec3(
                data.warpTargetPos.getX() + 0.5 - seatWorldPos.x,
                data.warpTargetPos.getY() + 0.5 - seatWorldPos.y,
                data.warpTargetPos.getZ() + 0.5 - seatWorldPos.z
        );
        if (targetDirection.lengthSqr() < 1.0E-6D) {
            return null;
        }
        return targetDirection.normalize();
    }


    private void syncWarpPreparationState() {
        ControlSeatBlockEntity controlSeat = getControlSeatBlockEntity();
        if (controlSeat == null) {
            return;
        }
        controlSeat.setChanged();
    }

    @Nullable
    private ControlSeatBlockEntity getControlSeatBlockEntity() {
        if (data.level == null || data.controlSeatPos == null) {
            return null;
        }
        if (data.level.getBlockEntity(data.controlSeatPos) instanceof ControlSeatBlockEntity controlSeat) {
            return controlSeat;
        }
        return null;
    }

    public static Vec3 calculateWorldTorque(Vector3d localTorque, Vec3 worldDirectionX, Vec3 worldDirectionY, Vec3 worldDirectionZ) {


        double[][] rotationMatrix = new double[3][3];
        rotationMatrix[0][0] = worldDirectionX.x;
        rotationMatrix[0][1] = worldDirectionY.x;
        rotationMatrix[0][2] = worldDirectionZ.x;

        rotationMatrix[1][0] = worldDirectionX.y;
        rotationMatrix[1][1] = worldDirectionY.y;
        rotationMatrix[1][2] = worldDirectionZ.y;

        rotationMatrix[2][0] = worldDirectionX.z;
        rotationMatrix[2][1] = worldDirectionY.z;
        rotationMatrix[2][2] = worldDirectionZ.z;


        double a = rotationMatrix[0][0] * localTorque.x + rotationMatrix[0][1] * localTorque.y + rotationMatrix[0][2] * localTorque.z;
        double b = rotationMatrix[1][0] * localTorque.x + rotationMatrix[1][1] * localTorque.y + rotationMatrix[1][2] * localTorque.z;
        double c = rotationMatrix[2][0] * localTorque.x + rotationMatrix[2][1] * localTorque.y + rotationMatrix[2][2] * localTorque.z;
        return new Vec3(a,b,c);

    }

}
