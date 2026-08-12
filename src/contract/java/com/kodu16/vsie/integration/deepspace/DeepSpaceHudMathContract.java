package com.kodu16.vsie.integration.deepspace;

import java.util.List;

/** Headless regression loop for model-bound aiming, relay exclusion, and surface distance. */
public final class DeepSpaceHudMathContract {
    private DeepSpaceHudMathContract() {
    }

    public static void main(String[] args) {
        Object relay = new Object();
        Object near = new Object();
        Object far = new Object();
        Object selected = DeepSpaceHudMath.findAimed(List.of(
                candidate(far, 20, -4, -4, 30, 4, 4, false),
                candidate(relay, 1, -1, -1, 2, 1, 1, true),
                candidate(near, 5, -2, -2, 9, 2, 2, false)
        ), point(0, 0, 0), point(1, 0, 0), 100);
        require(selected == near, "nearest intersected planet model must win and relays must be ignored");
        require(DeepSpaceHudMath.findAimed(List.of(
                candidate(near, 5, -2, -2, 9, 2, 2, false)
        ), point(0, 0, 0), point(0, 1, 0), 100) == null,
                "a ray that misses every model must not show a planet panel");
        require(Math.abs(DeepSpaceHudMath.distanceToBounds(
                point(0, 0, 0), new DeepSpaceHudMath.Bounds(5, -2, -2, 9, 2, 2)
        ) - 5.0D) < 1.0E-9, "distance must be measured to the planet surface rather than its center");
    }

    private static DeepSpaceHudMath.Point point(double x, double y, double z) {
        return new DeepSpaceHudMath.Point(x, y, z);
    }

    private static DeepSpaceHudMath.Candidate<Object> candidate(
            Object value, double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ, boolean relay
    ) {
        return new DeepSpaceHudMath.Candidate<>(value,
                new DeepSpaceHudMath.Bounds(minX, minY, minZ, maxX, maxY, maxZ), relay);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
