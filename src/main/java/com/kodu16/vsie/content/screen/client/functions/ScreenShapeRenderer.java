package com.kodu16.vsie.content.screen.client.functions;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

final class ScreenShapeRenderer {
    private ScreenShapeRenderer() {
    }

    // Function: submit screen-plane quads like the control-seat HUD so Iris keeps non-text shapes visible.
    static void drawQuad(Matrix4f matrix, float left, float top, float right, float bottom, int argb) {
        if (right <= left || bottom <= top) {
            return;
        }

        float a = (argb >> 24 & 255) / 255f;
        float r = (argb >> 16 & 255) / 255f;
        float g = (argb >> 8 & 255) / 255f;
        float b = (argb & 255) / 255f;

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, left, top, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, left, bottom, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, right, bottom, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, right, top, 0).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
