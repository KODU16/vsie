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

    // 炮塔配置寄存器 高4位为目标配置 低4位为频道配置
    public volatile int configRegister = TARGET_HOSTILE ;
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

    @Getter @Setter public volatile Matrix3d coordAxis = new Matrix3d();    //我们规定 模型渲染中 不进行旋转的FACING对应此处单位矩阵
    @Getter @Setter public volatile Vector3d basePivotOffset = new Vector3d();   //枢轴点偏移
    @Getter @Setter public volatile Vector3d worldPivotOffset = new Vector3d();   //枢轴点偏移


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
