package com.kodu16.vsie.content.turret.ciws.basicciws.client;

import com.kodu16.vsie.content.turret.ciws.basicciws.BasicCIWSBlockEntity;
import com.kodu16.vsie.registries.vsieSounds;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

public final class BasicCiwsSoundManager {
    private static final Map<Long, BasicCiwsLoopSoundInstance> ACTIVE_BASIC_CIWS_LOOPS = new HashMap<>();

    private BasicCiwsSoundManager() {
    }

    public static void updateCiws(BasicCIWSBlockEntity turret) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getSoundManager() == null) {
            return;
        }

        long turretKey = turret.getBlockPos().asLong();
        BasicCiwsLoopSoundInstance loopSound = ACTIVE_BASIC_CIWS_LOOPS.get(turretKey);
        if (loopSound != null && loopSound.isStopped()) {
            ACTIVE_BASIC_CIWS_LOOPS.remove(turretKey);
            loopSound = null;
        }

        if (turret.isRemoved() || !turret.isLoopSoundActive()) {
            stopCiws(turretKey);
            return;
        }

        if (loopSound == null) {
            loopSound = new BasicCiwsLoopSoundInstance(turret, vsieSounds.BASIC_CIWS_FIRE.get());
            ACTIVE_BASIC_CIWS_LOOPS.put(turretKey, loopSound);
            mc.getSoundManager().play(loopSound);
            return;
        }

        loopSound.updateFromTurret(turret);
    }

    private static void stopCiws(long turretKey) {
        BasicCiwsLoopSoundInstance loopSound = ACTIVE_BASIC_CIWS_LOOPS.remove(turretKey);
        if (loopSound != null) {
            loopSound.requestStop();
        }
    }
}
