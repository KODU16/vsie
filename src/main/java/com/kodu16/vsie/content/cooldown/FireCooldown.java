package com.kodu16.vsie.content.cooldown;

public record FireCooldown(Type type, int intervalTicks, int maxValue, double recoveryPerTick) {
    public enum Type {
        BASIC,
        VALUE
    }

    public static FireCooldown cool1(int intervalTicks) {
        // Function: intervalTicks is the fixed tick delay after every shot in the legacy cooldown.
        return new FireCooldown(Type.BASIC, Math.max(0, intervalTicks), 0, 0);
    }

    public static FireCooldown cool2(int intervalTicks, int maxValue, double recoveryPerTick) {
        // Function: intervalTicks is shot spacing, maxValue is stored firing charge, recoveryPerTick is idle charge recovery.
        return new FireCooldown(Type.VALUE, Math.max(0, intervalTicks), Math.max(1, maxValue), Math.max(0.0D, recoveryPerTick));
    }

    public boolean usesValue() {
        return type == Type.VALUE;
    }
}
