package com.kodu16.vsie.content.controlseat.client.HUD;

// 功能：适配 NeoForge 1.21.1 顶点提交流程，使用 addVertex/setColor 等新链式 API。

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;

public class DrawShape {
        public static void drawArc(GuiGraphics gg, int cx, int cy, int radius, int thickness,
                                   int argb, float startAngleDeg, float endAngleDeg) {
            // 计算段数，角度越大越细致，但别超过 360
            int segments = Math.max(16, (int) (Math.abs(endAngleDeg - startAngleDeg) / 360f * 180f));
            segments = Math.min(segments, 36);

            float start = (float) Math.toRadians(startAngleDeg);
            float end = (float) Math.toRadians(endAngleDeg);
            float step = (end - start) / segments;

            float a = (float) (argb >> 24 & 255) / 255.0F;
            float r = (float) (argb >> 16 & 255) / 255.0F;
            float g = (float) (argb >> 8  & 255) / 255.0F;
            float b = (float) (argb       & 255) / 255.0F;

            Matrix4f mat = gg.pose().last().pose();

            RenderSystem.setShader(GameRenderer::getPositionColorShader);  // 或者直接用 gui 的
            RenderSystem.enableBlend();
            // 功能：NeoForge 1.21.1 使用 Tesselator.begin(...) 直接创建并开始写入缓冲。
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            float innerR = radius - thickness;

            // 使用 TRIANGLE_STRIP 只需要两个点一组就能画出整个环
            for (int i = 0; i <= segments; i++) {
                float angle = start + i * step;

                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                // 外圈点
                float ox = cos * radius;
                float oy = sin * radius;
                // 内圈点
                float ix = cos * innerR;
                float iy = sin * innerR;

                // 先外后内，或者先内后外都行，只要顺序一致即可
                buffer.addVertex(mat, cx + ox, cy + oy, 0).setColor(r, g, b, a);
                buffer.addVertex(mat, cx + ix, cy + iy, 0).setColor(r, g, b, a);
            }

            // 功能：NeoForge 1.21.1 通过 buildOrThrow() 结束构建并提交到 GPU。
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            RenderSystem.disableBlend();
        }

        public static void drawPartialArc(GuiGraphics gg, int cx, int cy, int radius, int thickness,
                                          int argb, float startAngleDeg, float endAngleDeg) {

            float startRad = (float) Math.toRadians(startAngleDeg);
            float endRad = (float) Math.toRadians(endAngleDeg);
            if (endRad < startRad) endRad += Math.PI * 2f;

            int segments = Math.max(16, (int) Math.toDegrees(endRad - startRad)); // 每度1段就够顺滑了

            float a = (argb >> 24 & 255) / 255f;
            float r = (argb >> 16 & 255) / 255f;
            float g = (argb >>  8 & 255) / 255f;
            float b = (argb & 255) / 255f;

            var pose = gg.pose().last().pose();

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            // 功能：NeoForge 1.21.1 使用 Tesselator.begin(...) 直接创建并开始写入缓冲。
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            float innerR = radius - thickness;

            // Draw the partial arc
            for (int i = 0; i <= segments; i++) {
                float angle = startRad + i * (endRad - startRad) / segments;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                float outerX = cx + cos * radius;
                float outerY = cy + sin * radius;
                float innerX = cx + cos * innerR;
                float innerY = cy + sin * innerR;

                // Outer ring
                buffer.addVertex(pose, outerX, outerY, 0).setColor(r, g, b, a);
                // Inner ring
                buffer.addVertex(pose, innerX, innerY, 0).setColor(r, g, b, a);
            }

            // Finish the current drawing operation
            // 功能：NeoForge 1.21.1 通过 buildOrThrow() 结束构建并提交到 GPU。
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            // Disable blend mode after drawing
            RenderSystem.disableBlend();
        }

