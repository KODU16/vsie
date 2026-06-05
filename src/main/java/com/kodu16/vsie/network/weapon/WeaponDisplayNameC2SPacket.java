package com.kodu16.vsie.network.weapon;

import com.kodu16.vsie.content.weapon.redstone_relay.RedstoneRelayBlockEntity;
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

public class WeaponDisplayNameC2SPacket implements CustomPacketPayload {
    // Function: sync the relay HUD short name from the shared weapon GUI back to the authoritative server block entity.
    public static final Type<WeaponDisplayNameC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(vsie.ID, "weapon_display_name_c2s"));
    public static final StreamCodec<FriendlyByteBuf, WeaponDisplayNameC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(WeaponDisplayNameC2SPacket::encode, WeaponDisplayNameC2SPacket::decode);

    private final BlockPos pos;
    private final String displayName;

    public WeaponDisplayNameC2SPacket(BlockPos pos, String displayName) {
        this.pos = pos;
        this.displayName = displayName;
    }

    public static void encode(WeaponDisplayNameC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeUtf(pkt.displayName, 64);
    }

    public static WeaponDisplayNameC2SPacket decode(FriendlyByteBuf buf) {
        return new WeaponDisplayNameC2SPacket(buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(WeaponDisplayNameC2SPacket pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(pkt.pos);
            if (blockEntity instanceof RedstoneRelayBlockEntity relay) {
                relay.setCustomHudDisplayName(pkt.displayName);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
