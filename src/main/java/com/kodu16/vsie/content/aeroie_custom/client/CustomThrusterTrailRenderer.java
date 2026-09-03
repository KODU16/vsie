package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomThrusterBlockEntity;
import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/** Renders custom thruster trail tubes from authored trailpoint history instead of the block center. */
final class CustomThrusterTrailRenderer {
    private static final int SEGMENTS = 6;
    private static final int FULL_BRIGHT = 0xA000A0;
    private static final float M_2PI = (float) (Math.PI * 2);
    private static final RenderType TRAIL_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;

    private CustomThrusterTrailRenderer() {
    }

    static void render(PoseStack poseStack, CustomThrusterBlockEntity thruster,
                       MultiBufferSource bufferSource, int trailpointIndex) {
        List<Vec3> worldPoints = thruster.getCustomTrailVerticesSnapshot(trailpointIndex);
        if (!thruster.shouldRenderTrail() || worldPoints.size() < 2) {
            return;
        }
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();
        Matrix4f inversePose = new Matrix4f(pose).invert();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        List<Vector3f> localPoints = toLocalPoints(worldPoints, inversePose, camera);
        int color = thruster.getCustomTrailColor();
        renderTube(bufferSource.getBuffer(TRAIL_RENDER_TYPE), pose, normal, localPoints,
                Math.max(0.0F, thruster.getCustomTrailRadius()), thruster.getTrailAlpha(), color);
    }

    private static List<Vector3f> toLocalPoints(List<Vec3> worldPoints, Matrix4f inversePose, Vec3 camera) {
        List<Vector3f> localPoints = new ArrayList<>(worldPoints.size());
        for (Vec3 worldPoint : worldPoints) {
            Vector4f renderSpace = new Vector4f(
                    (float) (worldPoint.x - camera.x),
                    (float) (worldPoint.y - camera.y),
                    (float) (worldPoint.z - camera.z),
                    1.0F
            );
            inversePose.transform(renderSpace);
            localPoints.add(new Vector3f(renderSpace.x(), renderSpace.y(), renderSpace.z()));
        }
        return localPoints;
    }

    private static void renderTube(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                   List<Vector3f> points, float radius, float alpha, int argb) {
        int lastIndex = points.size() - 1;
        float red = (argb >>> 16 & 255) / 255.0F;
        float green = (argb >>> 8 & 255) / 255.0F;
        float blue = (argb & 255) / 255.0F;
        float colorAlpha = (argb >>> 24 & 255) / 255.0F;
        for (int i = 0; i < lastIndex; i++) {
            Vector3f current = points.get(i);
            Vector3f next = points.get(i + 1);
            Vector3f axis = new Vector3f(next).sub(current);
            if (axis.lengthSquared() <= 1.0E-6F) {
                continue;
            }
            axis.normalize();
            Vector3f reference = Math.abs(axis.y()) < 0.95F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(1.0F, 0.0F, 0.0F);
            Vector3f right = new Vector3f(axis).cross(reference).normalize();
            Vector3f up = new Vector3f(right).cross(axis).normalize();
            float startAlpha = fade(i / (float) lastIndex) * alpha * colorAlpha;
            float endAlpha = fade((i + 1) / (float) lastIndex) * alpha * colorAlpha;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                float angleA = segment / (float) SEGMENTS * M_2PI;
                float angleB = (segment + 1) / (float) SEGMENTS * M_2PI;
                vertex(vc, pose, normal, ringPoint(current, right, up, radius, angleA), red, green, blue, startAlpha);
                vertex(vc, pose, normal, ringPoint(current, right, up, radius, angleB), red, green, blue, startAlpha);
                vertex(vc, pose, normal, ringPoint(next, right, up, radius, angleB), red, green, blue, endAlpha);
                vertex(vc, pose, normal, ringPoint(next, right, up, radius, angleA), red, green, blue, endAlpha);
            }
        }
    }

    private static float fade(float t) {
        return Math.max(0.0F, Math.min(1.0F, Math.min(t * 3.0F, (1.0F - t) * 3.0F)));
    }

    private static Vector3f ringPoint(Vector3f center, Vector3f right, Vector3f up, float radius, float angle) {
        return new Vector3f(center)
                .add(new Vector3f(right).mul((float) Math.cos(angle) * radius))
                .add(new Vector3f(up).mul((float) Math.sin(angle) * radius));
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Vector3f point,
                               float red, float green, float blue, float alpha) {
        vc.addVertex(pose, point.x(), point.y(), point.z())
                .setColor(red, green, blue, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
    }
}
