package com.kodu16.vsie.network.controlseat.C2S;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ControlSeatWarpCancelC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<ControlSeatWarpCancelC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_c2s_controlseatwarpcancelc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatWarpCancelC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ControlSeatWarpCancelC2SPacket::encode, ControlSeatWarpCancelC2SPacket::decode);

    public final BlockPos controlSeatPos;

    public ControlSeatWarpCancelC2SPacket(BlockPos controlSeatPos) {
        this.controlSeatPos = controlSeatPos;
    }

    public static void encode(ControlSeatWarpCancelC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.controlSeatPos);
    }

    public static ControlSeatWarpCancelC2SPacket decode(FriendlyByteBuf buf) {
        return new ControlSeatWarpCancelC2SPacket(buf.readBlockPos());
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(ControlSeatWarpCancelC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(ControlSeatWarpCancelC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }
            ServerLevel level = sender.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(pkt.controlSeatPos);
            if (!(blockEntity instanceof ControlSeatBlockEntity controlSeat)) {
                sender.sendSystemMessage(Component.literal("Invalid control seat at " + pkt.controlSeatPos));
                return;
            }
            ControlSeatServerData serverData = controlSeat.getServerData();
            // 功能：玩家在 warp 准备状态再次按下 P 时，直接取消自动对准并清空当前目标。
            serverData.clearWarpPreparation();
            controlSeat.setChanged();
            controlSeat.sendData();
            sender.sendSystemMessage(Component.literal("已取消 warp 准备状态，并清空当前目标"));
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
