package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.utility.FxData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Vector3d;

import java.util.ArrayList;

// Function: stores runtime turret state and GUI-configured targeting settings.
public final class TurretData {
    public static final int DEFAULT_AIM_LIMIT_MIN = -180;
    public static final int DEFAULT_AIM_LIMIT_MAX = 180;

    public final int TARGET_HOSTILE = 0b0001_0000;
    public final int TARGET_PASSIVE = 0b0010_0000;
    public final int TARGET_PLAYER = 0b0100_0000;
    public final int TARGET_SHIP = 0b1000_0000;
    public final int TARGET_MANUAL = 0b0000_0000;
    public final int TARGET_HIDE = 0b1111_0000;

    public final int CHANNEL_1 = 0b0000_0001;
    public final int CHANNEL_2 = 0b0000_0010;
    public final int CHANNEL_3 = 0b0000_0100;
    public final int CHANNEL_4 = 0b0000_1000;
    public final int CHANNEL_HIDE = 0b0000_1111;

    // Function: upper 4 bits store target flags and lower 4 bits store channel flags.
    public volatile int configRegister = TARGET_MANUAL;
    public volatile int channelOfCtrl = 0;

    // Function: heavy turret fire mode stays here because the heavy screen already syncs through TurretData.
    public volatile int fireType = 0;
    // Function: turret block-breaking stays enabled by default so existing saves keep current behaviour.
    public volatile boolean breaksBlocks = true;

    public volatile int playerAngleX = 0;
    public volatile int playerAngleY = 0;
    // Function: auto-target limits are stored in degrees; the default span leaves both axes unrestricted.
    public volatile int aimLimitMinX = DEFAULT_AIM_LIMIT_MIN;
    public volatile int aimLimitMaxX = DEFAULT_AIM_LIMIT_MAX;
    public volatile int aimLimitMinY = DEFAULT_AIM_LIMIT_MIN;
    public volatile int aimLimitMaxY = DEFAULT_AIM_LIMIT_MAX;

    public volatile boolean isViewLocked = false;

    public volatile Vector3d location;
    public volatile double distance;
    public volatile ArrayList<SubLevel> enemyShipsData = new ArrayList<>();

    public volatile Matrix3d coordAxis = new Matrix3d();
    public volatile Vector3d basePivotOffset = new Vector3d();
    public volatile Vector3d worldPivotOffset = new Vector3d();

    @Nullable
    public FxData fxData;

    public int getTargetStatus() {
        return configRegister & TARGET_HIDE;
    }

    public int getChannelStatus() {
        return configRegister & CHANNEL_HIDE;
    }

    public synchronized void flip(int bit) {
        configRegister ^= bit;
    }

    public synchronized void set(int bit) {
        configRegister |= bit;
    }

    public synchronized void reset(int bit) {
        configRegister &= ~bit;
    }

    public boolean isTargetsHostile() {
        return (getTargetStatus() & TARGET_HOSTILE) != 0;
    }

    public boolean isTargetsPassive() {
        return (getTargetStatus() & TARGET_PASSIVE) != 0;
    }

    public boolean isTargetsPlayers() {
        return (getTargetStatus() & TARGET_PLAYER) != 0;
    }

    public boolean isTargetsShip() {
        return (getTargetStatus() & TARGET_SHIP) != 0;
    }

    public boolean isChannel1() {
        return (getChannelStatus() & CHANNEL_1) != 0;
    }

    public boolean isChannel2() {
        return (getChannelStatus() & CHANNEL_2) != 0;
    }

    public boolean isChannel3() {
        return (getChannelStatus() & CHANNEL_3) != 0;
    }

    public boolean isChannel4() {
        return (getChannelStatus() & CHANNEL_4) != 0;
    }

    public boolean isBreaksBlocks() {
        return breaksBlocks;
    }

    public void setBreaksBlocks(boolean breaksBlocks) {
        this.breaksBlocks = breaksBlocks;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public Matrix3d getCoordAxis() {
        return coordAxis;
    }

    public void setCoordAxis(Matrix3d coordAxis) {
        this.coordAxis = coordAxis;
    }

    public Vector3d getBasePivotOffset() {
        return basePivotOffset;
    }

    public void setBasePivotOffset(Vector3d basePivotOffset) {
        this.basePivotOffset = basePivotOffset;
    }

    public Vector3d getWorldPivotOffset() {
        return worldPivotOffset;
    }

    public void setWorldPivotOffset(Vector3d worldPivotOffset) {
        this.worldPivotOffset = worldPivotOffset;
    }

    public void setAimLimits(int minX, int maxX, int minY, int maxY) {
        this.aimLimitMinX = minX;
        this.aimLimitMaxX = maxX;
        this.aimLimitMinY = minY;
        this.aimLimitMaxY = maxY;
    }
}
