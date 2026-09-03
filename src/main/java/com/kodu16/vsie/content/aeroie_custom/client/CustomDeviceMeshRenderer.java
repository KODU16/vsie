package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceDefinition;
import com.kodu16.vsie.content.aeroie_custom.CustomTurretBlockEntity;
import com.kodu16.vsie.content.aeroie_custom.CustomTurretRuntimePose;
import com.kodu16.vsie.content.aeroie_custom.CustomThrusterBlockEntity;
import com.kodu16.vsie.content.aeroie_custom.CustomWeaponBlockEntity;
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
public final class CustomDeviceMeshRenderer {
    public static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/stone.png");
    private static final Map<String, Long> LAST_FIREPOINT_SYNC = new HashMap<>();

    private CustomDeviceMeshRenderer() {
    }

    public static void render(CustomDeviceDefinition definition, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight) {
        renderInternal(definition, null, null, poseStack, bufferSource, packedLight, 0.0F, 0.0F, false);
    }

    public static void render(CustomDeviceDefinition definition, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight,
                              float turretPitchRadians, float turretYawRadians) {
        renderInternal(definition, null, null, poseStack, bufferSource, packedLight,
                turretPitchRadians, turretYawRadians, true);
    }

    public static void render(CustomDeviceDefinition definition, CustomTurretBlockEntity turret,
                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                              float turretPitchRadians, float turretYawRadians) {
        renderInternal(definition, turret, null, poseStack, bufferSource, packedLight,
                turretPitchRadians, turretYawRadians, true);
    }

    public static void render(CustomDeviceDefinition definition, CustomThrusterBlockEntity thruster,
                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderInternal(definition, null, thruster, poseStack, bufferSource, packedLight,
                0.0F, 0.0F, false);
    }

    public static void render(CustomDeviceDefinition definition, CustomWeaponBlockEntity weapon,
                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderInternal(definition, null, null, weapon, poseStack, bufferSource, packedLight,
                0.0F, 0.0F, false);
    }

    private static void renderInternal(CustomDeviceDefinition definition, CustomTurretBlockEntity turret,
                                       CustomThrusterBlockEntity thruster,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       float turretPitchRadians, float turretYawRadians,
                                       boolean applyRuntimePose) {
        renderInternal(definition, turret, thruster, null, poseStack, bufferSource, packedLight,
                turretPitchRadians, turretYawRadians, applyRuntimePose);
    }

    private static void renderInternal(CustomDeviceDefinition definition, CustomTurretBlockEntity turret,
                                       CustomThrusterBlockEntity thruster, CustomWeaponBlockEntity weapon,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       float turretPitchRadians, float turretYawRadians,
                                       boolean applyRuntimePose) {
        Set<String> rendered = new HashSet<>();
        for (CustomDeviceDefinition.Bone bone : definition.bones) {
            if (bone.parent.isEmpty()) {
                renderBone(definition, turret, thruster, weapon, bone, poseStack, bufferSource, packedLight, rendered,
                        turretPitchRadians, turretYawRadians, applyRuntimePose);
            }
        }
    }

