package com.kodu16.vsie.network.fx;

import com.kodu16.vsie.utility.vsieFxHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class FxPositionS2CPacket implements CustomPacketPayload {
    // Plays a Photon FX at an exact world position with transform data.
    public static final CustomPacketPayload.Type<FxPositionS2CPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "fx_position_s2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, FxPositionS2CPacket> STREAM_CODEC =
            CustomPacketPayload.codec(FxPositionS2CPacket::encode, FxPositionS2CPacket::decode);

    private final ResourceLocation fx;
    private final double x;
    private final double y;
    private final double z;
    private final double velocityX;
    private final double velocityY;
    private final double velocityZ;
    private final Quaternionf rotation;
    private final Vector3f scale;
    private final boolean forceDead;
    private final boolean allowMulti;

    public FxPositionS2CPacket(ResourceLocation fx, double x, double y, double z,
                               Quaternionf rotation, Vector3f scale, boolean forceDead) {
        this(fx, x, y, z, 0.0D, 0.0D, 0.0D, rotation, scale, forceDead, false);
    }

    public FxPositionS2CPacket(ResourceLocation fx, double x, double y, double z,
                               double velocityX, double velocityY, double velocityZ,
                               Quaternionf rotation, Vector3f scale, boolean forceDead) {
        this(fx, x, y, z, velocityX, velocityY, velocityZ, rotation, scale, forceDead, false);
    }

    public FxPositionS2CPacket(ResourceLocation fx, double x, double y, double z,
                               double velocityX, double velocityY, double velocityZ,
                               Quaternionf rotation, Vector3f scale, boolean forceDead, boolean allowMulti) {
        this.fx = fx;
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.rotation = new Quaternionf(rotation);
        this.scale = new Vector3f(scale);
        this.forceDead = forceDead;
        this.allowMulti = allowMulti;
    }

    public ResourceLocation getFx() {
        return fx;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getVelocityZ() {
        return velocityZ;
    }

    public Quaternionf getRotation() {
        return new Quaternionf(rotation);
    }

    public Vector3f getScale() {
        return new Vector3f(scale);
    }

    public boolean isForceDead() {
        return forceDead;
    }

    public boolean isAllowMulti() {
        return allowMulti;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(fx);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeDouble(velocityX);
        buffer.writeDouble(velocityY);
        buffer.writeDouble(velocityZ);
        buffer.writeFloat(rotation.x);
        buffer.writeFloat(rotation.y);
        buffer.writeFloat(rotation.z);
        buffer.writeFloat(rotation.w);
        buffer.writeFloat(scale.x);
        buffer.writeFloat(scale.y);
        buffer.writeFloat(scale.z);
        buffer.writeBoolean(forceDead);
        // Function: let selected Photon position FX instances overlap instead of sharing the per-block cache gate.
        buffer.writeBoolean(allowMulti);
    }

    public static FxPositionS2CPacket decode(FriendlyByteBuf buf) {
        ResourceLocation fx = buf.readResourceLocation();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        double velocityX = buf.readDouble();
        double velocityY = buf.readDouble();
        double velocityZ = buf.readDouble();
        Quaternionf rotation = new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        Vector3f scale = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        boolean forceDead = buf.readBoolean();
        boolean allowMulti = buf.readBoolean();
        return new FxPositionS2CPacket(fx, x, y, z, velocityX, velocityY, velocityZ, rotation, scale, forceDead, allowMulti);
    }

    // NeoForge 1.21.1 client payload entry point.
    public static void handle(FxPositionS2CPacket pkt, IPayloadContext context) {
        pkt.handle(() -> new NetworkEvent.Context(context));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> vsieFxHelper.clientTriggerPositionEffectFx(this));
        ctx.get().setPacketHandled(true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
