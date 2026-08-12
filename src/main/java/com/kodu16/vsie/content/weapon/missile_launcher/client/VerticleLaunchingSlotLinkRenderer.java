package com.kodu16.vsie.content.weapon.missile_launcher.client;

import com.kodu16.vsie.content.item.linker.linker;
import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.RailCoreRegistry;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.weapon.electro_magnet_rail_accelerator.ElectromagnetRailAcceleratorBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotCoreBlockEntity;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
@SuppressWarnings("removal")
@EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
public class VerticleLaunchingSlotLinkRenderer {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int TEXT_COLOR = 0xFFFFD24A;
    private static final double SLOT_MARKER_HALF_SIZE = 0.25D;
    private static final MarkerStyle WEAPON_STYLE = new MarkerStyle(1.0F, 0.12F, 0.12F, 0xFFFF3030);
    private static final MarkerStyle STORAGE_STYLE = new MarkerStyle(1.0F, 0.82F, 0.18F, 0xFFFFD24A);
    private static final MarkerStyle TURRET_STYLE = new MarkerStyle(0.2F, 1.0F, 0.32F, 0xFF33FF55);
    private static final MarkerStyle PERIPHERAL_STYLE = new MarkerStyle(0.2F, 0.55F, 1.0F, 0xFF3399FF);
    private static final MarkerStyle RAIL_LINK_STYLE = new MarkerStyle(0.0F, 1.0F, 1.0F, 0xFF00FFFF);

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        Level level = MC.level;
        Player player = MC.player;
        if (level == null || player == null) {
            return;
        }

        ItemStack held = getHeldLinker(player);
        if (!held.is(vsieItems.LINKER.get())) {
            return;
        }

        CompoundTag tag = com.kodu16.vsie.utility.ItemStackNbt.get(held);
        if (tag == null) {
            return;
        }

        if (tag.contains(linker.VERTICAL_LAUNCH_CORE_POS_TAG)) {
            if (!(level.getBlockEntity(linker.getBlockPos(tag, linker.VERTICAL_LAUNCH_CORE_POS_TAG)) instanceof VerticleLaunchingSlotCoreBlockEntity core)) {
                return;
            }
            renderLinkedSlots(event.getPoseStack(), core.getLinkedSlots());
            return;
        }

        if (tag.contains(linker.ELECTRO_MAGNET_RAIL_CORE_POS_TAG)) {
            if (!(level.getBlockEntity(linker.getBlockPos(tag, linker.ELECTRO_MAGNET_RAIL_CORE_POS_TAG))
                    instanceof ElectroMagnetRailCoreBlockEntity core)) {
                return;
            }
            renderRailCoreLinks(event.getPoseStack(), core);
            return;
        }

