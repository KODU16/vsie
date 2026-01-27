package com.kodu16.vsie.content.controlseat.client.HUD;

import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
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
                    .append("\n")               // 这里 \n 有效！
                    .append(dist);
            render(playerpos, target, mc.getEntityRenderDispatcher(), mc.font, pose, buffers, text);
        }
    }
    private static void getRenderpos() {
        var level = mc.level;
        if (level == null) {
            return;
        }
        LogUtils.getLogger().warn("finding camera entity");
        if (!(mc.getCameraEntity() instanceof Player player)) {
            shipsData = new HashMap<>();
            playerpos = null;
            return;
        }
        shipsData = ClientDataManager.getClientData(player).shipsData;
        playerpos = player.getEyePosition();
        LogUtils.getLogger().warn("current player:"+player+"pos:"+playerpos);
    }

    private static void render(Vec3 camPos, Vec3 targetPos, EntityRenderDispatcher dispatcher,
                               Font font, PoseStack pose, MultiBufferSource buffer, Component text) {
        ItemRenderer itemRenderer = mc.getItemRenderer();
        //LogUtils.getLogger().warn("正在尝试渲染船 " + targetPos);

        double distance = Vec.Distance(new Vector3d(camPos.x,camPos.y,camPos.z), new Vector3d(targetPos.x,targetPos.y,targetPos.z));
        float baseScale = 0.6f;           // 基础大小，0.3～0.6 之间调
        float distanceFactor = (float) distance * 0.25f; // 或者用 Math.sqrt(dist) 增长更平滑
        float scale = baseScale * Math.max(0.4f, distanceFactor); // 避免太近过大

        pose.pushPose();
        try {
            ItemStack markerItem = new ItemStack(vsieItems.TARGET_FRAME);  // 换成你想要的物品
            pose.translate(targetPos.x - camPos.x, targetPos.y - camPos.y, targetPos.z - camPos.z);
            pose.mulPose(dispatcher.cameraOrientation());

            pose.scale(scale, scale, 0.001F);

            // 强烈建议加这两行做穿透测试
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            // 使用 FIXED 适合世界中静止的 billboard 物品
            itemRenderer.renderStatic(
                    markerItem,
                    ItemDisplayContext.FIXED,          // 或 GUI / THIRD_PERSON_LEFT_HAND 看效果
                    LightTexture.FULL_BRIGHT,          // 全亮（不受光照影响）
                    OverlayTexture.NO_OVERLAY,
                    pose,
                    buffer,
                    mc.level,                          // 可以传 null，如果不需要随机种子
                    0                                  // seed，通常 0
            );
            // 恢复状态！！！
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();

        } finally {
            pose.popPose();
        }

        pose.pushPose();
        try {
            // 然后再处理文字
            float w = font.width(text);
            float textOffsetX = 0.75f;           // 调这个值：0.5～1.2 之间最自然
            pose.translate(targetPos.x - camPos.x, targetPos.y - camPos.y, targetPos.z - camPos.z);
            pose.mulPose(dispatcher.cameraOrientation());
            pose.scale((float) (-0.02*scale), (float)(-0.02*scale), (float) -0.02*scale);
            font.drawInBatch(
                    text,
                    -w / 2f+textOffsetX,          // 仍然居中，但现在整体已经右移了
                    -4,            // 稍微向上抬一点（可选，避免和物品底部重叠）
                    0xFFFF5555,
                    false,
                    pose.last().pose(),
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    LightTexture.FULL_BRIGHT
            );

        } finally {
            pose.popPose();
        }
    }
}
