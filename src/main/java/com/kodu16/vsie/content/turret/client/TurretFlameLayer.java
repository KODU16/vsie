package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

public class TurretFlameLayer extends GeoRenderLayer<AbstractTurretBlockEntity> {

    private static final String BONE = "cannonend";

    private static final RenderType FLASH_RENDER =
            translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;

    private static final int FULL_BRIGHT = 0xF000F0;

    private static final int RADIAL = 10;
    private static final int LENGTH = 5;

    private static final float TWO_PI = (float) (Math.PI * 2);

    public TurretFlameLayer(GeoRenderer<AbstractTurretBlockEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack,
                       AbstractTurretBlockEntity animatable,
                       BakedGeoModel model,
                       RenderType type,
                       MultiBufferSource bufferSource,
                       VertexConsumer buffer,
                       float partialTick,
                       int packedLight,
                       int packedOverlay) {

        super.render(poseStack, animatable, model, type, bufferSource, buffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public void renderForBone(PoseStack poseStack,
                              AbstractTurretBlockEntity turret,
                              GeoBone bone,
                              RenderType renderType,
                              MultiBufferSource bufferSource,
                              VertexConsumer buffer,
                              float partialTick,
                              int packedLight,
                              int packedOverlay) {

        if (!bone.getName().equals(BONE) || turret.muzzleFlashTicks<=1) {
            super.renderForBone(poseStack, turret, bone, renderType,
                    bufferSource, buffer, partialTick, packedLight, packedOverlay);
            return;
        }

        // ────────────────────────────────────────────────
        // 关键：把当前骨骼的完整变换应用上去
        // ────────────────────────────────────────────────
        poseStack.pushPose();

        // GeckoLib 提供的标准骨骼变换顺序（非常重要不要打乱）
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);

        VertexConsumer vc = bufferSource.getBuffer(FLASH_RENDER);

        float tick = turret.getLevel().getGameTime() + partialTick;
        float pulse = (float) Math.sin(tick * 0.8f) * 0.15f + 1f;

        // 现在所有坐标都是相对于 cannonend 骨骼的本地空间了
        renderCoreFlash(vc, poseStack.last().pose(), poseStack.last().normal(), pulse);
        renderFlameCone(vc, poseStack.last().pose(), poseStack.last().normal(), pulse);
        renderShockDisk(vc, poseStack.last().pose(), poseStack.last().normal());
        renderLightning(vc, poseStack.last().pose(), poseStack.last().normal(), turret.getBlockPos().asLong());

        poseStack.popPose();
    }


    private static void renderCoreFlash(VertexConsumer vc, Matrix4f pose, Matrix3f normal, float scale) {

        float r = 0.25f * scale;

        quad(vc, pose, normal,
                -r, -r, 0,
                r, -r, 0,
                r,  r, 0,
                -r,  r, 0,
                1f,0.9f,0.4f,1f);
    }

    private static void renderFlameCone(VertexConsumer vc, Matrix4f pose, Matrix3f normal, float scale) {

        float base = 0.35f * scale;
        float tip = 0.05f;

        for (int seg = 0; seg < RADIAL; seg++) {

            float a1 = seg / (float) RADIAL * TWO_PI;
            float a2 = (seg + 1f) / RADIAL * TWO_PI;

            float c1 = (float)Math.cos(a1);
            float s1 = (float)Math.sin(a1);

            float c2 = (float)Math.cos(a2);
            float s2 = (float)Math.sin(a2);

            for (int i = 0; i < LENGTH; i++) {

                float t0 = i / (float)LENGTH;
                float t1 = (i + 1f) / (float)LENGTH;

                float z0 = t0;
                float z1 = t1;

                float r0 = lerp(base, tip, t0);
                float r1 = lerp(base, tip, t1);

                float alpha0 = lerp(0.9f,0.2f,t0);
                float alpha1 = lerp(0.9f,0.1f,t1);

                vertex(vc,pose,normal,r0*c1,r0*s1,z0,1f,0.7f,0.2f,alpha0);
                vertex(vc,pose,normal,r0*c2,r0*s2,z0,1f,0.7f,0.2f,alpha0);
                vertex(vc,pose,normal,r1*c2,r1*s2,z1,1f,0.4f,0.1f,alpha1);
                vertex(vc,pose,normal,r1*c1,r1*s1,z1,1f,0.4f,0.1f,alpha1);
            }
        }
    }

    private static void renderShockDisk(VertexConsumer vc, Matrix4f pose, Matrix3f normal) {

        float r = 0.5f;

        for(int i=0;i<RADIAL;i++){

            float a1=i/(float)RADIAL*TWO_PI;
            float a2=(i+1f)/(float)RADIAL*TWO_PI;

            vertex(vc,pose,normal,0,0,0,1f,0.8f,0.3f,0.6f);
            vertex(vc,pose,normal,r*(float)Math.cos(a1),r*(float)Math.sin(a1),0,1f,0.5f,0.2f,0.2f);
            vertex(vc,pose,normal,r*(float)Math.cos(a2),r*(float)Math.sin(a2),0,1f,0.5f,0.2f,0.2f);
        }
    }

    private static void renderLightning(VertexConsumer vc, Matrix4f pose, Matrix3f normal, long seed){

        int branches = 4;

        for(int b=0;b<branches;b++){

            float angle=b/(float)branches*TWO_PI;

            float x0=0.18f*(float)Math.cos(angle);
            float y0=0.18f*(float)Math.sin(angle);

            float x1=0.05f*(float)Math.cos(angle*1.3f);
            float y1=0.05f*(float)Math.sin(angle*1.3f);

            addLightning(vc,pose,normal,x0,y0,0,x1,y1,0.9f);
        }
    }

    private static void addLightning(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                     float x0,float y0,float z0,
                                     float x1,float y1,float z1){

        float w=0.02f;

        vertex(vc,pose,normal,x0-w,y0+w,z0,0.6f,0.9f,1f,0.8f);
        vertex(vc,pose,normal,x0+w,y0-w,z0,0.6f,0.9f,1f,0.8f);
        vertex(vc,pose,normal,x1+w,y1-w,z1,0.8f,1f,1f,0.2f);
        vertex(vc,pose,normal,x1-w,y1+w,z1,0.8f,1f,1f,0.2f);
    }

    private static float lerp(float a,float b,float t){
        return a+(b-a)*t;
    }

    private static void quad(VertexConsumer vc,Matrix4f pose,Matrix3f normal,
                             float x1,float y1,float z1,
                             float x2,float y2,float z2,
                             float x3,float y3,float z3,
                             float x4,float y4,float z4,
                             float r,float g,float b,float a){

        vertex(vc,pose,normal,x1,y1,z1,r,g,b,a);
        vertex(vc,pose,normal,x2,y2,z2,r,g,b,a);
        vertex(vc,pose,normal,x3,y3,z3,r,g,b,a);
        vertex(vc,pose,normal,x4,y4,z4,r,g,b,a);
    }

    private static void vertex(VertexConsumer vc,Matrix4f pose,Matrix3f normal,
                               float x,float y,float z,
                               float r,float g,float b,float a){

        vc.vertex(pose,x,y,z)
                .color(r,g,b,a)
                .overlayCoords(0)
                .uv2(FULL_BRIGHT)
                .normal(normal,0,1,0)
                .endVertex();
    }
}