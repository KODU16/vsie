package com.kodu16.vsie.content.controlseat.client.HUD;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import static com.kodu16.vsie.content.controlseat.client.HUD.HudOverlay.MAIN_COLOR;
import static com.kodu16.vsie.content.controlseat.client.HUD.HudOverlay.SUB_COLOR;

public class StatusIndicator {
    private static final int SIDEARC_RADIUS = 60; // 半圆半径（像素）
    private static final int SIDEARC_THICKNESS = 2; // 弧线厚度
    private static final int TEXT_ALPHA    = 7;   // 主文字透明度
    // 颜色（ARGB）
    private static final int WHITE = FastColor.ARGB32.color(TEXT_ALPHA, 0xBB, 0xBB, 0xBB);

    private static final int MAIN_COLOR_FUEL = FastColor.ARGB32.color(TEXT_ALPHA, 0xFF, 0xAA, 0x11);
    private static final int SUB_COLOR_FUEL  = FastColor.ARGB32.color(TEXT_ALPHA, 0x55, 0x55, 0x11);

    private static final int MAIN_COLOR_SHIELD = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0x55, 0xFF);
    private static final int SUB_COLOR_SHIELD  = FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0x22, 0x55);

    private static final int MAIN_COLOR_HEAT = FastColor.ARGB32.color(TEXT_ALPHA, 0xFF, 0x22, 0x22);
    private static final int SUB_COLOR_HEAT  = FastColor.ARGB32.color(TEXT_ALPHA, 0x55, 0x22, 0x22);
    private static final Minecraft mc = Minecraft.getInstance(); // drawGlowText 要用

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

        final int LINE_THICKNESS = 1;           // 分界线粗细（可调1~3）
        final float LINE_INNER = SIDEARC_RADIUS - SIDEARC_THICKNESS;   // 从弧内侧往里一点
        final float LINE_OUTER  = SIDEARC_RADIUS;                      // 稍微超出弧外一点，更醒目
        Matrix4f mat = gg.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);  // 或者直接用 gui 的
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // 默认 alpha 混合
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        // ────────────── 能量 ──────────────
        float energyAngle = -210 + 60 * energypercent;
        DrawShapeToBuffer.drawPartialArc(buffer, mat,centerX - centerX/20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR,      -210, energyAngle);
        DrawShapeToBuffer.drawPartialArc(buffer, mat, centerX - centerX/20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, WHITE,       energyAngle, -150);

        //drawRadialSeparator(gg, centerX - centerX/4, centerY, energyAngle, LINE_INNER, LINE_OUTER, LINE_THICKNESS, WHITE);

        // ────────────── 燃油 ──────────────
        float fuelAngle = -211 + 62 * fuelpercent;
        DrawShapeToBuffer.drawPartialArc(buffer, mat, centerX - centerX/20 - 2, centerY, SIDEARC_RADIUS+4, SIDEARC_THICKNESS, MAIN_COLOR_FUEL, -211, fuelAngle);
        DrawShapeToBuffer.drawPartialArc(buffer, mat, centerX - centerX/20 - 2, centerY, SIDEARC_RADIUS+4, SIDEARC_THICKNESS, WHITE,  fuelAngle, -149);

        //drawRadialSeparator(gg, centerX - centerX/4 - 3, centerY, fuelAngle, LINE_INNER, LINE_OUTER, LINE_THICKNESS, WHITE);

        // ────────────── 护盾 ──────────────
        float shieldAngle = 30 - 60 * shieldpercent;
        DrawShapeToBuffer.drawPartialArc(buffer, mat, centerX + centerX/20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, MAIN_COLOR_SHIELD, shieldAngle, 30);
        DrawShapeToBuffer.drawPartialArc(buffer, mat, centerX + centerX/20, centerY, SIDEARC_RADIUS, SIDEARC_THICKNESS, WHITE,  -30, shieldAngle);

        //drawRadialSeparator(gg, centerX + centerX/4, centerY, shieldAngle, LINE_INNER, LINE_OUTER, LINE_THICKNESS, WHITE);

        //热量（未实装）
        DrawShapeToBuffer.drawPartialArc(buffer, mat, centerX + centerX/20 + 2, centerY, SIDEARC_RADIUS+4, SIDEARC_THICKNESS, WHITE,  -31, 31);

        //油门条
        DrawShapeToBuffer.drawThickLine(buffer, mat,centerX-(3*centerX/10)-25, centerY+((centerY/2)), centerX-(3*centerX/10)+25, centerY+((centerY/2)), 4,SUB_COLOR);
        DrawShapeToBuffer.drawThickLine(buffer, mat,centerX-(3*centerX/10), centerY+((centerY/2)), centerX-(3*centerX/10)+(int)(0.25*throttle), centerY+((centerY/2)), 4,MAIN_COLOR);

        //鼠标控制指示线
        double deltax=(mousex<0?-1:1)*Math.sqrt((double) Math.abs(mousex) /2);
        double deltay=(mousey<0?-1:1)*Math.sqrt((double) Math.abs(mousey) /2);
        DrawShapeToBuffer.drawThickLine(buffer,mat,centerX,centerY, (int) (centerX+deltax), (int) (centerY+deltay), 1, MAIN_COLOR);

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.disableBlend();
    }

}
