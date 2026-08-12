package com.kodu16.vsie.network.misc;

import com.kodu16.vsie.content.misc.enemy_core.EnemyCoreBlockEntity;
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

public record EnemyCoreSettingsC2SPacket(
        BlockPos pos,
        String enemyPattern,
        String allyPattern,
        double orbitRadius,
        double orbitSpeed
) implements CustomPacketPayload {
    public static final Type<EnemyCoreSettingsC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "enemy_core_settings")
    );
    public static final StreamCodec<FriendlyByteBuf, EnemyCoreSettingsC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(EnemyCoreSettingsC2SPacket::encode, EnemyCoreSettingsC2SPacket::decode);

    private static void encode(EnemyCoreSettingsC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeUtf(packet.enemyPattern, EnemyCoreBlockEntity.MAX_PATTERN_LENGTH);
        buffer.writeUtf(packet.allyPattern, EnemyCoreBlockEntity.MAX_PATTERN_LENGTH);
        buffer.writeDouble(packet.orbitRadius);
        buffer.writeDouble(packet.orbitSpeed);
    }

    private static EnemyCoreSettingsC2SPacket decode(FriendlyByteBuf buffer) {
        return new EnemyCoreSettingsC2SPacket(
                buffer.readBlockPos(),
                buffer.readUtf(EnemyCoreBlockEntity.MAX_PATTERN_LENGTH),
                buffer.readUtf(EnemyCoreBlockEntity.MAX_PATTERN_LENGTH),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    public static void handle(EnemyCoreSettingsC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            // Function: reject non-finite values before applying server-side limits.
            if (!Double.isFinite(packet.orbitRadius) || !Double.isFinite(packet.orbitSpeed)) {
                return;
            }

            BlockEntity blockEntity = player.serverLevel().getBlockEntity(packet.pos);
            if (blockEntity instanceof EnemyCoreBlockEntity controller) {
                SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(player.serverLevel(), packet.pos);
                if (player.position().distanceToSqr(ServerShipUtils.getBlockCenterWorld(subLevel, packet.pos)) > 64.0D) {
                    return;
                }
                // Function: server-side clamping makes crafted packets unable to exceed controller limits.
                controller.applySettings(
                        packet.enemyPattern,
                        packet.allyPattern,
                        packet.orbitRadius,
                        packet.orbitSpeed
                );
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
