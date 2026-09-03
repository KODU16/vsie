package com.kodu16.vsie.foundation.projectile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Prevents chunk-loading performance from being hidden behind an infra-knife lifetime cap. */
class InfraKnifeChunkLoadingContractTest {
    @Test
    void usesTheSharedLongProjectileLifetime() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/content/bullet/entity/InfraKnifeBulletEntity.java"
        ));

        assertFalse(source.contains("MAX_TRAVEL_DISTANCE"));
        assertFalse(source.contains("MAX_LIFETIME_TICKS"));
        assertFalse(source.contains("protected int getMaxLifeTime()"));
    }
}
