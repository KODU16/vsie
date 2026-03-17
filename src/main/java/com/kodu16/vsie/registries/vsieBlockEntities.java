package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.controlseat.client.AbstractControlSeatGeoRenderer;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreGeoRenderer;
import com.kodu16.vsie.content.turret.ciws.basicciws.BasicCIWSBlockEntity;
import com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret.HeavyElectroMagnetTurretBlockEntity;
import com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret.HeavyElectroMagnetTurretGeoRenderer;
import com.kodu16.vsie.content.screen.client.AbstractScreenGeoRenderer;
import com.kodu16.vsie.content.screen.block.BasicScreenBlockEntity;
import com.kodu16.vsie.content.shield.ShieldGeneratorBlockEntity;
import com.kodu16.vsie.content.storage.ammobox.AmmoBoxBlockEntity;
import com.kodu16.vsie.content.storage.energybattery.AbstractEnergyBatteryGeoRenderer;
import com.kodu16.vsie.content.storage.energybattery.block.LargeEnergyBatteryBlockEntity;
import com.kodu16.vsie.content.storage.energybattery.block.MediumEnergyBatteryBlockEntity;
import com.kodu16.vsie.content.storage.energybattery.block.SmallEnergyBatteryBlockEntity;
import com.kodu16.vsie.content.storage.fueltank.AbstractFuelTankGeoRenderer;
import com.kodu16.vsie.content.storage.fueltank.block.LargeFuelTankBlockEntity;
import com.kodu16.vsie.content.storage.fueltank.block.MediumFuelTankBlockEntity;
import com.kodu16.vsie.content.storage.fueltank.block.SmallFuelTankBlockEntity;
import com.kodu16.vsie.content.thruster.block.BasicThrusterBlockEntity;
import com.kodu16.vsie.content.thruster.block.LargeThrusterBlockEntity;
import com.kodu16.vsie.content.thruster.block.MediumThrusterBlockEntity;
import com.kodu16.vsie.content.thruster.client.AbstractThrusterGeoRenderer;
import com.kodu16.vsie.content.turret.block.MediumLaserTurretBlockEntity;
import com.kodu16.vsie.content.turret.block.ParticleTurretBlockEntity;
import com.kodu16.vsie.content.vectorthruster.block.BasicVectorThrusterBlockEntity;
import com.kodu16.vsie.content.vectorthruster.client.AbstractVectorThrusterGeoRenderer;
import com.kodu16.vsie.content.weapon.arc_emitter.ArcEmitterBlockEntity;
import com.kodu16.vsie.content.weapon.cenix_plasma_cannon.CenixPlasmaCannonBlockEntity;
import com.kodu16.vsie.content.weapon.client.AbstractWeaponGeoRenderer;
import com.kodu16.vsie.content.weapon.arc_emitter.ArcEmitterGeoRenderer;
import com.kodu16.vsie.content.weapon.infra_knife_accelerator.InfraKnifeAcceleratorBlockEntity;
import com.kodu16.vsie.content.turret.client.AbstractTurretGeoRenderer;
import com.kodu16.vsie.content.weapon.missile_launcher.block.BasicMissileLauncherBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.client.AbstractMissileLauncherGeoRenderer;

