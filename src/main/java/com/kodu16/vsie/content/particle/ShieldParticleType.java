package com.kodu16.vsie.content.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ShieldParticleType extends ParticleType<ShieldParticleOptions> {

    public ShieldParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<ShieldParticleOptions> codec() {
        return ShieldParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ShieldParticleOptions> streamCodec() {
        return ShieldParticleOptions.STREAM_CODEC;
    }
}
