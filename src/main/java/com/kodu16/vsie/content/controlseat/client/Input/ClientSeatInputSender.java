package com.kodu16.vsie.content.controlseat.client.Input;

import com.kodu16.vsie.network.controlseat.C2S.ControlSeatC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatInputC2SPacket;
import com.kodu16.vsie.registries.vsieKeyMappings;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

import org.slf4j.Logger;

public class ClientSeatInputSender {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static long lastSendMs = 0;
    private static long lastSendInputMs = 0;
    /** 每 tick/隔几 tick 调用一次即可（例如在 ClientTickEvent.END） */
    public static void tickSend(BlockPos pos, UUID uuid,
                                double mousex, double mousey,
                                double roll,
                                boolean mouseLpress,
                                boolean isviewlock, Vec3 manualAimTargetPos) {
        Minecraft mc = Minecraft.getInstance();
        //合法性校验，省的玩家A动了读玩家B
        if (mc.player == null || mc.player.getUUID() != uuid) return;

        // 限速：快包（也就是持续状态检测）33ms一次，慢包（也就是按键切换检测）200ms一次
        long now = System.currentTimeMillis();
        if (now - lastSendMs > 33) {
            lastSendMs = now;
            int keys = 0;
            if (vsieKeyMappings.KEY_THROTTLE.isDown()) keys |= ControlSeatC2SPacket.Keys.THROTTLE;
            if (vsieKeyMappings.KEY_BRAKE.isDown()) keys |= ControlSeatC2SPacket.Keys.BRAKE;
            //if (vsieKeyMappings.KEY_SCAN.isDown()) keys |= ControlSeatInputC2SPacket.Keys.SCAN_PERIPHERAL;
            if (mc.options.keyLeft.isDown()) keys |= ControlSeatC2SPacket.Keys.ROLLL;
            if (mc.options.keyRight.isDown()) keys |= ControlSeatC2SPacket.Keys.ROLLR;
            if (mc.options.keyJump.isDown()) keys |= ControlSeatC2SPacket.Keys.SPACE;
            if (mc.options.keyShift.isDown()) keys |= ControlSeatC2SPacket.Keys.SHIFT;
            if (mc.options.keySprint.isDown()) keys |= ControlSeatC2SPacket.Keys.CTRL;
            if (mc.mouseHandler.isLeftPressed()) keys |= ControlSeatC2SPacket.Keys.MOUSEL;
            if (mc.mouseHandler.isRightPressed()) keys |= ControlSeatC2SPacket.Keys.MOUSER;
            ModNetworking.sendToServer(
                    new ControlSeatC2SPacket(pos, (float) mousex, (float) mousey, (float) roll, keys, mouseLpress)
            );
        }
        if (now - lastSendInputMs > 200) {
            lastSendInputMs = now;
            int keysInput = 0;
            if (vsieKeyMappings.KEY_TOGGLE_WEAPON_CHANNEL1.isDown()) keysInput |= ControlSeatInputC2SPacket.KeysInput.CHANNEL1;
            if (vsieKeyMappings.KEY_TOGGLE_WEAPON_CHANNEL2.isDown()) keysInput |= ControlSeatInputC2SPacket.KeysInput.CHANNEL2;
            if (vsieKeyMappings.KEY_TOGGLE_WEAPON_CHANNEL3.isDown()) keysInput |= ControlSeatInputC2SPacket.KeysInput.CHANNEL3;
            if (vsieKeyMappings.KEY_TOGGLE_WEAPON_CHANNEL4.isDown()) keysInput |= ControlSeatInputC2SPacket.KeysInput.CHANNEL4;
            if (vsieKeyMappings.KEY_SWITCH_ENEMY.isDown()) keysInput |= ControlSeatInputC2SPacket.KeysInput.SWITCHENEMY;
            if (vsieKeyMappings.KEY_TOGGLE_SHIELD.isDown()) keysInput |= ControlSeatInputC2SPacket.KeysInput.TOGGLESHIELD;
            if (vsieKeyMappings.KEY_TOGGLE_FLIGHT_ASSIST.isDown()) keysInput |= ControlSeatInputC2SPacket.KeysInput.TOGGLEFLIGHTASSIST;
            if (vsieKeyMappings.KEY_TOGGLE_ANTI_GRAVITY.isDown()) keysInput |= ControlSeatInputC2SPacket.KeysInput.TOGGLEANTIGRAVITY;
            ModNetworking.sendToServer(
                    // 功能：输入包直接上传客户端计算后的手动瞄准目标点，重型炮塔服务端不再用角度二次换算。
                    new ControlSeatInputC2SPacket(pos, keysInput, isviewlock,
                            manualAimTargetPos.x, manualAimTargetPos.y, manualAimTargetPos.z)
            );
        }
    }
}
