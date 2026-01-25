package com.kodu16.vsie.content.controlseat.client.Input;

import java.util.UUID;

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
    public volatile int throttle;
    public volatile Quaterniond shiprot = new Quaterniond();
    public volatile Vector3d shipfacing = new Vector3d(0,0,0);
    public volatile boolean mouseLpress = false;
    public volatile boolean channel1 = false;
    public volatile boolean channel2 = false;
    public volatile boolean channel3 = false;
    public volatile boolean channel4 = false;

    public void setLastMousex(double x) { lastmousex = x; }
    public void setLastMousey(double x) { lastmousey = x; }
    public double getLastMousex() { return lastmousex; }
    public double getLastMousey() { return lastmousey; }
    public void setAccumulatedx(double x) {accumulatedmousex=x;}
    public void setAccumulatedy(double x) {accumulatedmousey=x;}
    public double getAccumulatedMousex() {return accumulatedmousex;}
    public double getAccumulatedMousey() {return accumulatedmousey;}

    public void setUserUUID(UUID uuid) { userUUID = uuid; }
    public UUID getUserUUID() { return userUUID; }
    public void clearUserUUID() { userUUID=null; }

    public void updatelastKeyPressTime() {lastKeyPressTime = System.currentTimeMillis();}
    public long getLastKeyPressTime() {return lastKeyPressTime;}

    // 切换视角锁定状态
    public void toggleViewLock() {viewLock = !viewLock;}
    public void disableViewLock() {viewLock = false;}
    public boolean isViewLocked() {return viewLock;}


    public void setShipFacing(Vector3d v) { shipfacing = v; }
    public Vector3d getShipFacing() { return shipfacing; }

    public int getthrottle() {return throttle;}

    public void setthrottle(int t) {throttle = t;}

    public void reset() {
        accumulatedmousex=0;
        accumulatedmousey=0;
        lastmousex=0;
        lastmousey=0;
    }

}
