package com.kodu16.vsie.network.storage;

import com.kodu16.vsie.content.storage.ammobox.AmmoBoxRefillMarkerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class AmmoBoxRefillMarkerS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AmmoBoxRefillMarkerS2CPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("vsie", "storage_ammobox_refill_marker_s2c"));
    public static final StreamCodec<FriendlyByteBuf, AmmoBoxRefillMarkerS2CPacket> STREAM_CODEC =
            CustomPacketPayload.codec(AmmoBoxRefillMarkerS2CPacket::write, AmmoBoxRefillMarkerS2CPacket::decode);

    private final BlockPos ammoBoxPos;
    private final ItemStack ammoStack;
    private final int amount;
    private final int targetIndex;
    private final String targetDisplayName;
    private final int durationTicks;

    public AmmoBoxRefillMarkerS2CPacket(BlockPos ammoBoxPos, ItemStack ammoStack, int amount,
                                        int targetIndex, String targetDisplayName, int durationTicks) {
        this.ammoBoxPos = ammoBoxPos;
        this.ammoStack = ammoStack.copy();
        this.amount = amount;
        this.targetIndex = targetIndex;
        this.targetDisplayName = targetDisplayName == null ? "" : targetDisplayName;
        this.durationTicks = durationTicks;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(ammoBoxPos);
        buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(ammoStack.getItem()));
        buf.writeVarInt(ammoStack.getCount());
        buf.writeVarInt(ammoStack.getDamageValue());
        buf.writeVarInt(amount);
        buf.writeVarInt(targetIndex);
        buf.writeUtf(targetDisplayName, 64);
        buf.writeVarInt(durationTicks);
    }

    public static AmmoBoxRefillMarkerS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos ammoBoxPos = buf.readBlockPos();
        ResourceLocation itemId = buf.readResourceLocation();
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        ItemStack ammoStack = new ItemStack(item, buf.readVarInt());
        int damageValue = buf.readVarInt();
        if (ammoStack.isDamageableItem()) {
            ammoStack.setDamageValue(damageValue);
        }
        return new AmmoBoxRefillMarkerS2CPacket(
                ammoBoxPos,
                ammoStack,
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(64),
                buf.readVarInt()
        );
    }

    public static void handle(AmmoBoxRefillMarkerS2CPacket pkt, IPayloadContext context) {
        ClientHandler.handle(pkt, context);
    }

    // Keep renderer classes out of the payload class loaded by dedicated servers.
    private static final class ClientHandler {
        private static void handle(AmmoBoxRefillMarkerS2CPacket pkt, IPayloadContext context) {
            context.enqueueWork(() -> AmmoBoxRefillMarkerRenderer.showMarker(
                    pkt.ammoBoxPos,
                    pkt.ammoStack,
                    pkt.amount,
                    pkt.targetIndex,
                    pkt.targetDisplayName,
                    pkt.durationTicks
            ));
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
