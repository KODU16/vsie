package com.kodu16.vsie.content.controlseat.block;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;
import com.kodu16.vsie.content.controlseat.Initialize;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.controlseat.functions.ScanNearByShips;
import com.kodu16.vsie.content.controlseat.functions.ShieldHandler;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.controlseat.server.ServerShipHandler;
import com.kodu16.vsie.content.controlseat.client.Input.ClientMouseHandler;

import com.kodu16.vsie.content.controlseat.server.SeatRegistry;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.network.fx.FxEntityS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.registries.vsieSounds;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.kodu16.vsie.content.custom_turret.CustomTurretBlockEntity;
import com.kodu16.vsie.content.shield.ShieldGeneratorBlockEntity;
import com.kodu16.vsie.content.shield.ShieldInterception;
import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.storage.energybattery.AbstractEnergyBatteryBlockEntity;
import com.kodu16.vsie.content.storage.fueltank.AbstractFuelTankBlockEntity;
import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.electro_magnet_rail_accelerator.ElectromagnetRailAcceleratorBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotCoreBlockEntity;
import com.kodu16.vsie.network.fuel.FluidThrusterProperties;
import com.kodu16.vsie.registries.fuel.ThrusterFuelManager;
import com.kodu16.vsie.registries.vsieFluids;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.slf4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import net.neoforged.neoforge.items.ItemStackHandler;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.ArrayList;
import java.util.List;

