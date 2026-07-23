package com.kodu16.vsie.content.thruster.client.trailflame;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ThrusterFlameRenderer implements BlockEntityRenderer<AbstractThrusterBlockEntity> {

    private static final int SEGMENTS = 6;
    private static final int LENGTH_SEGMENTS = 12;
    private static final float BASE_RADIUS = 0.85f;
    private static final float TIP_RADIUS = 0.5f;
    private static final RenderType FLAME_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;
    private static final int FULL_BRIGHT = 0xF000F0;

    public ThrusterFlameRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AbstractThrusterBlockEntity entity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light, int overlay) {

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);

        BlockState state = entity.getBlockState();
        Direction facing = state.getValue(BlockStateProperties.FACING);

        switch (facing) {
            case DOWN -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(180f));
            }
            case UP -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
            }
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90f));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90f));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90f));
        }

        float FLAME_LENGTH = entity.getRaycastDistance();

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();

        VertexConsumer vc = bufferSource.getBuffer(FLAME_RENDER_TYPE);

        float[][] layers = new float[LENGTH_SEGMENTS + 1][];

        for (int i = 0; i <= LENGTH_SEGMENTS; i++) {
            float t = i / (float) LENGTH_SEGMENTS;

            float z = -t * FLAME_LENGTH;
            float radius = BASE_RADIUS + (TIP_RADIUS - BASE_RADIUS) * t;

            float r = lerp(0.2f, 0.3f, t);
            float g = lerp(0.7f, 0.4f, t);
            float b = lerp(0.9f, 0.9f, t);
            float a = lerp(0.95f, 0.0f, t);

            layers[i] = new float[]{z, radius, r, g, b, a};
        }

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

    private static final float M_2PI = (float) (Math.PI * 2);

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z,
                               float r, float g, float b, float a) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setOverlay(0)
                .setLight(FULL_BRIGHT)
                .setNormal(0, 1, 0);
    }
}
