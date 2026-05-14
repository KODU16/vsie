package com.kodu16.vsie.content.vectorthruster.client;

import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlockEntity;
import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class VectorThrusterFlameLayer extends GeoRenderLayer<AbstractVectorThrusterBlockEntity> {

    private static final String NOZZLE_BONE_NAME = "nozzle1";
    private static final int SEGMENTS = 8;
    private static final int LENGTH_SEGMENTS = 16;
    private static final float TIP_RADIUS_SCALE = 0.35f;
    private static final RenderType FLAME_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float M_2PI = (float) (Math.PI * 2);

    public VectorThrusterFlameLayer(GeoRenderer<AbstractVectorThrusterBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, AbstractVectorThrusterBlockEntity animatable,
                       software.bernie.geckolib.cache.object.BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        float flameLength = animatable.getRaycastDistance();
        if (flameLength < 0.05f) {
            return;
        }

        // The server-side visual solver syncs flame length through raycastDistance.
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public void renderForBone(PoseStack poseStack, AbstractVectorThrusterBlockEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (!NOZZLE_BONE_NAME.equals(bone.getName())) {
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay);
            return;
        }

        float flameLength = animatable.getRaycastDistance();
        VertexConsumer vc = bufferSource.getBuffer(FLAME_RENDER_TYPE);
        float[][] layers = new float[LENGTH_SEGMENTS + 1][];
        float baseRadius = animatable.getflamewidth();
        float tipRadius = baseRadius * TIP_RADIUS_SCALE;

        for (int i = 0; i <= LENGTH_SEGMENTS; i++) {
            float t = i / (float) LENGTH_SEGMENTS;
            float y = t * flameLength;
            float radius = baseRadius + (tipRadius - baseRadius) * t;
            float r = lerp(1.0f, 0.6f, t);
            float g = lerp(0.7f, 0.4f, t);
            float b = lerp(0.3f, 0.9f, t);
            float a = lerp(0.8f, 0.3f, t);
            // Function: vector thruster nozzles emit along their local +Y axis, matching the nozzle bone.
            layers[i] = new float[]{y, radius, r, g, b, a};
        }

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();

        for (int seg = 0; seg < SEGMENTS; seg++) {
            float a1 = seg / (float) SEGMENTS * M_2PI;
            float a2 = (seg + 1) / (float) SEGMENTS * M_2PI;
            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2);
            float sin2 = (float) Math.sin(a2);

            for (int i = 0; i < LENGTH_SEGMENTS; i++) {
                float[] p1 = layers[i];
                float[] p2 = layers[i + 1];
                vertex(vc, pose, normal, p1[1] * cos1, p1[0], p1[1] * sin1, p1[2], p1[3], p1[4], p1[5]);
                vertex(vc, pose, normal, p1[1] * cos2, p1[0], p1[1] * sin2, p1[2], p1[3], p1[4], p1[5]);
                vertex(vc, pose, normal, p2[1] * cos2, p2[0], p2[1] * sin2, p2[2], p2[3], p2[4], p2[5]);
                vertex(vc, pose, normal, p2[1] * cos1, p2[0], p2[1] * sin1, p2[2], p2[3], p2[4], p2[5]);
            }
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
    }
}
