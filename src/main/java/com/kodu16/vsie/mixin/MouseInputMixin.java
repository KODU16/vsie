package com.kodu16.vsie.mixin;

import com.kodu16.vsie.content.controlseat.client.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.foundation.clamp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
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

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)

    private void onMouseMove(long window, double xpos, double ypos, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        ControlSeatClientData data = null;
        if (player != null) {
            data = ClientDataManager.getClientData(player);
        }
        if (data != null && data.isViewLocked()) {
            // 视角被锁定时，拦截鼠标移动事件
            ci.cancel();
            data.setAccumulatedx(clamp.clampd(-2560,data.getAccumulatedMousex() + xpos - data.getLastMousex(),2560));
            data.setAccumulatedy(clamp.clampd(-1440,data.getAccumulatedMousey() + ypos - data.getLastMousey(),1440));
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
        ControlSeatClientData data = null;
        if (player != null) {
            data = ClientDataManager.getClientData(player);
        }
        if (data != null && data.isViewLocked()) {
            // 视角被锁定时，拦截鼠标点击事件
            ci.cancel();
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
            // 视角被锁定时，拦截鼠标滚轮事件
            ci.cancel();
        }
    }
}