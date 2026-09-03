package com.kodu16.vsie.content.missile;

import com.kodu16.vsie.content.missile.client.MissileSoundManager;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.projectile.ForwardChunkLoadController;
import com.kodu16.vsie.registries.vsieSounds;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FXHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;

import java.util.UUID;

public abstract class AbstractMissileEntity extends AbstractHurtingProjectile implements GeoEntity {
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(AbstractMissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(AbstractMissileEntity.class, EntityDataSerializers.INT);
    private static final float DEFAULT_SPEED = 5.5F;
    private static final float DEFAULT_SPEED_ONSTART = 3.0F;
    private static final float DEFAULT_MAX_TURN_RATE_PER_TICK = 0.15F;
    private static final int GUIDANCE_DELAY_TICKS = 20;
    private static final int MAX_LIFETIME_TICKS = 20 * 15;
    private static final float MISSILE_EXPLOSION_POWER = 16.0F;
    private static final double IMPACT_BLOCK_BREAK_RADIUS = 5.0D;
    private static final double TARGET_PROXIMITY_FUSE_RADIUS = 1.5D;
    private static final ResourceLocation MISSILE_SWITCH_TRACK_FX = ResourceLocation.fromNamespaceAndPath("vsie", "missile_switchtrack");
    private static final ResourceLocation MISSILE_TRAIL_FX = ResourceLocation.fromNamespaceAndPath("vsie", "missile_trail");

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private SubLevel target;
    private UUID launchSubLevelId;
    private Vec3 currentDirection = null;
    private float maxTurnRatePerTick = DEFAULT_MAX_TURN_RATE_PER_TICK;
    // Function: client-side FX state prevents replaying the transition and trail effects every tick.
    private boolean switchTrackFxStarted = false;
    private boolean trailFxStarted = false;
    private boolean launchSoundPlayed = false;
    private final ForwardChunkLoadController forwardChunkLoader = new ForwardChunkLoadController();

    public float xRot0 = 0f;
    public float yRot0 = 0f;
    public static SerializableDataTicket<Double> MISSILE_MOMENTUM_X;
    public static SerializableDataTicket<Double> MISSILE_MOMENTUM_Y;
    public static SerializableDataTicket<Double> MISSILE_MOMENTUM_Z;

    public AbstractMissileEntity(EntityType<? extends AbstractMissileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // Function: missiles launch with the slower start speed, then switch to homing speed after the delay.
        builder.define(DATA_SPEED, DEFAULT_SPEED_ONSTART);
        builder.define(DATA_AGE, 0);
    }

    public void setTarget(SubLevel ship) {
        this.target = ship;
    }

    public void setLaunchSubLevel(SubLevel subLevel) {
        this.launchSubLevelId = subLevel == null ? null : subLevel.getUniqueId();
    }

    public void setInitialDirection(Vec3 direction) {
        // Function: give the missile a valid first tick velocity before guidance takes over.
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        this.currentDirection = direction.normalize();
        this.setSpeed(DEFAULT_SPEED_ONSTART);
        this.setDeltaMovement(this.currentDirection.scale(getSpeed()));
        updateForwardChunkLoading();
        updateRotationFromMovement(this.getDeltaMovement());
    }

    public abstract String getmissiletype();

    public void setMaxTurnRate(float radiansPerTick) {
        this.maxTurnRatePerTick = radiansPerTick;
    }

    public float getSpeed() {
        return this.entityData.get(DATA_SPEED);
    }

    public void setSpeed(float speed) {
        this.entityData.set(DATA_SPEED, speed);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            playLaunchSoundOnce();
            int age = this.entityData.get(DATA_AGE) + 1;
            this.entityData.set(DATA_AGE, age);
            if (age >= MAX_LIFETIME_TICKS) {
                detonateAt(this.position());
                return;
            }
            if (age < GUIDANCE_DELAY_TICKS) {
                setSpeed(DEFAULT_SPEED_ONSTART);
                maintainLaunchDirection();
            } else {
                setSpeed(DEFAULT_SPEED);
                if (tryDetonateAtTargetPoint()) {
                    return;
                }
                updateGuidance();
            }
        } else {
            handleClientGuidanceFx();
            handleClientLoopSound();
        }
        updateForwardChunkLoading();
        updateRotationFromMovement(this.getDeltaMovement());
        // AbstractHurtingProjectile performs a swept hit test across the complete movement vector.
        super.tick();
    }

