package com.kodu16.vsie.content.controlseat.client.HUD;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;

public class Decorative {
    private static final int SIDEARC_RADIUS = 90; // 半圆半径（像素）
    private static final int SIDEARC_THICKNESS = 2; // 弧线厚度
    private static final int TEXT_ALPHA    = 5;   // 主文字透明度
    // 颜色（ARGB）
    private static final int MAIN_COLOR = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0xFF, 0xFF);
    private static final int SUB_COLOR  = FastColor.ARGB32.color(TEXT_ALPHA, 0x99, 0xFF, 0xFF);
    private static final Minecraft mc = Minecraft.getInstance(); // drawGlowText 要用

    public static void renderThrottleIndicator(GuiGraphics gg, int throttlePercent) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int centerX = sw / 2;
        int centerY = sh / 2;

        // 背景灰色半圆（45°~135°）
        DrawArc.drawPartialArc(gg, centerX+500, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, SUB_COLOR, 120, 180);
        DrawArc.drawPartialArc(gg, centerX-500, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR, -180, -120);

        /*// 文字直接用 gg.drawString（它内部也是批量的）
        String text = throttlePercent + "%";
        int textWidth = mc.font.width(text);
        gg.drawString(mc.font, text, centerX - textWidth / 2, centerY - 9, MAIN_COLOR, false);*/
    }
}
