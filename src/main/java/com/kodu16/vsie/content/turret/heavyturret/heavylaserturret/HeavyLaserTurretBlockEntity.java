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
    private static final int SHOT_INTERVAL_TICKS = 5;
    private static final int COOL2_MAX_CHARGE = 10;
    private static final double COOL2_RECOVERY_PER_TICK = 0.2D;
    private static final float LASER_EXPLOSION_RADIUS = 3.0F;
    private static final float BLOCK_BREAK_TNT_CHANCE = 0.0F;
    private static final int HIT_CLEAR_RADIUS = 1;
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean recoveredChargeThisTick = false;

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
                // Function: releasing fire input hides the persistent heavy laser beam without affecting 5-tick hit cadence.
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
    public void shootentity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide() || targetentity == null || !targetentity.isAlive() || targetentity.isRemoved()) {
            return;
        }

        // Function: entity damage mirrors the medium laser hit behaviour.
        syncVisualLaserDistance(currentworldpos.distanceTo(targetentity.position()));
        triggerAnim("controller", "shoot");
        targetentity.hurt(level.damageSources().onFire(), 15.0F);
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

        if (breaksBlocksEnabled()) {
            for (BlockPos pos : BlockPos.betweenClosed(
                    hitPos.offset(-1, -1, -1),
                    hitPos.offset(1, 1, 1)
            )) {
                breakTurretTargetBlockAsMined(level, pos);
            }
        }

        level.explode(
                null,
                hitPos.getX() + 0.5D,
                hitPos.getY() + 0.5D,
                hitPos.getZ() + 0.5D,
                3.0F,
                true,
                Level.ExplosionInteraction.NONE
        );
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

    private ShotTrace resolveShotTrace(Level level, Vec3 muzzle, Vec3 target) {
        // Function: manual/smart heavy-laser firing is disabled; automatic mode mirrors medium laser target tracing.
        return new ShotTrace(target, traceMediumLaserHit(level, currentworldpos, target));
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

    private void applyMediumLaserHit(Level level, BlockPos hitPos) {
        if (!breaksBlocksEnabled()) {
            level.explode(
                    null,
                    hitPos.getX() + 0.5D,
                    hitPos.getY() + 0.5D,
                    hitPos.getZ() + 0.5D,
                    LASER_EXPLOSION_RADIUS,
                    true,
                    Level.ExplosionInteraction.NONE
            );
            return;
        }
        // Function: heavy laser must break the hit cube through the vanilla break event path so no drops are created.
        for (BlockPos pos : BlockPos.betweenClosed(
                hitPos.offset(-HIT_CLEAR_RADIUS, -HIT_CLEAR_RADIUS, -HIT_CLEAR_RADIUS),
                hitPos.offset(HIT_CLEAR_RADIUS, HIT_CLEAR_RADIUS, HIT_CLEAR_RADIUS)
        )) {
            breakTurretTargetBlockAsMined(level, pos);
        }
        // Function: keep the heavy laser explosion visual-only because the actual block removal already happened above.
        level.explode(
                null,
                hitPos.getX() + 0.5D,
                hitPos.getY() + 0.5D,
                hitPos.getZ() + 0.5D,
                LASER_EXPLOSION_RADIUS,
                true,
                Level.ExplosionInteraction.NONE
        );
    }

    private record ShotTrace(Vec3 target, BlockHitResult hitResult) {
    }
}
