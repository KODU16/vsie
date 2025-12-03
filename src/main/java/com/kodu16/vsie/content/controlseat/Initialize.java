package com.kodu16.vsie.content.controlseat;

import com.kodu16.vsie.content.controlseat.server.ControlSeatForceAttachment;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.controlseat.server.ServerShipHandler;
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
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class Initialize {
    public static void initialize(Level level, BlockPos pos, BlockState state) {
        DirectionProperty FACING = BlockStateProperties.FACING;
        if (level.isClientSide()) return;
        Logger LOGGER = LogUtils.getLogger();
        ControlSeatForceAttachment ship = ControlSeatForceAttachment.get(level, pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractControlSeatBlockEntity controlseatBlockEntity) {
            LOGGER.warn(String.valueOf(Component.literal("blockentity of controlseat detected")));
            if (ship != null) {
                LOGGER.warn(String.valueOf(Component.literal("not null ship detected")));
                // Initialize data for Valkyrien Skies
                Vec3i rightVector;  // 默认右侧向量为零
                ControlSeatServerData data = controlseatBlockEntity.getControlSeatData();
                data.setTorque(new Vector3d(0, 0, 0));
                data.setDirectionForward(VectorConversionsMCKt.toJOMLD(state.getValue(FACING).getNormal()));
                data.setDirectionUp(VectorConversionsMCKt.toJOMLD(new Vec3i(0, 1, 0)));

                // 修正方向
                Direction facing = state.getValue(FACING); // 获取当前方块的朝向
                rightVector = switch (facing) {
                    case NORTH -> new Vec3i(1, 0, 0);   // 朝北，右侧为东（+X方向）
                    case SOUTH -> new Vec3i(-1, 0, 0);  // 朝南，右侧为西（-X方向）
                    case WEST -> new Vec3i(0, 0, -1);   // 朝西，右侧为南（-Z方向）
                    case EAST -> new Vec3i(0, 0, 1);    // 朝东，右侧为北（+Z方向）
                    default -> new Vec3i(1, 0, 0);      // 默认情况，右侧为东（+X方向）
                };
                LOGGER.warn(String.valueOf(Component.literal("facing:" + facing + "  right:" + rightVector)));
                data.setDirectionRight(VectorConversionsMCKt.toJOMLD(rightVector));
                ServerShipHandler applier = new ServerShipHandler(data);
                ship.addApplier(pos, applier);
            }
        }
    }
}

