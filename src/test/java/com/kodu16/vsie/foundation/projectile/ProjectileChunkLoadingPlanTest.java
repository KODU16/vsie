package com.kodu16.vsie.foundation.projectile;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileChunkLoadingPlanTest {
    @Test
    void buildsAThreeChunkWideForwardCorridorAtNormalBulletSpeed() {
        Set<ProjectileChunkLoadingPlan.ChunkCoordinate> chunks =
                ProjectileChunkLoadingPlan.plan(0.5D, 0.5D, 6.0D, 0.0D);

        assertTrue(chunks.contains(new ProjectileChunkLoadingPlan.ChunkCoordinate(0, 0)));
        assertTrue(chunks.contains(new ProjectileChunkLoadingPlan.ChunkCoordinate(2, -1)));
        assertTrue(chunks.contains(new ProjectileChunkLoadingPlan.ChunkCoordinate(2, 1)));
        assertEquals(9, chunks.size());
        assertFalse(chunks.contains(new ProjectileChunkLoadingPlan.ChunkCoordinate(3, 0)));
    }

    @Test
    void prioritizesCenterPathBeforeCorridorNeighbors() {
        var chunks = java.util.List.copyOf(ProjectileChunkLoadingPlan.plan(0.5D, 0.5D, 6.0D, 0.0D));

        assertEquals(new ProjectileChunkLoadingPlan.ChunkCoordinate(0, 0), chunks.get(0));
        assertEquals(new ProjectileChunkLoadingPlan.ChunkCoordinate(2, 0), chunks.get(2));
    }

    @Test
    void followsDiagonalMotionInsteadOfChoosingOnlyOneAxis() {
        Set<ProjectileChunkLoadingPlan.ChunkCoordinate> chunks =
                ProjectileChunkLoadingPlan.plan(0.5D, 0.5D, 80.0D, 80.0D);

        assertTrue(chunks.stream().anyMatch(chunk -> chunk.x() >= 6 && chunk.z() >= 6));
    }

    @Test
    void fasterProjectilesReceiveMoreLookaheadWithoutExceedingTheCap() {
        Set<ProjectileChunkLoadingPlan.ChunkCoordinate> slow =
                ProjectileChunkLoadingPlan.plan(0.5D, 0.5D, 6.0D, 0.0D);
        Set<ProjectileChunkLoadingPlan.ChunkCoordinate> fast =
                ProjectileChunkLoadingPlan.plan(0.5D, 0.5D, 600.0D, 0.0D);

        int slowMaxX = slow.stream().mapToInt(ProjectileChunkLoadingPlan.ChunkCoordinate::x).max().orElseThrow();
        int fastMaxX = fast.stream().mapToInt(ProjectileChunkLoadingPlan.ChunkCoordinate::x).max().orElseThrow();
        assertTrue(fastMaxX > slowMaxX);
        assertTrue(fastMaxX <= ProjectileChunkLoadingPlan.MAX_LOOKAHEAD_CHUNKS + 1);
    }
}
