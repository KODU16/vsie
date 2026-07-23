package com.kodu16.vsie.content.weapon.electro_magnet_rail_cannon.client;

import com.kodu16.vsie.registries.vsieSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class RailCannonFireSoundManager {
    private RailCannonFireSoundManager() {
    }

    public static void play(double x, double y, double z, float range) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.getSoundManager() == null || range <= 0.0F) {
            return;
        }

        Vec3 soundPos = new Vec3(x, y, z);
        if (minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(soundPos) > range * range) {
            return;
        }

        // Function: with a one-block base attenuation distance, volume scales the linear fade radius to the requested range.
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                vsieSounds.ELECTRO_MAGNET_RAIL_CANNON_FIRE.get(),
                SoundSource.BLOCKS,
                range,
                1.0F,
                RandomSource.create(),
                x,
                y,
                z
        ));
    }
}
