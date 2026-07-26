package com.kodu16.vsie.network.controlseat.S2C;

import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ControlSeatStatusS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ControlSeatStatusS2CPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_s2c_controlseatstatuss2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatStatusS2CPacket> STREAM_CODEC = CustomPacketPayload.codec(ControlSeatStatusS2CPacket::write, ControlSeatStatusS2CPacket::decode);

    private final BlockPos pos;
    private final UUID seatEntityId;
    public int energyavalible;
    public int energytotal;
    public int fuelavalible;
    public int fueltotal;
    public int e710avalible;
    public int warpE710CostMb;
    public boolean warpE710Insufficient;
    public boolean shieldon;
    public int shieldavalible;
    public int shieldtotal;
    public boolean shieldOverloaded;
    public boolean forceassiston;
    public boolean torqueassiston;
    public boolean forceAssistSuppressedByAccelerator;
    public boolean antigravityon;
    public boolean autoLevelOn;
    public boolean warpPreparing;
    public boolean pendingWarpTeleport;
    public String warpTargetName;
    public double warpAlignmentControlX;
    public double warpAlignmentControlY;
    public List<ActiveWeaponHudInfo> activeWeaponHudInfos;

    public ControlSeatStatusS2CPacket(BlockPos pos, UUID seatEntityId,
                                      int energyavalible, int energytotal,
                                      int fuelavalible, int fueltotal,
                                      int e710avalible, int warpE710CostMb, boolean warpE710Insufficient,
                                      boolean shieldon, int shieldavalible, int shieldtotal, boolean shieldOverloaded,
                                      boolean forceassiston, boolean torqueassiston, boolean forceAssistSuppressedByAccelerator,
                                      boolean antigravityon, boolean autoLevelOn,
                                      boolean warpPreparing, boolean pendingWarpTeleport, String warpTargetName,
                                      double warpAlignmentControlX, double warpAlignmentControlY,
                                      List<ActiveWeaponHudInfo> activeWeaponHudInfos) {
        this.pos = pos;
        this.seatEntityId = seatEntityId;
        this.energyavalible = energyavalible;
        this.energytotal = energytotal;
        this.fuelavalible = fuelavalible;
        this.fueltotal = fueltotal;
        this.e710avalible = e710avalible;
        this.warpE710CostMb = warpE710CostMb;
        this.warpE710Insufficient = warpE710Insufficient;
        this.shieldon = shieldon;
        this.shieldavalible = shieldavalible;
        this.shieldtotal = shieldtotal;
        this.shieldOverloaded = shieldOverloaded;
        this.forceassiston = forceassiston;
        this.torqueassiston = torqueassiston;
        this.forceAssistSuppressedByAccelerator = forceAssistSuppressedByAccelerator;
        this.antigravityon = antigravityon;
        this.autoLevelOn = autoLevelOn;
        this.warpPreparing = warpPreparing;
        this.pendingWarpTeleport = pendingWarpTeleport;
        this.warpTargetName = warpTargetName == null ? "" : warpTargetName;
        this.warpAlignmentControlX = warpAlignmentControlX;
        this.warpAlignmentControlY = warpAlignmentControlY;
        this.activeWeaponHudInfos = new ArrayList<>(activeWeaponHudInfos);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUUID(seatEntityId);
        buf.writeInt(energyavalible);
        buf.writeInt(energytotal);
        buf.writeInt(fuelavalible);
        buf.writeInt(fueltotal);
        buf.writeInt(e710avalible);
        buf.writeInt(warpE710CostMb);
        buf.writeBoolean(warpE710Insufficient);
        buf.writeBoolean(shieldon);
        buf.writeInt(shieldavalible);
        buf.writeInt(shieldtotal);
        buf.writeBoolean(shieldOverloaded);
        buf.writeBoolean(forceassiston);
        buf.writeBoolean(torqueassiston);
        buf.writeBoolean(forceAssistSuppressedByAccelerator);
        buf.writeBoolean(antigravityon);
        buf.writeBoolean(autoLevelOn);
        buf.writeBoolean(warpPreparing);
        buf.writeBoolean(pendingWarpTeleport);
        buf.writeUtf(warpTargetName);
        buf.writeDouble(warpAlignmentControlX);
        buf.writeDouble(warpAlignmentControlY);
        buf.writeInt(activeWeaponHudInfos.size());
        for (ActiveWeaponHudInfo info : activeWeaponHudInfos) {
            buf.writeUtf(info.displayName);
            buf.writeInt(info.currentTick);
            buf.writeInt(info.maxCooldown);
            buf.writeBoolean(info.remainingCooldown);
            buf.writeBoolean(info.fireReady);
        }
    }

    public static ControlSeatStatusS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID seatEntityId = buf.readUUID();
        int energyavalible = buf.readInt();
        int energytotal = buf.readInt();
        int fuelavalible = buf.readInt();
        int fueltotal = buf.readInt();
        int e710avalible = buf.readInt();
        int warpE710CostMb = buf.readInt();
        boolean warpE710Insufficient = buf.readBoolean();
        boolean shieldon = buf.readBoolean();
        int shieldavalible = buf.readInt();
        int shieldtotal = buf.readInt();
        boolean shieldOverloaded = buf.readBoolean();
        boolean forceassiston = buf.readBoolean();
        boolean torqueassiston = buf.readBoolean();
        boolean forceAssistSuppressedByAccelerator = buf.readBoolean();
        boolean antigravityon = buf.readBoolean();
        boolean autoLevelOn = buf.readBoolean();
        boolean warpPreparing = buf.readBoolean();
        boolean pendingWarpTeleport = buf.readBoolean();
        String warpTargetName = buf.readUtf();
        double warpAlignmentControlX = buf.readDouble();
        double warpAlignmentControlY = buf.readDouble();
        int weaponInfoSize = buf.readInt();
        List<ActiveWeaponHudInfo> activeWeaponHudInfos = new ArrayList<>();
        for (int i = 0; i < weaponInfoSize; i++) {
            String displayName = buf.readUtf();
            int currentTick = buf.readInt();
            int maxCooldown = buf.readInt();
            boolean remainingCooldown = buf.readBoolean();
            boolean fireReady = buf.readBoolean();
            activeWeaponHudInfos.add(new ActiveWeaponHudInfo(displayName, currentTick, maxCooldown, remainingCooldown, fireReady));
        }
        return new ControlSeatStatusS2CPacket(pos, seatEntityId, energyavalible, energytotal, fuelavalible, fueltotal, e710avalible, warpE710CostMb, warpE710Insufficient, shieldon, shieldavalible, shieldtotal, shieldOverloaded, forceassiston, torqueassiston, forceAssistSuppressedByAccelerator, antigravityon, autoLevelOn, warpPreparing, pendingWarpTeleport, warpTargetName, warpAlignmentControlX, warpAlignmentControlY, activeWeaponHudInfos);
    }

    public static void handle(ControlSeatStatusS2CPacket pkt, IPayloadContext context) {
        ClientHandler.handle(pkt, context);
    }

    // Keep client-only classes in a separate class file so dedicated servers can load the payload.
    private static final class ClientHandler {
        private static void handle(ControlSeatStatusS2CPacket pkt, IPayloadContext context) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;
                ControlSeatClientData clientData =
                        ClientDataManager.getClientDataForSeat(player, pkt.pos, pkt.seatEntityId);
                if (clientData == null) {
                    return;
                }
                clientData.energyavalible = pkt.energyavalible;
                clientData.energytotal = pkt.energytotal;

                clientData.fuelavalible = pkt.fuelavalible;
                clientData.fueltotal = pkt.fueltotal;
                clientData.e710avalible = pkt.e710avalible;
                clientData.warpE710CostMb = pkt.warpE710CostMb;
                clientData.warpE710Insufficient = pkt.warpE710Insufficient;

                clientData.shieldon = pkt.shieldon;
                clientData.shieldavalible = pkt.shieldavalible;
                clientData.shieldtotal = pkt.shieldtotal;
                clientData.isShieldOverloaded = pkt.shieldOverloaded;

                clientData.isforceassiston = pkt.forceassiston;
                clientData.istorqueassiston = pkt.torqueassiston;
                clientData.isForceAssistSuppressedByAccelerator = pkt.forceAssistSuppressedByAccelerator;
                clientData.isantigravityon = pkt.antigravityon;
                clientData.isAutoLevelOn = pkt.autoLevelOn;
                clientData.isWarpPreparing = pkt.warpPreparing;
                clientData.hasPendingWarpTeleport = pkt.pendingWarpTeleport;
                clientData.warpTargetName = pkt.warpTargetName;
                clientData.warpAlignmentControlX = pkt.warpAlignmentControlX;
                clientData.warpAlignmentControlY = pkt.warpAlignmentControlY;
                clientData.activeWeaponHudInfos = new ArrayList<>(pkt.activeWeaponHudInfos);
            });
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
