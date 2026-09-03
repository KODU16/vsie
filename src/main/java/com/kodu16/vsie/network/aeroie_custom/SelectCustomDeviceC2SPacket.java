package com.kodu16.vsie.network.aeroie_custom;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceDefinition;
import com.kodu16.vsie.content.aeroie_custom.CustomDevicePlacerItem;
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
public record SelectCustomDeviceC2SPacket(InteractionHand hand, String definitionJson)
        implements CustomPacketPayload {
    public static final Type<SelectCustomDeviceC2SPacket> TYPE = new Type<>(
            // Function: retain the legacy payload id so existing client/server pairs remain wire-compatible.
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "select_custom_turret")
    );
    public static final StreamCodec<FriendlyByteBuf, SelectCustomDeviceC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SelectCustomDeviceC2SPacket::encode, SelectCustomDeviceC2SPacket::decode);

    private static void encode(SelectCustomDeviceC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeUtf(packet.definitionJson, CustomDeviceDefinition.MAX_JSON_LENGTH);
    }

    private static SelectCustomDeviceC2SPacket decode(FriendlyByteBuf buffer) {
        return new SelectCustomDeviceC2SPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readUtf(CustomDeviceDefinition.MAX_JSON_LENGTH)
        );
    }

    public static void handle(SelectCustomDeviceC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getItemInHand(packet.hand);
            if (!(stack.getItem() instanceof CustomDevicePlacerItem)) {
                return;
            }
            try {
                CustomDevicePlacerItem.setDefinitionJson(stack, packet.definitionJson);
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
