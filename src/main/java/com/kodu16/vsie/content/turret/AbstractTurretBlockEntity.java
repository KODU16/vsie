package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.content.cooldown.FireCooldown;
import com.kodu16.vsie.foundation.LoadedChunkRaycast;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import com.kodu16.vsie.registries.vsieSounds;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.slf4j.Logger;

public abstract class AbstractTurretBlockEntity extends SmartBlockEntity implements GeoBlockEntity, MenuProvider, IItemHandlerModifiable {
    Logger LOGGER = LogUtils.getLogger();
    private static final String AMMO_INVENTORY_TAG = "AmmoInventory";
    private static final String LINKED_CONTROL_SEAT_POS_TAG = "LinkedControlSeatPos";
    protected static final int DEFAULT_CONTROL_SEAT_ENERGY_COST_PER_TICK = 5;

    public static SerializableDataTicket<Boolean> TURRET_HAS_TARGET;

    public boolean hasInitialized = false;
    public Level level = null;
    public BlockPos pos = this.getBlockPos();
    public BlockState state = this.getBlockState();
    public boolean onShip = false;

    public Vec3 targetPos = new Vec3(0,0,0);
    public double targetDistance;

    public double getTargetDistance() {
        return targetDistance;
    }

    public float getLaserLayerRadius() {
        // Function: medium laser is the default TurretLaserLayer scale used by the shared renderer.
        return 0.25F;
    }

    public String getLaserLayerBoneName() {
        return "cannon1";
    }

    public double getLaserLayerYOffset() {
        // Function: legacy laser turrets render their beam from the aimed turret origin unless subclasses opt into pure bone-space emission.
        return getYAxisOffset();
    }

    public boolean usesSquareLaserLayerBeam() {
        return false;
    }

    public boolean transformsLaserLayerFromBone() {
        return false;
    }

    public boolean flipsLaserLayerDirection() {
        return true;
    }

    public int getLaserLayerLengthSegments() {
        return 12;
    }

    public float getLaserLayerRed(float t) {
        return Mth.lerp(t, 0.7F, 0.3F);
    }

    public float getLaserLayerGreen(float t) {
        return 0.4F;
    }

    public float getLaserLayerBlue(float t) {
        return 0.9F;
    }

    public float getLaserLayerAlpha(float t) {
        return 0.5F;
    }

    public @Nullable LivingEntity targetentity;
    private @Nullable SubLevel selectedtargetShip;
    public List<Vector3d> targetPreVelocity = new ArrayList<Vector3d>();

    public int aimtype = 0;

