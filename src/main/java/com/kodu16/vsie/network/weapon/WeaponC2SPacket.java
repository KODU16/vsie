package com.kodu16.vsie.network.weapon;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class WeaponC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<WeaponC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "weapon_weaponc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, WeaponC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(WeaponC2SPacket::encode, WeaponC2SPacket::decode);

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

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(WeaponC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
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
            LogUtils.getLogger().warn(String.valueOf(Component.literal("changing weapon channel"+channelchange)));
            // 可选：标记方块实体为脏以保存更改
            weapon.setChanged();
            weapon.getLevel().sendBlockUpdated(     // 向附近玩家同步 BE
                    weapon.getBlockPos(),
                    weapon.getBlockState(),
                    weapon.getBlockState(),
                    Block.UPDATE_CLIENTS   // 3
            );
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
