package com.kodu16.vsie.content.custom_turret;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.foundation.LoadedChunkRaycast;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Definition-driven turret using AeroIE's shared targeting, menu and binding lifecycle. */
public final class CustomTurretBlockEntity extends AbstractTurretBlockEntity {
    private static final String DEFINITION_TAG = "CustomTurretDefinition";
    private static final String LASER_DISTANCE_PREFIX = "CustomLaserDistance";
    private static final double MAX_SAMPLED_FIREPOINT_DISTANCE_SQR = 4096.0D * 4096.0D;

    private CustomTurretDefinition definition = CustomTurretDefinition.createNew("missing");
    private String definitionJson = definition.toJson();
    private final Map<Integer, Vector3d> sampledFirepoints = new HashMap<>();
    private double[] laserDistances = new double[]{0.0D};
    private Vec3 pendingVolleyTarget = Vec3.ZERO;
    private int pendingVolleyIndex;
    private int pendingVolleyDelay;
    private boolean heavyAutomatic;
    private boolean heavyFireRequested;
    private int armedChannels;

    public CustomTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CustomTurretDefinition getDefinition() {
        return definition;
    }

    public String getDefinitionTurretType() {
        return definition.turretType;
    }

    public boolean usesHeavyControlSemantics() {
        return "heavyturret".equals(definition.turretType);
    }

    public boolean supportsManualHeavyControl() {
        // Function: energy-heavy custom turrets deliberately remain automatic-only.
        return usesHeavyControlSemantics() && !isEnergyTurret();
    }

    public boolean usesAutomaticHeavyTarget(boolean hasSeatedPlayer, boolean viewLocked) {
        if (isEnergyTurret()) {
            return true;
        }
        return getData().fireType == 1 || (getData().fireType == 2 && viewLocked);
    }

    public boolean usesManualHeavyTarget(boolean hasSeatedPlayer, boolean viewLocked) {
        return supportsManualHeavyControl() && hasSeatedPlayer && !viewLocked
                && (getData().fireType == 0 || getData().fireType == 2);
    }

    public void modifyHeavyFireType(int fireType) {
        if ((isEnergyTurret() && fireType == 1) || (!isEnergyTurret() && fireType >= 0 && fireType <= 2)) {
            getData().fireType = fireType;
            markUpdated();
        }
    }

    public void modifyHeavyChannel(int channelIndex) {
        int bit = switch (channelIndex) {
            case 1 -> getData().CHANNEL_1;
            case 2 -> getData().CHANNEL_2;
            case 3 -> getData().CHANNEL_3;
            case 4 -> getData().CHANNEL_4;
            default -> 0;
        };
        if (bit != 0) {
            getData().flip(bit);
            markUpdated();
        }
    }

    public void updateHeavyControl(@Nullable SubLevel automaticTarget, @Nullable Vec3 manualTarget,
                                   int armedChannels, boolean fireRequested) {
        if (!usesHeavyControlSemantics()) {
            return;
        }
        this.armedChannels = armedChannels;
        this.heavyFireRequested = fireRequested;
        if (automaticTarget != null) {
            heavyAutomatic = true;
            aimtype = 2;
            updateenemy(new java.util.ArrayList<>(List.of(automaticTarget)));
        } else if (supportsManualHeavyControl() && manualTarget != null) {
            heavyAutomatic = false;
            this.targetPos = manualTarget;
            this.aimtype = 0;
        } else {
            heavyAutomatic = false;
            clearControlSeatTargeting();
        }
    }

    public boolean isHeavyChannelArmed() {
        return (getData().getChannelStatus() & armedChannels) != 0;
    }

    public void setDefinitionJson(String json) {
        restoreDefinitionJson(json);
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    public void setFirePoint(int index, Vector3d firepoint) {
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
    }

    public double getLaserDistance(int index) {
        return index >= 1 && index <= laserDistances.length ? laserDistances[index - 1] : 0.0D;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        if (usesHeavyControlSemantics() && !heavyAutomatic) {
            tickManualHeavyControl();
        } else {
            super.tick();
        }
        if (level == null || level.isClientSide) {
            return;
        }
        tickPendingProjectileVolley();
        if (targetDistance <= 0.0D && clearLaserDistances()) {
            sendData();
        }
    }

    private void tickManualHeavyControl() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        currentworldpos = getTurretAimOriginWorld();
        tickFireCooldown(heavyFireRequested);
        if (targetPos == null || targetPos.equals(Vec3.ZERO)) {
            returnToDefaultRotation();
        } else {
            double[] angles = computeTargetAimAngles(targetPos);
            if (angles != null) {
                targetxrot = (float) angles[0];
                targetyrot = (float) angles[1];
                xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
                yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
                if (xOK && yOK && heavyFireRequested && isHeavyChannelArmed()
                        && isFireCooldownReady() && canShootCurrentTarget() && consumeAmmoForShot()) {
                    beginProjectileVolley(targetPos);
                    consumeFireCooldown();
                    muzzleFlashTicks = 10;
                }
            }
        }
        setAnimData(XROT, xRot0);
        setAnimData(YROT, yRot0);
        markUpdated();
    }

