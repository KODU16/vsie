package com.kodu16.vsie.foundation.projectile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps projectile-only chunks collision-free without weakening precise client motion. */
class ProjectileCollisionPolicyContractTest {
    @Test
    void scansOnlyNaturallyLoadedMotionSegments() throws IOException {
        String bullet = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/content/bullet/AbstractBulletEntity.java"
        ));
        String manager = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/foundation/projectile/ProjectileCorridorManager.java"
        ));
        String entities = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/registries/vsieEntities.java"
        ));
        String heavyBullet = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/content/bullet/entity/HeavyElectroMagnetBulletEntity.java"
        ));
        String railBullet = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/content/bullet/entity/ElectroMagnetRailCannonBulletEntity.java"
        ));

        assertTrue(bullet.contains("findCollisionInEligibleChunks"));
        assertTrue(bullet.contains("ProjectileCorridorManager.isCollisionEligible"));
        assertTrue(bullet.contains("if (movementEndT <= CHUNK_EDGE_EPSILON)"));
        assertFalse(bullet.contains("ProjectileUtil.getHitResultOnMoveVector"));
        assertFalse(bullet.contains("this.level().getEntities("));
        assertTrue(manager.contains("ServerTickEvent.Post"));
        assertTrue(manager.contains("ForwardChunkLoadController"));
        assertTrue(heavyBullet.contains("isCurrentChunkCollisionEligible()"));
        assertTrue(railBullet.contains("isCurrentChunkCollisionEligible()"));
        assertTrue(bullet.contains("tickClientFilteredMotion(movement)"));
        assertTrue(entities.contains("clientTrackingRange(256).updateInterval(1)"));
    }
}
