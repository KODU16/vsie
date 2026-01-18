// 我爱GPT5
package com.kodu16.vsie.network;

import com.kodu16.vsie.network.controlseat.ControlSeatInputC2SPacket;
import com.kodu16.vsie.network.controlseat.ControlSeatInputS2CPacket;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

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
                ControlSeatInputC2SPacket.class,
                ControlSeatInputC2SPacket::encode,
                ControlSeatInputC2SPacket::decode,
                ControlSeatInputC2SPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                TurretC2SPacket.class, // 你的新 C2S 数据包类
                TurretC2SPacket::encode, // 编码方法
                TurretC2SPacket::decode, // 解码方法
                TurretC2SPacket::handle  // 处理方法
        );

        // 注册S2C数据包
        CHANNEL.registerMessage(
                nextId(),
                ControlSeatInputS2CPacket.class,
                ControlSeatInputS2CPacket::write,
                ControlSeatInputS2CPacket::decode,
                ControlSeatInputS2CPacket::handle
        );
    }

}
