package com.kodu16.vsie.content.controlseat.server;


import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.controlseat.functions.ScanNearByShips;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.joml.Vector3d;
import com.kodu16.vsie.registries.ModNetworking;

import javax.annotation.Nullable;
import java.util.UUID;


public class ServerShipHandler {
    private static final UUID EMPTY_SEAT_ENTITY_ID = new UUID(0L, 0L);
    private static final double KEY_CONTROL_THRUST_EQUIVALENT = 0.10D;
    private static final double FLIGHT_ASSIST_LINEAR_RESPONSE = 0.60D;
    private static final double FLIGHT_ASSIST_ANGULAR_RESPONSE = 0.45D;
    private static final double IDLE_ANGULAR_HOLD_RESPONSE = 1.20D;
    private static final double ANTI_GRAVITY_VERTICAL_RESPONSE = 0.80D;
    private static final double FLIGHT_ASSIST_LINEAR_THRUST_FRACTION = 0.25D;
    private static final double FLIGHT_ASSIST_ANGULAR_THRUST_FRACTION = 0.18D;
    private static final double IDLE_ANGULAR_HOLD_FULL_AUTHORITY_THRUST_PER_MASS = 0.35D;
    private static final double IDLE_ANGULAR_HOLD_MIN_AUTHORITY_BLEND = 0.20D;
    private static final double ANTI_GRAVITY_DAMPING_THRUST_FRACTION = 0.25D;
    private static final double ANTI_GRAVITY_IDLE_FEEDBACK_GAIN = 0.35D;
    private static final double ANTI_GRAVITY_IDLE_MIN_THROTTLE = 0.0D;
    private static final double ANTI_GRAVITY_IDLE_MAX_THROTTLE = 2.0D;
    private static final double CONTROL_FORCE_SCALE = 0.25D;
    private static final double CONTROL_TORQUE_SCALE = 0.12D;
    private static final double LINEAR_REFERENCE_SPEED = 10.0D;
    private static final double ANGULAR_REFERENCE_SPEED = 1.5D;
    private static final double ANGULAR_ASSIST_REST_SPEED = 0.02D;
    private static final double MIN_VALID_MASS = 1.0D;
    private static final double MIN_VALID_INERTIA = 1.0D;
    private static final double CONTROL_INPUT_RESPONSE = 14.0D;
    private static final double THROTTLE_INPUT_RESPONSE = 8.0D;
    private static final double FREE_FALL_GRAVITY_IMPULSE_SCALE = 1.0D;
    private static final double STANDARD_GRAVITY = 9.0D;
    private static final double AXIS_EPSILON = 1.0E-8D;
    private ControlSeatServerData data;

    public ServerShipHandler(ControlSeatServerData data){
        this.data = data;
    }

    public static double getTranslationThrottleEquivalent() {
        return KEY_CONTROL_THRUST_EQUIVALENT / CONTROL_FORCE_SCALE;
    }

    public static float getKeyboardTorqueAxisScale() {
        return (float) (0.2D * (KEY_CONTROL_THRUST_EQUIVALENT / 0.10D));
    }

