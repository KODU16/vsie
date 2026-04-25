package com.kodu16.vsie.network.rail;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ElectroMagnetRailCoreDetectC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<ElectroMagnetRailCoreDetectC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "rail_electromagnetrailcoredetectc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, ElectroMagnetRailCoreDetectC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ElectroMagnetRailCoreDetectC2SPacket::encode, ElectroMagnetRailCoreDetectC2SPacket::decode);

    private final BlockPos pos;

    public ElectroMagnetRailCoreDetectC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ElectroMagnetRailCoreDetectC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
    }

    public static ElectroMagnetRailCoreDetectC2SPacket decode(FriendlyByteBuf buf) {
        return new ElectroMagnetRailCoreDetectC2SPacket(buf.readBlockPos());
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(ElectroMagnetRailCoreDetectC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(ElectroMagnetRailCoreDetectC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // 在服务端主线程执行检测，保证世界读写安全。
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }

            ServerLevel level = sender.serverLevel();
            BlockEntity be = level.getBlockEntity(pkt.pos);
            if (be instanceof ElectroMagnetRailCoreBlockEntity core) {
                // 按钮触发核心执行“终端扫描”逻辑。
                core.detectTerminal();
            }
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
