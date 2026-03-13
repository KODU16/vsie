package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.utility.FxData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.ArrayList;
import java.util.List;

// 功能：炮塔状态数据容器，已与 Mekanism 升级系统解耦。
public final class TurretData {

    //turrets only
    @Setter
    public volatile boolean targetsHostile = false;//1:敌对，2:被动，3:玩家，4:船只
    @Setter
    public volatile boolean targetsPassive = false;
    @Setter
    public volatile boolean targetsPlayers = false;
    @Setter
    public volatile boolean targetsShip = false;

    //heavy turrets only
    public volatile int firetype = 0;//0:手动，1:自动，2:智能
    public volatile int playerangleX = 0;//玩家当前朝向
    public volatile int playerangleY = 0;
    public volatile boolean isviewlocked = false;

    public volatile boolean targetsTrusted = false;
    // 功能：为重型炮塔提供与主武器一致的 4 路频道开关。
    public volatile boolean channel1 = true;
    public volatile boolean channel2 = false;
    public volatile boolean channel3 = false;
    public volatile boolean channel4 = false;
    // 功能：缓存控制椅当前下发给重型炮塔的频道编码。
    public volatile int receivingchannel = 0;
    public volatile Vector3d location;
    @Getter
    @Setter
    public volatile double distance;
    public volatile ArrayList<Ship> enemyshipsData = new ArrayList<>();
    public volatile Vec3 directionForward;
    public volatile Vec3 directionUp;
    public volatile Vec3 directionRight;

    @Nullable
    public FxData fxData;

    public boolean getTargetsHostile() { return targetsHostile; }
    public boolean getTargetsPassive() { return targetsPassive; }
    public boolean getTargetsPlayers() { return targetsPlayers; }
    public boolean getTargetsShip() { return targetsShip; }
    public boolean getChannel1() { return channel1; }
    public boolean getChannel2() { return channel2; }
    public boolean getChannel3() { return channel3; }
    public boolean getChannel4() { return channel4; }

}
