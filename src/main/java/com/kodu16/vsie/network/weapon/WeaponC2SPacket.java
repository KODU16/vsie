package com.kodu16.vsie.network.weapon;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
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

public class WeaponC2SPacket {
    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final int channelchange;
    public WeaponC2SPacket(BlockPos pos, int channelchange) {
        this.pos = pos;
        this.channelchange = channelchange;
    }
    public static void encode(WeaponC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.channelchange);
    }
    public static WeaponC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int channelchange = buf.readVarInt();
        return new WeaponC2SPacket(pos,channelchange);
    }

    public static void handle(WeaponC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        //对于主武器主要考虑的只有一个，当前数据包是想改哪个频道
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            // 读取玩家输入
            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            int channelchange = pkt.channelchange;
            BlockEntity BE = level.getBlockEntity(pos);
            if (!(BE instanceof AbstractWeaponBlockEntity weapon)) {
                // Optionally log an error if the block entity is not found or is incorrect
                sender.sendSystemMessage(Component.literal("Invalid weapon at " + pos));
                return;
            }
            weapon.modifychannel(channelchange);
            // 可选：标记方块实体为脏以保存更改
            weapon.setChanged();
        });
        ctx.setPacketHandled(true);
    }
}
