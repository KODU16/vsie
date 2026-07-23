package com.kodu16.vsie.network.IFF;

import com.kodu16.vsie.utility.ItemStackNbt;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class IFFC2SPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<IFFC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "iff_iffc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, IFFC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(IFFC2SPacket::encode, IFFC2SPacket::decode);

    public final String enemy;
    public final String ally;

    public IFFC2SPacket(String teamA, String teamB) {
        this.enemy = teamA != null ? teamA : "";
        this.ally = teamB != null ? teamB : "";
    }

    public static void encode(IFFC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.enemy, 64);
        buf.writeUtf(msg.ally, 64);
    }

    public static IFFC2SPacket decode(FriendlyByteBuf buf) {
        String enemy = buf.readUtf(64);
        String ally = buf.readUtf(64);
        return new IFFC2SPacket(enemy, ally);
    }

    public static void handle(IFFC2SPacket msg, IPayloadContext context) {
        handle(msg, () -> new NetworkEvent.Context(context));
    }

    public static void handle(IFFC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                return;
            }

            ItemStackNbt.update(stack, tag -> {
                tag.putString("enemy", msg.enemy);
                tag.putString("ally", msg.ally);
            });

            player.getInventory().setChanged();
            player.sendSystemMessage(Component.translatable("message.vsie.iff.updated"));
            player.sendSystemMessage(Component.translatable("message.vsie.iff.enemy", msg.enemy));
            player.sendSystemMessage(Component.translatable("message.vsie.iff.ally", msg.ally));
        });

        ctx.setPacketHandled(true);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
