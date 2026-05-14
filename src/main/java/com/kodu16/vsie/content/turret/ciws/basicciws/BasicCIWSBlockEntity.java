package com.kodu16.vsie.content.turret.ciws.basicciws;

import com.kodu16.vsie.content.turret.ciws.AbstractCIWSBlockEntity;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class BasicCIWSBlockEntity extends AbstractCIWSBlockEntity {
    private static final ResourceLocation CIWS_FIRE_FX = ResourceLocation.fromNamespaceAndPath(vsie.ID, "ciws_fire");
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.LOOP);
    private static final float ENTITY_DAMAGE = 10.0F;
    private static final double FIRE_ALIGNMENT_THRESHOLD = 0.7D;
    private static final double HIT_ALIGNMENT_THRESHOLD = 0.9D;
    private static final int PROJECTILE_INTERCEPT_FIRE_TICKS = 2;

    private boolean firedThisTick;
    private boolean shootAnimationActive;
    private Vec3 queuedFirepoint;
    private Vec3 queuedFireDirection;
    private Vec3 activeFirepoint;
    private Vec3 activeFireDirection;
    private Entity activeInterceptProjectile;
    private int activeInterceptFireTicks;

    public BasicCIWSBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        firedThisTick = false;
        queuedFirepoint = null;
        queuedFireDirection = null;
        super.tick();
        playQueuedFiringEffects();
        if (!firedThisTick && canKeepPlayingLatchedFireFx()) {
            playLatchedFiringEffects();
        }
        if (!firedThisTick) {
            stopCiwsFire();
        }
    }

    @Override
    public Vec3 getShootLocation(Vec3 vec, List<Vector3d> preV, Level lv, Vec3 selfpos) {
        return vec;
    }

    @Override
    public String getturrettype() {
        return "basic_ciws";
    }

    @Override
    public double getYAxisOffset() {
        return 2.5;
    }

    @Override
    public double getcannonlength() {
        return 2;
    }

    @Override
    public float getMaxSpinSpeed() {
        return Mth.PI/3;
    }

    @Override
    public int getCoolDown() {
        return 0;
    }

    @Override
    public int getenergypertick() {
        return 100;
    }

    @Override
    public void shootentity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide || targetentity == null || !targetentity.isAlive()) {
            stopCiwsFire();
            return;
        }

        Vec3 firepoint = getCannonMuzzleWorld(targetPos);
        if (firepoint == null) {
            return;
        }
        double alignment = getBarrelAimAlignment(targetPos);
        if (alignment < FIRE_ALIGNMENT_THRESHOLD) {
            return;
        }
        Vec3 direction = targetPos.subtract(firepoint);
        if (queueFiringEffects(firepoint, direction)) {
            return;
        }

        if (alignment >= HIT_ALIGNMENT_THRESHOLD) {
            targetentity.hurt(level.damageSources().magic(), ENTITY_DAMAGE);
        }

    }

    @Override
    public void shootship() {

    }

    @Override
    public void interceptprojectile() {
        Level level = this.getLevel();
        Entity projectile = getTargetProjectile();
        if (level == null || level.isClientSide || projectile == null || !isTargetProjectileValidForFire()) {
            stopCiwsFire();
            return;
        }
        if (projectile != activeInterceptProjectile) {
            activeInterceptProjectile = projectile;
            activeInterceptFireTicks = 0;
        }

        Vec3 firepoint = getCannonMuzzleWorld(targetPos);
        if (firepoint == null) {
            return;
        }
        double alignment = getBarrelAimAlignment(targetPos);
        if (alignment < FIRE_ALIGNMENT_THRESHOLD) {
            return;
        }
        Vec3 direction = targetPos.subtract(firepoint);
        if (queueFiringEffects(firepoint, direction)) {
            return;
        }

        // Function: keep projectile interception visible for several firing ticks before removing the target.
        activeInterceptFireTicks++;
        if (activeInterceptFireTicks >= PROJECTILE_INTERCEPT_FIRE_TICKS && alignment >= HIT_ALIGNMENT_THRESHOLD) {
            projectile.discard();
            clearTargetProjectile();
            activeInterceptProjectile = null;
            activeInterceptFireTicks = 0;
        }

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE)
                .triggerableAnim("shoot", SHOOT_ANIMATION));
    }

    @Override
    protected boolean canFireAtCurrentAim() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }
        return getCannonMuzzleWorld(targetPos) != null && getBarrelAimAlignment(targetPos) >= FIRE_ALIGNMENT_THRESHOLD;
    }

    private double getBarrelAimAlignment(Vec3 target) {
        Vec3 barrelDirection = getCurrentBarrelDirectionWorld();
        Vec3 targetDirection = target.subtract(getTurretAimOriginWorld());
        if (barrelDirection.lengthSqr() < 1.0E-6 || targetDirection.lengthSqr() < 1.0E-6) {
            return -1.0D;
        }

        // Function: keep CIWS fire thresholds based on turret rotation, not on the synthetic muzzle endpoint.
        return barrelDirection.normalize().dot(targetDirection.normalize());
    }

    private Vec3 getCurrentBarrelDirectionWorld() {
        double yaw = -yRot0;
        double pitch = xRot0;
        double horizontal = Math.cos(pitch);
        double localX = Math.sin(yaw) * horizontal;
        double localY = Math.sin(pitch);
        double localZ = Math.cos(yaw) * horizontal;

        // Function: rebuild the current barrel forward vector from the same world basis used by target rotation.
        return new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z).scale(localX)
                .add(new Vec3(worldYDirection.x, worldYDirection.y, worldYDirection.z).scale(localY))
                .add(new Vec3(worldXDirection.x, worldXDirection.y, worldXDirection.z).scale(localZ));
    }

    private boolean queueFiringEffects(Vec3 firepoint, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6) {
            return true;
        }

        // Function: latch the last real firing point so FX keeps playing every tick until target loss.
        queuedFirepoint = firepoint;
        queuedFireDirection = direction;
        activeFirepoint = firepoint;
        activeFireDirection = direction;
        return false;
    }

    private void playQueuedFiringEffects() {
        if (queuedFirepoint == null || queuedFireDirection == null) {
            //LogUtils.getLogger().warn("play queued fx:false");
            return;
        }
        //LogUtils.getLogger().warn("play queued fx:true");

        playFiringEffects(queuedFirepoint, queuedFireDirection);
    }

    private void playLatchedFiringEffects() {
        if (activeFirepoint == null || activeFireDirection == null) {
            //LogUtils.getLogger().warn("play latched fx:false");
            return;
        }
        //LogUtils.getLogger().warn("play latched fx:true");
        playFiringEffects(activeFirepoint, activeFireDirection);
    }

    private void playFiringEffects(Vec3 firepoint, Vec3 direction) {
        Vec3 normalizedDirection = direction.normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(
                0.0F, 1.0F, 0.0F,
                (float) normalizedDirection.x,
                (float) normalizedDirection.y,
                (float) normalizedDirection.z
        );
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                CIWS_FIRE_FX,
                firepoint.x,
                firepoint.y,
                firepoint.z,
                0.0D,
                0.0D,
                0.0D,
                rotation,
                new Vector3f(1.0F, 1.0F, 1.0F),
                false,
                true
        ));

        firedThisTick = true;
        if (!shootAnimationActive) {
            triggerAnim("controller", "shoot");
            shootAnimationActive = true;
        }
    }

    private boolean canKeepPlayingLatchedFireFx() {
        if (activeFirepoint == null || activeFireDirection == null) {
            return false;
        }

        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }

        // Function: after CIWS starts firing, only target loss stops the per-tick FX stream.
        if (aimtype == 1) {
            return isValidTargetEntity(targetentity);
        }
        if (aimtype == 2) {
            return isTargetProjectileValidForFire();
        }
        return false;
    }

    private void stopCiwsFire() {
        activeFirepoint = null;
        activeFireDirection = null;
        activeInterceptProjectile = null;
        activeInterceptFireTicks = 0;
        if (shootAnimationActive) {
            // Function: stop the looped fire animation as soon as the turret loses lock or target.
            stopTriggeredAnim("controller", "shoot");
            shootAnimationActive = false;
        }
    }
}
