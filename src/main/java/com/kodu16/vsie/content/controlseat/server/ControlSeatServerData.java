package com.kodu16.vsie.content.controlseat.server;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;



public class ControlSeatServerData {
    public volatile List<BlockPos> thrusterpositionslist = new ArrayList<>();
    public volatile Vec3 force =  new Vec3(0,0,0);
    public volatile Vec3 torque = new Vec3(0,0,0);
    public volatile int throttle = 0;

    public volatile Player player = null;
    //Direction in ship space. Expected to be normalized
    private volatile Vec3i directionForward;
    private volatile Vec3i directionUp;
    private volatile Vec3i directionRight;

    public volatile boolean channel1 = true;
    public volatile boolean channel2 = true;
    public volatile boolean channel3 = true;
    public volatile boolean channel4 = true;
    public volatile int channelencode = 0b1111;
    public volatile boolean isfiring = false;

    public volatile String enemy = "";
    public volatile String ally = "";
    public volatile int lockedenemyindex = 0;
    public volatile String lockedenemyslug = "";

    // Function: runtime cache for the currently locked enemy sublevel used by linked weapons.
    public volatile SubLevel lockedEnemySubLevel = null;
    public volatile Map<String, Object> shipsData = new HashMap<>();
    public volatile ArrayList<Vec3> enemyshipsData = new ArrayList<>();

    public volatile float thruster_strength = 0;
    public volatile float thruster_force_strength = 0;
    public volatile float thruster_torque_strength = 0;

    public volatile float[] facingMaxThrustSum = new float[6];

    public volatile int totalenergystorage = 100;
    public volatile int avalibleenergy = 0;

    public volatile int totalfuelstorage = 100;
    public volatile int avaliblefuel = 0;
    public volatile int avalibleE710 = 0;
    public volatile int warpE710CostMb = 0;
    public volatile boolean warpE710Insufficient = false;

    public volatile double totalshield = 1;
    public volatile double avalibleshield = 0;
    public volatile double shieldradius = 0;
    public volatile double shieldcostperprojectile = 0;
    public volatile double shieldregeneratepertick = 0;
    public volatile double shieldmaxcooldowntime = 0;
    public volatile double shieldcooldowntime = 0;
    public volatile boolean isshieldon = false;
    public volatile boolean isflightassiston = true;
    public volatile boolean isantigravityon = true;
    // Function: keeps the seat upright relative to world Y even when no player is riding.
    public volatile boolean isforceassiston = true;// Function: toggles automatic counter-force damping.
    public volatile boolean istorqueassiston = true;// Function: toggles automatic counter-torque damping.
    public volatile boolean isForceAssistSuppressedByAccelerator = false;// Function: rail acceleration hard-disables force assist while active.
    public volatile boolean isAutoLevelOn = false;
    public volatile double antiGravityIdleThrottle = 1.0D;
    public volatile double shieldmin = 0;
    public volatile double shieldmax = 0;


    public volatile List<ActiveWeaponHudInfo> activeWeaponHudInfos = new ArrayList<>();


    public volatile BlockPos warpTargetPos = BlockPos.ZERO;

    public volatile String warpTargetDimension = "";
    public volatile String warpTargetName = "";

    public volatile boolean isWarpPreparing = false;
    // Function: screen-space warp alignment control mirrors the mouse control line while the server owns aiming.
    public volatile double warpAlignmentControlX = 0.0D;
    public volatile double warpAlignmentControlY = 0.0D;

    public volatile boolean hasWarpStartSnapshot = false;
    public volatile Vector3d warpStartSubLevelWorldPos = new Vector3d();
    public volatile Vector3d warpLaunchDirection = new Vector3d();

    public volatile boolean hasPendingWarpTeleport = false;

    public volatile Vector3d pendingWarpTeleportPos = new Vector3d();

    public volatile long pendingWarpTeleportGameTime = -1L;

    public volatile boolean isviewlocked = false;

    public volatile double manualAimTargetX = 0;
    public volatile double manualAimTargetY = 0;
    public volatile double manualAimTargetZ = 0;


    public volatile BlockPos controlSeatPos = BlockPos.ZERO;

    public Level level;
    public ServerSubLevel serverShip;
    public volatile Vector3d finaltorque = new Vector3d(0,0,0);
    public volatile Vector3d finalforce = new Vector3d(0,0,0);
    public volatile Vector3d thrusterVisualForce = new Vector3d(0,0,0);
    public volatile double shipSpeed = 0.0D;
    public volatile Vector3d structureCenterWorld = new Vector3d();
    public volatile Vector3d structureVelocityWorld = new Vector3d();
    public volatile double seatGForce = 0.0D;

    public Vec3 getForce() {
        return force;
    }

    public void setForce(Vec3 force) {
        this.force = force;
    }

    public void setForce(Vector3d force) {
        this.force = new Vec3(force.x, force.y, force.z);
    }

    public Vec3 getTorque() {
        return torque;
    }

    public void setTorque(Vec3 torque) {
        this.torque = torque;
    }

    public void setTorque(Vector3d torque) {
        this.torque = new Vec3(torque.x, torque.y, torque.z);
    }

    public int getThrottle() {
        return throttle;
    }

