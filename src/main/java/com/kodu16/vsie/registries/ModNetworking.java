// 我爱GPT5
package com.kodu16.vsie.registries;

import com.kodu16.vsie.network.IFF.IFFC2SPacket;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatC2SPacket;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatInputC2SPacket;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatWarpTargetC2SPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatInputS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatStatusS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.NearbyShipsS2CPacket;
import com.kodu16.vsie.network.fuel.SyncThrusterFuelsPacket;
import com.kodu16.vsie.network.rail.ElectroMagnetRailCoreDetectC2SPacket;
import com.kodu16.vsie.network.fx.FxBlockS2CPacket;
import com.kodu16.vsie.network.fx.FxEntityS2CPacket;
import com.kodu16.vsie.network.screen.ScreenC2SPacket;
import com.kodu16.vsie.network.screen.ScreentypeC2SPacket;
import com.kodu16.vsie.network.turret.HeavyTurretC2SPacket;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
import com.kodu16.vsie.network.turret.TurretDefaultSpinC2SPacket;
import com.kodu16.vsie.network.weapon.WeaponC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.valkyrienskies.core.impl.shadow.CH;

@SuppressWarnings({"removal"})
public class ModNetworking {
    public static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("vsie", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;
    private static int nextId() { return id++; }

    public static void register() {
        // 注册C2S数据包
        CHANNEL.registerMessage(
                nextId(),
                ControlSeatC2SPacket.class,
                ControlSeatC2SPacket::encode,
                ControlSeatC2SPacket::decode,
                ControlSeatC2SPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ControlSeatInputC2SPacket.class,
                ControlSeatInputC2SPacket::encode,
                ControlSeatInputC2SPacket::decode,
                ControlSeatInputC2SPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ControlSeatWarpTargetC2SPacket.class,
                ControlSeatWarpTargetC2SPacket::encode,
                ControlSeatWarpTargetC2SPacket::decode,
                ControlSeatWarpTargetC2SPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                TurretC2SPacket.class, // 新 C2S 数据包类
                TurretC2SPacket::encode, // 编码方法
                TurretC2SPacket::decode, // 解码方法
                TurretC2SPacket::handle  // 处理方法
        );
        CHANNEL.registerMessage(
                nextId(),
                TurretDefaultSpinC2SPacket.class, // 新 C2S 数据包类
                TurretDefaultSpinC2SPacket::encode, // 编码方法
                TurretDefaultSpinC2SPacket::decode, // 解码方法
                TurretDefaultSpinC2SPacket::handle  // 处理方法
        );
        CHANNEL.registerMessage(
                nextId(),
                HeavyTurretC2SPacket.class, // 新 C2S 数据包类
                HeavyTurretC2SPacket::encode, // 编码方法
                HeavyTurretC2SPacket::decode, // 解码方法
                HeavyTurretC2SPacket::handle  // 处理方法
        );
        CHANNEL.registerMessage(
                nextId(),
                WeaponC2SPacket.class,
                WeaponC2SPacket::encode,
                WeaponC2SPacket::decode,
                WeaponC2SPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                IFFC2SPacket.class,
                IFFC2SPacket::encode,
                IFFC2SPacket::decode,
                IFFC2SPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ScreenC2SPacket.class,
                ScreenC2SPacket::encode,
                ScreenC2SPacket::decode,
                ScreenC2SPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ScreentypeC2SPacket.class,
                ScreentypeC2SPacket::encode,
                ScreentypeC2SPacket::decode,
                ScreentypeC2SPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ElectroMagnetRailCoreDetectC2SPacket.class,
                ElectroMagnetRailCoreDetectC2SPacket::encode,
                ElectroMagnetRailCoreDetectC2SPacket::decode,
                ElectroMagnetRailCoreDetectC2SPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ControlSeatS2CPacket.class,
                ControlSeatS2CPacket::write,
                ControlSeatS2CPacket::decode,
                ControlSeatS2CPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ControlSeatInputS2CPacket.class,
                ControlSeatInputS2CPacket::write,
                ControlSeatInputS2CPacket::decode,
                ControlSeatInputS2CPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                ControlSeatStatusS2CPacket.class,
                ControlSeatStatusS2CPacket::write,
                ControlSeatStatusS2CPacket::decode,
                ControlSeatStatusS2CPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                NearbyShipsS2CPacket.class,
                NearbyShipsS2CPacket::encode,
                NearbyShipsS2CPacket::decode,
                NearbyShipsS2CPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                FxBlockS2CPacket.class,
                FxBlockS2CPacket::encode,
                FxBlockS2CPacket::decode,
                FxBlockS2CPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                FxEntityS2CPacket.class,
                FxEntityS2CPacket::encode,
                FxEntityS2CPacket::decode,
                FxEntityS2CPacket::handle
        );

        CHANNEL.messageBuilder(SyncThrusterFuelsPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncThrusterFuelsPacket::encode)
                .decoder(SyncThrusterFuelsPacket::decode)
                .consumerMainThread(SyncThrusterFuelsPacket::handle)
                .add();
    }

    public static <MSG> void sendToAll(MSG message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

}
