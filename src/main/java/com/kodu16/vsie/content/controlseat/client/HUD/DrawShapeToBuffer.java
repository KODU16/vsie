package com.kodu16.vsie.content.controlseat.client.HUD;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

public class DrawShapeToBuffer {

    public static void drawPartialArc(BufferBuilder buffer, Matrix4f mat, int cx, int cy, int radius, int thickness,
                                      int argb, float startAngleDeg, float endAngleDeg) {

        float startRad = (float) Math.toRadians(startAngleDeg);
        float endRad = (float) Math.toRadians(endAngleDeg);
        if (endRad < startRad) {
            endRad += (float) (Math.PI * 2f);
        }

        int segments = Math.max(16, (int) Math.toDegrees(endRad - startRad) * 2);

        float a = (argb >> 24 & 255) / 255f;
        float r = (argb >> 16 & 255) / 255f;
        float g = (argb >> 8 & 255) / 255f;
        float b = (argb & 255) / 255f;

        float innerR = radius - thickness;

        for (int i = 0; i <= segments; i++) {
            float angle = startRad + i * (endRad - startRad) / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            float outerX = cx + cos * radius;
            float outerY = cy + sin * radius;
            float innerX = cx + cos * innerR;
            float innerY = cy + sin * innerR;

            buffer.addVertex(mat, outerX, outerY, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, innerX, innerY, 0).setColor(r, g, b, a);
            if (i == 0 || i == segments) {
                insertDegenerate(buffer, mat, outerX, outerY, argb);
            }
        }
    }

    public static void drawHollowRectangle(BufferBuilder buffer, Matrix4f mat, int centerX, int centerY,
                                           int width, int height, int thickness, int argb) {
        float halfOuterWidth = width / 2.0f;
        float halfOuterHeight = height / 2.0f;
        float halfInnerWidth = halfOuterWidth - thickness;
        float halfInnerHeight = halfOuterHeight - thickness;

        if (halfInnerWidth < 0) {
            halfInnerWidth = 0;
        }
        if (halfInnerHeight < 0) {
            halfInnerHeight = 0;
        }

        float a = (argb >> 24 & 255) / 255.0f;
        float r = (argb >> 16 & 255) / 255.0f;
        float g = (argb >> 8 & 255) / 255.0f;
        float b = (argb & 255) / 255.0f;

        buffer.addVertex(mat, centerX - halfOuterWidth, centerY - halfOuterHeight, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, centerX - halfInnerWidth, centerY - halfInnerHeight, 0).setColor(r, g, b, a);

        buffer.addVertex(mat, centerX + halfOuterWidth, centerY - halfOuterHeight, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, centerX + halfInnerWidth, centerY - halfInnerHeight, 0).setColor(r, g, b, a);

        buffer.addVertex(mat, centerX + halfOuterWidth, centerY + halfOuterHeight, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, centerX + halfInnerWidth, centerY + halfInnerHeight, 0).setColor(r, g, b, a);

        buffer.addVertex(mat, centerX - halfOuterWidth, centerY + halfOuterHeight, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, centerX - halfInnerWidth, centerY + halfInnerHeight, 0).setColor(r, g, b, a);

        buffer.addVertex(mat, centerX - halfOuterWidth, centerY - halfOuterHeight, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, centerX - halfInnerWidth, centerY - halfInnerHeight, 0).setColor(r, g, b, a);
    }

    public static void drawThickLine(BufferBuilder buffer, Matrix4f mat,
                                     int x1, int y1,
                                     int x2, int y2,
                                     int thickness,
                                     int argb) {
        if (thickness <= 0) {
            return;
        }

        float a = ((argb >> 24) & 255) / 255f;
        float r = ((argb >> 16) & 255) / 255f;
        float g = ((argb >> 8) & 255) / 255f;
        float b = (argb & 255) / 255f;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.1) {
            return;
        }

        float nx = dy / len;
        float ny = -dx / len;

        float half = thickness / 2f;

        insertDegenerate(buffer, mat, x1, y1, argb);
        buffer.addVertex(mat, x1 + nx * half, y1 + ny * half, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x1 - nx * half, y1 - ny * half, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x2 + nx * half, y2 + ny * half, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x2 - nx * half, y2 - ny * half, 0).setColor(r, g, b, a);
        insertDegenerate(buffer, mat, x2, y2, argb);
    }

    public static void insertDegenerate(BufferBuilder buffer, Matrix4f mat, float x, float y, int argb) {
        float a = ((argb >> 24) & 255) / 255f;
        float r = ((argb >> 16) & 255) / 255f;
        float g = ((argb >> 8) & 255) / 255f;
        float b = (argb & 255) / 255f;

        buffer.addVertex(mat, x, y, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x, y, 0).setColor(r, g, b, a);
    }

}
