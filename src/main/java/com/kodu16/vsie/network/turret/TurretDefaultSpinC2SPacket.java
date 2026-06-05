package com.kodu16.vsie.network.turret;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class TurretDefaultSpinC2SPacket implements CustomPacketPayload {
    // Function: registers the turret settings payload used by both normal and heavy turret screens.
    public static final CustomPacketPayload.Type<TurretDefaultSpinC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "turret_turretdefaultspinc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, TurretDefaultSpinC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(TurretDefaultSpinC2SPacket::encode, TurretDefaultSpinC2SPacket::decode);

    public static final Logger LOGGER = LogUtils.getLogger();

    public final BlockPos pos;
    public final int defaultspinx;
    public final int defaultspiny;
    public final int aimLimitMinX;
    public final int aimLimitMaxX;
    public final int aimLimitMinY;
    public final int aimLimitMaxY;

    public TurretDefaultSpinC2SPacket(
            BlockPos pos,
            int spinx,
            int spiny,
            int aimLimitMinX,
            int aimLimitMaxX,
            int aimLimitMinY,
            int aimLimitMaxY
    ) {
        this.pos = pos;
        this.defaultspinx = spinx;
        this.defaultspiny = spiny;
        this.aimLimitMinX = aimLimitMinX;
        this.aimLimitMaxX = aimLimitMaxX;
        this.aimLimitMinY = aimLimitMinY;
        this.aimLimitMaxY = aimLimitMaxY;
    }

    public static void encode(TurretDefaultSpinC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.defaultspinx);
        buf.writeVarInt(pkt.defaultspiny);
        buf.writeVarInt(pkt.aimLimitMinX);
        buf.writeVarInt(pkt.aimLimitMaxX);
        buf.writeVarInt(pkt.aimLimitMinY);
        buf.writeVarInt(pkt.aimLimitMaxY);
    }

    public static TurretDefaultSpinC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int defaultspinx = buf.readVarInt();
        int defaultspiny = buf.readVarInt();
        int aimLimitMinX = buf.readVarInt();
        int aimLimitMaxX = buf.readVarInt();
        int aimLimitMinY = buf.readVarInt();
        int aimLimitMaxY = buf.readVarInt();
        return new TurretDefaultSpinC2SPacket(pos, defaultspinx, defaultspiny, aimLimitMinX, aimLimitMaxX, aimLimitMinY, aimLimitMaxY);
    }

    public static void handle(TurretDefaultSpinC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(TurretDefaultSpinC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }

            ServerLevel level = sender.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(pkt.pos);
            if (!(blockEntity instanceof AbstractTurretBlockEntity turret)) {
                return;
            }

            turret.modifydefaultspin(pkt.defaultspinx, pkt.defaultspiny);
            // Function: save the turret's automatic targeting window together with its default return angles.
            turret.setAimLimits(pkt.aimLimitMinX, pkt.aimLimitMaxX, pkt.aimLimitMinY, pkt.aimLimitMaxY);
            turret.setChanged();
            turret.markUpdated();
        });
        ctx.setPacketHandled(true);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
