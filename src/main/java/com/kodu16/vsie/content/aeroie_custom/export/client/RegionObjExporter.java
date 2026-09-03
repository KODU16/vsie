package com.kodu16.vsie.content.aeroie_custom.export.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceStorage;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceAssetPackage;
import com.kodu16.vsie.content.aeroie_custom.export.RegionExportSelection;
import com.kodu16.vsie.content.aeroie_custom.export.RegionObjExportContract;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.NativeImage;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.io.IOException;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;

/** Converts world-space baked block quads into one OBJ, one MTL, and one padded texture atlas. */
public final class RegionObjExporter {
    public static final long MAX_BLOCKS = 1_000_000L;
    private static final int MAX_QUADS = 2_000_000;
    private static final int PADDING = 4;
    private static final int MAX_ATLAS_SIZE = 8192;
    private static final Logger LOGGER = LogUtils.getLogger();

    private RegionObjExporter() {
    }

    public static ExportResult export(ClientLevel level, RegionExportSelection selection,
                                      String folderPath, String requestedName) throws IOException {
        if (!selection.isIn(level)) {
            throw new IOException("The saved selection belongs to another dimension");
        }
        if (selection.volume() > MAX_BLOCKS) {
            throw new IOException("Selection exceeds " + MAX_BLOCKS + " blocks");
        }
        SubLevel firstSubLevel = Sable.HELPER.getContaining(level, selection.first());
        SubLevel secondSubLevel = Sable.HELPER.getContaining(level, selection.second());
        if (firstSubLevel != null || secondSubLevel != null) {
            throw new IOException("Sable sublevel selections are not supported");
        }
        String name = normalizeName(requestedName);
        String normalizedFolder = CustomDeviceStorage.normalizeRelativePath(folderPath);
        CapturedRegion captured = capture(level, selection);
        if (captured.quads.isEmpty()) {
            throw new IOException("No baked block faces were found in the selection");
        }
        Atlas atlas = packAtlas(captured.sprites);
        CustomDeviceAssetPackage assetPackage = CustomDeviceAssetPackage.named(name);
        Path componentsRoot = CustomDeviceStorage.componentsRoot();
        Path folderRoot = componentsRoot.resolve(normalizedFolder).normalize();
        if (!folderRoot.startsWith(componentsRoot)) {
            throw new IOException("Export folder escapes the AeroIE Components root");
        }
        Path packageDirectory = folderRoot.resolve(assetPackage.name()).normalize();
        Files.createDirectories(packageDirectory);
        Path obj = folderRoot.resolve(assetPackage.modelPath()).normalize();
        Path mtl = folderRoot.resolve(assetPackage.materialPath()).normalize();
        Path texture = folderRoot.resolve(assetPackage.texturePath()).normalize();
        writeFilesAtomically(name, captured, atlas, obj, mtl, texture);
        LOGGER.info("[AEROIE-OBJ-DIAG] phase=EXPORT_WRITTEN name={} obj={} mtl={} texture={} blocks={} quads={} sprites={} atlas={} dynamicSkipped={} unloadedSkipped={} faceCull={} bakedByDirection={} generalQuads={} degenerateQuads={} uvClamped={} uvExpanded={} spriteRemapped={} spriteByDirection={}",
                name, obj, mtl, texture, captured.nonAirBlocks, captured.quads.size(), captured.sprites.size(),
                atlas.size, captured.skippedDynamicBlocks, captured.skippedUnloadedBlocks,
                captured.stats.faceCullSummary(), captured.stats.bakedDirectionSummary(),
                captured.stats.generalQuads, captured.stats.degenerateQuads, captured.stats.uvClamped,
                captured.stats.uvExpanded, captured.stats.spriteRemapped, captured.stats.spriteByDirectionSummary());
        return new ExportResult(obj, mtl, texture, captured.nonAirBlocks, captured.quads.size(),
                captured.skippedDynamicBlocks, captured.skippedUnloadedBlocks);
    }

