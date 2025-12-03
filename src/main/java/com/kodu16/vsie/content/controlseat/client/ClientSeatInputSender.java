package com.kodu16.vsie.content.controlseat.client;

import com.kodu16.vsie.network.ControlSeatInputC2SPacket;
import com.kodu16.vsie.network.ModNetworking;
import com.kodu16.vsie.registries.vsieKeyMappings;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import java.util.UUID;
import net.minecraft.client.KeyMapping;

import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

public class ClientSeatInputSender {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static long lastSendMs = 0;
    /** 每 tick/隔几 tick 调用一次即可（例如在 ClientTickEvent.END） */
    public static void tickSend(BlockPos pos, UUID uuid, double mousex, double mousey, double roll) {
        Minecraft mc = Minecraft.getInstance();
        //合法性校验，省的玩家A动了读玩家B
        if (mc.player == null || mc.player.getUUID() != uuid) return;

        // 限速：每 2 tick/每 ~33ms 发一次，避免刷屏
        long now = System.currentTimeMillis();
        if (now - lastSendMs < 33) return;
        lastSendMs = now;

        int keys = 0;
        if (vsieKeyMappings.KEY_THROTTLE.isDown()) keys |= ControlSeatInputC2SPacket.Keys.THROTTLE;
        if (vsieKeyMappings.KEY_BRAKE.isDown()) keys |= ControlSeatInputC2SPacket.Keys.BRAKE;
        if (vsieKeyMappings.KEY_SCAN_PERIPHERAL.isDown()) keys |= ControlSeatInputC2SPacket.Keys.SCAN_PERIPHERAL;
        if (mc.options.keyLeft.isDown()) keys |= ControlSeatInputC2SPacket.Keys.ROLLL;
        if (mc.options.keyRight.isDown()) keys |= ControlSeatInputC2SPacket.Keys.ROLLR;
        if (mc.options.keyJump.isDown()) keys |= ControlSeatInputC2SPacket.Keys.SPACE;
        if (mc.options.keyShift.isDown()) keys |= ControlSeatInputC2SPacket.Keys.SHIFT;
        if (mc.options.keySprint.isDown()) keys |= ControlSeatInputC2SPacket.Keys.CTRL;
        if (mc.mouseHandler.isLeftPressed()) keys |= ControlSeatInputC2SPacket.Keys.MOUSEL;
        if (mc.mouseHandler.isRightPressed()) keys |= ControlSeatInputC2SPacket.Keys.MOUSER;

        // 归一化/限幅（与服务端 clamp 保持一致）
        //float cyaw   = clamp(yaw,   -1f, 1f);
        //float cpitch = clamp(pitch, -1f, 1f);
        //float croll  = clamp(roll,  -1f, 1f);

        ModNetworking.CHANNEL.sendToServer(
                new ControlSeatInputC2SPacket(pos, (float) mousex, (float) mousey, (float) roll, keys)
        );
        //LOGGER.warn(String.valueOf(Component.literal("C2S packet sent:mousex:"+mousex+"  mousey:"+mousey)));
    }

}