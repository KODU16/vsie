package com.kodu16.vsie.network.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ThrusterS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ThrusterS2CPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("vsie", "thruster_thrusters2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, ThrusterS2CPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ThrusterS2CPacket::write, ThrusterS2CPacket::decode);

    private final BlockPos pos;
    private final float raylength;

    public ThrusterS2CPacket(BlockPos pos, float raylength) {
        this.pos = pos;
        this.raylength = raylength;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(raylength);
    }

    public static ThrusterS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        float raylength = buf.readFloat();
        return new ThrusterS2CPacket(pos, raylength);
    }

    public static void handle(ThrusterS2CPacket pkt, IPayloadContext context) {
        // Reserved payload: decoding it currently requires no client-side state update.
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
