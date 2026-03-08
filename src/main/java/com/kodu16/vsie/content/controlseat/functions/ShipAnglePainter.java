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
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import static com.kodu16.vsie.content.controlseat.functions.WorldMarkerPainter.mc;

public class ShipAnglePainter {

    public static void drawAngleLine(GuiGraphics gg, Vector3d shipFacing, int centerX, int baseY, int color) {
        double yawDeg = getDirectedAnglesToAxes(VectorConversionsMCKt.toMinecraft(shipFacing))[0];

        yawDeg = yawDeg % 360;
        if (yawDeg < 0) yawDeg += 360;

        // ───────────── 配置参数 ─────────────
        int ticksEachSide   = 8;
        double pxPer10Deg   = 5.0;
        double fracOffset   = (yawDeg % 10.0) * (pxPer10Deg / 10.0);

        double labelThreshold   = 2.5;     // 显示数字标签的阈值
        double highlightRadius  = 5.0;     // 渐变影响范围（度）
        int    cardinalColor    = 0xFF88DDFF;  // 主方向专用颜色（亮蓝）
        int    cardinalAlpha    = 255;
        int    cardinalLength   = 2;       // 主方向永远最长
        int    cardinalThickness = 2;

        // ─── 先单独绘制 4 个主方向（N/S/E/W） ───
        int[] cardinalAngles = {0, 90, 180, 270};
        String[] cardinalLabels = {"E", "S", "W", "N"};

        for (int i = 0; i < 4; i++) {
            double targetAngle = cardinalAngles[i];
            double delta = ((yawDeg - targetAngle + 180 + 360) % 360) - 180; // 最短环形距离
            double screenOffset = delta * pxPer10Deg / 10.0;
            int xPos = centerX + (int) Math.round(screenOffset);

            // 主方向永远用最大参数绘制
            int lineTop    = baseY - cardinalLength;
            int lineBottom = baseY + cardinalLength + 1;
            // 主方向标签永远显示（可选择只在屏幕内显示）
            if (Math.abs(screenOffset) < centerX * 0.2) {  // 避免太边缘
                DrawShape.drawThickLine(gg, xPos, lineTop, xPos, lineBottom, cardinalThickness, cardinalColor);
                String txt = "§l§b" + cardinalLabels[i];
                drawCenteredText(gg, txt, xPos, baseY + 5, 0xFFCCFFFF);
            }
        }

        // ─── 再绘制普通 10° 刻度（排除主方向） ───
        for (int i = -ticksEachSide; i <= ticksEachSide; i++) {
            double angle = yawDeg + i * 10.0;
            double normalized = ((angle % 360) + 360) % 360;

            // 如果是主方向角度，跳过（已在上方绘制）
            if (isCardinal(normalized)) continue;

            double xOffset = i * pxPer10Deg - fracOffset;
            int xPos = centerX + (int) Math.round(xOffset);

            // ─── 计算当前刻度 与 船头 的角度距离（环形） ───
            double deltaDeg = Math.abs(normalized - yawDeg);
            deltaDeg = Math.min(deltaDeg, 360 - deltaDeg);

            // ─── 渐变参数 ───
            float t = (float) Math.max(0.0, Math.min(1.0, deltaDeg / highlightRadius));
            float smoothT = t * t;           // 二次缓入
            float strength = 1.0f - smoothT; // 越靠近越强

            // 颜色渐变（从普通色 → 亮色）
            int finalColor = lerpColor(color, cardinalColor, strength);

            // 长度、粗细、透明度都跟随强度
            int lineLength   = Math.round(1+strength);
            int thickness    = Math.round(1 + strength);                // 1～2
            int alpha        = Math.round(100 + strength * 155); // 100～255
            finalColor = (finalColor & 0x00FFFFFF) | (alpha << 24);

            // 绘制普通刻度线
            int lineTop    = baseY - lineLength;
            int lineBottom = baseY + lineLength + 1;
            DrawShape.drawThickLine(gg, xPos, lineTop, xPos, lineBottom, thickness, finalColor);
        }

        // 中心红色箭头（船头指示）
        //DrawShape.drawThickLine(gg, centerX, baseY - 12, centerX, baseY - 3, 3, 0xFFFF5555);
        //gg.fill(centerX - 3, baseY - 5, centerX + 3, baseY - 3, 0x88FF5555);
    }

    // 辅助判断是否为主方向（允许小误差）
    private static boolean isCardinal(double angle) {
        double[] cards = {0, 90, 180, 270};
        for (double c : cards) {
            double d = Math.abs(angle - c);
            d = Math.min(d, 360 - d);
            if (d < 0.5) return true;
        }
        return false;
    }

    /**
     * 线性插值两种颜色
     */
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
        gg.drawCenteredString(Minecraft.getInstance().font, Component.literal(text), x, y, color);
    }

    /**
     * 计算向量与三个正轴的夹角，范围 0°～360°
     */
    public static double[] getDirectedAnglesToAxes(Vec3 vec) {
        if (vec.lengthSqr() < 1e-12) {
            return new double[]{0, 0, 0};
        }

        Vec3 u = vec.normalize();
        double x = u.x, y = u.y, z = u.z;

        // 1. 与 +X 轴夹角（yaw）
        double angleToPosX = Math.toDegrees(Math.atan2(z, x));
        if (angleToPosX < 0) angleToPosX += 360;

        // 2. 与 +Z 轴夹角
        double angleToPosZ = Math.toDegrees(Math.atan2(x, z));
        if (angleToPosZ < 0) angleToPosZ += 360;

        // 3. 与 +Y 轴夹角
        double horizontalLen = Math.sqrt(x * x + z * z);
        double angleToPosY = Math.toDegrees(Math.atan2(horizontalLen, y));
        if (angleToPosY < 0) angleToPosY += 360;

        return new double[]{angleToPosX, angleToPosY, angleToPosZ};
    }

    public static void drawRotatingItem(GuiGraphics gg, ItemStack stack, int centerX, int centerY, float angle) {
        PoseStack pose = gg.pose(); // 获取当前 PoseStack

        pose.pushPose(); // 保存状态
        pose.translate(centerX, centerY, 0); // 移动到中心
        pose.scale(22.0f, 11.0f, 0.001f);        // 放大一点（可选）
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

        pose.popPose(); // 恢复状态
    }

    /**
     * 计算水平仪的旋转角度（单位：度）。
     * 角度基于船体前向轴计算滚转角，让绘制出的水平线始终与地平线平行。
     */
    public static float getHorizonAngleDegrees(Vector3d shipForward, Vector3d shipUp) {
        if (shipForward == null || shipUp == null) return 0f;
        if (shipForward.lengthSquared() < 1e-8 || shipUp.lengthSquared() < 1e-8) return 0f;

        Vector3d forward = new Vector3d(shipForward).normalize();
        Vector3d up = new Vector3d(shipUp).normalize();
        Vector3d worldUp = new Vector3d(0, 1, 0);

        // 去掉前向分量，只保留“屏幕平面”上的方向
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
