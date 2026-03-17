package com.kodu16.vsie.content.misc.electromagnet_rail.core;

import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ElectroMagnetRailCoreBeamLayer extends GeoRenderLayer<ElectroMagnetRailCoreBlockEntity> {
    // 功能：复用项目已有的半透明光束渲染管线，保持与推进器光束一致的视觉风格。
    private static final RenderType BEAM_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float BEAM_HALF_WIDTH = 0.12f;

    public ElectroMagnetRailCoreBeamLayer(GeoRenderer<ElectroMagnetRailCoreBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, ElectroMagnetRailCoreBlockEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer bufferSourceBuffer,
                       float partialTick, int packedLight, int packedOverlay) {
        // 功能：仅在绑定合法 top 且路径未阻挡时渲染双光束，其余状态（未绑定/方向错误/阻挡）一律不渲染。
        if (!animatable.hasValidTerminalBinding()) {
            return;
        }

        Direction facing = animatable.getBlockState().getValue(ElectroMagnetRailCoreBlock.FACING);
        float maxLength = (float) Math.sqrt(animatable.getBlockPos().distSqr(animatable.getTerminalPos()));
        float beamLength = Math.min(animatable.getBeamRenderDistance(), maxLength);
        if (beamLength <= 0.01f) {
            return;
        }

        poseStack.pushPose();
        // 功能：将渲染原点对齐到方块中心，便于按方块单位直接构造光束几何。
        poseStack.translate(0.5f, 0.5f, 0.5f);

        VertexConsumer consumer = bufferSource.getBuffer(BEAM_RENDER_TYPE);
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();

        float dirX = facing.getStepX();
        float dirY = facing.getStepY();
        float dirZ = facing.getStepZ();

        // 功能：计算朝向侧向单位向量，用于把两条平行光束整体平移到 core->top 方向侧边 3 格。
        float sideX;
        float sideY;
        float sideZ;
        if (facing.getAxis().isVertical()) {
            sideX = 1.0f;
            sideY = 0.0f;
            sideZ = 0.0f;
        } else {
            Direction sideDir = facing.getClockWise();
            sideX = sideDir.getStepX();
            sideY = sideDir.getStepY();
            sideZ = sideDir.getStepZ();
        }

        // 功能：构造与主方向垂直的第二个法线向量，用于生成光束截面。
        float upX = dirY * sideZ - dirZ * sideY;
        float upY = dirZ * sideX - dirX * sideZ;
        float upZ = dirX * sideY - dirY * sideX;

        drawSingleBeam(consumer, pose, normal, dirX, dirY, dirZ, sideX * 3.0f, sideY * 3.0f, sideZ * 3.0f, upX, upY, upZ, beamLength);
        drawSingleBeam(consumer, pose, normal, dirX, dirY, dirZ, -sideX * 3.0f, -sideY * 3.0f, -sideZ * 3.0f, upX, upY, upZ, beamLength);

        poseStack.popPose();
    }

    // 功能：绘制一条沿 core->top 方向延伸的方柱形光束，长度由每 tick 推进值控制。
    private void drawSingleBeam(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                float dirX, float dirY, float dirZ,
                                float offsetX, float offsetY, float offsetZ,
                                float upX, float upY, float upZ,
                                float length) {
        float sx = offsetX;
        float sy = offsetY;
        float sz = offsetZ;
        float ex = sx + dirX * length;
        float ey = sy + dirY * length;
        float ez = sz + dirZ * length;

        float sideX = (upY * dirZ - upZ * dirY);
        float sideY = (upZ * dirX - upX * dirZ);
        float sideZ = (upX * dirY - upY * dirX);

        float ux = upX * BEAM_HALF_WIDTH;
        float uy = upY * BEAM_HALF_WIDTH;
        float uz = upZ * BEAM_HALF_WIDTH;

        float vx = sideX * BEAM_HALF_WIDTH;
        float vy = sideY * BEAM_HALF_WIDTH;
        float vz = sideZ * BEAM_HALF_WIDTH;

        float r = 0.25f;
        float g = 0.75f;
        float b = 1.0f;

        // 功能：逐面输出方柱体 4 个侧面，尾部透明度更低以形成从 core 向外延展的视觉感。
        emitQuad(consumer, pose, normal,
                sx + ux + vx, sy + uy + vy, sz + uz + vz,
                sx + ux - vx, sy + uy - vy, sz + uz - vz,
                ex + ux - vx, ey + uy - vy, ez + uz - vz,
                ex + ux + vx, ey + uy + vy, ez + uz + vz,
                r, g, b, 0.85f, 0.15f);

        emitQuad(consumer, pose, normal,
                sx - ux - vx, sy - uy - vy, sz - uz - vz,
                sx - ux + vx, sy - uy + vy, sz - uz + vz,
                ex - ux + vx, ey - uy + vy, ez - uz + vz,
                ex - ux - vx, ey - uy - vy, ez - uz - vz,
                r, g, b, 0.85f, 0.15f);

        emitQuad(consumer, pose, normal,
                sx + ux - vx, sy + uy - vy, sz + uz - vz,
                sx - ux - vx, sy - uy - vy, sz - uz - vz,
                ex - ux - vx, ey - uy - vy, ez - uz - vz,
                ex + ux - vx, ey + uy - vy, ez + uz - vz,
                r, g, b, 0.85f, 0.15f);

        emitQuad(consumer, pose, normal,
                sx - ux + vx, sy - uy + vy, sz - uz + vz,
                sx + ux + vx, sy + uy + vy, sz + uz + vz,
                ex + ux + vx, ey + uy + vy, ez + uz + vz,
                ex - ux + vx, ey - uy + vy, ez - uz + vz,
                r, g, b, 0.85f, 0.15f);
    }

    // 功能：统一输出一个四边形面，并让 alpha 从起点到终点线性衰减。
    private void emitQuad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float x3, float y3, float z3,
                          float x4, float y4, float z4,
                          float r, float g, float b, float alphaStart, float alphaEnd) {
        vertex(vc, pose, normal, x1, y1, z1, r, g, b, alphaStart);
        vertex(vc, pose, normal, x2, y2, z2, r, g, b, alphaStart);
        vertex(vc, pose, normal, x3, y3, z3, r, g, b, alphaEnd);
        vertex(vc, pose, normal, x4, y4, z4, r, g, b, alphaEnd);
    }

    // 功能：输出全亮顶点，避免受场景光照影响导致光束忽明忽暗。
    private void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
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
