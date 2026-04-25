// ControlSeatInputC2SPacket.java
package com.kodu16.vsie.network.turret;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;
import org.slf4j.Logger;

public class TurretC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<TurretC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "turret_turretc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, TurretC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(TurretC2SPacket::encode, TurretC2SPacket::decode);

    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final int changetype;
    public TurretC2SPacket(BlockPos pos, int changetype) {
        this.pos = pos;
        this.changetype = changetype;

    }

    public static void encode(TurretC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.changetype);
    }

    public static TurretC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int changetype = buf.readVarInt();
        return new TurretC2SPacket(pos,changetype);
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(TurretC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(TurretC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        //对于炮塔主要考虑的只有一个，当前数据包改了哪个数值
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            // 读取玩家输入
            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            int changetype = pkt.changetype;
            BlockEntity BE = level.getBlockEntity(pos);
            if (!(BE instanceof AbstractTurretBlockEntity turret)) {
                // Optionally log an error if the block entity is not found or is incorrect
                sender.sendSystemMessage(Component.literal("Invalid turret at " + pos));
                return;
            }
            // 功能：普通炮塔数据包显式忽略重型炮塔，避免两类炮塔编码串线。
            if (turret instanceof AbstractHeavyTurretBlockEntity) {
                return;
            }
            // 功能：普通炮塔只允许 1~4（敌对/被动/玩家/舰船）目标编码，忽略其它编码。
            if (changetype < 1 || changetype > 4) {
                return;
            }
            turret.modifyTargetType(pkt.changetype);
            // 功能：目标配置改变后立即下发客户端，保证按钮状态与图标实时刷新。
            turret.markUpdated();
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