    private void updateForwardChunkLoading() {
        if (this.level() instanceof ServerLevel serverLevel) {
            // Function: guided turns rebuild the diagonal-safe force-loaded corridor every tick.
            forwardChunkLoader.update(serverLevel, this.position(), this.getDeltaMovement());
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClientLoopSound() {
        MissileSoundManager.updateMissile(this);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClientGuidanceFx() {
        if (this.tickCount < GUIDANCE_DELAY_TICKS) {
            return;
        }
        if (!switchTrackFxStarted) {
            // Function: play the handoff flash once when straight launch becomes guided flight.
            startEntityFx(MISSILE_SWITCH_TRACK_FX);
            switchTrackFxStarted = true;
        }
        if (!trailFxStarted) {
            // Function: keep missile_trail.fx attached for the rest of the guided flight.
            startEntityFx(MISSILE_TRAIL_FX);
            trailFxStarted = true;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void startEntityFx(ResourceLocation fxId) {
        var fx = FXHelper.getFX(fxId);
        if (fx == null) {
            return;
        }
        var effect = new EntityEffectExecutor(fx, this.level(), this, EntityEffectExecutor.AutoRotate.XROT);
        effect.setForcedDeath(false);
        effect.start();
    }

    private void updateGuidance() {
        Vec3 idealDirection = getTargetPosition()
                .map(targetPos -> targetPos.subtract(this.position()))
                .filter(vector -> vector.lengthSqr() > 1.0E-6D)
                .map(Vec3::normalize)
                .orElse(currentDirection);

        if (idealDirection == null) {
            return;
        }
        if (currentDirection == null || currentDirection.lengthSqr() < 1.0E-6D) {
            currentDirection = idealDirection;
        } else {
            currentDirection = limitTurnRate(currentDirection, idealDirection, maxTurnRatePerTick);
        }

        this.setDeltaMovement(currentDirection.scale(getSpeed()));
        if (MISSILE_MOMENTUM_X != null && MISSILE_MOMENTUM_Y != null && MISSILE_MOMENTUM_Z != null) {
            // Function: animation momentum tickets are optional; guidance must not depend on renderer setup.
            setAnimData(MISSILE_MOMENTUM_X, currentDirection.x());
            setAnimData(MISSILE_MOMENTUM_Y, currentDirection.y());
            setAnimData(MISSILE_MOMENTUM_Z, currentDirection.z());
        }
    }

    private void maintainLaunchDirection() {
        if (currentDirection == null || currentDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        // Function: keep the one-second launch phase at the same velocity before homing takes over.
        this.setDeltaMovement(currentDirection.normalize().scale(getSpeed()));
    }

    private java.util.Optional<Vec3> getTargetPosition() {
        if (target == null) {
            return java.util.Optional.empty();
        }
        Vec3 center = target instanceof ServerSubLevel serverSubLevel
                ? ServerShipUtils.getCenterOfMassWorld(serverSubLevel)
                : null;
        if (center == null) {
            center = ServerShipUtils.getStructureCenterWorld(target);
        }
        return java.util.Optional.ofNullable(center);
    }

    private boolean tryDetonateAtTargetPoint() {
        return getTargetPosition()
                .filter(targetPos -> {
                    double fuseRadius = Math.max(TARGET_PROXIMITY_FUSE_RADIUS, this.getDeltaMovement().length());
                    // Function: detonate once the missile can reach the target point within this tick so hollow targets cannot trap it in an orbit.
                    return this.position().distanceToSqr(targetPos) <= fuseRadius * fuseRadius;
                })
                .map(targetPos -> {
                    detonateAt(targetPos);
                    return true;
                })
                .orElse(false);
    }

    private Vec3 limitTurnRate(Vec3 current, Vec3 ideal, float maxTurnRate) {
        double dotProduct = Mth.clamp(current.dot(ideal), -1.0D, 1.0D);
        double angle = Math.acos(dotProduct);
        if(angle <= maxTurnRate) {
            return ideal;
        }

        double t = maxTurnRate / angle;
        double sinAngle = Math.sin(angle);
        if (Math.abs(sinAngle) < 1.0E-6D) {
            return ideal;
        }

        double factorCurrent = Math.sin((1.0D - t) * angle) / sinAngle;
        double factorIdeal = Math.sin(t * angle) / sinAngle;
        return new Vec3(
                current.x * factorCurrent + ideal.x * factorIdeal,
                current.y * factorCurrent + ideal.y * factorIdeal,
                current.z * factorCurrent + ideal.z * factorIdeal
        ).normalize();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            if (isLaunchSubLevelBlock(result.getBlockPos())) {
                // Function: missiles pass through their own launch ship without detonation or block damage.
                return;
            }
            if (!isGuidanceActive()) {
                // Function: during the straight launch phase, missiles ignore block hits so VLS cells cannot self-detonate them.
                return;
            }
            if (shouldDetonateOnBlockHit(result)) {
                // Function: after guidance starts, impacts outside the launch ship detonate at the exact contact point.
                detonateAt(result.getLocation());
            }
        }
    }

    private boolean isGuidanceActive() {
        return this.entityData.get(DATA_AGE) >= GUIDANCE_DELAY_TICKS;
    }

    protected void detonateAt(Vec3 position) {
        if (this.level() instanceof ServerLevel serverLevel) {
            destroyBlocksInSphere(serverLevel, position, IMPACT_BLOCK_BREAK_RADIUS);
            serverLevel.explode(
                    this,
                    position.x,
                    position.y,
                    position.z,
                    MISSILE_EXPLOSION_POWER,
                    false,
                    Level.ExplosionInteraction.NONE
            );
        }
        this.discard();
    }

    private void playLaunchSoundOnce() {
        if (launchSoundPlayed) {
            return;
        }

        launchSoundPlayed = true;
        this.level().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                vsieSounds.MISSILE_DECOUPLER_FIRE.get(),
                SoundSource.HOSTILE,
                0.9F,
                1.0F
        );
    }

    private boolean shouldDetonateOnBlockHit(BlockHitResult result) {
        SubLevel hitSubLevel = ServerShipUtils.getSubLevelAtBlockPos(this.level(), result.getBlockPos());
        if (hitSubLevel == null) {
            return true;
        }
        if (launchSubLevelId == null) {
            return true;
        }
        return !launchSubLevelId.equals(hitSubLevel.getUniqueId());
    }

    private boolean isLaunchSubLevelBlock(BlockPos pos) {
        if (launchSubLevelId == null) {
            return false;
        }
        SubLevel hitSubLevel = ServerShipUtils.getSubLevelAtBlockPos(this.level(), pos);
        return hitSubLevel != null && launchSubLevelId.equals(hitSubLevel.getUniqueId());
    }

    protected void destroyBlocksInSphere(ServerLevel level, Vec3 impactPoint, double radius) {
        double safeRadius = Math.max(0.0D, radius);
        int blockRadius = (int) Math.ceil(safeRadius);
        BlockPos center = BlockPos.containing(impactPoint);
        double radiusSqr = safeRadius * safeRadius;

        for (int x = -blockRadius; x <= blockRadius; x++) {
            for (int y = -blockRadius; y <= blockRadius; y++) {
                for (int z = -blockRadius; z <= blockRadius; z++) {
                    if (safeRadius > 0.0D && x * x + y * y + z * z > radiusSqr) {
                        continue;
                    }
                    BlockPos targetPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(targetPos);
                    if (!canMissileBreakBlock(level, targetPos, state)) {
                        continue;
                    }
                    if (level.random.nextDouble() > computeBlockBreakProbability(level, targetPos, impactPoint, safeRadius)) {
                        continue;
                    }
                    breakBlockAsMined(level, targetPos);
                }
            }
        }
    }

    protected boolean canMissileBreakBlock(ServerLevel level, BlockPos pos, BlockState state) {
        return level.isLoaded(pos) && !state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    protected double computeBlockBreakProbability(ServerLevel level, BlockPos pos, Vec3 impactPoint, double maxDistance) {
        BlockState state = level.getBlockState(pos);
        double hardness = Math.max(0.0D, state.getDestroySpeed(level, pos));
        double distanceRatio = maxDistance <= 1.0E-6D ? 0.0D : Vec3.atCenterOf(pos).distanceTo(impactPoint) / maxDistance;
        // Function: match bullet terrain clearing so missile blasts thin out with distance and block hardness.
        return Mth.clamp((1.2D - distanceRatio) * (1.2D - hardness / 100.0D), 0.0D, 1.0D);
    }

    protected boolean breakBlockAsMined(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!canMissileBreakBlock(level, pos, state)) {
            return false;
        }
        level.levelEvent(2001, pos, Block.getId(state));
        return level.destroyBlock(pos, false, this);
    }

    protected void updateRotationFromMovement(Vec3 movement) {
        if (movement.lengthSqr() < 1.0E-6D) {
            return;
        }
        float yaw = (float) Math.atan2(movement.x, movement.z) * Mth.RAD_TO_DEG;
        float pitch = (float) Math.atan2(movement.y, Math.sqrt(movement.x * movement.x + movement.z * movement.z)) * Mth.RAD_TO_DEG;
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    public void remove(Entity.RemovalReason removalReason) {
        forwardChunkLoader.release();
        if (this.level() != null && this.level().isClientSide) {
            stopClientLoopSound();
        }
        super.remove(removalReason);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Function: every missile subclass bypasses vanilla distance culling while the server tracks it.
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private void stopClientLoopSound() {
        MissileSoundManager.stopMissile(this);
    }

    @Override
    public boolean displayFireAnimation() {
        // Function: missiles use their Geo model only; hide Minecraft's built-in burning overlay.
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (launchSubLevelId != null) {
            tag.putUUID("LaunchSubLevelId", launchSubLevelId);
        }
        tag.putBoolean("LaunchSoundPlayed", launchSoundPlayed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("LaunchSubLevelId")) {
            launchSubLevelId = tag.getUUID("LaunchSubLevelId");
        } else {
            launchSubLevelId = null;
        }
        if (tag.contains("LaunchSoundPlayed")) {
            launchSoundPlayed = tag.getBoolean("LaunchSoundPlayed");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
