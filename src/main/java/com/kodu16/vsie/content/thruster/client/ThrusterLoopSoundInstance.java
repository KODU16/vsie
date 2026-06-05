package com.kodu16.vsie.content.thruster.client;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ThrusterLoopSoundInstance extends AbstractTickableSoundInstance {
    private static final double THROTTLE_EPSILON = 1.0E-6D;
    private final BlockPos thrusterPos;
    private final float maxVolume;
    private final float idlePitch;
    private final float pitchRange;
    private final float volumeExponent;
    private final float pitchExponent;
    private final float attackLerp;
    private final float releaseLerp;
    private float smoothedThrottle;
    private float targetThrottle;

    public ThrusterLoopSoundInstance(
            AbstractThrusterBlockEntity thruster,
            SoundEvent soundEvent,
            float maxVolume,
            float idlePitch,
            float pitchRange,
            float volumeExponent,
            float pitchExponent,
            float attackLerp,
            float releaseLerp
    ) {
        super(soundEvent, SoundSource.BLOCKS, RandomSource.create());
        this.thrusterPos = thruster.getBlockPos().immutable();
        this.maxVolume = maxVolume;
        this.idlePitch = idlePitch;
        this.pitchRange = pitchRange;
        this.volumeExponent = volumeExponent;
        this.pitchExponent = pitchExponent;
        this.attackLerp = attackLerp;
        this.releaseLerp = releaseLerp;
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        updateFromThruster(thruster);
    }

    public void updateFromThruster(AbstractThrusterBlockEntity thruster) {
        this.targetThrottle = (float) Mth.clamp(thruster.getData().getThrottle(), 0.0D, 1.0D);
        Vec3 worldPos = getThrusterWorldPos(thruster.getLevel(), thrusterPos);
        this.x = worldPos.x;
        this.y = worldPos.y;
        this.z = worldPos.z;
        syncAudioState();
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            stop();
            return;
        }

        if (!(level.getBlockEntity(thrusterPos) instanceof AbstractThrusterBlockEntity thruster)) {
            stop();
            return;
        }
        updateFromThruster(thruster);
    }

    public void requestStop() {
        stop();
    }

    private void syncAudioState() {
        float lerpRate = targetThrottle > smoothedThrottle ? attackLerp : releaseLerp;
        smoothedThrottle = Mth.lerp(lerpRate, smoothedThrottle, targetThrottle);
        if (targetThrottle <= THROTTLE_EPSILON && smoothedThrottle <= 0.0025F) {
            stop();
            return;
        }

        float volumeFactor = Mth.clamp((float) Math.pow(smoothedThrottle, volumeExponent), 0.0F, 1.0F);
        float pitchFactor = Mth.clamp((float) Math.pow(smoothedThrottle, pitchExponent), 0.0F, 1.0F);
        // Function: ease loop loudness and pitch so the jet sound spools up instead of stepping every tick.
        this.volume = Math.max(0.02F, maxVolume * volumeFactor);
        this.pitch = idlePitch + pitchRange * pitchFactor;
    }

    private static Vec3 getThrusterWorldPos(Level level, BlockPos thrusterPos) {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, thrusterPos);
        // Function: use world-space block centers so thruster audio follows moving sublevels correctly.
        return ServerShipUtils.getBlockCenterWorld(subLevel, thrusterPos);
    }
}
