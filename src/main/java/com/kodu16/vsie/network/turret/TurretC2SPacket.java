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

    public static void handle(TurretC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(TurretC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            int changetype = pkt.changetype;
            BlockEntity BE = level.getBlockEntity(pos);
            if (!(BE instanceof AbstractTurretBlockEntity turret)) {
                // Function: system chat packet encoding rejects raw BlockPos translation args.
                sender.sendSystemMessage(Component.translatable("message.vsie.network.invalid_turret", pos.toShortString()));
                return;
            }
            if (turret instanceof AbstractHeavyTurretBlockEntity) {
                return;
            }
            if (changetype < 1 || changetype > 5) {
                return;
            }
            if (changetype == 5) {
                turret.toggleBreaksBlocksEnabled();
                turret.markUpdated();
                return;
            }
            turret.modifyTargetType(pkt.changetype);
            turret.markUpdated();
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
