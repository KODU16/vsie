package com.kodu16.vsie.registries;

import com.kodu16.vsie.vsie;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class vsieSounds {
    private static final float DEFAULT_SOUND_RANGE = 200.0F;

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, vsie.ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BASIC_THRUSTER_LOOP =
            register("basic_thruster_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASIC_THRUSTER_BOOST =
            register("basic_thruster_boost");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASIC_VECTOR_THRUSTER_LOOP =
            register("basic_vector_thruster_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASIC_VECTOR_THRUSTER_BOOST =
            register("basic_vector_thruster_boost");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_THRUSTER_LOOP =
            register("medium_thruster_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_THRUSTER_BOOST =
            register("medium_thruster_boost");
    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_THRUSTER_LOOP =
            register("large_thruster_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_THRUSTER_BOOST =
            register("large_thruster_boost");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASIC_MISSILE_LOOP =
            register("basic_missile_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_DECOUPLER_FIRE =
            register("missile_decoupler_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_EXPLODE1 =
            register("bullet_explode1");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASIC_CIWS_FIRE =
            register("basic_ciws_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARC_EMITTER_FIRE =
            register("arc_emitter_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRO_MAGNET_RAIL_CANNON_FIRE =
            register("electro_magnet_rail_cannon_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> CENIX_PLASMA_CANNON_FIRE =
            register("cenix_plasma_cannon_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_TURRET_FIRE =
            register("particle_turret_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEAVY_ELECTROMAGNET_TURRET_FIRE =
            register("heavy_electromagnet_turret_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_LASER_FIRE_LOOP =
            register("small_laser_fire_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_LASER_FIRE_LOOP =
            register("medium_laser_fire_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEAVY_LASER_FIRE_LOOP =
            register("heavy_laser_fire_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> INFRA_KNIFE_FIRE_LOOP =
            register("infra_knife_fire_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> INFRA_KNIFE_ACCELERATOR_FIRE =
            register("infra_knife_accelerator_fire");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
        // Keep all mod sounds positional and fade them out at about 200 blocks.
        return registerFixedRange(id, DEFAULT_SOUND_RANGE);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerFixedRange(String id, float range) {
        return SOUND_EVENTS.register(id, () ->
                SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(vsie.ID, id), range));
    }

    public static void register() {
    }
}
