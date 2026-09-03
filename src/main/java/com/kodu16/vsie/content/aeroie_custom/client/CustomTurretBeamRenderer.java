package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws one full-bright laser along the selected firepoint bone's local +Z axis. */
final class CustomTurretBeamRenderer {
    private static final int DEFAULT_SEGMENTS = 8;
    private static final int FULL_BRIGHT = 0xF000F0;

    private CustomTurretBeamRenderer() {
    }

    static void render(PoseStack poseStack, MultiBufferSource buffers, double distance, float radius, int argb) {
        render(poseStack, buffers, distance, radius, argb, DEFAULT_SEGMENTS);
    }

    static void render(PoseStack poseStack, MultiBufferSource buffers,
                       double distance, float radius, int argb, int segments) {
        if (distance < 0.05D) {
            return;
        }
        int safeSegments = Math.max(3, Math.min(64, segments));
        VertexConsumer consumer = buffers.getBuffer(translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM);
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float red = (argb >>> 16 & 255) / 255.0F;
        float green = (argb >>> 8 & 255) / 255.0F;
        float blue = (argb & 255) / 255.0F;
        float alpha = (argb >>> 24 & 255) / 255.0F;
        for (int segment = 0; segment < safeSegments; segment++) {
            double angleA = Math.PI * 2.0D * segment / safeSegments;
            double angleB = Math.PI * 2.0D * (segment + 1) / safeSegments;
            float xA = (float) Math.cos(angleA) * radius;
            float yA = (float) Math.sin(angleA) * radius;
            float xB = (float) Math.cos(angleB) * radius;
            float yB = (float) Math.sin(angleB) * radius;
            vertex(consumer, pose, normal, xA, yA, 0.0F, red, green, blue, alpha);
            vertex(consumer, pose, normal, xB, yB, 0.0F, red, green, blue, alpha);
            vertex(consumer, pose, normal, xB, yB, (float) distance, red, green, blue, alpha);
            vertex(consumer, pose, normal, xA, yA, (float) distance, red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float red, float green, float blue, float alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setOverlay(0)
                .setLight(FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
