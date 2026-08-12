package com.kodu16.vsie.network.custom_turret;

import com.kodu16.vsie.content.custom_turret.CustomTurretDefinition;
import com.kodu16.vsie.content.custom_turret.CustomTurretPlacerItem;
import com.kodu16.vsie.vsie;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Copies a validated definition snapshot to the held placer item on the authoritative server. */
public record SelectCustomTurretC2SPacket(InteractionHand hand, String definitionJson)
        implements CustomPacketPayload {
    public static final Type<SelectCustomTurretC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "select_custom_turret")
    );
    public static final StreamCodec<FriendlyByteBuf, SelectCustomTurretC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SelectCustomTurretC2SPacket::encode, SelectCustomTurretC2SPacket::decode);

    private static void encode(SelectCustomTurretC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeUtf(packet.definitionJson, CustomTurretDefinition.MAX_JSON_LENGTH);
    }

    private static SelectCustomTurretC2SPacket decode(FriendlyByteBuf buffer) {
        return new SelectCustomTurretC2SPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readUtf(CustomTurretDefinition.MAX_JSON_LENGTH)
        );
    }

    public static void handle(SelectCustomTurretC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getItemInHand(packet.hand);
            if (!(stack.getItem() instanceof CustomTurretPlacerItem)) {
                return;
            }
            try {
                CustomTurretPlacerItem.setDefinitionJson(stack, packet.definitionJson);
            } catch (IllegalArgumentException ignored) {
                // Crafted packets cannot write malformed or over-sized definitions to an item.
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
