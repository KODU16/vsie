package com.kodu16.vsie.content.controlseat.client;

import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.core.BlockPos;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ControlSeatClientData {
    public volatile long lastKeyPressTime = 0;
    public volatile boolean viewLock = false;
    public volatile UUID userUUID = null;
    public volatile double accumulatedmousex = 0;
    public volatile double accumulatedmousey = 0;
    public volatile double lastmousex = 0;
    public volatile double lastmousey = 0;
    public volatile boolean mouseAnchorSet = false;
    public volatile int throttle;
    public volatile Quaterniond shiprot = new Quaterniond();
    public volatile Vector3d shipfacing = new Vector3d(0, 0, 0);
    public volatile Vector3d shipUp = new Vector3d(0, 0, 0);
    public volatile Vector3d prevShipfacing = new Vector3d(0, 0, 0);
    public volatile Vector3d prevShipUp = new Vector3d(0, 0, 0);
    public volatile double shipSpeed = 0.0D;
    public volatile Vector3d structureCenterWorld = new Vector3d();
    public volatile double seatGForce = 0.0D;
    public volatile boolean mouseLpress = false;

    public volatile boolean channel1 = false;
    public volatile boolean channel2 = false;
    public volatile boolean channel3 = false;
    public volatile boolean channel4 = false;

    public Map<String, Object> shipsData = new HashMap<>();
    public volatile String enemy = "";
    public volatile String ally = "";
    public volatile String lockedenemyslug = "";

    public volatile int energyavalible = 0;
    public volatile int energytotal = 100;

    public volatile int fuelavalible = 0;
    public volatile int fueltotal = 100;
    public volatile int e710avalible = 0;
    public volatile int warpE710CostMb = 0;
    public volatile boolean warpE710Insufficient = false;

    public volatile boolean shieldon = false;
    public volatile int shieldavalible = 0;
    public volatile int shieldtotal = 1;
    public volatile boolean isShieldOverloaded = false;

    public volatile float smoothEnergyRatio = 0f;
    public volatile float smoothFuelRatio = 0f;
    public volatile float smoothE710Ratio = 0f;
    public volatile float smoothWarpE710CostRatio = 0f;
    public volatile float smoothShieldRatio = 0f;
    public volatile float smoothThrottle = 0f;
    // Function: cache the server throttle target so the HUD can smooth toward it between packets.
    public volatile float throttleTargetRatio = 0f;

    public volatile boolean isflightassiston = false;
    public volatile boolean isforceassiston = false;
    public volatile boolean istorqueassiston = false;
    public volatile boolean isForceAssistSuppressedByAccelerator = false;
    public volatile boolean isantigravityon = false;
    public volatile boolean isAutoLevelOn = false;
    public volatile boolean isWarpPreparing = false;
    public volatile boolean hasPendingWarpTeleport = false;
    public volatile String warpTargetName = "";

    public volatile List<ActiveWeaponHudInfo> activeWeaponHudInfos = new ArrayList<>();
    public volatile List<Float> smoothWeaponCooldownRatios = new ArrayList<>();
    private final Map<Long, TurretHudMarkerState> turretHudMarkerStates = new HashMap<>();

    public long getLastKeyPressTime() {
        return lastKeyPressTime;
    }

    public UUID getUserUUID() {
        return userUUID;
    }

    public void setUserUUID(UUID userUUID) {
        this.userUUID = userUUID;
    }

    public void setLastMousex(double x) {
        lastmousex = x;
    }

    public void setLastMousey(double x) {
        lastmousey = x;
    }

    public double getLastMousex() {
        return lastmousex;
    }

    public double getLastMousey() {
        return lastmousey;
    }

    public void setMouseAnchorSet(boolean value) {
        mouseAnchorSet = value;
    }

    public boolean isMouseAnchorSet() {
        return mouseAnchorSet;
    }

    public void setAccumulatedx(double x) {
        accumulatedmousex = x;
    }

    public void setAccumulatedy(double x) {
        accumulatedmousey = x;
    }

    public double getAccumulatedMousex() {
        return accumulatedmousex;
    }

    public double getAccumulatedMousey() {
        return accumulatedmousey;
    }

    public void clearUserUUID() {
        userUUID = null;
    }

    public void updatelastKeyPressTime() {
        lastKeyPressTime = System.currentTimeMillis();
    }

    public void toggleViewLock() {
        viewLock = !viewLock;
    }

    public void disableViewLock() {
        viewLock = false;
    }

    public boolean isViewLocked() {
        return viewLock;
    }

    public void setShipFacing(Vector3d v) {
        shipfacing = v;
    }

    public Vector3d getShipFacing() {
        return shipfacing;
    }

    public void updateShipVectors(Vector3d newFacing, Vector3d newUp) {
        prevShipfacing = new Vector3d(shipfacing);
        prevShipUp = new Vector3d(shipUp);
        shipfacing = new Vector3d(newFacing);
        shipUp = new Vector3d(newUp);
    }

    public Vector3d getInterpolatedShipFacing(DeltaTracker partialTick) {
        return new Vector3d(prevShipfacing).lerp(shipfacing, partialTickValue(partialTick));
    }

    public Vector3d getInterpolatedShipUp(DeltaTracker partialTick) {
        return new Vector3d(prevShipUp).lerp(shipUp, partialTickValue(partialTick));
    }

    private static float partialTickValue(DeltaTracker partialTick) {
        if (partialTick == null) {
            return 0f;
        }

        return clamp01(partialTick.getGameTimeDeltaPartialTick(false));
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public void reset() {
        accumulatedmousex = 0;
        accumulatedmousey = 0;
        lastmousex = 0;
        lastmousey = 0;
        mouseAnchorSet = false;
        mouseLpress = false;
        turretHudMarkerStates.clear();
    }

    public TurretHudMarkerState getTurretHudMarkerState(BlockPos turretPos) {
        return turretHudMarkerStates.computeIfAbsent(turretPos.asLong(), ignored -> new TurretHudMarkerState());
    }

    public void retainTurretHudMarkers(List<BlockPos> activeTurretPositions) {
        Map<Long, TurretHudMarkerState> retainedStates = new HashMap<>(activeTurretPositions.size());
        for (BlockPos turretPos : activeTurretPositions) {
            long key = turretPos.asLong();
            TurretHudMarkerState state = turretHudMarkerStates.get(key);
            if (state != null) {
                retainedStates.put(key, state);
            }
        }
        // Function: discard smoothing state for turrets that are no longer active so old HUD markers cannot linger.
        turretHudMarkerStates.clear();
        turretHudMarkerStates.putAll(retainedStates);
    }

    public static class TurretHudMarkerState {
        public float screenX = 0f;
        public float screenY = 0f;
        public boolean initialized = false;
    }
}