    public void setThrottle(int throttle) {
        // Function: keep every caller inside the control seat's advertised full reverse/full forward range.
        this.throttle = Math.max(-100, Math.min(throttle, 100));
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Vec3i getDirectionForward() {
        return directionForward;
    }

    public void setDirectionForward(Vec3i directionForward) {
        this.directionForward = directionForward;
    }

    public Vec3i getDirectionUp() {
        return directionUp;
    }

    public void setDirectionUp(Vec3i directionUp) {
        this.directionUp = directionUp;
    }

    public Vec3i getDirectionRight() {
        return directionRight;
    }

    public void setDirectionRight(Vec3i directionRight) {
        this.directionRight = directionRight;
    }

    public Vector3d getFinaltorque() {
        return finaltorque;
    }

    public void setFinaltorque(Vector3d finaltorque) {
        this.finaltorque = finaltorque;
    }

    public Vector3d getFinalforce() {
        return finalforce;
    }

    public void setFinalforce(Vector3d finalforce) {
        this.finalforce = finalforce;
    }

    public Vector3d getThrusterVisualForce() {
        return thrusterVisualForce;
    }

    public void setThrusterVisualForce(Vector3d thrusterVisualForce) {
        // Function: visual thruster selection can differ from the physics impulse sign for seat throttle.
        this.thrusterVisualForce = thrusterVisualForce;
    }

    public boolean getChannel1() {return channel1;}
    public boolean getChannel2() {return channel2;}
    public boolean getChannel3() {return channel3;}
    public boolean getChannel4() {return channel4;}

    public void setWeaponChannelEncode(int channelencode) {
        this.channelencode = channelencode & 0b1111;
        this.channel1 = (this.channelencode & (1 << 0)) != 0;
        this.channel2 = (this.channelencode & (1 << 1)) != 0;
        this.channel3 = (this.channelencode & (1 << 2)) != 0;
        this.channel4 = (this.channelencode & (1 << 3)) != 0;
    }

    public void refreshWeaponChannelEncode() {
        // Function: persist the four independent weapon channel toggles as one compact bit mask.
        setWeaponChannelEncode(
                (channel1 ? (1 << 0) : 0)
                        | (channel2 ? (1 << 1) : 0)
                        | (channel3 ? (1 << 2) : 0)
                        | (channel4 ? (1 << 3) : 0)
        );
    }


    public void clearSeatOccupantState() {
        this.torque = new Vec3(0,0,0);
        this.force = new Vec3(0,0,0);
        this.throttle = 0;
        this.player = null;
        this.isfiring = false;
        this.isForceAssistSuppressedByAccelerator = false;
        clearWarpPreparation();
    }

    public void reset() {
        clearSeatOccupantState();
    }


    public void startWarpPreparation() {
        this.isWarpPreparing = true;
        clearWarpAlignmentControl();
        this.warpE710Insufficient = false;
        clearWarpAlignmentSnapshot();
        clearPendingWarpTeleport();
        this.torque = new Vec3(0, 0, 0);
        this.throttle = 0;
    }

    public void rejectWarpForInsufficientE710(int requiredMb) {
        this.isWarpPreparing = false;
        this.hasPendingWarpTeleport = false;
        clearWarpAlignmentControl();
        this.warpE710CostMb = Math.max(0, requiredMb);
        this.warpE710Insufficient = true;
        this.torque = new Vec3(0, 0, 0);
        this.throttle = 0;
    }

    public void clearWarpPreparation() {
        this.isWarpPreparing = false;
        clearWarpAlignmentControl();
        this.warpTargetPos = BlockPos.ZERO;
        this.warpTargetDimension = "";
        this.warpTargetName = "";
        this.warpE710CostMb = 0;
        this.warpE710Insufficient = false;
        clearWarpAlignmentSnapshot();
        this.torque = new Vec3(0, 0, 0);
        this.throttle = 0;
    }

    public void transitionWarpPreparationToPendingTeleport() {
        // Function: keep the current jump's E-710 cost visible on the HUD until the pending teleport actually finishes.
        this.isWarpPreparing = false;
        clearWarpAlignmentControl();
        this.warpTargetPos = BlockPos.ZERO;
        this.warpTargetDimension = "";
        this.warpTargetName = "";
        this.warpE710Insufficient = false;
        clearWarpAlignmentSnapshot();
        this.torque = new Vec3(0, 0, 0);
        this.throttle = 0;
    }


    public void schedulePendingWarpTeleport(Vector3d destination, long executeGameTime) {
        this.hasPendingWarpTeleport = true;
        this.pendingWarpTeleportPos = new Vector3d(destination);
        this.pendingWarpTeleportGameTime = executeGameTime;
    }


    public void clearPendingWarpTeleport() {
        this.hasPendingWarpTeleport = false;
        this.pendingWarpTeleportPos = new Vector3d();
        this.pendingWarpTeleportGameTime = -1L;
        this.warpE710CostMb = 0;
        this.warpE710Insufficient = false;
    }

    public void clearWarpAlignmentSnapshot() {
        // Function: warp aim direction must be captured once at preparation start, not recomputed while the ship rotates.
        this.hasWarpStartSnapshot = false;
        this.warpStartSubLevelWorldPos = new Vector3d();
        this.warpLaunchDirection = new Vector3d();
    }

    public void clearWarpAlignmentControl() {
        this.warpAlignmentControlX = 0.0D;
        this.warpAlignmentControlY = 0.0D;
    }

    public boolean setWarpAlignmentSnapshot(Vec3 startWorldPos, Vec3 targetWorldPos) {
        Vec3 direction = targetWorldPos.subtract(startWorldPos);
        if (direction.lengthSqr() < 1.0E-6D) {
            clearWarpAlignmentSnapshot();
            return false;
        }
        this.hasWarpStartSnapshot = true;
        this.warpStartSubLevelWorldPos = new Vector3d(startWorldPos.x, startWorldPos.y, startWorldPos.z);
        Vec3 normalizedDirection = direction.normalize();
        this.warpLaunchDirection = new Vector3d(normalizedDirection.x, normalizedDirection.y, normalizedDirection.z);
        return true;
    }
}
