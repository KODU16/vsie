package com.kodu16.vsie.mixin;

import com.kodu16.vsie.content.controlseat.client.ControlSeatWarpSelectionScreen;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardInputMixin {
    @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onKeyPress(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (key < GLFW.GLFW_KEY_1 || key > GLFW.GLFW_KEY_4) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null && !(minecraft.screen instanceof ControlSeatWarpSelectionScreen)) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof ControlSeatMountEntity)) {
            return;
        }

        // Function: seated weapon-channel hotkeys must not fall through to vanilla hotbar slot switching.
        ci.cancel();
    }
}