    public void resetControlInput() {
        clearManualControlInput();
        hasPreviousMotionSample = false;
        previousGForcePlayerId = null;
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
    private volatile Vec3 worldXDirection;
    private volatile Vec3 worldYDirection;
    private volatile Vec3 worldZDirection;
    private final Vector3d smoothedControlTorque = new Vector3d();
    // Function: smooth unlocked WASD translation so force does not step sharply when keys change.
    private final Vector3d smoothedTranslationInput = new Vector3d();
    private double smoothedThrottle = 0.0D;
    private double smoothedMass = Double.NaN;
    private double smoothedAverageInertia = Double.NaN;
    private final Vector3d previousPlayerPointVelocity = new Vector3d();
    private boolean hasPreviousMotionSample = false;
    private UUID previousGForcePlayerId = null;

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
                ModNetworking.sendToPlayer(new NearbyShipsS2CPacket(pos, getCurrentSeatEntityId(), data.shipsData), (ServerPlayer) data.getPlayer());
            }
        }
        if (data.getPlayer() != null) {
            if (now - lastSendMs > 50) {
                lastSendMs = now;
                ControlSeatS2CPacket packet = new ControlSeatS2CPacket(
                        pos,
                        getCurrentSeatEntityId(),
                        Vec.toVector3d(ForwardDirection),
                        Vec.toVector3d(UpDirection),
                        data.enemy,
                        data.ally,
                        data.lockedenemyslug,
                        data.getThrottle(),
                        data.isviewlocked,
                        data.shipSpeed,
                        new Vector3d(data.structureCenterWorld),
                        new Vector3d(data.structureVelocityWorld),
                        data.seatGForce
                );
                ModNetworking.sendToPlayer(packet, (ServerPlayer) data.getPlayer());
            }

            if(now - lastSendStatusMs > 250) {
                lastSendStatusMs = now;
                boolean shieldOverloaded = data.isshieldon && data.shieldcooldowntime > 0.0D;
                ControlSeatStatusS2CPacket packetstatus = new ControlSeatStatusS2CPacket(pos, getCurrentSeatEntityId(),
                        data.avalibleenergy,data.totalenergystorage,
                        data.avaliblefuel,data.totalfuelstorage,
                        data.avalibleE710, data.warpE710CostMb, data.warpE710Insufficient,
                        data.isshieldon, (int) data.avalibleshield, (int) data.totalshield, shieldOverloaded,
                        data.isforceassiston, data.istorqueassiston, data.isForceAssistSuppressedByAccelerator,
                        data.isantigravityon, data.isAutoLevelOn,
                        data.isWarpPreparing, data.hasPendingWarpTeleport, data.warpTargetName,
                        data.warpAlignmentControlX, data.warpAlignmentControlY,
                        data.activeWeaponHudInfos);
                //LogUtils.getLogger().warn("shieldtotal:"+data.totalshield+"avalible:"+data.avalibleshield);
                ModNetworking.sendToPlayer(packetstatus, (ServerPlayer) data.getPlayer());
            }

            if(now - lastSendInputMs > 250) {

                lastSendInputMs = now;
                ControlSeatInputS2CPacket packet = new ControlSeatInputS2CPacket(pos, getCurrentSeatEntityId(), data.channelencode);
                ModNetworking.sendToPlayer(packet, (ServerPlayer) data.getPlayer());
            }
        }
    }

    private UUID getCurrentSeatEntityId() {
        Player player = data.getPlayer();
        if (player != null && player.getVehicle() instanceof ControlSeatMountEntity mount) {
            return mount.getUUID();
        }
        return EMPTY_SEAT_ENTITY_ID;
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
        WarpUtils.processPendingWarpTeleport(data, subLevel);
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
        if (isIdleTorqueHoldActive(controlling)) {
            angularDampingAlpha = Math.max(
                    angularDampingAlpha,
                    idleAngularHoldDampingAlpha(totalTorqueThrust, mass, omega.length(), timeStep)
            );
        }
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
        Vector3d nonAntiGravityLinearImpulse = new Vector3d(0,0,0);
        boolean hasManualLinearInput = controlling && hasManualLinearInput(data.getForce(), data.getThrottle());

        if (data.istorqueassiston) {
            // Function: torque assist remains active during auto-level, so yaw damping is still preserved.
            finaltorque.add(invtorque);
        }
        if (data.isforceassiston) {
            finalforce.add(invforce);
            nonAntiGravityLinearImpulse.add(invforce);
        }
        if (data.isantigravityon) {
            double gravityLength = DimensionPhysicsData.getGravity(
                    subLevel.getLevel(),
                    subLevel.logicalPose().position(),
                    new Vector3d()
            ).length();
            if (gravityLength > 1.0E-6D) {
                // Function: gravity/up direction is fixed to world Y so nonstandard dimensions cannot tilt seat assists.
                Vector3d gravityDirection = new Vector3d(0.0D, -1.0D, 0.0D);
                Vector3d gravity = new Vector3d(0.0D, -gravityLength, 0.0D);
                double antiGravityThrottle = updateAntiGravityIdleThrottle(velocity, hasManualLinearInput, timeStep);
                // Function: idle anti-gravity trims the gravity-cancel impulse from world-Y velocity instead of staying fixed.
                finalforce.fma(-mass * timeStep * FREE_FALL_GRAVITY_IMPULSE_SCALE * antiGravityThrottle, gravity);

                double verticalVelocity = velocity.dot(gravityDirection);
                if (verticalVelocity > 0.0D) {
                    double verticalDampingAlpha = authorityDampingAlpha(
                            totalForceThrust * ANTI_GRAVITY_DAMPING_THRUST_FRACTION,
                            mass,
                            verticalVelocity,
                            LINEAR_REFERENCE_SPEED,
                            ANTI_GRAVITY_VERTICAL_RESPONSE,
                            timeStep
                    );
                    // Function: anti-gravity damps falling drift only, leaving active thrust against gravity unchanged.
                    finalforce.fma(-mass * verticalVelocity * verticalDampingAlpha, gravityDirection);
                }
            }
        }
        if (hasWorldControlAxes) {
            Vec3 autoLevelImpulse = AutoLevelUtils.calculateWorldAngularImpulse(
                    data,
                    subLevel,
                    momentOfInertia,
                    omega,
                    worldXDirection,
                    worldYDirection,
                    worldZDirection,
                    rawAverageInertia,
                    averageInertia,
                    deltaOmegaScale
            );
            finaltorque.add(Vec.toVector3d(autoLevelImpulse));
        }
        // Function: anti-gravity is not a thruster demand, so keep it out of visual throttle and fuel budgeting.
        Vector3d thrusterVisualForce = calculateVisualForceFromPhysicsForce(subLevel, nonAntiGravityLinearImpulse);

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

            // Function: block FACING points at the seat back, so warp aims the pilot-facing front/right axes.
            Vec3 warpForwardDirection = worldXDirection.scale(-1.0D);
            Vec3 warpRightDirection = worldZDirection.scale(-1.0D);
            Vec3 warpSeatControl = data.isWarpPreparing
                    ? WarpUtils.calculatePreparationSeatControl(data, subLevel, warpForwardDirection, worldYDirection, warpRightDirection, worldXDirection, worldYDirection, worldZDirection)
                    : Vec3.ZERO;
            if (data.isWarpPreparing) {
                // Function: brake using actual seat-local angular velocity so warp can settle instead of orbiting the target.
                warpSeatControl = warpSeatControl.subtract(calculateSeatLocalAngularVelocity(omega));
            }
            updateWarpAlignmentHudControl(data.isWarpPreparing ? warpSeatControl : Vec3.ZERO);
            Vec3 steeringTorque = data.isWarpPreparing ? Vec3.ZERO : torque;
            Vec3 translationInput = data.getForce();
            if (data.isWarpPreparing || data.hasPendingWarpTeleport) {
                smoothedControlTorque.set(0.0D, 0.0D, 0.0D);
            } else {
                smoothVector(smoothedControlTorque, steeringTorque.x, steeringTorque.y, steeringTorque.z, torqueAlpha);
            }
            smoothVector(smoothedTranslationInput, translationInput.x, translationInput.y, translationInput.z, torqueAlpha);
            smoothedThrottle += ((data.getThrottle() / 100.0D) - smoothedThrottle) * throttleAlpha;
            if (AutoLevelUtils.isEffective(data)) {
                // Function: stale roll/pitch smoothing must not bleed through after auto-level takes over leveling axes.
                smoothedControlTorque.x = 0.0D;
                smoothedControlTorque.z = 0.0D;
            }
            double activeDeltaOmegaScale = data.isWarpPreparing
                    ? Math.min(deltaOmegaScale, WarpUtils.WARP_SETTLE_ANGULAR_SPEED)
                    : deltaOmegaScale;
            Vector3d controlDeltaOmega = data.isWarpPreparing
                    ? Vec.toVector3d(warpSeatControl).mul(activeDeltaOmegaScale)
                    : new Vector3d(smoothedControlTorque).mul(deltaOmegaScale);

            if (data.isWarpPreparing) {

                WarpUtils.tryLaunchWarpProjectile(data, subLevel, omega, warpForwardDirection, worldYDirection, warpRightDirection);
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
            nonAntiGravityLinearImpulse.add(Vec.toVector3d(Invariantforce));
            nonAntiGravityLinearImpulse.add(Vec.toVector3d(translationForce));
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
        updateMotionTelemetry(subLevel, pos, player, velocity, omega, timeStep);

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

    private boolean isIdleTorqueHoldActive(boolean controlling) {
        if (!data.istorqueassiston || data.isWarpPreparing || data.hasPendingWarpTeleport) {
            return false;
        }
        if (!controlling) {
            return true;
        }
        Vec3 torqueInput = data.getTorque();
        return torqueInput == null || torqueInput.lengthSqr() <= AXIS_EPSILON;
    }

    private boolean hasManualLinearInput(Vec3 translationInput, int throttleInput) {
        return Math.abs(throttleInput) > 0
                || (translationInput != null && translationInput.lengthSqr() > AXIS_EPSILON);
    }

    private double updateAntiGravityIdleThrottle(Vector3d worldVelocity, boolean hasManualLinearInput, double timeStep) {
        if (!hasManualLinearInput && worldVelocity != null && Double.isFinite(worldVelocity.y) && timeStep > 0.0D) {
            double worldYVelocity = Math.abs(worldVelocity.y) <= 0.01 ? 0.0D : worldVelocity.y;
            data.antiGravityIdleThrottle = Mth.clamp(
                    data.antiGravityIdleThrottle - worldYVelocity * ANTI_GRAVITY_IDLE_FEEDBACK_GAIN * timeStep,
                    ANTI_GRAVITY_IDLE_MIN_THROTTLE,
                    ANTI_GRAVITY_IDLE_MAX_THROTTLE
            );
        }
        return data.antiGravityIdleThrottle;
    }

    private static double idleAngularHoldDampingAlpha(double totalTorqueThrust, double mass, double angularSpeed, double timeStep) {
        if (totalTorqueThrust <= AXIS_EPSILON || mass <= AXIS_EPSILON || angularSpeed < ANGULAR_ASSIST_REST_SPEED || timeStep <= 0.0D) {
            return 0.0D;
        }

        double thrustPerMass = totalTorqueThrust / Math.max(mass, MIN_VALID_MASS);
        double authorityBlend = Mth.clamp(
                thrustPerMass / IDLE_ANGULAR_HOLD_FULL_AUTHORITY_THRUST_PER_MASS,
                IDLE_ANGULAR_HOLD_MIN_AUTHORITY_BLEND,
                1.0D
        );
        // Function: idle hold damps angular velocity by response time so large inertia does not make orientation drift.
        return smoothingAlpha(IDLE_ANGULAR_HOLD_RESPONSE * authorityBlend, timeStep);
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

    private void updateMotionTelemetry(ServerSubLevel subLevel, BlockPos seatPos, @Nullable Player player, Vector3d velocity, Vector3d omega, double timeStep) {
        data.shipSpeed = velocity.length();
        data.structureVelocityWorld = new Vector3d(velocity);
        if (timeStep <= AXIS_EPSILON) {
            data.seatGForce = 0.0D;
            return;
        }

        Vec3 centerOfMassWorld = ServerShipUtils.getCenterOfMassWorld(subLevel);
        if (centerOfMassWorld == null) {
            data.seatGForce = 0.0D;
            return;
        }

        UUID currentPlayerId = player == null ? null : player.getUUID();
        if ((currentPlayerId == null && previousGForcePlayerId != null)
                || (currentPlayerId != null && !currentPlayerId.equals(previousGForcePlayerId))) {
            hasPreviousMotionSample = false;
            previousGForcePlayerId = currentPlayerId;
        }

        Vector3d sampleWorldPos = player != null
                ? new Vector3d(player.getX(), player.getY(), player.getZ())
                : subLevel.logicalPose().transformPosition(new Vector3d(
                        seatPos.getX() + 0.5D,
                        seatPos.getY() + 0.5D,
                        seatPos.getZ() + 0.5D
                ));
        Vector3d leverArm = sampleWorldPos.sub(new Vector3d(centerOfMassWorld.x, centerOfMassWorld.y, centerOfMassWorld.z), new Vector3d());
        Vector3d pointVelocity = new Vector3d(velocity).add(new Vector3d(omega).cross(leverArm));
        if (!hasPreviousMotionSample) {
            previousPlayerPointVelocity.set(pointVelocity);
            hasPreviousMotionSample = true;
            data.seatGForce = 0.0D;
            return;
        }

        Vector3d pointAcceleration = pointVelocity.sub(previousPlayerPointVelocity, new Vector3d()).div(timeStep);
        previousPlayerPointVelocity.set(pointVelocity);
        Vector3d gravityAcceleration = DimensionPhysicsData.getGravity(
                subLevel.getLevel(),
                sampleWorldPos,
                new Vector3d()
        );
        // Function: HUD G is the player's local proper acceleration, so natural free-fall gravity is subtracted.
        Vector3d nonGravityAcceleration = pointAcceleration.sub(gravityAcceleration);
        data.seatGForce = Math.max(0.0D, nonGravityAcceleration.length() / STANDARD_GRAVITY);
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
        double alpha = smoothingAlpha(2, timeStep);
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
        // Function: pilot input asks for angular velocity around the control-seat axes; inertia maps that to the COM angular impulse.
        Vec3 worldDeltaOmegaVec = calculateWorldTorque(controlDeltaOmega, worldXDirection, worldYDirection, worldZDirection);
        return calculateWorldAngularImpulseForWorldDeltaOmega(
                subLevel,
                inertia,
                Vec.toVector3d(worldDeltaOmegaVec),
                rawAverageInertia,
                effectiveAverageInertia
        );
    }

    private Vec3 calculateWorldAngularImpulseForWorldDeltaOmega(
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

    private void updateWarpAlignmentHudControl(Vec3 seatControl) {
        if (!isUsableAxis(seatControl)) {
            data.clearWarpAlignmentControl();
            return;
        }

        // Function: mirror mouse input mapping: screen X is yaw around seat-up, screen Y is pitch around seat-right.
        data.warpAlignmentControlX = -seatControl.y;
        data.warpAlignmentControlY = seatControl.z;
    }

    private Vec3 calculateSeatLocalAngularVelocity(Vector3d worldAngularVelocity) {
        if (worldAngularVelocity == null || !isFiniteVector(worldAngularVelocity)) {
            return Vec3.ZERO;
        }
        return new Vec3(
                dotWorldVector(worldAngularVelocity, worldXDirection),
                dotWorldVector(worldAngularVelocity, worldYDirection),
                dotWorldVector(worldAngularVelocity, worldZDirection)
        );
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

    private static double dotWorldVector(Vector3d vector, Vec3 axis) {
        return vector.x * axis.x + vector.y * axis.y + vector.z * axis.z;
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
