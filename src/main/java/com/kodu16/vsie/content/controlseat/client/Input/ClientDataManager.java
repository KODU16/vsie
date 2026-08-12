package com.kodu16.vsie.content.controlseat.client.Input;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("removal")
public class ClientDataManager {
    private static final Map<UUID, ControlSeatClientData> playerDataMap = new HashMap<>();

    public static ControlSeatClientData getClientData(Player player) {
        if (player == null) {
            return null;
        }
        UUID playerId = player.getUUID();
        ControlSeatClientData data = playerDataMap.computeIfAbsent(playerId, id -> new ControlSeatClientData());
        // Dimension observation preserves the same UUID-keyed client state across LocalPlayer replacement.
        data.observeDimension(player.level().dimension());
        return data;
    }

    public static ControlSeatClientData getClientDataForSeat(Player player, BlockPos seatPos) {
        return getClientDataForSeat(player, seatPos, null);
    }

    public static ControlSeatClientData getClientDataForSeat(Player player, BlockPos seatPos, UUID seatEntityId) {
        if (player == null || seatPos == null) {
            return null;
        }
        if (!(player.getVehicle() instanceof ControlSeatMountEntity mount) || !seatPos.equals(mount.getBoundBlockPos())) {
            return null;
        }
        if (seatEntityId != null && !seatEntityId.equals(mount.getUUID())) {
            return null;
        }

        ControlSeatClientData clientData = getClientData(player);
        if (clientData != null) {
            // Function: stale packets from a previous chair must not overwrite the currently ridden chair state.
            clientData.bindSeat(seatPos, mount.getUUID());
        }
        return clientData;
    }

    public static void clearSeatBinding(Player player) {
        ControlSeatClientData clientData = getClientData(player);
        if (clientData != null) {
            clientData.clearSeatBinding();
        }
    }

    public static void onPlayerLogout(Player player) {
        if (player != null) {
            playerDataMap.remove(player.getUUID());
        }
    }

    @EventBusSubscriber(modid = "vsie", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class PlayerEventHandler {

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            getClientData(event.getEntity());
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            onPlayerLogout(event.getEntity());
        }
    }
}
