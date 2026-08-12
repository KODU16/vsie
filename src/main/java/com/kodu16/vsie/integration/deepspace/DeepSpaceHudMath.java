package com.kodu16.vsie.integration.deepspace;

import java.util.List;

/** Dependency-free ray/AABB calculations shared by Minecraft rendering and the headless feedback loop. */
final class DeepSpaceHudMath {
    private static final double EPSILON = 1.0E-12D;

    private DeepSpaceHudMath() {
    }

    static <T> T findAimed(List<Candidate<T>> candidates, Point origin, Point direction, double aimDistance) {
        T nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Candidate<T> candidate : candidates) {
            if (candidate.hyperRelay()) {
                continue;
            }
            double distance = rayDistance(origin, direction, candidate.bounds(), aimDistance);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate.value();
            }
        }
        return nearest;
    }

    static double distanceToBounds(Point point, Bounds bounds) {
        double x = clamp(point.x(), bounds.minX(), bounds.maxX());
        double y = clamp(point.y(), bounds.minY(), bounds.maxY());
        double z = clamp(point.z(), bounds.minZ(), bounds.maxZ());
        return Math.sqrt(square(point.x() - x) + square(point.y() - y) + square(point.z() - z));
    }

    private static double rayDistance(Point origin, Point direction, Bounds bounds, double limit) {
        double length = Math.sqrt(square(direction.x()) + square(direction.y()) + square(direction.z()));
        if (length < EPSILON) {
            return Double.POSITIVE_INFINITY;
        }
        Point normalized = new Point(direction.x() / length, direction.y() / length, direction.z() / length);
        double[] interval = {0.0D, limit};
        if (!clipAxis(origin.x(), normalized.x(), bounds.minX(), bounds.maxX(), interval)
                || !clipAxis(origin.y(), normalized.y(), bounds.minY(), bounds.maxY(), interval)
                || !clipAxis(origin.z(), normalized.z(), bounds.minZ(), bounds.maxZ(), interval)) {
            return Double.POSITIVE_INFINITY;
        }
        return interval[0];
    }

    private static boolean clipAxis(double origin, double direction, double min, double max, double[] interval) {
        if (Math.abs(direction) < EPSILON) {
            return origin >= min && origin <= max;
        }
        double first = (min - origin) / direction;
        double second = (max - origin) / direction;
        if (first > second) {
            double swap = first;
            first = second;
            second = swap;
        }
        interval[0] = Math.max(interval[0], first);
        interval[1] = Math.min(interval[1], second);
        return interval[0] <= interval[1];
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private static double square(double value) {
        return value * value;
    }

    record Point(double x, double y, double z) {
    }

    record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }

    record Candidate<T>(T value, Bounds bounds, boolean hyperRelay) {
    }
}