public class ControlSeatBlockEntity extends AbstractControlSeatBlockEntity
        implements BlockEntitySubLevelActor, IHaveGoggleInformation, GeoBlockEntity {
    private static final ResourceLocation SHIELD_OPEN_FX = ResourceLocation.fromNamespaceAndPath("vsie", "shield_open");
    private static final ResourceLocation SHIELD_HIT_FX = ResourceLocation.fromNamespaceAndPath("vsie", "shield_hit");
    private static final ResourceLocation RAIL_ACCELERATION_FX = ResourceLocation.fromNamespaceAndPath("vsie", "cenix_plasma_bullet");
    private static final int RAIL_ACCELERATION_FX_REFRESH_TICKS = 20;
    // Cross-dimension Sable reconstruction can outlive the client-ready barrier; retain the mount for that window.
    private static final int OCCUPANT_RESTORE_GRACE_TICKS = 440;
    private static final float SHIELD_OPEN_DEFAULT_RADIUS = 8.0F;
    private static final String ANTI_GRAVITY_IDLE_THROTTLE_TAG = "AntiGravityIdleThrottle";
    private static final String WARP_FRAME_DIMENSION_TAG = "WarpFrameDimension";
    // Function: preserve the softer one-shot boost transient while moving its trigger to the control seat.
    private static final float THRUSTER_BOOST_VOLUME_SCALE = 0.6F;
    //private final ControlSeatServerData serverData = new ControlSeatServerData();
    public volatile boolean ride = false;
    private boolean hasInitialized = false;
    private boolean shieldOpenFxPlayed = false;
    private int railAccelerationFxEntityId = -1;
    private int railAccelerationFxRefreshTicks = 0;
    private boolean railAccelerationTrailOverrideActive = false;
    private boolean hasThrusterFuelThisTick = false;
    private int occupantRestoreGraceTicks = 0;
    private String restoredWarpFrameDimension = "";
    private boolean warpFrameValidationPending = false;
    public boolean previousfirestatus = false;
    private HolderLookup.Provider nbtRegistries;
    private Vector3d currentworldpos = new Vector3d();
    private List<ControlSeatMountEntity> seats = new ArrayList<>();
    private final ServerShipHandler serverShipHandler;
    private final AnimatableInstanceCache animationCache = new SingletonAnimatableInstanceCache(this);

    public float calculatedstrength = 0;
    public int energyspendpertick = 10;
    public int capacitorenergy = 0;
    public int totalenergy = 100;
    public int totalenergyavalible = 0;
    public boolean linkedBatteryPowerAvailableThisTick = false;
    public int fuelspendcurrenttick = 0;
    public int capacitorfuel = 0;
    public int totalfuel = 100;
    public int totalfuelavalible = 0;
    public double avalibleshield = 0;

    public SmartFluidTankBehaviour tank;


    private final ItemStackHandler warpChipInventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {

            return stack.is(vsieItems.WARP_DATA_CHIP.get());
        }
    };

    public ItemStackHandler getWarpChipInventory() {
        return warpChipInventory;
    }

    public ControlSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.serverShipHandler = new ServerShipHandler(controlseatData);
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        if (!validateSingleControlSeatForSubLevel(subLevel)) {
            return;
        }
        reconcileWarpFrameAfterDimensionChange();
        controlseatData.serverShip = subLevel;
        controlseatData.level = level;
        serverShipHandler.getandsendshipdata(subLevel, getBlockPos());
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (!validateSingleControlSeatForSubLevel(subLevel)) {
            return;
        }
        controlseatData.serverShip = subLevel;
        controlseatData.level = level;
        serverShipHandler.applyForceAndTorque(subLevel, getBlockPos(), timeStep);
    }

    @Override
    public boolean supportsLinkedPeripheralType(int type) {
        return type >= 0 && type <= 7;
    }

    @Override
    public void setAlly(String str) {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            serverSubLevel.setName(processSlug(serverSubLevel.getName(), str));
        }
        super.setAlly(str);
    }

    private static String processSlug(String currentName, String ally) {
        if (currentName != null && currentName.matches("^\\[.*?\\].*")) {
            int endIndex = currentName.indexOf(']');
            if (endIndex != -1) {
                return "[" + ally + "]" + currentName.substring(endIndex + 1);
            }
        }
        return "[" + ally + "]" + currentName;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }



    public void clientTick() {
        traceLinkedPeripheralResolutionIfChanged();
        ClientTicker.tick(this);
    }

    // Keep client input types out of the block entity class loaded by dedicated servers.
    private static final class ClientTicker {
        private static void tick(ControlSeatBlockEntity controlSeat) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (!(player != null && player.getVehicle() instanceof ControlSeatMountEntity mount)) {
                return;
            }
            BlockPos pos = controlSeat.getBlockPos();
            if (!pos.equals(mount.getBoundBlockPos())) {
                return;
            }
            ClientMouseHandler.handle(player, pos);
        }
    }

    private HolderLookup.Provider currentNbtRegistries() {
        return nbtRegistries != null ? nbtRegistries : this.level.registryAccess();
    }

    private void withNbtRegistries(HolderLookup.Provider registries, Runnable action) {
        HolderLookup.Provider previous = this.nbtRegistries;
        this.nbtRegistries = registries;
        try {
            action.run();
        } finally {
            this.nbtRegistries = previous;
        }
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        tag.put("WarpChipInventory", warpChipInventory.serializeNBT(registries));

        tag.putInt("WarpTargetX", controlseatData.warpTargetPos.getX());
        tag.putInt("WarpTargetY", controlseatData.warpTargetPos.getY());
        tag.putInt("WarpTargetZ", controlseatData.warpTargetPos.getZ());
        tag.putString("WarpTargetDimension", controlseatData.warpTargetDimension);
        tag.putString("WarpTargetName", controlseatData.warpTargetName);

        tag.putBoolean("IsWarpPreparing", controlseatData.isWarpPreparing);
        if (!clientPacket && level != null) {
            // Persist the coordinate frame so reconstructed seats cannot resume old-world warp work.
            tag.putString(WARP_FRAME_DIMENSION_TAG, level.dimension().location().toString());
        }
        // Function: persisted warp preparation must keep the original start-to-target aim vector after a world reload.
        tag.putBoolean("HasWarpStartSnapshot", controlseatData.hasWarpStartSnapshot);
        tag.putDouble("WarpStartWorldX", controlseatData.warpStartSubLevelWorldPos.x);
        tag.putDouble("WarpStartWorldY", controlseatData.warpStartSubLevelWorldPos.y);
        tag.putDouble("WarpStartWorldZ", controlseatData.warpStartSubLevelWorldPos.z);
        tag.putDouble("WarpLaunchDirectionX", controlseatData.warpLaunchDirection.x);
        tag.putDouble("WarpLaunchDirectionY", controlseatData.warpLaunchDirection.y);
        tag.putDouble("WarpLaunchDirectionZ", controlseatData.warpLaunchDirection.z);

        tag.putBoolean("IsViewLocked", controlseatData.isviewlocked);
        // Operator-selected flight state must survive Sable's block-entity recreation.
        tag.putInt("Throttle", controlseatData.getThrottle());
        tag.putBoolean("IsFlightAssistOn", controlseatData.isflightassiston);
        tag.putBoolean("IsForceAssistOn", controlseatData.isforceassiston);
        tag.putBoolean("IsTorqueAssistOn", controlseatData.istorqueassiston);
        tag.putBoolean("IsAntiGravityOn", controlseatData.isantigravityon);
        tag.putBoolean("IsShieldOn", controlseatData.isshieldon);
        tag.putInt("LockedEnemyIndex", controlseatData.lockedenemyindex);
        tag.putBoolean("WasSeatOccupied", ride || controlseatData.getPlayer() != null);
        // Function: auto-level is a ship mode like anti-gravity and should survive block reloads.
        tag.putBoolean("IsAutoLevelOn", controlseatData.isAutoLevelOn);
        // Function: idle anti-gravity learns a trim value and should resume from the last stable world load.
        tag.putDouble(ANTI_GRAVITY_IDLE_THROTTLE_TAG, controlseatData.antiGravityIdleThrottle);
        controlseatData.refreshWeaponChannelEncode();
        // Function: weapon channel toggles must survive world reloads, not just the live S2C HUD sync.
        tag.putInt("WeaponChannelEncode", controlseatData.channelencode);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("WarpChipInventory")) {

            warpChipInventory.deserializeNBT(registries, tag.getCompound("WarpChipInventory"));
        }

        controlseatData.warpTargetPos = new BlockPos(tag.getInt("WarpTargetX"), tag.getInt("WarpTargetY"), tag.getInt("WarpTargetZ"));
        controlseatData.warpTargetDimension = tag.getString("WarpTargetDimension");
        controlseatData.warpTargetName = tag.getString("WarpTargetName");
        controlseatData.isWarpPreparing = tag.getBoolean("IsWarpPreparing");
        if (!clientPacket) {
            restoredWarpFrameDimension = tag.getString(WARP_FRAME_DIMENSION_TAG);
            warpFrameValidationPending = !restoredWarpFrameDimension.isEmpty();
        }
        if (tag.contains("HasWarpStartSnapshot")) {
            controlseatData.hasWarpStartSnapshot = tag.getBoolean("HasWarpStartSnapshot");
            controlseatData.warpStartSubLevelWorldPos.set(
                    tag.getDouble("WarpStartWorldX"),
                    tag.getDouble("WarpStartWorldY"),
                    tag.getDouble("WarpStartWorldZ")
            );
            controlseatData.warpLaunchDirection.set(
                    tag.getDouble("WarpLaunchDirectionX"),
                    tag.getDouble("WarpLaunchDirectionY"),
                    tag.getDouble("WarpLaunchDirectionZ")
            );
        } else {
            controlseatData.clearWarpAlignmentSnapshot();
        }
        controlseatData.isviewlocked = tag.getBoolean("IsViewLocked");
        if (tag.contains("Throttle")) {
            controlseatData.setThrottle(tag.getInt("Throttle"));
        }
        if (tag.contains("IsFlightAssistOn")) {
            controlseatData.isflightassiston = tag.getBoolean("IsFlightAssistOn");
        }
        if (tag.contains("IsForceAssistOn")) {
            controlseatData.isforceassiston = tag.getBoolean("IsForceAssistOn");
        }
        if (tag.contains("IsTorqueAssistOn")) {
            controlseatData.istorqueassiston = tag.getBoolean("IsTorqueAssistOn");
        }
        if (tag.contains("IsAntiGravityOn")) {
            controlseatData.isantigravityon = tag.getBoolean("IsAntiGravityOn");
        }
        if (tag.contains("IsShieldOn")) {
            controlseatData.isshieldon = tag.getBoolean("IsShieldOn");
        }
        if (tag.contains("LockedEnemyIndex")) {
            controlseatData.lockedenemyindex = Math.max(0, tag.getInt("LockedEnemyIndex"));
        }
        if (!clientPacket) {
            occupantRestoreGraceTicks = tag.getBoolean("WasSeatOccupied") ? OCCUPANT_RESTORE_GRACE_TICKS : 0;
        }
        controlseatData.isAutoLevelOn = tag.getBoolean("IsAutoLevelOn");
        if (tag.contains(ANTI_GRAVITY_IDLE_THROTTLE_TAG)) {
            controlseatData.antiGravityIdleThrottle = Mth.clamp(tag.getDouble(ANTI_GRAVITY_IDLE_THROTTLE_TAG), 0.0D, 2.0D);
        } else {
            controlseatData.antiGravityIdleThrottle = 1.0D;
        }
        if (tag.contains("WeaponChannelEncode")) {
            controlseatData.setWeaponChannelEncode(tag.getInt("WeaponChannelEncode"));
        } else {
            controlseatData.refreshWeaponChannelEncode();
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        withNbtRegistries(registries, () -> read(tag, registries, true));
    }

    public void tick() {
        Logger LOGGER = LogUtils.getLogger();
        traceLinkedPeripheralResolutionIfChanged();
        if (level.isClientSide)
            return;
        reconcileWarpFrameAfterDimensionChange();
        if (!validateSingleControlSeatForSubLevel()) {
            return;
        }
        // Function: expose a destination mount immediately so a dimension-transfer handler can reattach the pilot.
        ensureOccupantRestoreMount();
        if (hasInitialized) {
            refreshSeatOccupancyFromWorld();

            controlseatData.controlSeatPos = getBlockPos();

            //update
            if (!ride) {
                if (isAwaitingOccupantRestore()) {
                    occupantRestoreGraceTicks--;
                } else {
                    removeUnoccupiedSeatMounts();
                    controlseatData.clearSeatOccupantState();
                    // Function: empty seat clears player input but leaves assist damping available for drift suppression.
                    serverShipHandler.clearManualControlInput();
                    controlseatData.setPlayer(null);
                }
            }
            this.calculatedstrength = 0;
            this.energyspendpertick = 0;
            this.fuelspendcurrenttick = 0;

            this.totalenergy =100;
            this.totalenergyavalible = 0;
            this.totalfuel = 0;
            this.totalfuelavalible = 0;
            this.capacitorenergy = 0;

            updateEnergy();
            this.linkedBatteryPowerAvailableThisTick = this.totalenergyavalible > 0;
            if (this.linkedBatteryPowerAvailableThisTick) {
                updateThruster();
                updateWeapon();
                updateTurret();
                updateShield();
                this.capacitorenergy = -this.energyspendpertick;
                this.totalenergy =100;
                this.totalenergyavalible = 0;
                updateEnergy();
                if(this.capacitorenergy < 0) {
                    this.capacitorenergy = 0;
                    disableLinkedPeripheralsForNoPower();
                }
            } else {
                // Function: no battery power disables only powered peripherals; resource and HUD updates still continue.
                disableLinkedPeripheralsForNoPower();
            }

            this.capacitorfuel = -this.fuelspendcurrenttick;
            updateFuel();
            updateScreen();

            this.capacitorenergy = 0;

            // Function: missing thruster fuel is a flight-only failure, so shields and screens keep ticking.
            if(this.fuelspendcurrenttick > 0 && (this.capacitorfuel < 0 || !this.hasThrusterFuelThisTick)) {
                this.capacitorfuel = 0;
                disableThrusterOutput();
            }
        }
        else {
            BlockPos pos = getBlockPos();
            BlockState state = null;
            if (level != null) {
                state = level.getBlockState(pos);
            }
            if (state != null) {
                Initialize.initialize(level, pos, state);
                hasInitialized = true;
            }
        }


        if(this.linkedBatteryPowerAvailableThisTick && controlseatData.isshieldon) {
            boolean shieldOverloaded = controlseatData.shieldcooldowntime > 0.0D;
            if (shieldOverloaded) {
                // Cooldown means the shield stays armed in HUD but remains physically closed and cannot regenerate yet.
                controlseatData.shieldcooldowntime = Math.max(0.0D, controlseatData.shieldcooldowntime - 1.0D);
                shieldOpenFxPlayed = false;
                updateShieldEnergyAvalible();
            } else {
                updateShieldEnergyAvalible();
                if (!shieldOpenFxPlayed) {
                    SubLevel shieldFxSublevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
                    Vec3 shieldFxCenter = shieldFxSublevel == null ? null : ServerShipUtils.getStructureCenterWorld(shieldFxSublevel);
                    if (shieldFxCenter != null && !linkedShields.isEmpty() && controlseatData.shieldradius > 0.0D) {
                        shieldOpenFxPlayed = playShieldOpenFx(shieldFxSublevel, shieldFxCenter);
                    }
                }
                SubLevel sublevel = ServerShipUtils.getSubLevelAtBlockPos(level,this.getBlockPos());
                if (sublevel == null) {
                    return;
                }
                Vec3 center = ServerShipUtils.getStructureCenterWorld(sublevel);
                if (center == null || linkedShields.isEmpty() || controlseatData.shieldradius <= 0.0D) {
                    return;
                }
                if (!shieldOpenFxPlayed) {
                    shieldOpenFxPlayed = playShieldOpenFx(sublevel, center);
                }
                AABB searchBox = ShieldInterception.searchBox(center, controlseatData.shieldradius);
                Vec3 finalCenter = center;
                int shieldCost = Math.max(0, (int) Math.ceil(controlseatData.shieldcostperprojectile));
                final int[] remainingShieldEnergy = {(int) Math.max(0.0D, controlseatData.avalibleshield)};
                final boolean[] overloadTriggered = {false};
                level.getEntitiesOfClass(Entity.class, searchBox, ShieldInterception::isCandidate).forEach(entity -> {

                    if (overloadTriggered[0]) return;
                    ShieldInterception.Hit shieldHit = ShieldInterception.findHit(entity, finalCenter, controlseatData.shieldradius);
                    if (shieldHit == null) return;

                    entity.discard();
                    Vec3 hitDir = shieldHit.normal();
                    Vec3 hitPoint = shieldHit.point();
                    playShieldHitFx(hitPoint, hitDir);

                    level.playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                            SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS,
                            1.0f, 1.2f + level.random.nextFloat() * 0.4f);

                    if (remainingShieldEnergy[0] >= shieldCost) {
                        SubtractShieldEnergy(shieldCost);
                        remainingShieldEnergy[0] = Math.max(0, remainingShieldEnergy[0] - shieldCost);
                    } else {
                        overloadTriggered[0] = true;
                        overloadShieldAfterIntercept();
                    }
                });
                if (!overloadTriggered[0]) {
                    RegenerateShieldEnergy((int) controlseatData.shieldregeneratepertick);
                }
            }
        }
        else {
            shieldOpenFxPlayed = false;
        }

    }


    public void updateEnergy() {
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractEnergyBatteryBlockEntity battery) {
                confirmLinkedPeripheralPresent(pos, 4);
                int energy = battery.getEnergy().getEnergyStored();
                if(energy>=-this.capacitorenergy) {
                    battery.getEnergyStorage().extractEnergy(-this.capacitorenergy,false);
                    this.capacitorenergy = 0;
                }
                else {
                    battery.getEnergyStorage().extractEnergy(energy,false);
                    this.capacitorenergy += energy;
                }
                totalenergy += battery.getEnergy().getMaxEnergyStored();
                totalenergyavalible += battery.getEnergy().getEnergyStored();
            } else {

                toRemove.add(pos);
            }
        }, 4);
        controlseatData.totalenergystorage = totalenergy;
        controlseatData.avalibleenergy = totalenergyavalible;
        //LogUtils.getLogger().warn("detected total energy:"+controlseatData.totalenergystorage+"avalible:"+controlseatData.avalibleenergy);

        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 4);
        }
    }

    public void updateThruster() {
        List<Vec3> toRemove = new ArrayList<>();

        float[] facingMaxThrustSum = new float[6];
        double[] forceStrengthSum = new double[1];
        double[] torqueStrengthSum = new double[1];

        List<AbstractThrusterBlockEntity> activeThrusters = new ArrayList<>();
        List<BlockPos> activeThrusterPositions = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractThrusterBlockEntity thruster) {
                confirmLinkedPeripheralPresent(pos, 0);
                Logger LOGGER = LogUtils.getLogger();
                this.energyspendpertick += thruster.getControlSeatEnergyCostPerTick();
                //LOGGER.warn("writing to thrusters:" +blockPos+ "torque:"+controlseatData.getFinaltorque()+"force:"+controlseatData.getThrusterVisualForce());
                // Function: control authority uses the player's per-thruster limits, not raw max thrust.
                double forceCoefficient = thruster.getForceCoefficient();
                double torqueCoefficient = thruster.getTorqueCoefficient();
                forceStrengthSum[0] += forceCoefficient;
                torqueStrengthSum[0] += torqueCoefficient;

                Direction thrusterFacing = thruster.getBlockState().getValue(BlockStateProperties.FACING);
                int facingIndex = getFacingThrustIndex(thrusterFacing);
                if (facingIndex >= 0) {
                    facingMaxThrustSum[facingIndex] += (float) forceCoefficient;
                }

                activeThrusters.add(thruster);
                activeThrusterPositions.add(blockPos.immutable());
                thruster.setRailAccelerationTrailOverride(railAccelerationTrailOverrideActive);
                this.fuelspendcurrenttick += thruster.fuelconsumptionperthrottle()*thruster.getFuelThrottle();
            } else {

                toRemove.add(pos);
            }
        }, 0);

        for (AbstractThrusterBlockEntity thruster : activeThrusters) {
            Direction thrusterFacing = thruster.getBlockState().getValue(BlockStateProperties.FACING);
            int facingIndex = getFacingThrustIndex(thrusterFacing);
            double sameFacingSum = facingIndex >= 0 ? facingMaxThrustSum[facingIndex] : thruster.getForceCoefficient();
            thruster.setdata(controlseatData.getFinaltorque(), controlseatData.getThrusterVisualForce(), sameFacingSum);
        }
        this.calculatedstrength = (float) forceStrengthSum[0];
        controlseatData.thruster_strength = this.calculatedstrength;
        controlseatData.thruster_force_strength = (float) forceStrengthSum[0];
        controlseatData.thruster_torque_strength = (float) torqueStrengthSum[0];

        controlseatData.facingMaxThrustSum = facingMaxThrustSum;
        // Function: ship trail emitters use the current linked thruster block positions in server ship handling.
        controlseatData.thrusterpositionslist = activeThrusterPositions;

        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 0);
        }
    }


    private int getFacingThrustIndex(Direction direction) {
        return switch (direction) {
            case EAST -> 0;
            case SOUTH -> 1;
            case WEST -> 2;
            case NORTH -> 3;
            case UP -> 4;
            case DOWN -> 5;
        };
    }

    public void updateWeapon() {


        previousfirestatus = controlseatData.isfiring;

        int activeSeatChannelEncode = 0;
        if (controlseatData.getChannel1()) activeSeatChannelEncode |= 1;
        if (controlseatData.getChannel2()) activeSeatChannelEncode |= 2;
        if (controlseatData.getChannel3()) activeSeatChannelEncode |= 4;
        if (controlseatData.getChannel4()) activeSeatChannelEncode |= 8;

        List<ActiveWeaponHudInfo> activeWeaponHudInfos = new ArrayList<>();
        List<Vec3> toRemove = new ArrayList<>();
        // Function: remember whether any active rail accelerator is currently forcing counter-force assist off.
        final boolean[] forceAssistSuppressedByAccelerator = {false};
        final boolean[] railAccelerationActive = {false};
        int finalActiveSeatChannelEncode = activeSeatChannelEncode;
        SubLevel lockedEnemySubLevel = resolveLockedEnemySubLevel();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractWeaponBlockEntity weapon) {
                confirmLinkedPeripheralPresent(pos, 1);
                this.energyspendpertick += weapon.getControlSeatEnergyCostPerTick();
                // Function: sync the currently locked enemy sublevel to linked weapons before firing.
                weapon.receivetarget(lockedEnemySubLevel);
                if (weapon instanceof VerticleLaunchingSlotCoreBlockEntity verticalLaunchCore) {
                    // Function: VLS cap animation follows armed channels even when the fire key is not held.
                    verticalLaunchCore.receiveArmedChannels(finalActiveSeatChannelEncode);
                }

                boolean activeForSeat = isWeaponInAnyActiveChannel(weapon, finalActiveSeatChannelEncode);
                if (activeForSeat) {

                    activeWeaponHudInfos.add(new ActiveWeaponHudInfo(
                            weapon.getDisplayName().getString(),
                            weapon.getCooldownHudValue(),
                            weapon.getCooldownHudMax(),
                            weapon.isCooldownHudRemaining(),
                            weapon.isHudFireReady()
                    ));
                }


                if (controlseatData.isfiring) {
                    weapon.receivechannel(finalActiveSeatChannelEncode);
                } else {
                    weapon.receivechannel(0);
                }
                if (controlseatData.isfiring
                        && activeForSeat
                        && weapon instanceof ElectromagnetRailAcceleratorBlockEntity accelerator
                        && accelerator.shouldSuppressForceAssist()) {
                    forceAssistSuppressedByAccelerator[0] = true;
                }
                if (controlseatData.isfiring
                        && activeForSeat
                        && weapon instanceof ElectromagnetRailAcceleratorBlockEntity accelerator
                        && accelerator.wasAcceleratingRecently()) {
                    railAccelerationActive[0] = true;
                }
            } else {

                toRemove.add(pos);
            }
        }, 1);


        controlseatData.activeWeaponHudInfos = activeWeaponHudInfos;
        // Function: rail acceleration overrides player preference and forces counter-force assist off while active.
        controlseatData.isForceAssistSuppressedByAccelerator = forceAssistSuppressedByAccelerator[0];
        if (forceAssistSuppressedByAccelerator[0]) {
            controlseatData.isforceassiston = false;
        }
        syncRailAccelerationFx(railAccelerationActive[0]);


        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 1);
        }
    }

    private void reconcileWarpFrameAfterDimensionChange() {
        if (!warpFrameValidationPending || level == null || level.isClientSide) {
            return;
        }
        String currentDimension = level.dimension().location().toString();
        warpFrameValidationPending = false;
        if (restoredWarpFrameDimension.equals(currentDimension)) {
            return;
        }

        boolean wasPreparing = controlseatData.isWarpPreparing;
        boolean hadPendingTeleport = controlseatData.hasPendingWarpTeleport;
        boolean hadStartSnapshot = controlseatData.hasWarpStartSnapshot;
        controlseatData.cancelWarpExecutionForDimensionChange();
        setChanged();
        LogUtils.getLogger().info(
                "[VSIE-WARP-TRANSFER] phase=TRANSIENT_STATE_CANCELLED seat={} sourceDimension={} destinationDimension={} "
                        + "wasPreparing={} hadPendingTeleport={} hadStartSnapshot={} persistentTargetPreserved={}",
                getBlockPos(), restoredWarpFrameDimension, currentDimension,
                wasPreparing, hadPendingTeleport, hadStartSnapshot,
                controlseatData.warpTargetPos != null && !controlseatData.warpTargetPos.equals(BlockPos.ZERO)
        );
        restoredWarpFrameDimension = currentDimension;
    }

    // Function: attach the rail glow to the accelerated ship's occupied control-seat entity for exactly the active interval.
    private void syncRailAccelerationFx(boolean accelerating) {
        if (level == null || level.isClientSide()) {
            return;
        }
        setRailAccelerationTrailOverrideActive(accelerating);

        Player player = controlseatData.getPlayer();
        ControlSeatMountEntity mount = player != null && player.getVehicle() instanceof ControlSeatMountEntity seat
                && seat.getBoundBlockPos().equals(getBlockPos()) ? seat : null;
        int currentEntityId = mount == null ? -1 : mount.getId();

        if (!accelerating || currentEntityId < 0) {
            stopRailAccelerationFx();
            return;
        }

        if (railAccelerationFxEntityId != currentEntityId) {
            stopRailAccelerationFx();
            railAccelerationFxEntityId = currentEntityId;
            railAccelerationFxRefreshTicks = 0;
            ModNetworking.sendToAll(new FxEntityS2CPacket(RAIL_ACCELERATION_FX, currentEntityId, false));
            return;
        }

        railAccelerationFxRefreshTicks++;
        if (railAccelerationFxRefreshTicks >= RAIL_ACCELERATION_FX_REFRESH_TICKS) {
            railAccelerationFxRefreshTicks = 0;
            // Function: refresh packets let clients that begin tracking mid-acceleration acquire the persistent effect.
            ModNetworking.sendToAll(new FxEntityS2CPacket(RAIL_ACCELERATION_FX, currentEntityId, false));
        }
    }

    // Function: propagate rail-acceleration trail state immediately to every linked thruster on state edges.
    private void setRailAccelerationTrailOverrideActive(boolean active) {
        if (railAccelerationTrailOverrideActive == active) {
            return;
        }
        railAccelerationTrailOverrideActive = active;
        this.forEachLinkedPeripheral(pos -> {
            BlockEntity blockEntity = level.getBlockEntity(BlockPos.containing(pos));
            if (blockEntity instanceof AbstractThrusterBlockEntity thruster) {
                thruster.setRailAccelerationTrailOverride(active);
            }
        }, 0);
    }

    private void stopRailAccelerationFx() {
        if (railAccelerationFxEntityId >= 0) {
            ModNetworking.sendToAll(FxEntityS2CPacket.stop(RAIL_ACCELERATION_FX, railAccelerationFxEntityId));
        }
        railAccelerationFxEntityId = -1;
        railAccelerationFxRefreshTicks = 0;
    }


    private boolean isWeaponInAnyActiveChannel(AbstractWeaponBlockEntity weapon, int activeSeatChannelEncode) {
        if (activeSeatChannelEncode == 0) {
            return false;
        }
        int weaponChannelEncode = 0;
        if (weapon.getData().getChannel1()) weaponChannelEncode |= 1;
        if (weapon.getData().getChannel2()) weaponChannelEncode |= 2;
        if (weapon.getData().getChannel3()) weaponChannelEncode |= 4;
        if (weapon.getData().getChannel4()) weaponChannelEncode |= 8;
        return (weaponChannelEncode & activeSeatChannelEncode) != 0;
    }

    private SubLevel resolveLockedEnemySubLevel() {
        // Function: resolve once per tick so every linked weapon receives the same locked target.
        SubLevel lockedEnemySubLevel = ScanNearByShips.scanEnemySubLevelByIndex(
                null,
                this.getBlockPos(),
                level,
                controlseatData.enemy,
                controlseatData.ally,
                controlseatData.lockedenemyindex
        );
        controlseatData.lockedEnemySubLevel = lockedEnemySubLevel;
        return lockedEnemySubLevel;
    }

    public void updateShield() {
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof ShieldGeneratorBlockEntity shield) {
                confirmLinkedPeripheralPresent(pos, 2);
                Logger LOGGER = LogUtils.getLogger();
            } else {

                toRemove.add(pos);
            }
        }, 2);

        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 2);
        }
        if (linkedShields.isEmpty()) {
            resetShieldStats();
            return;
        }
        double[] minmax = ShieldHandler.getMinMaxDistance(linkedShields);
        // ShieldHandler returns [min, max], so unpack in the same order here.
        double min = minmax[0];
        double max = minmax[1];
        if (max <= 0.0D || min <= 0.0D) {
            resetShieldStats();
            return;
        }

        controlseatData.shieldmax = max;
        controlseatData.shieldmin = min;
        controlseatData.shieldradius = 0.75*max;
        controlseatData.totalshield = 100000 * linkedShields.size();
        controlseatData.shieldcostperprojectile = ((max*(max/min)*linkedShields.size()))*100;
        controlseatData.shieldregeneratepertick = ((max*linkedShields.size()))*50;
        controlseatData.shieldmaxcooldowntime = (max/min)*50;
    }

    private void resetShieldStats() {
        avalibleshield = 0;
        controlseatData.avalibleshield = 0;
        controlseatData.totalshield = 0;
        controlseatData.shieldradius = 0;
        controlseatData.shieldcostperprojectile = 0;
        controlseatData.shieldregeneratepertick = 0;
        controlseatData.shieldmaxcooldowntime = 0;
        controlseatData.shieldcooldowntime = 0;
        controlseatData.shieldmin = 0;
        controlseatData.shieldmax = 0;
    }

    public void updateShieldEnergyAvalible() {
        if (linkedShields.isEmpty()) {
            avalibleshield = 0;
            controlseatData.avalibleshield = 0;
            return;
        }
        avalibleshield = 0;
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof ShieldGeneratorBlockEntity shield) {
                confirmLinkedPeripheralPresent(pos, 2);
                Logger LOGGER = LogUtils.getLogger();
                avalibleshield += shield.getEnergy().getEnergyStored();
                shield.maxreceiverate = (int) (controlseatData.shieldregeneratepertick/linkedShields.size())+10;
            }
        }, 2);
        controlseatData.avalibleshield = avalibleshield;
    }

    public void SubtractShieldEnergy(int energy) {
        if (energy <= 0 || linkedShields.isEmpty()) {
            return;
        }
        int eachsubtract = energy/linkedShields.size();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof ShieldGeneratorBlockEntity shield) {
                confirmLinkedPeripheralPresent(pos, 2);
                Logger LOGGER = LogUtils.getLogger();
                shield.getEnergy().extractEnergy(eachsubtract,false);
            }
        }, 2);
    }

    private void overloadShieldAfterIntercept() {
        drainAllLinkedShieldEnergy();
        controlseatData.avalibleshield = 0;
        avalibleshield = 0;
        controlseatData.shieldcooldowntime = Math.max(1.0D, controlseatData.shieldmaxcooldowntime);
        shieldOpenFxPlayed = false;
        setChanged();
    }

    private void drainAllLinkedShieldEnergy() {
        if (linkedShields.isEmpty()) {
            return;
        }
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof ShieldGeneratorBlockEntity shield) {
                confirmLinkedPeripheralPresent(pos, 2);
                int stored = shield.getEnergy().getEnergyStored();
                if (stored > 0) {
                    shield.getEnergy().extractEnergy(stored, false);
                    shield.setChanged();
                }
            }
        }, 2);
    }

    public void RegenerateShieldEnergy(int energy) {
        if (energy <= 0 || linkedShields.isEmpty()) {
            return;
        }
        int acceptedByShields = simulateLinkedShieldReceive(energy);
        if (acceptedByShields <= 0) {
            return;
        }

        int drainedFromBatteries = drainLinkedBatteriesForShield(acceptedByShields);
        if (drainedFromBatteries <= 0) {
            return;
        }

        int chargedToShields = chargeLinkedShields(drainedFromBatteries);
        int unusedEnergy = drainedFromBatteries - chargedToShields;
        if (unusedEnergy > 0) {
            refundLinkedBatteriesFromShield(unusedEnergy);
        }

        int consumedEnergy = drainedFromBatteries - unusedEnergy;
        if (consumedEnergy > 0) {
            // Shield regeneration is a real FE cost, so update the HUD-side battery cache this tick.
            totalenergyavalible = Math.max(0, totalenergyavalible - consumedEnergy);
            controlseatData.avalibleenergy = Math.max(0, controlseatData.avalibleenergy - consumedEnergy);
            updateShieldEnergyAvalible();
            setChanged();
        }
    }

    private int simulateLinkedShieldReceive(int energy) {
        int[] remaining = {energy};
        int[] accepted = {0};
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            if (remaining[0] <= 0) {
                return;
            }
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof ShieldGeneratorBlockEntity shield) {
                confirmLinkedPeripheralPresent(pos, 2);
                int received = shield.getEnergy().receiveEnergy(remaining[0], true);
                remaining[0] -= received;
                accepted[0] += received;
            } else {
                toRemove.add(pos);
            }
        }, 2);
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 2);
        }
        return accepted[0];
    }

    private int chargeLinkedShields(int energy) {
        int[] remaining = {energy};
        int[] charged = {0};
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            if (remaining[0] <= 0) {
                return;
            }
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof ShieldGeneratorBlockEntity shield) {
                confirmLinkedPeripheralPresent(pos, 2);
                int received = shield.getEnergy().receiveEnergy(remaining[0], false);
                remaining[0] -= received;
                charged[0] += received;
                if (received > 0) {
                    shield.setChanged();
                }
            } else {
                toRemove.add(pos);
            }
        }, 2);
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 2);
        }
        return charged[0];
    }

    private int drainLinkedBatteriesForShield(int energy) {
        int[] remaining = {energy};
        int[] drained = {0};
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            if (remaining[0] <= 0) {
                return;
            }
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractEnergyBatteryBlockEntity battery) {
                confirmLinkedPeripheralPresent(pos, 4);
                int extracted = battery.getEnergyStorage().extractEnergy(remaining[0], false);
                remaining[0] -= extracted;
                drained[0] += extracted;
                if (extracted > 0) {
                    battery.setChanged();
                }
            } else {
                toRemove.add(pos);
            }
        }, 4);
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 4);
        }
        return drained[0];
    }

    public boolean hasLinkedBatteryEnergy(int energy) {
        if (energy <= 0) {
            return true;
        }
        // Function: accelerator power checks must read the live linked-battery pool instead of cached HUD values.
        return getCurrentLinkedBatteryEnergyAvailable() >= energy;
    }

    public boolean consumeLinkedBatteryEnergy(int energy) {
        if (energy <= 0) {
            return true;
        }
        if (!hasLinkedBatteryEnergy(energy)) {
            return false;
        }
        int drained = drainLinkedBatteriesForShield(energy);
        if (drained < energy) {
            refundLinkedBatteriesFromShield(drained);
            return false;
        }
        int remainingEnergy = getCurrentLinkedBatteryEnergyAvailable();
        totalenergyavalible = remainingEnergy;
        controlseatData.avalibleenergy = remainingEnergy;
        setChanged();
        return true;
    }

    private int getCurrentLinkedBatteryEnergyAvailable() {
        int[] available = {0};
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractEnergyBatteryBlockEntity battery) {
                confirmLinkedPeripheralPresent(pos, 4);
                available[0] += battery.getEnergy().getEnergyStored();
            } else {
                toRemove.add(pos);
            }
        }, 4);
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 4);
        }
        return available[0];
    }

    private void refundLinkedBatteriesFromShield(int energy) {
        int[] remaining = {energy};
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            if (remaining[0] <= 0) {
                return;
            }
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractEnergyBatteryBlockEntity battery) {
                confirmLinkedPeripheralPresent(pos, 4);
                // Refund only protects against stale shield receive simulations in the same tick.
                int received = battery.getEnergyStorage().receiveEnergy(remaining[0], false);
                remaining[0] -= received;
                if (received > 0) {
                    battery.setChanged();
                }
            } else {
                toRemove.add(pos);
            }
        }, 4);
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 4);
        }
    }

    private boolean playShieldOpenFx(SubLevel sublevel, Vec3 center) {
        if (!(level instanceof ServerLevel) || controlseatData.shieldradius <= 0.0D) {
            return false;
        }
        Quaternionf rotation = shieldOpenRotation(sublevel);
        float scale = Math.max(0.01F, (float) controlseatData.shieldradius / SHIELD_OPEN_DEFAULT_RADIUS);
        Vector3d velocity = getSublevelLinearVelocity(sublevel);
        // Scale shield_open from its default radius 8 to the current shield radius.
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                SHIELD_OPEN_FX,
                center.x, center.y, center.z,
                velocity.x, velocity.y, velocity.z,
                rotation,
                new Vector3f(scale, scale, scale),
                true
        ));
        return true;
    }

    private Quaternionf shieldOpenRotation(SubLevel sublevel) {
        Vector3f shieldUp = controlSeatUpWorld(sublevel);
        Vector3f shieldNormal = controlSeatRightWorld(sublevel);

        // First align the effect local Y axis to the control seat local Y axis in world space.
        Quaternionf rotation = new Quaternionf().rotationTo(
                0.0F, 1.0F, 0.0F,
                shieldUp.x, shieldUp.y, shieldUp.z
        );

        // Then twist around that Y axis so the effect local Z axis still matches the shield plane normal.
        Vector3f rotatedLocalZ = new Vector3f(0.0F, 0.0F, 1.0F);
        rotation.transform(rotatedLocalZ);
        projectOntoPlane(rotatedLocalZ, shieldUp);
        Vector3f targetNormal = new Vector3f(shieldNormal);
        projectOntoPlane(targetNormal, shieldUp);
        if (rotatedLocalZ.lengthSquared() > 1.0E-6F && targetNormal.lengthSquared() > 1.0E-6F) {
            rotatedLocalZ.normalize();
            targetNormal.normalize();
            float dot = Mth.clamp(rotatedLocalZ.dot(targetNormal), -1.0F, 1.0F);
            float angle = (float) Math.acos(dot);
            Vector3f cross = rotatedLocalZ.cross(targetNormal, new Vector3f());
            if (cross.dot(shieldUp) < 0.0F) {
                angle = -angle;
            }
            rotation.rotateAxis(angle, shieldUp.x, shieldUp.y, shieldUp.z);
        }
        return rotation;
    }

    private Vector3d getSublevelLinearVelocity(SubLevel sublevel) {
        if (!(sublevel instanceof ServerSubLevel serverSubLevel)) {
            return new Vector3d();
        }
        RigidBodyHandle handle = RigidBodyHandle.of(serverSubLevel);
        if (handle == null || !handle.isValid()) {
            return new Vector3d();
        }
        // Capture the current ship velocity so shield_open keeps moving with the sublevel while it plays.
        return handle.getLinearVelocity(new Vector3d());
    }

    private void playShieldHitFx(Vec3 hitPoint, Vec3 normal) {
        if (!(level instanceof ServerLevel) || normal.lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vec3 normalized = normal.normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(
                0.0F, 1.0F, 0.0F,
                (float) normalized.x, (float) normalized.y, (float) normalized.z
        );
        // Align shield_hit local Y axis with the shield surface normal at the impact point.
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                SHIELD_HIT_FX,
                hitPoint.x, hitPoint.y, hitPoint.z,
                0.0D, 0.0D, 0.0D,
                rotation,
                new Vector3f(1.0F, 1.0F, 1.0F),
                true,
                true
        ));
    }

    private Vector3f controlSeatUpWorld(SubLevel sublevel) {
        Vector3d up = new Vector3d(0.0D, 1.0D, 0.0D);
        sublevel.logicalPose().orientation().transform(up);
        if (up.lengthSquared() <= 1.0E-6D) {
            up.set(0.0D, 1.0D, 0.0D);
        }
        up.normalize();
        return new Vector3f((float) up.x, (float) up.y, (float) up.z);
    }

    private Vector3f controlSeatRightWorld(SubLevel sublevel) {
        Direction facing = getBlockState().hasProperty(BlockStateProperties.FACING)
                ? getBlockState().getValue(BlockStateProperties.FACING)
                : Direction.EAST;
        Vector3d forward = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vector3d up = new Vector3d(0.0D, 1.0D, 0.0D);
        Vector3d right = forward.cross(up, new Vector3d());
        if (right.lengthSquared() <= 1.0E-6D) {
            right.set(0.0D, 0.0D, 1.0D);
        }
        right.normalize();
        sublevel.logicalPose().orientation().transform(right);
        right.normalize();
        // The seat uses local X as forward, Y as up, and Z as right; Z is the shield_open plane normal.
        return new Vector3f((float) right.x, (float) right.y, (float) right.z);
    }

    private static void projectOntoPlane(Vector3f vector, Vector3f planeNormal) {
        float alongNormal = vector.dot(planeNormal);
        vector.sub(
                planeNormal.x * alongNormal,
                planeNormal.y * alongNormal,
                planeNormal.z * alongNormal
        );
    }

    public void updateTurret() {

        int activeSeatChannelEncode = 0;
        if (controlseatData.getChannel1()) activeSeatChannelEncode |= 1;
        if (controlseatData.getChannel2()) activeSeatChannelEncode |= 2;
        if (controlseatData.getChannel3()) activeSeatChannelEncode |= 4;
        if (controlseatData.getChannel4()) activeSeatChannelEncode |= 8;

        List<Vec3> toRemove = new ArrayList<>();
        int finalActiveSeatChannelEncode = activeSeatChannelEncode;
        ArrayList<SubLevel> enemySubLevels = ScanNearByShips.scanEnemySubLevels(
                null,
                getBlockPos(),
                level,
                controlseatData.enemy,
                controlseatData.ally
        );
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractTurretBlockEntity turret) {
                confirmLinkedPeripheralPresent(pos, 3);
                if (be instanceof CustomTurretBlockEntity customTurret && customTurret.usesHeavyControlSemantics()) {
                    this.energyspendpertick += customTurret.getControlSeatEnergyCostPerTick();
                    boolean hasSeatedPlayer = controlseatData.getPlayer() != null;
                    SubLevel automaticTarget = controlseatData.lockedEnemySubLevel;
                    if (automaticTarget == null && !enemySubLevels.isEmpty()) {
                        int targetIndex = Math.floorMod(controlseatData.lockedenemyindex, enemySubLevels.size());
                        automaticTarget = enemySubLevels.get(targetIndex);
                    }
                    boolean automaticMode = customTurret.usesAutomaticHeavyTarget(
                            hasSeatedPlayer, controlseatData.isviewlocked);
                    Vec3 manualTarget = customTurret.usesManualHeavyTarget(
                            hasSeatedPlayer, controlseatData.isviewlocked)
                            ? new Vec3(controlseatData.manualAimTargetX, controlseatData.manualAimTargetY,
                            controlseatData.manualAimTargetZ)
                            : null;
                    // Function: energy-heavy custom turrets never receive a manual target; projectile-heavy turrets may.
                    customTurret.updateHeavyControl(
                            automaticMode ? automaticTarget : null,
                            manualTarget,
                            finalActiveSeatChannelEncode,
                            controlseatData.isfiring
                    );
                    if (customTurret.isHeavyChannelArmed()) {
                        controlseatData.activeWeaponHudInfos.add(new ActiveWeaponHudInfo(
                                customTurret.getDisplayName().getString(),
                                customTurret.getCooldownHudValue(),
                                customTurret.getCooldownHudMax(),
                                customTurret.isCooldownHudRemaining(),
                                customTurret.isHudFireReady()
                        ));
                    }
                } else if (be instanceof AbstractHeavyTurretBlockEntity heavyturret) {
                    this.energyspendpertick += heavyturret.getControlSeatEnergyCostPerTick();


                    boolean hasSeatedPlayer = controlseatData.getPlayer() != null;
                    boolean isViewLocked = controlseatData.isviewlocked;
                    heavyturret.armedChannelFromCtrl(finalActiveSeatChannelEncode);
                    heavyturret.updateControlSeatViewLock(isViewLocked);
                    // Function: choose heavy turret targeting from the current seat state, not stale turret NBT.
                    if (heavyturret.usesAutomaticTarget(hasSeatedPlayer, isViewLocked)) {
                        if (controlseatData.lockedEnemySubLevel != null) {
                            // Function: pass the live enemy sublevel so heavy turrets can track its current position every tick.
                            heavyturret.updatespecificenemy(controlseatData.lockedEnemySubLevel);
                        } else if (!controlseatData.enemyshipsData.isEmpty()) {
                            int targetIndex = Math.floorMod(controlseatData.lockedenemyindex, controlseatData.enemyshipsData.size());
                            heavyturret.updatespecificenemy(controlseatData.enemyshipsData.get(targetIndex));
                        } else {
                            heavyturret.clearSpecificEnemy();
                        }
                    }

                    else if (heavyturret.usesManualTarget(hasSeatedPlayer, isViewLocked)){

                        heavyturret.updateplayerstatus(
                                hasSeatedPlayer,
                                isViewLocked,
                                new Vec3(controlseatData.manualAimTargetX, controlseatData.manualAimTargetY, controlseatData.manualAimTargetZ)
                        );
                    } else {
                        // Function: manual mode with view lock or no seated player clears target and lets the turret re-center.
                        heavyturret.clearSpecificEnemy();
                    }
                    if (controlseatData.isfiring) {
                        heavyturret.channelFromCtrl(finalActiveSeatChannelEncode);
                    } else {
                        heavyturret.channelFromCtrl(0);
                    }
                    if (heavyturret.isArmedChannelMatch()) {
                        controlseatData.activeWeaponHudInfos.add(new ActiveWeaponHudInfo(
                                heavyturret.getDisplayName().getString(),
                                heavyturret.getCooldownHudValue(),
                                heavyturret.getCooldownHudMax(),
                                heavyturret.isCooldownHudRemaining(),
                                heavyturret.isHudFireReady()
                        ));
                    }
                } else {
                    // Function: normal turrets require live enemy SubLevels for ship-target acquisition.
                    turret.updateenemy(new ArrayList<>(enemySubLevels));
                    this.energyspendpertick += turret.getControlSeatEnergyCostPerTick();
                }
            } else {

                toRemove.add(pos);
            }
        }, 3);

        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 3);
        }
    }

    public void updateFuel() {
        List<Vec3> toRemove = new ArrayList<>();
        this.hasThrusterFuelThisTick = false;
        int[] totalE710Available = {0};
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractFuelTankBlockEntity fueltank) {
                confirmLinkedPeripheralPresent(pos, 5);
                FluidStack fluid = fueltank.getFluidTank().getFluid();
                int currenttankremain = fluid.getAmount();
                if (isE710Fluid(fluid)) {
                    totalE710Available[0] += currenttankremain;
                }
                FluidThrusterProperties fuelProperties = getFuelProperties(fluid.getFluid());
                if(fuelProperties == null) {
                    totalfuel += fueltank.getFluidTank().getCapacity();
                    controlseatData.totalfuelstorage = totalfuel;
                    return;
                }
                float consumptionmultiplier = Math.max(fuelProperties.consumptionMultiplier, 1.0E-6F);
                if (currenttankremain > 0) {
                    this.hasThrusterFuelThisTick = true;
                }
                if(this.capacitorfuel < 0 && currenttankremain > 0) {
                    // Function: round fluid drain up so low-throttle DT fuel consumption does not truncate to zero.
                    int requestedDrain = (int) Math.ceil((-this.capacitorfuel) * consumptionmultiplier);
                    int drained = fueltank.getFluidTank()
                            .drain(Math.min(currenttankremain, Math.max(1, requestedDrain)), IFluidHandler.FluidAction.EXECUTE)
                            .getAmount();
                    this.capacitorfuel += (int) Math.floor(drained / consumptionmultiplier);
                }
                totalfuel += fueltank.getFluidTank().getCapacity();
                totalfuelavalible += fueltank.getFluidTank().getFluid().getAmount();
            } else {

                toRemove.add(pos);
            }
        }, 5);
        controlseatData.totalfuelstorage = totalfuel;
        controlseatData.avaliblefuel = totalfuelavalible;
        controlseatData.avalibleE710 = totalE710Available[0];
        if (controlseatData.warpE710Insufficient
                && controlseatData.warpE710CostMb > 0
                && totalE710Available[0] >= controlseatData.warpE710CostMb) {

            controlseatData.warpE710Insufficient = false;
        }
        //LogUtils.getLogger().warn("detected total energy:"+controlseatData.totalenergystorage+"avalible:"+controlseatData.avalibleenergy);

        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 5);
        }
    }

    private void disableThrusterOutput() {
        this.calculatedstrength = 0;
        controlseatData.thruster_strength = 0;
        controlseatData.thruster_force_strength = 0;
        controlseatData.thruster_torque_strength = 0;
        controlseatData.setFinalforce(new Vector3d());
        controlseatData.setThrusterVisualForce(new Vector3d());
        controlseatData.setFinaltorque(new Vector3d());
        serverShipHandler.resetControlInput();
        // Function: linked thrusters receive zero demand immediately when fuel or energy cannot support thrust.
        this.forEachLinkedPeripheral(pos -> {
            BlockEntity be = level.getBlockEntity(BlockPos.containing(pos));
            if (be instanceof AbstractThrusterBlockEntity thruster) {
                confirmLinkedPeripheralPresent(pos, 0);
                thruster.setdata(new Vector3d(), new Vector3d(), 0.0D);
            }
        }, 0);
    }

    private void disableLinkedPeripheralsForNoPower() {
        disableThrusterOutput();
        shieldOpenFxPlayed = false;
        controlseatData.activeWeaponHudInfos = new ArrayList<>();
        controlseatData.isForceAssistSuppressedByAccelerator = false;

        // Function: zero all weapon channels/targets so linked weapons cannot keep firing on stale control-seat state.
        this.forEachLinkedPeripheral(pos -> {
            BlockEntity be = level.getBlockEntity(BlockPos.containing(pos));
            if (be instanceof AbstractWeaponBlockEntity weapon) {
                confirmLinkedPeripheralPresent(pos, 1);
                weapon.receivetarget(null);
                weapon.receivechannel(0);
                weapon.getData().isfiring = false;
                if (weapon instanceof VerticleLaunchingSlotCoreBlockEntity verticalLaunchCore) {
                    verticalLaunchCore.receiveArmedChannels(0);
                }
            }
        }, 1);

        // Function: clear all turret targets and channels so autonomous turrets do not continue to act while unpowered.
        this.forEachLinkedPeripheral(pos -> {
            BlockEntity be = level.getBlockEntity(BlockPos.containing(pos));
            if (be instanceof AbstractTurretBlockEntity turret) {
                confirmLinkedPeripheralPresent(pos, 3);
                turret.clearControlSeatTargeting();
                if (turret instanceof AbstractHeavyTurretBlockEntity heavyturret) {
                    heavyturret.armedChannelFromCtrl(0);
                    heavyturret.channelFromCtrl(0);
                    heavyturret.clearSpecificEnemy();
                }
            }
        }, 3);
    }

    public void updateScreen(){

        refreshWorldPosition();
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractScreenBlockEntity screen) {
                confirmLinkedPeripheralPresent(pos, 7);

                if (!screen.hasRadarPlayer() && controlseatData.getPlayer() != null) {
                    screen.setRadarPlayerUuid(controlseatData.getPlayer().getUUID());
                }

                // Function: push the current control-seat radar snapshot to every linked screen each server tick.
                screen.setRadarSnapshot(
                        controlseatData.shipsData,
                        controlseatData.enemy,
                        controlseatData.ally,
                        controlseatData.lockedenemyslug
                );
                screen.setRadarControlSeatWorldPos(new Vector3d(currentworldpos));
                return;
            }

            toRemove.add(pos);
        }, 7);
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 7);
        }
    }


    public void refreshWorldPosition() {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        if (subLevel != null) {
            if (subLevel instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel serverSubLevel) {
                controlseatData.serverShip = serverSubLevel;
            }
            Vec3 worldPos = ServerShipUtils.getBlockCenterWorld(subLevel, this.getBlockPos());
            currentworldpos = new Vector3d(worldPos.x, worldPos.y, worldPos.z);
            return;
        }
        Vec3 worldPos = Vec3.atCenterOf(this.getBlockPos());
        currentworldpos = new Vector3d(worldPos.x, worldPos.y, worldPos.z);
    }

    public static void lookAtEntityPos(Entity entity, Vec3 target) {
        Vec3 entityPos = entity.getEyePosition();
        double dx = target.x - entityPos.x;
        double dy = target.y - entityPos.y;
        double dz = target.z - entityPos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        float pitch = (float) (-(Mth.atan2(dy, distXZ) * (180F / Math.PI)));

        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;

        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
            living.yHeadRotO = yaw;
            living.setYBodyRot(yaw);
            living.yBodyRotO = yaw;
        }
    }

    public ControlSeatServerData getServerData() { return controlseatData; }

    public void playThrottleStartBoostSounds() {
        if (level == null || level.isClientSide) {
            return;
        }

        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (!(be instanceof AbstractThrusterBlockEntity thruster)) {
                return;
            }

            ThrusterBoostSoundProfile profile = ThrusterBoostSoundProfile.fromThrusterType(thruster.getthrustertype());
            if (profile == null) {
                return;
            }

            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, thruster.getBlockPos());
            Vec3 worldPos = ServerShipUtils.getBlockCenterWorld(subLevel, thruster.getBlockPos());
            level.playSound(
                    null,
                    worldPos.x,
                    worldPos.y,
                    worldPos.z,
                    profile.soundEvent(),
                    SoundSource.BLOCKS,
                    profile.volume() * THRUSTER_BOOST_VOLUME_SCALE,
                    profile.pitch()
            );
        }, 0);
    }

    private record ThrusterBoostSoundProfile(SoundEvent soundEvent, float volume, float pitch) {
        private static ThrusterBoostSoundProfile fromThrusterType(String thrusterType) {
            return switch (thrusterType) {
                // Function: mirror the original per-thruster boost transient tuning at the new server-side trigger point.
                case "basic" -> new ThrusterBoostSoundProfile(vsieSounds.BASIC_THRUSTER_BOOST.get(), 0.78F, 1.08F);
                case "basic_vector" -> new ThrusterBoostSoundProfile(vsieSounds.BASIC_VECTOR_THRUSTER_BOOST.get(), 0.85F, 1.04F);
                case "medium" -> new ThrusterBoostSoundProfile(vsieSounds.MEDIUM_THRUSTER_BOOST.get(), 1.02F, 0.96F);
                case "large" -> new ThrusterBoostSoundProfile(vsieSounds.LARGE_THRUSTER_BOOST.get(), 1.20F, 0.90F);
                default -> null;
            };
        }
    }

    public void clearControlInput() {
        controlseatData.reset();
        serverShipHandler.resetControlInput();
        setChanged();
    }

    //public ControlSeatClientData getClientData() { return ControlSeatClientData; }

    public boolean sit(Player player, boolean force) {
        if (player.level().isClientSide) {
            return false;
        }
        final Logger LOGGER = LogUtils.getLogger();
        //player.displayClientMessage(Component.literal("server side, executing sit logic"), true);

        if (!force && player.getVehicle() instanceof ControlSeatMountEntity seat && seats.contains(seat)) {
            //player.displayClientMessage(Component.literal("already sitting, returning true"), true);
            return true;
        }

        ServerLevel serverLevel = (ServerLevel) player.level();
        controlseatData.setPlayer(player);
        //LOGGER.warn(String.valueOf(Component.literal("seated player detected:"+controlseatData.getPlayer()+" uuid:"+controlseatData.getPlayer().getUUID())));
        return startRiding(force, getBlockPos(), getBlockState(), serverLevel);
    }



    public void onRemove() {
        // Function: block removal must detach any player-bound ship trail before seat runtime state is cleared.
        setRailAccelerationTrailOverrideActive(false);
        serverShipHandler.resetControlInput();
        controlseatData.reset();
        if (level != null && !level.isClientSide()) {
            SeatRegistry.unregisterControlSeat(level, getBlockPos());
            for (ControlSeatMountEntity seat : seats) {
                SeatRegistry.SEAT_TO_CONTROLSEAT.remove(seat.getUUID());
                seat.discard();
            }
            seats.clear();
        }

        super.setRemoved();
    }

    private boolean validateSingleControlSeatForSubLevel() {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        return validateSingleControlSeatForSubLevel(subLevel);
    }

    private boolean validateSingleControlSeatForSubLevel(SubLevel subLevel) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return true;
        }
        if (subLevel == null) {
            SeatRegistry.unregisterControlSeat(level, getBlockPos());
            return true;
        }
        if (SeatRegistry.registerOrUpdateControlSeat(level, getBlockPos(), subLevel)) {
            return true;
        }

        // Function: a sublevel may only keep one control seat, so a duplicate destroys itself immediately.
        destroyDuplicateControlSeat(serverLevel);
        return false;
    }

    private void destroyDuplicateControlSeat(ServerLevel serverLevel) {
        Vec3 explosionCenter = Vec3.atCenterOf(getBlockPos());
        SeatRegistry.unregisterControlSeat(level, getBlockPos());
        level.destroyBlock(getBlockPos(), false);
        serverLevel.explode(
                null,
                explosionCenter.x,
                explosionCenter.y,
                explosionCenter.z,
                4.0F,
                true,
                Level.ExplosionInteraction.TNT
        );
    }


    ControlSeatMountEntity spawnSeat(BlockPos pos, BlockState state, ServerLevel level) {
        ControlSeatMountEntity entity = vsieEntities.CONTROL_SEAT_MOUNT_ENTITY.get().create(level);
        assert entity != null;
        Vec3 mountPos = ControlSeatMountEntity.getSeatMountPosition(pos, state);
        float yaw = ControlSeatMountEntity.getSeatYaw(state);
        entity.setBoundBlockPos(pos);
        entity.setPos(mountPos);
        entity.setYRot(yaw);
        entity.yRotO = yaw;
        entity.setDeltaMovement(0, 0, 0);
        level.addFreshEntityWithPassengers(entity);
        SeatRegistry.SEAT_TO_CONTROLSEAT.put(entity.getUUID(), pos);
        return entity;
    }


    public boolean startRiding(boolean force, BlockPos blockPos, BlockState state, ServerLevel level) {
        Player player = controlseatData.getPlayer();
        Initialize.initialize(level,blockPos,state);


        for (int i = seats.size() - 1; i >= 0; i--) {
            ControlSeatMountEntity seat = seats.get(i);
            if (!seat.isVehicle()) {
                SeatRegistry.SEAT_TO_CONTROLSEAT.remove(seat.getUUID());
                seat.discard();
                seats.remove(i);

            } else if (!seat.isAlive()) {
                SeatRegistry.SEAT_TO_CONTROLSEAT.remove(seat.getUUID());
                seats.remove(i);
            }
        }

        ControlSeatMountEntity seat = spawnSeat(blockPos, state, level);
        ride = player.startRiding(seat, force);

        if (ride) {
            seats.add(seat);
            // Initialize mouse handler when the player sits down
        } else {
            SeatRegistry.SEAT_TO_CONTROLSEAT.remove(seat.getUUID());
            seat.discard();
        }
        return ride;
    }


    private Vec3 getSeatMountPosition(BlockPos pos, BlockState state) {
        return ControlSeatMountEntity.getSeatMountPosition(pos, state);
    }


    private void refreshSeatOccupancyFromWorld() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState state = getBlockState();
        Vec3 mountPos = getSeatMountPosition(getBlockPos(), state);
        AABB searchBox = new AABB(mountPos, mountPos).inflate(1.25D, 1.25D, 1.25D);


        seats.removeIf(seat -> seat == null || !seat.isAlive());

        Player seatedPlayer = null;
        for (ControlSeatMountEntity seatEntity : serverLevel.getEntitiesOfClass(ControlSeatMountEntity.class, searchBox, Entity::isAlive)) {
            if (!seatEntity.getBoundBlockPos().equals(getBlockPos())) {
                continue;
            }
            if (!seats.contains(seatEntity)) {
                seats.add(seatEntity);
            }
            SeatRegistry.SEAT_TO_CONTROLSEAT.put(seatEntity.getUUID(), getBlockPos());

            if (seatedPlayer == null && !seatEntity.getPassengers().isEmpty() && seatEntity.getPassengers().get(0) instanceof Player playerPassenger) {
                seatedPlayer = playerPassenger;
            }
        }

        if (seatedPlayer != null) {
            ride = true;
            occupantRestoreGraceTicks = 0;
            controlseatData.setPlayer(seatedPlayer);
        } else {
            ride = false;
            controlseatData.setPlayer(null);
            if (!isAwaitingOccupantRestore()) {
                // Function: a genuine seat exit still stops stale manual thrust immediately.
                serverShipHandler.clearManualControlInput();
                controlseatData.isviewlocked = false;
            }
        }
    }

    private boolean isAwaitingOccupantRestore() {
        return occupantRestoreGraceTicks > 0;
    }

    private void ensureOccupantRestoreMount() {
        if (!isAwaitingOccupantRestore() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (ControlSeatMountEntity seat : seats) {
            if (seat != null && seat.isAlive() && seat.getBoundBlockPos().equals(getBlockPos())) {
                return;
            }
        }
        ControlSeatMountEntity seat = spawnSeat(getBlockPos(), getBlockState(), serverLevel);
        seats.add(seat);
        LogUtils.getLogger().info(
                "[VSIE-SEAT-TRANSFER] phase=RESTORE_MOUNT_SPAWNED dimension={} seatPos={} mount={} graceTicks={}",
                serverLevel.dimension().location(),
                getBlockPos(),
                seat.getUUID(),
                occupantRestoreGraceTicks
        );
    }

    private void removeUnoccupiedSeatMounts() {
        for (int index = seats.size() - 1; index >= 0; index--) {
            ControlSeatMountEntity seat = seats.get(index);
            if (seat != null && seat.isAlive() && seat.isVehicle()) {
                continue;
            }
            if (seat != null) {
                SeatRegistry.SEAT_TO_CONTROLSEAT.remove(seat.getUUID());
                seat.discard();
            }
            seats.remove(index);
        }
    }

    public FluidThrusterProperties getFuelProperties(Fluid fluid) {
        return ThrusterFuelManager.getProperties(fluid);
    }

    public int getAvailableE710Mb() {
        int[] available = {0};
        this.forEachLinkedPeripheral(pos -> {
            BlockEntity be = level.getBlockEntity(BlockPos.containing(pos));
            if (be instanceof AbstractFuelTankBlockEntity fueltank) {
                confirmLinkedPeripheralPresent(pos, 5);
                if (isE710Fluid(fueltank.getFluidTank().getFluid())) {
                    available[0] += fueltank.getFluidTank().getFluidAmount();
                }
            }
        }, 5);
        return available[0];
    }

    public boolean consumeE710ForWarp(int amountMb) {
        if (amountMb <= 0) {
            return true;
        }
        if (getAvailableE710Mb() < amountMb) {
            return false;
        }
        int[] remaining = {amountMb};
        this.forEachLinkedPeripheral(pos -> {
            if (remaining[0] <= 0) {
                return;
            }
            BlockEntity be = level.getBlockEntity(BlockPos.containing(pos));
            if (be instanceof AbstractFuelTankBlockEntity fueltank) {
                confirmLinkedPeripheralPresent(pos, 5);
                if (isE710Fluid(fueltank.getFluidTank().getFluid())) {
                    int drained = fueltank.getFluidTank()
                            .drain(Math.min(remaining[0], fueltank.getFluidTank().getFluidAmount()), IFluidHandler.FluidAction.EXECUTE)
                            .getAmount();
                    remaining[0] -= drained;
                }
            }
        }, 5);

        setChanged();
        return remaining[0] <= 0;
    }

    public int calculateWarpE710CostMb(BlockPos targetPos) {
        if (level == null || targetPos == null) {
            return Integer.MAX_VALUE;
        }
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
            return Integer.MAX_VALUE;
        }
        MassData massData = serverSubLevel.getMassTracker();
        if (massData == null || massData.isInvalid()) {
            return Integer.MAX_VALUE;
        }
        Vec3 startWorldPos = ServerShipUtils.getStructureCenterWorld(subLevel);
        if (startWorldPos == null) {
            return Integer.MAX_VALUE;
        }
        Vec3 targetWorldPos = Vec3.atCenterOf(targetPos);
        // Function: warp E-710 cost uses the same structure-center start point as alignment and launch.
        double required = massData.getMass() * startWorldPos.distanceTo(targetWorldPos) * 0.1D;
        if (!Double.isFinite(required) || required < 0.0D) {
            return Integer.MAX_VALUE;
        }
        return required >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.ceil(required);
    }

    private boolean isE710Fluid(FluidStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.getFluid().getFluidType() == vsieFluids.E710.get().getFluidType();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }
}
