package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.turret.block.MediumLaserTurretBlock;
import com.kodu16.vsie.content.turret.block.MediumLaserTurretBlockEntity;
import com.kodu16.vsie.vsie;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlock;
import com.kodu16.vsie.content.thruster.block.basicthruster.BasicThrusterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;


public class vsieBlocks {
    public static final CreateRegistrate REGISTRATE = vsie.registrate();
    public static void register() {} //Loads this class

    public static final BlockEntry<ControlSeatBlock> CONTROL_SEAT_BLOCK = REGISTRATE.block("control_seat", ControlSeatBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL))
            .properties(p -> p.requiresCorrectToolForDrops())
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(5.5f, 4.0f))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<BasicThrusterBlock> BASIC_THRUSTER_BLOCK = REGISTRATE.block("basic_thruster", BasicThrusterBlock::new)
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

    /*public static final BlockEntry<InlineOpticalSensorBlock> INLINE_OPTICAL_SENSOR_BLOCK = REGISTRATE.block("inline_optical_sensor", InlineOpticalSensorBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(1.5F, 1.0F))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<OpticalSensorBlock> OPTICAL_SENSOR_BLOCK = REGISTRATE.block("optical_sensor", OpticalSensorBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(2.5F, 2.0F))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<PhysicsAssemblerBlock> PHYSICS_ASSEMBLER_BLOCK = REGISTRATE.block("physics_assembler", PhysicsAssemblerBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(2.5F, 2.0F))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<LodestoneTrackerBlock> LODESTONE_TRACKER_BLOCK = REGISTRATE.block("lodestone_tracker", LodestoneTrackerBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(2.5F, 2.0F))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<RedstoneMagnetBlock> REDSTONE_MAGNET_BLOCK = REGISTRATE.block("redstone_magnet", RedstoneMagnetBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_RED))
            .properties(p -> p.sound(SoundType.METAL))
            .properties(p -> p.strength(2.5F, 2.0F))
            .properties(p -> p.noOcclusion())
            .simpleItem()
            .register();

    public static final BlockEntry<TiltSensorBlock> TILT_SENSOR_BLOCK = REGISTRATE.block("tilt_sensor", TiltSensorBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();*/
}
