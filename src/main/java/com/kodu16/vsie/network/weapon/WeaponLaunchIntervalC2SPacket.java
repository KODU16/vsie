package com.kodu16.vsie.network.weapon;

import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotCoreBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class WeaponLaunchIntervalC2SPacket implements CustomPacketPayload {
    // Function: sync the VLS core launch interval k from its GUI to the authoritative server BE.
    public static final Type<WeaponLaunchIntervalC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(vsie.ID, "weapon_launch_interval_c2s"));
    public static final StreamCodec<FriendlyByteBuf, WeaponLaunchIntervalC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(WeaponLaunchIntervalC2SPacket::encode, WeaponLaunchIntervalC2SPacket::decode);

    private final BlockPos pos;
    private final int launchIntervalTicks;

    public WeaponLaunchIntervalC2SPacket(BlockPos pos, int launchIntervalTicks) {
        this.pos = pos;
        this.launchIntervalTicks = launchIntervalTicks;
    }

    public static void encode(WeaponLaunchIntervalC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.launchIntervalTicks);
    }

    public static WeaponLaunchIntervalC2SPacket decode(FriendlyByteBuf buf) {
        return new WeaponLaunchIntervalC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(WeaponLaunchIntervalC2SPacket pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(pkt.pos);
            if (blockEntity instanceof VerticleLaunchingSlotCoreBlockEntity core) {
                core.setLaunchIntervalTicks(pkt.launchIntervalTicks);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
