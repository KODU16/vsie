// ControlSeatInputC2SPacket.java
package com.kodu16.vsie.network.turret;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class TurretDefaultSpinC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<TurretDefaultSpinC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "turret_turretdefaultspinc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, TurretDefaultSpinC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(TurretDefaultSpinC2SPacket::encode, TurretDefaultSpinC2SPacket::decode);

    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final int defaultspinx;
    public final int defaultspiny;
    public TurretDefaultSpinC2SPacket(BlockPos pos, int spinx, int spiny) {
        this.pos = pos;
        this.defaultspinx = spinx;
        this.defaultspiny = spiny;

    }

    public static void encode(TurretDefaultSpinC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.defaultspinx);
        buf.writeVarInt(pkt.defaultspiny);
    }

    public static TurretDefaultSpinC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int defaultspinx = buf.readVarInt();
        int defaultspiny = buf.readVarInt();
        return new TurretDefaultSpinC2SPacket(pos,defaultspinx,defaultspiny);
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(TurretDefaultSpinC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(TurretDefaultSpinC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        //对于炮塔主要考虑的只有一个，当前数据包改了哪个数值
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            // 读取玩家输入
            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            BlockEntity BE = level.getBlockEntity(pos);
            if (!(BE instanceof AbstractTurretBlockEntity turret)) {return;}
            turret.modifydefaultspin(pkt.defaultspinx, pkt.defaultspiny);
            // 可选：标记方块实体为脏以保存更改
            turret.setChanged();
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
