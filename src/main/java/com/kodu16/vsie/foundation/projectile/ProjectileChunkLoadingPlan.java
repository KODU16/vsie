package com.kodu16.vsie.foundation.projectile;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Builds a diagonal-safe chunk corridor ahead of a fast projectile. */
public final class ProjectileChunkLoadingPlan {
    public static final int MIN_LOOKAHEAD_CHUNKS = 3;
    public static final int MAX_LOOKAHEAD_CHUNKS = 32;
    private static final int CHUNK_SIZE = 16;
    private static final int CORRIDOR_RADIUS = 1;
    private static final double MIN_HORIZONTAL_SPEED = 1.0E-10D;

    private ProjectileChunkLoadingPlan() {
    }

    public static Set<ChunkCoordinate> plan(double startX, double startZ, double velocityX, double velocityZ) {
        int startChunkX = blockToChunk(startX);
        int startChunkZ = blockToChunk(startZ);
        double horizontalSpeed = Math.hypot(velocityX, velocityZ);
        if (!Double.isFinite(horizontalSpeed) || horizontalSpeed < MIN_HORIZONTAL_SPEED) {
            return corridorAround(startChunkX, startChunkZ);
        }

        int lookaheadChunks = clamp((int) Math.ceil(horizontalSpeed / CHUNK_SIZE) + 2,
                MIN_LOOKAHEAD_CHUNKS, MAX_LOOKAHEAD_CHUNKS);
        double lookaheadDistance = (lookaheadChunks - 1) * (double) CHUNK_SIZE;
        double endX = startX + velocityX / horizontalSpeed * lookaheadDistance;
        double endZ = startZ + velocityZ / horizontalSpeed * lookaheadDistance;
        return traceCorridor(startX, startZ, endX, endZ, velocityX, velocityZ);
    }

    private static Set<ChunkCoordinate> traceCorridor(
            double startX, double startZ, double endX, double endZ, double velocityX, double velocityZ
    ) {
        LinkedHashSet<ChunkCoordinate> centerPath = new LinkedHashSet<>();
        for (ProjectileMotionPath.ChunkStep step : ProjectileMotionPath.trace(startX, startZ, endX, endZ)) {
            centerPath.add(new ChunkCoordinate(step.x(), step.z()));
        }

        // Keep one lateral neighbor on each side instead of dilating every center into a 3x3 square.
        LinkedHashSet<ChunkCoordinate> chunks = new LinkedHashSet<>(centerPath);
        int lateralX = Math.abs(velocityX) >= Math.abs(velocityZ) ? 0 : 1;
        int lateralZ = lateralX == 0 ? 1 : 0;
        centerPath.forEach(center -> addLateralNeighbors(chunks, center.x(), center.z(), lateralX, lateralZ));
        return Collections.unmodifiableSet(chunks);
    }

    private static Set<ChunkCoordinate> corridorAround(int chunkX, int chunkZ) {
        LinkedHashSet<ChunkCoordinate> chunks = new LinkedHashSet<>();
        chunks.add(new ChunkCoordinate(chunkX, chunkZ));
        return Collections.unmodifiableSet(chunks);
    }

    private static void addLateralNeighbors(
            Set<ChunkCoordinate> chunks, int centerX, int centerZ, int lateralX, int lateralZ
    ) {
        chunks.add(new ChunkCoordinate(centerX - lateralX * CORRIDOR_RADIUS, centerZ - lateralZ * CORRIDOR_RADIUS));
        chunks.add(new ChunkCoordinate(centerX + lateralX * CORRIDOR_RADIUS, centerZ + lateralZ * CORRIDOR_RADIUS));
    }

    private static int blockToChunk(double coordinate) {
        return Math.floorDiv((int) Math.floor(coordinate), CHUNK_SIZE);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record ChunkCoordinate(int x, int z) {
    }
}
