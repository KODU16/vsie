package com.kodu16.vsie.content.aeroie_custom;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Definition-driven custom thruster that reuses AeroIE's common control-seat thrust lifecycle. */
public final class CustomThrusterBlockEntity extends AbstractThrusterBlockEntity implements CustomDeviceBlockEntity {
    private static final String DEFINITION_TAG = "CustomThrusterDefinition";
    private static final int TRAIL_MAX_VERTICES = 20;

    private CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("missing_thruster", "thruster");
    private String definitionJson = definition.toJson();
    private final Map<Integer, Vector3d> sampledTrailpoints = new HashMap<>();
    private final Map<Integer, Vector3d> sampledFlamepoints = new HashMap<>();
    private final Map<Integer, Vector3d> sampledFlamepointDirections = new HashMap<>();
    private final Map<Integer, Deque<Vec3>> customTrailVertices = new HashMap<>();
    private long lastPointFxGameTime = Long.MIN_VALUE;

    public CustomThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public CustomDeviceDefinition getDefinition() {
        return definition;
    }

    @Override
    public void setDefinitionJson(String json) {
        CustomDeviceDefinition parsed = CustomDeviceDefinition.fromJson(json);
        if (!"thruster".equals(parsed.deviceType)) {
            throw new IllegalArgumentException("Custom thruster block requires a thruster definition");
        }
        definition = parsed;
        definitionJson = parsed.toJson();
        sampledTrailpoints.keySet().removeIf(index -> index > definition.trailpointCount);
        sampledFlamepoints.keySet().removeIf(index -> index > definition.flamepointCount);
        sampledFlamepointDirections.keySet().removeIf(index -> index > definition.flamepointCount);
        customTrailVertices.keySet().removeIf(index -> index > definition.trailpointCount);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public float getMaxFlameDistance() {
        return 10.0F;
    }

    @Override
    public float getZAxisOffset() {
        return 0.0F;
    }

    @Override
    public float getMaxThrust() {
        return definition.thrusterThrust;
    }

    @Override
    public float getflamewidth() {
        return definition.flameRadius;
    }

    public float getCustomTrailRadius() {
        return definition.trailRadius;
    }

    public int getCustomFlameColor() {
        return definition.flameColor;
    }

    public int getCustomTrailColor() {
        return definition.trailColor;
    }

    public int getCustomFlameSegments() {
        return definition.flameSegments;
    }

    public void setTrailPoint(int index, Vector3d trailpoint) {
        if (index < 1 || index > definition.trailpointCount || trailpoint == null
                || !Double.isFinite(trailpoint.x) || !Double.isFinite(trailpoint.y) || !Double.isFinite(trailpoint.z)) {
            return;
        }
        // Function: trailpoint samples are local/sublevel block coordinates from the same transform as OBJ rendering.
        sampledTrailpoints.put(index, new Vector3d(trailpoint));
    }

    public void setFlamePoint(int index, Vector3d flamepoint, Vector3d direction) {
        if (index < 1 || index > definition.flamepointCount || flamepoint == null
                || !Double.isFinite(flamepoint.x) || !Double.isFinite(flamepoint.y) || !Double.isFinite(flamepoint.z)) {
            return;
        }
        // Function: flamepoint samples drive optional Photon point FX in the same animated space as beam flames.
        sampledFlamepoints.put(index, new Vector3d(flamepoint));
        sampledFlamepointDirections.put(index, sanitizedDirection(direction));
    }

    public List<Vec3> getCustomTrailVerticesSnapshot(int index) {
        Deque<Vec3> vertices = customTrailVertices.get(index);
        return vertices == null ? List.of() : new ArrayList<>(vertices);
    }

    @Override
    public int fuelconsumptionperthrottle() {
        return definition.thrusterFuelMbPerTickPerPercent;
    }

    @Override
    protected boolean isWorking() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || getFuelThrottle() <= 0) {
            return;
        }
        playFlamepointFx();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // Function: validate deleted external OBJ files once when the placed custom device loads.
            CustomDeviceResourceGuard.removePlacedDeviceIfMissingObj(level, worldPosition, definition);
        }
    }

    private void playFlamepointFx() {
        long gameTime = level.getGameTime();
        if (lastPointFxGameTime != Long.MIN_VALUE && gameTime - lastPointFxGameTime < 2L) {
            return;
        }
        lastPointFxGameTime = gameTime;
        for (int index = 1; index <= definition.flamepointCount; index++) {
            CustomDeviceDefinition.Bone bone = definition.findBone("flamepoint" + index);
            Vec3 flamepoint = resolveFlamepointWorld(level, index);
            if (bone == null || !bone.pointFx.isEnabled() || flamepoint == null) {
                continue;
            }
            Vector3d direction = sampledFlamepointDirections.getOrDefault(index, new Vector3d(0.0D, 0.0D, 1.0D));
            ModNetworking.sendToAll(new FxPositionS2CPacket(
                    bone.pointFx.resourceLocation(), flamepoint.x, flamepoint.y, flamepoint.z,
                    CustomFxTransform.rotationForDirection(bone.pointFx, direction),
                    CustomFxTransform.scaleVector(bone.pointFx), false, true));
        }
    }

    @Override
    protected void tickClientTrail(Level level) {
        if (!shouldRenderTrail()) {
            customTrailVertices.clear();
            return;
        }
        customTrailVertices.keySet().removeIf(index -> index > definition.trailpointCount);
        for (int index = 1; index <= definition.trailpointCount; index++) {
            Vec3 worldPoint = resolveTrailpointWorld(level, index);
            if (worldPoint == null) {
                continue;
            }
            Deque<Vec3> vertices = customTrailVertices.computeIfAbsent(index, ignored -> new ArrayDeque<>());
            vertices.addLast(worldPoint);
            while (vertices.size() > TRAIL_MAX_VERTICES) {
                vertices.removeFirst();
            }
        }
    }

    private Vec3 resolveTrailpointWorld(Level level, int index) {
        Vector3d sampled = sampledTrailpoints.get(index);
        if (sampled == null) {
            return null;
        }
        Vec3 local = new Vec3(sampled.x, sampled.y, sampled.z);
        var subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        return subLevel == null ? local : subLevel.logicalPose().transformPosition(local);
    }

    private Vec3 resolveFlamepointWorld(Level level, int index) {
        Vector3d sampled = sampledFlamepoints.get(index);
        if (sampled == null) {
            return null;
        }
        Vec3 local = new Vec3(sampled.x, sampled.y, sampled.z);
        var subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        return subLevel == null ? local : subLevel.logicalPose().transformPosition(local);
    }

    @Override
    public String getthrustertype() {
        return "custom";
    }

    public Component getDisplayName() {
        return Component.literal(definition.name);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString(DEFINITION_TAG, definitionJson);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(DEFINITION_TAG)) {
            try {
                setDefinitionJson(tag.getString(DEFINITION_TAG));
            } catch (IllegalArgumentException ignored) {
                // Invalid migrated data leaves the safe fallback custom thruster definition loaded.
            }
        }
    }

    private static Vector3d sanitizedDirection(Vector3d direction) {
        if (direction == null || !Double.isFinite(direction.x) || !Double.isFinite(direction.y)
                || !Double.isFinite(direction.z) || direction.lengthSquared() < 1.0E-8D) {
            return new Vector3d(0.0D, 0.0D, 1.0D);
        }
        return new Vector3d(direction).normalize();
    }
}
