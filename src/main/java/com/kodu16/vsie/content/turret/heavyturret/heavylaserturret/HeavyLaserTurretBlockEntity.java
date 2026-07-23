package com.kodu16.vsie.content.turret.heavyturret.heavylaserturret;

import com.kodu16.vsie.content.cooldown.FireCooldown;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.content.turret.client.LaserTurretSoundManager;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.kodu16.vsie.foundation.LoadedChunkRaycast;
import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class HeavyLaserTurretBlockEntity extends AbstractHeavyTurretBlockEntity {
    private static final int SHOT_INTERVAL_TICKS = 2;
    private static final int EVERY_OTHER_SHOT_EXTRA_TICK = 1;
    private static final int COOL2_MAX_CHARGE = 10;
    private static final double COOL2_RECOVERY_PER_TICK = 0.2D;
    private static final float LASER_EXPLOSION_RADIUS = 3.0F;
    private static final float LASER_LAYER_RADIUS = 0.8F;
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.0F;
    private static final int HIT_CLEAR_RADIUS = 1;
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean recoveredChargeThisTick = false;
    private boolean addExtraCooldownTickNextShot = false;

    public HeavyLaserTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.turretData = new TurretData();
    }

    @Override
    public void tick() {
        Level level = getLevel();
        if (level != null && level.isClientSide()) {
            LaserTurretSoundManager.updateTurret(this);
        }
        super.tick();
        level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!isHeavyFireRequested()) {
            if (targetDistance > 0.0D) {
                // Function: releasing fire input hides the persistent heavy laser beam without affecting the alternating 2/3-tick hit cadence.
                targetDistance = 0.0D;
                markUpdated();
            }
            return;
        }
    }

    @Override
    public Vec3 getShootLocation(Vec3 vec, List<Vector3d> preV, Level lv, Vec3 pos) {
        return vec;
    }

    @Override
    public boolean isEnergyTurret() {
        // Function: heavy laser turrets are beam weapons, so they do not expose or consume ammo items.
        return true;
    }

    @Override
    public Item getAmmoItem() {
        return null;
    }

    @Override
    public String getturrettype() {
        return "heavy_laser";
    }

    @Override
    public float getLaserLayerRadius() {
        // Function: keep the heavy laser beam at its existing square-beam half-size.
        return LASER_LAYER_RADIUS;
    }

    @Override
    public String getLaserLayerBoneName() {
        return "locater";
    }

    @Override
    public boolean usesSquareLaserLayerBeam() {
        return true;
    }

    @Override
    public boolean transformsLaserLayerFromBone() {
        return true;
    }

    @Override
    public boolean flipsLaserLayerDirection() {
        return false;
    }

    @Override
    public int getLaserLayerLengthSegments() {
        return 1;
    }

    @Override
    public float getLaserLayerRed(float t) {
        return Mth.lerp(t, 0.30F, 0.45F);
    }

    @Override
    public float getLaserLayerGreen(float t) {
        return Mth.lerp(t, 0.35F, 0.5F);
    }

    @Override
    public float getLaserLayerBlue(float t) {
        return 1.0F;
    }

    @Override
    public float getLaserLayerAlpha(float t) {
        return Mth.lerp(t, 0.35F, 0.75F);
    }

    @Override
    public double getYAxisOffset() {
        return 3D;
    }

    @Override
    public double getcannonlength() {
        return 15.0D;
    }

    @Override
    public float getMaxSpinSpeed() {
        return Mth.PI / 256;
    }

    @Override
    public int getCoolDown() {
        return SHOT_INTERVAL_TICKS;
    }

    @Override
    public FireCooldown getFireCooldown() {
        // Function: cool2 uses shot interval, stored charge, and idle recovery rate for sustained heavy lasers.
        return FireCooldown.cool2(SHOT_INTERVAL_TICKS, COOL2_MAX_CHARGE, COOL2_RECOVERY_PER_TICK);
    }

    @Override
    protected void tickFireCooldown(boolean fireRequested) {
        double beforeRecovery = fireCooldownValue;
        // Function: heavy laser recovers charge on every tick where no shot is actually consumed.
        super.tickFireCooldown(false);
        recoveredChargeThisTick = fireCooldownValue > beforeRecovery;
    }

    @Override
    protected void consumeFireCooldown() {
        super.consumeFireCooldown();
        if (addExtraCooldownTickNextShot) {
            // Function: alternate between 2 and 3 ticks so the heavy laser averages exactly twice the old 5-tick fire rate.
            idleTicks += EVERY_OTHER_SHOT_EXTRA_TICK;
        }
        addExtraCooldownTickNextShot = !addExtraCooldownTickNextShot;
        if (recoveredChargeThisTick) {
            fireCooldownValue = Math.max(0.0D, fireCooldownValue - COOL2_RECOVERY_PER_TICK);
        }
        recoveredChargeThisTick = false;
    }

    @Override
    public int getenergypertick() {
        return 1000;
    }

    @Override
    protected float getBlockBreakTntChance() {
        return BLOCK_BREAK_TNT_CHANCE;
    }

    @Override
    public boolean isFireTypeSelectable(int fireType) {
        // Function: heavy laser currently only supports automatic targeting until manual and smart aim paths are finished.
        return fireType == 1;
    }

    @Override
    protected boolean manualFireContinuously() {
        return true;
    }

    @Override
    protected boolean canAutomaticFireWhileAiming() {
        // Function: heavy laser beams stay active during automatic tracking until target validity or line tracing fails.
        return true;
    }

    @Override
    public void shootentity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide() || targetentity == null || !targetentity.isAlive() || targetentity.isRemoved()) {
            return;
        }

        Vec3 targetPos = targetentity.position();
        BlockHitResult hitResult = traceHeavyLaserHit(level, targetPos);
        syncVisualLaserDistance(resolveVisualImpactDistance(hitResult, targetPos));
        triggerAnim("controller", "shoot");
        if (hitResult.getType() == HitResult.Type.BLOCK && !isBlockOnSameShipAsTurret(hitResult.getBlockPos())) {
            applyHeavyLaserHit(level, hitResult.getBlockPos());
            return;
        }
        // Function: laser damage is not fire damage, so fire-immune mobs must still take beam hits.
        targetentity.hurt(level.damageSources().generic(), 15.0F);
    }

    @Override
    public void shootship() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockPos hitPos = this.getLastShipShotHitBlockPos();
        if (hitPos.equals(BlockPos.ZERO)) {
            return;
        }
        syncVisualLaserDistance(currentworldpos.distanceTo(Vec3.atCenterOf(hitPos)));
        triggerAnim("controller", "shoot");
        applyHeavyLaserHit(level, hitPos);
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

    private Vec3 resolveLaserMuzzleWorld(Vec3 target) {
        Vector3d sampledFirePoint = getFirePoint();
        Level level = this.getLevel();
        if (sampledFirePoint != null && level != null) {
            Vec3 sampled = new Vec3(sampledFirePoint.x, sampledFirePoint.y, sampledFirePoint.z);
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
            // Function: locater firepoints are sampled in sublevel coordinates and must be promoted to world space before raycasting.
            return subLevel == null ? sampled : subLevel.logicalPose().transformPosition(sampled);
        }
        // Function: fall back to the medium laser muzzle projection when the locater sample has not arrived from the client.
        return target == null ? null : getCannonMuzzleWorld(target);
    }

    private BlockHitResult traceMediumLaserHit(Level level, Vec3 from, Vec3 target) {
        if (from.distanceToSqr(target) < 1.0E-6D) {
            return BlockHitResult.miss(from, Direction.UP, BlockPos.containing(from));
        }
        // Function: heavy laser beams ignore unloaded chunks instead of forcing distant chunk collision checks.
        return LoadedChunkRaycast.clipIgnoringUnloadedChunks(
                level,
                from,
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        );
    }

    private BlockHitResult traceHeavyLaserHit(Level level, Vec3 target) {
        Vec3 muzzle = resolveLaserMuzzleWorld(target);
        Vec3 traceStart = muzzle != null ? muzzle : currentworldpos;
        // Function: heavy-laser block impacts must use the muzzle sample so the broken area matches the rendered beam.
        return traceMediumLaserHit(level, traceStart, target);
    }

    private double resolveVisualImpactDistance(BlockHitResult hitResult, Vec3 target) {
        Vec3 muzzle = resolveLaserMuzzleWorld(target);
        Vec3 traceStart = muzzle != null ? muzzle : currentworldpos;
        Vec3 impact = hitResult.getType() == HitResult.Type.BLOCK ? hitResult.getLocation() : target;
        return traceStart.distanceTo(impact);
    }

    private void syncVisualLaserDistance(double distance) {
        if (Math.abs(targetDistance - distance) <= 0.01D) {
            return;
        }
        targetDistance = distance;
        markUpdated();
    }

    private boolean isBlockOnSameShipAsTurret(BlockPos blockPos) {
        Level level = this.getLevel();
        if (level == null) {
            return false;
        }
        SubLevel turretShip = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        SubLevel hitShip = ServerShipUtils.getSubLevelAtBlockPos(level, blockPos);
        return turretShip != null && hitShip != null && hitShip.hashCode() == turretShip.hashCode();
    }

    private void applyHeavyLaserHit(Level level, BlockPos hitPos) {
        BlockPos bodyHitPos = resolveSubLevelBodyHitPos(level, hitPos);
        // Function: heavy laser now reuses medium laser's body-hit cleanup path and only expands the cleared cube to 3x3x3.
        if (breaksBlocksEnabled()) {
            for (BlockPos pos : BlockPos.betweenClosed(
                    bodyHitPos.offset(-HIT_CLEAR_RADIUS, -HIT_CLEAR_RADIUS, -HIT_CLEAR_RADIUS),
                    bodyHitPos.offset(HIT_CLEAR_RADIUS, HIT_CLEAR_RADIUS, HIT_CLEAR_RADIUS)
            )) {
                breakTurretTargetBlockAsMined(level, pos);
            }
        }
        level.explode(
                null,
                hitPos.getX() + 0.5D,
                hitPos.getY() + 0.5D,
                hitPos.getZ() + 0.5D,
                LASER_EXPLOSION_RADIUS,
                false,
                Level.ExplosionInteraction.NONE
        );
    }

    private BlockPos resolveSubLevelBodyHitPos(Level level, BlockPos hitPos) {
        SubLevel hitSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, hitPos);
        if (hitSubLevel != null) {
            return hitPos;
        }

        SubLevel targetShip = getCurrentHeavyTargetShip();
        if (targetShip == null) {
            return hitPos;
        }

        // Function: visual/world hit positions must be projected into the locked sublevel body coordinates before breaking ship blocks.
        Vec3 bodyHitCenter = targetShip.logicalPose().transformPositionInverse(Vec3.atCenterOf(hitPos));
        return BlockPos.containing(bodyHitCenter);
    }
}
