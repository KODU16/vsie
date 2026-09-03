package com.kodu16.vsie.foundation.projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Traces an ordered horizontal chunk path and records where each chunk begins. */
public final class ProjectileMotionPath {
    private static final int CHUNK_SIZE = 16;
    private static final double MIN_DISTANCE = 1.0E-10D;

    private ProjectileMotionPath() {
    }

    public static List<ChunkStep> trace(double startX, double startZ, double endX, double endZ) {
        List<ChunkStep> path = new ArrayList<>();
        int chunkX = blockToChunk(startX);
        int chunkZ = blockToChunk(startZ);
        int endChunkX = blockToChunk(endX);
        int endChunkZ = blockToChunk(endZ);
        path.add(new ChunkStep(chunkX, chunkZ, 0.0D));

        double dx = endX - startX;
        double dz = endZ - startZ;
        if (Math.abs(dx) < MIN_DISTANCE && Math.abs(dz) < MIN_DISTANCE) {
            return Collections.unmodifiableList(path);
        }

        int stepX = Integer.compare(endChunkX, chunkX);
        int stepZ = Integer.compare(endChunkZ, chunkZ);
        double nextX = stepX == 0 ? Double.POSITIVE_INFINITY : firstBoundaryT(startX, dx, chunkX, stepX);
        double nextZ = stepZ == 0 ? Double.POSITIVE_INFINITY : firstBoundaryT(startZ, dz, chunkZ, stepZ);
        double deltaX = stepX == 0 ? Double.POSITIVE_INFINITY : CHUNK_SIZE / Math.abs(dx);
        double deltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : CHUNK_SIZE / Math.abs(dz);

        while (chunkX != endChunkX || chunkZ != endChunkZ) {
            double entryT = Math.min(nextX, nextZ);
            boolean advanceX = nextX <= nextZ;
            boolean advanceZ = nextZ <= nextX;
            if (advanceX) {
                chunkX += stepX;
                nextX += deltaX;
            }
            if (advanceZ) {
                chunkZ += stepZ;
                nextZ += deltaZ;
            }
            path.add(new ChunkStep(chunkX, chunkZ, Math.max(0.0D, Math.min(1.0D, entryT))));
        }
        return Collections.unmodifiableList(path);
    }

    private static int blockToChunk(double coordinate) {
        return Math.floorDiv((int) Math.floor(coordinate), CHUNK_SIZE);
    }

    private static double firstBoundaryT(double start, double delta, int chunk, int step) {
        double boundary = step > 0 ? (chunk + 1) * (double) CHUNK_SIZE : chunk * (double) CHUNK_SIZE;
        return (boundary - start) / delta;
    }

    public record ChunkStep(int x, int z, double entryT) {
    }
}
