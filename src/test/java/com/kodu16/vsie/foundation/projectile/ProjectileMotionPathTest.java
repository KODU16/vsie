package com.kodu16.vsie.foundation.projectile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks one reusable ordered chunk trace to each projectile movement segment. */
class ProjectileMotionPathTest {
    @Test
    void recordsOrderedChunkEntryTimes() {
        var path = ProjectileMotionPath.trace(8.0D, 8.0D, 40.0D, 8.0D);

        assertEquals(3, path.size());
        assertEquals(new ProjectileMotionPath.ChunkStep(0, 0, 0.0D), path.get(0));
        assertEquals(1, path.get(1).x());
        assertEquals(0.25D, path.get(1).entryT(), 1.0E-10D);
        assertEquals(2, path.get(2).x());
        assertEquals(0.75D, path.get(2).entryT(), 1.0E-10D);
    }

    @Test
    void tracesDiagonalMotionOnceWithoutLosingOrder() {
        var path = ProjectileMotionPath.trace(8.0D, 8.0D, 40.0D, 40.0D);

        assertEquals(3, path.size());
        assertTrue(path.get(0).entryT() < path.get(1).entryT());
        assertTrue(path.get(1).entryT() < path.get(2).entryT());
        assertEquals(2, path.get(2).x());
        assertEquals(2, path.get(2).z());
    }
}