import com.kodu16.vsie.vsie;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class vsieBlockEntities {
    public static final CreateRegistrate REGISTRATE = vsie.registrate();
    public static void register() {} //Loads this class

    public static final BlockEntityEntry<BasicThrusterBlockEntity> BASIC_THRUSTER_BLOCK_ENTITY =
            REGISTRATE.blockEntity("basic_thruster_block_entity", BasicThrusterBlockEntity::new)
                    .validBlocks(vsieBlocks.BASIC_THRUSTER_BLOCK)
                    .renderer(() -> AbstractThrusterGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<ControlSeatBlockEntity> CONTROL_SEAT_BLOCK_ENTITY =
            REGISTRATE.blockEntity("control_seat_block_entity", ControlSeatBlockEntity::new)
                    .validBlocks(vsieBlocks.CONTROL_SEAT_BLOCK)
                    .renderer(() -> AbstractControlSeatGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<MediumLaserTurretBlockEntity> MEDIUM_LASER_TURRET_BLOCK_ENTITY =
            REGISTRATE.blockEntity("medium_laser_turret_block_entity", MediumLaserTurretBlockEntity::new)
                    .validBlocks(vsieBlocks.MEDIUM_LASER_TURRET_BLOCK)
                    //.onRegister(be -> LOGGER.info("Medium Laser Turret BlockEntity registered!"))
                    .renderer(() -> AbstractTurretGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<ShieldGeneratorBlockEntity> SHIELD_GENERATOR_BLOCK_ENTITY =
            REGISTRATE.blockEntity("shield_generator_block_entity", ShieldGeneratorBlockEntity::new)
                    .validBlocks(vsieBlocks.SHIELD_GENERATOR_BLOCK)
                    .register();
    public static final BlockEntityEntry<BasicVectorThrusterBlockEntity> BASIC_VECTOR_THRUSTER_BLOCK_ENTITY =
            REGISTRATE.blockEntity("basic_vector_thruster_block_entity", BasicVectorThrusterBlockEntity::new)
                    .validBlocks(vsieBlocks.BASIC_VECTOR_THRUSTER_BLOCK)
                    .renderer(() -> AbstractVectorThrusterGeoRenderer::new)
                    .register();


    public static final BlockEntityEntry<InfraKnifeAcceleratorBlockEntity> INFRA_KNIFE_ACCELERATOR_BLOCK_ENTITY =
            REGISTRATE.blockEntity("infra_knife_accelerator_block_entity", InfraKnifeAcceleratorBlockEntity::new)
                    .validBlocks(vsieBlocks.INFRA_KNIFE_ACCELERATOR_BLOCK)
                    .renderer(() -> AbstractWeaponGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<BasicMissileLauncherBlockEntity> BASIC_MISSILE_LAUNCHER_BLOCK_ENTITY =
            REGISTRATE.blockEntity("basic_missile_launcher_block_entity", BasicMissileLauncherBlockEntity::new)
                    .validBlocks(vsieBlocks.BASIC_MISSILE_LAUNCHER_BLOCK)
                    .renderer(() -> AbstractMissileLauncherGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<ArcEmitterBlockEntity> ARC_EMITTER_BLOCK_ENTITY =
            REGISTRATE.blockEntity("arc_emitter_block_entity", ArcEmitterBlockEntity::new)
                    .validBlocks(vsieBlocks.ARC_EMITTER_BLOCK)
                    .renderer(() -> ArcEmitterGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<CenixPlasmaCannonBlockEntity> CENIX_PLASMA_CANNON_BLOCK_ENTITY =
            REGISTRATE.blockEntity("cenix_plasma_cannon_block_entity", CenixPlasmaCannonBlockEntity::new)
                    .validBlocks(vsieBlocks.CENIX_PLASMA_CANNON_BLOCK)
                    .renderer(() -> AbstractWeaponGeoRenderer::new)
                    .register();


    public static final BlockEntityEntry<MediumThrusterBlockEntity> MEDIUM_THRUSTER_BLOCK_ENTITY =
            REGISTRATE.blockEntity("medium_thruster_block_entity", MediumThrusterBlockEntity::new)
                    .validBlocks(vsieBlocks.MEDIUM_THRUSTER_BLOCK)
                    .renderer(() -> AbstractThrusterGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<LargeThrusterBlockEntity> LARGE_THRUSTER_BLOCK_ENTITY =
            REGISTRATE.blockEntity("large_thruster_block_entity", LargeThrusterBlockEntity::new)
                    .validBlocks(vsieBlocks.LARGE_THRUSTER_BLOCK)
                    .renderer(() -> AbstractThrusterGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<ParticleTurretBlockEntity> PARTICLE_TURRET_BLOCK_ENTITY =
            REGISTRATE.blockEntity("particle_turret_block_entity", ParticleTurretBlockEntity::new)
                    .validBlocks(vsieBlocks.PARTICLE_TURRET_BLOCK)
                    .renderer(() -> AbstractTurretGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<HeavyElectroMagnetTurretBlockEntity> HEAVY_ELECTROMAGNET_TURRET_BLOCK_ENTITY =
            REGISTRATE.blockEntity("heavy_electromagnet_turret_block_entity", HeavyElectroMagnetTurretBlockEntity::new)
                    .validBlocks(vsieBlocks.HEAVY_ELECTROMAGNET_TURRET_BLOCK)
                    .renderer(() -> HeavyElectroMagnetTurretGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<BasicCIWSBlockEntity> BASIC_CIWS_BLOCK_ENTITY =
            REGISTRATE.blockEntity("basic_ciws_block_entity", BasicCIWSBlockEntity::new)
                    .validBlocks(vsieBlocks.BASIC_CIWS_BLOCK)
                    .renderer(() -> AbstractTurretGeoRenderer::new)
                    .register();


    public static final BlockEntityEntry<SmallEnergyBatteryBlockEntity> SMALL_ENERGY_BATTERY_BLOCK_ENTITY =
            REGISTRATE.blockEntity("small_energy_battery_block_entity", SmallEnergyBatteryBlockEntity::new)
                    .validBlocks(vsieBlocks.SMALL_ENERGY_BATTERY_BLOCK)
                    .renderer(() -> AbstractEnergyBatteryGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<MediumEnergyBatteryBlockEntity> MEDIUM_ENERGY_BATTERY_BLOCK_ENTITY =
            REGISTRATE.blockEntity("medium_energy_battery_block_entity", MediumEnergyBatteryBlockEntity::new)
                    .validBlocks(vsieBlocks.MEDIUM_ENERGY_BATTERY_BLOCK)
                    .renderer(() -> AbstractEnergyBatteryGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<LargeEnergyBatteryBlockEntity> LARGE_ENERGY_BATTERY_BLOCK_ENTITY =
            REGISTRATE.blockEntity("large_energy_battery_block_entity", LargeEnergyBatteryBlockEntity::new)
                    .validBlocks(vsieBlocks.LARGE_ENERGY_BATTERY_BLOCK)
                    .renderer(() -> AbstractEnergyBatteryGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<SmallFuelTankBlockEntity> SMALL_FUELTANK_BLOCK_ENTITY =
            REGISTRATE.blockEntity("small_fueltank_block_entity", SmallFuelTankBlockEntity::new)
                    .validBlocks(vsieBlocks.SMALL_FUELTANK_BLOCK)
                    .renderer(() -> AbstractFuelTankGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<MediumFuelTankBlockEntity> MEDIUM_FUELTANK_BLOCK_ENTITY =
            REGISTRATE.blockEntity("medium_fueltank_block_entity", MediumFuelTankBlockEntity::new)
                    .validBlocks(vsieBlocks.MEDIUM_FUELTANK_BLOCK)
                    .renderer(() -> AbstractFuelTankGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<LargeFuelTankBlockEntity> LARGE_FUELTANK_BLOCK_ENTITY =
            REGISTRATE.blockEntity("large_fueltank_block_entity", LargeFuelTankBlockEntity::new)
                    .validBlocks(vsieBlocks.LARGE_FUELTANK_BLOCK)
                    .renderer(() -> AbstractFuelTankGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<BasicScreenBlockEntity> BASIC_SCREEN_BLOCK_ENTITY =
            REGISTRATE.blockEntity("basic_screen_block_entity", BasicScreenBlockEntity::new)
                    .validBlocks(vsieBlocks.BASIC_SCREEN_BLOCK)
                    .renderer(() -> AbstractScreenGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<AmmoBoxBlockEntity> AMMO_BOX_BLOCK_ENTITY =
            REGISTRATE.blockEntity("ammo_box_block_entity", AmmoBoxBlockEntity::new)
                    .validBlocks(vsieBlocks.AMMO_BOX_BLOCK)
                    .register();


    public static final BlockEntityEntry<ElectroMagnetRailCoreBlockEntity> ELECTRO_MAGNET_RAIL_CORE_BLOCK_ENTITY =
            REGISTRATE.blockEntity("electro_magnet_rail_core_block_entity", ElectroMagnetRailCoreBlockEntity::new)
                    .validBlocks(vsieBlocks.ELECTRO_MAGNET_RAIL_CORE_BLOCK)
                    .renderer(() -> ElectroMagnetRailCoreGeoRenderer::new)
                    .register();
}
