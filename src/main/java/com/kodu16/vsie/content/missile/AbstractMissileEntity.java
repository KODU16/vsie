package com.kodu16.vsie.content.missile;

import com.kodu16.vsie.foundation.ServerShipUtils;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FXHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;

public abstract class AbstractMissileEntity extends AbstractHurtingProjectile implements GeoEntity {
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(AbstractMissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(AbstractMissileEntity.class, EntityDataSerializers.INT);
    private static final float DEFAULT_SPEED = 5.5F;
    private static final float DEFAULT_SPEED_ONSTART = 3.0F;
    private static final float DEFAULT_MAX_TURN_RATE_PER_TICK = 0.15F;
    private static final int GUIDANCE_DELAY_TICKS = 20;
    private static final int MAX_LIFETIME_TICKS = 20 * 10;
    private static final ResourceLocation MISSILE_SWITCH_TRACK_FX = ResourceLocation.fromNamespaceAndPath("vsie", "missile_switchtrack");
    private static final ResourceLocation MISSILE_TRAIL_FX = ResourceLocation.fromNamespaceAndPath("vsie", "missile_trail");

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private SubLevel target;
    private Vec3 currentDirection = null;
    private float maxTurnRatePerTick = DEFAULT_MAX_TURN_RATE_PER_TICK;
    // Function: client-side FX state prevents replaying the transition and trail effects every tick.
    private boolean switchTrackFxStarted = false;
    private boolean trailFxStarted = false;

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

    public void setInitialDirection(Vec3 direction) {
        // Function: give the missile a valid first tick velocity before guidance takes over.
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        this.currentDirection = direction.normalize();
        this.setSpeed(DEFAULT_SPEED_ONSTART);
        this.setDeltaMovement(this.currentDirection.scale(getSpeed()));
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
            int age = this.entityData.get(DATA_AGE) + 1;
            this.entityData.set(DATA_AGE, age);
            if (age >= MAX_LIFETIME_TICKS) {
                explodeAndDiscard(this.position());
                return;
            }
            if (age < GUIDANCE_DELAY_TICKS) {
                setSpeed(DEFAULT_SPEED_ONSTART);
                maintainLaunchDirection();
            } else {
                setSpeed(DEFAULT_SPEED);
                updateGuidance();
            }
        } else {
            handleClientGuidanceFx();
        }
        updateRotationFromMovement(this.getDeltaMovement());
        super.tick();
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
            if (!isGuidanceActive()) {
                // Function: during the straight launch phase, missiles ignore block hits so VLS cells cannot self-detonate them.
                return;
            }
            // Function: after guidance starts, block impact is authoritative on the server and explodes at the contact point.
            explodeAndDiscard(result.getLocation());
        }
    }

    private boolean isGuidanceActive() {
        return this.entityData.get(DATA_AGE) >= GUIDANCE_DELAY_TICKS;
    }

    protected void explodeAndDiscard(Vec3 position) {
        this.level().explode(this,
                position.x, position.y, position.z,
                4.0F,
                Level.ExplosionInteraction.BLOCK
        );
        this.discard();
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
    public boolean displayFireAnimation() {
        // Function: missiles use their Geo model only; hide Minecraft's built-in burning overlay.
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
