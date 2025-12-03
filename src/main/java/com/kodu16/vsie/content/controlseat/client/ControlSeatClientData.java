package com.kodu16.vsie.content.controlseat.client;

import java.util.UUID;

import com.kodu16.vsie.foundation.clamp;
import org.joml.Quaterniond;
import org.joml.Vector3d;



public class ControlSeatClientData {
    //又见面了，眼熟不
    //GPT告诉我我可以这么写，不管如何，我都不会再把client和server混成一坨了
    // 静态变量存储视角锁定的状态
    //为什么一定要这么恶心？为了防止客户端不同玩家之间的数据串掉
    //服务端不再通过位置检验操纵有效性，而是比对本地玩家和存储的座位玩家的UUID
    //谁知道这byd读的是本地还是造船厂位置，反正我也不会让两个玩家上一个座
    //这里头还额外存mixin告诉的鼠标的位置

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
