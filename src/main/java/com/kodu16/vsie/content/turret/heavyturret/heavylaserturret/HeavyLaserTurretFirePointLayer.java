package com.kodu16.vsie.content.turret.heavyturret.heavylaserturret;

import com.kodu16.vsie.network.turret.TurretFirePointC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashMap;
import java.util.Map;

public class HeavyLaserTurretFirePointLayer extends GeoRenderLayer<HeavyLaserTurretBlockEntity> {
    private static final String FIREPOINT_BONE = "locater";
    private static final long SEND_INTERVAL_TICKS = 2L;
    private static final Map<BlockPos, Long> LAST_SENT_TICK = new HashMap<>();

    public HeavyLaserTurretFirePointLayer(GeoRenderer<HeavyLaserTurretBlockEntity> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, HeavyLaserTurretBlockEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (!FIREPOINT_BONE.equals(bone.getName())) {
            return;
        }

        Level level = animatable.getLevel();
        if (level == null) {
            return;
        }
        BlockPos blockPos = animatable.getBlockPos();
        long gameTime = level.getGameTime();
        long lastSentTick = LAST_SENT_TICK.getOrDefault(blockPos, Long.MIN_VALUE);
        if (lastSentTick != Long.MIN_VALUE && gameTime >= lastSentTick && gameTime - lastSentTick < SEND_INTERVAL_TICKS) {
            return;
        }

        Vector3d firePoint = resolveFirePoint(poseStack, bone, blockPos);
        if (!isFinite(firePoint)) {
            return;
        }
        LAST_SENT_TICK.put(blockPos, gameTime);
        ModNetworking.sendToServer(new TurretFirePointC2SPacket(blockPos, firePoint));
    }

    private static Vector3d resolveFirePoint(PoseStack poseStack, GeoBone bone, BlockPos blockPos) {
        poseStack.pushPose();
        // Function: sample the custom locater bone from the same transform used for the visible laser.
        RenderUtil.translateMatrixToBone(poseStack, bone);
        RenderUtil.rotateMatrixAroundBone(poseStack, bone);
        RenderUtil.scaleMatrixForBone(poseStack, bone);
        Vector4f pivot = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
        poseStack.last().pose().transform(pivot);
        poseStack.popPose();
        // Function: heavy laser muzzle is intentionally raised one block above the locater sample.
        return new Vector3d(blockPos.getX() + pivot.x(), blockPos.getY() + pivot.y() + 1.0D, blockPos.getZ() + pivot.z());
    }

    private static boolean isFinite(Vector3d value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
}
