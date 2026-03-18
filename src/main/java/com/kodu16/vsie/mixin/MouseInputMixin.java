package com.kodu16.vsie.mixin;

import com.kodu16.vsie.content.controlseat.client.ControlSeatWarpSelectionScreen;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseInputMixin {
    //你好啊
    //我注意到mixin能import其他库，太棒了
    //我猜我就在这直接改client的鼠标输入了
    //我一开始还以为读不到Clientdata，实在是太傻了
    private static final Logger LOGGER = LogUtils.getLogger();
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)

    private void onMouseMove(long window, double xpos, double ypos, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        ControlSeatClientData data = null;
        if (player != null) {
            data = ClientDataManager.getClientData(player);
        }
        if (data != null && data.isViewLocked()) {
            if (Minecraft.getInstance().screen instanceof ControlSeatWarpSelectionScreen) {
                return;
            }
            // 功能：仅在没有跃迁选单时拦截鼠标移动；打开选单后允许光标正常悬停按钮。
            ci.cancel();
            data.setAccumulatedx(Mth.clamp(data.getAccumulatedMousex() + xpos - data.getLastMousex(),-2560,2560));
            data.setAccumulatedy(Mth.clamp(data.getAccumulatedMousey() + ypos - data.getLastMousey(),-1440,1440));
            data.setLastMousex(xpos);
            data.setLastMousey(ypos);
        }
        else if(data!=null && !data.isViewLocked()){
            data.setAccumulatedx(0);
            data.setAccumulatedy(0);
            data.setLastMousex(0);
            data.setLastMousey(0);
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ControlSeatClientData data = ClientDataManager.getClientData(player);
        if (data == null || !data.isViewLocked()) return;
        if (Minecraft.getInstance().screen instanceof ControlSeatWarpSelectionScreen) return;

        // 只处理左键
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        // 视角锁定的情况下拦截所有鼠标点击
        ci.cancel();

        if (action == GLFW.GLFW_PRESS) {
            data.mouseLpress = true;
            LOGGER.warn("左键按下");
        }
        else if (action == GLFW.GLFW_RELEASE) {
            data.mouseLpress = false;
            LOGGER.warn("左键释放");
        }
    }


    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        ControlSeatClientData data = null;
        if (player != null) {
            data = ClientDataManager.getClientData(player);
        }
        if (data != null && data.isViewLocked()) {
            if (Minecraft.getInstance().screen instanceof ControlSeatWarpSelectionScreen) {
                return;
            }
            // 功能：仅在没有跃迁选单时拦截滚轮；打开选单后把滚轮交给选单滚动按钮列表。
            ci.cancel();
        }
    }
}