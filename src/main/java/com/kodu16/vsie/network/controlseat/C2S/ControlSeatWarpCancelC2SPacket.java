package com.kodu16.vsie.network.controlseat.C2S;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ControlSeatWarpCancelC2SPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ControlSeatWarpCancelC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_c2s_controlseatwarpcancelc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatWarpCancelC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ControlSeatWarpCancelC2SPacket::encode, ControlSeatWarpCancelC2SPacket::decode);

    public final BlockPos controlSeatPos;
    public final UUID seatEntityId;

    public ControlSeatWarpCancelC2SPacket(BlockPos controlSeatPos, UUID seatEntityId) {
        this.controlSeatPos = controlSeatPos;
        this.seatEntityId = seatEntityId;
    }

    public static void encode(ControlSeatWarpCancelC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.controlSeatPos);
        buf.writeUUID(pkt.seatEntityId);
    }

    public static ControlSeatWarpCancelC2SPacket decode(FriendlyByteBuf buf) {
        return new ControlSeatWarpCancelC2SPacket(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(ControlSeatWarpCancelC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new NetworkEvent.Context(context));
    }

    public static void handle(ControlSeatWarpCancelC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }

            ControlSeatBlockEntity controlSeat = ControlSeatPacketResolver.resolve(sender, pkt.controlSeatPos, pkt.seatEntityId);
            if (controlSeat == null) {
                return;
            }

            ControlSeatServerData serverData = controlSeat.getServerData();
            serverData.clearWarpPreparation();
            controlSeat.setChanged();
            controlSeat.sendData();
            sender.sendSystemMessage(Component.translatable("message.vsie.warp.cancelled"));
        });
        ctx.setPacketHandled(true);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
