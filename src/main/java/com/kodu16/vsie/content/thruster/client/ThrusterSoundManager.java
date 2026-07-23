package com.kodu16.vsie.content.thruster.client;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.registries.vsieSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

public final class ThrusterSoundManager {
    private static final double THROTTLE_EPSILON = 1.0E-6D;
    private static final double AUDIBLE_START_THROTTLE = 0.12D;
    private static final double AUDIBLE_KEEP_THROTTLE = 0.04D;
    private static final int AUDIBLE_START_TICKS = 4;
    private static final Map<Long, ThrusterLoopSoundInstance> ACTIVE_THRUSTER_LOOPS = new HashMap<>();
    private static final Map<Long, Integer> PENDING_THRUSTER_START_TICKS = new HashMap<>();

    private ThrusterSoundManager() {
    }

    public static void updateThruster(AbstractThrusterBlockEntity thruster) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getSoundManager() == null) {
            return;
        }

        long thrusterKey = thruster.getBlockPos().asLong();
        ThrusterSoundProfile soundProfile = ThrusterSoundProfile.fromThrusterType(thruster.getthrustertype());
        if (soundProfile == null) {
            stopLoop(thrusterKey);
            return;
        }

        ThrusterLoopSoundInstance loopSound = ACTIVE_THRUSTER_LOOPS.get(thrusterKey);
        if (loopSound != null && loopSound.isStopped()) {
            ACTIVE_THRUSTER_LOOPS.remove(thrusterKey);
            loopSound = null;
        }

        double throttle = Mth.clamp(thruster.getData().getThrottle(), 0.0D, 1.0D);
        if (throttle <= THROTTLE_EPSILON) {
            PENDING_THRUSTER_START_TICKS.remove(thrusterKey);
            if (loopSound != null) {
                // Function: let the loop sound release naturally so engine spool-down is audible.
                loopSound.updateFromThruster(thruster);
                if (loopSound.isStopped()) {
                    ACTIVE_THRUSTER_LOOPS.remove(thrusterKey);
                }
            }
            return;
        }

        if (loopSound == null) {
            if (throttle < AUDIBLE_START_THROTTLE) {
                PENDING_THRUSTER_START_TICKS.remove(thrusterKey);
                return;
            }
            int pendingTicks = PENDING_THRUSTER_START_TICKS.getOrDefault(thrusterKey, 0) + 1;
            if (pendingTicks < AUDIBLE_START_TICKS) {
                // Function: require a few consecutive client ticks of meaningful thrust so trim jitter does not retrigger spool-up audio.
                PENDING_THRUSTER_START_TICKS.put(thrusterKey, pendingTicks);
                return;
            }
            PENDING_THRUSTER_START_TICKS.remove(thrusterKey);
            loopSound = new ThrusterLoopSoundInstance(
                    thruster,
                    soundProfile.loopSoundEvent(),
                    soundProfile.loopMaxVolume(),
                    soundProfile.idlePitch(),
                    soundProfile.pitchRange(),
                    soundProfile.volumeExponent(),
                    soundProfile.pitchExponent(),
                    soundProfile.attackLerp(),
                    soundProfile.releaseLerp()
            );
            ACTIVE_THRUSTER_LOOPS.put(thrusterKey, loopSound);
            mc.getSoundManager().play(loopSound);
            return;
        }

        if (throttle < AUDIBLE_KEEP_THROTTLE) {
            // Function: once a thruster loop is running, keep a small hysteresis band so low-end trim demand does not chatter start/stop.
            loopSound.updateFromThruster(thruster);
            if (loopSound.isStopped()) {
                ACTIVE_THRUSTER_LOOPS.remove(thrusterKey);
            }
            return;
        }

        loopSound.updateFromThruster(thruster);
    }

    private static void stopLoop(long thrusterKey) {
        PENDING_THRUSTER_START_TICKS.remove(thrusterKey);
        ThrusterLoopSoundInstance loopSound = ACTIVE_THRUSTER_LOOPS.remove(thrusterKey);
        if (loopSound != null) {
            loopSound.requestStop();
        }
    }

    private record ThrusterSoundProfile(
            SoundEvent loopSoundEvent,
            float loopMaxVolume,
            float idlePitch,
            float pitchRange,
            float volumeExponent,
            float pitchExponent,
            float attackLerp,
            float releaseLerp
    ) {
        private static ThrusterSoundProfile fromThrusterType(String thrusterType) {
            return switch (thrusterType) {
                // Function: ease each tier into a turbine-like spool curve instead of raw linear pitch/volume jumps.
                case "basic" -> new ThrusterSoundProfile(vsieSounds.BASIC_THRUSTER_LOOP.get(), 0.62F, 0.96F, 0.18F, 0.78F, 0.62F, 0.30F, 0.18F);
                case "basic_vector" -> new ThrusterSoundProfile(vsieSounds.BASIC_VECTOR_THRUSTER_LOOP.get(), 0.68F, 0.92F, 0.20F, 0.74F, 0.60F, 0.32F, 0.18F);
                case "medium" -> new ThrusterSoundProfile(vsieSounds.MEDIUM_THRUSTER_LOOP.get(), 0.86F, 0.84F, 0.17F, 0.70F, 0.66F, 0.28F, 0.16F);
                case "large" -> new ThrusterSoundProfile(vsieSounds.LARGE_THRUSTER_LOOP.get(), 1.05F, 0.76F, 0.15F, 0.68F, 0.70F, 0.24F, 0.14F);
                default -> null;
            };
        }
    }
}
