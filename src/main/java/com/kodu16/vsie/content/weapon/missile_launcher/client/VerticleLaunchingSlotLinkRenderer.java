package com.kodu16.vsie.content.weapon.missile_launcher.client;

import com.kodu16.vsie.content.item.linker.linker;
import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotCoreBlockEntity;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
            // Function: use the vanilla line render type because this overlay only needs stable world-space outlines.
            VertexConsumer lineConsumer = buffers.getBuffer(RenderType.lines());
            for (int i = 0; i < slots.size(); i++) {
                BlockPos slotPos = slots.get(i);
                if (MC.level == null || !(MC.level.getBlockEntity(slotPos) instanceof VerticleLaunchingSlotBlockEntity)) {
                    continue;
                }
                renderOutline(pose, lineConsumer, slotPos, cameraPos, 1.0F, 0.82F, 0.18F);
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
            // Function: use the vanilla line render type because this overlay only needs stable world-space outlines.
            VertexConsumer lineConsumer = buffers.getBuffer(RenderType.lines());
            for (LinkMarker marker : markers) {
                if (MC.level == null || MC.level.getBlockEntity(marker.pos()) == null) {
                    continue;
                }
                renderOutline(pose, lineConsumer, marker.pos(), cameraPos, marker.style().red(), marker.style().green(), marker.style().blue());
                renderText(pose, buffers, marker.pos(), cameraPos, marker.index(), marker.style().textColor());
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

    private static void renderOutline(PoseStack pose, VertexConsumer lineConsumer, BlockPos slotPos, Vec3 cameraPos,
                                      float red, float green, float blue) {
        pose.pushPose();
        try {
            Vec3 worldCenter = getSlotWorldPosition(slotPos, 0.5D);
            pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            // Function: use a small world-space marker because tilted sublevels cannot be outlined with an axis-aligned box.
            AABB box = new AABB(
                    worldCenter.subtract(SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE),
                    worldCenter.add(SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE)
            ).inflate(0.01D);
            LevelRenderer.renderLineBox(pose, lineConsumer, box, red, green, blue, 1.0F);
        } finally {
            pose.popPose();
        }
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
