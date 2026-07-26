package com.kodu16.vsie.network.controlseat.S2C;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3d;

import java.util.UUID;

public class ControlSeatS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ControlSeatS2CPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_s2c_controlseats2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatS2CPacket> STREAM_CODEC = CustomPacketPayload.codec(ControlSeatS2CPacket::write, ControlSeatS2CPacket::decode);

    private final BlockPos pos;
    private final UUID seatEntityId;
    private final Vector3d shipFacing;
    private final Vector3d shipUp;
    public String enemy;
    public String ally;
    public String lockedenemyslug;
    public int throttle;
    public double shipSpeed;
    public Vector3d structureCenterWorld;
    public Vector3d structureVelocityWorld;
    public double seatGForce;
    public boolean isViewLocked;

    public ControlSeatS2CPacket(BlockPos pos, UUID seatEntityId, Vector3d shipFacing, Vector3d shipUp,
                                String enemy, String ally, String lockedenemyslug, int throttle,
                                boolean isViewLocked, double shipSpeed, Vector3d structureCenterWorld,
                                Vector3d structureVelocityWorld, double seatGForce) {
        this.pos = pos;
        this.seatEntityId = seatEntityId;
        this.shipFacing = shipFacing;
        this.shipUp = shipUp;
        this.enemy = enemy;
        this.ally = ally;
        this.lockedenemyslug = lockedenemyslug;
        this.throttle = throttle;
        this.isViewLocked = isViewLocked;
        this.shipSpeed = shipSpeed;
        this.structureCenterWorld = structureCenterWorld;
        this.structureVelocityWorld = structureVelocityWorld;
        this.seatGForce = seatGForce;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(seatEntityId);
        buf.writeDouble(shipFacing.x);
        buf.writeDouble(shipFacing.y);
        buf.writeDouble(shipFacing.z);
        buf.writeDouble(shipUp.x);
        buf.writeDouble(shipUp.y);
        buf.writeDouble(shipUp.z);
        buf.writeUtf(enemy, 64);
        buf.writeUtf(ally, 64);
        buf.writeUtf(lockedenemyslug, 64);
        buf.writeInt(throttle);
        buf.writeBoolean(isViewLocked);
        buf.writeDouble(shipSpeed);
        buf.writeDouble(structureCenterWorld.x);
        buf.writeDouble(structureCenterWorld.y);
        buf.writeDouble(structureCenterWorld.z);
        buf.writeDouble(structureVelocityWorld.x);
        buf.writeDouble(structureVelocityWorld.y);
        buf.writeDouble(structureVelocityWorld.z);
        buf.writeDouble(seatGForce);
    }

    public static ControlSeatS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID seatEntityId = buf.readUUID();
        Vector3d shipFacing = new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vector3d shipUp = new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        String enemy = buf.readUtf(64);
        String ally = buf.readUtf(64);
        String lockedenemyslug = buf.readUtf(64);
        int throttle = buf.readInt();
        boolean isViewLocked = buf.readBoolean();
        double shipSpeed = buf.readDouble();
        Vector3d structureCenterWorld = new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vector3d structureVelocityWorld = new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double seatGForce = buf.readDouble();
        return new ControlSeatS2CPacket(pos, seatEntityId, shipFacing, shipUp, enemy, ally, lockedenemyslug,
                throttle, isViewLocked, shipSpeed, structureCenterWorld, structureVelocityWorld, seatGForce);
    }

    public static void handle(ControlSeatS2CPacket pkt, IPayloadContext context) {
        ClientHandler.handle(pkt, context);
    }

    // Keep client-only classes in a separate class file so dedicated servers can load the payload.
    private static final class ClientHandler {
        private static void handle(ControlSeatS2CPacket pkt, IPayloadContext context) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;
                ControlSeatClientData clientData =
                        ClientDataManager.getClientDataForSeat(player, pkt.pos, pkt.seatEntityId);
                if (clientData == null) {
                    return;
                }
                clientData.updateShipVectors(pkt.shipFacing, pkt.shipUp);
                clientData.setUserUUID(player.getUUID());

                clientData.enemy = pkt.enemy;
                clientData.ally = pkt.ally;
                clientData.lockedenemyslug = pkt.lockedenemyslug;
                clientData.throttle = pkt.throttle;
                clientData.applyServerViewLock(pkt.isViewLocked);
                clientData.shipSpeed = pkt.shipSpeed;
                clientData.structureCenterWorld = new Vector3d(pkt.structureCenterWorld);
                // Velocity is projected by the HUD as an independent world-space cue.
                clientData.structureVelocityWorld = new Vector3d(pkt.structureVelocityWorld);
                clientData.seatGForce = pkt.seatGForce;
            });
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
