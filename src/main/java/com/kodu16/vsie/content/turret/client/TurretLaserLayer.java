package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

public class TurretLaserLayer<T extends AbstractTurretBlockEntity> extends GeoRenderLayer<T> {
    private static final int SEGMENTS = 4;
    private static final float M_2PI = (float) (Math.PI * 2);
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final RenderType FLAME_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;

    public TurretLaserLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer bufferSourceBuffer,
                       float partialTick, int packedLight, int packedOverlay) {
        double flameLength = animatable.getTargetDistance();
        if (flameLength < 0.1D) {
            return;
        }
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, bufferSourceBuffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.getLaserLayerBoneName().equals(bone.getName())) {
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay);
            return;
        }

        // Function: always read beam length from the current turret instance so one turret render cannot leak into another.
        double flameLength = animatable.getTargetDistance();
        if (flameLength < 0.1D) {
            return;
        }

        poseStack.pushPose();
        if (animatable.transformsLaserLayerFromBone()) {
            // Function: heavy laser samples the locater bone directly, matching its server-side raycast origin.
            RenderUtil.translateMatrixToBone(poseStack, bone);
            RenderUtil.rotateMatrixAroundBone(poseStack, bone);
            RenderUtil.scaleMatrixForBone(poseStack, bone);
        }
        if (animatable.flipsLaserLayerDirection()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }
        poseStack.translate(0, animatable.getLaserLayerYOffset(), 0);
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();
        VertexConsumer vc = bufferSource.getBuffer(FLAME_RENDER_TYPE);
        float length = (float) flameLength;
        float radius = animatable.getLaserLayerRadius();

        if (animatable.usesSquareLaserLayerBeam()) {
            drawSquareBeam(animatable, vc, pose, normal, length, radius);
        } else {
            drawSegmentedBeam(animatable, vc, pose, normal, length, radius);
        }

        poseStack.popPose();
    }

    private void drawSegmentedBeam(T animatable, VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                   float length, float radius) {
        int lengthSegments = Math.max(1, animatable.getLaserLayerLengthSegments());
        float[][] layers = buildLayers(animatable, length, radius, lengthSegments);

        for (int seg = 0; seg < SEGMENTS; seg++) {
            float a1 = seg / (float) SEGMENTS * M_2PI;
            float a2 = (seg + 1f) / (float) SEGMENTS * M_2PI;
            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2);
            float sin2 = (float) Math.sin(a2);

            for (int i = 0; i < lengthSegments; i++) {
                float[] p1 = layers[i];
                float[] p2 = layers[i + 1];
                float z1 = p1[0];
                float r1 = p1[1];
                float z2 = p2[0];
                float r2 = p2[1];

                vertex(vc, pose, normal, r1 * cos1, r1 * sin1, z1, p1[2], p1[3], p1[4], p1[5]);
                vertex(vc, pose, normal, r1 * cos2, r1 * sin2, z1, p1[2], p1[3], p1[4], p1[5]);
                vertex(vc, pose, normal, r2 * cos2, r2 * sin2, z2, p2[2], p2[3], p2[4], p2[5]);
                vertex(vc, pose, normal, r2 * cos1, r2 * sin1, z2, p2[2], p2[3], p2[4], p2[5]);
            }
        }
    }

    private void drawSquareBeam(T animatable, VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                float length, float radius) {
        int lengthSegments = Math.max(1, animatable.getLaserLayerLengthSegments());
        float[][] layers = buildLayers(animatable, length, radius, lengthSegments);

        for (int i = 0; i < lengthSegments; i++) {
            float[] p1 = layers[i];
            float[] p2 = layers[i + 1];
            // Function: square beams use the declared laser radius as the half-size on both local axes.
            quad(vc, pose, normal, -p1[1], -p1[1], p1[0], p1[1], -p1[1], p1[0],
                    p2[1], -p2[1], p2[0], -p2[1], -p2[1], p2[0], p1, p2);
            quad(vc, pose, normal, p1[1], -p1[1], p1[0], p1[1], p1[1], p1[0],
                    p2[1], p2[1], p2[0], p2[1], -p2[1], p2[0], p1, p2);
            quad(vc, pose, normal, p1[1], p1[1], p1[0], -p1[1], p1[1], p1[0],
                    -p2[1], p2[1], p2[0], p2[1], p2[1], p2[0], p1, p2);
            quad(vc, pose, normal, -p1[1], p1[1], p1[0], -p1[1], -p1[1], p1[0],
                    -p2[1], -p2[1], p2[0], -p2[1], p2[1], p2[0], p1, p2);
        }
    }

    private float[][] buildLayers(T animatable, float length, float radius, int lengthSegments) {
        float[][] layers = new float[lengthSegments + 1][];
        for (int i = 0; i <= lengthSegments; i++) {
            float t = i / (float) lengthSegments;
            layers[i] = new float[]{
                    t * length,
                    radius,
                    animatable.getLaserLayerRed(t),
                    animatable.getLaserLayerGreen(t),
                    animatable.getLaserLayerBlue(t),
                    animatable.getLaserLayerAlpha(t)
            };
        }
        return layers;
    }

    private static void quad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float[] p1, float[] p2) {
        vertex(vc, pose, normal, x1, y1, z1, p1[2], p1[3], p1[4], p1[5]);
        vertex(vc, pose, normal, x2, y2, z2, p1[2], p1[3], p1[4], p1[5]);
        vertex(vc, pose, normal, x3, y3, z3, p2[2], p2[3], p2[4], p2[5]);
        vertex(vc, pose, normal, x4, y4, z4, p2[2], p2[3], p2[4], p2[5]);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z,
                               float r, float g, float b, float a) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setOverlay(0)
                .setLight(FULL_BRIGHT)
                .setNormal(0, 1, 0);
    }
}
