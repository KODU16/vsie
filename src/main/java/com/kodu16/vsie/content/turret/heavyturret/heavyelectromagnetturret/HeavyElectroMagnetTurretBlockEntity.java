package com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret;

import com.kodu16.vsie.content.bullet.entity.HeavyElectroMagnetBulletEntity;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.kodu16.vsie.registries.vsieEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import rbasamoyai.ritchiesprojectilelib.effects.screen_shake.ScreenShakeEffect;
import rbasamoyai.ritchiesprojectilelib.network.ClientboundShakeScreenPacket;
import rbasamoyai.ritchiesprojectilelib.network.RPLNetwork;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class HeavyElectroMagnetTurretBlockEntity extends AbstractHeavyTurretBlockEntity {
    private static final double SIDE_FIREPOINT_OFFSET = 2.2D;
    private static final int SALVO_INTERVAL_TICKS = 10;
    private static final int SALVO_SHOT_COUNT = 3;
    private static final int FIRE_SCREEN_SHAKE_TICKS = 10;
    private static final double FIRE_SCREEN_SHAKE_RADIUS = 96.0D;
    private static final float FIRE_SCREEN_SHAKE_YAW = 5F;
    private static final float FIRE_SCREEN_SHAKE_PITCH = 5F;
    private static final float FIRE_SCREEN_SHAKE_ROLL = 0.25F;
    private static final float FIRE_SCREEN_SHAKE_JITTER = 1F;
    // Function: the heavy electromagnetic turret plays one shoot animation when each salvo starts.
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);

    private final Vec3[] pendingFirepoints = new Vec3[SALVO_SHOT_COUNT];
    private Vec3 pendingFireDirection = Vec3.ZERO;
    private int pendingShotIndex = SALVO_SHOT_COUNT;
    private int pendingShotDelay = 0;

    public HeavyElectroMagnetTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 鍒濆鍖?turretData
        this.turretData = new TurretData();
    }

    @Override
    public void tick() {
        super.tick();
        tickPendingSalvo();
    }

    @Override
    public Vec3 getShootLocation(Vec3 vec, List<Vector3d> preV, Level lv, Vec3 pos)  {
        return vec;
    }

    @Override
    public String getturrettype() {
        return "heavy_electromagnet";
    }

    @Override
    public double getYAxisOffset() {
        return 4.5;
    }

    @Override
    public double getcannonlength() {
        return 15;
    }

    @Override
    public float getMaxSpinSpeed() {
        return Mth.PI/256;
    }

    @Override
    public int getCoolDown() {
        return 100;
    }

    @Override
    public int getenergypertick() {
        return 1000;
    }

    @Override
    public void shootentity() {

    }

    @Override
    public void shootship() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        Vec3 target = getHeavyTurretTargetPos();
        Vec3 fireDirection = target.subtract(currentworldpos);
        Vec3 centerFirepoint = getCannonMuzzleWorld(target);
        if (centerFirepoint == null || fireDirection.lengthSqr() < 1.0E-6D) {
            clearPendingSalvo();
            return;
        }

        Vec3 sideDirection = getSideFirepointDirection(fireDirection.normalize());
        // Function: queue a left-to-right three-shot salvo around the calculated center firepoint.
        pendingFirepoints[0] = centerFirepoint.subtract(sideDirection.scale(SIDE_FIREPOINT_OFFSET));
        pendingFirepoints[1] = centerFirepoint;
        pendingFirepoints[2] = centerFirepoint.add(sideDirection.scale(SIDE_FIREPOINT_OFFSET));
        pendingFireDirection = fireDirection.normalize();
        pendingShotIndex = 0;
        pendingShotDelay = 0;
        tickPendingSalvo();
    }

    @Override
    public int getmaxpitchdowndegrees() {
        return 20;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE)
                .triggerableAnim("shoot", SHOOT_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private Vec3 getSideFirepointDirection(Vec3 fireDirection) {
        Vec3 forward = new Vec3(worldXDirection.x, worldXDirection.y, worldXDirection.z);
        Vec3 right = new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z);
        Vec3 planeNormal = forward.cross(right);
        Vec3 sideDirection = planeNormal.cross(fireDirection);
        if (sideDirection.lengthSqr() < 1.0E-6D) {
            sideDirection = right;
        }
        if (sideDirection.dot(right) < 0.0D) {
            sideDirection = sideDirection.scale(-1.0D);
        }
        return sideDirection.normalize();
    }

    private void tickPendingSalvo() {
        if (pendingShotIndex >= SALVO_SHOT_COUNT) {
            return;
        }
        if (pendingShotDelay > 0) {
            pendingShotDelay--;
            return;
        }

        if (pendingShotIndex == 0) {
            // Function: start the salvo animation on the same tick as the first firing position.
            triggerAnim("controller", "shoot");
        }
        Vec3 firepoint = pendingFirepoints[pendingShotIndex];
        spawnHeavyElectroMagnetBullet(firepoint, pendingFireDirection);
        shakeScreenAtFirepoint(firepoint);
        pendingShotIndex++;
        pendingShotDelay = SALVO_INTERVAL_TICKS;
        if (pendingShotIndex >= SALVO_SHOT_COUNT) {
            clearPendingSalvo();
        }
    }

    private void spawnHeavyElectroMagnetBullet(Vec3 firepoint, Vec3 direction) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide || firepoint == null || direction.lengthSqr() < 1.0E-6D) {
            return;
        }

        // Function: projectile velocity stays parallel to the turret-origin-to-target line for this salvo.
        HeavyElectroMagnetBulletEntity bullet = new HeavyElectroMagnetBulletEntity(vsieEntities.HEAVY_ELECTROMAGNETIC_BULLET.get(), level);
        bullet.setPos(firepoint);
        bullet.setDeltaMovement(direction.normalize().scale(HeavyElectroMagnetBulletEntity.SPEED));
        level.addFreshEntity(bullet);
    }

    private void shakeScreenAtFirepoint(Vec3 firepoint) {
        Level level = this.getLevel();
        if (!(level instanceof ServerLevel serverLevel) || firepoint == null) {
            return;
        }

        double radiusSqr = FIRE_SCREEN_SHAKE_RADIUS * FIRE_SCREEN_SHAKE_RADIUS;
        for (ServerPlayer player : serverLevel.players()) {
            double distanceSqr = player.distanceToSqr(firepoint);
            if (distanceSqr > radiusSqr) {
                continue;
            }

            double distanceFactor = 1.0D - Math.sqrt(distanceSqr) / FIRE_SCREEN_SHAKE_RADIUS;
            float scale = (float) Math.max(0.15D, distanceFactor);
            // Function: RPL's default shake handler does not attenuate by position, so scale before sending.
            ScreenShakeEffect effect = new ScreenShakeEffect(
                    FIRE_SCREEN_SHAKE_TICKS,
                    FIRE_SCREEN_SHAKE_YAW * scale,
                    FIRE_SCREEN_SHAKE_PITCH * scale,
                    FIRE_SCREEN_SHAKE_ROLL * scale,
                    FIRE_SCREEN_SHAKE_JITTER * scale,
                    FIRE_SCREEN_SHAKE_JITTER * scale,
                    FIRE_SCREEN_SHAKE_JITTER * scale,
                    firepoint.x,
                    firepoint.y,
                    firepoint.z
            );
            RPLNetwork.sendToClientPlayer(new ClientboundShakeScreenPacket(effect), player);
        }
    }

    private void clearPendingSalvo() {
        for (int i = 0; i < pendingFirepoints.length; i++) {
            pendingFirepoints[i] = null;
        }
        pendingFireDirection = Vec3.ZERO;
        pendingShotIndex = SALVO_SHOT_COUNT;
        pendingShotDelay = 0;
    }
}
