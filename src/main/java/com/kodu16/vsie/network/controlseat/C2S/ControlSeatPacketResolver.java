package com.kodu16.vsie.network.controlseat.C2S;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.UUID;

final class ControlSeatPacketResolver {

    private ControlSeatPacketResolver() {
    }

    static ControlSeatBlockEntity resolve(ServerPlayer sender, BlockPos pos, UUID seatEntityId) {
        if (!(sender.getVehicle() instanceof ControlSeatMountEntity mount)
                || !mount.getBoundBlockPos().equals(pos)
                || !mount.getUUID().equals(seatEntityId)) {
            return null;
        }

        if (mount.level() instanceof ServerLevel mountLevel) {
            ControlSeatBlockEntity controlSeat = findControlSeat(mountLevel, pos);
            if (controlSeat != null) {
                return controlSeat;
            }
        }

        // Function: after confirming the currently ridden mount, tolerate the warp tick where mount and block level disagree.
        for (ServerLevel level : sender.server.getAllLevels()) {
            ControlSeatBlockEntity controlSeat = findControlSeat(level, pos);
            if (controlSeat != null) {
                return controlSeat;
            }
        }
        return null;
    }

    private static ControlSeatBlockEntity findControlSeat(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ControlSeatBlockEntity controlSeat ? controlSeat : null;
    }
}
