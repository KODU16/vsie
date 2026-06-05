package com.kodu16.vsie.network.thruster;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ThrusterLimitC2SPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ThrusterLimitC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(vsie.ID, "thruster_limit_c2s"));
    public static final StreamCodec<FriendlyByteBuf, ThrusterLimitC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ThrusterLimitC2SPacket::encode, ThrusterLimitC2SPacket::decode);

    private final BlockPos pos;
    private final int forcePercent;
    private final int torquePercent;

    public ThrusterLimitC2SPacket(BlockPos pos, int forcePercent, int torquePercent) {
        this.pos = pos;
        this.forcePercent = forcePercent;
        this.torquePercent = torquePercent;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(forcePercent);
        buf.writeVarInt(torquePercent);
    }

    public static ThrusterLimitC2SPacket decode(FriendlyByteBuf buf) {
        return new ThrusterLimitC2SPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ThrusterLimitC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (sender == null) {
                return;
            }

            ServerLevel level = sender.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos);
            if (blockEntity instanceof AbstractThrusterBlockEntity thruster) {
                // Function: persist per-thruster force and torque authority limits from the GUI.
                thruster.setOutputLimitPercents(packet.forcePercent, packet.torquePercent);
                thruster.markUpdated();
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
