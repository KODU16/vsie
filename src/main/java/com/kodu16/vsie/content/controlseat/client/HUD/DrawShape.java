package com.kodu16.vsie.content.controlseat.client.HUD;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class DrawShape {
    // Function: prefer GuiGraphics.fill for axis-aligned HUD primitives to avoid per-line buffer submission overhead.
    private static void fillRect(GuiGraphics gg, int left, int top, int right, int bottom, int argb) {
        if (right <= left || bottom <= top) {
            return;
        }
        gg.fill(left, top, right, bottom, argb);
    }

    public static void drawArc(GuiGraphics gg, int cx, int cy, int radius, int thickness,
                               int argb, float startAngleDeg, float endAngleDeg) {
        int segments = Math.max(16, (int) (Math.abs(endAngleDeg - startAngleDeg) / 360f * 180f));
        segments = Math.min(segments, 36);

        float start = (float) Math.toRadians(startAngleDeg);
        float end = (float) Math.toRadians(endAngleDeg);
        float step = (end - start) / segments;

        float a = (float) (argb >> 24 & 255) / 255.0F;
        float r = (float) (argb >> 16 & 255) / 255.0F;
        float g = (float) (argb >> 8 & 255) / 255.0F;
        float b = (float) (argb & 255) / 255.0F;

        Matrix4f mat = gg.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerR = radius - thickness;
        for (int i = 0; i <= segments; i++) {
            float angle = start + i * step;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            float ox = cos * radius;
            float oy = sin * radius;
            float ix = cos * innerR;
            float iy = sin * innerR;

            buffer.addVertex(mat, cx + ox, cy + oy, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, cx + ix, cy + iy, 0).setColor(r, g, b, a);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    public static void drawPartialArc(GuiGraphics gg, int cx, int cy, int radius, int thickness,
                                      int argb, float startAngleDeg, float endAngleDeg) {
        float startRad = (float) Math.toRadians(startAngleDeg);
        float endRad = (float) Math.toRadians(endAngleDeg);
        if (endRad < startRad) {
            endRad += Math.PI * 2f;
        }
        if (endRad - startRad < 0.0001f) {
            return;
        }

        // Function: HUD arcs stay visually smooth with coarser tessellation than one quad per degree.
        int segments = Math.max(12, (int) Math.ceil(Math.toDegrees(endRad - startRad) / 2.5d));

        float a = (argb >> 24 & 255) / 255f;
        float r = (argb >> 16 & 255) / 255f;
        float g = (argb >> 8 & 255) / 255f;
        float b = (argb & 255) / 255f;

        Matrix4f pose = gg.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float innerR = radius - thickness;
        for (int i = 0; i < segments; i++) {
            float angle0 = startRad + i * (endRad - startRad) / segments;
            float angle1 = startRad + (i + 1) * (endRad - startRad) / segments;
            float cos0 = (float) Math.cos(angle0);
            float sin0 = (float) Math.sin(angle0);
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);

            float outerX0 = cx + cos0 * radius;
            float outerY0 = cy + sin0 * radius;
            float innerX0 = cx + cos0 * innerR;
            float innerY0 = cy + sin0 * innerR;
            float outerX1 = cx + cos1 * radius;
            float outerY1 = cy + sin1 * radius;
            float innerX1 = cx + cos1 * innerR;
            float innerY1 = cy + sin1 * innerR;

            buffer.addVertex(pose, outerX0, outerY0, 0).setColor(r, g, b, a);
            buffer.addVertex(pose, innerX0, innerY0, 0).setColor(r, g, b, a);
            buffer.addVertex(pose, innerX1, innerY1, 0).setColor(r, g, b, a);
            buffer.addVertex(pose, outerX1, outerY1, 0).setColor(r, g, b, a);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    public static void drawHollowRectangle(GuiGraphics gg, int centerX, int centerY,
                                           int width, int height, int thickness, int argb) {
        if (thickness <= 0 || width <= 0 || height <= 0) {
            return;
        }

        int halfOuterWidth = width / 2;
        int halfOuterHeight = height / 2;
        int left = centerX - halfOuterWidth;
        int right = centerX + halfOuterWidth;
        int top = centerY - halfOuterHeight;
        int bottom = centerY + halfOuterHeight;

        fillRect(gg, left, top, right, top + thickness, argb);
        fillRect(gg, left, bottom - thickness, right, bottom, argb);
        fillRect(gg, left, top + thickness, left + thickness, bottom - thickness, argb);
        fillRect(gg, right - thickness, top + thickness, right, bottom - thickness, argb);
    }

    public static void drawThickLine(GuiGraphics gg,
                                     int x1, int y1,
                                     int x2, int y2,
                                     int thickness,
                                     int argb) {
        if (thickness <= 0) {
            return;
        }

        if (x1 == x2) {
            int left = x1 - thickness / 2;
            int top = Math.min(y1, y2);
            fillRect(gg, left, top, left + thickness, Math.max(y1, y2) + 1, argb);
            return;
        }
        if (y1 == y2) {
            int top = y1 - thickness / 2;
            int left = Math.min(x1, x2);
            fillRect(gg, left, top, Math.max(x1, x2) + 1, top + thickness, argb);
            return;
        }

        float a = ((argb >> 24) & 255) / 255f;
        float r = ((argb >> 16) & 255) / 255f;
        float g = ((argb >> 8) & 255) / 255f;
        float b = (argb & 255) / 255f;

        Matrix4f mat = gg.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) {
            RenderSystem.disableBlend();
            return;
        }

        float nx = dy / len;
        float ny = -dx / len;
        float half = thickness / 2f;

        buffer.addVertex(mat, x1 + nx * half, y1 + ny * half, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x1 - nx * half, y1 - ny * half, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x2 + nx * half, y2 + ny * half, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x2 - nx * half, y2 - ny * half, 0).setColor(r, g, b, a);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
