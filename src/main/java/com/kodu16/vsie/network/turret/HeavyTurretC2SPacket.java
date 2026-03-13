package com.kodu16.vsie.network.turret;

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

public class HeavyTurretC2SPacket {
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
                int fireType = changetype - 100;
                heavyturret.modifyheavytargettype(fireType);
                LogUtils.getLogger().warn("C2S:setting heavy turret fire type to:" + fireType);
            } else {
                heavyturret.modifychannel(changetype);
                LogUtils.getLogger().warn("C2S:changing heavy turret channel:" + changetype);
            }
            heavyturret.setChanged();
        });
        ctx.setPacketHandled(true);
    }
}
