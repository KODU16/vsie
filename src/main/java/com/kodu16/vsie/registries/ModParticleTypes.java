package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.particle.ShieldParticleOptions;
import com.kodu16.vsie.content.particle.ShieldParticleType;
import com.kodu16.vsie.vsie;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticleTypes {

    // 功能：在 NeoForge 1.21.1 使用 BuiltInRegistries.PARTICLE_TYPE + DeferredRegister 注册粒子类型。
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, vsie.ID);

    // 功能：注册带自定义选项编解码的护盾粒子类型，用于服务器/客户端粒子同步。
    public static final DeferredHolder<ParticleType<?>, ParticleType<ShieldParticleOptions>> SHIELD =
            PARTICLES.register("shield",
                    () -> new ShieldParticleType(true)); // true = override limiter

    // 功能：把粒子注册器挂到 Mod 事件总线，确保在注册阶段生效。
    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
