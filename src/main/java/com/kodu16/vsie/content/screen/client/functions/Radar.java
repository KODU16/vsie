package com.kodu16.vsie.content.screen.client.functions;

import com.kodu16.vsie.content.controlseat.functions.WorldMarkerPainter;
import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.Map;

public class Radar {
    private static final int RADAR_COLOR_NEUTRAL = 0xFF6699FF;
    private static final int RADAR_COLOR_ENEMY = 0xFFFF5555;
    private static final int RADAR_COLOR_ALLY = 0xFF55FF55;
    private static final int RADAR_COLOR_LOCKED_ENEMY = 0xFFFFFF55;
    private static final int RADAR_COLOR_SELF = 0xFF33FFAA;
    private static final double RADAR_RANGE_METERS = 512.0D;
    private static final float RADAR_SELF_MARKER_HALF_SIZE = 0.02f;
    private static final float RADAR_TARGET_MARKER_HALF_SIZE = 0.02f;
    private static final float RADAR_TEXT_SCALE = 0.005f;
    private static final float RADAR_LABEL_OFFSET_Y = 0.05f;
    private static final String UNNAMED_SUBLEVEL = "[Unnamed Sublevel]";

    // Function: draw a filled square marker on the screen plane.
    public static void drawSquare(PoseStack poseStack, MultiBufferSource bufferSource, float centerX, float centerY, float halfSize, int argb) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.gui());
        Matrix4f matrix = poseStack.last().pose();

        float minX = centerX - halfSize;
        float maxX = centerX + halfSize;
        float minY = centerY - halfSize;
        float maxY = centerY + halfSize;

        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        consumer.addVertex(matrix, minX, minY, 0).setColor(r, g, b, a);
        consumer.addVertex(matrix, minX, maxY, 0).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, maxY, 0).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, minY, 0).setColor(r, g, b, a);
    }

    // Function: render the linked control-seat radar snapshot directly from the screen block entity.
    public static void renderRadar(PoseStack poseStack, AbstractScreenBlockEntity screen, MultiBufferSource bufferSource, Font font) {
        drawSquare(poseStack, bufferSource, 0f, 0f, RADAR_SELF_MARKER_HALF_SIZE, RADAR_COLOR_SELF);

        Map<String, Object> shipsData = screen.getRadarShipsData();
        if (shipsData == null || shipsData.isEmpty()) {
            return;
        }

        Vector3d seatWorldPos = screen.getRadarControlSeatWorldPos();
        for (Map.Entry<String, Object> entry : shipsData.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> rawMap)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> shipData = (Map<String, Object>) rawMap;
            double shipX = toDouble(shipData.get("x"));
            double shipZ = toDouble(shipData.get("z"));
            double dx = shipX - seatWorldPos.x;
            double dz = shipZ - seatWorldPos.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDistance > RADAR_RANGE_METERS) {
                continue;
            }

            // Function: project the world-space XZ delta into the flat radar plane.
            float px = (float) (dx / RADAR_RANGE_METERS * 2.0);
            float py = (float) (dz / RADAR_RANGE_METERS * 2.0);
            String slug = stringValue(shipData.get("slug"));
            int radarColor = radarColor(screen, slug);

            drawSquare(poseStack, bufferSource, px, py, RADAR_TARGET_MARKER_HALF_SIZE, radarColor);
            renderShipLabel(poseStack, bufferSource, font, displayName(slug), px, py + RADAR_LABEL_OFFSET_Y, radarColor);
        }
    }

    // Function: keep radar colors consistent with the control-seat HUD target rules.
    private static int radarColor(AbstractScreenBlockEntity screen, String slug) {
        int priority = WorldMarkerPainter.getPriority(screen.getRadarEnemy(), screen.getRadarAlly(), slug);
        if (!slug.isEmpty() && slug.equals(screen.getRadarLockedEnemySlug()) && priority == 1) {
            return RADAR_COLOR_LOCKED_ENEMY;
        }
        if (priority == 1) {
            return RADAR_COLOR_ENEMY;
        }
        if (priority == 2) {
            return RADAR_COLOR_ALLY;
        }
        return RADAR_COLOR_NEUTRAL;
    }

    // Function: draw a compact label below each radar point using the same font scale as server-info mode.
    private static void renderShipLabel(PoseStack poseStack, MultiBufferSource bufferSource, Font font, String label, float centerX, float centerY, int color) {
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0.0f);
        poseStack.scale(RADAR_TEXT_SCALE, RADAR_TEXT_SCALE, RADAR_TEXT_SCALE);
        float drawX = -font.width(label) / 2f;
        font.drawInBatch(
                label,
                drawX,
                0f,
                color,
                false,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                0x00F000F0
        );
        poseStack.popPose();
    }

    private static String displayName(String slug) {
        return slug == null || slug.isBlank() ? UNNAMED_SUBLEVEL : slug;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }
}
