package com.kodu16.vsie.registries.fuel;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public record ThrusterFuelDefinition (
        ResourceLocation fluidId,
        float thrustMultiplier,
        float consumptionMultiplier,
        Optional<String> requiredMod
) {
    public static final Codec<ThrusterFuelDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("fluid").forGetter(ThrusterFuelDefinition::fluidId),
                    Codec.FLOAT.fieldOf("thrust_multiplier").forGetter(ThrusterFuelDefinition::thrustMultiplier),
                    Codec.FLOAT.fieldOf("consumption_multiplier").forGetter(ThrusterFuelDefinition::consumptionMultiplier),
                    Codec.STRING.optionalFieldOf("required_mod").forGetter(ThrusterFuelDefinition::requiredMod)
            ).apply(instance, ThrusterFuelDefinition::new));


    public Fluid getFluid() {
        return BuiltInRegistries.FLUID.getOptional(this.fluidId).orElse(Fluids.EMPTY);
    }
}
