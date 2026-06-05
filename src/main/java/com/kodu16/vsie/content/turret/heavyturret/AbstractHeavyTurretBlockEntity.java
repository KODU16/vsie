package com.kodu16.vsie.content.turret.heavyturret;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.Initialize;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;

import java.util.List;

public abstract class AbstractHeavyTurretBlockEntity extends AbstractTurretBlockEntity {
    private static final double MANUAL_UNALIGNED_FIRE_FALLBACK_DISTANCE = 256.0D;

    protected AbstractHeavyTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // Function: heavy turrets own their extended targeting and channel state.
        this.turretData = new TurretData();
    }

    private volatile Vec3 targetPos = new Vec3(0,0,0);
    private volatile SubLevel targetShip = null;
    private volatile int armedChannelOfCtrl = 0;
    private volatile boolean currentTargetingAutomatic = false;
    private boolean manualFireQueued = false;
    @Nullable
    private Vec3 fireTargetOverride = null;


    public abstract int getmaxpitchdowndegrees();

    protected Vec3 getHeavyTurretTargetPos() {
        // Function: subclasses need the current heavy turret target for projectile spawning.
        return fireTargetOverride != null ? fireTargetOverride : targetPos;
    }

    protected boolean isCurrentHeavyTargetAutomatic() {
        // Function: subclasses need to distinguish locked targets from manual sight points when firing.
        return currentTargetingAutomatic;
    }

    public int getControlSeatEnergyCostPerTick() {
        // Function: heavy turrets now share the same per-tick control-seat upkeep model as other linked peripherals.
        return getenergypertick();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public void tick() {
        if (this.getLevel() == null || this.getLevel().isClientSide()) { return; }

        tickFireCooldown(isHeavyFireRequested());

        if (!hasInitialized){
            BlockPos pos = this.getBlockPos();
            BlockState state = this.getBlockState();
            Initialize.initialize(this.getLevel(),pos,state,pivotPoint);

            hasInitialized = true;
            return;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(this.getLevel(), this.getBlockPos());
        onShip = subLevel != null;

        // Function: heavy turrets share the same mount-aware Yoffset transform as ordinary turrets.
        currentworldpos = getTurretAimOriginWorld();

        refreshTrackedShipTarget();

        boolean hasTargetPos = hasHeavyTargetPos();
        if (hasTargetPos) {
            updateTargetRot();
            this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
            this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
            setAnimData(TURRET_HAS_TARGET, true);

            boolean firingAlignmentSatisfied = xOK && yOK;
            // Function: automatic heavy fire still waits for alignment, while manual fire can shoot along the current barrel axis.
            if (isFireCooldownReady() && shouldFireWhenReady() && (firingAlignmentSatisfied || !currentTargetingAutomatic)) {
                Vec3 fireTarget = resolveCurrentFireTarget(firingAlignmentSatisfied);
                if (fireTarget == null) {
                    return;
                }
                // Function: heavy turrets share the same internal ammo row, so each aligned shot must spend one round first.
                if (!consumeAmmoForShot()) {
                    return;
                }
                fireTargetOverride = fireTarget;
                try {
                    targetDistance = Vec.Distance(currentworldpos, fireTarget);
                    shootship();
                    consumeFireCooldown();
                } finally {
                    fireTargetOverride = null;
                }
            }
        } else {
            setAnimData(TURRET_HAS_TARGET, false);
            targetDistance = 0;
            // Function: invalid heavy-turret targeting states should visibly return to the configured rest angle.
            returnToDefaultRotation();
            targetShip = null;
            targetPreVelocity.clear();
        }

        this.setAnimData(XROT, xRot0);
        this.setAnimData(YROT, yRot0);
        this.markUpdated();
    }

    //heavy turret only
    public void modifyFireType(int type) {
        Level currentLevel = this.getLevel();
        if (currentLevel == null || currentLevel.isClientSide) { return; }
        if (!isFireTypeSelectable(type)) {
            return;
        }
        // Function: heavy turrets persist the chosen fire mode so control-seat target routing can react immediately.
        getData().fireType = Math.floorMod(type, 3);
    }

    public void modifyChannel(int channel) {
        Level currentLevel = this.getLevel();
        if (currentLevel == null || currentLevel.isClientSide) { return; }

        TurretData data = getData();

        if (channel == 1) {
            if ( data.isChannel1() )  { data.reset(data.CHANNEL_HIDE); }
            else {
                data.reset(data.CHANNEL_HIDE);
                data.set(data.CHANNEL_1);
            }
        }
        if (channel == 2) {
            if ( data.isChannel2() )  { data.reset(data.CHANNEL_HIDE); }
            else {
                data.reset(data.CHANNEL_HIDE);
                data.set(data.CHANNEL_2);
            }
        }
        if (channel == 3) {
            if ( data.isChannel3() )  { data.reset(data.CHANNEL_HIDE); }
            else {
                data.reset(data.CHANNEL_HIDE);
                data.set(data.CHANNEL_3);
            }
        }
        if (channel == 4) {
            if ( data.isChannel4() )  { data.reset(data.CHANNEL_HIDE); }
            else {
                data.reset(data.CHANNEL_HIDE);
                data.set(data.CHANNEL_4);
            }
        }
    }

    public boolean needupdateenemy(){
        return getData().fireType ==1 || getData().fireType==2 && getData().isViewLocked;
    }

    public boolean usesAutomaticTarget(boolean hasSeatedPlayer, boolean isViewLocked) {
        int fireType = getData().fireType;
        // Function: smart mode tracks enemies only while the control seat is view-locked onto an auto target.
        return fireType == 1 || (fireType == 2 && isViewLocked);
    }

    public boolean usesManualTarget(boolean hasSeatedPlayer, boolean isViewLocked) {
        int fireType = getData().fireType;
        // Function: smart mode falls back to the player's manual sight point when the view is unlocked.
        return hasSeatedPlayer && !isViewLocked && (fireType == 0 || fireType == 2);
    }

    public boolean isFireTypeSelectable(int fireType) {
        return fireType >= 0 && fireType <= 2;
    }

    public void updateControlSeatViewLock(boolean isviewlocked) {
        // Function: keep smart-mode target selection based on the control seat's current view-lock state.
        this.getData().isViewLocked = isviewlocked;
    }

    public void channelFromCtrl(int channel) {
        boolean wasFiringChannelMatched = isChannelMatch();
        getData().channelOfCtrl = channel;
        if (!manualFireContinuously() && !currentTargetingAutomatic && isFireCooldownReady() && !wasFiringChannelMatched && isChannelMatch()) {
            // Function: manual heavy turrets fire once per left-click press, then wait for another press after cooldown.
            manualFireQueued = true;
        }
    }

    public void armedChannelFromCtrl(int channel) {
        // Function: armed channels drive heavy turret HUD visibility and automatic-mode fire without requiring left click.
        this.armedChannelOfCtrl = channel;
    }

    public boolean isChannelMatch() {
        TurretData data = getData();
        int channel = data.getChannelStatus();
        return (channel & data.channelOfCtrl) != 0;
    }

    public boolean isArmedChannelMatch() {
        int channel = getData().getChannelStatus();
        return (channel & armedChannelOfCtrl) != 0;
    }

    public int getRemainingCoolDown() {
        return Math.max(0, idleTicks);
    }

    protected boolean isHeavyFireRequested() {
        // Function: cool2 value only recovers while automatic fire is not armed and manual fire is not being held.
        return currentTargetingAutomatic ? isArmedChannelMatch() : isChannelMatch();
    }

    private boolean shouldFireWhenReady() {
        if (currentTargetingAutomatic) {
            return isArmedChannelMatch();
        }
        if (manualFireContinuously()) {
            return isChannelMatch();
        }
        if (!manualFireQueued || !isChannelMatch()) {
            return false;
        }
        manualFireQueued = false;
        return true;
    }

    protected boolean manualFireContinuously() {
        return false;
    }

    private @Nullable Vec3 resolveCurrentFireTarget(boolean firingAlignmentSatisfied) {
        if (currentTargetingAutomatic || firingAlignmentSatisfied) {
            return targetPos;
        }

        Vec3 barrelDirection = getCurrentBarrelDirectionWorld();
        if (barrelDirection == null || barrelDirection.lengthSqr() < 1.0E-6D) {
            return targetPos;
        }

        double requestedDistance = targetPos == null ? 0.0D : currentworldpos.distanceTo(targetPos);
        double fireDistance = Math.max(MANUAL_UNALIGNED_FIRE_FALLBACK_DISTANCE, requestedDistance);
        // Function: use the current barrel vector for unaligned manual shots so pre-aim firing follows visible turret pose.
        return currentworldpos.add(barrelDirection.scale(fireDistance));
    }

    @Override
    protected @Nullable Vec3 getCannonMuzzleWorld(Vec3 target) {
        Vec3 origin = getTurretAimOriginWorld();
        Vec3 direction = target.subtract(origin);
        if (direction.lengthSqr() < 1.0E-6D) {
            return null;
        }

        // Function: heavy turret projectile spawning keeps the established target-line convention.
        return origin.add(direction.normalize().scale(getcannonlength()));
    }

    @Override
    protected double[] computeTargetAimAngles(Vec3 targetWorldPos) {
        double[] aimAngles = super.computeTargetAimAngles(targetWorldPos);
        if (aimAngles == null) {
            return null;
        }
        // Function: heavy turrets use the same pitch as the base turret but keep their flipped yaw convention.
        return new double[]{aimAngles[0], aimAngles[1] + Math.PI};
    }

    public void updatespecificenemy(Vec3 pos) {
        this.targetShip = null;
        this.currentTargetingAutomatic = true;
        this.targetPos = pos;
    }

    public void updatespecificenemy(SubLevel ship) {
        this.currentTargetingAutomatic = true;
        this.targetShip = ship;
        refreshTrackedShipTarget();
    }

    public void clearSpecificEnemy() {
        // Function: clear stale automatic target data when the control seat no longer has an enemy ship.
        this.targetShip = null;
        this.currentTargetingAutomatic = false;
        this.manualFireQueued = false;
        this.targetPos = Vec3.ZERO;
        this.targetDistance = 0;
        this.targetPreVelocity.clear();
    }

    public void updateplayerstatus(boolean hasSeatedPlayer, boolean isviewlocked, Vec3 manualAimTargetPos) {
        this.getData().isViewLocked = isviewlocked;
        this.targetShip = null;
        this.currentTargetingAutomatic = false;
        if (!hasSeatedPlayer || isviewlocked || manualAimTargetPos == null) {
            // Function: invalid manual states clear the target so the heavy turret returns to center.
            this.manualFireQueued = false;
            this.targetPos = Vec3.ZERO;
            this.targetDistance = 0;
            this.targetPreVelocity.clear();
            return;
        }
        this.targetPos = manualAimTargetPos;
    }

    private void refreshTrackedShipTarget() {
        if (targetShip == null) {
            return;
        }
        if (!isValidTargetShip(targetShip)) {
            clearSpecificEnemy();
            return;
        }

        // Function: automatic heavy turrets must aim at the enemy ship's current world position, not the last scanned Vec3 snapshot.
        Vec3 updatedTargetPos = getShipAimPoint(targetShip);
        if (updatedTargetPos != null) {
            this.targetPos = updatedTargetPos;
        }
    }

    private boolean hasHeavyTargetPos() {
        return targetPos != null && targetPos.lengthSqr() > 1.0E-6D;
    }

    private void updateTargetRot() {
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        Vec3 localUp  = Vec3.atLowerCornerOf(facing.getOpposite().getNormal());
        Vec3 localForward = switch (facing) {
            case NORTH -> new Vec3(0, 1, 0);
            case SOUTH -> new Vec3(0, -1, 0);
            case WEST,EAST,UP,DOWN -> new Vec3(0,0,-1);
        };

        Vec3 localRight  = switch (facing) {
            case NORTH, DOWN, SOUTH -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(0, -1, 0);
            case EAST -> new Vec3(0, 1, 0);
            case UP -> new Vec3(-1,0,0);
        };

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(this.getLevel(), this.getBlockPos());
        if(subLevel != null) {
            worldXDirection = subLevel.logicalPose().transformNormal(new Vector3d(localForward.x,localForward.y,localForward.z)).normalize();
            worldYDirection = subLevel.logicalPose().transformNormal(new Vector3d(localUp.x,localUp.y,localUp.z)).normalize();
            worldZDirection = subLevel.logicalPose().transformNormal(new Vector3d(localRight.x,localRight.y,localRight.z)).normalize();
        }
        else {
            worldXDirection = new Vector3d(localForward.x,localForward.y,localForward.z);
            worldYDirection = new Vector3d(localUp.x,localUp.y,localUp.z);
            worldZDirection = new Vector3d(localRight.x,localRight.y,localRight.z);

        }

        Vec3 toTargetWorld = new Vec3(
                targetPos.x - currentworldpos.x,
                targetPos.y - currentworldpos.y,
                targetPos.z - currentworldpos.z
        ).normalize();

        if (toTargetWorld.lengthSqr() < 1e-6) return;

        double localX = toTargetWorld.dot(new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z));     // 鏈湴鍙?
        double localY = toTargetWorld.dot(new Vec3(worldYDirection.x, worldYDirection.y, worldYDirection.z));        // 鏈湴鍚戜笂
        double localZ = toTargetWorld.dot(new Vec3(worldXDirection.x, worldXDirection.y, worldXDirection.z));   // 鏈湴鍚戝墠

        double yaw   = Math.atan2(localX, localZ);           // 娉ㄦ剰atan2椤哄簭
        double pitch = Math.atan2(localY, Math.sqrt(localX * localX + localZ * localZ));

        // Function: the heavy turret's server-side forward axis is opposite this local basis, so yaw must be flipped before aiming and spawning bullets.
        this.targetyrot = (float) (-yaw + Math.PI);

        this.targetxrot = (float) pitch;
        //LogUtils.getLogger().warn("X:"+worldXDirection+"Y:"+worldYDirection+"Z:"+worldZDirection+"target:"+targetPos+"turret:"+currentworldpos +"yaw:"+yaw+"pitch:"+pitch);
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
        tag.putInt("firetype", getData().fireType);
        tag.putDouble("distance", this.getTargetDistance());
        tag.putInt("playerxrot",this.getData().playerAngleX);
        tag.putInt("playeryrot",this.getData().playerAngleY);
        tag.putFloat("xrot",this.targetxrot);
        tag.putFloat("yrot",this.targetyrot);
        tag.putDouble("targetX", targetPos.x);
        tag.putDouble("targetY", targetPos.y);
        tag.putDouble("targetZ", targetPos.z);
        tag.putInt("configregister",this.getData().configRegister);
        tag.putInt("channelofctrl", getData().channelOfCtrl);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        if (tag.contains("firetype")) {this.getData().fireType = tag.getInt("firetype");}
        if (tag.contains("distance")) {this.targetDistance = tag.getDouble("distance");}
        if(tag.contains("playerxrot")) {this.getData().playerAngleX = tag.getInt("playerxrot");}
        if(tag.contains("playeryrot")) {this.getData().playerAngleY = tag.getInt("playeryrot");}
        if (tag.contains("xrot")) {this.targetxrot = tag.getFloat("xrot");}
        if (tag.contains("yrot")) {this.targetyrot = tag.getFloat("yrot");}
        if (tag.contains("targetX")) targetPos = new Vec3(
                tag.getDouble("targetX"),
                tag.getDouble("targetY"),
                tag.getDouble("targetZ")
        );
        if (tag.contains("configregister")) { this.getData().configRegister = (byte)tag.getInt("configregister"); }
        if (tag.contains("channelofctrl")) { this.getData().channelOfCtrl = tag.getInt("channelofctrl"); }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Heavy Turret Screen");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new HeavyTurretContainerMenu(containerId, inv, this);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