    private static void renderBone(CustomDeviceDefinition definition, CustomTurretBlockEntity turret,
                                   CustomThrusterBlockEntity thruster, CustomWeaponBlockEntity weapon,
                                   CustomDeviceDefinition.Bone bone,
                                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                   Set<String> rendered, float turretPitchRadians, float turretYawRadians,
                                   boolean applyRuntimePose) {
        if (!rendered.add(bone.id)) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(bone.position[0], bone.position[1], bone.position[2]);
        poseStack.translate(bone.pivot[0], bone.pivot[1], bone.pivot[2]);
        poseStack.mulPose(Axis.ZP.rotationDegrees(bone.rotation[2]));
        poseStack.mulPose(Axis.YP.rotationDegrees(bone.rotation[1]));
        poseStack.mulPose(Axis.XP.rotationDegrees(bone.rotation[0]));
        if (applyRuntimePose) {
            // Function: runtime includes the +Z-to-AeroIE adapter; the editor must preserve authored orientation.
            poseStack.mulPose(Axis.YP.rotation(CustomTurretRuntimePose.yawRadians(bone.id, turretYawRadians)));
            poseStack.mulPose(Axis.XP.rotation(CustomTurretRuntimePose.pitchRadians(bone.id, turretPitchRadians)));
        }
        poseStack.scale(bone.scale[0], bone.scale[1], bone.scale[2]);
        if (turret != null && CustomDeviceDefinition.isFirepointBoneGroup(bone.id)) {
            int firepointIndex = Integer.parseInt(bone.id.substring("firepoint".length()));
            sampleFirepoint(turret, firepointIndex, poseStack);
            if (turret.isEnergyTurret()) {
                // Function: every beam is emitted in its own animated firepoint bone space.
                CustomTurretBeamRenderer.render(poseStack, bufferSource,
                        turret.getLaserDistance(firepointIndex), definition.laserRadius, definition.laserColor);
            }
        }
        if (weapon != null && CustomDeviceDefinition.isFirepointBoneGroup(bone.id)) {
            int firepointIndex = Integer.parseInt(bone.id.substring("firepoint".length()));
            sampleFirepoint(weapon, firepointIndex, poseStack);
            if (weapon.isEnergyWeapon()) {
                // Function: custom weapon beams are drawn from authored firepoints instead of the block center.
                CustomTurretBeamRenderer.render(poseStack, bufferSource,
                        weapon.getLaserDistance(firepointIndex), definition.laserRadius, definition.laserColor);
            }
        }
        if (thruster != null && CustomDeviceDefinition.isFlamepointBoneGroup(bone.id)
                && thruster.shouldRenderFlame()) {
            int flamepointIndex = Integer.parseInt(bone.id.substring("flamepoint".length()));
            sampleFlamepoint(thruster, flamepointIndex, poseStack);
            // Function: each authored flamepoint owns a separate beam in the same transformed bone space.
            CustomTurretBeamRenderer.render(poseStack, bufferSource,
                    thruster.getRaycastDistance() * 1.5D, definition.flameRadius,
                    definition.flameColor, definition.flameSegments);
        }
        if (thruster != null && CustomDeviceDefinition.isTrailpointBoneGroup(bone.id)) {
            int trailpointIndex = Integer.parseInt(bone.id.substring("trailpoint".length()));
            sampleTrailpoint(thruster, trailpointIndex, poseStack);
            CustomThrusterTrailRenderer.render(poseStack, thruster, bufferSource, trailpointIndex);
        }
        poseStack.translate(-bone.pivot[0], -bone.pivot[1], -bone.pivot[2]);

        CustomObjMesh mesh = CustomDeviceAssetCache.mesh(definition.deviceType, bone.model);
        if (bone.texture.isBlank()) {
            renderMeshByMaterial(definition.deviceType, bone.model, mesh, poseStack, bufferSource, packedLight);
        } else {
            // Function: an explicit bone texture overrides the OBJ/MTL materials and textures the whole mesh with it.
            ResourceLocation texture = CustomDeviceAssetCache.texture(definition.deviceType, bone.texture, FALLBACK_TEXTURE);
            mesh.render(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight);
        }
        for (CustomDeviceDefinition.Bone child : definition.bones) {
            if (bone.id.equals(child.parent)) {
                renderBone(definition, turret, thruster, weapon, child, poseStack, bufferSource, packedLight, rendered,
                        turretPitchRadians, turretYawRadians, applyRuntimePose);
            }
        }
        poseStack.popPose();
    }

