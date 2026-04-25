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
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class vsieFluids {
    public static final CreateRegistrate REGISTRATE = vsie.registrate();
    public static void register() {} //Loads this class

    private static <R, T extends R> NonNullBiConsumer<DataGenContext<R, T>, RegistrateLangProvider> LANG() {
        return (ctx, prov) -> {};
    }

    private static final Supplier<FluidTypeFactory> DTFUEL_TYPE_FACTORY = DistExecutor.unsafeRunForDist(
            () -> vsieFluidsClient::getDtfuelTypeFactory,
            () -> vsieFluids::createGenericFactory
    );

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

    //Helpers

    private static Supplier<RenderType> getSidedRenderType() {
        return DistExecutor.unsafeRunForDist(
                () -> vsieFluidsClient::getDtfuelRenderType,
                () -> () -> null
        );
    }

    private static Supplier<FluidTypeFactory> createGenericFactory() {
        return () -> (properties, stillTexture, flowingTexture) -> new FluidType(properties);
    }
}
