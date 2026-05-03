package com.kodu16.vsie.network.thruster;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class VectorThrusterS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VectorThrusterS2CPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("vsie", "thruster_vectorthrusters2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, VectorThrusterS2CPacket> STREAM_CODEC =
            CustomPacketPayload.codec(VectorThrusterS2CPacket::write, VectorThrusterS2CPacket::decode);

    private final BlockPos pos;
    private final double rotX;
    private final double rotY;

    public VectorThrusterS2CPacket(BlockPos pos, double rotX, double rotY) {
        this.pos = pos;
        this.rotX = rotX;
        this.rotY = rotY;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeDouble(rotX);
        buf.writeDouble(rotY);
    }

    public static VectorThrusterS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        double rotX = buf.readDouble();
        double rotY = buf.readDouble();
        return new VectorThrusterS2CPacket(pos, rotX, rotY);
    }

    public static void handle(VectorThrusterS2CPacket pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
