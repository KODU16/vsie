package com.kodu16.vsie.content.controlseat.server;

import com.mojang.logging.LogUtils;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SeatRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LINK_TRACE_PREFIX = "[VSIE-LINK-TRACE]";
    public static final Map<UUID, BlockPos> SEAT_TO_CONTROLSEAT = new ConcurrentHashMap<>();
    public static final Map<ControlSeatKey, String> CONTROL_SEAT_TO_SUBLEVEL = new ConcurrentHashMap<>();
    public static final Map<String, ControlSeatKey> SUBLEVEL_TO_CONTROL_SEAT = new ConcurrentHashMap<>();

    public static boolean registerOrUpdateControlSeat(Level level, BlockPos pos, SubLevel subLevel) {
        if (level == null || pos == null || subLevel == null) {
            return true;
        }

        ControlSeatKey key = ControlSeatKey.of(level, pos);
        String subLevelId = stableSubLevelId(subLevel);
        // Function: ownership is scoped by Sable sublevel id, so separate ships in one host dimension remain independent.
        ControlSeatKey owner = SUBLEVEL_TO_CONTROL_SEAT.get(subLevelId);
        if (owner != null && !owner.equals(key)) {
            boolean sameHostDimension = owner.dimension().equals(key.dimension());
            boolean dimensionHandoff = !sameHostDimension;
            if (sameHostDimension && isControlSeatStillPresent(level, owner, subLevel)) {
                LOGGER.warn("{} phase=REGISTRY_DUPLICATE_REJECTED subLevel={} owner={} candidate={}",
                        LINK_TRACE_PREFIX, subLevelId, owner, key);
                return false;
            }
            // Function: the same Sable sublevel UUID may move between host dimensions during recreation.
            unregisterControlSeat(owner);
            if (dimensionHandoff) {
                LOGGER.info("{} phase=REGISTRY_DIMENSION_HANDOFF subLevel={} from={} to={}",
                        LINK_TRACE_PREFIX, subLevelId, owner, key);
            }
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

        unregisterControlSeat(ControlSeatKey.of(level, pos));
    }

    public static @Nullable ControlSeatBlockEntity resolveControlSeat(ServerSubLevel subLevel) {
        ServerLevel level = subLevel.getLevel();
        ControlSeatKey key = SUBLEVEL_TO_CONTROL_SEAT.get(stableSubLevelId(subLevel));
        if (key == null || !level.dimension().location().equals(key.dimension())) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(key.pos());
        return blockEntity instanceof ControlSeatBlockEntity controlSeat ? controlSeat : null;
    }

    private static void unregisterControlSeat(ControlSeatKey key) {
        String subLevelId = CONTROL_SEAT_TO_SUBLEVEL.remove(key);
        if (subLevelId != null) {
            SUBLEVEL_TO_CONTROL_SEAT.remove(subLevelId, key);
        }
    }

    private static boolean isControlSeatStillPresent(Level level, ControlSeatKey key, SubLevel expectedSubLevel) {
        if (!level.dimension().location().equals(key.dimension())) {
            return false;
        }
        SubLevel ownerSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, key.pos());
        if (ownerSubLevel == null || !stableSubLevelId(ownerSubLevel).equals(stableSubLevelId(expectedSubLevel))) {
            // Function: a plot relocation makes the old coordinates stale even if their chunk is not currently loaded.
            return false;
        }
        if (!level.isLoaded(key.pos())) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(key.pos());
        return blockEntity instanceof ControlSeatBlockEntity;
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
