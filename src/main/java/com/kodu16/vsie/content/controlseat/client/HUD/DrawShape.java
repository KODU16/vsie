package com.kodu16.vsie.content.controlseat.client.HUD;

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
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

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
                buffer.vertex(mat, cx + ox, cy + oy, 0).color(r, g, b, a).endVertex();
                buffer.vertex(mat, cx + ix, cy + iy, 0).color(r, g, b, a).endVertex();
            }

            BufferUploader.drawWithShader(buffer.end());

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
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

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
                buffer.vertex(pose, outerX, outerY, 0).color(r, g, b, a).endVertex();
                // Inner ring
                buffer.vertex(pose, innerX, innerY, 0).color(r, g, b, a).endVertex();
            }

            // Finish the current drawing operation
            BufferUploader.drawWithShader(buffer.end());

            // Disable blend mode after drawing
            RenderSystem.disableBlend();
        }

        public static void drawHollowSquare(GuiGraphics gg, int centerX, int centerY,
                                            int sideLength, int thickness, int argb) {
            float halfOuter = sideLength / 2.0f;
            float halfInner = halfOuter - thickness;

            // 防止厚度过大导致内框负数
            if (halfInner < 0) halfInner = 0;

            float a = (argb >> 24 & 255) / 255.0f;
            float r = (argb >> 16 & 255) / 255.0f;
            float g = (argb >>  8 & 255) / 255.0f;
            float b = (argb       & 255) / 255.0f;

            Matrix4f mat = gg.pose().last().pose();

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();

            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            // 按顺序画四条边（每条边用两个点 + 退化连接）
            // 左上外 → 左上内 → 右上外 → 右上内 → 右下外 → 右下内 → 左下外 → 左下内 → 回到左上外

            // 左上外
            buffer.vertex(mat, centerX - halfOuter, centerY - halfOuter, 0).color(r,g,b,a).endVertex();
            // 左上内
            buffer.vertex(mat, centerX - halfInner, centerY - halfInner, 0).color(r,g,b,a).endVertex();

            // 右上外
            buffer.vertex(mat, centerX + halfOuter, centerY - halfOuter, 0).color(r,g,b,a).endVertex();
            // 右上内
            buffer.vertex(mat, centerX + halfInner, centerY - halfInner, 0).color(r,g,b,a).endVertex();

            // 右下外
            buffer.vertex(mat, centerX + halfOuter, centerY + halfOuter, 0).color(r,g,b,a).endVertex();
            // 右下内
            buffer.vertex(mat, centerX + halfInner, centerY + halfInner, 0).color(r,g,b,a).endVertex();

            // 左下外
            buffer.vertex(mat, centerX - halfOuter, centerY + halfOuter, 0).color(r,g,b,a).endVertex();
            // 左下内
            buffer.vertex(mat, centerX - halfInner, centerY + halfInner, 0).color(r,g,b,a).endVertex();

            // 闭合回到起点（左上外）
            buffer.vertex(mat, centerX - halfOuter, centerY - halfOuter, 0).color(r,g,b,a).endVertex();
            buffer.vertex(mat, centerX - halfInner, centerY - halfInner, 0).color(r,g,b,a).endVertex();

            BufferUploader.drawWithShader(buffer.end());

            RenderSystem.disableBlend();
    }

}
