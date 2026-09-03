package com.kodu16.vsie.content.aeroie_custom;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.vsieEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Definition-driven custom weapon that reuses AeroIE's common weapon GUI, ammo and channel logic. */
public final class CustomWeaponBlockEntity extends AbstractWeaponBlockEntity implements CustomDeviceBlockEntity {
    private static final String DEFINITION_TAG = "CustomWeaponDefinition";
    private static final String LASER_DISTANCE_PREFIX = "CustomWeaponLaserDistance";
    private static final double MAX_SAMPLED_FIREPOINT_DISTANCE_SQR = 4096.0D * 4096.0D;

    private CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("missing_weapon", "weapon");
    private String definitionJson = definition.toJson();
    private final Map<Integer, Vector3d> sampledFirepoints = new HashMap<>();
    private final Map<Integer, Vector3d> sampledFirepointDirections = new HashMap<>();
    private double[] laserDistances = new double[]{0.0D};
    private int pendingVolleyIndex;
    private int pendingVolleyDelay;

    public CustomWeaponBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public CustomDeviceDefinition getDefinition() {
        return definition;
    }

    @Override
    public void setDefinitionJson(String json) {
        CustomDeviceDefinition parsed = CustomDeviceDefinition.fromJson(json);
        if (!"weapon".equals(parsed.deviceType)) {
            throw new IllegalArgumentException("Custom weapon block requires a weapon definition");
        }
        definition = parsed;
        definitionJson = parsed.toJson();
        sampledFirepoints.keySet().removeIf(index -> index > definition.firepointCount);
        sampledFirepointDirections.keySet().removeIf(index -> index > definition.firepointCount);
        ensureLaserDistanceSize();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        tickPendingProjectileVolley();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // Function: validate deleted external OBJ files once when the placed custom device loads.
            CustomDeviceResourceGuard.removePlacedDeviceIfMissingObj(level, worldPosition, definition);
        }
    }

    @Override
    public float getmaxrange() {
        return 512.0F;
    }

    @Override
    public int getcooldown() {
        return definition.fireCooldownTicks;
    }

    @Override
    protected boolean isFireCooldownReady() {
        // Function: prevent the shared weapon tick from consuming a new first-shot ammo item while a firepoint volley is still running.
        return (isEnergyWeapon() || pendingVolleyIndex == 0) && super.isFireCooldownReady();
    }

    @Override
    public boolean isEnergyWeapon() {
        return "energy".equals(definition.weaponType);
    }

    @Override
    public @Nullable Item getAmmoItem() {
        if (isEnergyWeapon()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(definition.ammoItemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.get(id);
    }

    @Override
    public void fire() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (isEnergyWeapon()) {
            fireEnergyBeams(serverLevel);
        } else {
            beginProjectileVolley(serverLevel);
        }
    }

    private void fireEnergyBeams(ServerLevel serverLevel) {
        if (resolveFirepointWorld(1) == null) {
            performRaycast(serverLevel);
        } else {
            fireFromSampledFirepoints(serverLevel);
        }
        if (hasRaycastHit() && breaksBlocksEnabled()) {
            breakWeaponTargetBlockAsMined(serverLevel, getRaycastHitBlockPos());
        }
    }

    public void setFirePoint(int index, Vector3d firepoint) {
        setFirePoint(index, firepoint, new Vector3d(0.0D, 0.0D, 1.0D));
    }

    public void setFirePoint(int index, Vector3d firepoint, Vector3d direction) {
        if (index < 1 || index > definition.firepointCount || firepoint == null
                || !Double.isFinite(firepoint.x) || !Double.isFinite(firepoint.y) || !Double.isFinite(firepoint.z)) {
            return;
        }
        Vector3d blockOrigin = new Vector3d(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
        if (firepoint.distanceSquared(blockOrigin) > MAX_SAMPLED_FIREPOINT_DISTANCE_SQR) {
            return;
        }
        // Function: samples are authored in normal/sublevel block coordinates and promoted only when firing.
        sampledFirepoints.put(index, new Vector3d(firepoint));
        sampledFirepointDirections.put(index, sanitizedDirection(direction));
    }

    public double getLaserDistance(int index) {
        return index >= 1 && index <= laserDistances.length ? laserDistances[index - 1] : 0.0D;
    }

    private void fireFromSampledFirepoints(ServerLevel serverLevel) {
        ensureLaserDistanceSize();
        Vec3 direction = getFiringDirectionWorld();
        if (direction.lengthSqr() <= 1.0E-8D) {
            return;
        }
        for (int index = 1; index <= definition.firepointCount; index++) {
            Vec3 firepoint = resolveFirepointWorld(index);
            if (firepoint == null) {
                laserDistances[index - 1] = 0.0D;
                continue;
            }
            playPointFx(index, firepoint);
            performRaycastFrom(serverLevel, firepoint, direction, getmaxrange());
            laserDistances[index - 1] = getRaycastDistance();
        }
    }

    private void beginProjectileVolley(ServerLevel serverLevel) {
        if (pendingVolleyIndex != 0) {
            return;
        }
        launchProjectile(serverLevel, 1);
        if (definition.firepointCount > 1) {
            pendingVolleyIndex = 2;
            pendingVolleyDelay = definition.firepointIntervalTicks;
        }
    }

    private void tickPendingProjectileVolley() {
        if (isEnergyWeapon() || pendingVolleyIndex == 0) {
            return;
        }
        if (--pendingVolleyDelay > 0) {
            return;
        }
        if (!(getLevel() instanceof ServerLevel serverLevel) || !consumeAmmoForShot()) {
            clearPendingVolley();
            return;
        }
        launchProjectile(serverLevel, pendingVolleyIndex);
        pendingVolleyIndex++;
        if (pendingVolleyIndex > definition.firepointCount) {
            clearPendingVolley();
        } else {
            pendingVolleyDelay = definition.firepointIntervalTicks;
        }
    }

    private void launchProjectile(ServerLevel serverLevel, int firepointIndex) {
        Vec3 firepoint = resolveFirepointWorld(firepointIndex);
        if (firepoint == null) {
            firepoint = weaponpos != null ? weaponpos : Vec3.atCenterOf(getBlockPos());
        }
        Vec3 direction = getFiringDirectionWorld();
        if (direction.lengthSqr() <= 1.0E-8D) {
            return;
        }
        playPointFx(firepointIndex, firepoint);
        // Function: non-energy custom weapons fire the same definition-configured projectile as custom turrets.
        CustomTurretProjectileEntity projectile =
                new CustomTurretProjectileEntity(vsieEntities.CUSTOM_TURRET_PROJECTILE.get(), serverLevel);
        projectile.configure(definition);
        projectile.setPos(firepoint);
        projectile.setLaunchSubLevel(ServerShipUtils.getSubLevelAtBlockPos(serverLevel, getBlockPos()));
        projectile.setPreciseLaunchVelocity(direction);
        projectile.setBreaksBlocksEnabled(breaksBlocksEnabled());
        serverLevel.addFreshEntity(projectile);
    }

    private void playPointFx(int firepointIndex, Vec3 firepoint) {
        CustomDeviceDefinition.Bone bone = definition.findBone("firepoint" + firepointIndex);
        if (bone == null || !bone.pointFx.isEnabled()) {
            return;
        }
        Vector3d direction = sampledFirepointDirections.getOrDefault(firepointIndex, new Vector3d(0.0D, 0.0D, 1.0D));
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                bone.pointFx.resourceLocation(), firepoint.x, firepoint.y, firepoint.z,
                CustomFxTransform.rotationForDirection(bone.pointFx, direction),
                CustomFxTransform.scaleVector(bone.pointFx), false, true));
    }

    private void clearPendingVolley() {
        pendingVolleyIndex = 0;
        pendingVolleyDelay = 0;
    }

    private Vec3 getFiringDirectionWorld() {
        if (!getBlockState().hasProperty(CustomWeaponBlock.FACING)) {
            return Vec3.ZERO;
        }
        Vec3 localDirection = new Vec3(getBlockState().getValue(CustomWeaponBlock.FACING).step());
        Level level = getLevel();
        if (level == null) {
            return localDirection;
        }
        var subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        if (subLevel == null) {
            return localDirection;
        }
        Vector3d rotated = subLevel.logicalPose().transformNormal(
                new Vector3d(localDirection.x, localDirection.y, localDirection.z));
        return new Vec3(rotated.x, rotated.y, rotated.z);
    }

    private @Nullable Vec3 resolveFirepointWorld(int index) {
        Vector3d sampled = sampledFirepoints.get(index);
        Level level = getLevel();
        if (sampled == null || level == null) {
            return null;
        }
        Vec3 local = new Vec3(sampled.x, sampled.y, sampled.z);
        var subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        return subLevel == null ? local : subLevel.logicalPose().transformPosition(local);
    }

    private void ensureLaserDistanceSize() {
        if (laserDistances.length != definition.firepointCount) {
            laserDistances = new double[definition.firepointCount];
        }
    }

    private static Vector3d sanitizedDirection(Vector3d direction) {
        if (direction == null || !Double.isFinite(direction.x) || !Double.isFinite(direction.y)
                || !Double.isFinite(direction.z) || direction.lengthSquared() < 1.0E-8D) {
            return new Vector3d(0.0D, 0.0D, 1.0D);
        }
        return new Vector3d(direction).normalize();
    }

    @Override
    public int getControlSeatEnergyCostPerTick() {
        return isEnergyWeapon() ? definition.energyPerTick : super.getControlSeatEnergyCostPerTick();
    }

    @Override
    public Component getDisplayName() {
        // Function: custom weapons publish the authored compact HUD label to the control-seat weapon list.
        return Component.literal(definition.hudShortName.isBlank() ? definition.name : definition.hudShortName);
    }

    @Override
    public String getweapontype() {
        return "custom_weapon";
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString(DEFINITION_TAG, definitionJson);
        ensureLaserDistanceSize();
        for (int index = 0; index < laserDistances.length; index++) {
            tag.putDouble(LASER_DISTANCE_PREFIX + (index + 1), laserDistances[index]);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(DEFINITION_TAG)) {
            try {
                setDefinitionJson(tag.getString(DEFINITION_TAG));
            } catch (IllegalArgumentException ignored) {
                // Invalid migrated data leaves the safe fallback custom weapon definition loaded.
            }
        }
        ensureLaserDistanceSize();
        for (int index = 0; index < laserDistances.length; index++) {
            String key = LASER_DISTANCE_PREFIX + (index + 1);
            laserDistances[index] = tag.contains(key) ? tag.getDouble(key) : 0.0D;
        }
    }
}
