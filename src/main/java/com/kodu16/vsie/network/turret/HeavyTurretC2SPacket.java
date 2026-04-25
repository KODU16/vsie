package com.kodu16.vsie.network.turret;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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

public class HeavyTurretC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<HeavyTurretC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "turret_heavyturretc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, HeavyTurretC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(HeavyTurretC2SPacket::encode, HeavyTurretC2SPacket::decode);

    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final int changetype;
    public HeavyTurretC2SPacket(BlockPos pos, int changetype) {
        this.pos = pos;
        this.changetype = changetype;

    }

    public static void encode(HeavyTurretC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.changetype);
    }

    public static HeavyTurretC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int changetype = buf.readVarInt();
        return new HeavyTurretC2SPacket(pos,changetype);
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(HeavyTurretC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(HeavyTurretC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
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
            if (!(BE instanceof AbstractHeavyTurretBlockEntity heavyturret)) {
                // Optionally log an error if the block entity is not found or is incorrect
                sender.sendSystemMessage(Component.literal("Invalid turret at " + pos));
                return;
            }
            // 功能：复用同一数据包，同时支持“切换开火模式”和“切换频道”。
            if (changetype >= 100) {
                // 功能：重型炮塔的模式编码只允许 100~102（手动/自动/智能），其它编码直接忽略。
                if (changetype > 102) {
                    return;
                }
                int fireType = changetype - 100;
                heavyturret.modifyFireType(fireType);
                LogUtils.getLogger().warn("C2S:setting heavy turret fire type to:" + fireType);
            } else {
                // 功能：重型炮塔的频道编码只允许 1~4，保持与主武器频道开关一致。
                if (changetype < 1 || changetype > 4) {
                    return;
                }
                heavyturret.modifyChannel(changetype);
                LogUtils.getLogger().warn("C2S:changing heavy turret channel:" + changetype);
            }
            // 功能：配置变更后立即同步方块实体，确保 GUI 与服务端状态保持一致。
            heavyturret.markUpdated();
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
