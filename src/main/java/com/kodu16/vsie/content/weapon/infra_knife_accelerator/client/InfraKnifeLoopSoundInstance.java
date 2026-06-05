package com.kodu16.vsie.content.weapon.infra_knife_accelerator.client;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class InfraKnifeLoopSoundInstance extends AbstractTickableSoundInstance {
    private static final float LOOP_VOLUME = 0.74F;
    private static final float LOOP_PITCH = 1.12F;
    private final BlockPos weaponPos;
    private long lastUpdateTick = Long.MIN_VALUE;

    public InfraKnifeLoopSoundInstance(AbstractWeaponBlockEntity weapon, SoundEvent soundEvent) {
        super(soundEvent, SoundSource.BLOCKS, RandomSource.create());
        this.weaponPos = weapon.getBlockPos();
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        updateFromWeapon(weapon);
    }

    public void updateFromWeapon(AbstractWeaponBlockEntity weapon) {
        Level level = weapon.getLevel();
        if (level == null) {
            return;
        }

        Vec3 soundPos = getWeaponWorldPos(level, weaponPos);
        this.x = soundPos.x;
        this.y = soundPos.y;
        this.z = soundPos.z;
        this.volume = LOOP_VOLUME;
        this.pitch = LOOP_PITCH;
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

    private static Vec3 getWeaponWorldPos(Level level, BlockPos weaponPos) {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, weaponPos);
        return ServerShipUtils.getBlockCenterWorld(subLevel, weaponPos);
    }
}
