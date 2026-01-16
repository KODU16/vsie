package com.kodu16.vsie.content.thruster;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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

            //设置左手系朝向
            //对于矢量推进器：direction本身是Y轴，模型铰链对的一边是X轴，另一边是Z轴
            data.setDirection(VectorConversionsMCKt.toJOMLD(state.getValue(FACING).getNormal()));//Y

            LOGGER.warn(String.valueOf(Component.literal("thruster facing(Y):"+FACING)));
            Direction facing = state.getValue(FACING); // 获取当前方块的朝向
            Vec3i XVector;
            XVector = switch (facing) {
                case NORTH -> new Vec3i(1, 0, 0);   // 朝北，模型正X为东
                case SOUTH -> new Vec3i(-1, 0, 0);  // 朝南，模型正X为西
                case WEST -> new Vec3i(0, -1, 0);   // 朝西，模型正X为下
                case EAST -> new Vec3i(0, 1, 0);    // 朝东，模型正X为上
                case UP -> new Vec3i(-1,0,0); //朝上，模型正X为西
                case DOWN -> new Vec3i(1,0,0); //朝下，模型正X为东
            };
            data.setDirectionX(VectorConversionsMCKt.toJOMLD(XVector));

            Vec3i ZVector;
            ZVector = switch (facing) {
                case NORTH -> new Vec3i(0, -1, 0);   // 朝北，模型正Z为下
                case SOUTH -> new Vec3i(0, 1, 0);  // 朝南，模型正Z为上
                case WEST -> new Vec3i(0, 0, -1);   // 朝西，模型正Z为南
                case EAST -> new Vec3i(0, 0, 1);    // 朝东，模型正Z为北
                case UP -> new Vec3i(0,0,-1); //朝上，模型正Z为南
                case DOWN -> new Vec3i(0,0,-1); //朝下，模型正X为南（你问我为啥？那我也不知道）
            };
            data.setDirectionZ(VectorConversionsMCKt.toJOMLD(ZVector));
        }
    }
}
