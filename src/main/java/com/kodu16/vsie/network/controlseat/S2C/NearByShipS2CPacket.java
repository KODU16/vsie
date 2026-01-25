package com.kodu16.vsie.network.controlseat.S2C;

import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.Input.ControlSeatClientData;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NearbyShipsS2CPacket {

    private final Map<String, Object> shipsData;

    public NearbyShipsS2CPacket(Map<String, Object> shipsData) {
        this.shipsData = shipsData;
    }

    // 编码（服务端 → 写入 buffer）
    public void encode(FriendlyByteBuf buf) {
        // 先写船只数量
        buf.writeVarInt(shipsData.size());

        for (Map.Entry<String, Object> entry : shipsData.entrySet()) {
            String shipIdStr = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> attr = (Map<String, Object>) entry.getValue();

            buf.writeUtf(shipIdStr);                      // ship id as string key

            buf.writeVarInt((int) attr.get("id"));        // ship numeric id
            buf.writeUtf((String) attr.get("slug"));      // slug
            buf.writeUtf((String) attr.get("dimension")); // dimension (通常是字符串或ResourceLocation)

            buf.writeDouble((double) attr.get("x"));
            buf.writeDouble((double) attr.get("y"));
            buf.writeDouble((double) attr.get("z"));

            buf.writeDouble((double) attr.get("min_x"));
            buf.writeDouble((double) attr.get("min_y"));
            buf.writeDouble((double) attr.get("min_z"));
            buf.writeDouble((double) attr.get("max_x"));
            buf.writeDouble((double) attr.get("max_y"));
            buf.writeDouble((double) attr.get("max_z"));
        }
    }

    // 解码（客户端读取）
    public static NearbyShipsS2CPacket decode(FriendlyByteBuf buf) {
        Map<String, Object> data = new HashMap<>();

        int size = buf.readVarInt();

        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();

            Map<String, Object> attr = new HashMap<>();
            attr.put("id", buf.readVarInt());
            attr.put("slug", buf.readUtf());
            attr.put("dimension", buf.readUtf());

            attr.put("x", buf.readDouble());
            attr.put("y", buf.readDouble());
            attr.put("z", buf.readDouble());

            attr.put("min_x", buf.readDouble());
            attr.put("min_y", buf.readDouble());
            attr.put("min_z", buf.readDouble());
            attr.put("max_x", buf.readDouble());
            attr.put("max_y", buf.readDouble());
            attr.put("max_z", buf.readDouble());

            data.put(key, attr);
        }

        return new NearbyShipsS2CPacket(data);
    }

    // 处理逻辑（客户端收到后执行）
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft mc = Minecraft.getInstance();
                    Player player = mc.player;
                    if (player == null) return;

                    ControlSeatClientData clientData = ClientDataManager.getClientData(player);
                    if (clientData == null) {
                        LogUtils.getLogger().warn("NearbyShipsS2CPacket received but clientData is null for player {}",
                                player.getName().getString());
                        return;
                    }

                    // 存入 clientData（你需要在这里新增一个字段或方法）
                    clientData.setNearbyShips(shipsData);

                    // 可选：在这里触发 HUD / 渲染更新
                    // 例如：ClientHudOverlay.updateShipRadar(); 或其他自定义逻辑
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
