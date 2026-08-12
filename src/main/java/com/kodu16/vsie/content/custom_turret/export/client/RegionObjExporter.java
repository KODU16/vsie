package com.kodu16.vsie.content.custom_turret.export.client;

import com.kodu16.vsie.content.custom_turret.CustomTurretStorage;
import com.kodu16.vsie.content.custom_turret.CustomTurretAssetPackage;
import com.kodu16.vsie.content.custom_turret.export.RegionExportSelection;
import com.kodu16.vsie.content.custom_turret.export.RegionObjExportContract;
import com.mojang.blaze3d.platform.NativeImage;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Converts world-space baked block quads into one OBJ, one MTL, and one padded texture atlas. */
public final class RegionObjExporter {
    public static final long MAX_BLOCKS = 1_000_000L;
    private static final int MAX_QUADS = 2_000_000;
    private static final int PADDING = 4;
    private static final int MAX_ATLAS_SIZE = 8192;

    private RegionObjExporter() {
    }

    public static ExportResult export(ClientLevel level, RegionExportSelection selection, String requestedName) throws IOException {
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
        CapturedRegion captured = capture(level, selection);
        if (captured.quads.isEmpty()) {
            throw new IOException("No baked block faces were found in the selection");
        }
        Atlas atlas = packAtlas(captured.sprites);
        CustomTurretAssetPackage assetPackage = CustomTurretAssetPackage.named(name);
        Path packageDirectory = CustomTurretStorage.resolveResource(assetPackage.name());
        Files.createDirectories(packageDirectory);
        Path obj = CustomTurretStorage.resolveResource(assetPackage.modelPath());
        Path mtl = CustomTurretStorage.resolveResource(assetPackage.materialPath());
        Path texture = CustomTurretStorage.resolveResource(assetPackage.texturePath());
        writeFilesAtomically(name, captured, atlas, obj, mtl, texture);
        return new ExportResult(obj, mtl, texture, captured.nonAirBlocks, captured.quads.size(),
                captured.skippedDynamicBlocks, captured.skippedUnloadedBlocks);
    }

    private static CapturedRegion capture(ClientLevel level, RegionExportSelection selection) throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        BlockColors colors = minecraft.getBlockColors();
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        List<CapturedQuad> quads = new ArrayList<>();
        Map<AtlasKey, TextureAtlasSprite> sprites = new LinkedHashMap<>();
        RandomSource random = RandomSource.create();
        int nonAir = 0;
        int skippedDynamic = 0;
        int skippedUnloaded = 0;

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
                    BakedModel model = dispatcher.getBlockModel(state);
                    ModelData modelData = model.getModelData(level, cursor, state, level.getModelData(cursor));
                    Vec3 offset = state.getOffset(level, cursor);
                    long seed = state.getSeed(cursor);
                    for (Direction direction : Direction.values()) {
                        BlockPos neighbor = cursor.relative(direction);
                        if (isInside(neighbor, min, max)
                                && !Block.shouldRenderFace(state, level, cursor, direction, neighbor)) {
                            continue;
                        }
                        random.setSeed(seed);
                        appendQuads(quads, sprites, model.getQuads(state, direction, random, modelData, null),
                                colors, level, state, cursor, min, offset);
                        ensureQuadLimit(quads);
                    }
                    random.setSeed(seed);
                    appendQuads(quads, sprites, model.getQuads(state, null, random, modelData, null),
                            colors, level, state, cursor, min, offset);
                    ensureQuadLimit(quads);
                }
            }
        }
        return new CapturedRegion(quads, sprites, nonAir, skippedDynamic, skippedUnloaded);
    }

    private static boolean isInside(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    private static void ensureQuadLimit(List<CapturedQuad> quads) throws IOException {
        if (quads.size() > MAX_QUADS) {
            throw new IOException("Export exceeds " + MAX_QUADS + " visible model faces; reduce the selection");
        }
    }

    private static void appendQuads(List<CapturedQuad> output, Map<AtlasKey, TextureAtlasSprite> sprites,
                                    List<BakedQuad> input, BlockColors colors, ClientLevel level, BlockState state,
                                    BlockPos pos, BlockPos origin, Vec3 offset) {
        for (BakedQuad quad : input) {
            int tint = quad.isTinted() ? colors.getColor(state, level, pos, quad.getTintIndex()) : 0xFFFFFF;
            if (tint == -1) {
                tint = 0xFFFFFF;
            }
            AtlasKey key = new AtlasKey(quad.getSprite().contents().name(), tint & 0xFFFFFF);
            sprites.putIfAbsent(key, quad.getSprite());
            float[] xyz = new float[12];
            float[] uv = new float[8];
            int[] data = quad.getVertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * IQuadTransformer.STRIDE;
                xyz[vertex * 3] = Float.intBitsToFloat(data[base + IQuadTransformer.POSITION])
                        + pos.getX() - origin.getX() + (float) offset.x;
                xyz[vertex * 3 + 1] = Float.intBitsToFloat(data[base + IQuadTransformer.POSITION + 1])
                        + pos.getY() - origin.getY() + (float) offset.y;
                xyz[vertex * 3 + 2] = Float.intBitsToFloat(data[base + IQuadTransformer.POSITION + 2])
                        + pos.getZ() - origin.getZ() + (float) offset.z;
                uv[vertex * 2] = quad.getSprite().getUOffset(Float.intBitsToFloat(data[base + IQuadTransformer.UV0]));
                uv[vertex * 2 + 1] = quad.getSprite().getVOffset(Float.intBitsToFloat(data[base + IQuadTransformer.UV0 + 1]));
            }
            output.add(new CapturedQuad(xyz, uv, quad.getDirection(), key));
        }
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
            writer.write("o " + name + "\nusemtl atlas\n");
            int normalIndex = 1;
            for (int quadIndex = 0; quadIndex < captured.quads.size(); quadIndex++) {
                CapturedQuad quad = captured.quads.get(quadIndex);
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

    private record CapturedQuad(float[] xyz, float[] uv, Direction direction, AtlasKey atlasKey) {
    }

    private record CapturedRegion(List<CapturedQuad> quads, Map<AtlasKey, TextureAtlasSprite> sprites,
                                  int nonAirBlocks, int skippedDynamicBlocks, int skippedUnloadedBlocks) {
    }

    private record AtlasEntry(AtlasKey key, TextureAtlasSprite sprite, int x, int y) {
    }

    private record Atlas(NativeImage image, int size, Map<AtlasKey, AtlasEntry> entries) {
    }
}
