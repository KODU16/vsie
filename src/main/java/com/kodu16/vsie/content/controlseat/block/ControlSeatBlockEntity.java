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
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.kodu16.vsie.content.shield.ShieldGeneratorBlockEntity;
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
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
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
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.ArrayList;
import java.util.List;

public class ControlSeatBlockEntity extends AbstractControlSeatBlockEntity implements BlockEntitySubLevelActor {
    private static final ResourceLocation SHIELD_OPEN_FX = ResourceLocation.fromNamespaceAndPath("vsie", "shield_open");
    private static final ResourceLocation SHIELD_HIT_FX = ResourceLocation.fromNamespaceAndPath("vsie", "shield_hit");
    private static final float SHIELD_OPEN_DEFAULT_RADIUS = 8.0F;
    //private final ControlSeatServerData serverData = new ControlSeatServerData();
    public volatile boolean ride = false;
    private boolean hasInitialized = false;
    private boolean shieldOpenFxPlayed = false;
    private boolean hasThrusterFuelThisTick = false;
    public boolean previousfirestatus = false;
    private HolderLookup.Provider nbtRegistries;
    private Vector3d currentworldpos = new Vector3d();
    private List<ControlSeatMountEntity> seats = new ArrayList<>();
    private final ServerShipHandler serverShipHandler;

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
        controlseatData.serverShip = subLevel;
        controlseatData.level = level;
        serverShipHandler.getandsendshipdata(subLevel, getBlockPos());
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        controlseatData.serverShip = subLevel;
        controlseatData.level = level;
        serverShipHandler.applyForceAndTorque(subLevel, getBlockPos(), timeStep);
    }

    public String getcontrolseattype() {
        return "control_seat";
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }



    public void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        BlockPos pos = getBlockPos();


        ClientMouseHandler.handle(lp, pos);
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

        tag.putBoolean("IsViewLocked", controlseatData.isviewlocked);
        // Function: auto-level is a ship mode like anti-gravity and should survive block reloads.
        tag.putBoolean("IsAutoLevelOn", controlseatData.isAutoLevelOn);
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
        controlseatData.isviewlocked = tag.getBoolean("IsViewLocked");
        controlseatData.isAutoLevelOn = tag.getBoolean("IsAutoLevelOn");
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
        if (level.isClientSide)
            return;
        if (hasInitialized) {
            refreshSeatOccupancyFromWorld();

            controlseatData.controlSeatPos = getBlockPos();

            //update
            if (!ride) {
                controlseatData.clearSeatOccupantState();
                // Function: empty seat clears player input but leaves assist damping available for drift suppression.
                serverShipHandler.clearManualControlInput();
                controlseatData.setPlayer(null);
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
            if (!this.linkedBatteryPowerAvailableThisTick) {
                disableLinkedPeripheralsForNoPower();
                updateFuel();
                return;
            }

            updateThruster();
            updateWeapon();
            updateTurret();
            updateShield();
            this.capacitorenergy = -this.energyspendpertick;
            this.capacitorfuel = -this.fuelspendcurrenttick;
            //LogUtils.getLogger().warn("current energy cost per tick:"+this.energyspendpertick);
            this.totalenergy =100;
            this.totalenergyavalible = 0;
            updateEnergy();
            updateFuel();
            updateScreen();

            if(this.capacitorenergy < 0) {
                this.capacitorenergy = 0;
                disableThrusterOutput();
                return;
            }
            this.capacitorenergy = 0;

            if(this.capacitorfuel < 0 || !this.hasThrusterFuelThisTick) {
                this.capacitorfuel = 0;
                disableThrusterOutput();
                return;
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
                AABB searchBox = new AABB(this.getBlockPos()).inflate(controlseatData.shieldradius + 3.0);
                Vec3 finalCenter = center;
                int shieldCost = Math.max(0, (int) Math.ceil(controlseatData.shieldcostperprojectile));
                final int[] remainingShieldEnergy = {(int) Math.max(0.0D, controlseatData.avalibleshield)};
                final boolean[] overloadTriggered = {false};
                level.getEntitiesOfClass(Entity.class, searchBox, entity -> {
                    if (entity.isRemoved() || entity instanceof LivingEntity)
                        return false;

                    double speed = entity.getDeltaMovement().length();
                    if (speed < 0.25) return false;

                    Vec3 toEntity = entity.position().subtract(finalCenter);
                    double dot = entity.getDeltaMovement().normalize().dot(toEntity.normalize());
                    return dot < -0.3;
                }).forEach(entity -> {

                    Vec3 toEntity = entity.position().subtract(finalCenter);
                    double distSq = toEntity.lengthSqr();

                    if (overloadTriggered[0]) return;
                    if (distSq > controlseatData.shieldradius * controlseatData.shieldradius || distSq < 0.25) return;

                    entity.discard();
                    Vec3 hitDir = toEntity.normalize();
                    Vec3 hitPoint = finalCenter.add(hitDir.scale(controlseatData.shieldradius));
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
                            weapon.isCooldownHudRemaining()
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


        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 1);
        }
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
        controlseatData.shieldcostperprojectile = ((max*(max/min)*linkedShields.size()))*1000;
        controlseatData.shieldregeneratepertick = ((max*linkedShields.size()))*500;
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
                if (be instanceof AbstractHeavyTurretBlockEntity heavyturret) {
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
                                heavyturret.isCooldownHudRemaining()
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

    protected boolean isWorking() {
        return true;
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



    @Override
    public void onRemove() {
        controlseatData.reset();
        if (level != null && !level.isClientSide()) {
            for (ControlSeatMountEntity seat : seats) {
                SeatRegistry.SEAT_TO_CONTROLSEAT.remove(seat.getUUID());
                seat.discard();
            }
            seats.clear();
        }

        super.setRemoved();
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
            controlseatData.setPlayer(seatedPlayer);
        } else {
            ride = false;
            controlseatData.setPlayer(null);
            // Function: seat exit should stop stale manual thrust immediately.
            serverShipHandler.clearManualControlInput();
            controlseatData.isviewlocked = false;
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
        Vec3 seatWorldPos = ServerShipUtils.getBlockCenterWorld(subLevel, getBlockPos());
        Vec3 targetWorldPos = Vec3.atCenterOf(targetPos);
        double required = massData.getMass() * seatWorldPos.distanceTo(targetWorldPos) * 10.0D;
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
