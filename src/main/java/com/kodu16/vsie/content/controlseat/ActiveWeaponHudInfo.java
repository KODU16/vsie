package com.kodu16.vsie.content.controlseat;

public class ActiveWeaponHudInfo {
    public final String displayName;
    public final int currentTick;
    public final int maxCooldown;
    public final boolean remainingCooldown;

    public ActiveWeaponHudInfo(String displayName, int currentTick, int maxCooldown) {
        this(displayName, currentTick, maxCooldown, false);
    }

    public ActiveWeaponHudInfo(String displayName, int currentTick, int maxCooldown, boolean remainingCooldown) {
        // Function: remainingCooldown marks values that count down after firing, such as heavy turret idleTicks.
        this.displayName = displayName;
        this.currentTick = currentTick;
        this.maxCooldown = maxCooldown;
        this.remainingCooldown = remainingCooldown;
    }
}
