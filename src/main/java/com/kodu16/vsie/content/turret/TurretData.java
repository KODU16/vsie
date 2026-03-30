package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.utility.FxData;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Matrix3d;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.ArrayList;

// 功能：炮塔状态数据容器，已与 Mekanism 升级系统解耦。
public final class TurretData {
    public final int TARGET_HOSTILE = 0b0001_0000;
    public final int TARGET_PASSIVE = 0b0010_0000;
    public final int TARGET_PLAYER  = 0b0100_0000;
    public final int TARGET_SHIP    = 0b1000_0000;
    public final int TARGET_MANUAL  = 0b0000_0000;
    public final int TARGET_HIDE    = 0b1111_0000;

    public final int CHANNEL_1 = 0b0000_0001;
    public final int CHANNEL_2 = 0b0000_0010;
    public final int CHANNEL_3 = 0b0000_0100;
    public final int CHANNEL_4 = 0b0000_1000;
    public final int CHANNEL_HIDE = 0b0000_1111;

    // 功能：炮塔配置寄存器（高4位目标、低4位频道）默认全部关闭，避免新放置炮塔自动激活索敌。
    public volatile int configRegister = TARGET_MANUAL;
    // 缓存控制椅当前下发给重型炮塔的频道编码
    public volatile int channelOfCtrl = 0;

    //heavy turrets only
    public volatile int fireType = 0;//0:手动，1:自动，2:智能

    public volatile int playerAngleX = 0;//玩家当前朝向
    public volatile int playerAngleY = 0;

    public volatile boolean isViewLocked = false;

    public volatile Vector3d location;

    @Getter @Setter public volatile double distance;
    public volatile ArrayList<Ship> enemyShipsData = new ArrayList<>();

    @Nullable public FxData fxData;

    public int getTargetStatus()    { return (configRegister&TARGET_HIDE); }
    public int getChannelStatus()   { return (configRegister&CHANNEL_HIDE); }

    public synchronized void flip  (int bit) { configRegister^=bit; }
    public synchronized void set   (int bit) { configRegister|=bit; }
    public synchronized void reset (int bit) { configRegister&=(~bit); }

    public boolean isTargetsHostile() { return (getTargetStatus()&TARGET_HOSTILE)!=0; }
    public boolean isTargetsPassive() { return (getTargetStatus()&TARGET_PASSIVE)!=0; }
    public boolean isTargetsPlayers() { return (getTargetStatus()&TARGET_PLAYER)!=0; }
    public boolean isTargetsShip()    { return (getTargetStatus()&TARGET_SHIP)!=0; }

    public boolean isChannel1() { return (getChannelStatus()&CHANNEL_1)!=0; }
    public boolean isChannel2() { return (getChannelStatus()&CHANNEL_2)!=0; }
    public boolean isChannel3() { return (getChannelStatus()&CHANNEL_3)!=0; }
    public boolean isChannel4() { return (getChannelStatus()&CHANNEL_4)!=0; }
}
