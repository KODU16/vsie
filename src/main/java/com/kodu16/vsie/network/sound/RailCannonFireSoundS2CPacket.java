package com.kodu16.vsie.network.sound;

import com.kodu16.vsie.content.weapon.electro_magnet_rail_cannon.client.RailCannonFireSoundManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RailCannonFireSoundS2CPacket(double x, double y, double z, float range)
        implements CustomPacketPayload {
    public static final Type<RailCannonFireSoundS2CPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("vsie", "rail_cannon_fire_sound_s2c")
    );
    public static final StreamCodec<FriendlyByteBuf, RailCannonFireSoundS2CPacket> STREAM_CODEC =
            CustomPacketPayload.codec(RailCannonFireSoundS2CPacket::write, RailCannonFireSoundS2CPacket::decode);

    private void write(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(range);
    }

    private static RailCannonFireSoundS2CPacket decode(FriendlyByteBuf buf) {
        return new RailCannonFireSoundS2CPacket(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat()
        );
    }

    public static void handle(RailCannonFireSoundS2CPacket packet, IPayloadContext context) {
        ClientHandler.handle(packet, context);
    }

    // Keep sound engine classes out of the payload class loaded by dedicated servers.
    private static final class ClientHandler {
        private static void handle(RailCannonFireSoundS2CPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> RailCannonFireSoundManager.play(
                    packet.x, packet.y, packet.z, packet.range
            ));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
