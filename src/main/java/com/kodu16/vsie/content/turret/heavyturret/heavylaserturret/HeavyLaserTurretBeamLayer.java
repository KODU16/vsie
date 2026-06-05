package com.kodu16.vsie.content.turret.heavyturret.heavylaserturret;

import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

public class HeavyLaserTurretBeamLayer extends GeoRenderLayer<HeavyLaserTurretBlockEntity> {
    private static final String LASER_BONE = "locater";
    private static final RenderType LASER_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int LENGTH_SEGMENTS = 1;
    private static final float LASER_HALF_SIZE = 0.8F;

    public HeavyLaserTurretBeamLayer(GeoRenderer<HeavyLaserTurretBlockEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, HeavyLaserTurretBlockEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer bufferSourceBuffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (animatable.getTargetDistance() < 0.1D) {
            return;
        }
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, bufferSourceBuffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public void renderForBone(PoseStack poseStack, HeavyLaserTurretBlockEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (!LASER_BONE.equals(bone.getName())) {
            return;
        }

        poseStack.pushPose();
        // Function: draw the heavy laser directly from the locater bone, matching the server raycast origin.
        RenderUtil.translateMatrixToBone(poseStack, bone);
        RenderUtil.rotateMatrixAroundBone(poseStack, bone);
        RenderUtil.scaleMatrixForBone(poseStack, bone);
        poseStack.translate(0.0D, animatable.getYAxisOffset(), 0.0D);
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();
        // Function: raycast distance is owned by the block entity; this layer only renders that length from the locater bone.
        float length = (float) animatable.getTargetDistance();
        if (length < 0.1F) {
            poseStack.popPose();
            return;
        }
        VertexConsumer vc = bufferSource.getBuffer(LASER_RENDER_TYPE);
        drawBeam(vc, pose, normal, length);
        poseStack.popPose();
    }

    private static void drawBeam(VertexConsumer vc, Matrix4f pose, Matrix3f normal, float length) {
        float[][] layers = new float[LENGTH_SEGMENTS + 1][];
        for (int i = 0; i <= LENGTH_SEGMENTS; i++) {
            float t = i / (float) LENGTH_SEGMENTS;
            // Function: keep the heavy laser as a constant radius-1 square beam for its full hit distance.
            float halfSize = LASER_HALF_SIZE;
            layers[i] = new float[]{t * length, halfSize, lerp(0.30F, 0.45F, t), lerp(0.35F, 0.5F, t), 1.0F, lerp(0.35F, 0.75F, t)};
        }

        for (int i = 0; i < LENGTH_SEGMENTS; i++) {
            float[] p1 = layers[i];
            float[] p2 = layers[i + 1];
            // Function: render a square-section beam, using half-size as the requested radius.
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

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
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
