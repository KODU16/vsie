package com.kodu16.vsie.network.misc;

import com.kodu16.vsie.content.misc.enemy_cannon.EnemyCannonBlockEntity;
import com.kodu16.vsie.content.misc.enemy_cannon.EnemyCannonContainerMenu;
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

public record EnemyCannonSettingsC2SPacket(BlockPos pos, int chargeCount, int cooldownTicks)
        implements CustomPacketPayload {
    public static final Type<EnemyCannonSettingsC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "enemy_cannon_settings")
    );
    public static final StreamCodec<FriendlyByteBuf, EnemyCannonSettingsC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(EnemyCannonSettingsC2SPacket::encode, EnemyCannonSettingsC2SPacket::decode);

    private static void encode(EnemyCannonSettingsC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.chargeCount);
        buffer.writeVarInt(packet.cooldownTicks);
    }

    private static EnemyCannonSettingsC2SPacket decode(FriendlyByteBuf buffer) {
        return new EnemyCannonSettingsC2SPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(EnemyCannonSettingsC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof EnemyCannonContainerMenu menu)
                    || !menu.getBlockPosition().equals(packet.pos)) {
                return;
            }
            BlockEntity blockEntity = player.serverLevel().getBlockEntity(packet.pos);
            if (!(blockEntity instanceof EnemyCannonBlockEntity cannon)) {
                return;
            }
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(player.serverLevel(), packet.pos);
            if (player.position().distanceToSqr(ServerShipUtils.getBlockCenterWorld(subLevel, packet.pos)) > 64.0D) {
                return;
            }
            // Function: clamp crafted packet values again on the authoritative server.
            cannon.applySettings(packet.chargeCount, packet.cooldownTicks);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
