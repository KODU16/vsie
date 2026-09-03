package com.kodu16.vsie.network.turret;

import com.kodu16.vsie.content.aeroie_custom.CustomTurretBlockEntity;
import com.kodu16.vsie.content.aeroie_custom.CustomThrusterBlockEntity;
import com.kodu16.vsie.content.aeroie_custom.CustomWeaponBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3d;

import java.util.function.Supplier;

/** Sends Geckolib-sampled turret muzzle positions to the authoritative server. */
public class TurretFirePointC2SPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TurretFirePointC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "turret_turretfirepointc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, TurretFirePointC2SPacket> STREAM_CODEC =
            CustomPacketPayload.codec(TurretFirePointC2SPacket::encode, TurretFirePointC2SPacket::decode);

    public final BlockPos pos;
    public final int firepointIndex;
    public final Vector3d postofire;
    public final Vector3d direction;

    public TurretFirePointC2SPacket(BlockPos pos, Vector3d postofire) {
        this(pos, 0, postofire);
    }

    public TurretFirePointC2SPacket(BlockPos pos, int firepointIndex, Vector3d postofire) {
        this(pos, firepointIndex, postofire, new Vector3d(0.0D, 0.0D, 1.0D));
    }

    public TurretFirePointC2SPacket(BlockPos pos, int firepointIndex, Vector3d postofire, Vector3d direction) {
        this.pos = pos;
        this.firepointIndex = firepointIndex;
        this.postofire = postofire;
        this.direction = direction == null ? new Vector3d(0.0D, 0.0D, 1.0D) : direction;
    }

    public static void encode(TurretFirePointC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.firepointIndex);
        buffer.writeDouble(packet.postofire.x);
        buffer.writeDouble(packet.postofire.y);
        buffer.writeDouble(packet.postofire.z);
        buffer.writeDouble(packet.direction.x);
        buffer.writeDouble(packet.direction.y);
        buffer.writeDouble(packet.direction.z);
    }

    public static TurretFirePointC2SPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int firepointIndex = buffer.readVarInt();
        Vector3d firepoint = new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vector3d direction = new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new TurretFirePointC2SPacket(pos, firepointIndex, firepoint, direction);
    }

    public static void handle(TurretFirePointC2SPacket packet, IPayloadContext context) {
        handle(packet, () -> new NetworkEvent.Context(context));
    }

    public static void handle(TurretFirePointC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            ServerLevel level = sender.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos);
            if (blockEntity instanceof CustomTurretBlockEntity customTurret && packet.firepointIndex > 0) {
                // Function: custom turrets cache every muzzle separately for independent hit tests.
                customTurret.setFirePoint(packet.firepointIndex, packet.postofire, packet.direction);
            } else if (blockEntity instanceof CustomWeaponBlockEntity customWeapon && packet.firepointIndex > 0) {
                // Function: custom weapons use the same GeckoLib-sampled firepoint positions as custom turrets.
                customWeapon.setFirePoint(packet.firepointIndex, packet.postofire, packet.direction);
            } else if (blockEntity instanceof CustomThrusterBlockEntity customThruster && packet.firepointIndex > 0) {
                // Function: custom thrusters reuse this point-sync payload for authored flamepoint positions.
                customThruster.setFlamePoint(packet.firepointIndex, packet.postofire, packet.direction);
            } else if (blockEntity instanceof AbstractTurretBlockEntity turret) {
                turret.setFirePoint(packet.postofire);
            }
        });
        context.setPacketHandled(true);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
