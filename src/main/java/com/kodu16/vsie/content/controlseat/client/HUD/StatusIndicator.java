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
    private static final int FUEL_BAR_RADIUS = SIDEARC_RADIUS + 4;
    private static final int TEXT_ALPHA = 7;
    private static final int WHITE = FastColor.ARGB32.color(TEXT_ALPHA, 0xBB, 0xBB, 0xBB);

    private static final int MAIN_COLOR_FUEL = FastColor.ARGB32.color(TEXT_ALPHA, 0xFF, 0xAA, 0x11);
    private static final int MAIN_COLOR_E710 = FastColor.ARGB32.color(TEXT_ALPHA, 0x66, 0x11, 0x88);
    private static final int MAIN_COLOR_WARP_E710_COST = FastColor.ARGB32.color(TEXT_ALPHA, 0xFF, 0x22, 0x33);
    private static final int MAIN_COLOR_SHIELD = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0x55, 0xFF);

    private static final Minecraft mc = Minecraft.getInstance();

    public static void renderDecorative(GuiGraphics gg,
                                        float energypercent,
                                        float fuelpercent,
                                        float e710percent,
                                        float warpE710CostPercent,
                                        boolean showWarpE710Bar,
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

        drawFuelStatusArc(gg, centerX - centerX / 20 - 2, centerY,
                fuelpercent, e710percent, warpE710CostPercent, showWarpE710Bar);

        float shieldAngle = 30 - 60 * shieldpercent;
        drawStatusArc(gg, centerX + centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR_SHIELD, shieldAngle, 30);
        drawStatusArc(gg, centerX + centerX / 20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, WHITE, -30, shieldAngle);

        drawStatusArc(gg, centerX + centerX / 20 + 2, centerY, SIDEARC_RADIUS + 4, SIDEARC_THICKNESS, WHITE, -31, 31);

        int throttleY = centerY + (centerY / 3);
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
        // Function: submit each arc separately so partial bars cannot form stray triangles between segments.
        DrawShape.drawPartialArc(gg, cx, cy, radius, thickness, argb, startAngleDeg, endAngleDeg);
    }

    private static void drawFuelStatusArc(GuiGraphics gg, int cx, int cy,
                                          float fuelpercent, float e710percent,
                                          float warpE710CostPercent, boolean showWarpE710Bar) {
        float startAngle = -211f;
        float endAngle = -149f;
        float span = endAngle - startAngle;
        if (!showWarpE710Bar) {
            float fuelRatio = Math.max(0f, Math.min(1f, fuelpercent));
            float fuelEndAngle = startAngle + span * fuelRatio;
            // Function: normal flight only shows the shared orange fuel bar against total tank capacity.
            drawStatusArc(gg, cx, cy, FUEL_BAR_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR_FUEL, startAngle, fuelEndAngle);
            drawStatusArc(gg, cx, cy, FUEL_BAR_RADIUS, SIDEARC_THICKNESS, WHITE, fuelEndAngle, endAngle);
            return;
        }

        float e710Ratio = Math.max(0f, Math.min(1f, e710percent));
        float e710EndAngle = startAngle + span * e710Ratio;
        // Function: warp preparation swaps the bar to E-710 while preserving the same total-capacity scale.
        drawStatusArc(gg, cx, cy, FUEL_BAR_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR_E710, startAngle, e710EndAngle);
        drawStatusArc(gg, cx, cy, FUEL_BAR_RADIUS, SIDEARC_THICKNESS, WHITE, e710EndAngle, endAngle);
        if (warpE710CostPercent > 0f && e710EndAngle > startAngle) {
            float visibleCostRatio = Math.max(0f, Math.min(e710Ratio, warpE710CostPercent));
            float costStartAngle = Math.max(startAngle, e710EndAngle - span * visibleCostRatio);
            // Function: draw the current jump cost on the outer edge so it reads as an overlay above the purple bar.
            drawStatusArc(gg, cx, cy, FUEL_BAR_RADIUS + 1, 1,
                    MAIN_COLOR_WARP_E710_COST, costStartAngle, e710EndAngle);
        }
    }
}
