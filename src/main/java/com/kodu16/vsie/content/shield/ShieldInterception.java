package com.kodu16.vsie.content.shield;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ShieldInterception {
    private static final double MIN_PROJECTILE_SPEED = 0.25D;
    private static final double MIN_HIT_RADIUS_SQR = 0.25D;
    private static final double MIN_INWARD_DOT = -0.01D;
    private static final double MIN_CURRENT_INWARD_DOT = -0.3D;
    private static final double MIN_SEGMENT_SQR = 1.0E-10D;
    private static final double MIN_NORMAL_SQR = 1.0E-6D;

    private ShieldInterception() {
    }

    public record Hit(Vec3 point, Vec3 normal) {
    }

    public static AABB searchBox(Vec3 center, double radius) {
        double extraRange = Math.max(32.0D, radius * 2.0D);
        return new AABB(center.x, center.y, center.z, center.x, center.y, center.z)
                .inflate(radius + extraRange);
    }

    public static boolean isCandidate(Entity entity) {
        return !entity.isRemoved()
                && !(entity instanceof LivingEntity)
                && entity.getDeltaMovement().length() >= MIN_PROJECTILE_SPEED;
    }

    public static Hit findHit(Entity entity, Vec3 center, double radius) {
        if (!isCandidate(entity) || radius <= 0.0D) {
            return null;
        }

        Vec3 movement = entity.getDeltaMovement();
        Vec3 end = entity.position();
        Vec3 start = end.subtract(movement);
        Vec3 segment = end.subtract(start);
        double segmentSqr = segment.lengthSqr();
        if (segmentSqr < MIN_SEGMENT_SQR) {
            return null;
        }

        Hit sweepHit = findSegmentSphereEntry(start, segment, segmentSqr, center, radius);
        if (sweepHit != null) {
            return sweepHit;
        }

        return findCurrentPositionFallback(end, movement, center, radius);
    }

    private static Hit findSegmentSphereEntry(Vec3 start, Vec3 segment, double segmentSqr, Vec3 center, double radius) {
        Vec3 startToCenter = start.subtract(center);
        double b = 2.0D * startToCenter.dot(segment);
        double c = startToCenter.lengthSqr() - radius * radius;
        double discriminant = b * b - 4.0D * segmentSqr * c;
        if (discriminant < 0.0D) {
            return null;
        }

        double sqrtDiscriminant = Math.sqrt(discriminant);
        Hit firstHit = hitAtSegmentT(start, segment, center, radius, (-b - sqrtDiscriminant) / (2.0D * segmentSqr));
        if (firstHit != null) {
            return firstHit;
        }
        return hitAtSegmentT(start, segment, center, radius, (-b + sqrtDiscriminant) / (2.0D * segmentSqr));
    }

    private static Hit hitAtSegmentT(Vec3 start, Vec3 segment, Vec3 center, double radius, double t) {
        if (t < 0.0D || t > 1.0D) {
            return null;
        }

        Vec3 hitPoint = start.add(segment.scale(t));
        Vec3 normalSource = hitPoint.subtract(center);
        if (normalSource.lengthSqr() < MIN_HIT_RADIUS_SQR) {
            return null;
        }

        Vec3 normal = normalSource.normalize();
        // Function: only the entry side of the sphere blocks incoming projectiles.
        if (segment.normalize().dot(normal) >= MIN_INWARD_DOT) {
            return null;
        }
        return new Hit(center.add(normal.scale(radius)), normal);
    }

    private static Hit findCurrentPositionFallback(Vec3 end, Vec3 movement, Vec3 center, double radius) {
        Vec3 toEntity = end.subtract(center);
        double distSq = toEntity.lengthSqr();
        if (distSq > radius * radius || distSq < MIN_HIT_RADIUS_SQR) {
            return null;
        }

        Vec3 normal = toEntity.normalize();
        if (movement.normalize().dot(normal) >= MIN_CURRENT_INWARD_DOT) {
            return null;
        }
        return new Hit(center.add(normal.scale(radius)), normal);
    }
}
