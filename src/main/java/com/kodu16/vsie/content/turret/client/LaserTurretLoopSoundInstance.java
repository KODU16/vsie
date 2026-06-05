package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LaserTurretLoopSoundInstance extends AbstractTickableSoundInstance {
    private final LaserTurretSoundManager.LaserTurretSoundProfile soundProfile;
    private long lastUpdateTick = Long.MIN_VALUE;

    public LaserTurretLoopSoundInstance(AbstractTurretBlockEntity turret, LaserTurretSoundManager.LaserTurretSoundProfile soundProfile) {
        super(soundProfile.soundEvent(), SoundSource.BLOCKS, RandomSource.create());
        this.soundProfile = soundProfile;
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        updateFromTurret(turret);
    }

    public void updateFromTurret(AbstractTurretBlockEntity turret) {
        Level level = turret.getLevel();
        if (level == null) {
            return;
        }

        Vec3 soundPos = turret.getHudAimOriginWorld();
        this.x = soundPos.x;
        this.y = soundPos.y;
        this.z = soundPos.z;
        this.volume = soundProfile.volume();
        this.pitch = soundProfile.pitch();
        this.lastUpdateTick = level.getGameTime();
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            stop();
            return;
        }
        if (level.getGameTime() > lastUpdateTick + 1L) {
            stop();
        }
    }

    public void requestStop() {
        stop();
    }
}
