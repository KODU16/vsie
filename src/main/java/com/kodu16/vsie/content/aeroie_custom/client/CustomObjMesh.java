package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.WavefrontFaceContract;
import com.kodu16.vsie.content.aeroie_custom.WavefrontBoundsContract;
import com.kodu16.vsie.content.aeroie_custom.WavefrontMaterialContract;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Minimal Wavefront mesh supporting positions, UVs, normals and quad-buffer-compatible polygon fans. */
final class CustomObjMesh {
    static final CustomObjMesh EMPTY = new CustomObjMesh(Map.of(), null);
    private static final int MAX_SOURCE_LINES = 2_000_000;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, List<DrawVertex>> trianglesByMaterial;
    private final Vector3f center;

    private CustomObjMesh(Map<String, List<DrawVertex>> trianglesByMaterial, Vector3f center) {
        this.trianglesByMaterial = trianglesByMaterial;
        this.center = center;
    }

    static CustomObjMesh load(Path path) throws IOException {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        Map<String, List<DrawVertex>> trianglesByMaterial = new LinkedHashMap<>();
        Set<String> emissiveMaterials = new LinkedHashSet<>();
        String currentMaterial = "";
        int lineCount = 0;
        LoadStats stats = new LoadStats();

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
                    case "v" -> {
                        positions.add(parseVector3(parts));
                        stats.vertices++;
                    }
                    case "vt" -> {
                        uvs.add(parseVector2(parts));
                        stats.uvs++;
                    }
                    case "vn" -> {
                        normals.add(parseVector3(parts));
                        stats.normals++;
                    }
                    case "mtllib" -> {
                        // Function: merge emissive flags across every referenced MTL so later libraries cannot erase earlier ones.
                        emissiveMaterials.addAll(loadEmissiveMaterials(path, trimmed));
                        stats.emissiveMaterials = emissiveMaterials.size();
                    }
                    case "usemtl" -> {
                        String material = trimmed.substring("usemtl".length()).strip();
                        int comment = material.indexOf('#');
                        currentMaterial = (comment >= 0 ? material.substring(0, comment) : material).strip();
                    }
                    case "f" -> appendFace(parts, positions, uvs, normals, trianglesByMaterial, currentMaterial,
                            emissiveMaterials.contains(currentMaterial), stats);
                    default -> {
                        // Other material properties and groups are ignored; textures are resolved from MTL map_Kd links.
                    }
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException exception) {
            throw new IOException("Malformed OBJ: " + path.getFileName(), exception);
        }
        LOGGER.info("[AEROIE-OBJ-DIAG] phase=OBJ_LOAD path={} lines={} vertices={} uvs={} normals={} faces={} triangles={} quads={} ngons={} emittedQuads={} skippedDegenerate={} invalidFaces={} emissiveMaterials={} materials={} drawVertices={}",
                path, lineCount, stats.vertices, stats.uvs, stats.normals, stats.faces, stats.triangles,
                stats.quads, stats.ngons, stats.emittedQuads, stats.skippedDegenerate, stats.invalidFaces,
                stats.emissiveMaterials, trianglesByMaterial.size(), drawVertexCount(trianglesByMaterial));
        Map<String, List<DrawVertex>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<DrawVertex>> entry : trianglesByMaterial.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new CustomObjMesh(Collections.unmodifiableMap(immutable), calculateCenter(positions));
    }

    /** Returns the OBJ-local bounds center used by the editor's center-pivot action. */
    Vector3f center() {
        return center == null ? null : new Vector3f(center);
    }

    /** Returns the OBJ's materials in first-use order so the renderer can pick one texture per material. */
    List<String> materials() {
        return List.copyOf(trianglesByMaterial.keySet());
    }

    private static Vector3f calculateCenter(List<Vector3f> positions) {
        List<float[]> vertices = positions.stream()
                .map(position -> new float[]{position.x, position.y, position.z})
                .toList();
        float[] center = WavefrontBoundsContract.center(vertices);
        return center == null ? null : new Vector3f(center[0], center[1], center[2]);
    }

    void render(PoseStack poseStack, VertexConsumer consumer, int light) {
        for (List<DrawVertex> materialTriangles : trianglesByMaterial.values()) {
            renderVertices(poseStack, consumer, materialTriangles, light);
        }
    }

    void renderMaterial(PoseStack poseStack, VertexConsumer consumer, String material, int light) {
        List<DrawVertex> materialTriangles = trianglesByMaterial.get(material);
        if (materialTriangles != null && !materialTriangles.isEmpty()) {
            renderVertices(poseStack, consumer, materialTriangles, light);
        }
    }

    private static void renderVertices(PoseStack poseStack, VertexConsumer consumer,
                                       List<DrawVertex> materialTriangles, int light) {
        PoseStack.Pose pose = poseStack.last();
        for (DrawVertex drawVertex : materialTriangles) {
            Vertex vertex = drawVertex.vertex;
            consumer.addVertex(pose.pose(), vertex.position.x, vertex.position.y, vertex.position.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(vertex.uv.x, 1.0F - vertex.uv.y)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(drawVertex.emissive ? LightTexture.FULL_BRIGHT : light)
                    .setNormal(pose, vertex.normal.x, vertex.normal.y, vertex.normal.z);
        }
    }

    private static int drawVertexCount(Map<String, List<DrawVertex>> trianglesByMaterial) {
        int count = 0;
        for (List<DrawVertex> materialTriangles : trianglesByMaterial.values()) {
            count += materialTriangles.size();
        }
        return count;
    }

    private static void appendFace(String[] parts, List<Vector3f> positions, List<Vector2f> uvs,
                                   List<Vector3f> normals, Map<String, List<DrawVertex>> trianglesByMaterial,
                                   String material,
                                   boolean emissive, LoadStats stats) throws IOException {
        if (parts.length < 4) {
            stats.invalidFaces++;
            return;
        }
        stats.faces++;
        int vertexCount = parts.length - 1;
        if (vertexCount == 3) {
            stats.triangles++;
        } else if (vertexCount == 4) {
            stats.quads++;
        } else {
            stats.ngons++;
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
            Vertex d = face.get(fan[index + 3]);
            Vector3f faceNormal = new Vector3f(b.position).sub(a.position)
                    .cross(new Vector3f(c.position).sub(a.position));
            if (faceNormal.lengthSquared() <= 1.0E-8F) {
                // Function: skip zero-area face fragments instead of rendering arbitrary fallback planes.
                stats.skippedDegenerate++;
                continue;
            }
            faceNormal.normalize();
            List<DrawVertex> materialTriangles = trianglesByMaterial.computeIfAbsent(material,
                    ignored -> new ArrayList<>());
            materialTriangles.add(new DrawVertex(a.withFallbackNormal(faceNormal), emissive));
            materialTriangles.add(new DrawVertex(b.withFallbackNormal(faceNormal), emissive));
            materialTriangles.add(new DrawVertex(c.withFallbackNormal(faceNormal), emissive));
            materialTriangles.add(new DrawVertex(d.withFallbackNormal(faceNormal), emissive));
            stats.emittedQuads++;
        }
    }

    private static Set<String> loadEmissiveMaterials(Path objPath, String mtllibLine) throws IOException {
        String libraries = mtllibLine.substring("mtllib".length()).strip();
        int comment = libraries.indexOf('#');
        libraries = (comment >= 0 ? libraries.substring(0, comment) : libraries).strip();
        if (libraries.isEmpty() || objPath.getParent() == null) {
            return Set.of();
        }
        Set<String> materials = new LinkedHashSet<>();
        for (String library : libraries.split("\\s+")) {
            Path mtlPath = objPath.getParent().resolve(library).normalize();
            if (Files.isRegularFile(mtlPath)) {
                materials.addAll(WavefrontMaterialContract.emissiveMaterials(
                        Files.readString(mtlPath, StandardCharsets.UTF_8)));
            }
        }
        return materials;
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

    private record DrawVertex(Vertex vertex, boolean emissive) {
    }

    private static final class LoadStats {
        private int vertices;
        private int uvs;
        private int normals;
        private int faces;
        private int triangles;
        private int quads;
        private int ngons;
        private int emittedQuads;
        private int skippedDegenerate;
        private int invalidFaces;
        private int emissiveMaterials;
    }
}
