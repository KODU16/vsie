package com.kodu16.vsie.content.missile.client;

import com.kodu16.vsie.content.missile.AbstractMissileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BasicMissileLoopSoundInstance extends AbstractTickableSoundInstance {
    private static final double MIN_MOVEMENT_SQR = 1.0E-6D;
    private final int missileId;
    private float smoothedVolume;
    private float smoothedPitch;

    public BasicMissileLoopSoundInstance(AbstractMissileEntity missile, SoundEvent soundEvent) {
        super(soundEvent, SoundSource.HOSTILE, RandomSource.create());
        this.missileId = missile.getId();
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        updateFromMissile(missile);
    }

    public void updateFromMissile(AbstractMissileEntity missile) {
        this.x = missile.getX();
        this.y = missile.getY();
        this.z = missile.getZ();

        Vec3 movement = missile.getDeltaMovement();
        float targetVolume = movement.lengthSqr() <= MIN_MOVEMENT_SQR ? 0.0F : 0.48F;
        float targetPitch = 0.92F + (float) Mth.clamp(movement.length() * 0.02D, 0.0D, 0.16D);
        smoothedVolume = Mth.lerp(0.35F, smoothedVolume, targetVolume);
        smoothedPitch = Mth.lerp(0.25F, smoothedPitch == 0.0F ? targetPitch : smoothedPitch, targetPitch);
        this.volume = smoothedVolume;
        this.pitch = smoothedPitch;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            stop();
            return;
        }

        Entity entity = level.getEntity(missileId);
        if (!(entity instanceof AbstractMissileEntity missile) || missile.isRemoved()) {
            stop();
            return;
        }

        updateFromMissile(missile);
        if (this.volume <= 0.01F) {
            stop();
        }
    }

    public void requestStop() {
        stop();
    }
}
