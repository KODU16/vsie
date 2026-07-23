package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.particle.ShieldParticleOptions;
import com.kodu16.vsie.content.particle.ShieldParticleType;
import com.kodu16.vsie.vsie;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, vsie.ID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<ShieldParticleOptions>> SHIELD =
            PARTICLES.register("shield", () -> new ShieldParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CANNON_MUZZLE_SMOKE =
            PARTICLES.register("cannon_muzzle_smoke", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
