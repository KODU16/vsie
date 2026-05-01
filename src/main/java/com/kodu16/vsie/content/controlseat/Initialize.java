package com.kodu16.vsie.content.controlseat;

import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.joml.Vector3d;

public class Initialize {
    public static void initialize(Level level, BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AbstractControlSeatBlockEntity controlseatBlockEntity)) {
            return;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, pos);
        if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
            return;
        }

        DirectionProperty facingProperty = BlockStateProperties.FACING;
        Direction facing = state.hasProperty(facingProperty) ? state.getValue(facingProperty) : Direction.NORTH;

        ControlSeatServerData data = controlseatBlockEntity.getControlSeatData();
        data.serverShip = serverSubLevel;
        data.level = level;
        data.setTorque(new Vector3d(0, 0, 0));
        data.setDirectionForward(facing.getNormal());
        data.setDirectionUp(new Vec3i(0, 1, 0));
        data.setDirectionRight(getRightVector(facing));
    }

    private static Vec3i getRightVector(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3i(1, 0, 0);
            case SOUTH -> new Vec3i(-1, 0, 0);
            case WEST -> new Vec3i(0, 0, -1);
            case EAST -> new Vec3i(0, 0, 1);
            default -> new Vec3i(1, 0, 0);
        };
    }
}
