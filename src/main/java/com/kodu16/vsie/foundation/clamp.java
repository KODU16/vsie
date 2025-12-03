package com.kodu16.vsie.foundation;

public class clamp {
    public static float clampf(float a, float b, float c) {
        return Math.max(a, Math.min(b, c));
    }
    public static double clampd(double a, double b, double c) {
        return Math.max(a, Math.min(b, c));
    }
}
