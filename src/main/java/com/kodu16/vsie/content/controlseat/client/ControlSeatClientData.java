package com.kodu16.vsie.content.controlseat.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;

import net.minecraft.client.DeltaTracker;
import org.joml.Quaterniond;
import org.joml.Vector3d;



public class ControlSeatClientData {
    //clientdata不是每个方块绑定一个！它是每个玩家绑定一个，当玩家坐上椅子时，会与椅子互通数据
    public volatile long lastKeyPressTime = 0;
    public volatile boolean viewLock = false;
    public volatile UUID userUUID = null;
    public volatile double accumulatedmousex=0;
    public volatile double accumulatedmousey=0;
    public volatile double lastmousex=0;
    public volatile double lastmousey=0;
    public volatile boolean mouseAnchorSet = false;
    public volatile int throttle;
    public volatile Quaterniond shiprot = new Quaterniond();
    public volatile Vector3d shipfacing = new Vector3d(0,0,0);
    public volatile Vector3d shipUp = new Vector3d(0,0,0);
    public volatile Vector3d prevShipfacing = new Vector3d(0,0,0);
    public volatile Vector3d prevShipUp = new Vector3d(0,0,0);
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

    public volatile boolean shieldon = false;
    public volatile int shieldavalible = 0;
    public volatile int shieldtotal = 1;

    public volatile float smoothEnergyRatio = 0f;
    public volatile float smoothFuelRatio = 0f;
    public volatile float smoothShieldRatio = 0f;
    public volatile float smoothThrottle = 0f;
    // 功能：缓存油门条的“目标值”，让 HUD 在慢包场景下按帧平滑追踪而不是直接跳变。
    public volatile float throttleTargetRatio = 0f;

    public volatile boolean isflightassiston = false;
    public volatile boolean isantigravityon = false;
    public volatile boolean isWarpPreparing = false;
    public volatile boolean hasPendingWarpTeleport = false;
    public volatile String warpTargetName = "";

    // 功能：保存服务端下发的“当前激活频道可响应武器”HUD 数据（名称+冷却），供 HUD 每行绘制。
    public volatile List<ActiveWeaponHudInfo> activeWeaponHudInfos = new ArrayList<>();
    // 功能：缓存每一行武器冷却进度条的平滑值，避免网络慢包导致进度条卡顿跳帧。
    public volatile List<Float> smoothWeaponCooldownRatios = new ArrayList<>();

    public long getLastKeyPressTime() {
        return lastKeyPressTime;
    }

    public UUID getUserUUID() {
        return userUUID;
    }

    public void setUserUUID(UUID userUUID) {
        this.userUUID = userUUID;
    }

    public void setLastMousex(double x) { lastmousex = x; }
    public void setLastMousey(double x) { lastmousey = x; }
    public double getLastMousex() { return lastmousex; }
    public double getLastMousey() { return lastmousey; }
    public void setMouseAnchorSet(boolean value) { mouseAnchorSet = value; }
    public boolean isMouseAnchorSet() { return mouseAnchorSet; }
    public void setAccumulatedx(double x) {accumulatedmousex=x;}
    public void setAccumulatedy(double x) {accumulatedmousey=x;}
    public double getAccumulatedMousex() {return accumulatedmousex;}
    public double getAccumulatedMousey() {return accumulatedmousey;}

    public void clearUserUUID() { userUUID=null; }

    public void updatelastKeyPressTime() {lastKeyPressTime = System.currentTimeMillis();}

    // 切换视角锁定状态
    public void toggleViewLock() {viewLock = !viewLock;}
    public void disableViewLock() {viewLock = false;}
    public boolean isViewLocked() {return viewLock;}


    public void setShipFacing(Vector3d v) { shipfacing = v; }
    public Vector3d getShipFacing() { return shipfacing; }

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
        accumulatedmousex=0;
        accumulatedmousey=0;
        lastmousex=0;
        lastmousey=0;
        mouseAnchorSet=false;
        mouseLpress=false;
    }

}