    @Override
    public Vec3 getShootLocation(Vec3 target, List<Vector3d> previousVelocity, Level level, Vec3 origin) {
        return target;
    }

    @Override
    public String getturrettype() {
        return "custom_turret";
    }

    @Override
    public boolean isEnergyTurret() {
        return "energy".equals(definition.weaponType);
    }

    @Override
    public @Nullable Item getAmmoItem() {
        if (isEnergyTurret()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(definition.ammoItemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.get(id);
    }

    @Override
    public double getYAxisOffset() {
        return 0.0D;
    }

    @Override
    public double getcannonlength() {
        return 0.0D;
    }

    @Override
    public float getMaxSpinSpeed() {
        return definition.rotationSpeedDegreesPerTick * Mth.DEG_TO_RAD;
    }

    @Override
    public int getCoolDown() {
        return definition.fireCooldownTicks;
    }

    @Override
    public int getenergypertick() {
        return isEnergyTurret() ? definition.energyPerTick : super.getenergypertick();
    }

    @Override
    public float getLaserLayerRadius() {
        return definition.laserRadius;
    }

    @Override
    public float getLaserLayerRed(float t) {
        return (definition.laserColor >>> 16 & 255) / 255.0F;
    }

    @Override
    public float getLaserLayerGreen(float t) {
        return (definition.laserColor >>> 8 & 255) / 255.0F;
    }

    @Override
    public float getLaserLayerBlue(float t) {
        return (definition.laserColor & 255) / 255.0F;
    }

    @Override
    public float getLaserLayerAlpha(float t) {
        return (definition.laserColor >>> 24 & 255) / 255.0F;
    }

    @Override
    public boolean supportsBlockDestructionToggle() {
        return !isEnergyTurret();
    }

    @Override
    protected boolean canShootCurrentTarget() {
        // Function: heavy automatic fire remains gated by the control seat's armed channel selection.
        return pendingVolleyIndex == 0
                && (!usesHeavyControlSemantics() || !heavyAutomatic || isHeavyChannelArmed())
                && super.canShootCurrentTarget();
    }

    @Override
    public void shootentity() {
        if (isEnergyTurret()) {
            fireEnergyVolley(targetentity);
        } else {
            beginProjectileVolley(targetPos);
        }
    }

    @Override
    public void shootship() {
        if (isEnergyTurret()) {
            fireEnergyVolley(null);
        } else {
            beginProjectileVolley(targetPos);
        }
    }

    private void fireEnergyVolley(@Nullable LivingEntity entityTarget) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        ensureLaserDistanceSize();
        for (int index = 1; index <= definition.firepointCount; index++) {
            Vec3 firepoint = resolveFirepointWorld(index);
            if (firepoint == null) {
                laserDistances[index - 1] = 0.0D;
                continue;
            }
            BeamImpact impact = traceBeam(level, firepoint, targetPos, entityTarget);
            laserDistances[index - 1] = impact.position.distanceTo(firepoint);
            if (impact.entity != null) {
                // Function: each unobstructed firepoint beam contributes its own hit and damage.
                impact.entity.hurt(level.damageSources().generic(), 15.0F);
            } else if (impact.block != null && breaksBlocksEnabled()) {
                BlockPos bodyHit = resolveEnergyBodyHit(level, impact.block);
                breakTurretTargetBlockAsMined(level, bodyHit);
            }
        }
        setChanged();
        sendData();
    }

    private BeamImpact traceBeam(Level level, Vec3 from, Vec3 target, @Nullable LivingEntity entityTarget) {
        if (target == null || from.distanceToSqr(target) < 1.0E-8D) {
            return new BeamImpact(from, null, null);
        }
        BlockHitResult blockHit = LoadedChunkRaycast.clipIgnoringUnloadedChunks(
                level, from, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        Vec3 blockPosition = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : target;
        if (entityTarget != null && entityTarget.isAlive() && !entityTarget.isRemoved()) {
            Optional<Vec3> intercept = entityTarget.getBoundingBox().inflate(0.05D).clip(from, target);
            if (intercept.isPresent() && intercept.get().distanceToSqr(from) <= blockPosition.distanceToSqr(from) + 1.0E-6D) {
                return new BeamImpact(intercept.get(), entityTarget, null);
            }
        }
        return new BeamImpact(blockPosition, null,
                blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getBlockPos() : null);
    }

    private BlockPos resolveEnergyBodyHit(Level level, BlockPos worldHit) {
        if (ServerShipUtils.getSubLevelAtBlockPos(level, worldHit) != null) {
            return worldHit;
        }
        SubLevel selectedShip = getSelectedTargetShip();
        if (selectedShip == null) {
            return worldHit;
        }
        // Function: world collision points on a Sable ship map back to the ship body's stored block coordinates.
        return BlockPos.containing(selectedShip.logicalPose().transformPositionInverse(Vec3.atCenterOf(worldHit)));
    }

    private void beginProjectileVolley(Vec3 target) {
        if (target == null || target.equals(Vec3.ZERO)) {
            return;
        }
        launchProjectile(1, target);
        if (definition.firepointCount > 1) {
            pendingVolleyTarget = target;
            pendingVolleyIndex = 2;
            pendingVolleyDelay = definition.firepointIntervalTicks;
        }
    }

    private void tickPendingProjectileVolley() {
        if (isEnergyTurret() || pendingVolleyIndex == 0) {
            return;
        }
        if (--pendingVolleyDelay > 0) {
            return;
        }
        if (!consumeAmmoForShot()) {
            clearPendingVolley();
            return;
        }
        launchProjectile(pendingVolleyIndex, pendingVolleyTarget);
        pendingVolleyIndex++;
        if (pendingVolleyIndex > definition.firepointCount) {
            clearPendingVolley();
        } else {
            pendingVolleyDelay = definition.firepointIntervalTicks;
        }
    }

    private void launchProjectile(int firepointIndex, Vec3 target) {
        Level level = getLevel();
        Vec3 firepoint = resolveFirepointWorld(firepointIndex);
        if (level == null || level.isClientSide || firepoint == null) {
            return;
        }
        Vec3 direction = target.subtract(firepoint);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }
        CustomTurretProjectileEntity projectile =
                new CustomTurretProjectileEntity(vsieEntities.CUSTOM_TURRET_PROJECTILE.get(), level);
        projectile.configure(definition);
        projectile.setPos(firepoint);
        projectile.setLaunchSubLevel(ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos()));
        projectile.setPreciseLaunchVelocity(direction);
        projectile.setBreaksBlocksEnabled(breaksBlocksEnabled());
        level.addFreshEntity(projectile);
    }

