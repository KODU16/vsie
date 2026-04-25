package com.kodu16.vsie.registries;

import javax.annotation.Nonnull;

import com.kodu16.vsie.vsie;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.CreativeModeTab.Output;

@EventBusSubscriber(bus = Bus.MOD)
public class vsieCreativeTab {
    // 功能：使用 NeoForge DeferredRegister 注册 VSIE 创造标签页。
    private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, vsie.ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_TAB = REGISTER.register("base",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.vsie.base"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(vsieBlocks.CONTROL_SEAT_BLOCK::asStack)
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
            // 功能：将粒子炮弹药容器加入创造模式物品栏，便于测试和生存拿取。
            output.accept(vsieItems.PARTICLE_CONTAINER);
            output.accept(vsieBlocks.BASIC_SCREEN_BLOCK);
            output.accept(vsieFluids.DTFUEL.getBucket().get());
            output.accept(vsieBlocks.CONTROL_SEAT_BLOCK);

            output.accept(vsieBlocks.BASIC_THRUSTER_BLOCK);
            output.accept(vsieBlocks.MEDIUM_THRUSTER_BLOCK);
            output.accept(vsieBlocks.LARGE_THRUSTER_BLOCK);

            output.accept(vsieBlocks.MEDIUM_LASER_TURRET_BLOCK);
            output.accept(vsieBlocks.PARTICLE_TURRET_BLOCK);
            output.accept(vsieBlocks.HEAVY_ELECTROMAGNET_TURRET_BLOCK);
            output.accept(vsieBlocks.BASIC_CIWS_BLOCK);

            output.accept(vsieBlocks.SHIELD_GENERATOR_BLOCK);
            output.accept(vsieBlocks.AMMO_BOX_BLOCK);
            output.accept(vsieBlocks.BASIC_VECTOR_THRUSTER_BLOCK);

            output.accept(vsieBlocks.INFRA_KNIFE_ACCELERATOR_BLOCK);
            output.accept(vsieBlocks.BASIC_MISSILE_LAUNCHER_BLOCK);
            output.accept(vsieBlocks.ARC_EMITTER_BLOCK);
            output.accept(vsieBlocks.CENIX_PLASMA_CANNON_BLOCK);
            output.accept(vsieBlocks.ELECTRO_MAGNET_RAIL_CANNON_BLOCK);

            output.accept(vsieBlocks.ELECTRO_MAGNET_RAIL_TOP_BLOCK);
            output.accept(vsieBlocks.ELECTRO_MAGNET_RAIL_CORE_BLOCK);
            output.accept(vsieBlocks.ELECTRO_MAGNET_RAIL_BLOCK);

            output.accept(vsieBlocks.SMALL_ENERGY_BATTERY_BLOCK);
            output.accept(vsieBlocks.MEDIUM_ENERGY_BATTERY_BLOCK);
            output.accept(vsieBlocks.LARGE_ENERGY_BATTERY_BLOCK);
            output.accept(vsieBlocks.SMALL_FUELTANK_BLOCK);
            output.accept(vsieBlocks.MEDIUM_FUELTANK_BLOCK);
            output.accept(vsieBlocks.LARGE_FUELTANK_BLOCK);
        }
    }
}
