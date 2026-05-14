package com.kodu16.vsie.content.controlseat.client.Input;

import com.kodu16.vsie.network.controlseat.C2S.ControlSeatC2SPacket;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatInputC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.vsieKeyMappings;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.UUID;

public class ClientSeatInputSender {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static long lastSendMs = 0;
    private static long lastSendInputMs = 0;
    private static final boolean[] numberKeyWasDown = new boolean[4];

    /** Called from the client tick while the local player is riding this control seat. */
    public static void tickSend(BlockPos pos, UUID uuid,
                                double mousex, double mousey,
                                double roll,
                                boolean mouseLpress,
                                boolean isviewlock, Vec3 manualAimTargetPos) {
        Minecraft mc = Minecraft.getInstance();
        // Function: only the seated local player may upload control input for this seat.
        if (mc.player == null || uuid == null || !mc.player.getUUID().equals(uuid)) return;

        long now = System.currentTimeMillis();
        if (now - lastSendMs > 33) {
            lastSendMs = now;
            int keys = 0;
            if (vsieKeyMappings.KEY_THROTTLE.isDown()) keys |= ControlSeatC2SPacket.Keys.THROTTLE;
            if (vsieKeyMappings.KEY_BRAKE.isDown()) keys |= ControlSeatC2SPacket.Keys.BRAKE;
            if (vsieKeyMappings.KEY_ROLL_L.isDown()) keys |= ControlSeatC2SPacket.Keys.ROLLL;
            if (vsieKeyMappings.KEY_ROLL_R.isDown()) keys |= ControlSeatC2SPacket.Keys.ROLLR;
            // Function: W/S and the configurable left/right keys are continuous flight controls.
            if (mc.options.keyUp.isDown()) keys |= ControlSeatC2SPacket.Keys.KEY_W;
            if (mc.options.keyDown.isDown()) keys |= ControlSeatC2SPacket.Keys.KEY_S;
            if (vsieKeyMappings.KEY_CONTROL_LEFT.isDown()) keys |= ControlSeatC2SPacket.Keys.KEY_CONTROL_LEFT;
            if (vsieKeyMappings.KEY_CONTROL_RIGHT.isDown()) keys |= ControlSeatC2SPacket.Keys.KEY_CONTROL_RIGHT;
            if (mc.options.keyJump.isDown()) keys |= ControlSeatC2SPacket.Keys.SPACE;
            if (mc.options.keyShift.isDown()) keys |= ControlSeatC2SPacket.Keys.SHIFT;
            if (mc.options.keySprint.isDown()) keys |= ControlSeatC2SPacket.Keys.CTRL;
            if (mc.mouseHandler.isLeftPressed()) keys |= ControlSeatC2SPacket.Keys.MOUSEL;
            if (mc.mouseHandler.isRightPressed()) keys |= ControlSeatC2SPacket.Keys.MOUSER;
            ModNetworking.sendToServer(
                    new ControlSeatC2SPacket(pos, (float) mousex, (float) mousey, (float) roll, keys, mouseLpress, isviewlock)
            );
        }

        int keysInput = collectToggleInput(mc);
        if (keysInput != 0 || now - lastSendInputMs > 200) {
            lastSendInputMs = now;
            ModNetworking.sendToServer(
                    // Function: upload the client-calculated manual aim point for server-side turret targeting.
                    new ControlSeatInputC2SPacket(pos, keysInput, isviewlock,
                            manualAimTargetPos.x, manualAimTargetPos.y, manualAimTargetPos.z)
            );
        }
    }

    private static int collectToggleInput(Minecraft mc) {
        int keysInput = 0;
        // Function: read raw number keys so vanilla hotbar key mappings cannot swallow weapon channel toggles.
        if (consumeNumberClick(mc, 0)) keysInput |= ControlSeatInputC2SPacket.KeysInput.CHANNEL1;
        if (consumeNumberClick(mc, 1)) keysInput |= ControlSeatInputC2SPacket.KeysInput.CHANNEL2;
        if (consumeNumberClick(mc, 2)) keysInput |= ControlSeatInputC2SPacket.KeysInput.CHANNEL3;
        if (consumeNumberClick(mc, 3)) keysInput |= ControlSeatInputC2SPacket.KeysInput.CHANNEL4;
        if (consumeClick(vsieKeyMappings.KEY_SWITCH_ENEMY)) keysInput |= ControlSeatInputC2SPacket.KeysInput.SWITCHENEMY;
        if (consumeClick(vsieKeyMappings.KEY_TOGGLE_SHIELD)) keysInput |= ControlSeatInputC2SPacket.KeysInput.TOGGLESHIELD;
        if (consumeClick(vsieKeyMappings.KEY_TOGGLE_FLIGHT_ASSIST)) keysInput |= ControlSeatInputC2SPacket.KeysInput.TOGGLEFLIGHTASSIST;
        if (consumeClick(vsieKeyMappings.KEY_TOGGLE_ANTI_GRAVITY)) keysInput |= ControlSeatInputC2SPacket.KeysInput.TOGGLEANTIGRAVITY;
        return keysInput;
    }

    private static boolean consumeNumberClick(Minecraft mc, int index) {
        long window = mc.getWindow().getWindow();
        boolean isDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_1 + index);
        boolean clicked = isDown && !numberKeyWasDown[index];
        numberKeyWasDown[index] = isDown;
        return clicked;
    }

    private static boolean consumeClick(KeyMapping keyMapping) {
        boolean clicked = false;
        while (keyMapping.consumeClick()) {
            clicked = true;
        }
        return clicked;
    }
}
