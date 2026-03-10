package com.kodu16.vsie.content.thruster;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.joml.Matrix3d;

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

            //以下仅用于矢量推进器
            //对于矢量推进器：direction本身是Y轴，模型红块的一边是X轴，蓝块是Z轴
            //MC是右手系
            data.setDirectionY(VectorConversionsMCKt.toJOMLD(state.getValue(FACING).getOpposite().getNormal()));

            LOGGER.warn(String.valueOf(Component.literal("thruster facing(Y):"+FACING)));
            Direction facing = state.getValue(FACING); // 获取当前方块的朝向

            Matrix3d modelCoordAxis = switch (facing){ // AbstractVectorThrusterGeoRenderer
                case DOWN -> new Matrix3d(
                        -1,0,0,
                        0,1,0,
                        0,0,1);     // 基准
                case UP -> new Matrix3d(
                        1,0,0,
                        0,-1,0,
                        0,0,1);     // 沿+Z旋转180度
                case EAST -> new Matrix3d(
                        0,-1,0,
                        -1,0,0,
                        0,0,1);    // 沿+Z旋转90度
                case WEST -> new Matrix3d(
                        0,1,0,
                        1,0,0,
                        0,0,1);    // 沿+Z旋转-90度
                case SOUTH -> new Matrix3d(
                        -1,0,0,
                        0,0,1,
                        0,-1,0);    // 沿+X旋转-90度
                case NORTH -> new Matrix3d(
                        -1,0,0,
                        0,0,-1,
                        0,1,0);     // 沿+X旋转90度
            };
            data.setCoordAxis(modelCoordAxis);
        }
    }
}
