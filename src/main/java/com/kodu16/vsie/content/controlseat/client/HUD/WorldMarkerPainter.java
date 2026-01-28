package com.kodu16.vsie.content.controlseat.client.HUD;

import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID)
public class WorldMarkerPainter {

    private static final Component MARKER = Component.literal("[+]").withStyle(ChatFormatting.RED);
    public static Map<String, Object> shipsData = new HashMap<>();
    static Minecraft mc = Minecraft.getInstance();
    public static Vec3 playerpos = null;
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        getRenderpos();
        if (shipsData.isEmpty() || playerpos == null) return;

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();

        for (var entry : shipsData.entrySet()) {
            @SuppressWarnings("unchecked")
            var attr = (Map<String, Object>) entry.getValue();

            double x = (double) attr.get("x");
            double y = (double) attr.get("y");
            double z = (double) attr.get("z");

            Vec3 target = new Vec3(x, y, z);
            Vec3 delta = target.subtract(playerpos);

            // 可选：太远就跳过，避免 z-fighting / 性能问题
            if (delta.lengthSqr() > 4096 * 4096) continue; // 约 64 格外

            //String name = ... // 你的名字逻辑
            double distance = Vec.Distance(new Vector3d(playerpos.x,playerpos.y,playerpos.z), new Vector3d(target.x,target.y,target.z));
            Component slug = Component.literal((String) attr.get("slug"))
                    .withStyle(ChatFormatting.AQUA);

            Component dist = Component.literal(String.format("%.1f m", distance))
                    .withStyle(ChatFormatting.WHITE);  // 或其他颜色

            Component text = slug.copy()
                    .append(" ")               // 这里 \n 有效！
                    .append(dist);
            render(playerpos, target, mc.getEntityRenderDispatcher(), mc.font, pose, buffers, text);
        }
    }
    private static void getRenderpos() {
        var level = mc.level;
        if (level == null) {
            return;
        }
        //LogUtils.getLogger().warn("finding camera entity");
        if (!(mc.getCameraEntity() instanceof Player player)) {
            shipsData = new HashMap<>();
            playerpos = null;
            return;
        }
        shipsData = ClientDataManager.getClientData(player).shipsData;
        playerpos = player.getEyePosition();
        //LogUtils.getLogger().warn("current player:"+player+"pos:"+playerpos);
    }

    private static void render(Vec3 camPos, Vec3 targetPos, EntityRenderDispatcher dispatcher,
                               Font font, PoseStack pose, MultiBufferSource buffer, Component text) {

        ItemRenderer itemRenderer = mc.getItemRenderer();

        double dist = camPos.distanceTo(targetPos);
        float baseScale = 0.1f;                 // 统一基础缩放（可调）
        float distanceScale = (float) (dist * 0.05); // 距离缩放因子
        float finalScale = baseScale * distanceScale;

        pose.pushPose();

        Vec3 lookVec = Vec3.directionFromRotation(mc.player.getXRot(), mc.player.getYRot()).normalize();
        Vec3 offset = targetPos.subtract(camPos);


        // 校正（危险，纯经验值）
        double adjustedCompensation = 0;
        // 根据水平偏移量的方向调整校正值
        if (offset.x > 0) {
            // 目标在视野右侧，应用负校正值
            adjustedCompensation = -dist * 0.05;
        } else if (offset.x < 0) {
            // 目标在视野左侧，应用正校正值
            adjustedCompensation = dist * 0.05;
        }
        Vec3 compensatedPos = targetPos.add(lookVec.scale(adjustedCompensation));
        offset = compensatedPos.subtract(camPos);

        //Vec3 offset = targetPos.subtract(camPos);
        pose.translate(offset.x, offset.y, offset.z);

        // 面向摄像机
        org.joml.Quaternionf cameraRot = dispatcher.cameraOrientation();
        pose.mulPose(cameraRot);
        //float yaw = (float) Math.toDegrees(Math.atan2(offset.x, offset.z)) + 90; // 或用玩家 yaw
        //pose.mulPose(Axis.YP.rotationDegrees(-yaw));

        // ─── 先渲染物品（放在中心） ───
        {
            pose.pushPose();

            pose.scale(finalScale*20, finalScale*20, finalScale * 0.001f);

            itemRenderer.renderStatic(
                    new ItemStack(vsieItems.TARGET_FRAME),
                    ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    pose,
                    buffer,
                    mc.level,
                    0
            );
            pose.popPose();
        }

        // ─── 再渲染文字（独立向上偏移） ───
        {
            pose.pushPose();

            // 文字整体向上移动（单位：物品渲染后的“高度”）
            float textYOffset = 1.4f*distanceScale;           // ← 这里是关键！调这个值

            pose.translate(0, textYOffset, 0);

            // 文字反向缩放（经典 billboard）
            pose.scale(-finalScale, -finalScale, -finalScale);

            float textWidth = font.width(text);
            float textHeight = font.lineHeight * text.getString().split("\n").length;

            // 文字居中
            font.drawInBatch(
                    text,
                    -textWidth / 2f,
                    -textHeight / 2f,           // 垂直居中（可改成 0 偏上）
                    0xFFFFFFFF,
                    false,
                    pose.last().pose(),
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    LightTexture.FULL_BRIGHT
            );

            pose.popPose();
        }

        // 恢复深度状态（如果你之前关了深度测试）
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        pose.popPose();
    }
}
