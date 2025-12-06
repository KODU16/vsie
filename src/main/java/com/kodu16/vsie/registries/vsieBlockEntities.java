package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.shield.ShieldGeneratorBlockEntity;
import com.kodu16.vsie.content.thruster.block.basicthruster.BasicThrusterBlockEntity;
import com.kodu16.vsie.content.turret.block.MediumLaserTurretBlockEntity;
import com.kodu16.vsie.content.thruster.trailflame.ThrusterFlameRenderer;
import com.kodu16.vsie.content.turret.client.AbstractTurretGeoRenderer;
import com.kodu16.vsie.vsie;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

public class vsieBlockEntities {
    public static final CreateRegistrate REGISTRATE = vsie.registrate();
    public static void register() {} //Loads this class
    static Logger LOGGER = LogUtils.getLogger();

    public static final BlockEntityEntry<ControlSeatBlockEntity> CONTROL_SEAT_BLOCK_ENTITY =
            REGISTRATE.blockEntity("control_seat_block_entity", ControlSeatBlockEntity::new)
                    .validBlocks(vsieBlocks.CONTROL_SEAT_BLOCK)
                    .register();
    public static final BlockEntityEntry<BasicThrusterBlockEntity> BASIC_THRUSTER_BLOCK_ENTITY =
            REGISTRATE.blockEntity("thruster_block_entity", BasicThrusterBlockEntity::new)
                    .validBlocks(vsieBlocks.BASIC_THRUSTER_BLOCK)
                    .renderer(() -> ThrusterFlameRenderer::new)
                    .register();
    public static final BlockEntityEntry<MediumLaserTurretBlockEntity> MEDIUM_LASER_TURRET_BLOCK_ENTITY =
            REGISTRATE.blockEntity("medium_laser_turret_entity", MediumLaserTurretBlockEntity::new)
                    .validBlocks(vsieBlocks.MEDIUM_LASER_TURRET_BLOCK)
                    .onRegister(be -> LOGGER.info("Medium Laser Turret BlockEntity registered!"))
                    .renderer(() -> AbstractTurretGeoRenderer::new)
                    .register();
    public static final BlockEntityEntry<ShieldGeneratorBlockEntity> SHIELD_GENERATOR_BLOCK_ENTITY =
            REGISTRATE.blockEntity("shield_generator_block_entity", ShieldGeneratorBlockEntity::new)
                    .validBlocks(vsieBlocks.SHIELD_GENERATOR_BLOCK)
                    .register();
}
