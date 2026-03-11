package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.math.Axis;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.renderer.GeoRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

// 你的 BlockEntity 类型
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;

public class TurretFrameLayer extends GeoRenderLayer<AbstractTurretBlockEntity> {

    public TurretFrameLayer(GeoRenderer<AbstractTurretBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }
    private static final int SEGMENTS = 4;
    private static final int LENGTH_SEGMENTS = 12;
    public double FLAME_LENGTH = 0f;
    private static final float BASE_RADIUS = 0.25f;
    private static final float TIP_RADIUS = 0.25f;
    private static final String cannonname = "cannon1";


    // 全亮光照（因为我们禁用了 lightmap）
    private static final int FULL_BRIGHT = 0xF000F0;
    @Override
    public void render(PoseStack poseStack, AbstractTurretBlockEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer bufferSourceBuffer,
                       float partialTick, int packedLight, int packedOverlay) {
        FLAME_LENGTH = animatable.getTargetdistance();
        if (FLAME_LENGTH < 0.1) {
            return;
        }
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, bufferSourceBuffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public void renderForBone(PoseStack poseStack, AbstractTurretBlockEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {

        if (!cannonname.equals(bone.getName())) {
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay);
            return;
        }
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
}