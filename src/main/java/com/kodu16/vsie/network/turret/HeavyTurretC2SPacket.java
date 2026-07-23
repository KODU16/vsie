package com.kodu16.vsie.network.turret;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import org.slf4j.Logger;

public class HeavyTurretC2SPacket implements CustomPacketPayload {
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

    public static void handle(HeavyTurretC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(HeavyTurretC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            int changetype = pkt.changetype;
            BlockEntity BE = level.getBlockEntity(pos);
            if (!(BE instanceof AbstractHeavyTurretBlockEntity heavyturret)) {
                // Function: system chat packet encoding rejects raw BlockPos translation args.
                sender.sendSystemMessage(Component.translatable("message.vsie.network.invalid_turret", pos.toShortString()));
                return;
            }
            if (changetype == 5) {
                // Function: heavy turrets reuse the same block-damage checkbox semantics as ordinary turrets.
                heavyturret.toggleBreaksBlocksEnabled();
            } else if (changetype >= 100) {
                if (changetype > 102) {
                    return;
                }
                int fireType = changetype - 100;
                // Function: each heavy turret subclass decides which fire modes are selectable.
                heavyturret.modifyFireType(fireType);
                LogUtils.getLogger().warn("C2S:setting heavy turret fire type to:" + fireType);
            } else {
                if (changetype < 1 || changetype > 4) {
                    return;
                }
                heavyturret.modifyChannel(changetype);
                LogUtils.getLogger().warn("C2S:changing heavy turret channel:" + changetype);
            }
            heavyturret.markUpdated();
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
