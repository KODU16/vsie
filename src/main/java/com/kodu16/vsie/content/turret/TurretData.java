package com.kodu16.vsie.content.turret;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.ArrayList;
import java.util.List;

// 功能：炮塔状态数据容器，已与 Mekanism 升级系统解耦。
public final class TurretData {

    //turrets only
    public volatile boolean targetsHostile = false;//1:敌对，2:被动，3:玩家，4:船只
    public volatile boolean targetsPassive = false;
    public volatile boolean targetsPlayers = false;
    public volatile boolean targetsShip = false;

    //heavy turrets only
    public volatile int firetype = 0;//0:手动，1:自动，2:智能
    public volatile int playerangleX = 0;//玩家当前朝向
    public volatile int playerangleY = 0;
    public volatile boolean isviewlocked = false;

    public volatile boolean targetsTrusted = false;
    public volatile Vector3d location;
    public volatile double distance;
    public volatile ArrayList<Ship> enemyshipsData = new ArrayList<>();
    public volatile Vec3 directionForward;
    public volatile Vec3 directionUp;
    public volatile Vec3 directionRight;

    public double getDistance() { return distance; }
    public boolean getTargetsHostile() { return targetsHostile; }
    public boolean getTargetsPassive() { return targetsPassive; }
    public boolean getTargetsPlayers() { return targetsPlayers; }
    public boolean getTargetsShip() { return targetsShip; }

    public void setDistance(double distance) { this.distance = distance; }
    public void setTargetsHostile(boolean targetsHostile) {this.targetsHostile = targetsHostile; }
    public void setTargetsPassive(boolean targetsPassive) {this.targetsPassive = targetsPassive; }
    public void setTargetsPlayers(boolean targetsPlayers) {this.targetsPlayers = targetsPlayers; }
    public void setTargetsShip(boolean targetsShip) {this.targetsShip = targetsShip; }
    
}
