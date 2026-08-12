package com.kodu16.vsie.registries;

import com.kodu16.vsie.network.IFF.IFFC2SPacket;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatC2SPacket;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatInputC2SPacket;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatWarpCancelC2SPacket;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatWarpTargetC2SPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatInputS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatStatusS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.NearbyShipsS2CPacket;
import com.kodu16.vsie.network.custom_turret.SelectCustomTurretC2SPacket;
import com.kodu16.vsie.network.fuel.SyncThrusterFuelsPacket;
import com.kodu16.vsie.network.fx.FxBlockS2CPacket;
import com.kodu16.vsie.network.fx.FxEntityS2CPacket;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.network.misc.EnemyCoreSettingsC2SPacket;
import com.kodu16.vsie.network.misc.EnemyCannonSettingsC2SPacket;
import com.kodu16.vsie.network.misc.EnemyAutocannonSettingsC2SPacket;
import com.kodu16.vsie.network.rail.ElectroMagnetRailCoreDetectC2SPacket;
import com.kodu16.vsie.network.screen.ScreenC2SPacket;
import com.kodu16.vsie.network.screen.ScreentypeC2SPacket;
import com.kodu16.vsie.network.sound.RailCannonFireSoundS2CPacket;
import com.kodu16.vsie.network.storage.AmmoBoxRefillMarkerS2CPacket;
import com.kodu16.vsie.network.thruster.ThrusterS2CPacket;
import com.kodu16.vsie.network.thruster.ThrusterLimitC2SPacket;
import com.kodu16.vsie.network.thruster.VectorThrusterS2CPacket;
import com.kodu16.vsie.network.turret.HeavyTurretC2SPacket;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
import com.kodu16.vsie.network.turret.TurretDefaultSpinC2SPacket;
import com.kodu16.vsie.network.turret.TurretFirePointC2SPacket;
import com.kodu16.vsie.network.weapon.WeaponC2SPacket;
import com.kodu16.vsie.network.weapon.WeaponDisplayNameC2SPacket;
import com.kodu16.vsie.network.weapon.WeaponLaunchIntervalC2SPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    public static final String PROTOCOL = "1";

    private ModNetworking() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        registrar.playToServer(ControlSeatC2SPacket.TYPE, ControlSeatC2SPacket.STREAM_CODEC, ControlSeatC2SPacket::handle);
        registrar.playToServer(ControlSeatInputC2SPacket.TYPE, ControlSeatInputC2SPacket.STREAM_CODEC, ControlSeatInputC2SPacket::handle);
        registrar.playToServer(ControlSeatWarpTargetC2SPacket.TYPE, ControlSeatWarpTargetC2SPacket.STREAM_CODEC, ControlSeatWarpTargetC2SPacket::handle);
        registrar.playToServer(ControlSeatWarpCancelC2SPacket.TYPE, ControlSeatWarpCancelC2SPacket.STREAM_CODEC, ControlSeatWarpCancelC2SPacket::handle);
        registrar.playToServer(TurretC2SPacket.TYPE, TurretC2SPacket.STREAM_CODEC, TurretC2SPacket::handle);
        registrar.playToServer(TurretDefaultSpinC2SPacket.TYPE, TurretDefaultSpinC2SPacket.STREAM_CODEC, TurretDefaultSpinC2SPacket::handle);
        registrar.playToServer(TurretFirePointC2SPacket.TYPE, TurretFirePointC2SPacket.STREAM_CODEC, TurretFirePointC2SPacket::handle);
        registrar.playToServer(HeavyTurretC2SPacket.TYPE, HeavyTurretC2SPacket.STREAM_CODEC, HeavyTurretC2SPacket::handle);
        registrar.playToServer(WeaponC2SPacket.TYPE, WeaponC2SPacket.STREAM_CODEC, WeaponC2SPacket::handle);
        registrar.playToServer(WeaponDisplayNameC2SPacket.TYPE, WeaponDisplayNameC2SPacket.STREAM_CODEC, WeaponDisplayNameC2SPacket::handle);
        registrar.playToServer(WeaponLaunchIntervalC2SPacket.TYPE, WeaponLaunchIntervalC2SPacket.STREAM_CODEC, WeaponLaunchIntervalC2SPacket::handle);
        registrar.playToServer(IFFC2SPacket.TYPE, IFFC2SPacket.STREAM_CODEC, IFFC2SPacket::handle);
        registrar.playToServer(ScreenC2SPacket.TYPE, ScreenC2SPacket.STREAM_CODEC, ScreenC2SPacket::handle);
        registrar.playToServer(ScreentypeC2SPacket.TYPE, ScreentypeC2SPacket.STREAM_CODEC, ScreentypeC2SPacket::handle);
        registrar.playToServer(ElectroMagnetRailCoreDetectC2SPacket.TYPE, ElectroMagnetRailCoreDetectC2SPacket.STREAM_CODEC, ElectroMagnetRailCoreDetectC2SPacket::handle);
        registrar.playToServer(ThrusterLimitC2SPacket.TYPE, ThrusterLimitC2SPacket.STREAM_CODEC, ThrusterLimitC2SPacket::handle);
        registrar.playToServer(EnemyCoreSettingsC2SPacket.TYPE, EnemyCoreSettingsC2SPacket.STREAM_CODEC, EnemyCoreSettingsC2SPacket::handle);
        registrar.playToServer(EnemyCannonSettingsC2SPacket.TYPE, EnemyCannonSettingsC2SPacket.STREAM_CODEC, EnemyCannonSettingsC2SPacket::handle);
        registrar.playToServer(EnemyAutocannonSettingsC2SPacket.TYPE, EnemyAutocannonSettingsC2SPacket.STREAM_CODEC, EnemyAutocannonSettingsC2SPacket::handle);
        registrar.playToServer(SelectCustomTurretC2SPacket.TYPE, SelectCustomTurretC2SPacket.STREAM_CODEC, SelectCustomTurretC2SPacket::handle);

        registrar.playToClient(ControlSeatS2CPacket.TYPE, ControlSeatS2CPacket.STREAM_CODEC, ControlSeatS2CPacket::handle);
        registrar.playToClient(ControlSeatInputS2CPacket.TYPE, ControlSeatInputS2CPacket.STREAM_CODEC, ControlSeatInputS2CPacket::handle);
        registrar.playToClient(ControlSeatStatusS2CPacket.TYPE, ControlSeatStatusS2CPacket.STREAM_CODEC, ControlSeatStatusS2CPacket::handle);
        registrar.playToClient(NearbyShipsS2CPacket.TYPE, NearbyShipsS2CPacket.STREAM_CODEC, NearbyShipsS2CPacket::handle);
        registrar.playToClient(FxBlockS2CPacket.TYPE, FxBlockS2CPacket.STREAM_CODEC, FxBlockS2CPacket::handle);
        registrar.playToClient(FxEntityS2CPacket.TYPE, FxEntityS2CPacket.STREAM_CODEC, FxEntityS2CPacket::handle);
        registrar.playToClient(FxPositionS2CPacket.TYPE, FxPositionS2CPacket.STREAM_CODEC, FxPositionS2CPacket::handle);
        registrar.playToClient(ThrusterS2CPacket.TYPE, ThrusterS2CPacket.STREAM_CODEC, ThrusterS2CPacket::handle);
        registrar.playToClient(VectorThrusterS2CPacket.TYPE, VectorThrusterS2CPacket.STREAM_CODEC, VectorThrusterS2CPacket::handle);
        registrar.playToClient(SyncThrusterFuelsPacket.TYPE, SyncThrusterFuelsPacket.STREAM_CODEC, SyncThrusterFuelsPacket::handle);
        registrar.playToClient(AmmoBoxRefillMarkerS2CPacket.TYPE, AmmoBoxRefillMarkerS2CPacket.STREAM_CODEC, AmmoBoxRefillMarkerS2CPacket::handle);
        registrar.playToClient(RailCannonFireSoundS2CPacket.TYPE, RailCannonFireSoundS2CPacket.STREAM_CODEC, RailCannonFireSoundS2CPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
