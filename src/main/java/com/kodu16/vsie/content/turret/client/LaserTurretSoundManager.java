package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.registries.vsieSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;

public final class LaserTurretSoundManager {
    private static final double ACTIVE_DISTANCE_EPSILON = 1.0E-6D;
    private static final Map<Long, LaserTurretLoopSoundInstance> ACTIVE_LASER_LOOPS = new HashMap<>();

    private LaserTurretSoundManager() {
    }

    public static void updateTurret(AbstractTurretBlockEntity turret) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getSoundManager() == null) {
            return;
        }

        long turretKey = turret.getBlockPos().asLong();
        LaserTurretSoundProfile soundProfile = LaserTurretSoundProfile.fromTurretType(turret.getturrettype());
        if (soundProfile == null) {
            stopTurret(turretKey);
            return;
        }

        LaserTurretLoopSoundInstance loopSound = ACTIVE_LASER_LOOPS.get(turretKey);
        if (loopSound != null && loopSound.isStopped()) {
            ACTIVE_LASER_LOOPS.remove(turretKey);
            loopSound = null;
        }

        if (turret.isRemoved() || turret.getTargetDistance() <= ACTIVE_DISTANCE_EPSILON) {
            stopTurret(turretKey);
            return;
        }

        if (loopSound == null) {
            loopSound = new LaserTurretLoopSoundInstance(turret, soundProfile);
            ACTIVE_LASER_LOOPS.put(turretKey, loopSound);
            mc.getSoundManager().play(loopSound);
            return;
        }

        loopSound.updateFromTurret(turret);
    }

    private static void stopTurret(long turretKey) {
        LaserTurretLoopSoundInstance loopSound = ACTIVE_LASER_LOOPS.remove(turretKey);
        if (loopSound != null) {
            loopSound.requestStop();
        }
    }

    public record LaserTurretSoundProfile(SoundEvent soundEvent, float volume, float pitch) {
        public static LaserTurretSoundProfile fromTurretType(String turretType) {
            return switch (turretType) {
                // Function: laser turrets share the same scorch loop source for now, but keep per-class ids for later replacement.
                case "small_laser" -> new LaserTurretSoundProfile(vsieSounds.SMALL_LASER_FIRE_LOOP.get(), 0.52F, 1.18F);
                case "medium_laser" -> new LaserTurretSoundProfile(vsieSounds.MEDIUM_LASER_FIRE_LOOP.get(), 0.68F, 1.08F);
                case "heavy_laser" -> new LaserTurretSoundProfile(vsieSounds.HEAVY_LASER_FIRE_LOOP.get(), 0.92F, 0.94F);
                default -> null;
            };
        }
    }
}
