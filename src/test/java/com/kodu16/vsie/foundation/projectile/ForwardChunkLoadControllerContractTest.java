package com.kodu16.vsie.foundation.projectile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the forward corridor to change-only forced tickets beside RPL's precise path loader. */
class ForwardChunkLoadControllerContractTest {
    @Test
    void updatesOnlyCorridorTicketDifferences() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/foundation/projectile/ForwardChunkLoadController.java"
        ));
        String bulletSource = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/content/bullet/AbstractBulletEntity.java"
        ));
        String managerSource = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/foundation/projectile/ProjectileCorridorManager.java"
        ));

        assertTrue(source.contains("setChunkForced"));
        assertTrue(source.contains("forceChunk"));
        assertTrue(source.contains("unforceChunk"));
        assertFalse(source.contains("RitchiesProjectileLib.queueForceLoad"));
        assertFalse(source.contains("MAX_NEW_CHUNKS_PER_TICK"));
        assertTrue(source.contains("isCollisionEligible"));
        assertTrue(source.contains("lastChunkX"));
        assertTrue(managerSource.contains("controller.update"));
        assertTrue(bulletSource.contains("requestPreciseMotionChunkLoading(serverLevel, motionPath)"));
        assertTrue(bulletSource.contains("RitchiesProjectileLib.queueForceLoad"));
    }
}
