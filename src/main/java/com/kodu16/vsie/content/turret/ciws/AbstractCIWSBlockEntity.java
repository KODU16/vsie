package com.kodu16.vsie.content.turret.ciws;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieBlocks;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public abstract class AbstractCIWSBlockEntity extends AbstractTurretBlockEntity {
    private static final double SEARCH_RADIUS = 256.0D;
    private static final double MIN_PROJECTILE_SPEED_SQR = 0.01D;
    private static final int RETURN_TO_DEFAULT_DELAY_TICKS = 100;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private @Nullable Entity targetprojectile = null;
    private int noTargetTicks = 0;

    protected AbstractCIWSBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.turretData = new TurretData();
    }

    @Override
    public boolean isEnergyTurret() {
        return false;
    }

    @Override
    public boolean supportsBlockDestructionToggle() {
        return false;
    }

    @Override
    public Item getAmmoItem() {
        return vsieBlocks.SMALL_AMMOBOX_BLOCK.asItem();
    }

    @Override
    protected boolean shouldThrottleEntityTargetSearch() {
        // Function: CIWS retargets every tick so consecutive kills do not introduce a scan gap.
        return false;
    }

    @Override
    protected boolean hasAmmoReady() {
        for (int slot = 0; slot < getSlots(); slot++) {
            if (isUsableSmallAmmoBox(getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean consumeAmmoForShot() {
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (!isUsableSmallAmmoBox(stack)) {
                continue;
            }

            // Function: one CIWS shot consumes one durability point from the first valid small ammobox.
            int nextDamage = stack.getDamageValue() + 1;
            if (nextDamage >= stack.getMaxDamage()) {
                setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                stack.setDamageValue(nextDamage);
                setStackInSlot(slot, stack);
            }
            setChanged();
            return true;
        }
        return false;
    }

    private boolean isUsableSmallAmmoBox(ItemStack stack) {
        return stack.is(getAmmoItem()) && stack.isDamageableItem() && stack.getDamageValue() < stack.getMaxDamage();
    }

    protected @Nullable Entity getTargetProjectile() {
        return targetprojectile;
    }

    protected boolean isTargetProjectileValidForFire() {
        return isValidTargetProjectile(targetprojectile);
    }

    protected void clearTargetProjectile() {
        // Function: let CIWS subclasses finish an interception and force the next tick to search again.
        targetprojectile = null;
        targetDistance = 0;
        targetPreVelocity.clear();
        setAnimData(TURRET_HAS_TARGET, false);
        setChanged();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {
    }

    @Override
    public void tick() {
        // Function: keep CIWS rotation server-authoritative so client prediction cannot fight synced turret angles.
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        this.level = level;

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, pos);
        // Function: ground-mounted CIWS still needs its default-angle servo path even without sublevel transforms.
        currentworldpos = subLevel != null ? ServerShipUtils.getBlockCenterWorld(subLevel, pos) : getTurretAimOriginWorld();
        tryInvalidateTarget();
        tickFireCooldown(hasValidTarget());
        acquireTargetByAimType();
        if (hasValidTarget()) {
            noTargetTicks = 0;
            appendTargetVelocitySample();
            updateCurrentTargetPos();

            targetPos = getShootLocation(targetPos, targetPreVelocity, level, currentworldpos);
            updateTargetRot();
            this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
            this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
            if (canFireAtCurrentAim()) {
                fireWhenLocked();
            }
        } else {
            // Function: delay default rotation so short target loss does not snap the CIWS back and forth.
            if (++noTargetTicks < RETURN_TO_DEFAULT_DELAY_TICKS) {
                return;
            }
            returnToDefaultRotation();
        }

        this.setAnimData(XROT, xRot0);
        this.setAnimData(YROT, yRot0);
        this.markUpdated();
    }

    private void tryInvalidateTarget() {
        if (aimtype == 1) {
            if (!isValidTargetEntity(targetentity)) {
                setAnimData(TURRET_HAS_TARGET, false);
                targetentity = null;
                targetDistance = 0;
                targetPreVelocity.clear();
            }
        } else if (aimtype == 2) {
            if (!isValidTargetProjectile(targetprojectile)) {
                setAnimData(TURRET_HAS_TARGET, false);
                targetprojectile = null;
                targetDistance = 0;
                targetPreVelocity.clear();
            }
        }
    }

    private boolean isValidTargetProjectile(@Nullable Entity e) {
        if (e == null || e.isRemoved() || e.getDeltaMovement().lengthSqr() < MIN_PROJECTILE_SPEED_SQR) {
            return false;
        }
        if (!isNoHealthProjectileCandidate(e)) {
            return false;
        }

        Vec3 center = getCiwsProtectedCenter();
        if (!isProjectileMovingTowardCenter(e, center)) {
            return false;
        }

        double distSq = e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        if (distSq > SEARCH_RADIUS * SEARCH_RADIUS) {
            return false;
        }
        if (!canSeeTarget(new Vector3d(e.getX(), e.getY() + e.getEyeHeight(), e.getZ()))) {
            return false;
        }
        return true;
    }

    private Vec3 getCiwsProtectedCenter() {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        if (subLevel != null) {
            Vec3 structureCenter = ServerShipUtils.getStructureCenterWorld(subLevel);
            if (structureCenter != null) {
                return structureCenter;
            }
        }
        return new Vec3(this.currentworldpos.x, this.currentworldpos.y, this.currentworldpos.z);
    }

    private boolean canSeeTarget(Vector3d pos) {
        Vec3 turretpos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        Vec3 targetPos = new Vec3(Math.round(pos.x() * 10) / 10.0, Math.round(pos.y() * 10) / 10.0, Math.round(pos.z() * 10) / 10.0);
        Vec3 lookVec = turretpos.vectorTo(targetPos).normalize().scale(0.75F);
        ClipContext ctx = new ClipContext(turretpos.add(lookVec), targetPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty());
        return level.clip(ctx).getType().equals(HitResult.Type.MISS);
    }

    private void updateTargetRot() {
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        Vec3 localUp = Vec3.atLowerCornerOf(facing.getOpposite().getNormal());
        Vec3 localForward = switch (facing) {
            case NORTH -> new Vec3(0, 1, 0);
            case SOUTH -> new Vec3(0, -1, 0);
            case WEST, EAST, UP, DOWN -> new Vec3(0, 0, -1);
        };

        Vec3 localRight = switch (facing) {
            case NORTH, DOWN, SOUTH -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(0, -1, 0);
            case EAST -> new Vec3(0, 1, 0);
            case UP -> new Vec3(-1, 0, 0);
        };

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        if (subLevel != null) {
            worldXDirection = subLevel.logicalPose().transformNormal(new Vector3d(localForward.x, localForward.y, localForward.z)).normalize();
            worldYDirection = subLevel.logicalPose().transformNormal(new Vector3d(localUp.x, localUp.y, localUp.z)).normalize();
            worldZDirection = subLevel.logicalPose().transformNormal(new Vector3d(localRight.x, localRight.y, localRight.z)).normalize();
        } else {
            worldXDirection = new Vector3d(localForward.x, localForward.y, localForward.z);
            worldYDirection = new Vector3d(localUp.x, localUp.y, localUp.z);
            worldZDirection = new Vector3d(localRight.x, localRight.y, localRight.z);
        }

        Vec3 toTargetWorld = new Vec3(
                targetPos.x - currentworldpos.x,
                targetPos.y - currentworldpos.y,
                targetPos.z - currentworldpos.z
        ).normalize();
        if (toTargetWorld.lengthSqr() < 1e-6) {
            return;
        }

        double localX = toTargetWorld.dot(new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z));
        double localY = toTargetWorld.dot(new Vec3(worldYDirection.x, worldYDirection.y, worldYDirection.z));
        double localZ = toTargetWorld.dot(new Vec3(worldXDirection.x, worldXDirection.y, worldXDirection.z));

        double yaw = Math.atan2(localX, localZ);
        double pitch = Math.atan2(localY, Math.sqrt(localX * localX + localZ * localZ));

        this.targetyrot = (float) -yaw;
        this.targetxrot = (float) pitch;
    }

    private void acquireTargetByAimType() {
        if (aimtype == 1) {
            tryFindTargetEntity();
        }
        if (aimtype == 2) {
            tryFindTargetProjectile();
        }
    }

    public void tryFindTargetProjectile() {
        if (isValidTargetProjectile(targetprojectile)) {
            return;
        }

        // Function: scan the full 256-block CIWS sphere every tick so fast projectiles are not skipped between grid batches.
        AABB searchBox = new AABB(
                currentworldpos.x - SEARCH_RADIUS,
                currentworldpos.y - SEARCH_RADIUS,
                currentworldpos.z - SEARCH_RADIUS,
                currentworldpos.x + SEARCH_RADIUS,
                currentworldpos.y + SEARCH_RADIUS,
                currentworldpos.z + SEARCH_RADIUS
        );

        double bestDistSq = Double.MAX_VALUE;
        Entity bestCandidate = null;
        List<Entity> candidates = level.getEntitiesOfClass(Entity.class, searchBox, this::isValidTargetProjectile);
        for (Entity candidate : candidates) {
            double distSq = candidate.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null) {
            return;
        }

        targetprojectile = bestCandidate;
        this.targetPos = new Vec3(
                targetprojectile.getX(),
                targetprojectile.getY(),
                targetprojectile.getZ()
        );
        // Function: CIWS subclasses can snapshot projectile-lock data such as the initial intercept distance.
        onProjectileTargetLocked(bestCandidate, Math.sqrt(bestDistSq));
        setChanged();
    }

    private boolean hasValidTarget() {
        return (aimtype == 1 && isValidTargetEntity(targetentity))
                || (aimtype == 2 && isValidTargetProjectile(targetprojectile));
    }

    protected boolean hasValidTargetForCiwsFire() {
        return hasValidTarget();
    }

    protected void onProjectileTargetLocked(Entity projectile, double lockDistance) {
    }

    private void updateCurrentTargetPos() {
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPos = new Vec3(
                    targetentity.getX(),
                    targetentity.getY() + targetentity.getEyeHeight(),
                    targetentity.getZ()
            );
        }
        if (aimtype == 2 && isValidTargetProjectile(targetprojectile)) {
            targetPos = new Vec3(
                    targetprojectile.getX(),
                    targetprojectile.getY(),
                    targetprojectile.getZ()
            );
        }
    }

    private void appendTargetVelocitySample() {
        if (targetPreVelocity.size() >= 5) {
            targetPreVelocity.remove(0);
        }
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPreVelocity.add(new Vector3d(targetentity.getDeltaMovement().x, targetentity.getDeltaMovement().y, targetentity.getDeltaMovement().z));
        } else if (aimtype == 2 && isValidTargetProjectile(targetprojectile)) {
            targetPreVelocity.add(new Vector3d(targetprojectile.getDeltaMovement().x, targetprojectile.getDeltaMovement().y, targetprojectile.getDeltaMovement().z));
        }
    }

    private void fireWhenLocked() {
        if (!isFireCooldownReady()) {
            return;
        }
        if (!consumeAmmoForShot()) {
            return;
        }
        if (aimtype == 1) {
            shootentity();
            consumeFireCooldown();
        } else if (aimtype == 2) {
            interceptprojectile();
            consumeFireCooldown();
        }
    }

    protected boolean canFireAtCurrentAim() {
        // Function: subclasses can start CIWS fire before the turret reaches a perfect lock.
        return xOK && yOK;
    }

    private boolean isNoHealthProjectileCandidate(Entity e) {
        // Function: CIWS projectile mode targets moving non-living shots, not entities that own health.
        if (e instanceof LivingEntity) {
            return false;
        }
        // Function: dropped items can move like shots but should not be intercepted or cleared by CIWS.
        return !(e instanceof ItemEntity);
    }

    private boolean isProjectileMovingTowardCenter(Entity e, Vec3 center) {
        Vec3 velocity = e.getDeltaMovement();
        if (velocity.lengthSqr() < MIN_PROJECTILE_SPEED_SQR) {
            return false;
        }

        Vec3 projectileToCenter = center.subtract(e.position());
        if (projectileToCenter.lengthSqr() < 1.0E-6D) {
            return true;
        }

        // Function: dot > 0 means projectile-to-center and velocity form an angle smaller than 90 degrees.
        return velocity.dot(projectileToCenter) > 0.0D;
    }

    public abstract void interceptprojectile();

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
