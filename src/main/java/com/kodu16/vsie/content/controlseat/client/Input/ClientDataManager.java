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
        ControlSeatClientData clientData = getClientData(player);
        if (clientData == null) {
            return null;
        }

        boolean ridingSeat = player.getVehicle() instanceof ControlSeatMountEntity mount
                && seatPos.equals(mount.getBoundBlockPos());
        boolean sameMount = ridingSeat
                && (seatEntityId == null || seatEntityId.equals(((ControlSeatMountEntity) player.getVehicle()).getUUID()));
        // 跨维度后 Sable 会重建 seat mount，服务器发来的 entity id 可能和客户端当前挂载短暂不一致；
        // 只要玩家还骑着该座位，就按座位坐标重新绑定，避免控制输入被直接丢弃导致飞船冻结。
        if (ridingSeat && (sameMount || clientData.isDimensionTransferPending())) {
            clientData.bindSeat(seatPos, ((ControlSeatMountEntity) player.getVehicle()).getUUID());
            return clientData;
        }
        if (clientData.isDimensionTransferPending() && clientData.isBoundToSeat(seatPos)) {
            // 客户端尚未收到新的骑乘关系时，继续沿用上一次绑定的座位数据完成过渡。
            return clientData;
        }
        if (clientData.isBoundToSeat(seatPos)) {
            // 跨维度重建期间服务端可能先于客户端更新 mount UUID，此时仍按座位坐标接受输入同步。
            clientData.bindSeat(seatPos, ridingSeat ? ((ControlSeatMountEntity) player.getVehicle()).getUUID() : seatEntityId);
            return clientData;
        }
        return null;
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
