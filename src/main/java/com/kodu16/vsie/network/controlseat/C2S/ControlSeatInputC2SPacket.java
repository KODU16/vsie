package com.kodu16.vsie.network.controlseat.C2S;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

public class ControlSeatInputC2SPacket implements CustomPacketPayload {
    // Function: NeoForge 1.21.1 payload id and stream codec registration entry.
    public static final CustomPacketPayload.Type<ControlSeatInputC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_c2s_controlseatinputc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatInputC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ControlSeatInputC2SPacket::encode, ControlSeatInputC2SPacket::decode);

    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final UUID seatEntityId;
    public final int keys;
    public final boolean isviewlock;
    // Function: client-side manual aim ray result used directly by server-side turret targeting.
    public final double aimTargetX;
    public final double aimTargetY;
    public final double aimTargetZ;

    public ControlSeatInputC2SPacket(BlockPos pos, UUID seatEntityId, int keys, boolean isviewlock, double aimTargetX, double aimTargetY, double aimTargetZ) {
        this.pos = pos;
        this.seatEntityId = seatEntityId;
        this.keys = keys;
        this.isviewlock = isviewlock;
        this.aimTargetX = aimTargetX;
        this.aimTargetY = aimTargetY;
        this.aimTargetZ = aimTargetZ;
    }

    public static void encode(ControlSeatInputC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeUUID(pkt.seatEntityId);
        buf.writeVarInt(pkt.keys);
        buf.writeBoolean(pkt.isviewlock);
        buf.writeDouble(pkt.aimTargetX);
        buf.writeDouble(pkt.aimTargetY);
        buf.writeDouble(pkt.aimTargetZ);
    }

    public static ControlSeatInputC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID seatEntityId = buf.readUUID();
        int keys = buf.readVarInt();
        boolean isviewlock = buf.readBoolean();
        double aimTargetX = buf.readDouble();
        double aimTargetY = buf.readDouble();
        double aimTargetZ = buf.readDouble();
        return new ControlSeatInputC2SPacket(pos, seatEntityId, keys, isviewlock, aimTargetX, aimTargetY, aimTargetZ);
    }

    // Function: NeoForge handler entry that reuses the existing Supplier<NetworkEvent.Context> path.
    public static void handle(ControlSeatInputC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new NetworkEvent.Context(context));
    }

    public static void handle(ControlSeatInputC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            int keys = pkt.keys;
            ControlSeatBlockEntity controlSeat = ControlSeatPacketResolver.resolve(sender, pos, pkt.seatEntityId);
            if (controlSeat == null) {
                return;
            }

            ControlSeatServerData serverData = controlSeat.getServerData();
            if ((keys & KeysInput.CHANNEL1) != 0) {
                serverData.channel1 = !serverData.getChannel1();
            }
            if ((keys & KeysInput.CHANNEL2) != 0) {
                serverData.channel2 = !serverData.getChannel2();
            }
            if ((keys & KeysInput.CHANNEL3) != 0) {
                serverData.channel3 = !serverData.getChannel3();
            }
            if ((keys & KeysInput.CHANNEL4) != 0) {
                serverData.channel4 = !serverData.getChannel4();
            }
            serverData.refreshWeaponChannelEncode();

            if ((keys & KeysInput.SWITCHENEMY) != 0 && !serverData.enemyshipsData.isEmpty()) {
                // Function: cycle through detected enemy targets; number keys are reserved for weapon channels.
                int index = serverData.lockedenemyindex + 1;
                serverData.lockedenemyindex = index % serverData.enemyshipsData.size();
            }
            if ((keys & KeysInput.TOGGLESHIELD) != 0) {
                // Function: overloaded shields stay armed in HUD but cannot be toggled until cooldown ends.
                if (serverData.shieldcooldowntime <= 0.0D) {
                    serverData.isshieldon = !serverData.isshieldon;
                } else {
                    serverData.isshieldon = true;
                }
            }
            if ((keys & KeysInput.TOGGLEFORCEASSIST) != 0) {
                // Function: active rail acceleration can hard-disable force assist and reject re-enable attempts for that tick.
                if (!serverData.isForceAssistSuppressedByAccelerator) {
                    serverData.isforceassiston = !serverData.isforceassiston;
                } else {
                    serverData.isforceassiston = false;
                }
            }
            if ((keys & KeysInput.TOGGLETORQUEASSIST) != 0) {
                serverData.istorqueassiston = !serverData.istorqueassiston;
            }
            if ((keys & KeysInput.TOGGLEANTIGRAVITY) != 0) {
                serverData.isantigravityon = !serverData.isantigravityon;
            }
            if ((keys & KeysInput.TOGGLEAUTOLEVEL) != 0) {
                // Function: warp alignment temporarily owns pitch/roll correction, so auto-level cannot be toggled until warp finishes.
                if (!serverData.isWarpPreparing && !serverData.hasPendingWarpTeleport) {
                    // Function: auto-level persists like anti-gravity so an empty seat can keep stabilizing.
                    serverData.isAutoLevelOn = !serverData.isAutoLevelOn;
                }
            }

            serverData.isviewlocked = pkt.isviewlock;
            // Function: cache the uploaded manual aim point for heavy turret targetPos use.
            serverData.manualAimTargetX = pkt.aimTargetX;
            serverData.manualAimTargetY = pkt.aimTargetY;
            serverData.manualAimTargetZ = pkt.aimTargetZ;
            controlSeat.setChanged();
        });
        ctx.setPacketHandled(true);
    }

    /** Input bit definitions shared by the client sender and server handler. */
    public static final class KeysInput {
        public static final int CHANNEL1 = 1 << 0;
        public static final int CHANNEL2 = 1 << 1;
        public static final int CHANNEL3 = 1 << 2;
        public static final int CHANNEL4 = 1 << 3;
        public static final int SWITCHENEMY = 1 << 4;
        public static final int TOGGLESHIELD = 1 << 5;
        public static final int TOGGLEFORCEASSIST = 1 << 6;
        public static final int TOGGLETORQUEASSIST = 1 << 7;
        public static final int TOGGLEANTIGRAVITY = 1 << 8;
        public static final int TOGGLEAUTOLEVEL = 1 << 9;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