        if (tag.contains(linker.CONTROL_SEAT_POS_TAG)
                && level.getBlockEntity(linker.getBlockPos(tag, linker.CONTROL_SEAT_POS_TAG)) instanceof AbstractControlSeatBlockEntity controlSeat) {
            renderControlSeatLinks(event.getPoseStack(), controlSeat);
        }
    }

    private static ItemStack getHeldLinker(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        return mainHand.is(vsieItems.LINKER.get()) ? mainHand : player.getOffhandItem();
    }

    private static void renderLinkedSlots(PoseStack pose, List<BlockPos> slots) {
        if (slots.isEmpty()) {
            return;
        }

        Vec3 cameraPos = MC.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffers = MC.renderBuffers().bufferSource();

        // Function: disable depth only during the slot overlay so numbers and outlines stay visible through terrain.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            for (int i = 0; i < slots.size(); i++) {
                BlockPos slotPos = slots.get(i);
                if (MC.level == null || !(MC.level.getBlockEntity(slotPos) instanceof VerticleLaunchingSlotBlockEntity)) {
                    continue;
                }
                renderOutline(pose, slotPos, cameraPos, 1.0F, 0.82F, 0.18F);
                renderText(pose, buffers, slotPos, cameraPos, i + 1, TEXT_COLOR);
            }
            buffers.endBatch();
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderControlSeatLinks(PoseStack pose, AbstractControlSeatBlockEntity controlSeat) {
        List<LinkMarker> markers = new ArrayList<>();
        appendControlSeatMarkers(controlSeat, markers, 1, WEAPON_STYLE);
        appendControlSeatMarkers(
                controlSeat,
                markers,
                AbstractControlSeatBlockEntity.ENEMY_CANNON_PERIPHERAL_TYPE,
                WEAPON_STYLE
        );
        appendControlSeatMarkers(controlSeat, markers, STORAGE_STYLE, 4, 5, 6);
        appendControlSeatMarkers(controlSeat, markers, 3, TURRET_STYLE);
        appendControlSeatMarkers(controlSeat, markers, PERIPHERAL_STYLE, 0, 2, 7);
        if (markers.isEmpty()) {
            return;
        }

        Vec3 cameraPos = MC.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffers = MC.renderBuffers().bufferSource();

        // Function: render control-seat linker overlays with the same through-wall visibility as launch-slot markers.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            for (LinkMarker marker : markers) {
                if (MC.level == null || MC.level.getBlockEntity(marker.pos()) == null) {
                    continue;
                }
                renderOutline(pose, marker.pos(), cameraPos, marker.style().red(), marker.style().green(), marker.style().blue());
                renderText(pose, buffers, marker.pos(), cameraPos, marker.index(), marker.style().textColor());
            }
            buffers.endBatch();
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderRailCoreLinks(PoseStack pose, ElectroMagnetRailCoreBlockEntity core) {
        if (MC.level == null || core.getRailBindingId() == null) {
            return;
        }
        List<ElectromagnetRailAcceleratorBlockEntity> accelerators =
                RailCoreRegistry.getClientAccelerators(core.getRailBindingId(), MC.level);
        if (accelerators.isEmpty()) {
            return;
        }
        accelerators.sort(java.util.Comparator.comparingLong(accelerator -> accelerator.getBlockPos().asLong()));

        Vec3 cameraPos = MC.gameRenderer.getMainCamera().getPosition();
        Vec3 coreWorldPos = getSlotWorldPosition(core.getBlockPos(), 0.5D);
        MultiBufferSource.BufferSource buffers = MC.renderBuffers().bufferSource();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            for (int i = 0; i < accelerators.size(); i++) {
                BlockPos acceleratorPos = accelerators.get(i).getBlockPos();
                Vec3 acceleratorWorldPos = getSlotWorldPosition(acceleratorPos, 0.5D);
                renderOutline(pose, acceleratorPos, cameraPos,
                        RAIL_LINK_STYLE.red(), RAIL_LINK_STYLE.green(), RAIL_LINK_STYLE.blue());
                renderWorldLine(pose, coreWorldPos, acceleratorWorldPos, cameraPos,
                        RAIL_LINK_STYLE.red(), RAIL_LINK_STYLE.green(), RAIL_LINK_STYLE.blue());
                renderText(pose, buffers, acceleratorPos, cameraPos, i + 1, RAIL_LINK_STYLE.textColor());
            }
            buffers.endBatch();
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static void appendControlSeatMarkers(AbstractControlSeatBlockEntity controlSeat, List<LinkMarker> markers, int type, MarkerStyle style) {
        appendControlSeatMarkers(controlSeat, markers, style, type);
    }

    private static void appendControlSeatMarkers(AbstractControlSeatBlockEntity controlSeat, List<LinkMarker> markers, MarkerStyle style, int... types) {
        int[] index = {1};
        for (int type : types) {
            controlSeat.forEachLinkedPeripheral(pos -> markers.add(new LinkMarker(BlockPos.containing(pos), style, index[0]++)), type);
        }
    }

    private static void renderOutline(PoseStack pose, BlockPos slotPos, Vec3 cameraPos, float red, float green, float blue) {
        pose.pushPose();
        try {
            Vec3 relativeCenter = getSlotWorldPosition(slotPos, 0.5D).subtract(cameraPos);
            // Function: use a small world-space marker because tilted sublevels cannot be outlined with an axis-aligned box.
            AABB box = new AABB(
                    relativeCenter.subtract(SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE),
                    relativeCenter.add(SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE)
            ).inflate(0.01D);
            renderLineBoxImmediate(pose.last().pose(), box, red, green, blue, 1.0F);
        } finally {
            pose.popPose();
        }
    }

    private static void renderWorldLine(PoseStack pose, Vec3 worldStart, Vec3 worldEnd, Vec3 cameraPos,
                                        float red, float green, float blue) {
        Vec3 start = worldStart.subtract(cameraPos);
        Vec3 end = worldEnd.subtract(cameraPos);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // Function: use the same immediate debug-line path as the cyan accelerator outlines.
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        addBoxLine(buffer, pose.last().pose(), start.x, start.y, start.z, end.x, end.y, end.z,
                red, green, blue, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void renderLineBoxImmediate(Matrix4f matrix, AABB box, float red, float green, float blue, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // Function: keep outline drawing independent from Minecraft's shared level buffer to avoid stale RenderType.lines state.
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        addBoxLine(buffer, matrix, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, red, green, blue, alpha);
        addBoxLine(buffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, red, green, blue, alpha);
        addBoxLine(buffer, matrix, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, red, green, blue, alpha);
        addBoxLine(buffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, red, green, blue, alpha);

        addBoxLine(buffer, matrix, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        addBoxLine(buffer, matrix, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        addBoxLine(buffer, matrix, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
        addBoxLine(buffer, matrix, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, red, green, blue, alpha);

        addBoxLine(buffer, matrix, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        addBoxLine(buffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        addBoxLine(buffer, matrix, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        addBoxLine(buffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void addBoxLine(BufferBuilder buffer, Matrix4f matrix,
                                   double x0, double y0, double z0, double x1, double y1, double z1,
                                   float red, float green, float blue, float alpha) {
        buffer.addVertex(matrix, (float) x0, (float) y0, (float) z0).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(red, green, blue, alpha);
    }

    private static void renderText(PoseStack pose, MultiBufferSource buffer, BlockPos slotPos, Vec3 cameraPos, int index, int textColor) {
        EntityRenderDispatcher dispatcher = MC.getEntityRenderDispatcher();
        Font font = MC.font;
        Vec3 textPos = getSlotWorldPosition(slotPos, 1.65D);
        Vec3 offset = textPos.subtract(cameraPos);
        Component text = Component.literal("#" + index);
        float scale = 0.025F;
        Quaternionf cameraRot = dispatcher.cameraOrientation();

        pose.pushPose();
        try {
            pose.translate(offset.x, offset.y, offset.z);
            pose.mulPose(cameraRot);
            pose.scale(scale, -scale, -scale);
            Matrix4f matrix = pose.last().pose();
            font.drawInBatch(
                    text,
                    -font.width(text) / 2.0F,
                    0,
                    textColor,
                    false,
                    matrix,
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    LightTexture.FULL_BRIGHT
            );
        } finally {
            pose.popPose();
        }
    }

    private static Vec3 getSlotWorldPosition(BlockPos slotPos, double localYOffset) {
        Level level = MC.level;
        Vec3 localPos = new Vec3(slotPos.getX() + 0.5D, slotPos.getY() + localYOffset, slotPos.getZ() + 0.5D);
        if (level == null) {
            return localPos;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(level, slotPos);
        // Function: linked slots live in sublevel coordinates; convert them before drawing client overlays.
        return subLevel == null ? localPos : subLevel.logicalPose().transformPosition(localPos);
    }

    private record MarkerStyle(float red, float green, float blue, int textColor) {
    }

    private record LinkMarker(BlockPos pos, MarkerStyle style, int index) {
    }
}
