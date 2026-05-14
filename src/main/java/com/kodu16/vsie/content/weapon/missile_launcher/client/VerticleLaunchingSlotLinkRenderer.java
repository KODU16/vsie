package com.kodu16.vsie.content.weapon.missile_launcher.client;

import com.kodu16.vsie.content.item.linker.linker;
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

import java.util.List;
@SuppressWarnings("removal")
@EventBusSubscriber(value = Dist.CLIENT, modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
public class VerticleLaunchingSlotLinkRenderer {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int TEXT_COLOR = 0xFFFFD24A;
    private static final double SLOT_MARKER_HALF_SIZE = 0.25D;

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
        if (tag == null || !tag.contains(linker.VERTICAL_LAUNCH_CORE_POS_TAG)) {
            return;
        }

        if (!(level.getBlockEntity(linker.getBlockPos(tag, linker.VERTICAL_LAUNCH_CORE_POS_TAG)) instanceof VerticleLaunchingSlotCoreBlockEntity core)) {
            return;
        }

        renderLinkedSlots(event.getPoseStack(), core.getLinkedSlots());
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
            VertexConsumer lineConsumer = buffers.getBuffer(RenderType.lines());
            for (int i = 0; i < slots.size(); i++) {
                BlockPos slotPos = slots.get(i);
                if (MC.level == null || !(MC.level.getBlockEntity(slotPos) instanceof VerticleLaunchingSlotBlockEntity)) {
                    continue;
                }
                renderOutline(pose, lineConsumer, slotPos, cameraPos);
                renderText(pose, buffers, slotPos, cameraPos, i + 1);
            }
            buffers.endBatch();
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderOutline(PoseStack pose, VertexConsumer lineConsumer, BlockPos slotPos, Vec3 cameraPos) {
        pose.pushPose();
        try {
            Vec3 worldCenter = getSlotWorldPosition(slotPos, 0.5D);
            pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            // Function: use a small world-space marker because tilted sublevels cannot be outlined with an axis-aligned box.
            AABB box = new AABB(
                    worldCenter.subtract(SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE),
                    worldCenter.add(SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE, SLOT_MARKER_HALF_SIZE)
            ).inflate(0.01D);
            LevelRenderer.renderLineBox(pose, lineConsumer, box, 1.0F, 0.82F, 0.18F, 1.0F);
        } finally {
            pose.popPose();
        }
    }

    private static void renderText(PoseStack pose, MultiBufferSource buffer, BlockPos slotPos, Vec3 cameraPos, int index) {
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
                    TEXT_COLOR,
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
}
