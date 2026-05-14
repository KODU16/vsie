package com.kodu16.vsie.content.controlseat.client.HUD;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;

import static com.kodu16.vsie.content.controlseat.client.HUD.HudOverlay.MAIN_COLOR;
import static com.kodu16.vsie.content.controlseat.client.HUD.HudOverlay.SUB_COLOR;

public class StatusIndicator {
    private static final int SIDEARC_RADIUS = 60;
    private static final int SIDEARC_THICKNESS = 2;
    private static final int TEXT_ALPHA = 7;
    private static final int WHITE = FastColor.ARGB32.color(TEXT_ALPHA, 0xBB, 0xBB, 0xBB);

    private static final int MAIN_COLOR_FUEL = FastColor.ARGB32.color(TEXT_ALPHA, 0xFF, 0xAA, 0x11);
    private static final int MAIN_COLOR_SHIELD = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0x55, 0xFF);

    private static final Minecraft mc = Minecraft.getInstance();

    public static void renderDecorative(GuiGraphics gg,
                                        float energypercent,
                                        float fuelpercent,
                                        float shieldpercent,
                                        int throttle,
                                        int mousex, int mousey) {
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int centerX = sw / 2;
        int centerY = sh / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float energyAngle = -210 + 60 * energypercent;
        drawStatusArc(gg, centerX - centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR, -210, energyAngle);
        drawStatusArc(gg, centerX - centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, WHITE, energyAngle, -150);

        float fuelAngle = -211 + 62 * fuelpercent;
        drawStatusArc(gg, centerX - centerX / 20 - 2, centerY, SIDEARC_RADIUS + 4, SIDEARC_THICKNESS, MAIN_COLOR_FUEL, -211, fuelAngle);
        drawStatusArc(gg, centerX - centerX / 20 - 2, centerY, SIDEARC_RADIUS + 4, SIDEARC_THICKNESS, WHITE, fuelAngle, -149);

        float shieldAngle = 30 - 60 * shieldpercent;
        drawStatusArc(gg, centerX + centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR_SHIELD, shieldAngle, 30);
        drawStatusArc(gg, centerX + centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, WHITE, -30, shieldAngle);

        drawStatusArc(gg, centerX + centerX / 20 + 2, centerY, SIDEARC_RADIUS + 4, SIDEARC_THICKNESS, WHITE, -31, 31);

        int throttleY = centerY + (centerY / 2);
        DrawShape.drawThickLine(gg, centerX - (3 * centerX / 10) - 25, throttleY,
                centerX - (3 * centerX / 10) + 25, throttleY, 4, SUB_COLOR);
        DrawShape.drawThickLine(gg, centerX - (3 * centerX / 10), throttleY,
                centerX - (3 * centerX / 10) + (int) (0.25 * throttle), throttleY, 4, MAIN_COLOR);

        double deltax = (mousex < 0 ? -1 : 1) * Math.sqrt((double) Math.abs(mousex) / 2);
        double deltay = (mousey < 0 ? -1 : 1) * Math.sqrt((double) Math.abs(mousey) / 2);
        DrawShape.drawThickLine(gg, centerX, centerY, (int) (centerX + deltax), (int) (centerY + deltay), 1, MAIN_COLOR);

        RenderSystem.disableBlend();
    }

    private static void drawStatusArc(GuiGraphics gg, int cx, int cy, int radius, int thickness,
                                      int argb, float startAngleDeg, float endAngleDeg) {
        // Each status arc is submitted as its own triangle strip to prevent adjacent bars from forming connector triangles.
        DrawShape.drawPartialArc(gg, cx, cy, radius, thickness, argb, startAngleDeg, endAngleDeg);
    }
}
