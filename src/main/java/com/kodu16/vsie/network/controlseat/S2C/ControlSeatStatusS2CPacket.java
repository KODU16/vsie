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
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

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
        pkt.handle(() -> new NetworkEvent.Context(context));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            ControlSeatClientData clientData = ClientDataManager.getClientDataForSeat(player, pos, seatEntityId);
            if (clientData == null) {
                return;
            }
            clientData.energyavalible = energyavalible;
            clientData.energytotal = energytotal;

            clientData.fuelavalible = fuelavalible;
            clientData.fueltotal = fueltotal;
            clientData.e710avalible = e710avalible;
            clientData.warpE710CostMb = warpE710CostMb;
            clientData.warpE710Insufficient = warpE710Insufficient;

            clientData.shieldon = shieldon;
            clientData.shieldavalible = shieldavalible;
            clientData.shieldtotal = shieldtotal;
            clientData.isShieldOverloaded = shieldOverloaded;

            clientData.isforceassiston = forceassiston;
            clientData.istorqueassiston = torqueassiston;
            clientData.isForceAssistSuppressedByAccelerator = forceAssistSuppressedByAccelerator;
            clientData.isantigravityon = antigravityon;
            clientData.isAutoLevelOn = autoLevelOn;
            clientData.isWarpPreparing = warpPreparing;
            clientData.hasPendingWarpTeleport = pendingWarpTeleport;
            clientData.warpTargetName = warpTargetName;
            clientData.warpAlignmentControlX = warpAlignmentControlX;
            clientData.warpAlignmentControlY = warpAlignmentControlY;
            clientData.activeWeaponHudInfos = new ArrayList<>(activeWeaponHudInfos);
        });
        ctx.get().setPacketHandled(true);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
