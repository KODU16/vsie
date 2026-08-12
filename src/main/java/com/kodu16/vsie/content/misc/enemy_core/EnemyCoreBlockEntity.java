package com.kodu16.vsie.content.misc.enemy_core;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.functions.ScanNearByShips;
import com.kodu16.vsie.content.misc.enemy_cannon.EnemyCannonBlockEntity;
import com.kodu16.vsie.content.misc.enemy_cannon.EnemyCannonCbcCompat;
import com.kodu16.vsie.content.misc.enemy_autocannon.EnemyAutocannonBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.ModMenuTypes;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3dc;
import org.joml.Vector3d;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class EnemyCoreBlockEntity extends AbstractControlSeatBlockEntity implements MenuProvider {
    public static final int MAX_PATTERN_LENGTH = 64;
    public static final double MIN_ORBIT_RADIUS = 8.0D;
    public static final double MAX_ORBIT_RADIUS = 2048.0D;
    public static final double MIN_ORBIT_SPEED = 0.1D;
    public static final double MAX_ORBIT_SPEED = 128.0D;

    private static final int TARGET_SCAN_INTERVAL_TICKS = 10;
    private static final double MIN_SEARCH_RADIUS = 256.0D;
    private static final double MAX_SEARCH_RADIUS = 4096.0D;
    private static final double LINEAR_RESPONSE = 0.28D;
    private static final double POSITION_GAIN = 0.12D;
    private static final double MAX_POSITION_CORRECTION_FACTOR = 0.75D;
    private static final double NORMAL_ORIENTATION_GAIN = 0.42D;
    private static final double NORMAL_ANGULAR_DAMPING = 0.78D;
    private static final double NORMAL_MAX_ANGULAR_CORRECTION = 0.28D;
    private static final double ATTACK_ORIENTATION_GAIN = 1.10D;
    private static final double ATTACK_ANGULAR_DAMPING = 0.94D;
    private static final double ATTACK_MAX_ANGULAR_CORRECTION = 0.60D;
    private static final double ATTACK_TORQUE_REFERENCE_SPEED = 4.0D;
    private static final double MIN_ATTACK_TORQUE_SCALE = 1.0D;
    private static final double MAX_ATTACK_TORQUE_SCALE = 8.0D;
    private static final double MIN_VELOCITY_ALIGNMENT_SPEED = 0.25D;
    private static final double PHYSICS_STEP_SECONDS = 1.0D / 20.0D;
    private static final double FULL_ORBIT_RADIANS = Math.PI * 2.0D;
    private static final double MIN_PLANE_TILT = Math.toRadians(12.0D);
    private static final double MAX_PLANE_TILT = Math.toRadians(35.0D);
    private static final int MIN_ROLL_WAIT_TICKS = 80;
    private static final int MAX_ROLL_WAIT_TICKS = 240;
    private static final int MIN_ROLL_DURATION_TICKS = 12;
    private static final int MAX_ROLL_DURATION_TICKS = 24;
    private static final int MIN_ROLL_HOLD_TICKS = 6;
    private static final int MAX_ROLL_HOLD_TICKS = 16;
    private static final double MIN_ROLL_ANGLE = Math.toRadians(12.0D);
    private static final double MAX_ROLL_ANGLE = Math.toRadians(38.0D);
    private static final double BASE_ACCURACY_ORBIT_RADIUS = 32.0D;
    private static final double BASE_ATTACK_ALIGNMENT_ERROR = 0.01D;
    private static final double ATTACK_ALIGNMENT_ERROR_DECAY = 0.10D;
    private static final double MAX_ATTACK_FIRE_ALIGNMENT_DOT = 0.99999D;
    // Function: firing uses distance-scaled precision while returning keeps the looser completion tolerance.
    private static final double RETURN_ALIGNMENT_DOT = 0.95D;
    private static final double ATTACK_RETURN_BLEND_PER_TICK = 1.0D / 20.0D;
    private static final double EPSILON = 1.0E-8D;

    private double orbitRadius = 64.0D;
    private double orbitSpeed = 8.0D;

    private boolean forceLoadChecked;
    private int targetScanCooldown;
    private @Nullable Target target;
    private final Vector3d orbitRadial = new Vector3d(1.0D, 0.0D, 0.0D);
    private final Vector3d orbitNormal = new Vector3d(0.0D, 1.0D, 0.0D);
    private double orbitCycleAngle;
    private double orbitCycleLength = FULL_ORBIT_RADIANS;
    private boolean planeTransitionActive;
    private double planeTransitionAngle;
    private boolean orbitPathInitialized;
    private int rollStage;
    private int rollTicksRemaining;
    private int rollDurationTicks;
    private int rollHoldTicks;
    private int rollReturnTicks;
    private int rollWaitAfterCycle;
    private double rollStartAngle;
    private double rollTargetAngle;
    private double currentRollAngle;
    private double previousRollAngle;
    private AttackPhase attackPhase = AttackPhase.ORBITING;
    private boolean attackVolleyStarted;
    private double attackAimBlend;
    private final Random orbitRandom;

    public EnemyCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.orbitRandom = new Random(pos.asLong());
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        SubLevel containing = ServerShipUtils.getSubLevelAtBlockPos(serverLevel, worldPosition);
        if (!(containing instanceof ServerSubLevel ownShip) || ownShip.isRemoved()) {
            target = null;
            forceLoadChecked = false;
            resetAttackState();
            return;
        }

        ensureSableForceLoaded(serverLevel, ownShip);
        updateTarget(serverLevel, ownShip);
        Target activeTarget = target;
        if (activeTarget == null) {
            resetAttackState();
            return;
        }

        TargetMotion targetMotion = resolveTargetMotion(serverLevel, ownShip, activeTarget);
        if (targetMotion == null) {
            target = null;
            targetScanCooldown = 0;
            resetAttackState();
            return;
        }
        AttackWeapons attackWeapons = collectAttackWeapons(serverLevel);
        applyOrbitGuidance(
                serverLevel,
                ownShip,
                targetMotion,
                activeTarget.type == TargetType.PLAYER,
                attackWeapons
        );
    }

    private void ensureSableForceLoaded(ServerLevel serverLevel, ServerSubLevel ownShip) {
        if (forceLoadChecked) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        if (container == null) {
            return;
        }
        // Function: mirror `/sable forceload add` only when no existing Sable loading ticket already retains this ship.
        if (!container.collectForceLoadedSubLevels().contains(ownShip)) {
            container.addForceLoadTicket(ownShip, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
        }
        forceLoadChecked = true;
    }

    private void updateTarget(ServerLevel level, ServerSubLevel ownShip) {
        if (targetScanCooldown-- > 0 && target != null) {
            return;
        }
        targetScanCooldown = TARGET_SCAN_INTERVAL_TICKS;

        Target nextTarget = findNearestHostilePlayer(level, ownShip);
        if (nextTarget == null) {
            nextTarget = findNearestHostileShip(level, ownShip);
        }
        if (!Objects.equals(target, nextTarget)) {
            target = nextTarget;
            if (nextTarget != null) {
                resetOrbitPath();
            }
        }
    }

    private @Nullable Target findNearestHostilePlayer(ServerLevel level, ServerSubLevel ownShip) {
        Vec3 ownCenter = ServerShipUtils.getStructureCenterWorld(ownShip);
        if (ownCenter == null) {
            return null;
        }
        double searchRadiusSqr = Mth.square(getSearchRadius());
        return level.players().stream()
                .filter(player -> player.isAlive() && !player.isRemoved() && !player.isSpectator())
                .filter(player -> !isInsideShipBounds(player.position(), ownShip))
                .filter(player -> ScanNearByShips.getPriority(
                        getEnemyPattern(),
                        getAllyPattern(),
                        player.getGameProfile().getName()
                ) == 1)
                .filter(player -> player.distanceToSqr(ownCenter) <= searchRadiusSqr)
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(ownCenter)))
                .map(player -> new Target(TargetType.PLAYER, player.getUUID()))
                .orElse(null);
    }

    private @Nullable Target findNearestHostileShip(ServerLevel level, ServerSubLevel ownShip) {
        Vec3 ownCenter = ServerShipUtils.getStructureCenterWorld(ownShip);
        if (ownCenter == null) {
            return null;
        }
        double searchRadiusSqr = Mth.square(getSearchRadius());
        List<SubLevel> hostileShips = ScanNearByShips.scanEnemySubLevels(
                null,
                worldPosition,
                level,
                getEnemyPattern(),
                getAllyPattern()
        );
        return hostileShips.stream()
                .filter(ship -> ship instanceof ServerSubLevel && ship != ownShip && !ship.isRemoved())
                .filter(ship -> {
                    Vec3 center = ServerShipUtils.getStructureCenterWorld(ship);
                    return center != null && center.distanceToSqr(ownCenter) <= searchRadiusSqr;
                })
                .min(Comparator.comparingDouble(ship -> {
                    Vec3 center = ServerShipUtils.getStructureCenterWorld(ship);
                    return center == null ? Double.MAX_VALUE : center.distanceToSqr(ownCenter);
                }))
                .map(ship -> new Target(TargetType.SHIP, ship.getUniqueId()))
                .orElse(null);
    }

    private @Nullable TargetMotion resolveTargetMotion(ServerLevel level, ServerSubLevel ownShip, Target selected) {
        if (selected.type == TargetType.PLAYER) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(selected.id);
            if (player == null || player.serverLevel() != level || !player.isAlive() || player.isSpectator()
                    || isInsideShipBounds(player.position(), ownShip)
                    || ScanNearByShips.getPriority(getEnemyPattern(), getAllyPattern(), player.getGameProfile().getName()) != 1) {
                return null;
            }
            // Function: Minecraft player motion is per tick, while Sable rigid-body velocity is expressed per second.
            return new TargetMotion(
                    player.position(),
                    player.getEyePosition(),
                    player.getDeltaMovement().scale(20.0D)
            );
        }

        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || !(container.getSubLevel(selected.id) instanceof ServerSubLevel targetShip)
                || targetShip == ownShip || targetShip.isRemoved()
                || ScanNearByShips.getPriority(getEnemyPattern(), getAllyPattern(), targetShip.getName()) != 1) {
            return null;
        }
        Vec3 center = ServerShipUtils.getStructureCenterWorld(targetShip);
        RigidBodyHandle handle = RigidBodyHandle.of(targetShip);
        if (center == null || handle == null || !handle.isValid()) {
            return null;
        }
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        return isFinite(velocity)
                ? new TargetMotion(center, center, new Vec3(velocity.x, velocity.y, velocity.z))
                : null;
    }

    private void resetOrbitPath() {
        orbitPathInitialized = false;
        orbitCycleAngle = 0.0D;
        orbitCycleLength = FULL_ORBIT_RADIANS;
        planeTransitionActive = false;
        planeTransitionAngle = 0.0D;
        rollStage = 0;
        rollTicksRemaining = randomTicks(MIN_ROLL_WAIT_TICKS, MAX_ROLL_WAIT_TICKS);
        currentRollAngle = 0.0D;
        previousRollAngle = 0.0D;
    }

    private void applyOrbitGuidance(
            ServerLevel serverLevel,
            ServerSubLevel ship,
            TargetMotion targetMotion,
            boolean playerTarget,
            AttackWeapons attackWeapons
    ) {
        MassData massData = ship.getMassTracker();
        RigidBodyHandle handle = RigidBodyHandle.of(ship);
        Vec3 shipCenter = ServerShipUtils.getCenterOfMassWorld(ship);
        if (massData == null || massData.isInvalid() || shipCenter == null || handle == null || !handle.isValid()) {
            return;
        }

        double mass = massData.getMass();
        Matrix3dc inertia = massData.getInertiaTensor();
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        Vector3d omega = handle.getAngularVelocity(new Vector3d());
        if (!Double.isFinite(mass) || mass <= EPSILON || inertia == null || !isFinite(velocity) || !isFinite(omega)) {
            return;
        }

        Vector3d targetCenter = toVector(targetMotion.position);
        if (!orbitPathInitialized) {
            initializeOrbitPath(toVector(shipCenter).sub(targetCenter));
        }

        double effectiveOrbitRadius = calculateEffectiveOrbitRadius(serverLevel, ship);
        double angularSpeed = orbitSpeed / effectiveOrbitRadius;
        updateRoll();
        boolean readyWeapon = !attackWeapons.readyCannons.isEmpty()
                || !attackWeapons.readyAutocannons.isEmpty();
        boolean activeBurst = attackVolleyStarted && attackWeapons.hasActiveAutocannonBurst;
        updateAttackState(playerTarget && (readyWeapon || activeBurst));
        boolean directAttackActive = attackPhase == AttackPhase.AIMING;
        if (!directAttackActive) {
            advanceOrbitPath(angularSpeed * PHYSICS_STEP_SECONDS);
        }
        Vector3d radial = new Vector3d(orbitRadial);
        Vector3d tangent = orbitNormal.cross(radial, new Vector3d()).normalize();
        Vector3d attackDirection = toVector(targetMotion.aimPosition)
                .sub(toVector(ServerShipUtils.getBlockCenterWorld(ship, worldPosition)));
        if (!isFinite(attackDirection) || attackDirection.lengthSquared() < EPSILON) {
            attackDirection.set(tangent);
        } else {
            attackDirection.normalize();
        }

        Vector3d desiredVelocity;
        if (directAttackActive) {
            // Function: attack mode suspends orbital correction and drives straight toward the target.
            desiredVelocity = toVector(targetMotion.velocity).fma(orbitSpeed, attackDirection);
        } else {
            Vector3d desiredPosition = new Vector3d(targetCenter).fma(effectiveOrbitRadius, radial);
            Vector3d positionError = desiredPosition.sub(toVector(shipCenter), new Vector3d());
            desiredVelocity = toVector(targetMotion.velocity).fma(orbitSpeed, tangent);
            double maxPositionCorrection = Math.max(orbitSpeed * MAX_POSITION_CORRECTION_FACTOR, 2.0D);
            Vector3d positionCorrection = clampLength(positionError.mul(POSITION_GAIN), maxPositionCorrection);
            desiredVelocity.add(positionCorrection);
        }
        Vector3d normalForward = selectVelocityForward(velocity, desiredVelocity, tangent);

        Vector3d deltaVelocity = desiredVelocity.sub(velocity, new Vector3d()).mul(LINEAR_RESPONSE);
        double maxDeltaVelocity = Math.max(orbitSpeed * 0.25D, 1.0D);
        clampLength(deltaVelocity, maxDeltaVelocity);
        Vector3d worldImpulse = deltaVelocity.mul(mass);
        addGravityCompensation(ship, mass, worldImpulse);

        double easedAttackBlend = smoothStep(attackAimBlend);
        Vector3d desiredForward = directAttackActive
                ? new Vector3d(attackDirection)
                : blendDirections(normalForward, attackDirection, orbitNormal, easedAttackBlend);
        Vector3d desiredUp = new Vector3d(radial)
                .fma(-radial.dot(desiredForward), desiredForward);
        if (!isFinite(desiredUp) || desiredUp.lengthSquared() < EPSILON) {
            desiredUp.set(orbitNormal).fma(-orbitNormal.dot(desiredForward), desiredForward);
        }
        desiredUp.normalize();

        Vector3d currentForward = getCurrentWorldForward(ship);
        double requiredAttackAlignment = calculateAttackFireAlignment(effectiveOrbitRadius);
        if (attackPhase == AttackPhase.AIMING
                && !attackVolleyStarted
                && currentForward.dot(attackDirection) >= requiredAttackAlignment) {
            boolean firedAny = false;
            for (EnemyCannonBlockEntity cannon : attackWeapons.readyCannons) {
                boolean fired = EnemyCannonCbcCompat.fireConfiguredProjectile(
                        serverLevel,
                        ship,
                        cannon.getBlockPos(),
                        orbitRadius * 0.25D,
                        cannon.getConfiguredProjectile(),
                        cannon.getChargeCount()
                );
                if (fired) {
                    // Function: only a successfully spawned round starts this cannon's independent cooldown.
                    cannon.markFired(serverLevel.getGameTime());
                    firedAny = true;
                }
            }
            boolean startedBurst = false;
            for (EnemyAutocannonBlockEntity autocannon : attackWeapons.readyAutocannons) {
                startedBurst |= autocannon.startBurst(
                        serverLevel.getGameTime(), orbitRadius * 0.25D
                );
            }
            attackVolleyStarted = firedAny || startedBurst;
            if (firedAny) {
                // Function: single-shot cannon volleys return immediately unless an autocannon burst is active.
                if (!startedBurst) {
                    attackPhase = AttackPhase.RETURNING;
                    attackVolleyStarted = false;
                }
            } else if (!startedBurst) {
                attackVolleyStarted = false;
            }
        } else if (attackPhase == AttackPhase.AIMING
                && attackVolleyStarted
                && !attackWeapons.hasActiveAutocannonBurst) {
            // Function: hold attack alignment until every started autocannon finishes its complete burst.
            attackPhase = AttackPhase.RETURNING;
            attackVolleyStarted = false;
        } else if (attackPhase == AttackPhase.RETURNING
                && attackAimBlend <= EPSILON
                && currentForward.dot(normalForward) >= RETURN_ALIGNMENT_DOT) {
            attackPhase = AttackPhase.ORBITING;
        }

        Vector3d worldTorqueImpulse = calculateOrientationImpulse(
                ship,
                inertia,
                omega,
                desiredForward,
                desiredUp,
                orbitNormal,
                directAttackActive ? 0.0D : angularSpeed,
                (currentRollAngle - previousRollAngle) / PHYSICS_STEP_SECONDS,
                directAttackActive || attackPhase == AttackPhase.RETURNING,
                directAttackActive ? 0.0D : 1.0D - easedAttackBlend,
                calculateAttackTorqueScale(orbitSpeed)
        );
        if (isFinite(worldImpulse) && isFinite(worldTorqueImpulse)) {
            ServerShipUtils.applyWorldForceAndTorqueAtCenterOfMass(ship, worldImpulse, worldTorqueImpulse);
        }
    }

    private void initializeOrbitPath(Vector3d initialOffset) {
        if (isFinite(initialOffset) && initialOffset.lengthSquared() > EPSILON) {
            orbitRadial.set(initialOffset).normalize();
        } else {
            randomUnitVector(orbitRadial);
        }
        chooseInitialOrbitPlane();
        scheduleStableOrbit();
        orbitPathInitialized = true;
    }

    private void advanceOrbitPath(double angularDistance) {
        // Function: alternate a complete circular orbit with a full-circle, eased plane transition.
        int cycleGuard = 0;
        while (angularDistance > EPSILON && cycleGuard++ < 16) {
            double remainingCycle = orbitCycleLength - orbitCycleAngle;
            double step = Math.min(angularDistance, remainingCycle);
            double oldProgress = orbitCycleAngle / orbitCycleLength;
            double newProgress = (orbitCycleAngle + step) / orbitCycleLength;

            orbitRadial.rotateAxis(
                    step,
                    orbitNormal.x,
                    orbitNormal.y,
                    orbitNormal.z
            ).normalize();
            if (planeTransitionActive) {
                double tiltStep = planeTransitionAngle
                        * (smoothStep(newProgress) - smoothStep(oldProgress));
                orbitNormal.rotateAxis(
                        tiltStep,
                        orbitRadial.x,
                        orbitRadial.y,
                        orbitRadial.z
                );
                // Function: remove numerical drift so the instantaneous trajectory remains a true tangent circle.
                orbitNormal.fma(-orbitNormal.dot(orbitRadial), orbitRadial).normalize();
            }

            orbitCycleAngle += step;
            angularDistance -= step;
            if (orbitCycleAngle >= orbitCycleLength - EPSILON) {
                orbitCycleAngle = 0.0D;
                if (planeTransitionActive) {
                    planeTransitionActive = false;
                    planeTransitionAngle = 0.0D;
                    scheduleStableOrbit();
                } else {
                    planeTransitionActive = true;
                    orbitCycleLength = FULL_ORBIT_RADIANS;
                    double magnitude = Mth.lerp(
                            orbitRandom.nextDouble(),
                            MIN_PLANE_TILT,
                            MAX_PLANE_TILT
                    );
                    planeTransitionAngle = orbitRandom.nextBoolean() ? magnitude : -magnitude;
                }
            }
        }
    }

    private void scheduleStableOrbit() {
        // Function: complete at least one circle, then choose a different point on it as the next smooth-change pivot.
        orbitCycleLength = FULL_ORBIT_RADIANS * (1.0D + orbitRandom.nextDouble());
    }

    private void chooseInitialOrbitPlane() {
        Vector3d helper = new Vector3d();
        randomUnitVector(helper);
        orbitRadial.cross(helper, orbitNormal);
        if (!isFinite(orbitNormal) || orbitNormal.lengthSquared() < EPSILON) {
            helper.set(Math.abs(orbitRadial.y) < 0.9D ? 0.0D : 1.0D,
                    Math.abs(orbitRadial.y) < 0.9D ? 1.0D : 0.0D,
                    0.0D);
            orbitRadial.cross(helper, orbitNormal);
        }
        orbitNormal.normalize();
    }

    private void randomUnitVector(Vector3d destination) {
        double y = orbitRandom.nextDouble() * 2.0D - 1.0D;
        double azimuth = orbitRandom.nextDouble() * Math.PI * 2.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        destination.set(horizontal * Math.cos(azimuth), y, horizontal * Math.sin(azimuth));
    }

    private void updateRoll() {
        previousRollAngle = currentRollAngle;
        if (rollStage == 0) {
            if (--rollTicksRemaining <= 0) {
                rollStage = 1;
                rollStartAngle = currentRollAngle;
                double magnitude = Mth.lerp(
                        orbitRandom.nextDouble(),
                        MIN_ROLL_ANGLE,
                        MAX_ROLL_ANGLE
                );
                rollTargetAngle = orbitRandom.nextBoolean() ? magnitude : -magnitude;
                int rollOutTicks = randomTicks(MIN_ROLL_DURATION_TICKS, MAX_ROLL_DURATION_TICKS);
                rollHoldTicks = randomTicks(MIN_ROLL_HOLD_TICKS, MAX_ROLL_HOLD_TICKS);
                rollReturnTicks = randomTicks(MIN_ROLL_DURATION_TICKS, MAX_ROLL_DURATION_TICKS);
                int startInterval = randomTicks(MIN_ROLL_WAIT_TICKS, MAX_ROLL_WAIT_TICKS);
                rollWaitAfterCycle = Math.max(
                        1,
                        startInterval - rollOutTicks - rollHoldTicks - rollReturnTicks
                );
                beginRollTransition(rollOutTicks);
            }
            return;
        }
        if (rollStage == 2) {
            if (--rollTicksRemaining <= 0) {
                rollStage = 3;
                rollStartAngle = currentRollAngle;
                rollTargetAngle = 0.0D;
                beginRollTransition(rollReturnTicks);
            }
            return;
        }

        int elapsedTicks = rollDurationTicks - rollTicksRemaining;
        double progress = Mth.clamp((elapsedTicks + 1.0D) / rollDurationTicks, 0.0D, 1.0D);
        double easedProgress = progress * progress * (3.0D - 2.0D * progress);
        currentRollAngle = Mth.lerp(easedProgress, rollStartAngle, rollTargetAngle);
        if (--rollTicksRemaining <= 0) {
            currentRollAngle = rollTargetAngle;
            if (rollStage == 1) {
                rollStage = 2;
                rollTicksRemaining = rollHoldTicks;
            } else {
                rollStage = 0;
                rollTicksRemaining = rollWaitAfterCycle;
            }
        }
    }

    private void beginRollTransition(int durationTicks) {
        rollDurationTicks = durationTicks;
        rollTicksRemaining = durationTicks;
    }

    private int randomTicks(int minimum, int maximum) {
        return minimum + orbitRandom.nextInt(maximum - minimum + 1);
    }

    private void updateAttackState(boolean canAttack) {
        if (!canAttack) {
            if (attackPhase == AttackPhase.AIMING) {
                attackPhase = AttackPhase.RETURNING;
                attackVolleyStarted = false;
            }
        }

        if (attackPhase == AttackPhase.ORBITING) {
            attackAimBlend = 0.0D;
            if (!canAttack) {
                return;
            }
            // Function: a ready linked cannon now starts the attack; the core no longer owns firing cadence.
            attackPhase = AttackPhase.AIMING;
            attackAimBlend = 1.0D;
        } else if (attackPhase == AttackPhase.AIMING) {
            attackAimBlend = 1.0D;
        } else {
            attackAimBlend = Math.max(0.0D, attackAimBlend - ATTACK_RETURN_BLEND_PER_TICK);
        }
    }

    private void resetAttackState() {
        attackPhase = AttackPhase.ORBITING;
        attackVolleyStarted = false;
        attackAimBlend = 0.0D;
    }

    private AttackWeapons collectAttackWeapons(ServerLevel serverLevel) {
        Direction coreFacing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        List<EnemyCannonBlockEntity> readyCannons = new java.util.ArrayList<>();
        List<EnemyAutocannonBlockEntity> readyAutocannons = new java.util.ArrayList<>();
        boolean activeAutocannonBurst = false;
        for (BlockPos weaponPos : getLinkedEnemyCannonPositionsInOrder()) {
            var blockEntity = serverLevel.getBlockEntity(weaponPos);
            if (blockEntity instanceof EnemyCannonBlockEntity cannon
                    && worldPosition.equals(cannon.getLinkedEnemyCorePos())) {
                confirmLinkedPeripheralPresent(Vec3.atLowerCornerOf(weaponPos), ENEMY_CANNON_PERIPHERAL_TYPE);
                if (hasMatchingFacing(cannon.getBlockState(), coreFacing)
                        && cannon.isReadyToFire(serverLevel.getGameTime())) {
                    readyCannons.add(cannon);
                }
                continue;
            }
            if (blockEntity instanceof EnemyAutocannonBlockEntity autocannon
                    && worldPosition.equals(autocannon.getLinkedEnemyCorePos())) {
                confirmLinkedPeripheralPresent(Vec3.atLowerCornerOf(weaponPos), ENEMY_CANNON_PERIPHERAL_TYPE);
                if (hasMatchingFacing(autocannon.getBlockState(), coreFacing)) {
                    activeAutocannonBurst |= autocannon.hasActiveBurst();
                    if (autocannon.isReadyToStartBurst(serverLevel.getGameTime())) {
                        readyAutocannons.add(autocannon);
                    }
                }
                continue;
            }
            removeLinkedPeripheral(Vec3.atLowerCornerOf(weaponPos), ENEMY_CANNON_PERIPHERAL_TYPE);
        }
        return new AttackWeapons(readyCannons, readyAutocannons, activeAutocannonBurst);
    }

    private static boolean hasMatchingFacing(BlockState state, Direction coreFacing) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                && state.getValue(HorizontalDirectionalBlock.FACING) == coreFacing;
    }

    private static double smoothStep(double progress) {
        double clamped = Mth.clamp(progress, 0.0D, 1.0D);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private static Vector3d blendDirections(
            Vector3d from,
            Vector3d to,
            Vector3d fallbackAxis,
            double progress
    ) {
        double dot = Mth.clamp(from.dot(to), -1.0D, 1.0D);
        Vector3d rotationAxis = from.cross(to, new Vector3d());
        if (rotationAxis.lengthSquared() < EPSILON) {
            if (dot > 0.0D) {
                return new Vector3d(from);
            }
            rotationAxis.set(fallbackAxis).fma(-fallbackAxis.dot(from), from);
        }
        if (!isFinite(rotationAxis) || rotationAxis.lengthSquared() < EPSILON) {
            rotationAxis.set(0.0D, 1.0D, 0.0D).cross(from);
        }
        // Function: angular interpolation remains smooth even when the orbit tangent faces exactly away from the player.
        return new Vector3d(from).rotateAxis(
                Math.acos(dot) * Mth.clamp(progress, 0.0D, 1.0D),
                rotationAxis.normalize().x,
                rotationAxis.y,
                rotationAxis.z
        ).normalize();
    }

    private static Vector3d selectVelocityForward(
            Vector3d currentVelocity,
            Vector3d desiredVelocity,
            Vector3d fallbackTangent
    ) {
        double minimumSpeedSquared = MIN_VELOCITY_ALIGNMENT_SPEED * MIN_VELOCITY_ALIGNMENT_SPEED;
        if (isFinite(currentVelocity) && currentVelocity.lengthSquared() >= minimumSpeedSquared) {
            // Function: the core block's world-facing direction follows the ship's actual travel direction.
            return new Vector3d(currentVelocity).normalize();
        }
        if (isFinite(desiredVelocity) && desiredVelocity.lengthSquared() >= minimumSpeedSquared) {
            return new Vector3d(desiredVelocity).normalize();
        }
        return new Vector3d(fallbackTangent).normalize();
    }

    private static double calculateAttackTorqueScale(double configuredOrbitSpeed) {
        // Function: faster orbit settings proportionally raise attack turning and braking authority.
        return Mth.clamp(
                configuredOrbitSpeed / ATTACK_TORQUE_REFERENCE_SPEED,
                MIN_ATTACK_TORQUE_SCALE,
                MAX_ATTACK_TORQUE_SCALE
        );
    }

    private static double calculateAttackFireAlignment(double effectiveOrbitRadius) {
        double distanceSteps = Math.max(
                0.0D,
                (effectiveOrbitRadius - BASE_ACCURACY_ORBIT_RADIUS) / BASE_ACCURACY_ORBIT_RADIUS
        );
        double alignmentError = BASE_ATTACK_ALIGNMENT_ERROR
                * Math.pow(ATTACK_ALIGNMENT_ERROR_DECAY, distanceSteps);
        // Function: every additional 32 blocks reduces allowed aim error tenfold, without reaching an impossible dot of one.
        return Math.min(MAX_ATTACK_FIRE_ALIGNMENT_DOT, 1.0D - alignmentError);
    }

    private double calculateEffectiveOrbitRadius(ServerLevel serverLevel, ServerSubLevel ownShip) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        if (container == null) {
            return orbitRadius;
        }

        double largestNonFriendlySize = 0.0D;
        for (ServerSubLevel candidate : container.getAllSubLevels()) {
            if (candidate == null || candidate == ownShip || candidate.isRemoved()) {
                continue;
            }
            if (ScanNearByShips.getPriority(
                    getEnemyPattern(),
                    getAllyPattern(),
                    candidate.getName()
            ) == 2) {
                continue;
            }
            largestNonFriendlySize = Math.max(
                    largestNonFriendlySize,
                    ServerShipUtils.getStructureMaxDimension(candidate)
            );
        }
        // Function: neutral and hostile loaded ships enlarge the orbit so their bounding boxes cannot occupy the nominal path.
        return orbitRadius + largestNonFriendlySize;
    }

    private Vector3d calculateOrientationImpulse(
            ServerSubLevel ship,
            Matrix3dc inertia,
            Vector3d currentOmega,
            Vector3d desiredForward,
            Vector3d desiredUp,
            Vector3d orbitAxis,
            double orbitAngularSpeed,
            double rollAngularSpeed,
            boolean attackTurning,
            double rollAuthority,
            double attackTorqueScale
    ) {
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        Vector3d localForward = new Vector3d(facing.getStepX(), 0.0D, facing.getStepZ()).normalize();
        Vector3d localUp = new Vector3d(0.0D, 1.0D, 0.0D);
        Vector3d localRight = localForward.cross(localUp, new Vector3d()).normalize();

        Vector3d currentForward = ship.logicalPose().orientation().transform(new Vector3d(localForward));
        Vector3d currentUp = ship.logicalPose().orientation().transform(new Vector3d(localUp));
        Vector3d currentRight = ship.logicalPose().orientation().transform(new Vector3d(localRight));
        desiredUp.rotateAxis(
                currentRollAngle * rollAuthority,
                desiredForward.x,
                desiredForward.y,
                desiredForward.z
        ).normalize();
        Vector3d desiredRight = desiredForward.cross(desiredUp, new Vector3d()).normalize();

        Vector3d error = currentForward.cross(desiredForward, new Vector3d())
                .add(currentUp.cross(desiredUp, new Vector3d()))
                .add(currentRight.cross(desiredRight, new Vector3d()))
                .mul(0.5D);
        if (error.lengthSquared() < EPSILON && currentForward.dot(desiredForward) < 0.0D) {
            // Function: provide a deterministic axis when the controller starts exactly 180 degrees from its path.
            error.set(currentUp).cross(desiredForward).normalize();
            if (!isFinite(error) || error.lengthSquared() < EPSILON) {
                error.set(currentRight);
            }
        }

        Vector3d desiredOmega = new Vector3d(orbitAxis).mul(orbitAngularSpeed)
                .fma(rollAngularSpeed * rollAuthority, desiredForward);
        double orientationGain = attackTurning
                ? ATTACK_ORIENTATION_GAIN * attackTorqueScale
                : NORMAL_ORIENTATION_GAIN;
        double angularDamping = attackTurning ? ATTACK_ANGULAR_DAMPING : NORMAL_ANGULAR_DAMPING;
        double maxAngularCorrection = attackTurning
                ? ATTACK_MAX_ANGULAR_CORRECTION
                : NORMAL_MAX_ANGULAR_CORRECTION;
        Vector3d deltaOmegaWorld = error.mul(orientationGain)
                .add(desiredOmega.sub(currentOmega, new Vector3d()).mul(angularDamping));
        double torqueScale = attackTurning ? attackTorqueScale : 1.0D;
        // Function: scale turn demand and torque limit, but keep per-tick damping below one to prevent attack jitter.
        clampLength(deltaOmegaWorld, maxAngularCorrection * torqueScale);

        Vector3d localDeltaOmega = ship.logicalPose().orientation().transformInverse(new Vector3d(deltaOmegaWorld));
        Vector3d localImpulse = inertia.transform(localDeltaOmega, new Vector3d());
        return ship.logicalPose().orientation().transform(localImpulse);
    }

    private Vector3d getCurrentWorldForward(ServerSubLevel ship) {
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        Vector3d localForward = new Vector3d(facing.getStepX(), 0.0D, facing.getStepZ()).normalize();
        return ship.logicalPose().orientation().transform(localForward).normalize();
    }

    private void addGravityCompensation(ServerSubLevel ship, double mass, Vector3d worldImpulse) {
        Vector3d gravity = DimensionPhysicsData.getGravity(
                ship.getLevel(),
                ship.logicalPose().position(),
                new Vector3d()
        );
        if (isFinite(gravity)) {
            // Function: orbit guidance supplies its own hover authority instead of requiring a control-seat anti-gravity mode.
            worldImpulse.fma(-mass * PHYSICS_STEP_SECONDS, gravity);
        }
    }

    private boolean isInsideShipBounds(Vec3 position, ServerSubLevel ship) {
        var bounds = ship.boundingBox();
        return position.x >= bounds.minX() && position.x <= bounds.maxX()
                && position.y >= bounds.minY() && position.y <= bounds.maxY()
                && position.z >= bounds.minZ() && position.z <= bounds.maxZ();
    }

    private double getSearchRadius() {
        return Mth.clamp(orbitRadius * 3.0D, MIN_SEARCH_RADIUS, MAX_SEARCH_RADIUS);
    }

    private static Vector3d clampLength(Vector3d vector, double maxLength) {
        double lengthSquared = vector.lengthSquared();
        if (lengthSquared > maxLength * maxLength && lengthSquared > EPSILON) {
            vector.mul(maxLength / Math.sqrt(lengthSquared));
        }
        return vector;
    }

    private static Vector3d toVector(Vec3 value) {
        return new Vector3d(value.x, value.y, value.z);
    }

    private static boolean isFinite(Vector3d value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    public void applySettings(String enemy, String ally, double radius, double speed) {
        setEnemy(sanitizePattern(enemy));
        setAlly(sanitizePattern(ally));
        orbitRadius = Mth.clamp(radius, MIN_ORBIT_RADIUS, MAX_ORBIT_RADIUS);
        orbitSpeed = Mth.clamp(speed, MIN_ORBIT_SPEED, MAX_ORBIT_SPEED);
        target = null;
        targetScanCooldown = 0;
        setChanged();
    }

    private static String sanitizePattern(@Nullable String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(MAX_PATTERN_LENGTH);
        value.trim().codePoints()
                .filter(codePoint -> codePoint >= 32 && codePoint != 127)
                .limit(MAX_PATTERN_LENGTH)
                .forEach(sanitized::appendCodePoint);
        return sanitized.toString();
    }

    public String getEnemyPattern() {
        return controlseatData.enemy;
    }

    public String getAllyPattern() {
        return controlseatData.ally;
    }

    public double getOrbitRadius() {
        return orbitRadius;
    }

    public double getOrbitSpeed() {
        return orbitSpeed;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vsie.enemy_core");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EnemyCoreContainerMenu(
                id,
                inventory,
                worldPosition,
                getEnemyPattern(),
                getAllyPattern(),
                orbitRadius,
                orbitSpeed
        );
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        // Function: accept saves created before enemy core inherited shared controller IFF storage.
        if (controlseatData.enemy.isEmpty() && tag.contains("EnemyPattern")) {
            setEnemy(sanitizePattern(tag.getString("EnemyPattern")));
        }
        if (controlseatData.ally.isEmpty() && tag.contains("AllyPattern")) {
            setAlly(sanitizePattern(tag.getString("AllyPattern")));
        }
        if (tag.contains("OrbitRadius")) {
            orbitRadius = Mth.clamp(tag.getDouble("OrbitRadius"), MIN_ORBIT_RADIUS, MAX_ORBIT_RADIUS);
        }
        if (tag.contains("OrbitSpeed")) {
            orbitSpeed = Mth.clamp(tag.getDouble("OrbitSpeed"), MIN_ORBIT_SPEED, MAX_ORBIT_SPEED);
        }
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("OrbitRadius", orbitRadius);
        tag.putDouble("OrbitSpeed", orbitSpeed);
    }

    @Override
    public boolean supportsLinkedPeripheralType(int type) {
        return type == ENEMY_CANNON_PERIPHERAL_TYPE;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    private enum TargetType {
        PLAYER,
        SHIP
    }

    private enum AttackPhase {
        ORBITING,
        AIMING,
        RETURNING
    }

    private record Target(TargetType type, UUID id) {
    }

    private record TargetMotion(Vec3 position, Vec3 aimPosition, Vec3 velocity) {
    }

    private record AttackWeapons(
            List<EnemyCannonBlockEntity> readyCannons,
            List<EnemyAutocannonBlockEntity> readyAutocannons,
            boolean hasActiveAutocannonBurst
    ) {
    }
}
