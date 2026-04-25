// 我爱GPT5
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
import com.kodu16.vsie.network.fuel.SyncThrusterFuelsPacket;
import com.kodu16.vsie.network.fx.FxBlockS2CPacket;
import com.kodu16.vsie.network.fx.FxEntityS2CPacket;
import com.kodu16.vsie.network.rail.ElectroMagnetRailCoreDetectC2SPacket;
import com.kodu16.vsie.network.screen.ScreenC2SPacket;
import com.kodu16.vsie.network.screen.ScreentypeC2SPacket;
import com.kodu16.vsie.network.turret.HeavyTurretC2SPacket;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
import com.kodu16.vsie.network.turret.TurretDefaultSpinC2SPacket;
import com.kodu16.vsie.network.turret.TurretFirePointC2SPacket;
import com.kodu16.vsie.network.weapon.WeaponC2SPacket;
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

    // 功能：在 NeoForge 1.21.1 中通过事件总线监听 RegisterPayloadHandlersEvent 来注册所有网络载荷。
    public static void register(IEventBus modBus) {
        modBus.addListener(ModNetworking::registerPayloads);
    }

    // 功能：按方向注册 play 阶段 payload，替代旧版 SimpleChannel#registerMessage。
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
        registrar.playToServer(IFFC2SPacket.TYPE, IFFC2SPacket.STREAM_CODEC, IFFC2SPacket::handle);
        registrar.playToServer(ScreenC2SPacket.TYPE, ScreenC2SPacket.STREAM_CODEC, ScreenC2SPacket::handle);
        registrar.playToServer(ScreentypeC2SPacket.TYPE, ScreentypeC2SPacket.STREAM_CODEC, ScreentypeC2SPacket::handle);
        registrar.playToServer(ElectroMagnetRailCoreDetectC2SPacket.TYPE, ElectroMagnetRailCoreDetectC2SPacket.STREAM_CODEC, ElectroMagnetRailCoreDetectC2SPacket::handle);

        registrar.playToClient(ControlSeatS2CPacket.TYPE, ControlSeatS2CPacket.STREAM_CODEC, ControlSeatS2CPacket::handle);
        registrar.playToClient(ControlSeatInputS2CPacket.TYPE, ControlSeatInputS2CPacket.STREAM_CODEC, ControlSeatInputS2CPacket::handle);
        registrar.playToClient(ControlSeatStatusS2CPacket.TYPE, ControlSeatStatusS2CPacket.STREAM_CODEC, ControlSeatStatusS2CPacket::handle);
        registrar.playToClient(NearbyShipsS2CPacket.TYPE, NearbyShipsS2CPacket.STREAM_CODEC, NearbyShipsS2CPacket::handle);
        registrar.playToClient(FxBlockS2CPacket.TYPE, FxBlockS2CPacket.STREAM_CODEC, FxBlockS2CPacket::handle);
        registrar.playToClient(FxEntityS2CPacket.TYPE, FxEntityS2CPacket.STREAM_CODEC, FxEntityS2CPacket::handle);
        registrar.playToClient(SyncThrusterFuelsPacket.TYPE, SyncThrusterFuelsPacket.STREAM_CODEC, SyncThrusterFuelsPacket::handle);
    }

    // 功能：封装客户端->服务端发包，统一替换旧 CHANNEL.sendToServer。
    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    // 功能：封装服务端->全体玩家发包，统一替换旧 PacketDistributor.ALL。
    public static void sendToAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    // 功能：封装服务端->指定玩家发包，统一替换旧 PacketDistributor.PLAYER。
    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
