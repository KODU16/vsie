package com.kodu16.vsie.registries;

import java.util.function.Supplier;

import com.kodu16.vsie.vsie;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class vsieFluids {
    public static final CreateRegistrate REGISTRATE = vsie.registrate();
    public static void register() {} //Loads this class

    private static <R, T extends R> NonNullBiConsumer<DataGenContext<R, T>, RegistrateLangProvider> LANG() {
        return (ctx, prov) -> {};
    }

    private static final Supplier<FluidTypeFactory> DTFUEL_TYPE_FACTORY = createDtfuelTypeFactory();
    private static final Supplier<FluidTypeFactory> E710_TYPE_FACTORY = createE710TypeFactory();
    private static final Supplier<FluidTypeFactory> CHARGED_PARTICLES_FUEL_TYPE_FACTORY = createChargedParticlesFuelTypeFactory();

    // 功能：NeoForge 1.21.1 将 ForgeFlowingFluid 重命名为 BaseFlowingFluid，这里同步更新流体注册泛型和 Source 构造器。
    public static final FluidEntry<BaseFlowingFluid.Flowing> DTFUEL = REGISTRATE.fluid("dtfuel",
                    ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still"),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow"),
                    DTFUEL_TYPE_FACTORY.get())
            .renderType(getSidedRenderType())
            .source(BaseFlowingFluid.Source::new)
            .setData(ProviderType.LANG, LANG())
            .block().setData(ProviderType.LANG, LANG()).build()
            .bucket().setData(ProviderType.LANG, LANG()).build()
            .properties(p -> p.viscosity(1000).density(500))
            .fluidProperties(p -> p.levelDecreasePerBlock(1)
                    .tickRate(7)
                    .slopeFindDistance(3)
                    .explosionResistance(100f))
            .register();

    // 功能：注册跃迁用 E-710；它不写入 thruster_fuels 数据，因此不会被引擎当作燃料抽取。
    public static final FluidEntry<BaseFlowingFluid.Flowing> E710 = REGISTRATE.fluid("e710",
                    ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still"),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow"),
                    E710_TYPE_FACTORY.get())
            .renderType(getSidedRenderType())
            .source(BaseFlowingFluid.Source::new)
            .setData(ProviderType.LANG, LANG())
            .block().setData(ProviderType.LANG, LANG()).build()
            .bucket().setData(ProviderType.LANG, LANG()).build()
            .properties(p -> p.viscosity(1300).density(650))
            .fluidProperties(p -> p.levelDecreasePerBlock(1)
                    .tickRate(8)
                    .slopeFindDistance(3)
                    .explosionResistance(100f))
            .register();

    // Function: charged-particles fuel is a lime thruster fuel configured by data/vsie/thruster_fuels.
    public static final FluidEntry<BaseFlowingFluid.Flowing> CHARGED_PARTICLES_FUEL = REGISTRATE.fluid("charged_particles_fuel",
                    ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still"),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow"),
                    CHARGED_PARTICLES_FUEL_TYPE_FACTORY.get())
            .renderType(getSidedRenderType())
            .source(BaseFlowingFluid.Source::new)
            .setData(ProviderType.LANG, LANG())
            .block().setData(ProviderType.LANG, LANG()).build()
            .bucket().setData(ProviderType.LANG, LANG()).build()
            .properties(p -> p.viscosity(1000).density(500))
            .fluidProperties(p -> p.levelDecreasePerBlock(1)
                    .tickRate(7)
                    .slopeFindDistance(3)
                    .explosionResistance(100f))
            .register();

    //Helpers

    private static Supplier<Supplier<RenderType>> getSidedRenderType() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return vsieFluidsClient::getDtfuelRenderType;
        }
        return () -> () -> null;
    }

    private static Supplier<FluidTypeFactory> createDtfuelTypeFactory() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return vsieFluidsClient.getDtfuelTypeFactory();
        }
        return createGenericFactory();
    }

    private static Supplier<FluidTypeFactory> createE710TypeFactory() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return vsieFluidsClient.getE710TypeFactory();
        }
        return createGenericFactory();
    }

    private static Supplier<FluidTypeFactory> createChargedParticlesFuelTypeFactory() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return vsieFluidsClient.getChargedParticlesFuelTypeFactory();
        }
        return createGenericFactory();
    }

    private static Supplier<FluidTypeFactory> createGenericFactory() {
        return () -> (properties, stillTexture, flowingTexture) -> new FluidType(properties);
    }
}
