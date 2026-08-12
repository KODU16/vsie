package com.kodu16.vsie.content.custom_turret.export.client;

import com.kodu16.vsie.content.custom_turret.export.RegionExportSelection;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import dev.ryanhcode.sable.Sable;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Draws the saved corner or inclusive cuboid only while the export tool is held. */
@SuppressWarnings("removal")
@EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
public final class RegionExportSelectionRenderer {
    private static final Minecraft MC = Minecraft.getInstance();

    private RegionExportSelectionRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || MC.level == null || MC.player == null) {
            return;
        }
        ItemStack stack = heldTool(MC.player);
        if (!stack.is(vsieItems.REGION_OBJ_EXPORT_TOOL.get()) || RegionExportSelection.subLevelId(stack).isPresent()) {
            return;
        }
        Vec3 camera = MC.gameRenderer.getMainCamera().getPosition();
        BlockBounds bounds = RegionExportSelection.complete(stack)
                .filter(selection -> selection.isIn(MC.level))
                .map(selection -> new BlockBounds(selection.min(), selection.max()))
                .orElseGet(() -> RegionExportSelection.first(stack)
                        .map(pos -> new BlockBounds(pos, pos))
                        .orElse(null));
        if (bounds == null || RegionExportSelection.dimension(stack).filter(MC.level.dimension().location()::equals).isEmpty()) {
            return;
        }
        if (Sable.HELPER.getContaining(MC.level, bounds.min) != null
                || Sable.HELPER.getContaining(MC.level, bounds.max) != null) {
            return;
        }
        Vec3[] relativeCorners = worldCorners(bounds).clone();
        for (int index = 0; index < relativeCorners.length; index++) {
            relativeCorners[index] = relativeCorners[index].subtract(camera);
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            drawBox(event.getPoseStack(), relativeCorners);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static Vec3[] worldCorners(BlockBounds bounds) {
        double minX = bounds.min.getX();
        double minY = bounds.min.getY();
        double minZ = bounds.min.getZ();
        double maxX = bounds.max.getX() + 1.0D;
        double maxY = bounds.max.getY() + 1.0D;
        double maxZ = bounds.max.getZ() + 1.0D;
        Vec3[] corners = new Vec3[]{
                new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ),
                new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ),
                new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ),
                new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, maxZ)
        };
        return corners;
    }

    private static ItemStack heldTool(Player player) {
        return player.getMainHandItem().is(vsieItems.REGION_OBJ_EXPORT_TOOL.get())
                ? player.getMainHandItem()
                : player.getOffhandItem();
    }

    private static void drawBox(PoseStack pose, Vec3[] corners) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = pose.last().pose();
        edge(buffer, matrix, corners, 0, 1); edge(buffer, matrix, corners, 1, 2);
        edge(buffer, matrix, corners, 2, 3); edge(buffer, matrix, corners, 3, 0);
        edge(buffer, matrix, corners, 4, 5); edge(buffer, matrix, corners, 5, 6);
        edge(buffer, matrix, corners, 6, 7); edge(buffer, matrix, corners, 7, 4);
        edge(buffer, matrix, corners, 0, 4); edge(buffer, matrix, corners, 1, 5);
        edge(buffer, matrix, corners, 2, 6); edge(buffer, matrix, corners, 3, 7);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void edge(BufferBuilder buffer, Matrix4f matrix, Vec3[] corners, int from, int to) {
        Vec3 start = corners[from];
        Vec3 end = corners[to];
        line(buffer, matrix, start.x, start.y, start.z, end.x, end.y, end.z);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix,
                             double x0, double y0, double z0, double x1, double y1, double z1) {
        buffer.addVertex(matrix, (float) x0, (float) y0, (float) z0).setColor(0.53F, 0.81F, 0.98F, 1.0F);
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(0.53F, 0.81F, 0.98F, 1.0F);
    }

    private record BlockBounds(BlockPos min, BlockPos max) {
    }
}
