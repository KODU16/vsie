package com.kodu16.vsie.content.thruster.client.trailflame;

// 功能：适配 NeoForge 1.21.1 顶点提交流程，使用 addVertex/setColor 等新链式 API。

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ThrusterFlameLayer extends GeoRenderLayer<AbstractThrusterBlockEntity> {

    public ThrusterFlameLayer(GeoRenderer<AbstractThrusterBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    private static final String NOZZLE_BONE_NAME = "nozzle";

    private static final int SEGMENTS = 6;           // 圆周分段数
    private static final int LENGTH_SEGMENTS = 16;   // 长度分段数
    private static final RenderType FLAME_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float M_2PI = (float) (Math.PI * 2);

    @Override
    public void render(PoseStack poseStack, AbstractThrusterBlockEntity animatable,
                       software.bernie.geckolib.cache.object.BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        poseStack.pushPose();
        renderflame(poseStack, animatable, bufferSource);
        poseStack.popPose();
    }

    public void renderflame(PoseStack poseStack, AbstractThrusterBlockEntity animatable, MultiBufferSource bufferSource) {
            poseStack.mulPose(Axis.XP.rotationDegrees(270f));
            poseStack.translate(0,0,0.2);
            float length = animatable.getRaycastDistance();
            float flameLength = length * 1.5f;  // 最大长度 4 格，可自行调整

            VertexConsumer vc = bufferSource.getBuffer(FLAME_RENDER_TYPE);

            // 预计算每一层的 z、radius、颜色
            float[][] layers = new float[LENGTH_SEGMENTS + 1][];

            for (int i = 0; i <= LENGTH_SEGMENTS; i++) {
                float t = i / (float) LENGTH_SEGMENTS;
                float z = t * flameLength;                    // 沿 -Z 方向延伸
                float radius = animatable.getflamewidth() + (-0.05f * animatable.getflamewidth()) * t;

                float r = lerp(0.4f, 0.3f, t);
                float g = lerp(0.6f, 0.3f, t);
                float b = lerp(0.6f, 0.9f, t);
                float a = lerp(0.7f, 0.0f, t);                  // 末端更透明

                layers[i] = new float[]{z, radius, r, g, b, a};
            }

            PoseStack.Pose last = poseStack.last();
            Matrix4f pose = last.pose();
            Matrix3f normal = last.normal();

            // 绘制圆锥侧面（四边形条带）
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

                    // 四边形：顺时针或逆时针均可（这里按逆时针，确保正面朝外）
                    vertex(vc, pose, normal, p1[1] * cos1, p1[1] * sin1, p1[0], p1[2], p1[3], p1[4], p1[5]);
                    vertex(vc, pose, normal, p1[1] * cos2, p1[1] * sin2, p1[0], p1[2], p1[3], p1[4], p1[5]);
                    vertex(vc, pose, normal, p2[1] * cos2, p2[1] * sin2, p2[0], p2[2], p2[3], p2[4], p2[5]);
                    vertex(vc, pose, normal, p2[1] * cos1, p2[1] * sin1, p2[0], p2[2], p2[3], p2[4], p2[5]);
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
                .setNormal(0f, 1f, 0f)  // 法线随意，半透明渲染通常不依赖光照
                ;
    }
}
