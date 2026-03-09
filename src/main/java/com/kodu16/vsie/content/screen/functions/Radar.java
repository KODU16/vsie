package com.kodu16.vsie.content.screen.functions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public class Radar {

    // 功能：在屏幕平面绘制一个实心小方框，作为雷达上的船只标记。
    public static void drawSquare(PoseStack poseStack, MultiBufferSource bufferSource, float centerX, float centerY, float halfSize, int argb) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.gui());
        Matrix4f matrix = poseStack.last().pose();

        float minX = centerX - halfSize;
        float maxX = centerX + halfSize;
        float minY = centerY - halfSize;
        float maxY = centerY + halfSize;

        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        consumer.vertex(matrix, minX, minY, 0).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, 0).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, 0).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, minY, 0).color(r, g, b, a).endVertex();
    }
}
