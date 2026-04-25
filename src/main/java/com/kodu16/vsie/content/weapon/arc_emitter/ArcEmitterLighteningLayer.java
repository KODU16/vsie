package com.kodu16.vsie.content.weapon.arc_emitter;

// 功能：适配 NeoForge 1.21.1 顶点提交流程，使用 addVertex/setColor 等新链式 API。

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

public class ArcEmitterLighteningLayer extends GeoRenderLayer<AbstractWeaponBlockEntity> {

    public ArcEmitterLighteningLayer(GeoRenderer<AbstractWeaponBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    // 直接使用我们自己定义的 RenderType
    private static final RenderType FLAME_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;


    @Override
    public void render(PoseStack poseStack, AbstractWeaponBlockEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer bufferSourceBuffer,
                       float partialTick, int packedLight, int packedOverlay) {
        Vec3 weaponPos = new Vec3(animatable.getWeaponPos().x,animatable.getWeaponPos().y,animatable.getWeaponPos().z);
        Vec3 targetPos = animatable.getTargetpos();

        LogUtils.getLogger().warn("rendering lightening:from"+weaponPos+"to:"+targetPos);
        if(animatable.getRaycastDistance() == 0) {return;}
        LogUtils.getLogger().warn("rendering lightening!");

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();
        VertexConsumer consumer = bufferSource.getBuffer(FLAME_RENDER_TYPE);

        // 创建锯齿状的闪电路径
        List<Vec3> points = generateLightningPath(weaponPos, targetPos, 15); // 15个分段

        // 渲染每段
        for (int i = 0; i < points.size() - 1; i++) {
            float t = i / (float) 15;

            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);

            Vec3 dir = p2.subtract(p1);
            Vec3 right = new Vec3(-dir.z, 0, dir.x).normalize().scale((float) 0.55 * 0.5); // 粗略的横向偏移

            Vec3 v1 = p1.add(right);
            Vec3 v2 = p1.subtract(right);
            Vec3 v3 = p2.add(right);
            Vec3 v4 = p2.subtract(right);

            float r = lerp(0.7f, 0.7f, t);
            float g = lerp(0.7f, 0.1f, t);
            float b = lerp(0.7f, 0.4f, t);
            float a = lerp(0.5f, 0.5f, t);

            // 画一个四边形（两面对三角形）
            consumer.addVertex(pose, (float)v1.x, (float)v1.y, (float)v1.z).setColor(r,g,b,a).setOverlay(0).setNormal(0, 1, 0).setLight(FULL_BRIGHT);
            consumer.addVertex(pose, (float)v2.x, (float)v2.y, (float)v2.z).setColor(r,g,b,a).setOverlay(0).setNormal(0, 1, 0).setLight(FULL_BRIGHT);
            consumer.addVertex(pose, (float)v4.x, (float)v4.y, (float)v4.z).setColor(r,g,b,a).setOverlay(0).setNormal(0, 1, 0).setLight(FULL_BRIGHT);

            consumer.addVertex(pose, (float)v1.x, (float)v1.y, (float)v1.z).setColor(r,g,b,a).setOverlay(0).setNormal(0, 1, 0).setLight(FULL_BRIGHT);
            consumer.addVertex(pose, (float)v4.x, (float)v4.y, (float)v4.z).setColor(r,g,b,a).setOverlay(0).setNormal(0, 1, 0).setLight(FULL_BRIGHT);
            consumer.addVertex(pose, (float)v3.x, (float)v3.y, (float)v3.z).setColor(r,g,b,a).setOverlay(0).setNormal(0, 1, 0).setLight(FULL_BRIGHT);
        }
        poseStack.popPose();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static List<Vec3> generateLightningPath(Vec3 start, Vec3 end, int segments) {
        List<Vec3> points = new ArrayList<>();
        points.add(start);

        Random random = new Random();
        Vec3 direction = end.subtract(start);

        for (int i = 1; i < segments; i++) {
            double t = (double) i / segments;
            Vec3 basePoint = start.add(direction.scale(t));

            // 添加随机偏移创造锯齿效果
            double offset = 0.3;
            Vec3 randomOffset = new Vec3(
                    (random.nextDouble() - 0.5) * offset,
                    (random.nextDouble() - 0.5) * offset,
                    (random.nextDouble() - 0.5) * offset
            );

            points.add(basePoint.add(randomOffset));
        }

        points.add(end);
        return points;
    }

}