package com.kodu16.vsie.content.screen.client.functions;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

public class ServerInfo {

    // Function: render server metrics and compact history bars on the screen plane.
    public static void renderServerInfo(PoseStack poseStack,
                                        AbstractScreenBlockEntity screen,
                                        MultiBufferSource bufferSource,
                                        Font font) {
        String[] lines = new String[]{
                "TPS-" + screen.tps,
                "PhysTPS-" + screen.phystps,
                "\u670d\u52a1\u5668\u5185\u5b58\u4f7f\u7528" + Math.round(screen.serverJVMpercentage * 100) + "%",
                "\u5ba2\u6237\u7aef\u5185\u5b58\u4f7f\u7528" + Math.round(screen.clientJVMpercentage * 100) + "%"
        };

        float startX = -85f;
        float startY = -85f;
        float lineHeight = 40f;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            float drawX = startX;
            float drawY = startY + i * lineHeight;
            font.drawInBatch(line, drawX, drawY, 0xFFFFFFFF, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH,
                    0, 0x00F000F0);
        }

        renderHistoryBars(poseStack, screen, startX, startY, lineHeight);
    }

    private static void renderHistoryBars(PoseStack poseStack,
                                          AbstractScreenBlockEntity screen,
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

        boolean[] highIsGood = new boolean[]{true, true, false, false};

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

                int color = highIsGood[row] ? ratioToRedGreenColor(ratio) : ratioToRedGreenColor(1f - ratio);
                drawRect(poseStack, left, top, right, bottom, color);
            }
        }
    }

    private static void drawRect(PoseStack poseStack,
                                 float left,
                                 float top,
                                 float right,
                                 float bottom,
                                 int argb) {
        ScreenShapeRenderer.drawQuad(poseStack.last().pose(), left, top, right, bottom, argb);
    }

    private static int ratioToRedGreenColor(float ratio) {
        float clamped = clamp01(ratio);
        int red = Math.round(255f * (1f - clamped));
        int green = Math.round(255f * clamped);
        int blue = 0;
        int alpha = 255;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        return Math.min(value, 1f);
    }
}
