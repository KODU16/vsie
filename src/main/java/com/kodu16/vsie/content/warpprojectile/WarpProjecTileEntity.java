package com.kodu16.vsie.content.warpprojectile;

import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class WarpProjecTileEntity extends Projectile {
    public static final double SPEED_PER_TICK = 3.0D;
    private static final double MIN_TRAVEL_DISTANCE = 1.0D;
    private static final float WARP_PROJECTILE_DEFAULT_RADIUS = 1.0F;
    private static final float WARP_PROJECTILE_FINAL_DEFAULT_RADIUS = 8.0F;
    private static final EntityDataAccessor<Float> TRAVEL_DISTANCE =
            SynchedEntityData.defineId(WarpProjecTileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FX_SOURCE_SIZE =
            SynchedEntityData.defineId(WarpProjecTileEntity.class, EntityDataSerializers.FLOAT);

    private static final ResourceLocation WARP_PROJECTILE_FX = ResourceLocation.fromNamespaceAndPath("vsie", "warp_projectile");
    private static final ResourceLocation WARP_PROJECTILE_FINAL_FX = ResourceLocation.fromNamespaceAndPath("vsie", "warp_projectile_final");

    private double traveledDistance = 0.0D;
    private Vec3 lastNonZeroVelocity = Vec3.ZERO;
    private boolean lifecycleFxStarted = false;
    private boolean finalFxPlayed = false;

    public WarpProjecTileEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void configureLaunch(Vec3 direction, double travelDistance, double fxSourceSize) {
        Vec3 normalizedDirection = direction.lengthSqr() < 1.0E-6D ? Vec3.ZERO : direction.normalize();
        this.setDeltaMovement(normalizedDirection.scale(SPEED_PER_TICK));
        this.lastNonZeroVelocity = this.getDeltaMovement();
        // Function: keep flight range separate from the source bounds used to scale the authored FX radius.
        this.entityData.set(TRAVEL_DISTANCE, (float) Math.max(MIN_TRAVEL_DISTANCE, travelDistance));
        this.entityData.set(FX_SOURCE_SIZE, (float) Math.max(MIN_TRAVEL_DISTANCE, fxSourceSize));
    }

    public static int lifeTicksForDistance(double travelDistance) {
        return Math.max(1, (int) Math.ceil(Math.max(MIN_TRAVEL_DISTANCE, travelDistance) / SPEED_PER_TICK));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAVEL_DISTANCE, (float) MIN_TRAVEL_DISTANCE);
        builder.define(FX_SOURCE_SIZE, (float) MIN_TRAVEL_DISTANCE);
    }

    @Override
    public void tick() {
        super.tick();

        double maxTravelDistance = Math.max(MIN_TRAVEL_DISTANCE, this.entityData.get(TRAVEL_DISTANCE));
        double fxSourceSize = Math.max(MIN_TRAVEL_DISTANCE, this.entityData.get(FX_SOURCE_SIZE));
        if (!lifecycleFxStarted && this.tickCount >= 1 && this.level().isClientSide()) {
            FX fx = FXHelper.getFX(WARP_PROJECTILE_FX);
            if (fx != null) {
                startLifecycleFx(fx, fxSourceSize);
            }
            lifecycleFxStarted = true;
        }

        Vec3 movement = movementForRemainingDistance(maxTravelDistance);
        updateRotationFromVelocity(movement);

        this.setPos(this.position().add(movement));
        this.traveledDistance += movement.length();

        if (!this.level().isClientSide() && this.traveledDistance >= maxTravelDistance - 1.0E-6D) {
            playFinalFx(fxSourceSize);
            this.discard();
        }
    }

    private Vec3 movementForRemainingDistance(double maxTravelDistance) {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-6D) {
            return Vec3.ZERO;
        }

        double remainingDistance = maxTravelDistance - this.traveledDistance;
        if (remainingDistance <= 0.0D) {
            return Vec3.ZERO;
        }

        Vec3 constantMovement = movement.normalize().scale(Math.min(SPEED_PER_TICK, remainingDistance));
        this.setDeltaMovement(constantMovement);
        return constantMovement;
    }

    private void startLifecycleFx(FX fx, double maxTravelDistance) {
        float scale = Math.max(0.01F, (float) (maxTravelDistance * 0.1D / WARP_PROJECTILE_DEFAULT_RADIUS));
        var effect = new EntityEffectExecutor(fx, this.level(), this, EntityEffectExecutor.AutoRotate.XROT);
        // Function: match bullet lifecycle FX playback while scaling the authored radius to the source sublevel bounds.
        effect.setScale(new Vector3f(scale, scale, scale));
        effect.setForcedDeath(false);
        effect.start();
    }

    private void playFinalFx(double maxTravelDistance) {
        if (this.finalFxPlayed) {
            return;
        }
        this.finalFxPlayed = true;

        Vec3 direction = this.lastNonZeroVelocity.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : this.lastNonZeroVelocity.normalize();
        float scale = Math.max(0.01F, (float) (maxTravelDistance / WARP_PROJECTILE_FINAL_DEFAULT_RADIUS));
        // Function: final FX is authored on local Y, so rotate local Y onto the projectile flight direction.
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                WARP_PROJECTILE_FINAL_FX,
                this.getX(), this.getY(), this.getZ(),
                0.0D, 0.0D, 0.0D,
                rotationYToDirection(direction),
                new Vector3f(scale, scale, scale),
                false,
                true
        ));
    }

    private Quaternionf rotationYToDirection(Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : direction.normalize();
        return new Quaternionf().rotationTo(
                0.0F, 1.0F, 0.0F,
                (float) normalized.x, (float) normalized.y, (float) normalized.z
        );
    }

    private void updateRotationFromVelocity(Vec3 velocity) {
        if (velocity.lengthSqr() < 1.0E-6D) {
            return;
        }

        this.lastNonZeroVelocity = velocity;
        float yaw = (float) Math.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG;
        float pitch = (float) Math.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)) * Mth.RAD_TO_DEG;
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (target != null) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Function: warp projectiles must pass through ship/world blocks and expire only by travel distance.
    }
}
