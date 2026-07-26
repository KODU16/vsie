package com.kodu16.vsie.network.controlseat.S2C;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class ControlSeatInputS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ControlSeatInputS2CPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    "vsie", "controlseat_s2c_controlseatinputs2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatInputS2CPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ControlSeatInputS2CPacket::write, ControlSeatInputS2CPacket::decode);

    private final BlockPos pos;
    private final UUID seatEntityId;
    private final int channelencode;

    public ControlSeatInputS2CPacket(BlockPos pos, UUID seatEntityId, int channelencode) {
        this.pos = pos;
        this.seatEntityId = seatEntityId;
        this.channelencode = channelencode;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(seatEntityId);
        buf.writeInt(channelencode);
    }

    public static ControlSeatInputS2CPacket decode(FriendlyByteBuf buf) {
        return new ControlSeatInputS2CPacket(buf.readBlockPos(), buf.readUUID(), buf.readInt());
    }

    public static void handle(ControlSeatInputS2CPacket pkt, IPayloadContext context) {
        ClientHandler.handle(pkt, context);
    }

    // Keep client-only classes in a separate class file so dedicated servers can load the payload.
    private static final class ClientHandler {
        private static void handle(ControlSeatInputS2CPacket pkt, IPayloadContext context) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;
                ControlSeatClientData clientData =
                        ClientDataManager.getClientDataForSeat(player, pkt.pos, pkt.seatEntityId);
                if (clientData == null) {
                    LogUtils.getLogger().warn("Received ControlSeatInputS2C without matching client data");
                    return;
                }
                clientData.channel1 = (pkt.channelencode & 1) != 0;
                clientData.channel2 = (pkt.channelencode & 2) != 0;
                clientData.channel3 = (pkt.channelencode & 4) != 0;
                clientData.channel4 = (pkt.channelencode & 8) != 0;
            });
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
