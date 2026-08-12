package com.kodu16.vsie.content.misc.electromagnet_rail.structure.core;

import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ElectroMagnetRailCoreBeamLayer extends GeoRenderLayer<ElectroMagnetRailCoreBlockEntity> {
    private static final RenderType BEAM_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float BEAM_HALF_WIDTH = 3 / 10f;

    public ElectroMagnetRailCoreBeamLayer(GeoRenderer<ElectroMagnetRailCoreBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, ElectroMagnetRailCoreBlockEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer bufferSourceBuffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.hasValidTerminalBinding()) {
            return;
        }

        float maxLength = (float) Math.sqrt(animatable.getBlockPos().distSqr(animatable.getTerminalPos()));
        float beamLength = Math.min(animatable.getBeamRenderDistance(), maxLength);
        if (beamLength <= 0.01f) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0, 0.5f, 0);

        VertexConsumer consumer = bufferSource.getBuffer(BEAM_RENDER_TYPE);
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();

        // The renderer already rotates model-local +Z toward the bound terminal.
        float dirX = 0.0f;
        float dirY = 0.0f;
        float dirZ = 1.0f;
        float sideX = 1.0f;
        float sideY = 0.0f;
        float sideZ = 0.0f;
        float upX = dirY * sideZ - dirZ * sideY;
        float upY = dirZ * sideX - dirX * sideZ;
        float upZ = dirX * sideY - dirY * sideX;

        drawSingleBeam(consumer, pose, normal, dirX, dirY, dirZ,
                sideX * 3.0f, sideY * 3.0f, sideZ * 3.0f, upX, upY, upZ, beamLength);
        drawSingleBeam(consumer, pose, normal, dirX, dirY, dirZ,
                -sideX * 3.0f, -sideY * 3.0f, -sideZ * 3.0f, upX, upY, upZ, beamLength);

        poseStack.popPose();
    }

    private void drawSingleBeam(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                float dirX, float dirY, float dirZ,
                                float offsetX, float offsetY, float offsetZ,
                                float upX, float upY, float upZ,
                                float length) {
        float sx = offsetX;
        float sy = offsetY;
        float sz = offsetZ;
        float ex = sx + dirX * length;
        float ey = sy + dirY * length;
        float ez = sz + dirZ * length;

        float sideX = upY * dirZ - upZ * dirY;
        float sideY = upZ * dirX - upX * dirZ;
        float sideZ = upX * dirY - upY * dirX;

        float ux = upX * BEAM_HALF_WIDTH;
        float uy = upY * BEAM_HALF_WIDTH;
        float uz = upZ * BEAM_HALF_WIDTH;

        float vx = sideX * BEAM_HALF_WIDTH;
        float vy = sideY * BEAM_HALF_WIDTH;
        float vz = sideZ * BEAM_HALF_WIDTH;

        float r = 0.25f;
        float g = 0.75f;
        float b = 1.0f;

        emitQuad(consumer, pose, normal,
                sx + ux + vx, sy + uy + vy, sz + uz + vz,
                sx + ux - vx, sy + uy - vy, sz + uz - vz,
                ex + ux - vx, ey + uy - vy, ez + uz - vz,
                ex + ux + vx, ey + uy + vy, ez + uz + vz,
                r, g, b, 0.85f, 0.15f);

        emitQuad(consumer, pose, normal,
                sx - ux - vx, sy - uy - vy, sz - uz - vz,
                sx - ux + vx, sy - uy + vy, sz - uz + vz,
                ex - ux + vx, ey - uy + vy, ez - uz + vz,
                ex - ux - vx, ey - uy - vy, ez - uz - vz,
                r, g, b, 0.85f, 0.15f);

        emitQuad(consumer, pose, normal,
                sx + ux - vx, sy + uy - vy, sz + uz - vz,
                sx - ux - vx, sy - uy - vy, sz - uz - vz,
                ex - ux - vx, ey - uy - vy, ez - uz - vz,
                ex + ux - vx, ey + uy - vy, ez + uz - vz,
                r, g, b, 0.85f, 0.15f);

        emitQuad(consumer, pose, normal,
                sx - ux + vx, sy - uy + vy, sz - uz + vz,
                sx + ux + vx, sy + uy + vy, sz + uz + vz,
                ex + ux + vx, ey + uy + vy, ez + uz + vz,
                ex - ux + vx, ey - uy + vy, ez - uz + vz,
                r, g, b, 0.85f, 0.15f);
    }

    private void emitQuad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float x3, float y3, float z3,
                          float x4, float y4, float z4,
                          float r, float g, float b, float alphaStart, float alphaEnd) {
        vertex(vc, pose, normal, x1, y1, z1, r, g, b, alphaStart);
        vertex(vc, pose, normal, x2, y2, z2, r, g, b, alphaStart);
        vertex(vc, pose, normal, x3, y3, z3, r, g, b, alphaEnd);
        vertex(vc, pose, normal, x4, y4, z4, r, g, b, alphaEnd);
    }

    private void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                        float x, float y, float z,
                        float r, float g, float b, float a) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setOverlay(0)
                .setLight(FULL_BRIGHT)
                .setNormal(0, 1, 0);
    }
}
