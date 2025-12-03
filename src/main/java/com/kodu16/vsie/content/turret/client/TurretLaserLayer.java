package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.foundation.translucentbeamrendertype;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.renderer.GeoRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

// 你的 BlockEntity 类型
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;

public class TurretLaserLayer extends GeoRenderLayer<AbstractTurretBlockEntity> {

    public TurretLaserLayer(GeoRenderer<AbstractTurretBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }
    private static final int SEGMENTS = 4;
    private static final int LENGTH_SEGMENTS = 12;
    public double FLAME_LENGTH = 0f;
    private static final float BASE_RADIUS = 0.3f;
    private static final float TIP_RADIUS = 0.3f;

    // 直接使用我们自己定义的 RenderType
    private static final RenderType FLAME_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;

    // 全亮光照（因为我们禁用了 lightmap）
    private static final int FULL_BRIGHT = 0xF000F0;
    @Override
    public void render(PoseStack poseStack, AbstractTurretBlockEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer bufferSourceBuffer,
                       float partialTick, int packedLight, int packedOverlay) {

        poseStack.pushPose();
        poseStack.translate(0f, 2.5f, 0f);

        // 如果你以后需要根据 thruster 朝向旋转，这里可以打开
        // poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getDirection().toYRot()));

        FLAME_LENGTH = animatable.getTargetdistance();
        if (FLAME_LENGTH < 0.1) {
            poseStack.popPose();
            return;
        }

        Vector3d targetPos = animatable.getTargetPos();
        Vector3d turretPos = animatable.getTurretPos();

        Vec3 direction = new Vec3(
                targetPos.x - turretPos.x,
                targetPos.y - turretPos.y,
                targetPos.z - turretPos.z
        ).normalize();

        Matrix3f rot = new Matrix3f();
        lookAlong(rot, direction);

// 旋转法线
        poseStack.last().normal().mul(rot);
// 旋转位置（关键修复）
        poseStack.last().pose().mul(new Matrix4f().set(rot));

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();

        // 使用我们自定义的 RenderType
        VertexConsumer vc = bufferSource.getBuffer(FLAME_RENDER_TYPE);

        float[][] layers = new float[LENGTH_SEGMENTS + 1][];

        for (int i = 0; i <= LENGTH_SEGMENTS; i++) {
            float t = i / (float) LENGTH_SEGMENTS;

            float z = (float) (-t * FLAME_LENGTH);
            float radius = BASE_RADIUS + (TIP_RADIUS - BASE_RADIUS) * t;

            float r = lerp(0.7f, 0.3f, t);
            float g = lerp(0.4f, 0.4f, t);
            float b = lerp(0.9f, 0.9f, t);
            float a = lerp(0.5f, 0.5f, t);

            layers[i] = new float[]{z, radius, r, g, b, a};
        }

        // 渲染圆锥侧面
        for (int seg = 0; seg < SEGMENTS; seg++) {
            float a1 = (seg) / (float) SEGMENTS * M_2PI;
            float a2 = (seg + 1f) / (float) SEGMENTS * M_2PI;

            float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2), sin2 = (float) Math.sin(a2);

            for (int i = 0; i < LENGTH_SEGMENTS; i++) {
                float[] p1 = layers[i];
                float[] p2 = layers[i + 1];

                float z1 = p1[0]; float r1 = p1[1];
                float z2 = p2[0]; float r2 = p2[1];

                vertex(vc, pose, normal, r1 * cos1, r1 * sin1, z1, p1[2], p1[3], p1[4], p1[5]);
                vertex(vc, pose, normal, r1 * cos2, r1 * sin2, z1, p1[2], p1[3], p1[4], p1[5]);
                vertex(vc, pose, normal, r2 * cos2, r2 * sin2, z2, p2[2], p2[3], p2[4], p2[5]);
                vertex(vc, pose, normal, r2 * cos1, r2 * sin1, z2, p2[2], p2[3], p2[4], p2[5]);
            }
        }

        poseStack.popPose();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static final float M_2PI = (float) (Math.PI * 2);

    // 去掉 light 参数，直接写死全亮
    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z,
                               float r, float g, float b, float a) {
        vc.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .overlayCoords(0)
                .uv2(FULL_BRIGHT)                 // 全亮
                .normal(normal, 0, 1, 0)          // 法线随便填，shader 不使用光照
                .endVertex();
    }

    // 让矩阵的 -Z 轴朝向 dir（dir 必须是单位向量）
    private static void lookAlong(Matrix3f mat, Vec3 dir) {
        double x = dir.x;
        double y = dir.y;
        double z = dir.z;

        // 构造一个临时的 right 向量
        Vec3 up = Math.abs(z) < 0.999 ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1);
        Vec3 right = dir.cross(up).normalize();
        Vec3 newUp = right.cross(dir);

        mat.set(
                (float) right.x,   (float) right.y,   (float) right.z,
                (float) newUp.x,   (float) newUp.y,   (float) newUp.z,
                (float) -dir.x,    (float) -dir.y,    (float) -dir.z
        );
    }

}