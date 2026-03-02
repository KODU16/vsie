package com.kodu16.vsie.registries;

import javax.annotation.Nonnull;

import com.kodu16.vsie.vsie;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.CreativeModeTab.Output;

@EventBusSubscriber(bus = Bus.MOD)
public class vsieCreativeTab {
    private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, vsie.ID);

    public static final RegistryObject<CreativeModeTab> BASE_TAB = REGISTER.register("base",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.vsie.base"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> vsieBlocks.CONTROL_SEAT_BLOCK.asStack())
                    .displayItems(new RegistrateDisplayItemsGenerator())
                    .build());

    public static void register(IEventBus modEventBus){
        REGISTER.register(modEventBus);
    }

    private static class RegistrateDisplayItemsGenerator implements DisplayItemsGenerator {
        public RegistrateDisplayItemsGenerator() {}

        @Override
        public void accept(@Nonnull ItemDisplayParameters parameters, @Nonnull Output output) {
            output.accept(vsieItems.TEST_ITEM);
            output.accept(vsieItems.LINKER);
            output.accept(vsieItems.IFF);
            output.accept(vsieItems.SHIELD_TOOL);
            output.accept(vsieBlocks.BASIC_SCREEN_BLOCK);
            output.accept(vsieFluids.DTFUEL.getBucket().get());
            output.accept(vsieBlocks.CONTROL_SEAT_BLOCK);
            output.accept(vsieBlocks.BASIC_THRUSTER_BLOCK);
            output.accept(vsieBlocks.MEDIUM_THRUSTER_BLOCK);
            output.accept(vsieBlocks.LARGE_THRUSTER_BLOCK);
            output.accept(vsieBlocks.MEDIUM_LASER_TURRET_BLOCK);
            output.accept(vsieBlocks.PARTICLE_TURRET_BLOCK);
            output.accept(vsieBlocks.SHIELD_GENERATOR_BLOCK);
            output.accept(vsieBlocks.BASIC_VECTOR_THRUSTER_BLOCK);
            output.accept(vsieBlocks.INFRA_KNIFE_ACCELERATOR_BLOCK);
            output.accept(vsieBlocks.BASIC_MISSILE_LAUNCHER_BLOCK);
            output.accept(vsieBlocks.ARC_EMITTER_BLOCK);
            output.accept(vsieBlocks.SMALL_ENERGY_BATTERY_BLOCK);
            output.accept(vsieBlocks.MEDIUM_ENERGY_BATTERY_BLOCK);
            output.accept(vsieBlocks.LARGE_ENERGY_BATTERY_BLOCK);
            output.accept(vsieBlocks.SMALL_FUELTANK_BLOCK);
            output.accept(vsieBlocks.MEDIUM_FUELTANK_BLOCK);
            output.accept(vsieBlocks.LARGE_FUELTANK_BLOCK);
        }
    }
}
