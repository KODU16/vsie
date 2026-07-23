package com.kodu16.vsie.content.turret.ciws.basicciws;

import com.kodu16.vsie.content.turret.ciws.AbstractCIWSBlockEntity;
import com.kodu16.vsie.content.turret.ciws.basicciws.client.BasicCiwsSoundManager;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
    private static final String LOOP_SOUND_ACTIVE_TAG = "ciwsLoopSoundActive";

    private boolean firedThisTick;
    private boolean loopSoundActive;
    private boolean shootAnimationActive;
    private Vec3 queuedFirepoint;
    private Vec3 queuedFireDirection;
    private boolean firingEffectsLatched;
    private Entity activeInterceptProjectile;
    private int activeInterceptFireTicks;
    private int activeInterceptRequiredFireTicks = 1;

    public BasicCIWSBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        Level level = this.getLevel();
        if (level != null && level.isClientSide()) {
            BasicCiwsSoundManager.updateCiws(this);
        }
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
        if (level != null && !level.isClientSide()) {
            syncLoopSoundState(firedThisTick);
        }
    }

    public boolean isLoopSoundActive() {
        return loopSoundActive;
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
        return Mth.PI/12;
    }

    @Override
    public int getCoolDown() {
        return 0;
    }

    @Override
    public int getenergypertick() {
        return 10;
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
        if (queueCurrentBarrelFiringEffects()) {
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
        if (queueCurrentBarrelFiringEffects()) {
            return;
        }

        // Function: projectile cleanup must respect the lock-time budget derived from the original lock distance.
        activeInterceptFireTicks++;
        if (activeInterceptFireTicks >= activeInterceptRequiredFireTicks && alignment >= HIT_ALIGNMENT_THRESHOLD) {
            projectile.discard();
            clearTargetProjectile();
            activeInterceptProjectile = null;
            activeInterceptFireTicks = 0;
            activeInterceptRequiredFireTicks = 1;
        }

    }

    @Override
    protected void onProjectileTargetLocked(Entity projectile, double lockDistance) {
        // Function: freeze the minimum interception time from the first lock distance so later approach does not shorten it.
        activeInterceptRequiredFireTicks = Math.max(1, Mth.ceil(lockDistance / 10.0D));
        if (projectile != activeInterceptProjectile) {
            activeInterceptProjectile = projectile;
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
        if (barrelDirection == null || barrelDirection.lengthSqr() < 1.0E-6 || targetDirection.lengthSqr() < 1.0E-6) {
            return -1.0D;
        }

        // Function: keep CIWS fire thresholds based on turret rotation, not on the synthetic muzzle endpoint.
        return barrelDirection.normalize().dot(targetDirection.normalize());
    }

    private boolean queueCurrentBarrelFiringEffects() {
        BarrelFireEffect effect = resolveCurrentBarrelFireEffect();
        if (effect == null) {
            return true;
        }

        // Function: latch only the firing state; each tick recomputes FX from the live barrel pose during retargeting.
        queuedFirepoint = effect.firepoint();
        queuedFireDirection = effect.direction();
        firingEffectsLatched = true;
        return false;
    }

    private void playQueuedFiringEffects() {
        if (queuedFirepoint == null || queuedFireDirection == null) {
            return;
        }

        playFiringEffects(queuedFirepoint, queuedFireDirection);
    }

    private void playLatchedFiringEffects() {
        BarrelFireEffect effect = resolveCurrentBarrelFireEffect();
        if (effect == null) {
            return;
        }

        playFiringEffects(effect.firepoint(), effect.direction());
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
        if (!firingEffectsLatched) {
            return false;
        }

        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }

        // Function: once CIWS has opened fire, visuals stay live through target swaps until no valid target remains.
        return hasValidTargetForCiwsFire();
    }

    private void stopCiwsFire() {
        firingEffectsLatched = false;
        activeInterceptProjectile = null;
        activeInterceptFireTicks = 0;
        activeInterceptRequiredFireTicks = 1;
        if (shootAnimationActive) {
            // Function: stop the looped fire animation as soon as the turret loses lock or target.
            stopTriggeredAnim("controller", "shoot");
            shootAnimationActive = false;
        }
    }

    private void syncLoopSoundState(boolean firingNow) {
        if (loopSoundActive == firingNow) {
            return;
        }

        // Function: mirror the sustained-fire state to clients so the CIWS loop can start and stop immediately.
        loopSoundActive = firingNow;
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean(LOOP_SOUND_ACTIVE_TAG, loopSoundActive);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(LOOP_SOUND_ACTIVE_TAG)) {
            loopSoundActive = tag.getBoolean(LOOP_SOUND_ACTIVE_TAG);
        }
    }

    private BarrelFireEffect resolveCurrentBarrelFireEffect() {
        Vec3 direction = getCurrentBarrelDirectionWorld();
        if (direction == null || direction.lengthSqr() < 1.0E-6D) {
            return null;
        }

        Vec3 firepoint = getBarrelMuzzleWorld(direction);
        return firepoint == null ? null : new BarrelFireEffect(firepoint, direction.normalize());
    }

    private Vec3 getBarrelMuzzleWorld(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D) {
            return null;
        }

        // Function: match the heavy electromagnetic turret by projecting the FX origin from the live barrel axis.
        return getTurretAimOriginWorld().add(direction.normalize().scale(getcannonlength()));
    }

    private record BarrelFireEffect(Vec3 firepoint, Vec3 direction) {
    }
}
