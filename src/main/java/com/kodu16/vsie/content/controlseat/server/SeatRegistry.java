package com.kodu16.vsie.content.controlseat.server;

import net.minecraft.core.BlockPos;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SeatRegistry {
    public static final Map<UUID, BlockPos> SEAT_TO_CONTROLSEAT = new ConcurrentHashMap<>();
}