    public static void drawHollowRectangle(GuiGraphics gg, int centerX, int centerY,
                                           int width, int height, int thickness, int argb) {
        float halfOuterWidth = width / 2.0f;
        float halfOuterHeight = height / 2.0f;
        float halfInnerWidth = halfOuterWidth - thickness;
        float halfInnerHeight = halfOuterHeight - thickness;

        // 防止厚度过大导致内框负数
        if (halfInnerWidth < 0) halfInnerWidth = 0;
        if (halfInnerHeight < 0) halfInnerHeight = 0;

        float a = (argb >> 24 & 255) / 255.0f;
        float r = (argb >> 16 & 255) / 255.0f;
        float g = (argb >>  8 & 255) / 255.0f;
        float b = (argb       & 255) / 255.0f;

        Matrix4f mat = gg.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();

        // 功能：NeoForge 1.21.1 使用 Tesselator.begin(...) 直接创建并开始写入缓冲。
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // 按顺序画四条边（每条边用两个点 + 退化连接）
        // 左上外 → 左上内 → 右上外 → 右上内 → 右下外 → 右下内 → 左下外 → 左下内 → 回到左上外

        // 左上外
        buffer.addVertex(mat, centerX - halfOuterWidth, centerY - halfOuterHeight, 0).setColor(r,g,b,a);
        // 左上内
        buffer.addVertex(mat, centerX - halfInnerWidth, centerY - halfInnerHeight, 0).setColor(r,g,b,a);

        // 右上外
        buffer.addVertex(mat, centerX + halfOuterWidth, centerY - halfOuterHeight, 0).setColor(r,g,b,a);
        // 右上内
        buffer.addVertex(mat, centerX + halfInnerWidth, centerY - halfInnerHeight, 0).setColor(r,g,b,a);

        // 右下外
        buffer.addVertex(mat, centerX + halfOuterWidth, centerY + halfOuterHeight, 0).setColor(r,g,b,a);
        // 右下内
        buffer.addVertex(mat, centerX + halfInnerWidth, centerY + halfInnerHeight, 0).setColor(r,g,b,a);

        // 左下外
        buffer.addVertex(mat, centerX - halfOuterWidth, centerY + halfOuterHeight, 0).setColor(r,g,b,a);
        // 左下内
        buffer.addVertex(mat, centerX - halfInnerWidth, centerY + halfInnerHeight, 0).setColor(r,g,b,a);

        // 闭合回到起点（左上外）
        buffer.addVertex(mat, centerX - halfOuterWidth, centerY - halfOuterHeight, 0).setColor(r,g,b,a);
        buffer.addVertex(mat, centerX - halfInnerWidth, centerY - halfInnerHeight, 0).setColor(r,g,b,a);

        // 功能：NeoForge 1.21.1 通过 buildOrThrow() 结束构建并提交到 GPU。
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
    }


    public static void drawThickLine(GuiGraphics gg,
                                     int x1, int y1,
                                     int x2, int y2,
                                     int thickness,
                                     int argb) {
        if (thickness <= 0) return;

        float a = ((argb >> 24) & 255) / 255f;
        float r = ((argb >> 16) & 255) / 255f;
        float g = ((argb >>  8) & 255) / 255f;
        float b = ( argb        & 255) / 255f;

        Matrix4f mat = gg.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        // 如果你发现颜色偏暗或不显示，可以临时加这行测试：
        // RenderSystem.disableCull();   // 禁用背面剔除（调试用，正式不建议长期开）

        // 功能：NeoForge 1.21.1 使用 Tesselator.begin(...) 直接创建并开始写入缓冲。
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx*dx + dy*dy);

        if (len < 0.01f) {
            RenderSystem.disableBlend();
            return;
        }

        // 关键：逆时针法线（从起点看过去，左侧在上）
        float nx =  dy / len;
        float ny = -dx / len;

        float half = thickness / 2f;

        // 逆时针顺序
        buffer.addVertex(mat, x1 + nx * half, y1 + ny * half, 0).setColor(r,g,b,a); // 起点左
        buffer.addVertex(mat, x1 - nx * half, y1 - ny * half, 0).setColor(r,g,b,a); // 起点右
        buffer.addVertex(mat, x2 + nx * half, y2 + ny * half, 0).setColor(r,g,b,a); // 终点左
        buffer.addVertex(mat, x2 - nx * half, y2 - ny * half, 0).setColor(r,g,b,a); // 终点右

        // 功能：NeoForge 1.21.1 通过 buildOrThrow() 结束构建并提交到 GPU。
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

}
