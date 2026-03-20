package com.kodu16.vsie.content.warpprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "vsie")
// 功能：负责绘制 warp projectile 消失后的六边形末地门平面残影，并保留实体本身的空渲染器职责。
public class WarpProjectileRenderer extends EntityRenderer<WarpProjecTileEntity> {
    // 功能：指定消失平面使用的末地门纹理，让残影视觉上与末地门效果一致。
    private static final ResourceLocation END_PORTAL_TEXTURE = new ResourceLocation("minecraft", "textures/block/end_portal.png");
    // 功能：改用明确绑定末地门贴图的半透明双面渲染类型，修复 RenderType.endPortal() 不显示自定义六边形顶点的问题。
    private static final RenderType DECAY_RENDER_TYPE = RenderType.entityTranslucentEmissive(END_PORTAL_TEXTURE, false);
    // 功能：统一约束消失残影持续时间，满足需求中的 80 tick 生命周期。
    private static final int DECAY_DURATION_TICKS = 80;
    // 功能：维护当前世界内所有活跃的跃迁弹消失残影，供世界渲染阶段统一绘制。
    private static final List<DecayPlane> ACTIVE_DECAY_PLANES = new ArrayList<>();

    public WarpProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    // 功能：在跃迁弹移除时登记一个残影平面，记录中心点、法线方向与六边形半径。
    public static void spawnDecayEffect(Vec3 center, Vec3 velocity, float radius) {
        if (radius <= 0.0F) {
            return;
        }
        Vec3 normal = velocity.lengthSqr() > 1.0E-7D ? velocity.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
        ACTIVE_DECAY_PLANES.add(new DecayPlane(center, normal, radius, net.minecraft.client.Minecraft.getInstance().level != null
                ? net.minecraft.client.Minecraft.getInstance().level.getGameTime() : 0L));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // 功能：在世界后处理阶段绘制残影，确保其与场景空间坐标对齐。
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || ACTIVE_DECAY_PLANES.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(DECAY_RENDER_TYPE);

        long gameTime = net.minecraft.client.Minecraft.getInstance().level != null
                ? net.minecraft.client.Minecraft.getInstance().level.getGameTime()
                : 0L;
        Iterator<DecayPlane> iterator = ACTIVE_DECAY_PLANES.iterator();
        while (iterator.hasNext()) {
            DecayPlane plane = iterator.next();
            int age = (int) (gameTime - plane.spawnGameTime);
            if (age > DECAY_DURATION_TICKS) {
                iterator.remove();
                continue;
            }

            poseStack.pushPose();
            // 功能：将平面移动到消失位置，并转换到相机相对坐标系中绘制。
            poseStack.translate(plane.center.x - cameraPos.x, plane.center.y - cameraPos.y, plane.center.z - cameraPos.z);
            orientPlaneToNormal(poseStack, plane.normal);
            renderHexPlane(poseStack, consumer, plane, 1.0F - age / (float) DECAY_DURATION_TICKS);
            poseStack.popPose();
        }

        bufferSource.endBatch(DECAY_RENDER_TYPE);
    }

    @Override
    public void render(WarpProjecTileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 功能：不绘制实体模型，只保留实体生命周期与特效驱动。
    }

    @Override
    public ResourceLocation getTextureLocation(WarpProjecTileEntity entity) {
        // 功能：空渲染器不使用贴图，返回末地门纹理对应资源便于保持资源引用一致。
        return END_PORTAL_TEXTURE;
    }

    // 功能：将局部 Z 轴旋转到速度法线方向，使六边形平面始终垂直于跃迁弹的飞行速度。
    private static void orientPlaneToNormal(PoseStack poseStack, Vec3 normal) {
        Vector3f from = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f to = new Vector3f((float) normal.x, (float) normal.y, (float) normal.z).normalize();
        float dot = Mth.clamp(from.dot(to), -1.0F, 1.0F);
        if (dot > 0.9999F) {
            return;
        }
        if (dot < -0.9999F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            return;
        }
        Vector3f axis = from.cross(to, new Vector3f()).normalize();
        poseStack.mulPose(new org.joml.Quaternionf().fromAxisAngleRad(axis, (float) Math.acos(dot)));
    }

    // 功能：绘制一个以 maxlife 为半径的六边形末地门平面，并随时间逐渐淡出。
    private static void renderHexPlane(PoseStack poseStack, VertexConsumer consumer, DecayPlane plane, float alphaScale) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        float radius = plane.radius;
        float innerAlpha = 0.85F * alphaScale;
        float outerAlpha = 0.35F * alphaScale;

        // 功能：先绘制朝向法线的正面六个三角面，形成完整的六边形末地门平面。
        for (int i = 0; i < 6; i++) {
            float angle0 = (float) (Math.PI / 3.0D * i);
            float angle1 = (float) (Math.PI / 3.0D * (i + 1));
            float x0 = Mth.cos(angle0) * radius;
            float y0 = Mth.sin(angle0) * radius;
            float x1 = Mth.cos(angle1) * radius;
            float y1 = Mth.sin(angle1) * radius;

            vertex(consumer, matrix4f, matrix3f, 0.0F, 0.0F, 0.0F, 0.5F, 0.5F, innerAlpha, 1.0F);
            vertex(consumer, matrix4f, matrix3f, x0, y0, 0.0F, 0.5F + x0 / (radius * 2.0F), 0.5F + y0 / (radius * 2.0F), outerAlpha, 1.0F);
            vertex(consumer, matrix4f, matrix3f, x1, y1, 0.0F, 0.5F + x1 / (radius * 2.0F), 0.5F + y1 / (radius * 2.0F), outerAlpha, 1.0F);
        }

        // 功能：再补绘背面六个三角面，保证玩家从跃迁终点另一侧观察时六边形纹理仍然可见。
        for (int i = 0; i < 6; i++) {
            float angle0 = (float) (Math.PI / 3.0D * i);
            float angle1 = (float) (Math.PI / 3.0D * (i + 1));
            float x0 = Mth.cos(angle0) * radius;
            float y0 = Mth.sin(angle0) * radius;
            float x1 = Mth.cos(angle1) * radius;
            float y1 = Mth.sin(angle1) * radius;

            vertex(consumer, matrix4f, matrix3f, 0.0F, 0.0F, 0.0F, 0.5F, 0.5F, innerAlpha, -1.0F);
            vertex(consumer, matrix4f, matrix3f, x1, y1, 0.0F, 0.5F + x1 / (radius * 2.0F), 0.5F + y1 / (radius * 2.0F), outerAlpha, -1.0F);
            vertex(consumer, matrix4f, matrix3f, x0, y0, 0.0F, 0.5F + x0 / (radius * 2.0F), 0.5F + y0 / (radius * 2.0F), outerAlpha, -1.0F);
        }
    }

    // 功能：向缓冲区写入一个带末地门纹理 UV、全亮度和渐隐透明度的平面顶点，可按需要切换正反面法线。
    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float u, float v, float alpha, float normalZ) {
        consumer.vertex(pose, x, y, z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, normalZ)
                .endVertex();
    }

    // 功能：封装单个消失残影平面的运行时数据，便于按 tick 管理位置、方向、半径与寿命。
    private static class DecayPlane {
        private final Vec3 center;
        private final Vec3 normal;
        private final float radius;
        private final long spawnGameTime;

        private DecayPlane(Vec3 center, Vec3 normal, float radius, long spawnGameTime) {
            this.center = center;
            this.normal = normal;
            this.radius = radius;
            this.spawnGameTime = spawnGameTime;
        }
    }
}
