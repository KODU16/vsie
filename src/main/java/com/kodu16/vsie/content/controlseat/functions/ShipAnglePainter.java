package com.kodu16.vsie.content.controlseat.functions;

import com.kodu16.vsie.content.controlseat.client.HUD.DrawShape;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import static com.kodu16.vsie.content.controlseat.functions.WorldMarkerPainter.mc;

public class ShipAnglePainter {

    public static void drawAngleLine(GuiGraphics gg, Vector3d shipFacing, int centerX, int baseY, int color) {
        double yawDeg = getDirectedAnglesToAxes(new Vec3(shipFacing.x, shipFacing.y, shipFacing.z))[0];

        yawDeg = yawDeg % 360;
        if (yawDeg < 0) yawDeg += 360;

        int ticksEachSide   = 8;
        double pxPer10Deg   = 5.0;
        double fracOffset   = (yawDeg % 10.0) * (pxPer10Deg / 10.0);

        double labelThreshold   = 2.5;
        double highlightRadius  = 5.0;
        int    cardinalColor    = 0xFF88DDFF;
        int    cardinalAlpha    = 255;
        int    cardinalLength   = 2;
        int    cardinalThickness = 2;

        int[] cardinalAngles = {0, 90, 180, 270};
        String[] cardinalLabels = {"N", "E", "S", "W"};

        for (int i = 0; i < 4; i++) {
            double targetAngle = cardinalAngles[i];
            double delta = ((targetAngle - yawDeg + 180 + 360) % 360) - 180;
            double screenOffset = delta * pxPer10Deg / 10.0;
            int xPos = centerX + (int) Math.round(screenOffset);

            int lineTop    = baseY - cardinalLength;
            int lineBottom = baseY + cardinalLength + 1;
            if (Math.abs(screenOffset) < centerX * 0.15) {
                DrawShape.drawThickLine(gg, xPos, lineTop, xPos, lineBottom, cardinalThickness, cardinalColor);
                String txt = cardinalLabels[i];
                drawCenteredText(gg, txt, xPos, baseY + 5, 0xFFCCFFFF);
            }
        }

        for (int i = -ticksEachSide; i <= ticksEachSide; i++) {
            double angle = yawDeg + i * 10.0;
            double normalized = ((angle % 360) + 360) % 360;

            if (isCardinal(normalized)) continue;

            double xOffset = i * pxPer10Deg - fracOffset;
            int xPos = centerX + (int) Math.round(xOffset);

            double deltaDeg = Math.abs(normalized - yawDeg);
            deltaDeg = Math.min(deltaDeg, 360 - deltaDeg);

            float t = (float) Math.max(0.0, Math.min(1.0, deltaDeg / highlightRadius));
            float smoothT = t * t;
            float strength = 1.0f - smoothT;

            int finalColor = lerpColor(color, cardinalColor, strength);

            int lineLength   = Math.round(1+strength);
            int thickness    = Math.round(1 + strength);
            int alpha        = Math.round(100 + strength * 155);
            finalColor = (finalColor & 0x00FFFFFF) | (alpha << 24);

            int lineTop    = baseY - lineLength;
            int lineBottom = baseY + lineLength + 1;
            DrawShape.drawThickLine(gg, xPos, lineTop, xPos, lineBottom, thickness, finalColor);
        }

    }

    private static boolean isCardinal(double angle) {
        double[] cards = {0, 90, 180, 270};
        for (double c : cards) {
            double d = Math.abs(angle - c);
            d = Math.min(d, 360 - d);
            if (d < 0.5) return true;
        }
        return false;
    }

