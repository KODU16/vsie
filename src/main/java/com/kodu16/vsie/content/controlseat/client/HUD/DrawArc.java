package com.kodu16.vsie.content.controlseat.client.HUD;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;

public class DrawArc {
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




    /*private static void fillQuad(GuiGraphics gg,
                                 float x0, float y0,
                                 float x1, float y1,
                                 float x2, float y2,
                                 float x3, float y3,
                                 int argb) {

        float a = (float) (argb >> 24 & 255) / 255.0F;
        float r = (float) (argb >> 16 & 255) / 255.0F;
        float g = (float) (argb >>  8 & 255) / 255.0F;
        float b = (float) (argb       & 255) / 255.0F;

        Matrix4f mat = gg.pose().last().pose();

        // 1) 设定着色器（纯位置+颜色）
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // 2) 可选：在HUD上通常需要开启混合
        RenderSystem.enableBlend();
        // RenderSystem.defaultBlendFunc(); // 如需默认混合，可打开

        // 3) 用 Tesselator 提供的 BufferBuilder
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buffer.vertex(mat, x0, y0, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(mat, x1, y1, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(mat, x2, y2, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(mat, x3, y3, 0.0F).color(r, g, b, a).endVertex();

        // 4) 提交绘制（1.20.1 没有 buildOrThrow；使用 end() + drawWithShader）
        BufferUploader.drawWithShader(buffer.end());

        // 5) 可选：恢复状态
        RenderSystem.disableBlend();
    }*/
}
