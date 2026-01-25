package com.kodu16.vsie.content.controlseat.client.HUD;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector4f;
import com.mojang.datafixers.util.Pair;
import org.valkyrienskies.core.impl.shadow.lp;

public class ItemProjectile {
    public static Vector2i worldToScreen(Vec3 worldPos, PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return null;
        }

        // 获取相机位置（眼睛位置）
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        // 世界坐标 → 相对相机的向量
        Vector4f pos = new Vector4f(
                (float)(worldPos.x - camPos.x),
                (float)(worldPos.y - camPos.y),
                (float)(worldPos.z - camPos.z),
                1.0f
        );

        // 获取当前的视图 + 投影矩阵（已经包含了玩家的朝向、FOV、aspect 等）
        Matrix4f projection = mc.gameRenderer.getProjectionMatrix(mc.options.fov().get());
        Matrix4f view = poseStack.last().pose();           // 或 mc.gameRenderer.getMainCamera().getPositionAndRotation() 相关矩阵
        // 注意：1.20+ 很多时候直接用 RenderSystem.getProjectionMatrix() 和 getModelViewMatrix()

        // 视图变换
        pos.mul(view);
        // 投影变换
        pos.mul(projection);

        if (pos.w() <= 0.0f) {
            return null; // 在相机后面或裁剪掉了
        }

        // NDC → 视口空间
        float ndcX = pos.x() / pos.w();
        float ndcY = pos.y() / pos.w();

        // 映射到屏幕像素（注意 Y 轴翻转）
        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float screenX = (ndcX * 0.5f + 0.5f) * screenWidth;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * screenHeight;

        // 如果你想要从右往左排（你原代码有 SCREEN_WIDTH - x）
        int finalX = screenWidth - Math.round(screenX);
        int finalY = Math.round(screenY);

        return new Vector2i(finalX, finalY);
    }
}
