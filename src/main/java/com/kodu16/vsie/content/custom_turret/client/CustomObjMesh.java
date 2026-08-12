package com.kodu16.vsie.content.custom_turret.client;

import com.kodu16.vsie.content.custom_turret.WavefrontFaceContract;
import com.kodu16.vsie.content.custom_turret.WavefrontBoundsContract;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Minimal Wavefront mesh supporting positions, UVs, normals and quad-buffer-compatible polygon fans. */
final class CustomObjMesh {
    static final CustomObjMesh EMPTY = new CustomObjMesh(List.of(), null);
    private static final int MAX_SOURCE_LINES = 2_000_000;
    private final List<Vertex> triangles;
    private final Vector3f center;

    private CustomObjMesh(List<Vertex> triangles, Vector3f center) {
        this.triangles = triangles;
        this.center = center;
    }

    static CustomObjMesh load(Path path) throws IOException {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vertex> triangles = new ArrayList<>();
        int lineCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (++lineCount > MAX_SOURCE_LINES) {
                    throw new IOException("OBJ has too many lines");
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                switch (parts[0]) {
                    case "v" -> positions.add(parseVector3(parts));
                    case "vt" -> uvs.add(parseVector2(parts));
                    case "vn" -> normals.add(parseVector3(parts));
                    case "f" -> appendFace(parts, positions, uvs, normals, triangles);
                    default -> {
                        // Material libraries and groups are intentionally ignored; texture is assigned per bone.
                    }
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException exception) {
            throw new IOException("Malformed OBJ: " + path.getFileName(), exception);
        }
        return new CustomObjMesh(List.copyOf(triangles), calculateCenter(positions));
    }

    /** Returns the OBJ-local bounds center used by the editor's center-pivot action. */
    Vector3f center() {
        return center == null ? null : new Vector3f(center);
    }

    private static Vector3f calculateCenter(List<Vector3f> positions) {
        List<float[]> vertices = positions.stream()
                .map(position -> new float[]{position.x, position.y, position.z})
                .toList();
        float[] center = WavefrontBoundsContract.center(vertices);
        return center == null ? null : new Vector3f(center[0], center[1], center[2]);
    }

    void render(PoseStack poseStack, VertexConsumer consumer, int light) {
        PoseStack.Pose pose = poseStack.last();
        for (Vertex vertex : triangles) {
            consumer.addVertex(pose.pose(), vertex.position.x, vertex.position.y, vertex.position.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(vertex.uv.x, 1.0F - vertex.uv.y)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, vertex.normal.x, vertex.normal.y, vertex.normal.z);
        }
    }

    private static void appendFace(String[] parts, List<Vector3f> positions, List<Vector2f> uvs,
                                   List<Vector3f> normals, List<Vertex> triangles) throws IOException {
        if (parts.length < 4) {
            return;
        }
        List<Vertex> face = new ArrayList<>(parts.length - 1);
        for (int index = 1; index < parts.length; index++) {
            face.add(parseVertex(parts[index], positions, uvs, normals));
        }
        int[] fan = WavefrontFaceContract.quadCompatibleTriangleFan(face.size());
        for (int index = 0; index < fan.length; index += 4) {
            Vertex a = face.get(fan[index]);
            Vertex b = face.get(fan[index + 1]);
            Vertex c = face.get(fan[index + 2]);
            Vector3f faceNormal = new Vector3f(b.position).sub(a.position)
                    .cross(new Vector3f(c.position).sub(a.position));
            if (faceNormal.lengthSquared() > 1.0E-8F) {
                faceNormal.normalize();
            } else {
                faceNormal.set(0.0F, 1.0F, 0.0F);
            }
            triangles.add(a.withFallbackNormal(faceNormal));
            triangles.add(b.withFallbackNormal(faceNormal));
            triangles.add(c.withFallbackNormal(faceNormal));
            // Function: duplicate the final vertex so RenderType's QUADS mode cannot join adjacent OBJ triangles.
            triangles.add(c.withFallbackNormal(faceNormal));
        }
    }

    private static Vertex parseVertex(String token, List<Vector3f> positions, List<Vector2f> uvs,
                                      List<Vector3f> normals) throws IOException {
        String[] indices = token.split("/", -1);
        Vector3f position = positions.get(resolveIndex(indices[0], positions.size()));
        Vector2f uv = indices.length > 1 && !indices[1].isEmpty()
                ? uvs.get(resolveIndex(indices[1], uvs.size())) : new Vector2f();
        Vector3f normal = indices.length > 2 && !indices[2].isEmpty()
                ? normals.get(resolveIndex(indices[2], normals.size())) : null;
        return new Vertex(new Vector3f(position), new Vector2f(uv), normal == null ? null : new Vector3f(normal));
    }

    private static int resolveIndex(String value, int size) throws IOException {
        int parsed = Integer.parseInt(value);
        int resolved = parsed > 0 ? parsed - 1 : size + parsed;
        if (resolved < 0 || resolved >= size) {
            throw new IOException("OBJ index is out of range");
        }
        return resolved;
    }

    private static Vector3f parseVector3(String[] parts) {
        return new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
    }

    private static Vector2f parseVector2(String[] parts) {
        return new Vector2f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
    }

    private record Vertex(Vector3f position, Vector2f uv, Vector3f normal) {
        private Vertex withFallbackNormal(Vector3f fallback) {
            return normal == null ? new Vertex(position, uv, new Vector3f(fallback)) : this;
        }
    }
}
