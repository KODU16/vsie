package com.kodu16.vsie.content.controlseat.client.HUD;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;

public class ThrottleIndicator {
    // 在类最上面加几个配置（以后可以移到config）
    private static final int THROTTLE_X = 0;      // 相对屏幕中心X偏移
    private static final int THROTTLE_Y = 10;     // 相对屏幕中心Y偏移（正数=向下）
    private static final int THROTTLE_RADIUS = 30; // 半圆半径（像素）
    private static final int THROTTLE_THICKNESS = 3; // 弧线厚度
    private static final int TEXT_ALPHA    = 5;   // 主文字透明度
    // 颜色（ARGB）
    private static final int MAIN_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0xFF, 0xFF);
    private static final int SUB_COLOR  = FastColor.ARGB32.color(TEXT_ALPHA, 0x99, 0xFF, 0xFF);
    private static final Minecraft mc = Minecraft.getInstance(); // drawGlowText 要用

    public static void renderThrottleIndicator(GuiGraphics gg, int throttlePercent) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int centerX = sw / 2 + THROTTLE_X;
        int centerY = sh / 2 + THROTTLE_Y;

        // 背景灰色半圆（45°~135°）
        DrawArc.drawPartialArc(gg, centerX, centerY, THROTTLE_RADIUS, THROTTLE_THICKNESS, SUB_COLOR, 45, 135);

        // 动态填充（0% 在左边 6点钟方向，100% 填满到右边 4点钟方向）
        if (throttlePercent > 0) {
            float endAngle = 90f + 45f * (throttlePercent / 100f); // 90° 开始，顺时针到 135°
            DrawArc.drawPartialArc(gg, centerX, centerY, THROTTLE_RADIUS, THROTTLE_THICKNESS, MAIN_COLOR, 90, endAngle);
        }

        // 文字直接用 gg.drawString（它内部也是批量的）
        String text = throttlePercent + "%";
        int textWidth = mc.font.width(text);
        gg.drawString(mc.font, text, centerX - textWidth / 2, centerY - 9, MAIN_COLOR, false);
    }

}
