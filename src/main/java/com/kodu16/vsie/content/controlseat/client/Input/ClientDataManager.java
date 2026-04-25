package com.kodu16.vsie.content.controlseat.client.Input;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientDataManager {
    // 我必须搞个初始化
    // 存储每个玩家的 ClientData
    private static final Map<UUID, ControlSeatClientData> playerDataMap = new HashMap<>();

    // 根据玩家的 UUID 获取该玩家的 ClientData
    public static ControlSeatClientData getClientData(Player player) {
        UUID playerId = player.getUUID();
        // 如果玩家的 ClientData 不存在，就创建一个新的
        return playerDataMap.computeIfAbsent(playerId, id -> new ControlSeatClientData());
    }

    // 退出时清理玩家数据
    public static void onPlayerLogout(Player player) {
        UUID playerId = player.getUUID();
        playerDataMap.remove(playerId);
    }

    // 在 PlayerEventHandler 内部
    @EventBusSubscriber(modid = "vsie", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class PlayerEventHandler {

        // 功能：监听客户端玩家登录事件，确保控制椅 HUD 数据容器在 NeoForge 1.21.1 下及时初始化。
        // 玩家进入时初始化 ClientData
        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            Player player = event.getEntity();
            ControlSeatClientData clientData = getClientData(player);
            // 初始化时可能需要根据玩家的情况设置默认值
            // 比如你可以通过 player 设置一些默认状态
        }

        // 功能：监听客户端玩家退出事件，避免旧会话数据残留造成 HUD/输入状态串号。
        // 玩家退出时清理 ClientData
        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            Player player = event.getEntity();
            onPlayerLogout(player);
        }
    }
}
