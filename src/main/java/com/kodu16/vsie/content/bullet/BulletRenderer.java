package com.kodu16.vsie.content.bullet;

import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@SuppressWarnings({"removal"})
public class BulletRenderer<T extends AbstractBulletEntity> extends EntityRenderer<T> {

    public static final ResourceLocation LASER_TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/entity/bullet.png");
    private static final int TRAIL_SEGMENTS = 8;
    private static final int TRAIL_LENGTH_SEGMENTS = 10;
    private static final float TRAIL_START_RADIUS = 3.6F;
    private static final float TRAIL_END_RADIUS = 0.5F;
    private static final float M_2PI = (float) (Math.PI * 2.0);

    public BulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public boolean shouldRender(T bullet, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // Function: bullets must stay renderable even when they cross distant loaded chunks.
        return true;
    }

    @Override
    public void render(T pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        if (pEntity.tickCount <= pEntity.getRenderStartTick()) {
            return;
        }
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.yRotO, pEntity.getYRot()) - 90.0F));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())));

        pPoseStack.scale(
                pEntity.getRenderLength() / 16.0F,
                pEntity.getRenderWidth() / 4.0F,
                pEntity.getRenderWidth() / 4.0F
        );
        VertexConsumer vertexconsumer = pBuffer.getBuffer(translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM);
        PoseStack.Pose pose = pPoseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        int renderColor = pEntity.getRenderColor();

        for (int i = 0; i < 4; i++) {
            longFace(matrix4f, matrix3f, vertexconsumer, pPackedLight, renderColor);
            pPoseStack.translate(0F, 2F, -2F);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        pPoseStack.translate(-8F, 0F, -2F);
        for (int i = 0; i < 2; i++) {
            shortFace(matrix4f, matrix3f, vertexconsumer, pPackedLight, renderColor);
            pPoseStack.translate(16F, 0F, 0F);
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
        // renderIonTrail(pPoseStack.last(), vertexconsumer, pPackedLight, pEntity.tickCount * pEntity.tickCount * 10);

        pPoseStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);
    }

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

                int[] colorNear = trailColor(t1);
                int[] colorFar = trailColor(t2);

                vertex(matrix4f, matrix3f, consumer, x1, radius1 * cos1, radius1 * sin1, colorNear, packedLight);
                vertex(matrix4f, matrix3f, consumer, x1, radius1 * cos2, radius1 * sin2, colorNear, packedLight);
                vertex(matrix4f, matrix3f, consumer, x2, radius2 * cos2, radius2 * sin2, colorFar, packedLight);
                vertex(matrix4f, matrix3f, consumer, x2, radius2 * cos1, radius2 * sin1, colorFar, packedLight);
            }
        }
    }

    private int[] trailColor(float t) {
        int r = (int) Mth.lerp(t, 90.0F, 20.0F);
        int g = (int) Mth.lerp(t, 170.0F, 100.0F);
        int b = (int) Mth.lerp(t, 255.0F, 255.0F);
        int a = (int) Mth.lerp(t, 100.0F, 45.0F);
        return new int[]{r, g, b, a};
    }

    private void shortFace(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer vertexconsumer, int pPackedLight, int renderColor) {
        this.vertex(matrix4f, matrix3f, vertexconsumer, 0, -2, -2, 0.0F, 0.0F, 0, -1, 0, pPackedLight, renderColor);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 0, -2, 2, 0.125F, 0.0F, 0, -1, 0, pPackedLight, renderColor);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 0, 2, 2, 0.125F, 0.125F, 0, -1, 0, pPackedLight, renderColor);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 0, 2, -2, 0.0F, 0.125F, 0, -1, 0, pPackedLight, renderColor);
    }

    private void longFace(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer vertexconsumer, int pPackedLight, int renderColor) {
        this.vertex(matrix4f, matrix3f, vertexconsumer, -8, 0, -2, 0.0F, 0.0F, 0, -1, 0, pPackedLight, renderColor);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 8, 0, -2, 0.375F, 0.0F, 0, -1, 0, pPackedLight, renderColor);
        this.vertex(matrix4f, matrix3f, vertexconsumer, 8, 0, 2, 0.375F, 0.125F, 0, -1, 0, pPackedLight, renderColor);
        this.vertex(matrix4f, matrix3f, vertexconsumer, -8, 0, 2, 0.0F, 0.125F, 0, -1, 0, pPackedLight, renderColor);
    }

    public void vertex(Matrix4f pMatrix, Matrix3f pNormal, VertexConsumer pConsumer, int pX, int pY, int pZ, float pU, float pV, int pNormalX, int pNormalZ, int pNormalY, int pPackedLight, int renderColor) {
        int alpha = renderColor >>> 24 & 0xFF;
        int red = renderColor >>> 16 & 0xFF;
        int green = renderColor >>> 8 & 0xFF;
        int blue = renderColor & 0xFF;
        pConsumer.addVertex(pMatrix, pX, pY, pZ).setColor(red, green, blue, alpha).setUv(pU, pV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(pPackedLight).setNormal((float) pNormalX, (float) pNormalY, (float) pNormalZ);
    }

    private void vertex(Matrix4f pMatrix, Matrix3f pNormal, VertexConsumer pConsumer, float pX, float pY, float pZ, int[] rgba, int pPackedLight) {
        pConsumer.addVertex(pMatrix, pX, pY, pZ)
                .setColor(rgba[0], rgba[1], rgba[2], rgba[3])
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pPackedLight)
                .setNormal(1.0F, 0.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T pEntity) {
        return LASER_TEXTURE;
    }
}
