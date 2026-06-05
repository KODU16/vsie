package com.kodu16.vsie.content.weapon.infra_knife_accelerator.client;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.registries.vsieSounds;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

public final class InfraKnifeSoundManager {
    private static final Map<Long, InfraKnifeLoopSoundInstance> ACTIVE_INFRA_KNIFE_LOOPS = new HashMap<>();

    private InfraKnifeSoundManager() {
    }

    public static void updateWeapon(AbstractWeaponBlockEntity weapon) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getSoundManager() == null) {
            return;
        }

        long weaponKey = weapon.getBlockPos().asLong();
        InfraKnifeLoopSoundInstance loopSound = ACTIVE_INFRA_KNIFE_LOOPS.get(weaponKey);
        if (loopSound != null && loopSound.isStopped()) {
            ACTIVE_INFRA_KNIFE_LOOPS.remove(weaponKey);
            loopSound = null;
        }

        if (weapon.isRemoved() || !weapon.getData().isfiring || !"infra_knife_accelerator".equals(weapon.getweapontype())) {
            stopWeapon(weaponKey);
            return;
        }

        if (loopSound == null) {
            loopSound = new InfraKnifeLoopSoundInstance(weapon, vsieSounds.INFRA_KNIFE_FIRE_LOOP.get());
            ACTIVE_INFRA_KNIFE_LOOPS.put(weaponKey, loopSound);
            mc.getSoundManager().play(loopSound);
            return;
        }

        loopSound.updateFromWeapon(weapon);
    }

    private static void stopWeapon(long weaponKey) {
        InfraKnifeLoopSoundInstance loopSound = ACTIVE_INFRA_KNIFE_LOOPS.remove(weaponKey);
        if (loopSound != null) {
            loopSound.requestStop();
        }
    }
}
