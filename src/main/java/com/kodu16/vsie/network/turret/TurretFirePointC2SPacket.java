package com.kodu16.vsie.network.turret;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3d;

import java.util.function.Supplier;

public class TurretFirePointC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<TurretFirePointC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "turret_turretfirepointc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, TurretFirePointC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(TurretFirePointC2SPacket::encode, TurretFirePointC2SPacket::decode);

    public final BlockPos pos;
    public final Vector3d postofire;

    public TurretFirePointC2SPacket(BlockPos pos, Vector3d postofire) {
        this.pos = pos;
        this.postofire = postofire;
    }

    public static void encode(TurretFirePointC2SPacket pkt, FriendlyByteBuf buf) {
        // 功能：把客户端 firepoint 坐标发送到服务端，供粒子炮直接作为生成点使用。
        buf.writeBlockPos(pkt.pos);
        buf.writeDouble(pkt.postofire.x);
        buf.writeDouble(pkt.postofire.y);
        buf.writeDouble(pkt.postofire.z);
    }

    public static TurretFirePointC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Vector3d postofire = new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new TurretFirePointC2SPacket(pos, postofire);
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(TurretFirePointC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(TurretFirePointC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }
            ServerLevel level = sender.serverLevel();
            BlockEntity be = level.getBlockEntity(pkt.pos);
            if (be instanceof AbstractTurretBlockEntity turret) {
                // 功能：缓存firepoint 坐标，供服务端开火时直接读取。
                turret.setFirePoint(pkt.postofire);
            }
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