    private static CapturedRegion capture(ClientLevel level, RegionExportSelection selection) throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        BlockColors colors = minecraft.getBlockColors();
        List<TextureAtlasSprite> blockSprites = List.copyOf(minecraft.getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS).getTextures().values());
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        List<CapturedQuad> quads = new ArrayList<>();
        Map<AtlasKey, TextureAtlasSprite> sprites = new LinkedHashMap<>();
        RandomSource random = RandomSource.create();
        int nonAir = 0;
        int skippedDynamic = 0;
        int skippedUnloaded = 0;
        CaptureStats stats = new CaptureStats();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    cursor.set(x, y, z);
                    if (!level.hasChunkAt(cursor)) {
                        skippedUnloaded++;
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    nonAir++;
                    if (state.getRenderShape() != RenderShape.MODEL) {
                        skippedDynamic++;
                        continue;
                    }
                    stats.modelBlocks++;
                    BakedModel model = dispatcher.getBlockModel(state);
                    ModelData modelData = model.getModelData(level, cursor, state, level.getModelData(cursor));
                    Vec3 offset = state.getOffset(level, cursor);
                    long seed = state.getSeed(cursor);
                    for (Direction direction : Direction.values()) {
                        BlockPos neighbor = cursor.relative(direction);
                        if (shouldCullRegionFace(level, state, cursor, neighbor, min, max)) {
                            stats.faceCulled[direction.ordinal()]++;
                            continue;
                        }
                        random.setSeed(seed);
                        int before = quads.size();
                        appendQuads(quads, sprites, model.getQuads(state, direction, random, modelData, null),
                                colors, level, state, cursor, min, offset, isEmissive(state, level, cursor),
                                blockSprites, stats);
                        stats.directionQuads[direction.ordinal()] += quads.size() - before;
                        ensureQuadLimit(quads);
                    }
                    random.setSeed(seed);
                    int before = quads.size();
                    appendQuads(quads, sprites, model.getQuads(state, null, random, modelData, null),
                            colors, level, state, cursor, min, offset, isEmissive(state, level, cursor),
                            blockSprites, stats);
                    stats.generalQuads += quads.size() - before;
                    ensureQuadLimit(quads);
                }
            }
        }
        LOGGER.info("[AEROIE-OBJ-DIAG] phase=EXPORT_CAPTURE selectionMin={} selectionMax={} volume={} nonAir={} modelBlocks={} dynamicSkipped={} unloadedSkipped={} capturedQuads={} sprites={} faceCull={} bakedByDirection={} generalQuads={} degenerateQuads={} uvClamped={} uvExpanded={} spriteRemapped={} spriteByDirection={}",
                min.toShortString(), max.toShortString(), selection.volume(), nonAir, stats.modelBlocks,
                skippedDynamic, skippedUnloaded, quads.size(), sprites.size(), stats.faceCullSummary(),
                stats.bakedDirectionSummary(), stats.generalQuads, stats.degenerateQuads, stats.uvClamped,
                stats.uvExpanded, stats.spriteRemapped, stats.spriteByDirectionSummary());
        return new CapturedRegion(quads, sprites, nonAir, skippedDynamic, skippedUnloaded, stats);
    }

    private static boolean isEmissive(BlockState state, ClientLevel level, BlockPos pos) {
        return state.getLightEmission(level, pos) > 0;
    }

    private static boolean isInside(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    private static boolean shouldCullRegionFace(ClientLevel level, BlockState state, BlockPos pos,
                                                BlockPos neighbor, BlockPos min, BlockPos max) {
        if (!isInside(neighbor, min, max)) {
            return false;
        }
        BlockState neighborState = level.getBlockState(neighbor);
        if (neighborState.isAir() || neighborState.getRenderShape() != RenderShape.MODEL) {
            return false;
        }
        // Function: region OBJ export only removes faces between two fully opaque cube blocks.
        return state.isSolidRender(level, pos) && neighborState.isSolidRender(level, neighbor);
    }

    private static void ensureQuadLimit(List<CapturedQuad> quads) throws IOException {
        if (quads.size() > MAX_QUADS) {
            throw new IOException("Export exceeds " + MAX_QUADS + " visible model faces; reduce the selection");
        }
    }

    private static void appendQuads(List<CapturedQuad> output, Map<AtlasKey, TextureAtlasSprite> sprites,
                                    List<BakedQuad> input, BlockColors colors, ClientLevel level, BlockState state,
                                    BlockPos pos, BlockPos origin, Vec3 offset, boolean emissive,
                                    List<TextureAtlasSprite> blockSprites, CaptureStats stats) {
        for (BakedQuad quad : input) {
            int tint = quad.isTinted() ? colors.getColor(state, level, pos, quad.getTintIndex()) : 0xFFFFFF;
            if (tint == -1) {
                tint = 0xFFFFFF;
            }
            float[] xyz = new float[12];
            float[] uv = new float[8];
            float[] rawUv = new float[8];
            int[] data = quad.getVertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * IQuadTransformer.STRIDE;
                xyz[vertex * 3] = Float.intBitsToFloat(data[base + IQuadTransformer.POSITION])
                        + pos.getX() - origin.getX() + (float) offset.x;
                xyz[vertex * 3 + 1] = Float.intBitsToFloat(data[base + IQuadTransformer.POSITION + 1])
                        + pos.getY() - origin.getY() + (float) offset.y;
                xyz[vertex * 3 + 2] = Float.intBitsToFloat(data[base + IQuadTransformer.POSITION + 2])
                        + pos.getZ() - origin.getZ() + (float) offset.z;
                rawUv[vertex * 2] = Float.intBitsToFloat(data[base + IQuadTransformer.UV0]);
                rawUv[vertex * 2 + 1] = Float.intBitsToFloat(data[base + IQuadTransformer.UV0 + 1]);
            }
            TextureAtlasSprite sourceSprite = resolveSprite(quad.getSprite(), blockSprites, rawUv);
            if (sourceSprite != quad.getSprite()) {
                stats.spriteRemapped++;
            }
            AtlasKey key = new AtlasKey(sourceSprite.contents().name(), tint & 0xFFFFFF);
            sprites.putIfAbsent(key, sourceSprite);
            for (int vertex = 0; vertex < 4; vertex++) {
                float localU = sourceSprite.getUOffset(rawUv[vertex * 2]);
                float localV = sourceSprite.getVOffset(rawUv[vertex * 2 + 1]);
                float[] expanded = expandShrunkUv(localU, localV, sourceSprite, rawUv);
                if (expanded[0] != localU || expanded[1] != localV) {
                    stats.uvExpanded++;
                    localU = expanded[0];
                    localV = expanded[1];
                }
                float clampedU = Mth.clamp(localU, 0.0F, 1.0F);
                float clampedV = Mth.clamp(localV, 0.0F, 1.0F);
                if (clampedU != localU || clampedV != localV) {
                    stats.uvClamped++;
                }
                uv[vertex * 2] = clampedU;
                uv[vertex * 2 + 1] = clampedV;
            }
            if (isDegenerateQuad(xyz)) {
                stats.degenerateQuads++;
            }
            stats.recordSprite(quad.getDirection(), key.sprite);
            output.add(new CapturedQuad(xyz, uv, quad.getDirection(), key, emissive));
        }
    }

    private static TextureAtlasSprite resolveSprite(TextureAtlasSprite fallback, List<TextureAtlasSprite> blockSprites,
                                                    float[] rawUv) {
        float centerU = 0.0F;
        float centerV = 0.0F;
        for (int vertex = 0; vertex < 4; vertex++) {
            centerU += rawUv[vertex * 2];
            centerV += rawUv[vertex * 2 + 1];
        }
        centerU /= 4.0F;
        centerV /= 4.0F;
        if (containsUv(fallback, centerU, centerV)) {
            return fallback;
        }
        for (TextureAtlasSprite candidate : blockSprites) {
            if (containsUv(candidate, centerU, centerV)) {
                return candidate;
            }
        }
        return fallback;
    }

    private static boolean containsUv(TextureAtlasSprite sprite, float u, float v) {
        return u >= sprite.getU0() && u <= sprite.getU1()
                && v >= sprite.getV0() && v <= sprite.getV1();
    }

    private static float[] expandShrunkUv(float localU, float localV, TextureAtlasSprite sprite, float[] rawUv) {
        float shrink = sprite.uvShrinkRatio();
        if (shrink <= 0.0F || shrink >= 1.0F) {
            return new float[]{localU, localV};
        }
        float centerU = 0.0F;
        float centerV = 0.0F;
        for (int vertex = 0; vertex < 4; vertex++) {
            centerU += sprite.getUOffset(rawUv[vertex * 2]);
            centerV += sprite.getVOffset(rawUv[vertex * 2 + 1]);
        }
        centerU /= 4.0F;
        centerV /= 4.0F;
        // Function: FaceBakery slightly shrinks atlas UVs; standalone OBJ atlases need the unshrunk face span.
        return new float[]{
                centerU + (localU - centerU) / (1.0F - shrink),
                centerV + (localV - centerV) / (1.0F - shrink)
        };
    }

    private static boolean isDegenerateQuad(float[] xyz) {
        float ax = xyz[3] - xyz[0];
        float ay = xyz[4] - xyz[1];
        float az = xyz[5] - xyz[2];
        float bx = xyz[6] - xyz[0];
        float by = xyz[7] - xyz[1];
        float bz = xyz[8] - xyz[2];
        float cx = ay * bz - az * by;
        float cy = az * bx - ax * bz;
        float cz = ax * by - ay * bx;
        return cx * cx + cy * cy + cz * cz <= 1.0E-8F;
    }

    private static Atlas packAtlas(Map<AtlasKey, TextureAtlasSprite> sprites) throws IOException {
        List<AtlasEntry> entries = sprites.entrySet().stream()
                .map(entry -> new AtlasEntry(entry.getKey(), entry.getValue(), 0, 0))
                .sorted(Comparator.comparingInt((AtlasEntry entry) -> entry.sprite.contents().height()).reversed())
                .toList();
        int atlasSize = 256;
        List<AtlasEntry> placed = null;
        while (atlasSize <= MAX_ATLAS_SIZE && placed == null) {
            placed = tryPlace(entries, atlasSize);
            if (placed == null) {
                atlasSize *= 2;
            }
        }
        if (placed == null) {
            throw new IOException("Texture atlas exceeds " + MAX_ATLAS_SIZE + "x" + MAX_ATLAS_SIZE);
        }

        Map<AtlasKey, AtlasEntry> byKey = new LinkedHashMap<>();
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, atlasSize, atlasSize, true);
        for (AtlasEntry entry : placed) {
            byKey.put(entry.key, entry);
            copyWithPadding(image, entry);
        }
        return new Atlas(image, atlasSize, byKey);
    }

    private static List<AtlasEntry> tryPlace(List<AtlasEntry> entries, int size) {
        List<AtlasEntry> placed = new ArrayList<>(entries.size());
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (AtlasEntry entry : entries) {
            int slotWidth = entry.sprite.contents().width() + PADDING * 2;
            int slotHeight = entry.sprite.contents().height() + PADDING * 2;
            if (slotWidth > size || slotHeight > size) {
                return null;
            }
            if (x + slotWidth > size) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }
            if (y + slotHeight > size) {
                return null;
            }
            placed.add(new AtlasEntry(entry.key, entry.sprite, x + PADDING, y + PADDING));
            x += slotWidth;
            rowHeight = Math.max(rowHeight, slotHeight);
        }
        return placed;
    }

    private static void copyWithPadding(NativeImage target, AtlasEntry entry) {
        int width = entry.sprite.contents().width();
        int height = entry.sprite.contents().height();
        for (int dy = -PADDING; dy < height + PADDING; dy++) {
            for (int dx = -PADDING; dx < width + PADDING; dx++) {
                int sourceX = Mth.clamp(dx, 0, width - 1);
                int sourceY = Mth.clamp(dy, 0, height - 1);
                int pixel = tintAbgr(entry.sprite.getPixelRGBA(0, sourceX, sourceY), entry.key.tint);
                target.setPixelRGBA(entry.x + dx, entry.y + dy, pixel);
            }
        }
    }

    private static int tintAbgr(int abgr, int tint) {
        int alpha = abgr >>> 24;
        int red = (abgr & 0xFF) * ((tint >>> 16) & 0xFF) / 255;
        int green = ((abgr >>> 8) & 0xFF) * ((tint >>> 8) & 0xFF) / 255;
        int blue = ((abgr >>> 16) & 0xFF) * (tint & 0xFF) / 255;
        return alpha << 24 | blue << 16 | green << 8 | red;
    }

    private static void writeFilesAtomically(String name, CapturedRegion captured, Atlas atlas,
                                             Path obj, Path mtl, Path texture) throws IOException {
        Path root = obj.getParent();
        Path objTemp = Files.createTempFile(root, name + "_", ".obj.tmp");
        Path mtlTemp = Files.createTempFile(root, name + "_", ".mtl.tmp");
        Path textureTemp = Files.createTempFile(root, name + "_", ".png");
        try {
            writeObj(objTemp, name, captured, atlas);
            Files.writeString(mtlTemp, buildMtl(name), StandardCharsets.UTF_8);
            atlas.image.writeToFile(textureTemp);
            replace(objTemp, obj);
            replace(mtlTemp, mtl);
            replace(textureTemp, texture);
        } finally {
            atlas.image.close();
            Files.deleteIfExists(objTemp);
            Files.deleteIfExists(mtlTemp);
            Files.deleteIfExists(textureTemp);
        }
    }

    private static void writeObj(Path path, String name, CapturedRegion captured, Atlas atlas) throws IOException {
        // Function: stream large OBJ files so a valid but detailed selection cannot allocate a second full mesh-sized string.
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("# AeroIE world-region export\n");
            writer.write("# Origin is the minimum selected block corner; one block equals one unit.\n");
            writer.write("mtllib " + name + ".mtl\n");
            writer.write("o " + name + "\n");
            int normalIndex = 1;
            String currentMaterial = "";
            for (int quadIndex = 0; quadIndex < captured.quads.size(); quadIndex++) {
                CapturedQuad quad = captured.quads.get(quadIndex);
                String material = quad.emissive ? "atlas_emissive" : "atlas";
                if (!material.equals(currentMaterial)) {
                    writer.write("usemtl " + material + '\n');
                    currentMaterial = material;
                }
                int index = RegionObjExportContract.vertexIndexForQuad(quadIndex);
                AtlasEntry entry = atlas.entries.get(quad.atlasKey);
                for (int vertex = 0; vertex < 4; vertex++) {
                    writer.write(String.format(Locale.ROOT, "v %.6f %.6f %.6f\n",
                            quad.xyz[vertex * 3], quad.xyz[vertex * 3 + 1], quad.xyz[vertex * 3 + 2]));
                }
                for (int vertex = 0; vertex < 4; vertex++) {
                    float u = (entry.x + quad.uv[vertex * 2] * entry.sprite.contents().width()) / atlas.size;
                    float topV = (entry.y + quad.uv[vertex * 2 + 1] * entry.sprite.contents().height()) / atlas.size;
                    writer.write(String.format(Locale.ROOT, "vt %.7f %.7f\n", u, 1.0F - topV));
                }
                writer.write(String.format(Locale.ROOT, "vn %.1f %.1f %.1f\n",
                        (float) quad.direction.getStepX(), (float) quad.direction.getStepY(), (float) quad.direction.getStepZ()));
                writer.write("f " + index + '/' + index + '/' + normalIndex + ' '
                        + (index + 1) + '/' + (index + 1) + '/' + normalIndex + ' '
                        + (index + 2) + '/' + (index + 2) + '/' + normalIndex + ' '
                        + (index + 3) + '/' + (index + 3) + '/' + normalIndex + '\n');
                normalIndex++;
            }
        }
    }

    private static String buildMtl(String name) {
        return "# AeroIE texture atlas\nnewmtl atlas\nKa 1.000 1.000 1.000\nKd 1.000 1.000 1.000\n"
                + "Ks 0.000 0.000 0.000\nd 1.0\nillum 1\nmap_Kd " + name + ".png\n\n"
                + "newmtl atlas_emissive\n# aeroie_emissive true\nKa 1.000 1.000 1.000\n"
                + "Kd 1.000 1.000 1.000\nKe 1.000 1.000 1.000\n"
                + "Ks 0.000 0.000 0.000\nd 1.0\nillum 1\nmap_Kd " + name + ".png\n";
    }

    private static void replace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveUnsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String normalizeName(String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException("Export name must use 1-64 lowercase letters, digits, _ or -");
        }
        return normalized;
    }

    public record ExportResult(Path obj, Path mtl, Path texture, int blocks, int quads,
                               int skippedDynamicBlocks, int skippedUnloadedBlocks) {
    }

    private record AtlasKey(ResourceLocation sprite, int tint) {
    }

    private record CapturedQuad(float[] xyz, float[] uv, Direction direction, AtlasKey atlasKey, boolean emissive) {
    }

    private record CapturedRegion(List<CapturedQuad> quads, Map<AtlasKey, TextureAtlasSprite> sprites,
                                  int nonAirBlocks, int skippedDynamicBlocks, int skippedUnloadedBlocks,
                                  CaptureStats stats) {
    }

    private record AtlasEntry(AtlasKey key, TextureAtlasSprite sprite, int x, int y) {
    }

    private record Atlas(NativeImage image, int size, Map<AtlasKey, AtlasEntry> entries) {
    }

    private static final class CaptureStats {
        private final int[] faceCulled = new int[Direction.values().length];
        private final int[] directionQuads = new int[Direction.values().length];
        private int modelBlocks;
        private int generalQuads;
        private int degenerateQuads;
        private int uvClamped;
        private int uvExpanded;
        private int spriteRemapped;
        private final Map<Direction, Map<ResourceLocation, Integer>> spritesByDirection = new EnumMap<>(Direction.class);

        private String faceCullSummary() {
            return directionSummary(faceCulled);
        }

        private String bakedDirectionSummary() {
            return directionSummary(directionQuads);
        }

        private void recordSprite(Direction direction, ResourceLocation sprite) {
            spritesByDirection.computeIfAbsent(direction, ignored -> new LinkedHashMap<>())
                    .merge(sprite, 1, Integer::sum);
        }

        private String spriteByDirectionSummary() {
            StringBuilder builder = new StringBuilder();
            for (Direction direction : Direction.values()) {
                if (!builder.isEmpty()) {
                    builder.append(';');
                }
                builder.append(direction.getName()).append('=');
                Map<ResourceLocation, Integer> sprites = spritesByDirection.get(direction);
                if (sprites == null || sprites.isEmpty()) {
                    builder.append("none");
                    continue;
                }
                boolean first = true;
                for (Map.Entry<ResourceLocation, Integer> entry : sprites.entrySet()) {
                    if (!first) {
                        builder.append('|');
                    }
                    first = false;
                    builder.append(entry.getKey()).append(':').append(entry.getValue());
                }
            }
            return builder.toString();
        }

        private static String directionSummary(int[] values) {
            StringBuilder builder = new StringBuilder();
            for (Direction direction : Direction.values()) {
                if (!builder.isEmpty()) {
                    builder.append(',');
                }
                builder.append(direction.getName()).append('=').append(values[direction.ordinal()]);
            }
            return builder.toString();
        }
    }
}
