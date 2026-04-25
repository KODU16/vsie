package com.kodu16.vsie.content.controlseat.functions;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

// 功能：在 NeoForge 1.21.1 客户端 GAME 总线上订阅世界阶段渲染事件以绘制目标标记。
@EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
@SuppressWarnings("removal")
public class WorldMarkerPainter {
    //private static final Component MARKER = Component.literal("[+]").withStyle(ChatFormatting.RED);
    public static Map<String, Object> shipsData = new HashMap<>();
    public static String enemy = "";
    public static String ally = "";
    public static String lockedenemyslug = "";
    static Minecraft mc = Minecraft.getInstance();
    public static Vec3 playerpos = null;
    private static final int TEXT_ALPHA = 200;   // 主文字透明度

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        getRenderpos();
        if (shipsData.isEmpty() || playerpos == null) return;
        for (var entry : shipsData.entrySet()) {
            PoseStack pose = event.getPoseStack();
            MultiBufferSource buffers = mc.renderBuffers().bufferSource();
            @SuppressWarnings("unchecked")
            var attr = (Map<String, Object>) entry.getValue();
            double x = (double) attr.get("x");
            double y = (double) attr.get("y");
            double z = (double) attr.get("z");
            Vec3 target = new Vec3(x, y, z);
            Vec3 delta = target.subtract(playerpos);
            int targettype = getPriority(enemy,ally, (String) attr.get("slug"));

            ItemStack item = new ItemStack(vsieItems.TARGET_FRAME);
            Component slug = Component.literal((String) attr.get("slug"))
                    .withStyle(ChatFormatting.AQUA);

            // 可选：太远就跳过，避免 z-fighting / 性能问题
            if (delta.lengthSqr() > 4096 * 4096) continue; // 约 64 格外

            //String name = ... // 你的名字逻辑
            double distance = Vec.Distance(new Vector3d(playerpos.x,playerpos.y,playerpos.z), new Vector3d(target.x,target.y,target.z));
            if(targettype ==1)
            {
                item = new ItemStack(vsieItems.TARGET_FRAME_ENEMY);
                slug = Component.literal((String) attr.get("slug"))
                        .withStyle(ChatFormatting.RED);
            }
            if(targettype ==2)
            {
                item = new ItemStack(vsieItems.TARGET_FRAME_ALLY);
                slug = Component.literal((String) attr.get("slug"))
                        .withStyle(ChatFormatting.GREEN);
            }
            Component dist = Component.literal(String.format("%.1f m", distance))
                    .withStyle(ChatFormatting.WHITE);  // 或其他颜色

            Component text = slug.copy()
                    .append(" ")               // 这里 \n 有效！
                    .append(dist);
            rendertext(playerpos, target, mc.getEntityRenderDispatcher(), mc.font, pose, buffers, text, FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0xFF, 0xFF));
            if((attr.get("slug")).equals(lockedenemyslug)) {
                item = new ItemStack(vsieItems.TARGET_FRAME_ENEMY_LOCKED);
                rendertargettext(playerpos, target, mc.getEntityRenderDispatcher(), mc.font, pose, buffers, FastColor.ARGB32.color(TEXT_ALPHA, 0x00, 0xFF, 0xFF));
            }
            render(playerpos, target, mc.getEntityRenderDispatcher(), pose, buffers, item);
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
            enemy = "";
            ally = "";
            playerpos = null;
            return;
        }
        ControlSeatClientData data = ClientDataManager.getClientData(player);
        shipsData = data.shipsData;
        enemy = data.enemy;
        ally = data.ally;
        lockedenemyslug = data.lockedenemyslug;
        playerpos = player.getEyePosition();
    }

    private static void render(Vec3 camPos, Vec3 targetPos, EntityRenderDispatcher dispatcher,
                               PoseStack pose, MultiBufferSource buffer, ItemStack item) {
        ItemRenderer itemRenderer = mc.getItemRenderer();
        double dist = camPos.distanceTo(targetPos);
        float baseScale = 0.1f;                 // 统一基础缩放（可调）
        float distanceScale = (float) (dist * 0.05); // 距离缩放因子
        float finalScale = baseScale * distanceScale;
        Vec3 offset = targetPos.subtract(camPos);
        Quaternionf cameraRot = dispatcher.cameraOrientation();
        pose.pushPose();
        {
            pose.translate(offset.x, offset.y, offset.z);
            pose.mulPose(cameraRot);
            pose.scale(finalScale*20, finalScale*20, finalScale * 0.0001f);
            itemRenderer.renderStatic(
                    item,
                    ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    pose,
                    buffer,
                    mc.level,
                    0
            );
        }

        pose.popPose();
    }
    private static void rendertext(
            Vec3 camPos, Vec3 targetPos, EntityRenderDispatcher dispatcher,
            Font font, PoseStack pose, MultiBufferSource buffer,
            Component text, int color) {

        Quaternionf cameraRot = dispatcher.cameraOrientation();
        float scale = 0.003f * (float) camPos.distanceTo(targetPos);

        // ─── 6. 渲染文字（注意 y 轴偏移让文字居中或偏上） ───
        float textWidth = font.width(text);
        pose.pushPose();
        {
            pose.translate(targetPos.x - camPos.x, targetPos.y - camPos.y, targetPos.z - camPos.z);
            pose.mulPose(cameraRot);
            pose.scale(-scale, -scale, -scale);   // 注意 Y 轴要取反（Minecraft 文字坐标系 Y 向下）
            pose.translate(0,18,0);
            Matrix4f matrix4f = pose.last().pose();
            font.drawInBatch(
                    text,
                    -textWidth / 2f,          // x 居中
                    0,    // y 稍微偏上
                    color,
                    false,
                    matrix4f,
                    buffer,
                    Font.DisplayMode.NORMAL,   // 改成 NORMAL 才有光照/深度
                    0,
                    LightTexture.FULL_BRIGHT
            );
        }

        pose.popPose();
    }

    private static void rendertargettext(
            Vec3 camPos, Vec3 targetPos, EntityRenderDispatcher dispatcher,
            Font font, PoseStack pose, MultiBufferSource buffer, int color) {

        Quaternionf cameraRot = dispatcher.cameraOrientation();
        float scale = 0.005f * (float) camPos.distanceTo(targetPos);

        // ─── 6. 渲染文字（注意 y 轴偏移让文字居中或偏上） ───
        float textWidth = 3;
        pose.pushPose();
        {
            pose.translate(targetPos.x - camPos.x, targetPos.y - camPos.y, targetPos.z - camPos.z);
            pose.mulPose(cameraRot);
            pose.scale(-scale, -scale, -scale);   // 注意 Y 轴要取反（Minecraft 文字坐标系 Y 向下）
            Matrix4f matrix4f = pose.last().pose();
            Component tgt = Component.literal("TGT")
                    .withStyle(ChatFormatting.RED);  // 或其他颜色
            pose.translate(0,-18,0);
            font.drawInBatch(
                    tgt,
                    (-textWidth / 2f)-6.5f,          // x 居中
                    0,    // y 稍微偏上
                    color,
                    false,
                    matrix4f,
                    buffer,
                    Font.DisplayMode.NORMAL,   // 改成 NORMAL 才有光照/深度
                    0,
                    LightTexture.FULL_BRIGHT
            );
        }
        pose.popPose();
    }


    public static int getPriority(String a, String b, String c) {
        // 防御性编程：处理 null 和空字符串
        if (c == null || c.isEmpty() || a.isEmpty() || b.isEmpty()) {
            return 0;
        }

        // 查找第一次出现的位置（-1 表示没找到）
        int posA = c.indexOf(a);
        int posB = c.indexOf(b);

        // 都不包含
        if (posA == -1 && posB == -1) {
            return 0;
        }

        // 只包含 a
        if (posA != -1 && posB == -1) {
            return 1;
        }

        // 只包含 b
        if (posA == -1 && posB != -1) {
            return 2;
        }

        // 同时包含 → 比较第一次出现的位置
        // posA < posB 说明 a 更靠前
        return posA < posB ? 1 : 2;
    }

}
