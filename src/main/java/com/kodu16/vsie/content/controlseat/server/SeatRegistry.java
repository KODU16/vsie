package com.kodu16.vsie.content.controlseat.server;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SeatRegistry {
    public static final Map<UUID, BlockPos> SEAT_TO_CONTROLSEAT = new ConcurrentHashMap<>();
    public static final Map<ControlSeatKey, String> CONTROL_SEAT_TO_SUBLEVEL = new ConcurrentHashMap<>();
    public static final Map<String, ControlSeatKey> SUBLEVEL_TO_CONTROL_SEAT = new ConcurrentHashMap<>();

    public static boolean registerOrUpdateControlSeat(Level level, BlockPos pos, SubLevel subLevel) {
        if (level == null || pos == null || subLevel == null) {
            return true;
        }

        ControlSeatKey key = ControlSeatKey.of(level, pos);
        String subLevelId = stableSubLevelId(subLevel);
        ControlSeatKey owner = SUBLEVEL_TO_CONTROL_SEAT.get(subLevelId);
        if (owner != null && !owner.equals(key)) {
            if (isControlSeatStillPresent(level, owner)) {
                return false;
            }
            unregisterControlSeat(level, owner.pos());
        }

        String previousSubLevelId = CONTROL_SEAT_TO_SUBLEVEL.put(key, subLevelId);
        if (previousSubLevelId != null && !previousSubLevelId.equals(subLevelId)) {
            SUBLEVEL_TO_CONTROL_SEAT.remove(previousSubLevelId, key);
        }
        // Function: keep the reverse lookup authoritative for one-control-seat-per-sublevel checks.
        SUBLEVEL_TO_CONTROL_SEAT.put(subLevelId, key);
        return true;
    }

    public static void unregisterControlSeat(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }

        ControlSeatKey key = ControlSeatKey.of(level, pos);
        String subLevelId = CONTROL_SEAT_TO_SUBLEVEL.remove(key);
        if (subLevelId != null) {
            SUBLEVEL_TO_CONTROL_SEAT.remove(subLevelId, key);
        }
    }

    private static boolean isControlSeatStillPresent(Level level, ControlSeatKey key) {
        if (!level.dimension().location().equals(key.dimension())) {
            return true;
        }
        if (!level.isLoaded(key.pos())) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(key.pos());
        return blockEntity instanceof AbstractControlSeatBlockEntity;
    }

    private static String stableSubLevelId(SubLevel subLevel) {
        UUID uniqueId = subLevel.getUniqueId();
        if (uniqueId != null) {
            return uniqueId.toString();
        }
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            return String.valueOf(serverSubLevel.getRuntimeId());
        }
        return String.valueOf(System.identityHashCode(subLevel));
    }

    public record ControlSeatKey(ResourceLocation dimension, BlockPos pos) {
        public static ControlSeatKey of(Level level, BlockPos pos) {
            // Function: include the level dimension so equal block coordinates in different worlds do not collide.
            return new ControlSeatKey(level.dimension().location(), pos.immutable());
        }
    }
}
