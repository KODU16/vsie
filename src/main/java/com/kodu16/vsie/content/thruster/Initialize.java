package com.kodu16.vsie.content.thruster;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import org.slf4j.Logger;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class Initialize {
    public static void initialize(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        Logger LOGGER = LogUtils.getLogger();
        LOGGER.warn(String.valueOf(Component.literal("onPlace called, detecting!")));
        final DirectionProperty FACING = BlockStateProperties.FACING;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractThrusterBlockEntity thrusterBlockEntity) {
            LOGGER.warn(String.valueOf(Component.literal("blockentity of thruster detected")));
            LOGGER.warn(String.valueOf(Component.literal("not null ship detected")));
            ThrusterData data = thrusterBlockEntity.getData();
            data.setDirection(VectorConversionsMCKt.toJOMLD(state.getValue(FACING).getNormal()));
            LOGGER.warn(String.valueOf(Component.literal("thruster facing:"+FACING)));
        }
    }
}
