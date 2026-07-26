package com.kodu16.vsie.network.fx;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.utility.vsieFxHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;


public class FxEntityS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FxEntityS2CPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "fx_fxentitys2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, FxEntityS2CPacket> STREAM_CODEC = CustomPacketPayload.codec(FxEntityS2CPacket::encode, FxEntityS2CPacket::decode);

    private final ResourceLocation fx;
    private final int entityID;
    private final boolean forceDead;
    private final boolean stop;
    private final Vector3f offset;
    private final Vector3f scale;

    public ResourceLocation getFx() {
        return fx;
    }

    public int getEntityID() {
        return entityID;
    }

    public boolean isForceDead() {
        return forceDead;
    }

    public boolean getForceDead() {
        return forceDead;
    }

    public boolean isStop() {
        return stop;
    }

    public Vector3f getOffset() {
        return new Vector3f(offset);
    }

    public Vector3f getScale() {
        return new Vector3f(scale);
    }

    public FxEntityS2CPacket(ResourceLocation fx, int entityID, Boolean forceDead) {
        this(fx, entityID, forceDead, false, new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));
    }

    public FxEntityS2CPacket(ResourceLocation fx, int entityID, Boolean forceDead, Vector3f offset, Vector3f scale) {
        this(fx, entityID, forceDead, false, offset, scale);
    }

    private FxEntityS2CPacket(ResourceLocation fx, int entityID, boolean forceDead, boolean stop,
                              Vector3f offset, Vector3f scale) {
        this.fx = fx;
        this.entityID = entityID;
        this.forceDead = forceDead;
        this.stop = stop;
        this.offset = new Vector3f(offset);
        this.scale = new Vector3f(scale);
    }

    // Function: stop one named FX without removing the entity it follows.
    public static FxEntityS2CPacket stop(ResourceLocation fx, int entityID) {
        return new FxEntityS2CPacket(fx, entityID, true, true, new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(fx);
        buffer.writeInt(entityID);
        buffer.writeBoolean(forceDead);
        buffer.writeBoolean(stop);
        buffer.writeFloat(offset.x);
        buffer.writeFloat(offset.y);
        buffer.writeFloat(offset.z);
        buffer.writeFloat(scale.x);
        buffer.writeFloat(scale.y);
        buffer.writeFloat(scale.z);
    }

    public static FxEntityS2CPacket decode(FriendlyByteBuf buf) {
        ResourceLocation rl = buf.readResourceLocation();
        int entityid = buf.readInt();
        boolean forcedead = buf.readBoolean();
        boolean stop = buf.readBoolean();
        Vector3f offset = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        Vector3f scale = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        return new FxEntityS2CPacket(rl, entityid, forcedead, stop, offset, scale);
    }

    public static void handle(FxEntityS2CPacket pkt, IPayloadContext context) {
        ClientHandler.handle(pkt, context);
    }

    // Keep Photon client classes out of the payload class loaded by dedicated servers.
    private static final class ClientHandler {
        private static void handle(FxEntityS2CPacket pkt, IPayloadContext context) {
            context.enqueueWork(() -> vsieFxHelper.clientTriggerEntityFx(pkt));
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
