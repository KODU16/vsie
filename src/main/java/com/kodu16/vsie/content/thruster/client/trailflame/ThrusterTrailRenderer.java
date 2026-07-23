package com.kodu16.vsie.content.thruster.client.trailflame;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
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

public final class ThrusterTrailRenderer {
    private static final int SEGMENTS = 6;
    private static final int SMOOTH_STEPS = 3;
    private static final int FULL_BRIGHT = 0xA000A0;
    private static final float M_2PI = (float) (Math.PI * 2);
    private static final float TRAIL_R = 0.35F;
    private static final float TRAIL_G = 0.55F;
    private static final float TRAIL_B = 0.75F;
    private static final int TAIL_CONE_VERTICES = 10;
    private static final int FRONT_FADE_VERTICES = 3;
    private static final int END_FADE_VERTICES = 10;
    private static final int DIRECTION_GUIDE_POINTS = 3;
    private static final RenderType TRAIL_RENDER_TYPE = translucentbeamrendertype.SOLID_TRANSLUCENT_BEAM;

    private ThrusterTrailRenderer() {
    }

    public static void render(PoseStack poseStack, AbstractThrusterBlockEntity thruster, MultiBufferSource bufferSource) {
        List<Vec3> worldPoints = thruster.getTrailVerticesSnapshot();
        if (!thruster.shouldRenderTrail() || worldPoints.isEmpty()) {
            return;
        }
        float alpha = thruster.getTrailAlpha();

        List<Vec3> trailPath = buildTrailPath(worldPoints, thruster.getTrailWorldOffset());
        List<Vec3> smoothedWorldPoints = smoothTrail(trailPath);
        if (smoothedWorldPoints.size() < 2) {
            return;
        }

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();
        Matrix4f inversePose = new Matrix4f(pose).invert();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        List<Vector3f> localPoints = toLocalPoints(smoothedWorldPoints, inversePose, camera);
        renderTube(bufferSource.getBuffer(TRAIL_RENDER_TYPE), pose, normal, localPoints, trailRadius(thruster), alpha, trailPath.size());
    }

    private static List<Vec3> buildTrailPath(List<Vec3> worldPoints, Vec3 trailOffset) {
        List<Vec3> path = new ArrayList<>(worldPoints);
        if (path.isEmpty() || trailOffset.lengthSqr() <= 1.0E-6D) {
            return path;
        }

        Vec3 latestWorldPoint = path.get(path.size() - 1);
        // Function: generated head points follow the live nozzle direction while older points stay at recorded world vertices.
        for (int i = 1; i <= DIRECTION_GUIDE_POINTS; i++) {
            double step = i / (double) DIRECTION_GUIDE_POINTS;
            path.add(latestWorldPoint.add(trailOffset.scale(step)));
        }
        return path;
    }

