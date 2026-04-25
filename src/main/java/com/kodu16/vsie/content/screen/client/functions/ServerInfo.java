package com.kodu16.vsie.content.screen.client.functions;

// 功能：适配 NeoForge 1.21.1 顶点提交流程，使用 addVertex/setColor 等新链式 API。

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public class ServerInfo {

    // 功能：在屏幕 layer 上按行渲染服务器信息文本（信息类型-数值）。
    public static void renderServerInfo(PoseStack poseStack,
                                        AbstractScreenBlockEntity screen,
                                        MultiBufferSource bufferSource,
                                        Font font) {
        // 功能：固定信息文案，满足“每行一个信息”的显示要求。
        String[] lines = new String[]{
                "TPS-" + screen.tps,
                "PhysTPS-" + screen.phystps,
                "服务器内存使用-" + Math.round(screen.serverJVMpercentage * 100) + "%",
                "客户端内存使用-" + Math.round(screen.clientJVMpercentage * 100) + "%"
        };

        // 功能：将文本放到屏幕上端起始位置，并逐行向下偏移。
        float startX = -85f;
        float startY = -85f;
        float lineHeight = 40f;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            float drawX = startX;
            float drawY = (startY + i * lineHeight);
            // 功能：绘制白色文本到当前屏幕 layer。
            font.drawInBatch(line, drawX, drawY, 0xFFFFFFFF, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                    0, 0x00F000F0);
        }

        // 功能：在每行文字下方绘制 20 列动态柱状图，显示四项指标相对最大值的占比。
        renderHistoryBars(poseStack, screen, bufferSource, startX, startY, lineHeight);
    }

    // 功能：绘制四行历史柱状图（TPS/PhysTPS/服务器内存/客户端内存），每次新采样后整体左移。
    private static void renderHistoryBars(PoseStack poseStack,
                                          AbstractScreenBlockEntity screen,
                                          MultiBufferSource bufferSource,
                                          float startX,
                                          float startY,
                                          float lineHeight) {
        int historySize = screen.getServerInfoHistorySize();
        if (historySize <= 0) {
            return;
        }

        float[][] histories = new float[][]{
                screen.getTpsHistory(),
                screen.getPhysTpsHistory(),
                screen.getServerMemoryHistory(),
                screen.getClientMemoryHistory()
        };

        // 功能：标记哪一行是“高值更绿”（TPS 类）与“低值更绿”（内存占用类）。
        boolean[] highIsGood = new boolean[]{true, true, false, false};

        // 功能：柱状图绘制参数；每行最多 20 列，每列 3 像素宽并保留 1 像素间隔。
        float barAreaX = startX;
        float barWidth = 3f;
        float barGap = 1f;
        float maxBarHeight = 24f;
        float barBaselineOffset = 34f;

        for (int row = 0; row < histories.length; row++) {
            float[] history = histories[row];
            float baselineY = startY + row * lineHeight + barBaselineOffset;
            for (int i = 0; i < historySize; i++) {
                float ratio = clamp01(history[i]);
                float left = barAreaX + i * (barWidth + barGap);
                float right = left + barWidth;
                float top = baselineY - ratio * maxBarHeight;
                float bottom = baselineY;

                // 功能：根据比例映射红绿渐变色（TPS 高=绿，内存占用低=绿）。
                int color = highIsGood[row] ? ratioToRedGreenColor(ratio) : ratioToRedGreenColor(1f - ratio);
                drawRect(poseStack, bufferSource, left, top, right, bottom, color);
            }
        }
    }

    // 功能：在屏幕平面绘制实心矩形柱子。
    private static void drawRect(PoseStack poseStack,
                                 MultiBufferSource bufferSource,
                                 float left,
                                 float top,
                                 float right,
                                 float bottom,
                                 int argb) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.gui());
        Matrix4f matrix = poseStack.last().pose();

        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        consumer.addVertex(matrix, left, top, 0).setColor(r, g, b, a);
        consumer.addVertex(matrix, left, bottom, 0).setColor(r, g, b, a);
        consumer.addVertex(matrix, right, bottom, 0).setColor(r, g, b, a);
        consumer.addVertex(matrix, right, top, 0).setColor(r, g, b, a);
    }

    // 功能：把 0~1 的比例映射为“低值偏红、高值偏绿”的 ARGB 颜色。
    private static int ratioToRedGreenColor(float ratio) {
        float clamped = clamp01(ratio);
        int red = Math.round(255f * (1f - clamped));
        int green = Math.round(255f * clamped);
        int blue = 0;
        int alpha = 255;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    // 功能：限制比例在 0~1 之间，避免异常值导致柱子绘制出界。
    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        return Math.min(value, 1f);
    }
}
