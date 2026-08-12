package com.kodu16.vsie.network.misc;

import com.kodu16.vsie.content.misc.enemy_autocannon.EnemyAutocannonBlockEntity;
import com.kodu16.vsie.content.misc.enemy_autocannon.EnemyAutocannonContainerMenu;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.vsie;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EnemyAutocannonSettingsC2SPacket(
        BlockPos pos, double spreadAngle, int shotIntervalTicks, int burstShots
) implements CustomPacketPayload {
    public static final Type<EnemyAutocannonSettingsC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "enemy_autocannon_settings")
    );
    public static final StreamCodec<FriendlyByteBuf, EnemyAutocannonSettingsC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(EnemyAutocannonSettingsC2SPacket::encode, EnemyAutocannonSettingsC2SPacket::decode);

    private static void encode(EnemyAutocannonSettingsC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeDouble(packet.spreadAngle);
        buffer.writeVarInt(packet.shotIntervalTicks);
        buffer.writeVarInt(packet.burstShots);
    }

    private static EnemyAutocannonSettingsC2SPacket decode(FriendlyByteBuf buffer) {
        return new EnemyAutocannonSettingsC2SPacket(
                buffer.readBlockPos(), buffer.readDouble(), buffer.readVarInt(), buffer.readVarInt()
        );
    }

    public static void handle(EnemyAutocannonSettingsC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof EnemyAutocannonContainerMenu menu)
                    || !menu.getBlockPosition().equals(packet.pos)
                    || !Double.isFinite(packet.spreadAngle)) {
                return;
            }
            BlockEntity blockEntity = player.serverLevel().getBlockEntity(packet.pos);
            if (!(blockEntity instanceof EnemyAutocannonBlockEntity autocannon)) {
                return;
            }
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(player.serverLevel(), packet.pos);
            if (player.position().distanceToSqr(ServerShipUtils.getBlockCenterWorld(subLevel, packet.pos)) > 64.0D) {
                return;
            }
            // Function: server-side clamping rejects numeric abuse while permitting valid GUI settings.
            autocannon.applySettings(packet.spreadAngle, packet.shotIntervalTicks, packet.burstShots);
        });
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
