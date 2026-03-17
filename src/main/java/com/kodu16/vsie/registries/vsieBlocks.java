package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.misc.electromagnet_rail.ElectroMagnetRailBlock;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreBlock;
import com.kodu16.vsie.content.turret.ciws.basicciws.BasicCIWSBlock;
import com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret.HeavyElectroMagnetTurretBlock;
import com.kodu16.vsie.content.screen.block.BasicScreenBlock;
import com.kodu16.vsie.content.shield.ShieldGeneratorBlock;
import com.kodu16.vsie.content.storage.ammobox.AmmoBoxBlock;
import com.kodu16.vsie.content.storage.energybattery.block.LargeEnergyBatteryBlock;
import com.kodu16.vsie.content.storage.energybattery.block.MediumEnergyBatteryBlock;
import com.kodu16.vsie.content.storage.energybattery.block.SmallEnergyBatteryBlock;
import com.kodu16.vsie.content.storage.fueltank.block.LargeFuelTankBlock;
import com.kodu16.vsie.content.storage.fueltank.block.MediumFuelTankBlock;
import com.kodu16.vsie.content.storage.fueltank.block.SmallFuelTankBlock;
import com.kodu16.vsie.content.thruster.block.LargeThrusterBlock;
import com.kodu16.vsie.content.thruster.block.MediumThrusterBlock;
import com.kodu16.vsie.content.turret.block.MediumLaserTurretBlock;
import com.kodu16.vsie.content.turret.block.ParticleTurretBlock;
import com.kodu16.vsie.content.vectorthruster.block.BasicVectorThrusterBlock;
import com.kodu16.vsie.content.weapon.arc_emitter.ArcEmitterBlock;
import com.kodu16.vsie.content.weapon.cenix_plasma_cannon.CenixPlasmaCannonBlock;
import com.kodu16.vsie.content.weapon.infra_knife_accelerator.InfraKnifeAcceleratorBlock;
import com.kodu16.vsie.content.weapon.missile_launcher.block.BasicMissileLauncherBlock;
import com.kodu16.vsie.vsie;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlock;
import com.kodu16.vsie.content.thruster.block.BasicThrusterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;


public class vsieBlocks {
    public static final CreateRegistrate REGISTRATE = vsie.registrate();
    public static void register() {} //Loads this class

    public static final BlockEntry<BasicThrusterBlock> BASIC_THRUSTER_BLOCK = REGISTRATE.block("basic_thruster", BasicThrusterBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<ControlSeatBlock> CONTROL_SEAT_BLOCK = REGISTRATE.block("control_seat", ControlSeatBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<MediumLaserTurretBlock> MEDIUM_LASER_TURRET_BLOCK = REGISTRATE.block("medium_laser_turret", MediumLaserTurretBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<ShieldGeneratorBlock> SHIELD_GENERATOR_BLOCK = REGISTRATE.block("shield_generator", ShieldGeneratorBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<BasicVectorThrusterBlock> BASIC_VECTOR_THRUSTER_BLOCK = REGISTRATE.block("basic_vector_thruster", BasicVectorThrusterBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<InfraKnifeAcceleratorBlock> INFRA_KNIFE_ACCELERATOR_BLOCK = REGISTRATE.block("infra_knife_accelerator", InfraKnifeAcceleratorBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<BasicMissileLauncherBlock> BASIC_MISSILE_LAUNCHER_BLOCK = REGISTRATE.block("basic_missile_launcher", BasicMissileLauncherBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<CenixPlasmaCannonBlock> CENIX_PLASMA_CANNON_BLOCK = REGISTRATE.block("cenix_plasma_cannon", CenixPlasmaCannonBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<ArcEmitterBlock> ARC_EMITTER_BLOCK = REGISTRATE.block("arc_emitter", ArcEmitterBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<MediumThrusterBlock> MEDIUM_THRUSTER_BLOCK = REGISTRATE.block("medium_thruster", MediumThrusterBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<LargeThrusterBlock> LARGE_THRUSTER_BLOCK = REGISTRATE.block("large_thruster", LargeThrusterBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<ParticleTurretBlock> PARTICLE_TURRET_BLOCK = REGISTRATE.block("particle_turret", ParticleTurretBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<HeavyElectroMagnetTurretBlock> HEAVY_ELECTROMAGNET_TURRET_BLOCK = REGISTRATE.block("heavy_electromagnet_turret", HeavyElectroMagnetTurretBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<BasicCIWSBlock> BASIC_CIWS_BLOCK = REGISTRATE.block("basic_ciws", BasicCIWSBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<SmallEnergyBatteryBlock> SMALL_ENERGY_BATTERY_BLOCK = REGISTRATE.block("small_energy_battery", SmallEnergyBatteryBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<MediumEnergyBatteryBlock> MEDIUM_ENERGY_BATTERY_BLOCK = REGISTRATE.block("medium_energy_battery", MediumEnergyBatteryBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<LargeEnergyBatteryBlock> LARGE_ENERGY_BATTERY_BLOCK = REGISTRATE.block("large_energy_battery", LargeEnergyBatteryBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<SmallFuelTankBlock> SMALL_FUELTANK_BLOCK = REGISTRATE.block("small_fueltank", SmallFuelTankBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<MediumFuelTankBlock> MEDIUM_FUELTANK_BLOCK = REGISTRATE.block("medium_fueltank", MediumFuelTankBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<LargeFuelTankBlock> LARGE_FUELTANK_BLOCK = REGISTRATE.block("large_fueltank", LargeFuelTankBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<BasicScreenBlock> BASIC_SCREEN_BLOCK = REGISTRATE.block("basic_screen", BasicScreenBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<AmmoBoxBlock> AMMO_BOX_BLOCK = REGISTRATE.block("ammo_box", AmmoBoxBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<ElectroMagnetRailCoreBlock> ELECTRO_MAGNET_RAIL_CORE_BLOCK = REGISTRATE.block("electro_magnet_rail_core", ElectroMagnetRailCoreBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();
    public static final BlockEntry<ElectroMagnetRailBlock> ELECTRO_MAGNET_RAIL_BLOCK = REGISTRATE.block("electro_magnet_rail", ElectroMagnetRailBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

}
