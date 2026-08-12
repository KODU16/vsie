package com.kodu16.vsie.mixin;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.Input.ClientMouseHandler;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseInputMixin {
    @Inject(method = "onMove(JDD)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onMouseMove(long window, double xpos, double ypos, CallbackInfo ci) {
        if (shouldYieldToOpenScreen()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        ControlSeatClientData data = getCurrentSeatData(player);
        if (data != null && data.isViewLocked()) {
            // Function: only the active chair's view lock may capture mouse movement.
            ci.cancel();
            if (!data.isMouseAnchorSet()) {
                data.setLastMousex(xpos);
                data.setLastMousey(ypos);
                data.setMouseAnchorSet(true);
                return;
            }
            data.setAccumulatedx(Mth.clamp(data.getAccumulatedMousex() + xpos - data.getLastMousex(), -2560, 2560));
            data.setAccumulatedy(Mth.clamp(data.getAccumulatedMousey() + ypos - data.getLastMousey(), -1440, 1440));
            data.setLastMousex(xpos);
            data.setLastMousey(ypos);
        } else if (data != null) {
            data.setAccumulatedx(0);
            data.setAccumulatedy(0);
            data.setLastMousex(0);
            data.setLastMousey(0);
            data.setMouseAnchorSet(false);
        }
    }

    @Inject(method = "onPress(JIII)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (shouldYieldToOpenScreen()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        ControlSeatClientData data = getCurrentSeatData(player);
        if (data == null) return;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        // Function: seated left click drives weapon input and must not fall through to vanilla block-destroy handling.
        ci.cancel();
        if (action == GLFW.GLFW_PRESS) {
            data.mouseLpress = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            data.mouseLpress = false;
        }
    }

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onMouseScroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
        if (shouldYieldToOpenScreen()) {
            ClientMouseHandler.LOGGER.info(
                    "[VSIE-MOUSE] phase=YIELD_TO_SCREEN input=SCROLL screen={} deltaX={} deltaY={}",
                    Minecraft.getInstance().screen.getClass().getName(), xoffset, yoffset
            );
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        ControlSeatClientData data = getCurrentSeatData(player);
        if (data != null && data.isViewLocked()) {
            // Function: only the active chair's view lock may capture scroll input.
            ci.cancel();
        }
    }

    /** Open GUIs, including the ESC pause menu, always own mouse movement, buttons, and scroll. */
    private static boolean shouldYieldToOpenScreen() {
        return Minecraft.getInstance().screen != null;
    }

    private static ControlSeatClientData getCurrentSeatData(LocalPlayer player) {
        ClientMouseHandler.clearInactiveSeatState(player);
        if (player == null || !(player.getVehicle() instanceof ControlSeatMountEntity mount)) {
            return null;
        }
        BlockPos seatPos = mount.getBoundBlockPos();
        return ClientDataManager.getClientDataForSeat(player, seatPos);
    }
}