    private @Nullable Vec3 resolveFirepointWorld(int index) {
        Vector3d sampled = sampledFirepoints.get(index);
        Level level = getLevel();
        if (sampled == null || level == null) {
            return null;
        }
        Vec3 local = new Vec3(sampled.x, sampled.y, sampled.z);
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        return subLevel == null ? local : subLevel.logicalPose().transformPosition(local);
    }

    private void clearPendingVolley() {
        pendingVolleyTarget = Vec3.ZERO;
        pendingVolleyIndex = 0;
        pendingVolleyDelay = 0;
    }

    private void ensureLaserDistanceSize() {
        if (laserDistances.length != definition.firepointCount) {
            laserDistances = new double[definition.firepointCount];
        }
    }

    private boolean clearLaserDistances() {
        boolean changed = false;
        for (int index = 0; index < laserDistances.length; index++) {
            if (laserDistances[index] != 0.0D) {
                laserDistances[index] = 0.0D;
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal(definition.name);
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
                // Function: embedded definitions survive chunk and Sable NBT reconstruction.
                restoreDefinitionJson(tag.getString(DEFINITION_TAG));
            } catch (IllegalArgumentException ignored) {
                // Invalid migrated data leaves the safe fallback definition loaded.
            }
        }
        ensureLaserDistanceSize();
        for (int index = 0; index < laserDistances.length; index++) {
            String key = LASER_DISTANCE_PREFIX + (index + 1);
            laserDistances[index] = tag.contains(key) ? tag.getDouble(key) : 0.0D;
        }
    }

    private void restoreDefinitionJson(String json) {
        CustomTurretDefinition parsed = CustomTurretDefinition.fromJson(json);
        definition = parsed;
        definitionJson = parsed.toJson();
        sampledFirepoints.keySet().removeIf(index -> index > definition.firepointCount);
        ensureLaserDistanceSize();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    private record BeamImpact(Vec3 position, @Nullable LivingEntity entity, @Nullable BlockPos block) {
    }
}
