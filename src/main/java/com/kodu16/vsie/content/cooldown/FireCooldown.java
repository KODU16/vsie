package com.kodu16.vsie.content.cooldown;

public record FireCooldown(Type type, int intervalTicks, int maxValue, int recoveryPerTick) {
    public enum Type {
        BASIC,
        VALUE
    }

    public static FireCooldown cool1(int intervalTicks) {
        // Function: cool1 keeps the legacy one-shot-then-wait cooldown behavior.
        return new FireCooldown(Type.BASIC, Math.max(0, intervalTicks), 0, 0);
    }

    public static FireCooldown cool2(int intervalTicks, int maxValue, int recoveryPerTick) {
        // Function: cool2 adds a finite firing value that recovers only while fire is not requested.
        return new FireCooldown(Type.VALUE, Math.max(0, intervalTicks), Math.max(1, maxValue), Math.max(1, recoveryPerTick));
    }

    public boolean usesValue() {
        return type == Type.VALUE;
    }
}
