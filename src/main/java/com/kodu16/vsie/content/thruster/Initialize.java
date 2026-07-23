package com.kodu16.vsie.content.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.joml.Matrix3d;
import org.joml.Vector3d;

public class Initialize {
    public static Vector3d toVector3d(Direction direction) {
        return new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    public static void initialize(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        final DirectionProperty FACING = BlockStateProperties.FACING;
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof AbstractThrusterBlockEntity thrusterBlockEntity) {
            ThrusterData data = thrusterBlockEntity.getData();
            data.setDirectionY(toVector3d(state.getValue(FACING).getOpposite()));

            Direction facing = state.getValue(FACING);
            // Function: map Minecraft block facing into the vector-thruster model coordinate basis.
            Matrix3d modelCoordAxis = switch (facing){
                case DOWN -> new Matrix3d(
                        -1,0,0,
                        0,1,0,
                        0,0,1);
                case UP -> new Matrix3d(
                        1,0,0,
                        0,-1,0,
                        0,0,1);
                case EAST -> new Matrix3d(
                        0,-1,0,
                        -1,0,0,
                        0,0,1);
                case WEST -> new Matrix3d(
                        0,1,0,
                        1,0,0,
                        0,0,1);
                case SOUTH -> new Matrix3d(
                        -1,0,0,
                        0,0,1,
                        0,-1,0);
                case NORTH -> new Matrix3d(
                        -1,0,0,
                        0,0,-1,
                        0,1,0);
            };
            data.setCoordAxis(modelCoordAxis);
        }
    }
}
