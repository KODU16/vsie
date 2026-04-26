package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.shield.ShieldGeneratorBlockEntity;
import com.kodu16.vsie.content.storage.ammobox.AmmoBoxBlockEntity;
import com.kodu16.vsie.content.storage.energybattery.AbstractEnergyBatteryBlockEntity;
import com.kodu16.vsie.content.storage.fueltank.AbstractFuelTankBlockEntity;
import com.kodu16.vsie.content.turret.block.ParticleTurretBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class vsieCapabilities {

    // 功能：集中注册 NeoForge 1.21.1 方块实体 capability，替代旧版 getCapability/LazyOptional 暴露方式。
    public static void register(RegisterCapabilitiesEvent event) {
        // 功能：为弹药箱注册物品处理能力，支持漏斗与物流系统访问库存。
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, vsieBlockEntities.AMMO_BOX_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());

        // 功能：为粒子炮注册物品处理能力，供自动化输入 particle_container。
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, vsieBlockEntities.PARTICLE_TURRET_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());

        // 功能：为电磁轨核心注册物品处理能力，用于自动化补充轨道方块。
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, vsieBlockEntities.ELECTRO_MAGNET_RAIL_CORE_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());

        // 功能：为燃料罐注册流体处理能力，供桶和流体管道抽插燃料。
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, vsieBlockEntities.SMALL_FUELTANK_BLOCK_ENTITY.get(),
                (blockEntity, side) -> getFuelHandler(blockEntity));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, vsieBlockEntities.MEDIUM_FUELTANK_BLOCK_ENTITY.get(),
                (blockEntity, side) -> getFuelHandler(blockEntity));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, vsieBlockEntities.LARGE_FUELTANK_BLOCK_ENTITY.get(),
                (blockEntity, side) -> getFuelHandler(blockEntity));

        // 功能：为护盾发生器与电池组注册能源能力，供电网读写 FE。
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, vsieBlockEntities.SHIELD_GENERATOR_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyCapability());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, vsieBlockEntities.SMALL_ENERGY_BATTERY_BLOCK_ENTITY.get(),
                (blockEntity, side) -> getEnergyHandler(blockEntity));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, vsieBlockEntities.MEDIUM_ENERGY_BATTERY_BLOCK_ENTITY.get(),
                (blockEntity, side) -> getEnergyHandler(blockEntity));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, vsieBlockEntities.LARGE_ENERGY_BATTERY_BLOCK_ENTITY.get(),
                (blockEntity, side) -> getEnergyHandler(blockEntity));
    }

    // 功能：统一抽象燃料罐 capability 提供逻辑，避免重复泛型推断代码。
    private static net.neoforged.neoforge.fluids.capability.IFluidHandler getFuelHandler(AbstractFuelTankBlockEntity blockEntity) {
        return blockEntity.getFluidHandler();
    }

    // 功能：统一抽象电池 capability 提供逻辑，避免重复泛型推断代码。
    private static net.neoforged.neoforge.energy.IEnergyStorage getEnergyHandler(AbstractEnergyBatteryBlockEntity blockEntity) {
        return blockEntity.getEnergyCapability();
    }
}
