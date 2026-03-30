package com.kodu16.vsie.network.turret;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3d;

import java.util.function.Supplier;

public class TurretFirePointC2SPacket {
    public final BlockPos pos;
    public final Vector3d firePoint;

    public TurretFirePointC2SPacket(BlockPos pos, Vector3d firePoint) {
        this.pos = pos;
        this.firePoint = firePoint;
    }

    public static void encode(TurretFirePointC2SPacket pkt, FriendlyByteBuf buf) {
        // 功能：把客户端 firepoint 坐标发送到服务端，供粒子炮直接作为生成点使用。
        buf.writeBlockPos(pkt.pos);
        buf.writeDouble(pkt.firePoint.x);
        buf.writeDouble(pkt.firePoint.y);
        buf.writeDouble(pkt.firePoint.z);
    }

    public static TurretFirePointC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Vector3d firePoint = new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new TurretFirePointC2SPacket(pos, firePoint);
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
                turret.setFirePoint(pkt.firePoint);
            }
        });
        ctx.setPacketHandled(true);
    }
}
