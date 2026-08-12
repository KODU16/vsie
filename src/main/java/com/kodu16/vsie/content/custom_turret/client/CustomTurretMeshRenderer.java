package com.kodu16.vsie.content.custom_turret.client;

import com.kodu16.vsie.content.custom_turret.CustomTurretDefinition;
import com.kodu16.vsie.content.custom_turret.CustomTurretBlockEntity;
import com.kodu16.vsie.content.custom_turret.CustomTurretRuntimePose;
import com.kodu16.vsie.network.turret.TurretFirePointC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Applies definition parent transforms before emitting each external OBJ mesh. */
public final class CustomTurretMeshRenderer {
    public static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/stone.png");
    private static final Map<String, Long> LAST_FIREPOINT_SYNC = new HashMap<>();

    private CustomTurretMeshRenderer() {
    }

    public static void render(CustomTurretDefinition definition, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight) {
        render(definition, poseStack, bufferSource, packedLight, 0.0F, 0.0F);
    }

    public static void render(CustomTurretDefinition definition, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight,
                              float turretPitchRadians, float turretYawRadians) {
        render(definition, null, poseStack, bufferSource, packedLight, turretPitchRadians, turretYawRadians);
    }

    public static void render(CustomTurretDefinition definition, CustomTurretBlockEntity turret,
                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                              float turretPitchRadians, float turretYawRadians) {
        Set<String> rendered = new HashSet<>();
        for (CustomTurretDefinition.Bone bone : definition.bones) {
            if (bone.parent.isEmpty()) {
                renderBone(definition, turret, bone, poseStack, bufferSource, packedLight, rendered,
                        turretPitchRadians, turretYawRadians);
            }
        }
    }

    private static void renderBone(CustomTurretDefinition definition, CustomTurretBlockEntity turret,
                                   CustomTurretDefinition.Bone bone,
                                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                   Set<String> rendered, float turretPitchRadians, float turretYawRadians) {
        if (!rendered.add(bone.id)) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(bone.position[0], bone.position[1], bone.position[2]);
        poseStack.translate(bone.pivot[0], bone.pivot[1], bone.pivot[2]);
        poseStack.mulPose(Axis.ZP.rotationDegrees(bone.rotation[2]));
        poseStack.mulPose(Axis.YP.rotationDegrees(bone.rotation[1]));
        poseStack.mulPose(Axis.XP.rotationDegrees(bone.rotation[0]));
        // Function: shared turret servo angles animate the fixed turret/cannon groups while preserving authored offsets.
        poseStack.mulPose(Axis.YP.rotation(CustomTurretRuntimePose.yawRadians(bone.id, turretYawRadians)));
        poseStack.mulPose(Axis.XP.rotation(CustomTurretRuntimePose.pitchRadians(bone.id, turretPitchRadians)));
        poseStack.scale(bone.scale[0], bone.scale[1], bone.scale[2]);
        if (turret != null && CustomTurretDefinition.isFirepointBoneGroup(bone.id)) {
            int firepointIndex = Integer.parseInt(bone.id.substring("firepoint".length()));
            sampleFirepoint(turret, firepointIndex, poseStack);
            if (turret.isEnergyTurret()) {
                // Function: every beam is emitted in its own animated firepoint bone space.
                CustomTurretBeamRenderer.render(poseStack, bufferSource,
                        turret.getLaserDistance(firepointIndex), definition.laserRadius, definition.laserColor);
            }
        }
        poseStack.translate(-bone.pivot[0], -bone.pivot[1], -bone.pivot[2]);

        String texturePath = CustomTurretAssetCache.texturePath(bone.model, bone.texture);
        ResourceLocation texture = CustomTurretAssetCache.texture(texturePath, FALLBACK_TEXTURE);
        CustomTurretAssetCache.mesh(bone.model).render(
                poseStack,
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture)),
                packedLight
        );
        for (CustomTurretDefinition.Bone child : definition.bones) {
            if (bone.id.equals(child.parent)) {
                renderBone(definition, turret, child, poseStack, bufferSource, packedLight, rendered,
                        turretPitchRadians, turretYawRadians);
            }
        }
        poseStack.popPose();
    }

    private static void sampleFirepoint(CustomTurretBlockEntity turret, int index, PoseStack poseStack) {
        if (turret.getLevel() == null) {
            return;
        }
        long gameTime = turret.getLevel().getGameTime();
        String key = turret.getLevel().dimension().location() + ":" + turret.getBlockPos().asLong() + ":" + index;
        long last = LAST_FIREPOINT_SYNC.getOrDefault(key, Long.MIN_VALUE);
        if (last != Long.MIN_VALUE && gameTime >= last && gameTime - last < 2L) {
            return;
        }
        Vector4f local = poseStack.last().pose().transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        if (!Float.isFinite(local.x) || !Float.isFinite(local.y) || !Float.isFinite(local.z)) {
            return;
        }
        LAST_FIREPOINT_SYNC.put(key, gameTime);
        ModNetworking.sendToServer(new TurretFirePointC2SPacket(
                turret.getBlockPos(), index,
                new org.joml.Vector3d(
                        turret.getBlockPos().getX() + local.x,
                        turret.getBlockPos().getY() + local.y,
                        turret.getBlockPos().getZ() + local.z)));
    }
}
