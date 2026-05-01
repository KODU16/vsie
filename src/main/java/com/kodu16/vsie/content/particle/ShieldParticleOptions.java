package com.kodu16.vsie.content.particle;

import com.kodu16.vsie.registries.ModParticleTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class ShieldParticleOptions implements ParticleOptions {

    public static final MapCodec<ShieldParticleOptions> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.INT.fieldOf("life_offset").forGetter(ShieldParticleOptions::getLifeOffset)
            ).apply(instance, ShieldParticleOptions::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ShieldParticleOptions::getLifeOffset,
            ShieldParticleOptions::new
    );

    private final int lifeOffset;

    public ShieldParticleOptions(int lifeOffset) {
        this.lifeOffset = Math.max(0, lifeOffset);
    }

    public int getLifeOffset() {
        return lifeOffset;
    }

    @Override
    public ParticleType<ShieldParticleOptions> getType() {
        return ModParticleTypes.SHIELD.get();
    }
}
