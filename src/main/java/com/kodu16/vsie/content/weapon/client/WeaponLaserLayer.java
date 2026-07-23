package com.kodu16.vsie.content.weapon.client;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
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

public class WeaponLaserLayer extends GeoRenderLayer<AbstractWeaponBlockEntity> {
    private static final String CANNON_BONE_NAME = "laser_locator";
    private static final int SEGMENTS = 4;
    private static final int LENGTH_SEGMENTS = 8;
    private static final float BASE_RADIUS = 0.25F;
    private static final float TIP_RADIUS = 0.25F;
    private static final float M_2PI = (float) (Math.PI * 2);
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final RenderType FLAME_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;

    private double laserLength = 0.0D;

    public WeaponLaserLayer(GeoRenderer<AbstractWeaponBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, AbstractWeaponBlockEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer bufferSourceBuffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if ("infra_knife_accelerator".equals(animatable.getweapontype())) {
            return;
        }
        laserLength = animatable.getRaycastDistance();
        if (laserLength < 0.1D) {
            return;
        }
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, bufferSourceBuffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public void renderForBone(PoseStack poseStack, AbstractWeaponBlockEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (!CANNON_BONE_NAME.equals(bone.getName())) {
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay);
            return;
        }

        poseStack.pushPose();
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(FLAME_RENDER_TYPE);
        float[][] layers = new float[LENGTH_SEGMENTS + 1][];

        for (int i = 0; i <= LENGTH_SEGMENTS; i++) {
            float t = i / (float) LENGTH_SEGMENTS;
            float z = (float) (-t * laserLength);
            float radius = BASE_RADIUS + (TIP_RADIUS - BASE_RADIUS) * t;
            float red = lerp(0.7F, 0.7F, t);
            float green = lerp(0.2F, 0.4F, t);
            float blue = lerp(0.2F, 0.4F, t);
            float alpha = lerp(0.5F, 0.5F, t);
            layers[i] = new float[]{z, radius, red, green, blue, alpha};
        }

        for (int segment = 0; segment < SEGMENTS; segment++) {
            float angle1 = segment / (float) SEGMENTS * M_2PI;
            float angle2 = (segment + 1.0F) / (float) SEGMENTS * M_2PI;
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            for (int i = 0; i < LENGTH_SEGMENTS; i++) {
                float[] current = layers[i];
                float[] next = layers[i + 1];
                vertex(vertexConsumer, pose, normal, current[1] * cos1, current[1] * sin1, current[0], current);
                vertex(vertexConsumer, pose, normal, current[1] * cos2, current[1] * sin2, current[0], current);
                vertex(vertexConsumer, pose, normal, next[1] * cos2, next[1] * sin2, next[0], next);
                vertex(vertexConsumer, pose, normal, next[1] * cos1, next[1] * sin1, next[0], next);
            }
        }

        poseStack.popPose();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void vertex(VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float[] layer) {
        vertexConsumer.addVertex(pose, x, y, z)
                .setColor(layer[2], layer[3], layer[4], layer[5])
                .setOverlay(0)
                .setLight(FULL_BRIGHT)
                .setNormal(0, 1, 0);
    }
}
