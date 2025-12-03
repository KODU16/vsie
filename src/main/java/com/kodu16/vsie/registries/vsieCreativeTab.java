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
            /*//From 0.1
            output.accept(PropulsionBlocks.INLINE_OPTICAL_SENSOR_BLOCK);
            output.accept(PropulsionBlocks.OPTICAL_SENSOR_BLOCK);
            output.accept(PropulsionBlocks.THRUSTER_BLOCK);
            //From 0.2
            output.accept(PropulsionBlocks.REDSTONE_MAGNET_BLOCK);
            output.accept(PropulsionBlocks.PHYSICS_ASSEMBLER_BLOCK);
            //From 0.2 (items)
            output.accept(PropulsionItems.ASSEMBLY_GAUGE);
            output.accept(PropulsionFluids.TURPENTINE.getBucket().get());
            output.accept(PropulsionItems.PINE_RESIN);
            output.accept(PropulsionItems.OPTICAL_LENS);
            output.accept(PropulsionItems.FLUID_LENS);
            output.accept(PropulsionItems.FOCUS_LENS);*/
            output.accept(vsieItems.TEST_ITEM);
            output.accept(vsieBlocks.CONTROL_SEAT_BLOCK);
            output.accept(vsieBlocks.BASIC_THRUSTER_BLOCK);
            output.accept(vsieBlocks.MEDIUM_LASER_TURRET_BLOCK);
        }
    }
}
