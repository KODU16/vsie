package com.kodu16.vsie.content.turret;

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
    public static void initialize(
            Level level,
            BlockPos pos,
            BlockState state,
            Vector3d modelPivotPoint
    ) {
        if (level.isClientSide()) { return; }

        final DirectionProperty FACING = BlockStateProperties.FACING;
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof AbstractTurretBlockEntity turretBlockEntity) {

            TurretProperty property = turretBlockEntity.getProperty();
            Direction facing = state.getValue(FACING); // 获取当前方块的朝向

            Matrix3d modelCoordAxis = switch (facing){
                case DOWN -> new Matrix3d(
                        1,0,0,
                        0,1,0,
                        0,0,1);     // 基准
                case UP -> new Matrix3d(
                        -1,0,0,
                        0,-1,0,
                        0,0,1);     // 沿+Z旋转180度
                case EAST -> new Matrix3d(
                        0,-1,0,
                        1,0,0,
                        0,0,1);    // 沿+Z旋转90度
                case WEST -> new Matrix3d(
                        0,1,0,
                        -1,0,0,
                        0,0,1);    // 沿+Z旋转-90度
                case SOUTH -> new Matrix3d(
                        1,0,0,
                        0,0,1,
                        0,-1,0);    // 沿+X旋转-90度
                case NORTH -> new Matrix3d(
                        1,0,0,
                        0,0,-1,
                        0,1,0);     // 沿+X旋转90度
            };

            property.setCoordAxis(modelCoordAxis);
            property.setBasePivotOffset(modelCoordAxis.transpose().transform(modelPivotPoint)); //左乘坐标基的逆 正交矩阵中M^T=M^-1
        }
    }
}
