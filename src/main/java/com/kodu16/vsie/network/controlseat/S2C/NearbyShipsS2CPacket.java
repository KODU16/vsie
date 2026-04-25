package com.kodu16.vsie.network.controlseat.S2C;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NearbyShipsS2CPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<NearbyShipsS2CPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_s2c_nearbyshipss2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, NearbyShipsS2CPacket> STREAM_CODEC = CustomPacketPayload.codec((buf, pkt) -> pkt.encode(buf), NearbyShipsS2CPacket::decode);


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

            buf.writeVarLong((long) attr.get("id"));        // ship numeric id
            buf.writeUtf((String) attr.get("slug"));      // slug
            buf.writeUtf((String) attr.get("dimension")); // dimension (通常是字符串或ResourceLocation)

            buf.writeDouble((double) attr.get("x"));
            buf.writeDouble((double) attr.get("y"));
            buf.writeDouble((double) attr.get("z"));
        }
    }

    // 解码（客户端读取）
    public static NearbyShipsS2CPacket decode(FriendlyByteBuf buf) {
        Map<String, Object> data = new HashMap<>();

        int size = buf.readVarInt();

        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();

            Map<String, Object> attr = new HashMap<>();
            attr.put("id", buf.readVarLong());
            attr.put("slug", buf.readUtf());
            attr.put("dimension", buf.readUtf());

            attr.put("x", buf.readDouble());
            attr.put("y", buf.readDouble());
            attr.put("z", buf.readDouble());

            data.put(key, attr);
        }

        return new NearbyShipsS2CPacket(data);
    }

    // 处理逻辑（客户端收到后执行）
    // 功能：NeoForge 1.21.1 处理器入口，复用旧版实例方法逻辑。
    public static void handle(NearbyShipsS2CPacket pkt, IPayloadContext context) {
        pkt.handle(() -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

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
                    clientData.shipsData = shipsData;

                    // 可选：在这里触发 HUD / 渲染更新
                    // 例如：ClientHudOverlay.updateShipRadar(); 或其他自定义逻辑
                })
        );
        ctx.get().setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