    public static SerializableDataTicket<Float> XROT;
    public static SerializableDataTicket<Float> YROT;
    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);


    public static Vector3d pivotPoint = new Vector3d();

    public int idleTicks = 0;
    protected double fireCooldownValue = -1.0D;
    // Function: every ordinary turret gets a shared 9-slot ammo buffer; energy turrets reject all inserts.
    private final ItemStackHandler ammoInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return acceptsAmmoStack(stack);
        }
    };
    public int muzzleFlashTicks = 0;

    private Vector3d FirePoint = null;
    private BlockPos lastShipShotHitBlockPos = BlockPos.ZERO;
    private boolean shipShotBlockedBySelfShip = false;

    public Vector3d getFirePoint() {
        return FirePoint;
    }

    public BlockPos getLastShipShotHitBlockPos() {
        return lastShipShotHitBlockPos;
    }

    protected boolean isShipShotBlockedBySelfShip() {
        return shipShotBlockedBySelfShip;
    }

    private static final double SEARCH_RADIUS = 128.0;

    public Vec3 currentworldpos = new Vec3(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
    protected TurretData turretData;
    private BlockPos linkedControlSeatPos = BlockPos.ZERO;

    public Vector3d worldXDirection = new Vector3d(0,0,0);
    public Vector3d worldYDirection = new Vector3d(0,0,0);
    public Vector3d worldZDirection = new Vector3d(0,0,0);

    protected AbstractTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.turretData = new TurretData();
    }

    public TurretData getData() {
        if (turretData == null) { turretData = new TurretData(); }
        return turretData;
    }

    public BlockPos getLinkedControlSeatPos() {
        return linkedControlSeatPos;
    }

    public void setLinkedControlSeatPos(BlockPos linkedControlSeatPos) {
        this.linkedControlSeatPos = linkedControlSeatPos == null ? BlockPos.ZERO : linkedControlSeatPos.immutable();
        setChanged();
    }

    public void modifyTargetType(int type) {
        this.level = this.getLevel();
        if (level == null || level.isClientSide) { return; }

        TurretData data = getData();

        if(type==4){
            this.aimtype = 2;
            data.flip(data.TARGET_SHIP);
            if ( data.isTargetsShip() ) { data.reset(( data.TARGET_HOSTILE | data.TARGET_PASSIVE | data.TARGET_PLAYER )); }
        }
        else{
            this.aimtype = 1;
            if(type==1){ data.flip(data.TARGET_HOSTILE); }
            if(type==2){ data.flip(data.TARGET_PASSIVE); }
            if(type==3){ data.flip(data.TARGET_PLAYER); }
        }
        if (data.getTargetStatus()==data.TARGET_MANUAL) { this.aimtype = 0; }

        else if ((data.getTargetStatus()&(~data.TARGET_SHIP))!=0) { data.reset(data.TARGET_SHIP); }
    }

    public void modifydefaultspin(int spinx, int spiny) {
        this.defaultspinx = spinx;
        this.defaultspiny = spiny;
    }

    public void setAimLimits(int minX, int maxX, int minY, int maxY) {
        getData().setAimLimits(minX, maxX, minY, maxY);
    }

    public boolean supportsBlockDestructionToggle() {
        return true;
    }

    public boolean breaksBlocksEnabled() {
        return getData().isBreaksBlocks();
    }

    public void setBreaksBlocksEnabled(boolean enabled) {
        getData().setBreaksBlocks(enabled);
    }

    public void toggleBreaksBlocksEnabled() {
        setBreaksBlocksEnabled(!breaksBlocksEnabled());
    }

    protected boolean breakTurretTargetBlockAsMined(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockState state = serverLevel.getBlockState(pos);
        if (!serverLevel.isLoaded(pos) || state.isAir() || state.getDestroySpeed(serverLevel, pos) < 0.0F) {
            return false;
        }
        // Function: turret terrain damage should look like a mined block break and never spawn item drops.
        serverLevel.levelEvent(2001, pos, Block.getId(state));
        boolean destroyed = serverLevel.destroyBlock(pos, false);
        if (destroyed) {
            maybeTriggerTurretBlockBreakTnt(serverLevel, pos);
        }
        return destroyed;
    }

    protected float getBlockBreakTntChance() {
        return 0.0F;
    }

    protected float getBlockBreakTntPower() {
        return 4.0F;
    }

    protected void maybeTriggerTurretBlockBreakTnt(ServerLevel level, BlockPos pos) {
        float chance = Mth.clamp(getBlockBreakTntChance(), 0.0F, 1.0F);
        if (chance <= 0.0F || level.random.nextFloat() >= chance) {
            return;
        }
        // Function: turret classes can opt into a TNT-like follow-up blast after a successful block break.
        level.explode(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                getBlockBreakTntPower(),
                false,
                Level.ExplosionInteraction.TNT
        );
    }

    public void tick() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) { return; }
        this.level = level;

        if (!hasInitialized){
            BlockPos pos = this.getBlockPos();
            BlockState state = this.getBlockState();
            Initialize.initialize(level,pos,state,pivotPoint);

            hasInitialized = true;
            return;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,pos);
        onShip = subLevel != null;
        if (subLevel != null) {
            Vector3d pivotoffsetworld = subLevel.logicalPose().transformNormal(this.turretData.getBasePivotOffset().normalize().mul(this.turretData.getBasePivotOffset().length())
            );
            this.turretData.setWorldPivotOffset(pivotoffsetworld);
        }

        if (muzzleFlashTicks > 0) {
            muzzleFlashTicks = muzzleFlashTicks - 1;
        }
        // Function: ordinary turrets must keep aiming and returning to defaults even when mounted in the normal level.
        currentworldpos = getTurretAimOriginWorld();
        acquireTargetByAimType();
        tryInvalidateTarget();
        tickFireCooldown(hasValidTarget());

        if (hasValidTarget()) {
            appendTargetVelocitySample();
            updateCurrentTargetPos();

            targetPos = getShootLocation(targetPos, targetPreVelocity, level, currentworldpos);
            updateTargetRot();
            this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
            this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
            if (xOK && yOK) {
                fireWhenLocked();
            }
        } else {
            returnToDefaultRotation();
        }
        //LogUtils.getLogger().warn("targetx:"+targetxrot+"y:"+targetyrot+"currentx:"+xRot0+"y:"+yRot0+"OK?"+xOK+yOK);
        this.setAnimData(XROT, xRot0);
        this.setAnimData(YROT, yRot0);
        this.markUpdated();
    }

    private void acquireTargetByAimType() {
        if (aimtype == 1) {
            tryFindTargetEntity();
        } else if (aimtype == 2) {
            tryFindtargetShip();
        }
    }

    private boolean hasValidTarget() {
        return (aimtype == 1 && isValidTargetEntity(targetentity))
                || (aimtype == 2 && isValidTargetShip(selectedtargetShip));
    }

    private void appendTargetVelocitySample() {
        if (targetPreVelocity.size() >= 5) {
            targetPreVelocity.remove(0);
        }
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPreVelocity.add(new Vector3d(targetentity.getDeltaMovement().x, targetentity.getDeltaMovement().y, targetentity.getDeltaMovement().z));
        } else if (aimtype == 2 && isValidTargetShip(selectedtargetShip)) {
            RigidBodyHandle rigidBodyHandle = RigidBodyHandle.of((ServerSubLevel) selectedtargetShip);
            targetPreVelocity.add(rigidBodyHandle.getLinearVelocity(new Vector3d()));
        }
    }

    private void updateCurrentTargetPos() {
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPos = new Vec3(
                    targetentity.getX(),
                    targetentity.getY(),
                    targetentity.getZ()
            );
            return;
        }
        if (aimtype == 2 && isValidTargetShip(selectedtargetShip)) {
            targetPos = getShipAimPoint(selectedtargetShip);
        }
    }

    private void fireWhenLocked() {
        //LogUtils.getLogger().warn("shooting");
        if (!isFireCooldownReady()) {
            return;
        }
        if (!canShootCurrentTarget()) {
            return;
        }
        if (!consumeAmmoForShot()) {
            return;
        }
        playTurretFireSound();
        if (aimtype == 1) {
            targetDistance = Vec.Distance(currentworldpos, targetPos);
            shootentity();
            consumeFireCooldown();
            muzzleFlashTicks = 10;
        } else if (aimtype == 2) {
            targetDistance = Vec.Distance(currentworldpos, targetPos);
            recordShipShotHitBlockPos();
            if (isShipShotBlockedBySelfShip()) {
                return;
            }
            shootship();
            consumeFireCooldown();
            muzzleFlashTicks = 10;
        }
    }



    // Function: use recent target velocity samples to lead moving targets.
    public abstract Vec3 getShootLocation(Vec3 vec, List<Vector3d> preV, Level lv, Vec3 pos);

    public abstract String getturrettype();

    public boolean isEnergyTurret() {
        return true;
    }

    public @Nullable Item getAmmoItem() {
        return null;
    }

    public abstract double getYAxisOffset();

    public abstract double getcannonlength();

    protected Vec3 getTurretLocalUpDirection() {
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        // Function: Yoffset is authored in turret-local space, so map it onto the mounted block's local up axis first.
        return Vec3.atLowerCornerOf(facing.getOpposite().getNormal());
    }

    protected Vec3 getTurretAimOriginWorld() {
        Vec3 localOrigin = Vec3.atCenterOf(this.getBlockPos()).add(getTurretLocalUpDirection().scale(getYAxisOffset()));
        Level level = this.getLevel();
        if (level == null) {
            return localOrigin;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        // Function: compute the server-side aim origin in turret-local coordinates before any sublevel rotation is applied.
        return subLevel == null ? localOrigin : subLevel.logicalPose().transformPosition(localOrigin);
    }

    public Vec3 getHudAimOriginWorld() {
        // Function: HUD marker projection uses the same raised aim origin as real turret firing.
        return getTurretAimOriginWorld();
    }

    protected @Nullable Vec3 getCannonMuzzleWorld(Vec3 target) {
        Vec3 origin = getTurretAimOriginWorld();
        Vec3 direction = getCurrentBarrelDirectionWorld();
        if (direction == null) {
            direction = target.subtract(origin);
        }
        if (direction.lengthSqr() < 1.0E-6D) {
            return null;
        }

        // Function: project cannon length from the raised origin along the current barrel axis.
        return origin.add(direction.normalize().scale(getcannonlength()));
    }

    protected @Nullable Vec3 getCurrentBarrelDirectionWorld() {
        if (this.getLevel() == null) {
            return null;
        }
        return getBarrelDirectionWorldForAngles(xRot0, yRot0);
    }

    public @Nullable Vec3 getRenderedBarrelDirectionWorld() {
        if (this.getLevel() == null) {
            return null;
        }
        // Function: HUD markers follow the same smoothed client-side angles the player is actually seeing.
        return getBarrelDirectionWorldForAngles(prevxrot, prevyrot);
    }

    protected @Nullable Vec3 getBarrelDirectionWorldForAngles(float pitch, float yaw) {
        updateWorldControlAxes();

        double worldYaw = -yaw;
        double worldPitch = pitch;
        double horizontal = Math.cos(worldPitch);
        double localX = Math.sin(worldYaw) * horizontal;
        double localY = Math.sin(worldPitch);
        double localZ = Math.cos(worldYaw) * horizontal;

        // Function: rebuild the muzzle axis from the same basis used by server-side aiming.
        Vec3 direction = new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z).scale(localX)
                .add(new Vec3(worldYDirection.x, worldYDirection.y, worldYDirection.z).scale(localY))
                .add(new Vec3(worldXDirection.x, worldXDirection.y, worldXDirection.z).scale(localZ));
        return direction.lengthSqr() < 1.0E-6D ? null : direction.normalize();
    }

    public abstract float getMaxSpinSpeed();

    public abstract int getCoolDown();

    public FireCooldown getFireCooldown() {
        return FireCooldown.cool1(getCoolDown());
    }

    public int getenergypertick() {
        // Function: linked turrets consume a baseline control-seat FE upkeep even before subclasses tune it.
        return DEFAULT_CONTROL_SEAT_ENERGY_COST_PER_TICK;
    }

    public int getControlSeatEnergyCostPerTick() {
        return getenergypertick();
    }

    public abstract void shootentity();

    public abstract void shootship();

    protected boolean canShootCurrentTarget() {
        return hasAmmoReady();
    }

    public boolean hasAmmoInventorySlots() {
        return !isEnergyTurret() && getAmmoItem() != null;
    }

    protected boolean hasAmmoReady() {
        if (!hasAmmoInventorySlots()) {
            return true;
        }
        for (int slot = 0; slot < ammoInventory.getSlots(); slot++) {
            if (!ammoInventory.extractItem(slot, 1, true).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    protected boolean consumeAmmoForShot() {
        if (!hasAmmoInventorySlots()) {
            return true;
        }
        for (int slot = 0; slot < ammoInventory.getSlots(); slot++) {
            ItemStack extracted = ammoInventory.extractItem(slot, 1, false);
            if (!extracted.isEmpty()) {
                setChanged();
                return true;
            }
        }
        return false;
    }

    protected boolean acceptsAmmoStack(ItemStack stack) {
        Item ammoItem = getAmmoItem();
        return ammoItem != null && hasAmmoInventorySlots() && stack.is(ammoItem);
    }

    public void dropStoredAmmo(Level level, BlockPos pos) {
        if (!hasAmmoInventorySlots()) {
            return;
        }
        // Function: preserve buffered ammo when a non-energy turret is broken.
        for (int slot = 0; slot < ammoInventory.getSlots(); slot++) {
            ItemStack stack = ammoInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack.copy());
                ammoInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    protected void playTurretFireSound() {
        SoundEvent soundEvent = getTurretFireSoundEvent();
        if (soundEvent == null || level == null || currentworldpos == null) {
            return;
        }

        // Function: turret fire sounds use the resolved world-space aim origin so sublevel turrets sound anchored in place.
        level.playSound(
                null,
                currentworldpos.x,
                currentworldpos.y,
                currentworldpos.z,
                soundEvent,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
    }

    protected @Nullable SoundEvent getTurretFireSoundEvent() {
        return switch (getturrettype()) {
            case "particle" -> vsieSounds.PARTICLE_TURRET_FIRE.get();
            default -> null;
        };
    }

    public IItemHandlerModifiable getItemHandler() {
        return this;
    }

    protected void tickFireCooldown(boolean fireRequested) {
        FireCooldown cooldown = getFireCooldown();
        if (idleTicks > 0) {
            idleTicks--;
        }
        ensureFireCooldownValue(cooldown);
        if (cooldown.usesValue() && !fireRequested) {
            fireCooldownValue = Math.min(cooldown.maxValue(), fireCooldownValue + cooldown.recoveryPerTick());
        }
    }

    protected boolean isFireCooldownReady() {
        FireCooldown cooldown = getFireCooldown();
        ensureFireCooldownValue(cooldown);
        return idleTicks <= 0 && (!cooldown.usesValue() || fireCooldownValue > 0);
    }

    public boolean isHudFireReady() {
        // Function: HUD warning state only covers heat/cooldown and ammo availability, not target or trigger state.
        return isFireCooldownReady() && hasAmmoReady();
    }

    protected void consumeFireCooldown() {
        FireCooldown cooldown = getFireCooldown();
        ensureFireCooldownValue(cooldown);
        idleTicks = cooldown.intervalTicks();
        if (cooldown.usesValue()) {
            fireCooldownValue = Math.max(0, fireCooldownValue - 1);
        }
    }

    public int getCooldownHudValue() {
        FireCooldown cooldown = getFireCooldown();
        ensureFireCooldownValue(cooldown);
        return cooldown.usesValue() ? Mth.floor(fireCooldownValue) : Math.max(0, idleTicks);
    }

    public int getCooldownHudMax() {
        FireCooldown cooldown = getFireCooldown();
        return cooldown.usesValue() ? cooldown.maxValue() : cooldown.intervalTicks();
    }

    public boolean isCooldownHudRemaining() {
        return !getFireCooldown().usesValue();
    }

    private void ensureFireCooldownValue(FireCooldown cooldown) {
        if (!cooldown.usesValue()) {
            return;
        }
        if (fireCooldownValue < 0) {
            fireCooldownValue = cooldown.maxValue();
        } else if (fireCooldownValue > cooldown.maxValue()) {
            fireCooldownValue = cooldown.maxValue();
        }
    }

    protected Vector3d getTurretPivotInGeoPixels() {
        return new Vector3d(0.0, 0.0, 0.0);
    }

    protected Vector3d getCannonPivotInGeoPixels() {
        return new Vector3d(0.0, 0.0, 0.0);
    }


    public void updateenemy(ArrayList<SubLevel> enemyshipsData) {
        this.getData().enemyShipsData = enemyshipsData;
    }

    public void clearControlSeatTargeting() {
        // Function: power loss must immediately clear stale targets so autonomous turrets cannot keep firing on old data.
        this.getData().enemyShipsData = new ArrayList<>();
        this.targetentity = null;
        this.selectedtargetShip = null;
        this.targetPreVelocity.clear();
        this.targetPos = Vec3.ZERO;
        this.targetDistance = 0.0D;
    }

    protected boolean shouldApplyAutoAimLimits() {
        // Function: base turrets always gate automatic target acquisition through the configured aim windows.
        return true;
    }

    protected void updateWorldControlAxes() {
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

        SubLevel ship = ServerShipUtils.getSubLevelAtBlockPos(this.getLevel(), this.getBlockPos());
        if (ship != null) {
            worldXDirection = ship.logicalPose().transformNormal(new Vector3d(localForward.x, localForward.y, localForward.z)).normalize();
            worldYDirection = ship.logicalPose().transformNormal(new Vector3d(localUp.x, localUp.y, localUp.z)).normalize();
            worldZDirection = ship.logicalPose().transformNormal(new Vector3d(localRight.x, localRight.y, localRight.z)).normalize();
            return;
        }

        worldXDirection = new Vector3d(localForward.x, localForward.y, localForward.z).normalize();
        worldYDirection = new Vector3d(localUp.x, localUp.y, localUp.z).normalize();
        worldZDirection = new Vector3d(localRight.x, localRight.y, localRight.z).normalize();
    }

    protected double[] computeTargetAimAngles(Vec3 targetWorldPos) {
        updateWorldControlAxes();

        Vec3 toTargetWorld = new Vec3(
                targetWorldPos.x - currentworldpos.x,
                targetWorldPos.y - currentworldpos.y,
                targetWorldPos.z - currentworldpos.z
        );
        if (toTargetWorld.lengthSqr() < 1.0E-6D) {
            return null;
        }
        toTargetWorld = toTargetWorld.normalize();

        double localZ = toTargetWorld.dot(new Vec3(worldXDirection.x, worldXDirection.y, worldXDirection.z));
        double localY = toTargetWorld.dot(new Vec3(worldYDirection.x, worldYDirection.y, worldYDirection.z));
        double localX = toTargetWorld.dot(new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z));

        double yaw = Math.atan2(localX, localZ);
        double pitch = Math.atan2(localY, Math.sqrt(localX * localX + localZ * localZ));
        return new double[]{pitch, -yaw};
    }

    protected boolean isTargetWithinAimLimits(Vec3 targetWorldPos) {
        if (!shouldApplyAutoAimLimits()) {
            return true;
        }

        double[] aimAngles = computeTargetAimAngles(targetWorldPos);
        if (aimAngles == null) {
            return false;
        }

        TurretData data = getData();
        double xDegrees = Math.toDegrees(aimAngles[0]);
        double yDegrees = Math.toDegrees(aimAngles[1]);
        return isAngleInsideWindow(xDegrees, data.aimLimitMinX, data.aimLimitMaxX)
                && isAngleInsideWindow(yDegrees, data.aimLimitMinY, data.aimLimitMaxY);
    }

    protected double[] clampAimAnglesToConfiguredWindow(double[] aimAngles) {
        if (aimAngles == null) {
            return null;
        }

        TurretData data = getData();
        double clampedPitch = Math.toRadians(clampAngleDegreesToWindow(
                Math.toDegrees(aimAngles[0]),
                data.aimLimitMinX,
                data.aimLimitMaxX
        ));
        double clampedYaw = Math.toRadians(clampAngleDegreesToWindow(
                Math.toDegrees(aimAngles[1]),
                data.aimLimitMinY,
                data.aimLimitMaxY
        ));
        return new double[]{clampedPitch, clampedYaw};
    }

    protected double clampAngleDegreesToWindow(double angleDegrees, int minDegrees, int maxDegrees) {
        if (minDegrees <= TurretData.DEFAULT_AIM_LIMIT_MIN && maxDegrees >= TurretData.DEFAULT_AIM_LIMIT_MAX) {
            return normalizeDegrees(angleDegrees);
        }

        double normalizedAngle = normalizeDegrees(angleDegrees);
        if (isAngleInsideWindow(normalizedAngle, minDegrees, maxDegrees)) {
            return normalizedAngle;
        }

        double normalizedMin = normalizeDegrees(minDegrees);
        double normalizedMax = normalizeDegrees(maxDegrees);
        double distanceToMin = angularDistanceDegrees(normalizedAngle, normalizedMin);
        double distanceToMax = angularDistanceDegrees(normalizedAngle, normalizedMax);
        // Function: manual heavy aiming should stop on the nearest configured boundary instead of rotating through it.
        return distanceToMin <= distanceToMax ? normalizedMin : normalizedMax;
    }

    protected boolean isAngleInsideWindow(double angleDegrees, int minDegrees, int maxDegrees) {
        // Function: the default [-180, 180] span means unrestricted rotation on that axis.
        if (minDegrees <= TurretData.DEFAULT_AIM_LIMIT_MIN && maxDegrees >= TurretData.DEFAULT_AIM_LIMIT_MAX) {
            return true;
        }

        double normalizedAngle = normalizeDegrees(angleDegrees);
        double normalizedMin = normalizeDegrees(minDegrees);
        double normalizedMax = normalizeDegrees(maxDegrees);
        if (normalizedMin <= normalizedMax) {
            return normalizedAngle >= normalizedMin && normalizedAngle <= normalizedMax;
        }
        // Function: allow wrap-around windows such as [150, -150] for a rear-only firing arc.
        return normalizedAngle >= normalizedMin || normalizedAngle <= normalizedMax;
    }

    protected double normalizeDegrees(double degrees) {
        double normalized = degrees % 360.0D;
        if (normalized <= -180.0D) {
            normalized += 360.0D;
        } else if (normalized > 180.0D) {
            normalized -= 360.0D;
        }
        return normalized;
    }

    private double angularDistanceDegrees(double fromDegrees, double toDegrees) {
        return Math.abs(normalizeDegrees(fromDegrees - toDegrees));
    }

    private void tryInvalidateTarget() {
        if(aimtype==1) {
            if(!isValidTargetEntity(targetentity)) {
                setAnimData(TURRET_HAS_TARGET, false);
                targetentity = null;
                targetDistance = 0;
                targetPreVelocity.clear();
            }
        }
        else if(aimtype==2) {
            if(!isValidTargetShip(selectedtargetShip)) {
                setAnimData(TURRET_HAS_TARGET, false);
                selectedtargetShip = null;
                targetDistance = 0;
                targetPreVelocity.clear();
            }
        }
    }

    protected boolean shouldThrottleEntityTargetSearch() {
        return true;
    }

    public void tryFindTargetEntity() {
        if (targetentity != null && targetentity.isAlive()) return;

        // Function: most turrets stagger target scans, but fast trackers can disable the delay.
        if (shouldThrottleEntityTargetSearch() && (this.getLevel().getGameTime() + this.hashCode()) % 5 != 0) return;

        AABB searchBox = new AABB(
                currentworldpos.x - SEARCH_RADIUS,
                currentworldpos.y - SEARCH_RADIUS,
                currentworldpos.z - SEARCH_RADIUS,
                currentworldpos.x + SEARCH_RADIUS,
                currentworldpos.y + SEARCH_RADIUS,
                currentworldpos.z + SEARCH_RADIUS
        );

        List<LivingEntity> candidates = this.getLevel().getEntitiesOfClass(LivingEntity.class, searchBox, this::isValidTargetEntity);

        if (candidates.isEmpty()) {
            return;
        }

        targetentity = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z)))
                .orElse(null);
        this.targetPos = new Vec3(
                targetentity.getX(),
                targetentity.getY(),
                targetentity.getZ()
        );
        setChanged();
    }

    public void tryFindtargetShip() {
        ArrayList<SubLevel> enemylist = getData().enemyShipsData;
        //LogUtils.getLogger().warn("enemy list size:"+getData().enemyShipsData.size());
        if (enemylist.isEmpty()) {
            selectedtargetShip = null;
            targetPos = Vec3.ZERO;
            targetDistance = 0;
            targetPreVelocity.clear();
            setAnimData(TURRET_HAS_TARGET, false);
            return;
        }
        if (selectedtargetShip != null && !enemylist.contains(selectedtargetShip)) {
            selectedtargetShip = null;
        }
        if (isValidTargetShip(selectedtargetShip)) return;

        this.selectedtargetShip = findNextVisibleTargetShip(enemylist);

        if (this.selectedtargetShip != null) {
            this.targetPos = getShipAimPoint(this.selectedtargetShip);
            setChanged();
        } else {
            targetPos = Vec3.ZERO;
            targetDistance = 0;
            targetPreVelocity.clear();
            setAnimData(TURRET_HAS_TARGET, false);
        }
    }

    private @Nullable SubLevel findNextVisibleTargetShip(List<SubLevel> enemylist) {
        int currentIndex = selectedtargetShip == null ? -1 : enemylist.indexOf(selectedtargetShip);
        int startIndex = currentIndex >= 0 ? currentIndex + 1 : 0;
        for (int offset = 0; offset < enemylist.size(); offset++) {
            SubLevel candidate = enemylist.get(Math.floorMod(startIndex + offset, enemylist.size()));
            // Function: scan enemy sublevels in order and lock only the first one with a visible aim point.
            if (isValidTargetShip(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public boolean isValidTargetEntity(@Nullable LivingEntity e) {

        if (e == null) {
            return false;
        }
        if (!e.isAlive()) {
            return false;
        }
        MobCategory category = e.getType().getCategory();
        if (getData().isTargetsHostile() && category.isFriendly()
                || getData().isTargetsPassive() && !category.isFriendly()
                || getData().isTargetsPlayers() && e instanceof Player player && player.isCreative()) {
            return false;
        }

        double distSq = e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        if (distSq > SEARCH_RADIUS * SEARCH_RADIUS) {
            return false;
        }

        Vec3 candidatePos = new Vec3(e.getX(), e.getY(), e.getZ());
        if (!isTargetWithinAimLimits(candidatePos)) {
            return false;
        }
        return canSeeTarget(candidatePos);
    }

    protected boolean isValidTargetShip(SubLevel ship) {
        if (ship == null) {
            return false;
        }
        Vec3 shippos = ServerShipUtils.getStructureCenterWorld(ship);
        Vec3 pos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        double distance = Vec.Distance(pos, shippos);
        if (distance > 1280) {
            return false;
        }
        if (!canAimAtShip(ship)) {
            return false;
        }
        return canSeeShipTarget(ship);
    }

    private boolean canAimAtShip(SubLevel ship) {
        for (Vec3 samplePoint : getShipAimCandidates(ship)) {
            if (isTargetWithinAimLimits(samplePoint)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSeeShipTarget(SubLevel ship) {
        for (Vec3 samplePoint : getShipAimCandidates(ship)) {
            if (isTargetWithinAimLimits(samplePoint) && traceTargetShipBlock(ship, samplePoint) != null) {
                return true;
            }
        }
        return false;
    }

    protected Vec3 getShipAimPoint(SubLevel ship) {
        for (Vec3 samplePoint : getShipAimCandidates(ship)) {
            if (isTargetWithinAimLimits(samplePoint) && traceTargetShipBlock(ship, samplePoint) != null) {
                return samplePoint;
            }
        }
        return ServerShipUtils.getStructureCenterWorld(ship);
    }

    protected @Nullable SubLevel getSelectedTargetShip() {
        // Function: subclasses need the locked ship to convert visual hit positions back to sublevel body positions.
        return selectedtargetShip;
    }

    private List<Vec3> getShipAimCandidates(SubLevel ship) {
        BoundingBox3dc worldAabb = ship.boundingBox();
        double minX = worldAabb.minX();
        double minY = worldAabb.minY();
        double minZ = worldAabb.minZ();
        double maxX = worldAabb.maxX();
        double maxY = worldAabb.maxY();
        double maxZ = worldAabb.maxZ();
        double centerX = (minX + maxX) * 0.5;
        double centerY = (minY + maxY) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;

        List<Vec3> samplePoints = new ArrayList<>();
        // Function: center-first probing avoids locking onto exposed bounding-box air.
        samplePoints.add(new Vec3(centerX, centerY, centerZ));
        samplePoints.add(new Vec3(minX, centerY, centerZ));
        samplePoints.add(new Vec3(maxX, centerY, centerZ));
        samplePoints.add(new Vec3(centerX, minY, centerZ));
        samplePoints.add(new Vec3(centerX, maxY, centerZ));
        samplePoints.add(new Vec3(centerX, centerY, minZ));
        samplePoints.add(new Vec3(centerX, centerY, maxZ));
        samplePoints.add(new Vec3(minX, maxY, minZ));
        samplePoints.add(new Vec3(minX, maxY, maxZ));
        samplePoints.add(new Vec3(maxX, maxY, minZ));
        samplePoints.add(new Vec3(maxX, maxY, maxZ));
        samplePoints.add(new Vec3(centerX, centerY, centerZ));
        return samplePoints;
    }

    private @Nullable BlockHitResult traceTargetShipBlock(SubLevel ship, Vec3 aimPoint) {
        Level level = this.getLevel();
        if (level == null || ship == null) {
            return null;
        }
        Vec3 from = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        BlockHitResult hitResult = LoadedChunkRaycast.clipIgnoringUnloadedChunks(
                level,
                from,
                aimPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty(),
                this::configureShipTargetClipContext
        );
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos hitPos = hitResult.getBlockPos();
        // Function: sublevel targets must resolve to an actual block on the selected enemy ship, not AABB air.
        if (isBlockOnSameShipAsTurret(hitPos) || !isBlockOnShip(ship, hitPos)) {
            return null;
        }
        return hitResult;
    }

    private void configureShipTargetClipContext(ClipContext context) {
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        SubLevel ownShip = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        if (ownShip != null && context instanceof ClipContextExtension extension) {
            // Function: ship target rays must pass through the turret's own sublevel before testing enemies.
            extension.sable$setIgnoredSubLevel(ownShip);
        }
    }

    private boolean canSeeTarget(Vec3 pos) {
        Vec3 turretpos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        Vec3 targetPos = new Vec3(pos.x(), pos.y(), pos.z());
        Vec3 lookVec = turretpos.vectorTo(targetPos).normalize().scale(0.75F);
        return LoadedChunkRaycast.clipIgnoringUnloadedChunks(
                this.getLevel(),
                turretpos.add(lookVec),
                targetPos,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ).getType().equals(HitResult.Type.MISS);
    }

    private boolean isBlockOnSameShipAsTurret(BlockPos blockPos) {
        Level level = this.getLevel();
        if (level == null) {
            return false;
        }
        SubLevel turretShip = ServerShipUtils.getSubLevelAtBlockPos(level,pos);
        if (turretShip == null) {
            return false;
        }
        SubLevel hitShip = ServerShipUtils.getSubLevelAtBlockPos(level,blockPos);
        return hitShip != null && hitShip.hashCode() == turretShip.hashCode();
    }

    private boolean isBlockOnShip(SubLevel ship, BlockPos blockPos) {
        Level level = this.getLevel();
        if (level == null || ship == null) {
            return false;
        }
        SubLevel hitShip = ServerShipUtils.getSubLevelAtBlockPos(level, blockPos);
        return hitShip != null && hitShip.hashCode() == ship.hashCode();
    }

    protected void recordShipShotHitBlockPos() {
        recordShipShotHitBlockPos(selectedtargetShip);
    }

    protected void recordShipShotHitBlockPos(@Nullable SubLevel targetShip) {
        Level level = this.getLevel();
        this.shipShotBlockedBySelfShip = false;
        if (level == null) {
            this.lastShipShotHitBlockPos = BlockPos.ZERO;
            return;
        }
        Vec3 from = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        List<Vec3> shotCandidates = new ArrayList<>();
        shotCandidates.add(targetPos);
        if (isValidTargetShip(targetShip)) {
            for (Vec3 candidate : getShipAimCandidates(targetShip)) {
                if (Vec.Distance(candidate,targetPos) > 1.0e-6) {
                    shotCandidates.add(candidate);
                }
            }
        }

        for (Vec3 shotPoint : shotCandidates) {
            Vec3 to = new Vec3(shotPoint.x, shotPoint.y, shotPoint.z);
            BlockHitResult hitResult = LoadedChunkRaycast.clipIgnoringUnloadedChunks(
                    level,
                    from,
                    to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty(),
                    this::configureShipTargetClipContext
            );
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hitResult.getBlockPos();
                if (isBlockOnSameShipAsTurret(hitPos)) {
                    this.shipShotBlockedBySelfShip = true;
                    this.lastShipShotHitBlockPos = BlockPos.ZERO;
                    return;
                }
                // Function: ship shots only accept body-space hits on the currently selected target.
                if (targetShip != null && !isBlockOnShip(targetShip, hitPos)) {
                    continue;
                }
                this.targetPos = shotPoint;
                this.lastShipShotHitBlockPos = hitPos;
                return;
            }
        }
        this.lastShipShotHitBlockPos = BlockPos.ZERO;
    }

    @Override
    public double getTick(Object BlockEntity) {
        return RenderUtil.getCurrentTick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        markUpdated();
    }

    public void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        //if(!this.level.isClientSide()) sendUpdatePacket();
    }


    @Override
    public Component getDisplayName() {
        // Function: control-seat HUD expects every turret to expose a stable four-letter code.
        return switch (getturrettype()) {
            case "particle" -> Component.literal("PART");
            case "small_laser" -> Component.literal("SMLR");
            case "medium_laser" -> Component.literal("MDLR");
            case "basic_ciws" -> Component.literal("CIWS");
            case "heavy_laser" -> Component.literal("HVLR");
            case "heavy_electromagnet" -> Component.literal("HVEM");
            default -> Component.literal("TRET");
        };
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TurretContainerMenu(containerId, inv, this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        read(tag, registries, true);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put(AMMO_INVENTORY_TAG, ammoInventory.serializeNBT(registries));
        tag.putInt("aimtype", aimtype);
        tag.putInt("configregister", turretData.configRegister);
        tag.putDouble("distance", this.getTargetDistance());
        tag.putFloat("xrot", this.targetxrot);
        tag.putFloat("yrot", this.targetyrot);
        tag.putInt("defaultxrot", this.defaultspinx);
        tag.putInt("defaultyrot", this.defaultspiny);
        tag.putInt("aimLimitMinX", getData().aimLimitMinX);
        tag.putInt("aimLimitMaxX", getData().aimLimitMaxX);
        tag.putInt("aimLimitMinY", getData().aimLimitMinY);
        tag.putInt("aimLimitMaxY", getData().aimLimitMaxY);
        tag.putBoolean("breaksBlocks", getData().isBreaksBlocks());
        tag.putInt("muzzleFlashTicks", this.muzzleFlashTicks);
        tag.putDouble("fireCooldownValue", this.fireCooldownValue);
        tag.putLong(LINKED_CONTROL_SEAT_POS_TAG, this.linkedControlSeatPos.asLong());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(AMMO_INVENTORY_TAG)) {
            ammoInventory.deserializeNBT(registries, tag.getCompound(AMMO_INVENTORY_TAG));
        }
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        if (tag.contains("aimtype")) {this.aimtype = tag.getInt("aimtype");}
        if (tag.contains("configregister")) {turretData.configRegister = tag.getInt("configregister");}
        if (tag.contains("distance")) {this.targetDistance = tag.getDouble("distance");}
        if (tag.contains("xrot")) {this.targetxrot = tag.getFloat("xrot");}
        if (tag.contains("yrot")) {this.targetyrot = tag.getFloat("yrot");}
        if (tag.contains("defaultyrot")) {this.defaultspiny = tag.getInt("defaultyrot");}
        if (tag.contains("defaultxrot")) {this.defaultspinx = tag.getInt("defaultxrot");}
        if (tag.contains("aimLimitMinX")) {getData().aimLimitMinX = tag.getInt("aimLimitMinX");}
        if (tag.contains("aimLimitMaxX")) {getData().aimLimitMaxX = tag.getInt("aimLimitMaxX");}
        if (tag.contains("aimLimitMinY")) {getData().aimLimitMinY = tag.getInt("aimLimitMinY");}
        if (tag.contains("aimLimitMaxY")) {getData().aimLimitMaxY = tag.getInt("aimLimitMaxY");}
        if (tag.contains("breaksBlocks")) {getData().setBreaksBlocks(tag.getBoolean("breaksBlocks"));}
        if (tag.contains("muzzleFlashTicks")) {this.muzzleFlashTicks = tag.getInt("muzzleFlashTicks");}
        if (tag.contains("fireCooldownValue")) {this.fireCooldownValue = tag.getDouble("fireCooldownValue");}
        if (tag.contains(LINKED_CONTROL_SEAT_POS_TAG, Tag.TAG_LONG)) {
            this.linkedControlSeatPos = BlockPos.of(tag.getLong(LINKED_CONTROL_SEAT_POS_TAG));
        } else {
            this.linkedControlSeatPos = BlockPos.ZERO;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public float closestReachableX(float current, float maxChange, float target) {
        float delta = target - current;
        delta = (delta + Mth.PI) % (Mth.TWO_PI) - Mth.PI;

        float minAllowed = -maxChange;
        float maxAllowed = maxChange;

        float move;
        if (delta < minAllowed) {
            move = minAllowed;
            this.xOK = false;
        } else if (delta > maxAllowed) {
            move = maxAllowed;
            this.xOK = false;
        } else {
            move = delta;
            this.xOK = true;
        }

        return current + move;
    }

    public float closestReachableY(float current, float maxChange, float target) {
        float delta = target - current;
        delta = (delta + Mth.PI) % (Mth.TWO_PI) - Mth.PI;

        float minAllowed = -maxChange;
        float maxAllowed = maxChange;

        float move;
        if (delta < minAllowed) {
            move = minAllowed;
            this.yOK = false;
        } else if (delta > maxAllowed) {
            move = maxAllowed;
            this.yOK = false;
        } else {
            move = delta;
            this.yOK = true;
        }

        return current + move;
    }

    private void updateTargetRot() {
        double[] aimAngles = computeTargetAimAngles(targetPos);
        if (aimAngles == null) {
            return;
        }

        this.targetxrot = (float) aimAngles[0];
        this.targetyrot = (float) aimAngles[1];
    }

    public void returnToDefaultRotation() {
        this.targetxrot = this.defaultspinx;
        this.targetyrot = this.defaultspiny;
        this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot*Mth.PI/180);
        this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot*Mth.PI/180);
    }

    public void setFirePoint(Vector3d postofire) {
        if (postofire == null) {
            this.FirePoint = null;
            return;
        }
        // Store an absolute sublevel-space muzzle point captured from the Geckolib firepoint bone.
        this.FirePoint = new Vector3d(postofire);
    }

    public float xRot0 = 0;
    public float yRot0 = 0;
    public float prevxrot = 0;
    public float prevyrot = 0;
    public boolean xOK = false;
    public boolean yOK = false;
    public float targetxrot = 0;
    public float targetyrot = 0;
    public int defaultspinx = 0;
    public int defaultspiny = 0;



    protected final float MAX_OMEGA_YAW = 1;
    protected final float MAX_OMEGA_PITCH = 1;

    protected float defaultYaw = 0;
    protected float defaultPitch = 0;

    protected float currentYaw = 0;
    protected float currentPitch = 0;


    public class servo{
        // d^2/dt^2 angle = Kp * (target - angle) - Kd * d/dt angle
        // Phi = Kp/(s^2 + Kd*s + Kp)
        // Omega_N = sqrt(Kp)
        // Epsilon = Kd / ( 2*sqrt(Kp) )

        public float angle = 0; // rad
        public float omega = 0;
        public float beta  = 0;
        private float Kp;
        private float Kd;
        private final float dt = 1f / 20;
        private static final float PI = (float) Math.PI;

        public boolean isStable = false;

        public void servoInitial(float Kp, float Kd){
            this.Kp = Kp;
            this.Kd = Kd;
        }

        public void servoAutoInitial(int stableTick){
            float second = stableTick * dt;
            this.Kp = 32f / (second * second);
            this.Kd = 8f / second;
        }

        private static float angleNormalize(float angle) {
            angle %= 2 * PI;
            if (angle > PI) angle -= 2 * PI;
            else if (angle < PI) angle += 2 * PI;
            return angle;
        }

        public boolean updateServo(float target){
            float error = angleNormalize( target - this.angle );
            this.beta = Kp * error - Kd* this.omega;

            this.omega += this.beta * dt;
            this.angle += this.omega * dt;

            this.angle = angleNormalize(this.angle);
            this.isStable = (error <=0.034);;

            return this.isStable;
        }
    }
    
    private double[] doSightTransform(
            Vector3d dirInWorld,
            SubLevel subLevel
    ){
        Vector3d dirInShip=subLevel.logicalPose().transformNormalInverse(dirInWorld);
        Vector3d dirInModel=this.turretData.getCoordAxis().transform(dirInShip);

        double yaw = Math.atan2(
                dirInModel.x,
                dirInModel.z
        );
        double pitch=Math.atan2(
                Math.sqrt(dirInModel.x * dirInModel.x + dirInModel.z * dirInModel.z),
                dirInModel.y
        );

        return new double[]{yaw,pitch};
    }

    public double[] sightTransformByDir(
            Vector3d dirInWorld,
            SubLevel subLevel
    ){
        return doSightTransform(dirInWorld, subLevel);
    }

    public double[] sightTransformByVec3Pos(
            Vector3d TargetPosInWorld,
            SubLevel subLevel
    ){
        Vector3d TurretPos = new Vector3d(this.getBlockPos().getX(),this.getBlockPos().getY(),this.getBlockPos().getZ());
        TurretPos.add(this.getData().basePivotOffset);
        Vector3d dirInWorld = TargetPosInWorld.sub(TurretPos);

        return doSightTransform(dirInWorld, subLevel);
    }
    public double[] sightTransformByBlockPos(
            BlockPos TargetBlockPosInWorld,
            SubLevel subLevel
    ){
        Vector3d TurretPos = new Vector3d(this.getBlockPos().getX(),this.getBlockPos().getY(),this.getBlockPos().getZ());
        TurretPos.add(this.getData().basePivotOffset);
        Vector3d TargetPosInWorld = new Vector3d(TargetBlockPosInWorld.getX(),TargetBlockPosInWorld.getY(),TargetBlockPosInWorld.getZ());
        Vector3d dirInWorld = TargetPosInWorld.sub(TurretPos);

        return doSightTransform(dirInWorld, subLevel);
    }
    @Override
    public int getSlots() {
        return ammoInventory.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return ammoInventory.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return ammoInventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ammoInventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return ammoInventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return ammoInventory.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        ammoInventory.setStackInSlot(slot, stack);
        setChanged();
    }
}

