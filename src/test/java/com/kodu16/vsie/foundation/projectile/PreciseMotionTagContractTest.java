package com.kodu16.vsie.foundation.projectile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps every VSIE long-range projectile on RPL's precise-motion network path. */
class PreciseMotionTagContractTest {
    @Test
    void includesEveryLongRangeVsieProjectile() throws IOException {
        String tag = Files.readString(Path.of(
                "src/main/resources/data/ritchiesprojectilelib/tags/entity_type/precise_motion.json"
        ));
        List<String> entityIds = List.of(
                "vsie:basic_missile",
                "vsie:particle_bullet",
                "vsie:custom_turret_projectile",
                "vsie:cenix_plasma_bullet",
                "vsie:heavy_electromagnetic_bullet",
                "vsie:electro_magnet_rail_cannon_bullet",
                "vsie:infra_knife_bullet",
                "vsie:warp_projectile"
        );

        for (String entityId : entityIds) {
            assertTrue(tag.contains('"' + entityId + '"'), () -> "Missing precise-motion entity: " + entityId);
        }
    }
}
