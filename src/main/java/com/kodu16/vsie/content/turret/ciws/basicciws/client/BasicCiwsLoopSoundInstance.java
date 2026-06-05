package com.kodu16.vsie.content.turret.ciws.basicciws.client;

import com.kodu16.vsie.content.turret.ciws.basicciws.BasicCIWSBlockEntity;
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

public class BasicCiwsLoopSoundInstance extends AbstractTickableSoundInstance {
    private static final float LOOP_VOLUME = 0.88F;
    private static final float LOOP_PITCH = 1.0F;
    private final BlockPos turretPos;
    private long lastUpdateTick = Long.MIN_VALUE;

    public BasicCiwsLoopSoundInstance(BasicCIWSBlockEntity turret, SoundEvent soundEvent) {
        super(soundEvent, SoundSource.BLOCKS, RandomSource.create());
        this.turretPos = turret.getBlockPos();
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        updateFromTurret(turret);
    }

    public void updateFromTurret(BasicCIWSBlockEntity turret) {
        Level level = turret.getLevel();
        if (level == null) {
            return;
        }

        Vec3 worldPos = getTurretWorldPos(level, turretPos);
        this.x = worldPos.x;
        this.y = worldPos.y;
        this.z = worldPos.z;
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

    private static Vec3 getTurretWorldPos(Level level, BlockPos turretPos) {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, turretPos);
        return ServerShipUtils.getBlockCenterWorld(subLevel, turretPos);
    }
}
