package com.kodu16.vsie.integration.deepspace;

import com.kodu16.vsie.registries.vsieKeyMappings;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renders the optional DeepSpace relay and planet inspection layer without a hard mod dependency. */
@EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
@SuppressWarnings("removal")
public final class DeepSpaceHudRenderer {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final ResourceLocation TARGET_FRAME =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/item/target_frame.png");
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    private static final int PANEL_BACKGROUND = 0xB0182028;
    private static final int PANEL_BORDER = 0xB060BFFF;
    private static final int TEXT_COLOR = 0xFFF2F7FF;
    private static final double AIM_DISTANCE = 10_000_000.0D;
    private static boolean enabled;

    private DeepSpaceHudRenderer() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (vsieKeyMappings.KEY_TOGGLE_DEEPSPACE_HUD.consumeClick()) {
            if (!DeepSpaceHudBridge.available()) {
                enabled = false;
                continue;
            }
            enabled = !enabled;
            if (MC.player != null) {
                MC.player.displayClientMessage(Component.translatable(
                        enabled ? "gui.vsie.deepspace_hud.enabled" : "gui.vsie.deepspace_hud.disabled"
                ), true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER
                || !canRender() || MC.level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = MC.renderBuffers().bufferSource();
        beginSeeThroughRender();
        try {
            for (DeepSpaceHudBridge.Body body : DeepSpaceHudBridge.bodies(MC.level.dimension())) {
                if (body.hyperRelay()) {
                    renderRelay(event.getPoseStack(), buffers, camera, body);
                }
            }
            buffers.endBatch();
        } finally {
            endSeeThroughRender();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName()) || !canRender() || MC.level == null) {
            return;
        }
        DeepSpaceHudBridge.Body aimed = findAimedPlanet(
                DeepSpaceHudBridge.bodies(MC.level.dimension()),
                MC.gameRenderer.getMainCamera().getPosition(),
                cameraLookVector()
        );
        if (aimed != null) {
            renderPlanetPanels(event.getGuiGraphics(), aimed, MC.gameRenderer.getMainCamera().getPosition());
        }
    }

    private static boolean canRender() {
        return enabled && !MC.options.hideGui && DeepSpaceHudBridge.available();
    }

    /** Uses the actual model bounds, so a large nearby planet wins over a farther center point. */
    static DeepSpaceHudBridge.Body findAimedPlanet(List<DeepSpaceHudBridge.Body> bodies, Vec3 origin, Vec3 direction) {
        List<DeepSpaceHudMath.Candidate<DeepSpaceHudBridge.Body>> candidates = bodies.stream()
                .map(body -> new DeepSpaceHudMath.Candidate<>(body, bounds(body.bounds()), body.hyperRelay()))
                .toList();
        return DeepSpaceHudMath.findAimed(candidates, point(origin), point(direction), AIM_DISTANCE);
    }

    private static Vec3 cameraLookVector() {
        Vector3f look = MC.gameRenderer.getMainCamera().getLookVector();
        return new Vec3(look.x, look.y, look.z);
    }

    private static void renderRelay(PoseStack pose, MultiBufferSource buffer, Vec3 camera,
                                    DeepSpaceHudBridge.Body relay) {
        EntityRenderDispatcher dispatcher = MC.getEntityRenderDispatcher();
        renderIcon(camera, relay.center(), dispatcher, pose);
        renderText(camera, relay.center(), dispatcher, pose, buffer,
                Component.literal(relay.name()).withStyle(ChatFormatting.AQUA), 18.0F);
    }

    private static void renderIcon(Vec3 camera, Vec3 target, EntityRenderDispatcher dispatcher, PoseStack pose) {
        double distance = Math.max(1.0D, camera.distanceTo(target));
        float scale = (float) distance * 0.1F;
        Vec3 offset = target.subtract(camera);
        pose.pushPose();
        try {
            pose.translate(offset.x, offset.y, offset.z);
            pose.mulPose(dispatcher.cameraOrientation());
            pose.scale(scale, scale, scale);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TARGET_FRAME);
            BufferBuilder vertices = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX
            );
            Matrix4f matrix = pose.last().pose();
            vertices.addVertex(matrix, -0.5F, -0.5F, 0.0F).setUv(0.0F, 1.0F);
            vertices.addVertex(matrix, 0.5F, -0.5F, 0.0F).setUv(1.0F, 1.0F);
            vertices.addVertex(matrix, 0.5F, 0.5F, 0.0F).setUv(1.0F, 0.0F);
            vertices.addVertex(matrix, -0.5F, 0.5F, 0.0F).setUv(0.0F, 0.0F);
            BufferUploader.drawWithShader(vertices.buildOrThrow());
        } finally {
            pose.popPose();
        }
    }

    private static void renderText(Vec3 camera, Vec3 target, EntityRenderDispatcher dispatcher,
                                   PoseStack pose, MultiBufferSource buffer, Component text, float yOffset) {
        float scale = 0.003F * (float) Math.max(1.0D, camera.distanceTo(target));
        pose.pushPose();
        try {
            pose.translate(target.x - camera.x, target.y - camera.y, target.z - camera.z);
            Quaternionf orientation = dispatcher.cameraOrientation();
            pose.mulPose(orientation);
            pose.scale(scale, -scale, -scale);
            pose.translate(0.0F, yOffset, 0.0F);
            MC.font.drawInBatch(text, -MC.font.width(text) / 2.0F, 0.0F, TEXT_COLOR, false,
                    pose.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
        } finally {
            pose.popPose();
        }
    }

    private static void renderPlanetPanels(GuiGraphics graphics, DeepSpaceHudBridge.Body planet, Vec3 camera) {
        List<Component> first = List.of(
                Component.literal(planet.name()),
                planet.discoverer().isBlank()
                        ? Component.translatable("gui.vsie.deepspace_hud.new_discovery")
                        : Component.translatable("gui.vsie.deepspace_hud.discoverer", planet.discoverer())
        );
        List<Component> second = new ArrayList<>();
        second.add(Component.translatable("gui.vsie.deepspace_hud.distance",
                String.format(Locale.ROOT, "%.1f", distanceToBounds(camera, planet.bounds()))));
        second.add(planet.biomes().isEmpty()
                ? Component.translatable("gui.vsie.deepspace_hud.unknown_planet")
                : Component.translatable("gui.vsie.deepspace_hud.planet_type", translatedId("biome", planet.biomes().getFirst())));
        planet.fluids().forEach(id -> second.add(Component.literal("◆ ").append(translatedId("fluid", id))));
        planet.blocks().forEach(id -> second.add(Component.literal("◇ ").append(translatedId("block", id))));

        int x = graphics.guiWidth() / 2 + 24;
        int y = Math.max(12, graphics.guiHeight() / 2 - 62);
        drawPanel(graphics, first, x, y);
        drawPanel(graphics, second, x, y + panelHeight(first) + 5);
    }

    private static Component translatedId(String kind, String rawId) {
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null) {
            return Component.literal(rawId);
        }
        String key = kind + "." + id.getNamespace() + "." + id.getPath();
        Component translated = Component.translatable(key);
        return translated.getString().equals(key)
                ? Component.literal(id.getPath().replace('_', ' '))
                : translated;
    }

    private static double distanceToBounds(Vec3 point, AABB bounds) {
        return DeepSpaceHudMath.distanceToBounds(point(point), bounds(bounds));
    }

    private static DeepSpaceHudMath.Point point(Vec3 value) {
        return new DeepSpaceHudMath.Point(value.x, value.y, value.z);
    }

    private static DeepSpaceHudMath.Bounds bounds(AABB value) {
        return new DeepSpaceHudMath.Bounds(
                value.minX, value.minY, value.minZ, value.maxX, value.maxY, value.maxZ
        );
    }

    private static void drawPanel(GuiGraphics graphics, List<Component> lines, int x, int y) {
        int width = lines.stream().mapToInt(MC.font::width).max().orElse(0) + 12;
        int height = panelHeight(lines);
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.renderOutline(x, y, width, height, PANEL_BORDER);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(MC.font, lines.get(index), x + 6, y + 5 + index * 10, TEXT_COLOR, false);
        }
    }

    private static int panelHeight(List<Component> lines) {
        return 10 * lines.size() + 8;
    }

    private static void beginSeeThroughRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static void endSeeThroughRender() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