    private static int lerpColor(int color1, int color2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >>  8) & 0xFF;
        int b1 = (color1      ) & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >>  8) & 0xFF;
        int b2 = (color2      ) & 0xFF;

        int a = (int) (a1 + t * (a2 - a1));
        int r = (int) (r1 + t * (r2 - r1));
        int g = (int) (g1 + t * (g2 - g1));
        int b = (int) (b1 + t * (b2 - b1));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void drawCenteredText(GuiGraphics gg, String text, int x, int y, int color) {
        int textX = x - Minecraft.getInstance().font.width(text) / 2;
        gg.drawString(Minecraft.getInstance().font, text, textX, y, color, false);
    }

    public static double getPitchDegrees(Vector3d shipForward, Vector3d shipUp) {
        if (shipForward == null || shipForward.lengthSquared() < 1e-8) return 0.0;

        Vector3d forward = new Vector3d(shipForward).normalize();


        // Function: pitch comes from the forward vertical component, so roll does not affect the pitch indicator.
        return Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, forward.y))));
    }

    public static void drawPitchLine(GuiGraphics gg, double pitchDeg, int barX, int centerY, int color) {
        pitchDeg = Math.max(-90.0, Math.min(90.0, pitchDeg));

        int ticksEachSide = 8;
        double pxPer10Deg = 5.0;
        double fracOffset = (pitchDeg % 10.0) * (pxPer10Deg / 10.0);

        int minorHalfLength = 4;
        int majorHalfLength = 7;
        int majorColor = 0xFF88DDFF;

        for (int i = -ticksEachSide; i <= ticksEachSide; i++) {
            double angle = pitchDeg + i * 10.0;
            if (angle < -90.0 || angle > 90.0) continue;

            int yPos = centerY + (int) Math.round(i * pxPer10Deg - fracOffset);
            DrawShape.drawThickLine(gg, barX - minorHalfLength, yPos, barX + minorHalfLength, yPos, 1, color);
        }

        int[] majorAngles = {-90, 0, 90};
        for (int majorAngle : majorAngles) {
            double delta = majorAngle - pitchDeg;
            int yPos = centerY + (int) Math.round(delta * pxPer10Deg / 10.0);

            if (Math.abs(yPos - centerY) > ticksEachSide * pxPer10Deg + 8) continue;

            DrawShape.drawThickLine(gg, barX - majorHalfLength, yPos, barX + majorHalfLength, yPos, 2, majorColor);
            drawCenteredText(gg, String.valueOf(majorAngle), barX - 16, yPos - 4, 0xFFCCFFFF);
        }
    }

    public static void drawPitchLineCompact(GuiGraphics gg, double pitchDeg, int barX, int centerY, int color) {
        pitchDeg = Math.max(-90.0, Math.min(90.0, pitchDeg));

        int ticksEachSide = 8;
        double pxPer10Deg = 2.5;
        double fracOffset = (pitchDeg % 10.0) * (pxPer10Deg / 10.0);

        int minorHalfLength = 2;
        int majorHalfLength = 4;
        int majorColor = 0xFF88DDFF;

        // Function: compact pitch tape keeps only the numeric pitch so the seat HUD stays uncluttered.
        drawCenteredTextScaled(gg, formatSignedAngle(Math.round(pitchDeg)), barX, centerY - 30, color, 0.55F);
        for (int i = -ticksEachSide; i <= ticksEachSide; i++) {
            double angle = pitchDeg + i * 10.0;
            if (angle < -90.0 || angle > 90.0) continue;

            int yPos = centerY + (int) Math.round(i * pxPer10Deg - fracOffset);
            DrawShape.drawThickLine(gg, barX - minorHalfLength, yPos, barX + minorHalfLength, yPos, 1, color);
        }

        int[] majorAngles = {-90, 0, 90};
        for (int majorAngle : majorAngles) {
            double delta = majorAngle - pitchDeg;
            int yPos = centerY + (int) Math.round(delta * pxPer10Deg / 10.0);
            if (Math.abs(yPos - centerY) > ticksEachSide * pxPer10Deg + 4) continue;

            DrawShape.drawThickLine(gg, barX - majorHalfLength, yPos, barX + majorHalfLength, yPos, 1, majorColor);
            drawCenteredTextScaled(gg, String.valueOf(majorAngle), barX - 12, yPos - 2, 0xFFCCFFFF, 0.5F);
        }
    }

    private static void drawCenteredTextScaled(GuiGraphics gg, String text, int x, int y, int color, float scale) {
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1.0F);
        float inv = 1.0F / scale;
        Minecraft minecraft = Minecraft.getInstance();
        int scaledTextX = (int) ((x - (minecraft.font.width(text) * scale) / 2.0f) * inv);
        gg.drawString(minecraft.font, text, scaledTextX, (int) (y * inv), color, false);
        gg.pose().popPose();
    }

    private static String formatSignedAngle(long angle) {
        long clamped = Math.max(-99L, Math.min(99L, angle));
        String digits = Math.abs(clamped) < 10 ? "0" + Math.abs(clamped) : Long.toString(Math.abs(clamped));
        return (clamped >= 0 ? "+" : "-") + digits;
    }

    public static double[] getDirectedAnglesToAxes(Vec3 vec) {
        if (vec.lengthSqr() < 1e-12) {
            return new double[]{0, 0, 0};
        }

        Vec3 u = vec.normalize();
        double x = u.x, y = u.y, z = u.z;

        // Function: HUD heading uses compass bearings: north is 0, east is 90, south is 180, west is 270.
        double angleToPosX = Math.toDegrees(Math.atan2(x, -z));
        if (angleToPosX < 0) angleToPosX += 360;

        double angleToPosZ = Math.toDegrees(Math.atan2(x, z));
        if (angleToPosZ < 0) angleToPosZ += 360;

        double horizontalLen = Math.sqrt(x * x + z * z);
        double angleToPosY = Math.toDegrees(Math.atan2(horizontalLen, y));
        if (angleToPosY < 0) angleToPosY += 360;

        return new double[]{angleToPosX, angleToPosY, angleToPosZ};
    }

    public static void drawRotatingItem(GuiGraphics gg, ItemStack stack, int centerX, int centerY, float angle) {
        PoseStack pose = gg.pose();

        pose.pushPose();
        pose.translate(centerX, centerY, 0);
        pose.scale(22.0f, 11.0f, 0.001f);
        Quaternionf cameraRot = mc.getEntityRenderDispatcher().cameraOrientation();
        pose.mulPose(cameraRot);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();
        ItemRenderer renderer = mc.getItemRenderer();
        renderer.renderStatic(
                stack,
                ItemDisplayContext.GUI,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                pose,
                buffers,
                mc.level,
                0
        );

        pose.popPose();
    }

    public static float getHorizonAngleDegrees(Vector3d shipForward, Vector3d shipUp) {
        if (shipForward == null || shipUp == null) return 0f;
        if (shipForward.lengthSquared() < 1e-8 || shipUp.lengthSquared() < 1e-8) return 0f;

        Vector3d forward = new Vector3d(shipForward).normalize();
        Vector3d up = new Vector3d(shipUp).normalize();
        Vector3d worldUp = new Vector3d(0, 1, 0);

        Vector3d projectedShipUp = new Vector3d(up).sub(new Vector3d(forward).mul(up.dot(forward)));
        Vector3d projectedWorldUp = new Vector3d(worldUp).sub(new Vector3d(forward).mul(worldUp.dot(forward)));

        if (projectedShipUp.lengthSquared() < 1e-8 || projectedWorldUp.lengthSquared() < 1e-8) return 0f;

        projectedShipUp.normalize();
        projectedWorldUp.normalize();

        double cos = projectedShipUp.dot(projectedWorldUp);
        cos = Math.max(-1.0, Math.min(1.0, cos));

        Vector3d cross = new Vector3d(projectedShipUp).cross(projectedWorldUp);
        double sin = forward.dot(cross);

        return (float) Math.toDegrees(Math.atan2(sin, cos));
    }

}
