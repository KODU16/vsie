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

public class TurretFlameLayer extends GeoRenderLayer<AbstractTurretBlockEntity> {

    public TurretFlameLayer(GeoRenderer<AbstractTurretBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    // 功能：将特效绑定到炮管骨骼上，仅在该骨骼执行炮口焰绘制。
    private static final String CANNON_BONE_NAME = "cannonend";
    // 功能：使用加法混合渲染炮口焰与闪电，提升发光感。
    private static final RenderType FLAME_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;
    // 功能：固定为全亮光照，避免受环境亮度影响导致特效变暗。
    private static final int FULL_BRIGHT = 0xF000F0;

    // 功能：定义火焰锥体沿长度方向的分段精度。
    private static final int FLAME_LENGTH_SEGMENTS = 6;
    // 功能：定义火焰外壳径向分段数量。
    private static final int FLAME_RADIAL_SEGMENTS = 8;
    // 功能：定义闪电分支数量，形成“火焰+闪电”的复合效果。
    private static final int LIGHTNING_BRANCHES = 3;
    // 功能：定义每个闪电分支的折线段数量。
    private static final int LIGHTNING_SEGMENTS = 5;

    // 功能：保存当前目标距离，供本次渲染判定是否需要显示炮口焰。
    private double flameLength = 0d;

    @Override
    public void render(PoseStack poseStack, AbstractTurretBlockEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer bufferSourceBuffer,
                       float partialTick, int packedLight, int packedOverlay) {
        // 功能：读取当前目标距离，供本层做长度/状态判断（保留原有数据通路）。
        flameLength = animatable.getTargetdistance();
        // 功能：改为使用“开火后延时标记”控制显示，避免 targetdistance 归零导致火焰只闪一帧。
        if (!animatable.shouldRenderMuzzleFlash()) {
            return;
        }

        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, bufferSourceBuffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public void renderForBone(PoseStack poseStack, AbstractTurretBlockEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {

        if (!CANNON_BONE_NAME.equals(bone.getName())) {
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay);
            return;
        }

        poseStack.pushPose();
        // 功能：对齐炮管朝向并应用已有的炮口 Y 偏移，确保特效附着在炮口位置。
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(0, animatable.getYAxisOffset(), 0);

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();
        VertexConsumer vc = bufferSource.getBuffer(FLAME_RENDER_TYPE);

        // 功能：将炮口焰尺寸限制在 1*1*1 方块范围内（半径 0.5，长度 1.0）。
        renderFlameCore(vc, pose, normal);
        // 功能：叠加火焰外壳，增强“火焰喷发”层次感。
        renderFlameShell(vc, pose, normal);
        // 功能：叠加闪电分支，满足“带有闪电的炮口发射焰”视觉需求。
        renderLightningBranches(vc, pose, normal, animatable.getBlockPos().asLong());

        poseStack.popPose();
    }

    // 功能：绘制内层高亮火焰锥体。
    private static void renderFlameCore(VertexConsumer vc, Matrix4f pose, Matrix3f normal) {
        renderTaperedFlame(vc, pose, normal,
                0.45f, 0.05f,
                0.95f, 0.82f, 0.25f, 0.85f,
                0.95f, 0.45f, 0.08f, 0.12f);
    }

    // 功能：绘制外层半透明火焰锥体，形成柔和边缘。
    private static void renderFlameShell(VertexConsumer vc, Matrix4f pose, Matrix3f normal) {
        renderTaperedFlame(vc, pose, normal,
                0.5f, 0.15f,
                0.95f, 0.55f, 0.10f, 0.45f,
                0.85f, 0.20f, 0.08f, 0.05f);
    }

    // 功能：按参数绘制锥形火焰网格。
    private static void renderTaperedFlame(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                           float baseRadius, float tipRadius,
                                           float r0, float g0, float b0, float a0,
                                           float r1, float g1, float b1, float a1) {
        for (int seg = 0; seg < FLAME_RADIAL_SEGMENTS; seg++) {
            float a1Rad = (seg / (float) FLAME_RADIAL_SEGMENTS) * M_2PI;
            float a2Rad = ((seg + 1f) / FLAME_RADIAL_SEGMENTS) * M_2PI;

            float cos1 = (float) Math.cos(a1Rad);
            float sin1 = (float) Math.sin(a1Rad);
            float cos2 = (float) Math.cos(a2Rad);
            float sin2 = (float) Math.sin(a2Rad);

            for (int i = 0; i < FLAME_LENGTH_SEGMENTS; i++) {
                float t0 = i / (float) FLAME_LENGTH_SEGMENTS;
                float t1 = (i + 1f) / FLAME_LENGTH_SEGMENTS;

                float z0 = t0;
                float z1 = t1;
                float radius0 = lerp(baseRadius, tipRadius, t0);
                float radius1 = lerp(baseRadius, tipRadius, t1);

                float rStart = lerp(r0, r1, t0);
                float gStart = lerp(g0, g1, t0);
                float bStart = lerp(b0, b1, t0);
                float aStart = lerp(a0, a1, t0);

                float rEnd = lerp(r0, r1, t1);
                float gEnd = lerp(g0, g1, t1);
                float bEnd = lerp(b0, b1, t1);
                float aEnd = lerp(a0, a1, t1);

                vertex(vc, pose, normal, radius0 * cos1, radius0 * sin1, z0, rStart, gStart, bStart, aStart);
                vertex(vc, pose, normal, radius0 * cos2, radius0 * sin2, z0, rStart, gStart, bStart, aStart);
                vertex(vc, pose, normal, radius1 * cos2, radius1 * sin2, z1, rEnd, gEnd, bEnd, aEnd);
                vertex(vc, pose, normal, radius1 * cos1, radius1 * sin1, z1, rEnd, gEnd, bEnd, aEnd);
            }
        }
    }

    // 功能：绘制带细微抖动的闪电分支，限制在 1*1*1 范围内。
    private static void renderLightningBranches(VertexConsumer vc, Matrix4f pose, Matrix3f normal, long seed) {
        for (int branch = 0; branch < LIGHTNING_BRANCHES; branch++) {
            float startAngle = (branch / (float) LIGHTNING_BRANCHES) * M_2PI;
            float phase = pseudoRandom(seed, branch);

            float prevX = 0.18f * (float) Math.cos(startAngle);
            float prevY = 0.18f * (float) Math.sin(startAngle);
            float prevZ = 0.0f;

            for (int i = 1; i <= LIGHTNING_SEGMENTS; i++) {
                float t = i / (float) LIGHTNING_SEGMENTS;
                float z = t;
                float sway = 0.06f * (float) Math.sin((t * 11f) + phase * 6.28f);
                float angle = startAngle + t * 1.4f;
                float radius = lerp(0.18f, 0.04f, t);

                float x = radius * (float) Math.cos(angle) + sway;
                float y = radius * (float) Math.sin(angle) - sway;

                // 功能：每段闪电使用极细四边形表现，增强“电弧”可见性。
                addLightningQuad(vc, pose, normal, prevX, prevY, prevZ, x, y, z);

                prevX = x;
                prevY = y;
                prevZ = z;
            }
        }
    }

    // 功能：为单段闪电生成一个朝向固定的细长面片。
    private static void addLightningQuad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                         float x0, float y0, float z0,
                                         float x1, float y1, float z1) {
        float width = 0.015f;

        vertex(vc, pose, normal, x0 - width, y0 + width, z0, 0.6f, 0.85f, 1.0f, 0.8f);
        vertex(vc, pose, normal, x0 + width, y0 - width, z0, 0.6f, 0.85f, 1.0f, 0.8f);
        vertex(vc, pose, normal, x1 + width, y1 - width, z1, 0.8f, 0.95f, 1.0f, 0.3f);
        vertex(vc, pose, normal, x1 - width, y1 + width, z1, 0.8f, 0.95f, 1.0f, 0.3f);
    }

    // 功能：提供稳定插值计算。
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // 功能：生成与炮塔位置相关的伪随机值，让不同炮塔闪电形态略有差异。
    private static float pseudoRandom(long seed, int branch) {
        long mixed = seed ^ (branch * 0x9E3779B97F4A7C15L);
        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= (mixed >>> 33);
        return (mixed & 0xFFFF) / 65535.0f;
    }

    // 功能：保存圆周常量，避免重复计算。
    private static final float M_2PI = (float) (Math.PI * 2);

    // 功能：统一写入顶点属性，使用全亮和颜色渲染特效。
    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z,
                               float r, float g, float b, float a) {
        vc.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .overlayCoords(0)
                .uv2(FULL_BRIGHT)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }
}
