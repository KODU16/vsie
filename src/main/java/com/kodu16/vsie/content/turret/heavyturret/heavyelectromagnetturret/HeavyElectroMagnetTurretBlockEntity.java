package com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret;

import com.kodu16.vsie.content.bullet.entity.HeavyElectroMagnetBulletEntity;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.ModParticleTypes;
import com.kodu16.vsie.registries.vsieBlocks;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
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
    private static final double PROJECTILE_MUZZLE_FORWARD_OFFSET = 0.35D;
    private static final int TURRET_BULLET_MAX_LIFETIME_TICKS = 80;
    private static final int MUZZLE_SMOKE_PARTICLE_COUNT = 24;
    private static final double MUZZLE_SMOKE_VISIBLE_DISTANCE = 220.0D;
    private static final double MUZZLE_SMOKE_BASE_SPEED = 0.55D;
    private static final double MUZZLE_SMOKE_SPREAD = 0.16D;
    private static final int SALVO_INTERVAL_TICKS = 10;
    private static final int SALVO_SHOT_COUNT = 3;
    private static final int FIRE_SCREEN_SHAKE_TICKS = 10;
    private static final double FIRE_SCREEN_SHAKE_RADIUS = 96.0D;
    private static final float FIRE_SCREEN_SHAKE_YAW = 5F;
    private static final float FIRE_SCREEN_SHAKE_PITCH = 5F;
    private static final float FIRE_SCREEN_SHAKE_ROLL = 0.25F;
    private static final float FIRE_SCREEN_SHAKE_JITTER = 1F;
    private static final ResourceLocation ARC_SPARK_FX = ResourceLocation.fromNamespaceAndPath("vsie", "arc_spark");
    // Function: the heavy electromagnetic turret plays one shoot animation when each salvo starts.
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);

    private final Vec3[] pendingFirepoints = new Vec3[SALVO_SHOT_COUNT];
    private Vec3 pendingFireDirection = Vec3.ZERO;
    private int pendingShotIndex = SALVO_SHOT_COUNT;
    private int pendingShotDelay = 0;

    public HeavyElectroMagnetTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
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
    public boolean isEnergyTurret() {
        // Function: heavy electromagnetic salvos consume dedicated shell ammo instead of energy only.
        return false;
    }

    @Override
    public Item getAmmoItem() {
        return vsieBlocks.ELECTRO_MAGNET_SHELL_BLOCK.asItem();
    }

    @Override
    public String getturrettype() {
        return "heavy_electromagnet";
    }

    @Override
    public double getYAxisOffset() {
        return 4.7;
    }

    @Override
    public double getcannonlength() {
        return 15;
    }

    @Override
    public float getMaxSpinSpeed() {
        // Function: keep the heavy electromagnetic barrels fixed until all queued salvo shots leave the muzzle.
        if (hasPendingSalvo()) {
            return 0.0F;
        }
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
        queueSalvoAtTarget(getHeavyTurretTargetPos());
    }

    @Override
    protected void shootManualTarget(Vec3 fireTarget) {
        // Function: manual and smart-manual fire along the visible barrel, even before the turret reaches the sight point.
        queueSalvoAlongCurrentBarrel(fireTarget);
    }

    @Override
    protected boolean consumeAmmoForShot() {
        // Function: a queued three-shot salvo must finish before another ammo/cooldown cycle can start.
        return !hasPendingSalvo() && super.consumeAmmoForShot();
    }

    private void queueSalvoAtTarget(Vec3 target) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (hasPendingSalvo() || target == null) {
            return;
        }

        Vec3 fireDirection = resolveBarrelFireDirection(target);
        if (fireDirection == null || fireDirection.lengthSqr() < 1.0E-6D) {
            clearPendingSalvo();
            return;
        }

        fireDirection = fireDirection.normalize();
        Vec3 centerFirepoint = currentworldpos.add(fireDirection.scale(getcannonlength()));
        Vec3 sideDirection = getSideFirepointDirection(fireDirection);
        queueAnimationOrderedSalvo(centerFirepoint, sideDirection, fireDirection);
    }

    private void queueSalvoAlongCurrentBarrel(Vec3 fallbackTarget) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide || hasPendingSalvo()) {
            return;
        }

        Vec3 fireDirection = getCurrentBarrelDirectionWorld();
        if ((fireDirection == null || fireDirection.lengthSqr() < 1.0E-6D) && fallbackTarget != null) {
            fireDirection = fallbackTarget.subtract(currentworldpos);
        }
        if (fireDirection == null || fireDirection.lengthSqr() < 1.0E-6D) {
            clearPendingSalvo();
            return;
        }

        fireDirection = fireDirection.normalize();
        Vec3 centerFirepoint = currentworldpos.add(fireDirection.scale(getcannonlength()));
        Vec3 sideDirection = getSideFirepointDirection(fireDirection);
        queueAnimationOrderedSalvo(centerFirepoint, sideDirection, fireDirection);
    }

    private void queueAnimationOrderedSalvo(Vec3 centerFirepoint, Vec3 rightDirection, Vec3 fireDirection) {
        boolean reverseOrder = shouldReverseSalvoOrderForYaw();
        Vec3 firstSide = reverseOrder ? rightDirection : rightDirection.scale(-1.0D);
        Vec3 thirdSide = firstSide.scale(-1.0D);
        // Function: yaw 0-90 and 270-360 visually mirror left/right, so compensate to keep animation order stable.
        pendingFirepoints[0] = centerFirepoint.add(firstSide.scale(SIDE_FIREPOINT_OFFSET));
        pendingFirepoints[1] = centerFirepoint;
        pendingFirepoints[2] = centerFirepoint.add(thirdSide.scale(SIDE_FIREPOINT_OFFSET));
        pendingFireDirection = fireDirection;
        pendingShotIndex = 0;
        pendingShotDelay = 0;
    }

    private boolean shouldReverseSalvoOrderForYaw() {
        float yaw = normalizeYawPositive(yRot0);
        return yaw < Mth.HALF_PI || yaw >= Mth.HALF_PI * 3.0F;
    }

    private float normalizeYawPositive(float yaw) {
        float normalized = yaw % Mth.TWO_PI;
        return normalized < 0.0F ? normalized + Mth.TWO_PI : normalized;
    }

    private @Nullable Vec3 resolveBarrelFireDirection(Vec3 fallbackTarget) {
        Vec3 fireDirection = getCurrentBarrelDirectionWorld();
        if ((fireDirection == null || fireDirection.lengthSqr() < 1.0E-6D) && fallbackTarget != null) {
            fireDirection = fallbackTarget.subtract(currentworldpos);
        }
        return fireDirection;
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
        Vec3 right = new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z);
        Vec3 normalizedFireDirection = fireDirection.normalize();
        Vec3 sideDirection = right.subtract(normalizedFireDirection.scale(right.dot(normalizedFireDirection)));
        if (sideDirection.lengthSqr() < 1.0E-6D) {
            sideDirection = right;
        }
        // Function: sideDirection always points to the turret's local right, projected onto the muzzle plane.
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

        Vec3 firepoint = pendingFirepoints[pendingShotIndex];
        if (!spawnHeavyElectroMagnetBullet(firepoint, pendingFireDirection)) {
            clearPendingSalvo();
            return;
        }
        if (pendingShotIndex == 0) {
            // Function: start the salvo animation only after the first projectile is accepted by the world.
            triggerAnim("controller", "shoot");
        }
        playBarrelFireSound(firepoint);
        playArcSparkFx(firepoint, pendingFireDirection);
        spawnBarrelMuzzleSmoke(firepoint, pendingFireDirection);
        shakeScreenAtFirepoint(firepoint);
        pendingShotIndex++;
        pendingShotDelay = SALVO_INTERVAL_TICKS;
        if (pendingShotIndex >= SALVO_SHOT_COUNT) {
            clearPendingSalvo();
        }
    }

    private boolean spawnHeavyElectroMagnetBullet(Vec3 firepoint, Vec3 direction) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide || firepoint == null || direction.lengthSqr() < 1.0E-6D) {
            return false;
        }

        Vec3 normalizedDirection = direction.normalize();
        // Function: projectile velocity and muzzle smoke both use the same barrel-aligned axis for this salvo.
        HeavyElectroMagnetBulletEntity bullet = new HeavyElectroMagnetBulletEntity(vsieEntities.HEAVY_ELECTROMAGNETIC_BULLET.get(), level);
        // Function: spawn just in front of the muzzle so unaligned manual fire cannot immediately collide with the turret body.
        bullet.setPos(firepoint.add(normalizedDirection.scale(PROJECTILE_MUZZLE_FORWARD_OFFSET)));
        // Function: heavy-turret misses should disappear quickly instead of leaving dozens of long-lived projectile FX in the world.
        bullet.configureMaxLifeTimeTicks(TURRET_BULLET_MAX_LIFETIME_TICKS);
        bullet.configureLifetimeExpireExplosion(false);
        bullet.setLaunchSubLevel(ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos()));
        bullet.setPreciseLaunchVelocity(normalizedDirection);
        bullet.setBreaksBlocksEnabled(breaksBlocksEnabled());
        return level.addFreshEntity(bullet);
    }

    private void playArcSparkFx(Vec3 firepoint, Vec3 direction) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide || firepoint == null || direction.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 normalizedDirection = direction.normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(
                0.0F, 1.0F, 0.0F,
                (float) normalizedDirection.x,
                (float) normalizedDirection.y,
                (float) normalizedDirection.z
        );
        // Function: allowMulti keeps each barrel spark visible during rapid salvo fire instead of replacing the previous instance.
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                ARC_SPARK_FX,
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
    }

    private void playBarrelFireSound(Vec3 firepoint) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide || firepoint == null) {
            return;
        }

        // Function: each heavy electromagnetic barrel shot gets its own one-shot sound at the active muzzle.
        level.playSound(
                null,
                firepoint.x,
                firepoint.y,
                firepoint.z,
                vsieSounds.HEAVY_ELECTROMAGNET_TURRET_FIRE.get(),
                SoundSource.BLOCKS,
                2.0F,
                1.0F
        );
    }

    private void spawnBarrelMuzzleSmoke(Vec3 firepoint, Vec3 fireDirection) {
        Level level = this.getLevel();
        if (!(level instanceof ServerLevel serverLevel) || firepoint == null || fireDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 direction = fireDirection.normalize();
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(direction).normalize();

        for (int i = 0; i < MUZZLE_SMOKE_PARTICLE_COUNT; i++) {
            double axialSpeed = MUZZLE_SMOKE_BASE_SPEED + level.random.nextDouble() * 10D;
            double sideSpeed = (level.random.nextDouble() - level.random.nextDouble()) * MUZZLE_SMOKE_SPREAD;
            double upSpeed = (level.random.nextDouble() - level.random.nextDouble()) * MUZZLE_SMOKE_SPREAD;
            Vec3 velocity = direction.scale(axialSpeed).add(right.scale(sideSpeed)).add(up.scale(upSpeed));
            // Function: count zero keeps the explicit plume velocity; per-player force makes the smoke visible at cannon ranges.
            sendMuzzleSmokeParticle(serverLevel, firepoint, velocity);
        }
    }

    private void sendMuzzleSmokeParticle(ServerLevel serverLevel, Vec3 firepoint, Vec3 velocity) {
        double visibleDistanceSqr = MUZZLE_SMOKE_VISIBLE_DISTANCE * MUZZLE_SMOKE_VISIBLE_DISTANCE;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(firepoint) > visibleDistanceSqr) {
                continue;
            }
            serverLevel.sendParticles(
                    player,
                    ModParticleTypes.CANNON_MUZZLE_SMOKE.get(),
                    true,
                    firepoint.x,
                    firepoint.y,
                    firepoint.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0D
            );
        }
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

    private boolean hasPendingSalvo() {
        return pendingShotIndex < SALVO_SHOT_COUNT;
    }
}
