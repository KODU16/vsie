package com.kodu16.vsie.content.bullet;

import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieSounds;
import com.kodu16.vsie.utility.FxData;
import com.kodu16.vsie.utility.vsieFxHelper;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesprojectilelib.RPLTags;
import rbasamoyai.ritchiesprojectilelib.RitchiesProjectileLib;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractBulletEntity extends Projectile {
    private record PortalTraceContext(Level level, Vec3 from, Vec3 to) {
    }

    private static final EntityDataAccessor<Float> DATA_LAUNCH_DIR_X =
            SynchedEntityData.defineId(AbstractBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LAUNCH_DIR_Y =
            SynchedEntityData.defineId(AbstractBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LAUNCH_DIR_Z =
            SynchedEntityData.defineId(AbstractBulletEntity.class, EntityDataSerializers.FLOAT);

    private static final int DEFAULT_MAX_LIFETIME_TICKS = 80;
    private static final int MAX_UNLOADED_CHUNK_WAIT_TICKS = 20;
    private static final double CHUNK_EDGE_EPSILON = 1.0E-6D;
    private static final double CLIENT_HARD_SNAP_DISTANCE_SQR = 48.0D * 48.0D;
    private static final double CLIENT_POSITION_PULL = 0.18D;
    private static final double CLIENT_VELOCITY_LERP = 0.55D;
    private static final double CLIENT_MAX_CORRECTION_SPEED_FACTOR = 0.45D;

    private int lifeTime = 0;
    private int unloadedChunkWaitTicks = 0;
    private boolean clientMotionFilterReady = false;
    private boolean localVelocityUpdate = false;
    private int clientTicksSincePreciseSync = 0;
    private Vec3 clientVisualPosition = Vec3.ZERO;
    private Vec3 clientVisualVelocity = Vec3.ZERO;
    private Vec3 clientSyncPosition = Vec3.ZERO;
    private Vec3 clientSyncVelocity = Vec3.ZERO;
    private float clientSyncYRot = 0.0F;
    private float clientSyncXRot = 0.0F;
    private boolean lifecycleFxStarted = false;
    private boolean destructionSoundPlayed = false;
    // BulletData supplies the FX resource used by the lifecycle executor.
    private BulletData dataBase = BulletData.createParticleBulletDefault();
    // Function: weapons/turrets can disable terrain damage per shot while preserving entity-hit behaviour.
    private boolean breaksBlocks = true;
    private UUID launchSubLevelId = null;

    public BulletData getDataBase() {
        return dataBase;
    }

    public boolean breaksBlocksEnabled() {
        return breaksBlocks;
    }

    public void setBreaksBlocksEnabled(boolean breaksBlocks) {
        this.breaksBlocks = breaksBlocks;
    }

    public AbstractBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setLaunchSubLevel(SubLevel subLevel) {
        // Function: bullets pass through blocks belonging to their launch ship without block damage or motion changes.
        this.launchSubLevelId = subLevel == null ? null : subLevel.getUniqueId();
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        // Function: bullet subclasses handle nether portals as breakable blocks instead of dimension travel.
        return false;
    }

    @Override
    public void tick() {
        // Advance vanilla entity counters so tickCount-based FX windows follow the real entity lifetime.
        super.tick();

        if (!lifecycleFxStarted && this.tickCount >= startemitticks() && this.tickCount <= stopemitticks()) {
            if (this.level().isClientSide()) {
                vsieFxHelper.extractFxUnit(getDataBase().getFxData(), FxData::getAwakeFx)
                        .map(FxData.FxUnit::getId).map(FXHelper::getFX)
                        .ifPresent(this::startLifecycleFx);
                lifecycleFxStarted = true;
            }
        }

        Vec3 movement = applyConstantSpeed();
        updateRotationFromMovement(movement);
        Vec3 start = this.position();
        Vec3 end = start.add(movement);

        if (this.level().isClientSide()) {
            tickClientFilteredMotion(movement);
            return;
        }

        double loadedEndT = findFirstUnloadedChunkT(this.level(), start, end);
        boolean enteredUnloadedChunk = loadedEndT < 1.0D;
        Vec3 collisionEnd = start.lerp(end, enteredUnloadedChunk ? Math.max(0.0D, loadedEndT - CHUNK_EDGE_EPSILON) : 1.0D);
        Vec3 collisionMovement = collisionEnd.subtract(start);
        Vec3 serverMoveEnd = enteredUnloadedChunk ? collisionEnd : end;


        Vec3 originalMovement = this.getDeltaMovement();
        this.setDeltaMovement(collisionMovement);
        HitResult hitResult = collisionMovement.lengthSqr() > 1.0E-10D
                ? ProjectileUtil.getHitResultOnMoveVector(
                        this,
                        this::canHitEntity
                )
                : missAt(start, start.subtract(movement));
        this.setDeltaMovement(originalMovement);

        if (hitResult.getType() == HitResult.Type.BLOCK && isLaunchSubLevelBlock(((BlockHitResult) hitResult).getBlockPos())) {
            hitResult = BlockHitResult.miss(collisionEnd, Direction.getNearest(movement.x, movement.y, movement.z), BlockPos.containing(collisionEnd));
        }

        if (hitResult.getType() == HitResult.Type.MISS) {

            List<Entity> entities = this.level().getEntities(
                    this,
                    // Function: match TACZ's swept lookup margin so high-speed bullets still find entities near the path edge.
                    this.getBoundingBox().expandTowards(collisionMovement).inflate(1.0D)
            );

            Entity closest = null;
            Vec3 closestIntercept = null;
            double closestDistSq = Double.MAX_VALUE;

            for (Entity entity : entities) {

                if (!this.canHitEntity(entity)) continue;

                Optional<Vec3> intercept = entity.getBoundingBox().clip(start, collisionEnd);

                if (intercept.isPresent()) {

                    double distSq = intercept.get().distanceToSqr(start);

                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closest = entity;
                        closestIntercept = intercept.get();
                    }
                }
            }

            if (closest != null) {
                hitResult = new EntityHitResult(closest, closestIntercept);
            }
        }

        BlockHitResult portalHit = findFirstNetherPortalHit(start, collisionEnd);
        if (portalHit != null && !isLaunchSubLevelBlock(portalHit.getBlockPos()) && isCloserHit(start, portalHit, hitResult)) {
            hitResult = portalHit;
        }


        if (hitResult.getType() == HitResult.Type.ENTITY) {

            this.onHitEntity((EntityHitResult) hitResult);
            if (shouldDiscardAfterEntityHit((EntityHitResult) hitResult)) {
                this.discard();
                return;
            }

        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            if (isNetherPortalHit((BlockHitResult) hitResult)) {
                destroyNetherPortalBlock((BlockHitResult) hitResult);
                if (breaksBlocksEnabled()) {
                    this.onHitBlock((BlockHitResult) hitResult);
                }
                if (!breaksBlocksEnabled() || shouldDiscardAfterBlockHit((BlockHitResult) hitResult)) {
                    this.discard();
                    return;
                }
            } else {
                if (!breaksBlocksEnabled()) {
                    this.discard();
                    return;
                }

                this.onHitBlock((BlockHitResult) hitResult);
                if (shouldDiscardAfterBlockHit((BlockHitResult) hitResult)) {
                    this.discard();
                    return;
                }
            }
        }

        requestPreciseMotionChunkLoading(start, end);
        if (enteredUnloadedChunk) {
            // Function: keep bullets in entity-ticking chunks until RPL's batched force loader catches up.
            unloadedChunkWaitTicks++;
            if (unloadedChunkWaitTicks >= MAX_UNLOADED_CHUNK_WAIT_TICKS) {
                this.discard();
                return;
            }
        } else {
            unloadedChunkWaitTicks = 0;
        }
        this.setPos(serverMoveEnd);
        finishServerBulletMove(hitResult, true);
    }

    protected boolean shouldDiscardAfterEntityHit(EntityHitResult result) {
        return true;
    }

    protected boolean shouldDiscardAfterBlockHit(BlockHitResult result) {
        return true;
    }

    protected void afterServerBulletMove(HitResult hitResult) {
        // Function: piercing bullets can keep moving after a collision while normal bullets keep the default discard path.
    }

    protected boolean canBulletBreakBlock(Level level, BlockPos pos, BlockState state) {
        return level.isLoaded(pos) && !state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private BlockHitResult findFirstNetherPortalHit(Vec3 from, Vec3 to) {
        if (from.equals(to)) {
            return null;
        }
        PortalTraceContext context = new PortalTraceContext(this.level(), from, to);
        return BlockGetter.traverseBlocks(from, to, context, (traceContext, pos) -> {
            Level traceLevel = traceContext.level();
            if (!traceLevel.isLoaded(pos)) {
                return null;
            }
            BlockState state = traceLevel.getBlockState(pos);
            if (!state.is(Blocks.NETHER_PORTAL)) {
                return null;
            }
            BlockHitResult shapeHit = state.getShape(traceLevel, pos).clip(traceContext.from(), traceContext.to(), pos);
            if (shapeHit != null) {
                return shapeHit;
            }
            Vec3 direction = traceContext.to().subtract(traceContext.from());
            return new BlockHitResult(Vec3.atCenterOf(pos), Direction.getNearest(direction.x, direction.y, direction.z), pos.immutable(), false);
        }, traceContext -> null);
    }

    private boolean isCloserHit(Vec3 from, BlockHitResult candidate, HitResult current) {
        if (current.getType() == HitResult.Type.MISS) {
            return true;
        }
        return from.distanceToSqr(candidate.getLocation()) < from.distanceToSqr(current.getLocation());
    }

    private boolean isLaunchSubLevelBlock(BlockPos pos) {
        if (launchSubLevelId == null || this.level() == null) {
            return false;
        }
        SubLevel hitSubLevel = ServerShipUtils.getSubLevelAtBlockPos(this.level(), pos);
        return hitSubLevel != null && launchSubLevelId.equals(hitSubLevel.getUniqueId());
    }

    private boolean isNetherPortalHit(BlockHitResult result) {
        return this.level().isLoaded(result.getBlockPos())
                && this.level().getBlockState(result.getBlockPos()).is(Blocks.NETHER_PORTAL);
    }

    private void destroyNetherPortalBlock(BlockHitResult result) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.NETHER_PORTAL)) {
            return;
        }
        // Function: bullets destroy nether portal blocks on contact so vanilla portal ticking never teleports them.
        level.levelEvent(2001, pos, Block.getId(state));
        level.destroyBlock(pos, false, this);
    }

    protected float getBlockBreakTntChance() {
        return 0.0F;
    }

    protected float getBlockBreakTntPower() {
        return 4.0F;
    }

    protected double getBlockBreakRadius() {
        return 0.0D;
    }

    protected double computeBlockBreakProbability(ServerLevel level, BlockPos pos, Vec3 impactPoint, double maxDistance) {
        BlockState state = level.getBlockState(pos);
        double hardness = Math.max(0.0D, state.getDestroySpeed(level, pos));
        double distanceRatio = maxDistance <= 1.0E-6D ? 0.0D : Vec3.atCenterOf(pos).distanceTo(impactPoint) / maxDistance;
        // Function: bullet break chance falls off with both impact distance and block hardness, but never exceeds 100 percent.
        return Mth.clamp((1.2D - distanceRatio) * (1.2D - hardness / 100.0D), 0.0D, 1.0D);
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
                    if (!canBulletBreakBlock(level, targetPos, state)) {
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

    protected boolean breakBlockAsMined(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!canBulletBreakBlock(level, pos, state)) {
            return false;
        }
        // Function: bullet terrain hits should use the vanilla break event while suppressing item drops.
        level.levelEvent(2001, pos, Block.getId(state));
        boolean destroyed = level.destroyBlock(pos, false, this);
        if (destroyed) {
            maybeTriggerBlockBreakTnt(level, pos);
        }
        return destroyed;
    }

    protected void maybeTriggerBlockBreakTnt(ServerLevel level, BlockPos pos) {
        float chance = Mth.clamp(getBlockBreakTntChance(), 0.0F, 1.0F);
        if (chance <= 0.0F || level.random.nextFloat() >= chance) {
            return;
        }
        // Function: some bullets can optionally turn a successful block break into a TNT-like follow-up blast.
        level.explode(
                this,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                getBlockBreakTntPower(),
                false,
                Level.ExplosionInteraction.TNT
        );
    }

    protected int getMaxLifeTime() {
        // Function: short-lived bullets must clear quickly so missed shots cannot pile up near chunk boundaries.
        return DEFAULT_MAX_LIFETIME_TICKS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LAUNCH_DIR_X, 0.0F);
        builder.define(DATA_LAUNCH_DIR_Y, 0.0F);
        builder.define(DATA_LAUNCH_DIR_Z, 0.0F);
    }

    private void tickClientFilteredMotion(Vec3 syncedMovement) {
        if (!clientMotionFilterReady) {
            clientMotionFilterReady = true;
            clientVisualPosition = this.position();
            clientVisualVelocity = syncedMovement;
            clientSyncPosition = clientVisualPosition;
            clientSyncVelocity = syncedMovement;
        }

        // Function: start prediction from the latest server sample; adding one tick here made fast bullets overshoot every precise sync.
        Vec3 predictedServerPosition = clientSyncPosition.add(clientSyncVelocity.scale(clientTicksSincePreciseSync));
        clientTicksSincePreciseSync++;
        Vec3 positionError = predictedServerPosition.subtract(clientVisualPosition);
        if (positionError.lengthSqr() > CLIENT_HARD_SNAP_DISTANCE_SQR) {
            clientVisualPosition = predictedServerPosition;
            clientVisualVelocity = clientSyncVelocity;
            this.setPos(clientVisualPosition);
            updateClientFilteredRotation(clientVisualVelocity);
            return;
        }

        Vec3 velocityTarget = clientSyncVelocity.lengthSqr() > 1.0E-10D ? clientSyncVelocity : syncedMovement;
        Vec3 velocityCorrection = velocityTarget.subtract(clientVisualVelocity).scale(CLIENT_VELOCITY_LERP);
        Vec3 positionCorrection = clampLength(
                positionError.scale(CLIENT_POSITION_PULL),
                Math.max(getSpeed() * CLIENT_MAX_CORRECTION_SPEED_FACTOR, 0.25D)
        );
        clientVisualVelocity = clientVisualVelocity.add(velocityCorrection).add(positionCorrection);
        clientVisualPosition = clientVisualPosition.add(clientVisualVelocity);
        this.setPos(clientVisualPosition);
        updateClientFilteredRotation(clientVisualVelocity);
    }

    private Vec3 clampLength(Vec3 vector, double maxLength) {
        double lengthSqr = vector.lengthSqr();
        if (lengthSqr <= maxLength * maxLength || lengthSqr < 1.0E-10D) {
            return vector;
        }
        return vector.normalize().scale(maxLength);
    }

    private void updateClientFilteredRotation(Vec3 movement) {
        updateRotationFromMovement(movement);
        if (movement.lengthSqr() < 1.0E-6D) {
            this.setYRot(clientSyncYRot);
            this.setXRot(clientSyncXRot);
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (!this.level().isClientSide()) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
            return;
        }

        Vec3 target = new Vec3(x, y, z);
        if (clientMotionFilterReady) {
            Vec3 sampledVelocity = target.subtract(clientSyncPosition)
                    .scale(1.0D / Math.max(1, clientTicksSincePreciseSync));
            if (sampledVelocity.lengthSqr() > 1.0E-10D) {
                // Function: precise position samples preserve direction even when vanilla velocity packets are clamped.
                clientSyncVelocity = normalizeClientSyncVelocity(sampledVelocity);
            }
        }
        clientSyncPosition = target;
        clientSyncYRot = yRot;
        clientSyncXRot = xRot;
        clientTicksSincePreciseSync = 0;

        if (!clientMotionFilterReady || this.position().distanceToSqr(target) > CLIENT_HARD_SNAP_DISTANCE_SQR) {
            clientMotionFilterReady = true;
            clientVisualPosition = target;
            clientVisualVelocity = normalizeClientSyncVelocity(this.getDeltaMovement(), true);
            this.setPos(target);
        }
        this.setRot(yRot, xRot);
    }

    @Override
    public void setDeltaMovement(Vec3 deltaMovement) {
        super.setDeltaMovement(deltaMovement);
        if (this.level() != null && this.level().isClientSide() && !localVelocityUpdate) {
            clientSyncVelocity = normalizeClientSyncVelocity(deltaMovement, true);
            if (!clientMotionFilterReady) {
                clientVisualVelocity = clientSyncVelocity;
            } else if (clientVisualVelocity.lengthSqr() < 1.0E-10D && deltaMovement.lengthSqr() > 1.0E-10D) {
                clientVisualVelocity = clientSyncVelocity;
            }
        }
    }

    private Vec3 normalizeClientSyncVelocity(Vec3 velocity) {
        return normalizeClientSyncVelocity(velocity, false);
    }

    private Vec3 normalizeClientSyncVelocity(Vec3 velocity, boolean preferLaunchDirection) {
        if (velocity.lengthSqr() < 1.0E-10D) {
            return Vec3.ZERO;
        }

        double speed = getSpeed();
        if (speed <= 0.0D || !Double.isFinite(speed)) {
            return velocity;
        }

        if (preferLaunchDirection && this.level() != null && this.level().isClientSide() && !clientMotionFilterReady) {
            Vec3 launchDirection = getSyncedLaunchDirection();
            if (launchDirection.lengthSqr() > 1.0E-10D) {
                return launchDirection.normalize().scale(speed);
            }
        }

        // Function: vanilla entity motion packets clamp components above about 3.9, so restore the intended bullet speed.
        return velocity.normalize().scale(speed);
    }

    public void setPreciseLaunchDirection(Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-10D ? Vec3.ZERO : direction.normalize();
        this.entityData.set(DATA_LAUNCH_DIR_X, (float) normalized.x);
        this.entityData.set(DATA_LAUNCH_DIR_Y, (float) normalized.y);
        this.entityData.set(DATA_LAUNCH_DIR_Z, (float) normalized.z);
    }

    public void setPreciseLaunchVelocity(Vec3 direction) {
        setPreciseLaunchDirection(direction);
        Vec3 normalized = direction.lengthSqr() < 1.0E-10D ? Vec3.ZERO : direction.normalize();
        this.setDeltaMovement(normalized.scale(getSpeed()));
    }

    public static Vec3 spawnBehindMuzzle(Vec3 muzzle, Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-10D ? Vec3.ZERO : direction.normalize();
        // Function: mirror CBC's stable launch setup by spawning just behind the muzzle along the same shot axis.
        return muzzle.subtract(normalized.scale(2.0D));
    }

    private Vec3 getSyncedLaunchDirection() {
        return new Vec3(
                this.entityData.get(DATA_LAUNCH_DIR_X),
                this.entityData.get(DATA_LAUNCH_DIR_Y),
                this.entityData.get(DATA_LAUNCH_DIR_Z)
        );
    }

    private void finishServerBulletMove(HitResult hitResult, boolean allowAfterMove) {
        lifeTime++;
        if (allowAfterMove) {
            afterServerBulletMove(hitResult);
        }
        if (this.isRemoved()) {
            return;
        }

        if (lifeTime >= getMaxLifeTime()) {
            explodeAndDiscardAfterLifetime();
        }
    }

    private void requestPreciseMotionChunkLoading(Vec3 from, Vec3 to) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!this.getType().is(RPLTags.PRECISE_MOTION)) {
            return;
        }
        if (from.distanceToSqr(to) < 1.0E-10D) {
            return;
        }

        // Function: precise-motion bullets must queue every crossed chunk before the next server tick so long shots keep aging and colliding.
        queueChunksAlongPath(serverLevel, from, to);
    }

    private void queueChunksAlongPath(ServerLevel level, Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        int chunkX = SectionPos.blockToSectionCoord(Mth.floor(from.x));
        int chunkZ = SectionPos.blockToSectionCoord(Mth.floor(from.z));
        int endChunkX = SectionPos.blockToSectionCoord(Mth.floor(to.x));
        int endChunkZ = SectionPos.blockToSectionCoord(Mth.floor(to.z));

        queueChunk(level, chunkX, chunkZ);
        if (chunkX == endChunkX && chunkZ == endChunkZ) {
            return;
        }
        if (Math.abs(dx) < 1.0E-10D && Math.abs(dz) < 1.0E-10D) {
            queueChunk(level, endChunkX, endChunkZ);
            return;
        }

        int stepX = Integer.compare(endChunkX, chunkX);
        int stepZ = Integer.compare(endChunkZ, chunkZ);
        double nextX = stepX == 0 ? Double.POSITIVE_INFINITY : firstChunkBoundaryT(from.x, dx, chunkX, stepX);
        double nextZ = stepZ == 0 ? Double.POSITIVE_INFINITY : firstChunkBoundaryT(from.z, dz, chunkZ, stepZ);
        double deltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 16.0D / Math.abs(dx);
        double deltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 16.0D / Math.abs(dz);

        while (chunkX != endChunkX || chunkZ != endChunkZ) {
            boolean advanceX = nextX <= nextZ;
            boolean advanceZ = nextZ <= nextX;
            if (advanceX) {
                chunkX += stepX;
                nextX += deltaX;
            }
            if (advanceZ) {
                chunkZ += stepZ;
                nextZ += deltaZ;
            }
            queueChunk(level, chunkX, chunkZ);
        }
    }

    private void queueChunk(ServerLevel level, int chunkX, int chunkZ) {
        RitchiesProjectileLib.queueForceLoad(level, chunkX, chunkZ);
    }

    private double findFirstUnloadedChunkT(Level level, Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        int chunkX = SectionPos.blockToSectionCoord(Mth.floor(from.x));
        int chunkZ = SectionPos.blockToSectionCoord(Mth.floor(from.z));

        if (!isChunkEntityTicking(level, chunkX, chunkZ)) {
            return 0.0D;
        }
        if (Math.abs(dx) < 1.0E-10D && Math.abs(dz) < 1.0E-10D) {
            return 1.0D;
        }

        int stepX = dx > 0.0D ? 1 : dx < 0.0D ? -1 : 0;
        int stepZ = dz > 0.0D ? 1 : dz < 0.0D ? -1 : 0;
        double nextX = stepX == 0 ? Double.POSITIVE_INFINITY : firstChunkBoundaryT(from.x, dx, chunkX, stepX);
        double nextZ = stepZ == 0 ? Double.POSITIVE_INFINITY : firstChunkBoundaryT(from.z, dz, chunkZ, stepZ);
        double deltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 16.0D / Math.abs(dx);
        double deltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 16.0D / Math.abs(dz);
        double segmentStart = 0.0D;

        while (segmentStart < 1.0D) {
            double segmentEnd = Math.min(1.0D, Math.min(nextX, nextZ));
            if (segmentEnd >= 1.0D) {
                return 1.0D;
            }

            boolean advanceX = nextX <= nextZ;
            boolean advanceZ = nextZ <= nextX;
            if (advanceX) {
                chunkX += stepX;
                nextX += deltaX;
            }
            if (advanceZ) {
                chunkZ += stepZ;
                nextZ += deltaZ;
            }

            segmentStart = segmentEnd;
            if (!isChunkEntityTicking(level, chunkX, chunkZ)) {
                return segmentStart;
            }
        }

        return 1.0D;
    }

    private boolean isChunkEntityTicking(Level level, int chunkX, int chunkZ) {
        if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return true;
        }
        BlockPos chunkOrigin = new BlockPos(SectionPos.sectionToBlockCoord(chunkX), 0, SectionPos.sectionToBlockCoord(chunkZ));
        // Function: bullets must only move into chunks where the entity itself will keep ticking.
        return serverLevel.isPositionEntityTicking(chunkOrigin);
    }

    private double firstChunkBoundaryT(double start, double delta, int chunk, int step) {
        double boundary = step > 0 ? (chunk + 1) * 16.0D : chunk * 16.0D;
        return (boundary - start) / delta;
    }

    private BlockHitResult missAt(Vec3 location, Vec3 previous) {
        Vec3 direction = previous.subtract(location);
        return BlockHitResult.miss(location, Direction.getNearest(direction.x, direction.y, direction.z), BlockPos.containing(location));
    }

    protected void explodeAndDiscardAfterLifetime() {
        // Function: bullets must be hard-cleared on timeout so missed shots cannot linger or trigger extra cleanup work.
        this.discard();
    }

    @Override
    public void remove(Entity.RemovalReason removalReason) {
        playDestroySoundOnce();
        super.remove(removalReason);
    }

    private void playDestroySoundOnce() {
        if (destructionSoundPlayed || this.level() == null || this.level().isClientSide()) {
            return;
        }

        destructionSoundPlayed = true;
        this.level().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                vsieSounds.BULLET_EXPLODE1.get(),
                SoundSource.HOSTILE,
                0.85F,
                1.0F
        );
    }

    // Keep the entity's authoritative rotation aligned with its velocity for hitbox debug and attached FX.
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

    // Function: subclasses own bullet speed; the base class only enforces constant velocity along current direction.
    public abstract double getSpeed();

    public int getRenderColor() {
        return 0xC080C080;
    }

    public float getRenderLength() {
        return 4.0F;
    }

    public float getRenderWidth() {
        return 1.0F;
    }

    public int getRenderStartTick() {
        return 10;
    }

    private Vec3 applyConstantSpeed() {
        Vec3 movement = this.getDeltaMovement();
        if (this.level() != null && this.level().isClientSide() && !clientMotionFilterReady) {
            Vec3 launchDirection = getSyncedLaunchDirection();
            if (launchDirection.lengthSqr() > 1.0E-10D) {
                movement = launchDirection.normalize().scale(getSpeed());
            }
        }
        if (movement.lengthSqr() < 1.0E-6D) {
            return movement;
        }

        Vec3 constantMovement = movement.normalize().scale(getSpeed());
        localVelocityUpdate = true;
        try {
            this.setDeltaMovement(constantMovement);
        } finally {
            localVelocityUpdate = false;
        }
        return constantMovement;
    }

    public abstract int startemitticks();
    public abstract int stopemitticks();
    protected void startLifecycleFx(FX fx) {
        var effect = new EntityEffectExecutor(fx, this.level(), this, EntityEffectExecutor.AutoRotate.XROT);
        // Function: lifecycle bullet FX must stay attached to the entity instead of being force-killed on start.
        effect.setForcedDeath(false);
        effect.start();
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        BlockState state = (level().getBlockState(pResult.getBlockPos()));
        if(state.isCollisionShapeFullBlock(level(), pResult.getBlockPos())) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity target = pResult.getEntity();
        target.hurt(this.level().damageSources().onFire(),15);
        this.discard();
    }


    // Allow subclasses to override FX data while keeping a particle-bullet fallback.
    public void setDataBase(BulletData dataBase) {
        this.dataBase = dataBase == null ? BulletData.createParticleBulletDefault() : dataBase;
        this.lifecycleFxStarted = false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("BreaksBlocks", this.breaksBlocks);
        tag.putInt("LifeTime", this.lifeTime);
        Vec3 launchDirection = getSyncedLaunchDirection();
        tag.putDouble("LaunchDirX", launchDirection.x);
        tag.putDouble("LaunchDirY", launchDirection.y);
        tag.putDouble("LaunchDirZ", launchDirection.z);
        if (launchSubLevelId != null) {
            tag.putUUID("LaunchSubLevelId", launchSubLevelId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BreaksBlocks")) {
            this.breaksBlocks = tag.getBoolean("BreaksBlocks");
        }
        if (tag.contains("LifeTime")) {
            this.lifeTime = tag.getInt("LifeTime");
        }
        if (tag.contains("LaunchDirX") && tag.contains("LaunchDirY") && tag.contains("LaunchDirZ")) {
            setPreciseLaunchDirection(new Vec3(
                    tag.getDouble("LaunchDirX"),
                    tag.getDouble("LaunchDirY"),
                    tag.getDouble("LaunchDirZ")
            ));
        }
        launchSubLevelId = tag.hasUUID("LaunchSubLevelId") ? tag.getUUID("LaunchSubLevelId") : null;
    }
}
