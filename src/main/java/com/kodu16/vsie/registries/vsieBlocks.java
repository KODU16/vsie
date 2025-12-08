package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.shield.ShieldGeneratorBlock;
import com.kodu16.vsie.content.turret.block.MediumLaserTurretBlock;
import com.kodu16.vsie.content.turret.block.MediumLaserTurretBlockEntity;
import com.kodu16.vsie.vsie;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlock;
import com.kodu16.vsie.content.thruster.block.basicthruster.BasicThrusterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraftforge.eventbus.api.IEventBus;


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

}
