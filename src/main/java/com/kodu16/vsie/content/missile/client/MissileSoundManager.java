package com.kodu16.vsie.content.missile.client;

import com.kodu16.vsie.content.missile.AbstractMissileEntity;
import com.kodu16.vsie.registries.vsieSounds;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

public final class MissileSoundManager {
    private static final Map<Integer, BasicMissileLoopSoundInstance> ACTIVE_BASIC_MISSILE_LOOPS = new HashMap<>();

    private MissileSoundManager() {
    }

    public static void updateMissile(AbstractMissileEntity missile) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getSoundManager() == null) {
            return;
        }

        int missileId = missile.getId();
        if (!"basic_missile".equals(missile.getmissiletype())) {
            stopMissile(missileId);
            return;
        }

        BasicMissileLoopSoundInstance loopSound = ACTIVE_BASIC_MISSILE_LOOPS.get(missileId);
        if (loopSound != null && loopSound.isStopped()) {
            ACTIVE_BASIC_MISSILE_LOOPS.remove(missileId);
            loopSound = null;
        }

        if (missile.isRemoved() || missile.getDeltaMovement().lengthSqr() <= 1.0E-6D) {
            stopMissile(missileId);
            return;
        }

        if (loopSound == null) {
            loopSound = new BasicMissileLoopSoundInstance(missile, vsieSounds.BASIC_MISSILE_LOOP.get());
            ACTIVE_BASIC_MISSILE_LOOPS.put(missileId, loopSound);
            mc.getSoundManager().play(loopSound);
            return;
        }

        loopSound.updateFromMissile(missile);
    }

    public static void stopMissile(AbstractMissileEntity missile) {
        stopMissile(missile.getId());
    }

    private static void stopMissile(int missileId) {
        BasicMissileLoopSoundInstance loopSound = ACTIVE_BASIC_MISSILE_LOOPS.remove(missileId);
        if (loopSound != null) {
            loopSound.requestStop();
        }
    }
}
