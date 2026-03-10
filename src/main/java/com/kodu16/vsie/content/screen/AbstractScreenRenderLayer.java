package com.kodu16.vsie.content.screen;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.functions.WorldMarkerPainter;
import com.kodu16.vsie.content.screen.functions.Radar;
import com.kodu16.vsie.registries.vsieItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.Map;
import java.util.UUID;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class AbstractScreenRenderLayer extends GeoRenderLayer<AbstractScreenBlockEntity> {

    public AbstractScreenRenderLayer(GeoRenderer<AbstractScreenBlockEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    private static final String NOZZLE_BONE_NAME = "screen";
    // 功能：雷达默认中立目标颜色（浅蓝）。
    private static final int RADAR_COLOR_NEUTRAL = 0xFF66CCFF;
    // 功能：雷达敌对目标颜色（红）。
    private static final int RADAR_COLOR_ENEMY = 0xFFFF5555;
    // 功能：雷达友方目标颜色（绿）。
    private static final int RADAR_COLOR_ALLY = 0xFF55FF55;
    // 功能：雷达锁定敌对目标颜色（黄）。
    private static final int RADAR_COLOR_LOCKED_ENEMY = 0xFFFFFF55;
    private static Minecraft mc = Minecraft.getInstance();
    ItemRenderer itemRenderer = mc.getItemRenderer();
    Font font = mc.font;

    @Override
    public void render(PoseStack poseStack, AbstractScreenBlockEntity animatable,
                       software.bernie.geckolib.cache.object.BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public void renderForBone(PoseStack poseStack, AbstractScreenBlockEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (!NOZZLE_BONE_NAME.equals(bone.getName())) {
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay);
            return;
        }
        Level level = animatable.getLevel();
        if (level == null) return;

        ItemStack stack = animatable.getRenderStack();
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        // 旋转以平躺于表面（针对顶部面）
        poseStack.mulPose(Axis.XP.rotationDegrees(-270.0f));  // 对于其他面，使用 Axis.YP 等旋转
        //poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));  // 对于其他面，使用 Axis.YP 等旋转
        poseStack.translate(0, 0, -0.05f);  // 调整为目标面，例如 NORTH: translate(0.5, 0.5, 1.0)
        poseStack.scale(0.99f,0.99f,0.99f);
        itemRenderer.renderStatic(new ItemStack(vsieItems.SCREEN_BG), ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
                level, 0);
        //poseStack.scale(0.15f, 0.15f, 0.15f);
        poseStack.translate(0, 0, -0.05f);  // 调整为目标面，例如 NORTH: translate(0.5, 0.5, 1.0)
        renderRadar(poseStack, animatable, bufferSource);
        poseStack.popPose();
    }

    // 功能：读取绑定玩家的 shipsData，并在屏幕上绘制俯视雷达。
    private void renderRadar(PoseStack poseStack, AbstractScreenBlockEntity screen, MultiBufferSource bufferSource) {
        UUID radarPlayerUuid = screen.getRadarPlayerUuid();
        if (radarPlayerUuid == null || mc.level == null) {
            return;
        }
        // 功能：通过屏幕记录的玩家 UUID 反查该玩家的客户端控制数据。
        var player = mc.level.getPlayerByUUID(radarPlayerUuid);
        if (player == null) {
            return;
        }
        ControlSeatClientData clientData = ClientDataManager.getClientData(player);
        if (clientData == null || clientData.shipsData == null) {
            return;
        }
        // 功能：将雷达绘制区域放在屏幕中间，并保持与现有物品/文字渲染同平面。

        // 功能：先绘制中心方框，表示当前控制椅所在船只（雷达自身）。
        Radar.drawSquare(poseStack, bufferSource, 0f, 0f, 0.02f, 0xFF33FF33);

        Vector3d seatWorldPos = screen.getRadarControlSeatWorldPos();
        for (Map.Entry<String, Object> entry : clientData.shipsData.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> rawMap)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> shipData = (Map<String, Object>) rawMap;
            if (!shipData.containsKey("x") || !shipData.containsKey("z")) {
                continue;
            }
            double shipX = toDouble(shipData.get("x"));
            double shipZ = toDouble(shipData.get("z"));
            double dx = shipX - seatWorldPos.x;
            double dz = shipZ - seatWorldPos.z;
            // 功能：仅显示 512 范围内其它船只，减少噪声并满足需求。
            if (Math.sqrt(dx * dx + dz * dz) > 512.0) {
                continue;
            }
            // 功能：将世界 XZ 相对坐标投影到屏幕局部平面，形成俯视雷达图。
            float px = (float) (dx / 512.0 * 2.0);
            float py = (float) (dz / 512.0 * 2.0);

            // 功能：复用 WorldMarkerPainter 的敌我识别规则，保证 HUD 与雷达判定一致。
            String slug = String.valueOf(shipData.getOrDefault("slug", ""));
            int priority = WorldMarkerPainter.getPriority(clientData.enemy, clientData.ally, slug);
            int radarColor = RADAR_COLOR_NEUTRAL;

            // 功能：根据敌我关系给雷达点分配颜色（enemy=红，ally=绿，其它=浅蓝）。
            if (priority == 1) {
                radarColor = RADAR_COLOR_ENEMY;
            } else if (priority == 2) {
                radarColor = RADAR_COLOR_ALLY;
            }

            // 功能：若该目标是当前锁定敌人，则覆盖为黄色，突出锁定状态。
            if (!slug.isEmpty() && slug.equals(clientData.lockedenemyslug) && priority == 1) {
                radarColor = RADAR_COLOR_LOCKED_ENEMY;
            }

            // 跳过中心点附近，避免与本船方框重叠。
            Radar.drawSquare(poseStack, bufferSource, px, py, 0.02f, radarColor);
        }
    }

    // 功能：将 Object 数值安全转成 double，兼容网络包里的 Number 类型。
    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

}