    private static void renderMeshByMaterial(String deviceType, String modelPath, CustomObjMesh mesh,
                                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Map<String, String> materialTextures = CustomDeviceAssetCache.materialTextures(deviceType, modelPath);
        String fallbackTexturePath = materialTextures.values().stream()
                .filter(path -> !path.isBlank())
                .findFirst()
                .orElse("");
        for (String material : mesh.materials()) {
            String texturePath = materialTextures.getOrDefault(material, fallbackTexturePath);
            ResourceLocation texture = CustomDeviceAssetCache.texture(deviceType, texturePath, FALLBACK_TEXTURE);
            // Function: every OBJ material owns its MTL map_Kd texture so multi-material meshes keep all faces textured.
            mesh.renderMaterial(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture)),
                    material, packedLight);
        }
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
        Vector4f direction = poseStack.last().pose().transform(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
        if (!Float.isFinite(local.x) || !Float.isFinite(local.y) || !Float.isFinite(local.z)) {
            return;
        }
        LAST_FIREPOINT_SYNC.put(key, gameTime);
        ModNetworking.sendToServer(new TurretFirePointC2SPacket(
                turret.getBlockPos(), index,
                new org.joml.Vector3d(
                        turret.getBlockPos().getX() + local.x,
                        turret.getBlockPos().getY() + local.y,
                        turret.getBlockPos().getZ() + local.z),
                new org.joml.Vector3d(direction.x, direction.y, direction.z)));
    }

    private static void sampleFirepoint(CustomWeaponBlockEntity weapon, int index, PoseStack poseStack) {
        if (weapon.getLevel() == null) {
            return;
        }
        long gameTime = weapon.getLevel().getGameTime();
        String key = weapon.getLevel().dimension().location() + ":weapon:" + weapon.getBlockPos().asLong() + ":" + index;
        long last = LAST_FIREPOINT_SYNC.getOrDefault(key, Long.MIN_VALUE);
        if (last != Long.MIN_VALUE && gameTime >= last && gameTime - last < 2L) {
            return;
        }
        Vector4f local = poseStack.last().pose().transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        Vector4f direction = poseStack.last().pose().transform(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
        if (!Float.isFinite(local.x) || !Float.isFinite(local.y) || !Float.isFinite(local.z)) {
            return;
        }
        LAST_FIREPOINT_SYNC.put(key, gameTime);
        ModNetworking.sendToServer(new TurretFirePointC2SPacket(
                weapon.getBlockPos(), index,
                new org.joml.Vector3d(
                        weapon.getBlockPos().getX() + local.x,
                        weapon.getBlockPos().getY() + local.y,
                        weapon.getBlockPos().getZ() + local.z),
                new org.joml.Vector3d(direction.x, direction.y, direction.z)));
    }

    private static void sampleFlamepoint(CustomThrusterBlockEntity thruster, int index, PoseStack poseStack) {
        if (thruster.getLevel() == null) {
            return;
        }
        long gameTime = thruster.getLevel().getGameTime();
        String key = thruster.getLevel().dimension().location() + ":flame:" + thruster.getBlockPos().asLong() + ":" + index;
        long last = LAST_FIREPOINT_SYNC.getOrDefault(key, Long.MIN_VALUE);
        if (last != Long.MIN_VALUE && gameTime >= last && gameTime - last < 2L) {
            return;
        }
        Vector4f local = poseStack.last().pose().transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        Vector4f direction = poseStack.last().pose().transform(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
        if (!Float.isFinite(local.x) || !Float.isFinite(local.y) || !Float.isFinite(local.z)) {
            return;
        }
        LAST_FIREPOINT_SYNC.put(key, gameTime);
        ModNetworking.sendToServer(new TurretFirePointC2SPacket(
                thruster.getBlockPos(), index,
                new org.joml.Vector3d(
                        thruster.getBlockPos().getX() + local.x,
                        thruster.getBlockPos().getY() + local.y,
                        thruster.getBlockPos().getZ() + local.z),
                new org.joml.Vector3d(direction.x, direction.y, direction.z)));
    }

    private static void sampleTrailpoint(CustomThrusterBlockEntity thruster, int index, PoseStack poseStack) {
        Vector4f local = poseStack.last().pose().transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        if (!Float.isFinite(local.x) || !Float.isFinite(local.y) || !Float.isFinite(local.z)) {
            return;
        }
        thruster.setTrailPoint(index, new org.joml.Vector3d(
                thruster.getBlockPos().getX() + local.x,
                thruster.getBlockPos().getY() + local.y,
                thruster.getBlockPos().getZ() + local.z));
    }
}
