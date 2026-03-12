package com.kodu16.vsie.content.bullet;

import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@SuppressWarnings({"removal"})
public class BulletRenderer<T extends AbstractBulletEntity> extends EntityRenderer<T> {

    public static final ResourceLocation LASER_TEXTURE = new ResourceLocation(vsie.ID, "textures/entity/bullet.png");
    // 激光拖尾横向切分数量，值越大越圆滑。
    private static final int TRAIL_SEGMENTS = 8;
    // 激光拖尾长度切分数量，值越大渐变越平滑。
    private static final int TRAIL_LENGTH_SEGMENTS = 10;
    // 激光拖尾起始半径（靠近离子弹本体）。
    private static final float TRAIL_START_RADIUS = 3.6F;
    // 激光拖尾末端半径（远离离子弹）。
    private static final float TRAIL_END_RADIUS = 0.5F;
    private static final float M_2PI = (float) (Math.PI * 2.0);

    public BulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(T pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        if(pEntity.tickCount<=10) {
            return;
        }
        fixRotation(pEntity);
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.yRotO, pEntity.getYRot()) - 90.0F));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())));

        pPoseStack.scale(0.05F, 0.05F, 0.05F);
        pPoseStack.translate(0F, 0F, 0F);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM);
        PoseStack.Pose pose = pPoseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        for (int i = 0; i < 4; i++) {
            longFace(matrix4f, matrix3f, vertexconsumer, pPackedLight);
            pPoseStack.translate(0F, 2F, -2F);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        pPoseStack.translate(-8F, 0F, -2F);
        for (int i = 0; i < 2; i++) {
            shortFace(matrix4f, matrix3f, vertexconsumer, pPackedLight);
            pPoseStack.translate(16F, 0F, 0F);
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
        // 绘制离子弹激光拖尾：方向始终沿速度方向反向（本地 -X 轴），并在远端做颜色变浅。
        //renderIonTrail(pPoseStack.last(), vertexconsumer, pPackedLight, pEntity.tickCount*pEntity.tickCount*10);

        pPoseStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);
    }

    // 生成与炮口激光层类似的体积拖尾，并按距离进行颜色/透明度衰减。
    private void renderIonTrail(PoseStack.Pose pose, VertexConsumer consumer, int packedLight, float length) {
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        for (int seg = 0; seg < TRAIL_SEGMENTS; seg++) {
            float angle1 = seg / (float) TRAIL_SEGMENTS * M_2PI;
            float angle2 = (seg + 1.0F) / (float) TRAIL_SEGMENTS * M_2PI;

            float cos1 = Mth.cos(angle1);
            float sin1 = Mth.sin(angle1);
            float cos2 = Mth.cos(angle2);
            float sin2 = Mth.sin(angle2);

            for (int i = 0; i < TRAIL_LENGTH_SEGMENTS; i++) {
                float t1 = i / (float) TRAIL_LENGTH_SEGMENTS;
                float t2 = (i + 1.0F) / (float) TRAIL_LENGTH_SEGMENTS;

                float x1 = -length * t1;
                float x2 = -length * t2;
                float radius1 = Mth.lerp(t1, TRAIL_START_RADIUS, TRAIL_END_RADIUS);
                float radius2 = Mth.lerp(t2, TRAIL_START_RADIUS, TRAIL_END_RADIUS);

                // 根据距离做颜色渐变：越远越浅，同时透明度逐步降低。
                int[] colorNear = trailColor(t1);
                int[] colorFar = trailColor(t2);

                vertex(matrix4f, matrix3f, consumer, x1, radius1 * cos1, radius1 * sin1, colorNear, packedLight);
                vertex(matrix4f, matrix3f, consumer, x1, radius1 * cos2, radius1 * sin2, colorNear, packedLight);
                vertex(matrix4f, matrix3f, consumer, x2, radius2 * cos2, radius2 * sin2, colorFar, packedLight);
                vertex(matrix4f, matrix3f, consumer, x2, radius2 * cos1, radius2 * sin1, colorFar, packedLight);
            }
        }
    }

    // 计算拖尾分段颜色：t 越大表示离子弹越远，颜色越接近高亮浅色。
    private int[] trailColor(float t) {
        int r = (int) Mth.lerp(t, 90.0F, 20.0F);
        int g = (int) Mth.lerp(t, 170.0F, 100.0F);
        int b = (int) Mth.lerp(t, 255.0F, 255.0F);
        int a = (int) Mth.lerp(t, 100.0F, 45.0F);
        return new int[]{r, g, b, a};
    }

    private void shortFace(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer vertexconsumer, int pPackedLight) {
        this.vertex(matrix4f, matrix3f, vertexconsumer, 0, -2, -2, 0.0F, 0.0F, 0, -1, 0, pPackedLight);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 0, -2, 2, 0.125F, 0.0F, 0, -1, 0, pPackedLight);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 0, 2, 2, 0.125F, 0.125F, 0, -1, 0, pPackedLight);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 0, 2, -2, 0.0F, 0.125F, 0, -1, 0, pPackedLight);
    }

    private void longFace(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer vertexconsumer, int pPackedLight) {
        this.vertex(matrix4f, matrix3f, vertexconsumer, -8, 0, -2, 0.0F, 0.0F, 0, -1, 0, pPackedLight);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 8, 0, -2, 0.375F, 0.0F, 0, -1, 0, pPackedLight);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 8, 0, 2, 0.375F, 0.125F, 0, -1, 0, pPackedLight);
        this.vertex(matrix4f, matrix3f, vertexconsumer, -8, 0, 2, 0.0F, 0.125F, 0, -1, 0, pPackedLight);
    }

    public void vertex(Matrix4f pMatrix, Matrix3f pNormal, VertexConsumer pConsumer, int pX, int pY, int pZ, float pU, float pV, int pNormalX, int pNormalZ, int pNormalY, int pPackedLight) {
        pConsumer.vertex(pMatrix, pX, pY, pZ).color(128, 192, 128, 192).uv(pU, pV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(pPackedLight).normal(pNormal, (float)pNormalX, (float)pNormalY, (float)pNormalZ).endVertex();
    }

    // 为拖尾提供浮点坐标顶点写入方法，避免整数顶点导致拖尾细节丢失。
    private void vertex(Matrix4f pMatrix, Matrix3f pNormal, VertexConsumer pConsumer,
                        float pX, float pY, float pZ, int[] rgba, int pPackedLight) {
        pConsumer.vertex(pMatrix, pX, pY, pZ)
                .color(rgba[0], rgba[1], rgba[2], rgba[3])
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(pPackedLight)
                .normal(pNormal, 1.0F, 0.0F, 0.0F)
                .endVertex();
    }

    private void fixRotation(T entity) {
        Vec3 vec = entity.getDeltaMovement();
        entity.setYRot((float) Math.atan2(vec.x, vec.z) * Mth.RAD_TO_DEG);
        entity.setXRot((float) Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z)) * Mth.RAD_TO_DEG);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractBulletEntity pEntity) {
        return LASER_TEXTURE;
    }
}
