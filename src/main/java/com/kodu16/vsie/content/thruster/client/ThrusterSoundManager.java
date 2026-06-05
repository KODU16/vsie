package com.kodu16.vsie.content.thruster.client;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieSounds;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class ThrusterSoundManager {
    private static final double THROTTLE_EPSILON = 1.0E-6D;
    // Function: keep boost transients at 10% of the previously calculated volume.
    private static final float BOOST_VOLUME_SCALE = 0.1F;
    private static final Map<Long, ThrusterLoopSoundInstance> ACTIVE_THRUSTER_LOOPS = new HashMap<>();

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
            // Function: play the boost transient only when throttle rises from zero into active thrust.
            playBoostSound(thruster, soundProfile, throttle);
            mc.getSoundManager().play(loopSound);
            return;
        }

        loopSound.updateFromThruster(thruster);
    }

    private static void stopLoop(long thrusterKey) {
        ThrusterLoopSoundInstance loopSound = ACTIVE_THRUSTER_LOOPS.remove(thrusterKey);
        if (loopSound != null) {
            loopSound.requestStop();
        }
    }

    private static void playBoostSound(AbstractThrusterBlockEntity thruster, ThrusterSoundProfile profile, double throttle) {
        Level level = thruster.getLevel();
        if (level == null) {
            return;
        }

        Vec3 worldPos = getThrusterWorldPos(level, thruster.getBlockPos());
        float volume = Math.max(0.15F, profile.boostVolume() * (0.55F + (float) throttle * 0.45F)) * BOOST_VOLUME_SCALE;
        level.playLocalSound(
                worldPos.x,
                worldPos.y,
                worldPos.z,
                profile.boostSoundEvent(),
                SoundSource.BLOCKS,
                volume,
                profile.boostPitch(),
                false
        );
    }

    private static Vec3 getThrusterWorldPos(Level level, BlockPos thrusterPos) {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, thrusterPos);
        return ServerShipUtils.getBlockCenterWorld(subLevel, thrusterPos);
    }

    private record ThrusterSoundProfile(
            SoundEvent loopSoundEvent,
            SoundEvent boostSoundEvent,
            float loopMaxVolume,
            float boostVolume,
            float idlePitch,
            float pitchRange,
            float volumeExponent,
            float pitchExponent,
            float attackLerp,
            float releaseLerp,
            float boostPitch
    ) {
        private static ThrusterSoundProfile fromThrusterType(String thrusterType) {
            return switch (thrusterType) {
                // Function: ease each tier into a turbine-like spool curve instead of raw linear pitch/volume jumps.
                case "basic" -> new ThrusterSoundProfile(vsieSounds.BASIC_THRUSTER_LOOP.get(), vsieSounds.BASIC_THRUSTER_BOOST.get(), 0.62F, 0.78F, 0.96F, 0.18F, 0.78F, 0.62F, 0.30F, 0.18F, 1.08F);
                case "basic_vector" -> new ThrusterSoundProfile(vsieSounds.BASIC_VECTOR_THRUSTER_LOOP.get(), vsieSounds.BASIC_VECTOR_THRUSTER_BOOST.get(), 0.68F, 0.85F, 0.92F, 0.20F, 0.74F, 0.60F, 0.32F, 0.18F, 1.04F);
                case "medium" -> new ThrusterSoundProfile(vsieSounds.MEDIUM_THRUSTER_LOOP.get(), vsieSounds.MEDIUM_THRUSTER_BOOST.get(), 0.86F, 1.02F, 0.84F, 0.17F, 0.70F, 0.66F, 0.28F, 0.16F, 0.96F);
                case "large" -> new ThrusterSoundProfile(vsieSounds.LARGE_THRUSTER_LOOP.get(), vsieSounds.LARGE_THRUSTER_BOOST.get(), 1.05F, 1.20F, 0.76F, 0.15F, 0.68F, 0.70F, 0.24F, 0.14F, 0.90F);
                default -> null;
            };
        }
    }
}