    private static List<Vec3> smoothTrail(List<Vec3> points) {
        List<Vec3> smoothed = new ArrayList<>();
        if (points.isEmpty()) {
            return smoothed;
        }

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p0 = points.get(Math.max(0, i - 1));
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);
            Vec3 p3 = points.get(Math.min(points.size() - 1, i + 2));
            for (int step = 0; step < SMOOTH_STEPS; step++) {
                float t = step / (float) SMOOTH_STEPS;
                smoothed.add(catmullRom(p0, p1, p2, p3, t));
            }
        }
        smoothed.add(points.get(points.size() - 1));
        return smoothed;
    }

    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double x = 0.5D * ((2.0D * p1.x) + (-p0.x + p2.x) * t
                + (2.0D * p0.x - 5.0D * p1.x + 4.0D * p2.x - p3.x) * t2
                + (-p0.x + 3.0D * p1.x - 3.0D * p2.x + p3.x) * t3);
        double y = 0.5D * ((2.0D * p1.y) + (-p0.y + p2.y) * t
                + (2.0D * p0.y - 5.0D * p1.y + 4.0D * p2.y - p3.y) * t2
                + (-p0.y + 3.0D * p1.y - 3.0D * p2.y + p3.y) * t3);
        double z = 0.5D * ((2.0D * p1.z) + (-p0.z + p2.z) * t
                + (2.0D * p0.z - 5.0D * p1.z + 4.0D * p2.z - p3.z) * t2
                + (-p0.z + 3.0D * p1.z - 3.0D * p2.z + p3.z) * t3);
        return new Vec3(x, y, z);
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

    private static void renderTube(VertexConsumer vc, Matrix4f pose, Matrix3f normal, List<Vector3f> points,
                                   float radius, float alpha, int sourcePointCount) {
        int lastIndex = points.size() - 1;
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
            float startRadius = radiusAt(i / (float) lastIndex, radius, sourcePointCount);
            float endRadius = radiusAt((i + 1) / (float) lastIndex, radius, sourcePointCount);
            float startAlpha = alphaAt(i / (float) lastIndex, alpha, sourcePointCount);
            float endAlpha = alphaAt((i + 1) / (float) lastIndex, alpha, sourcePointCount);

            for (int seg = 0; seg < SEGMENTS; seg++) {
                float angle1 = seg / (float) SEGMENTS * M_2PI;
                float angle2 = (seg + 1) / (float) SEGMENTS * M_2PI;
                Vector3f a = ringPoint(current, right, up, startRadius, angle1);
                Vector3f b = ringPoint(current, right, up, startRadius, angle2);
                Vector3f c = ringPoint(next, right, up, endRadius, angle2);
                Vector3f d = ringPoint(next, right, up, endRadius, angle1);
                vertex(vc, pose, normal, a, startAlpha);
                vertex(vc, pose, normal, b, startAlpha);
                vertex(vc, pose, normal, c, endAlpha);
                vertex(vc, pose, normal, d, endAlpha);
            }
        }
    }

    private static float radiusAt(float t, float radius, int sourcePointCount) {
        // Function: the far trail end tapers across source vertices with a curved side profile.
        float maxIndex = Math.max(1.0F, sourcePointCount - 1.0F);
        float sourceIndex = t * maxIndex;
        if (sourceIndex <= 0.0F) {
            return 0.0F;
        }
        if (sourceIndex < TAIL_CONE_VERTICES) {
            float blend = sourceIndex / TAIL_CONE_VERTICES;
            float curvedBlend = 1.0F - (float) Math.cos(blend * M_2PI * 0.25F);
            return radius * curvedBlend;
        }
        return radius;
    }

    private static float alphaAt(float t, float baseAlpha, int sourcePointCount) {
        // Function: both ends of the retained world trail fade to invisible regardless of throttle alpha.
        float maxIndex = Math.max(1.0F, sourcePointCount - 1.0F);
        float sourceIndex = t * maxIndex;
        float startFade = Math.min(1.0F, sourceIndex / FRONT_FADE_VERTICES);
        float endFade = Math.min(1.0F, (maxIndex - sourceIndex) / END_FADE_VERTICES);
        return baseAlpha * startFade * endFade;
    }

    private static Vector3f ringPoint(Vector3f center, Vector3f right, Vector3f up, float radius, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Vector3f(center)
                .add(new Vector3f(right).mul(cos * radius))
                .add(new Vector3f(up).mul(sin * radius));
    }

    private static float trailRadius(AbstractThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic_vector" -> 0.25F * 0.5F;
            case "basic" -> 0.5F * 0.5F;
            case "medium" -> 1.5F * 0.5F;
            case "large" -> 2.5F * 0.5F;
            default -> Math.max(0.0F, thruster.getflamewidth());
        };
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Vector3f point, float alpha) {
        vc.addVertex(pose, point.x(), point.y(), point.z())
                .setColor(TRAIL_R, TRAIL_G, TRAIL_B, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
    }
}
