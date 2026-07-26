package com.kodu16.vsie.network.controlseat.S2C;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class NearbyShipsS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NearbyShipsS2CPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_s2c_nearbyshipss2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, NearbyShipsS2CPacket> STREAM_CODEC = CustomPacketPayload.codec(NearbyShipsS2CPacket::encode, NearbyShipsS2CPacket::decode);

    private final BlockPos pos;
    private final UUID seatEntityId;
    private final Map<String, Object> shipsData;

    public NearbyShipsS2CPacket(BlockPos pos, UUID seatEntityId, Map<String, Object> shipsData) {
        this.pos = pos;
        this.seatEntityId = seatEntityId;
        this.shipsData = shipsData;
    }

    public void encode(FriendlyByteBuf buf) {
        // Function: seat position lets the client reject radar packets from a previously ridden chair.
        buf.writeBlockPos(pos);
        buf.writeUUID(seatEntityId);
        buf.writeVarInt(shipsData.size());

        for (Map.Entry<String, Object> entry : shipsData.entrySet()) {
            String shipIdStr = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> attr = (Map<String, Object>) entry.getValue();

            buf.writeUtf(shipIdStr);
            buf.writeVarLong((long) attr.get("id"));
            buf.writeUtf((String) attr.get("slug"));
            buf.writeUtf((String) attr.get("dimension"));
            buf.writeDouble((double) attr.get("x"));
            buf.writeDouble((double) attr.get("y"));
            buf.writeDouble((double) attr.get("z"));
            buf.writeVarInt(toInt(attr.get("targetIndex")));
        }
    }

    public static NearbyShipsS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID seatEntityId = buf.readUUID();
        Map<String, Object> data = new LinkedHashMap<>();

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
            attr.put("targetIndex", buf.readVarInt());

            data.put(key, attr);
        }

        return new NearbyShipsS2CPacket(pos, seatEntityId, data);
    }

    public static void handle(NearbyShipsS2CPacket pkt, IPayloadContext context) {
        ClientHandler.handle(pkt, context);
    }

    // Keep client-only classes in a separate class file so dedicated servers can load the payload.
    private static final class ClientHandler {
        private static void handle(NearbyShipsS2CPacket pkt, IPayloadContext context) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;
                if (player == null) {
                    return;
                }

                ControlSeatClientData clientData =
                        ClientDataManager.getClientDataForSeat(player, pkt.pos, pkt.seatEntityId);
                if (clientData == null) {
                    return;
                }

                clientData.shipsData = pkt.shipsData;
            });
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
