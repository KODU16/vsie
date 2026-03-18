package com.kodu16.vsie.content.controlseat.block;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;
import com.kodu16.vsie.content.controlseat.Initialize;
import com.kodu16.vsie.content.controlseat.functions.ShieldHandler;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientMouseHandler;

import com.kodu16.vsie.content.controlseat.server.SeatRegistry;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.kodu16.vsie.content.shield.ShieldGeneratorBlockEntity;
import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.storage.energybattery.AbstractEnergyBatteryBlockEntity;
import com.kodu16.vsie.content.storage.fueltank.AbstractFuelTankBlockEntity;
import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.network.fuel.FluidThrusterProperties;
import com.kodu16.vsie.registries.fuel.ThrusterFuelManager;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.joml.primitives.AABBdc;
import org.slf4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
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
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.entity.ShipMountingEntity;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class ControlSeatBlockEntity extends AbstractControlSeatBlockEntity {
    //private final ControlSeatServerData serverData = new ControlSeatServerData();
    public volatile boolean ride = false;
    private boolean hasInitialized = false;
    public boolean previousfirestatus = false;

    // 功能：缓存控制椅当前世界坐标，供屏幕雷达使用。
    private Vector3d currentworldpos = new Vector3d();


    //即使我不想写的这么恶心，为了跨维度我还是得干
    //有两个hashmap，第二个是为了渲染HUD的时候用来反查controlseat
    private List<ShipMountingEntity> seats = new ArrayList<>();

    public SmartFluidTankBehaviour tank;

    // 功能：为 Shift+右键打开的控制椅专用 GUI 提供 27 格 warp data chip 存储。
    private final ItemStackHandler warpChipInventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 功能：限制控制椅仓位只接收 warp data chip。
            return stack.is(vsieItems.WARP_DATA_CHIP.get());
        }
    };

    public ControlSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public ItemStackHandler getWarpChipInventory() {
        return warpChipInventory;
    }

    public String getcontrolseattype() {
        return "control_seat";
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }


    //先接收client更新，叫client向服务端发包
    public void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        BlockPos pos = getBlockPos();
        // 只有当本地玩家就是这张座椅的乘客时才生效
        //这是个静态方法，最好提前确定好你在server存好了他上一次的鼠标位置和他上一次操作时间
        ClientMouseHandler.handle(lp, pos);
    }

    //再从服务端更新推力和力矩
    //窝草你发包怎么不告诉我对应不了服务端

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        // 功能：持久化控制椅 GUI 中存放的 warp data chip。
        tag.put("WarpChipInventory", warpChipInventory.serializeNBT());
    }

    @Override
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("WarpChipInventory")) {
            // 功能：在区块加载/同步时恢复控制椅 GUI 中保存的 warp data chip。
            warpChipInventory.deserializeNBT(tag.getCompound("WarpChipInventory"));
        }
    }

    public void tick() {
        Logger LOGGER = LogUtils.getLogger();
        if (level.isClientSide)
            return;
        if (hasInitialized) {

            //update
            if (!ride) {
                controlseatData.reset();
                controlseatData.setPlayer(null);
            }
            this.calculatedstrength = 0;
            this.energyspendpertick = 0;
            this.fuelspendcurrenttick = 0;

            this.totalenergy =100;
            this.totalenergyavalible = 0;
            this.totalfuel = 100;
            this.totalfuelavalible = 0;

            updateThruster();
            updateWeapon();
            updateTurret();
            updateShield();
            this.capacitorenergy = -this.energyspendpertick;
            this.capacitorfuel = -this.fuelspendcurrenttick;
            //LogUtils.getLogger().warn("current energy cost per tick:"+this.energyspendpertick);
            updateEnergy();
            updateFuel();
            updateScreen();

            if(this.capacitorenergy < 0) {
                this.capacitorenergy = 0;
                this.calculatedstrength = 0;
                return;
            }
            this.capacitorenergy = 0;

            if(this.capacitorfuel < 0) {
                this.capacitorfuel = 0;
                this.calculatedstrength = 0;
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

        //护盾
        if(controlseatData.isshieldon) {//如果护盾开启
            updateShieldEnergyAvalible();
            int currentcooldown = (int) controlseatData.shieldcooldowntime;
            if(controlseatData.shieldcooldowntime <= 0) {
                Ship ship = VSGameUtilsKt.getShipManagingPos(level,this.getBlockPos());
                Vec3 center = null;
                if (ship == null) {
                    return;
                }
                double[] c = getAABBdcCenter(ship.getWorldAABB());
                center = new Vec3(c[0],c[1],c[2]);
                AABB searchBox = new AABB(this.getBlockPos()).inflate(controlseatData.shieldradius + 3.0); // 多搜一点，防止高速实体一帧穿过去

                // 核心：只筛选“没有生命值 + 速度够快 + 不是玩家也不是盔甲架”之类的实体
                Vec3 finalCenter = center;
                level.getEntitiesOfClass(Entity.class, searchBox, entity -> {
                    if (entity.isRemoved() || entity instanceof LivingEntity)
                        return false;

                    // 速度阈值，可调（单位：方块/刻）
                    double speed = entity.getDeltaMovement().length();
                    if (speed < 0.25) return false; // 太慢的直接忽略（比如漂浮的物品）

                    // 计算是否朝护盾飞来
                    Vec3 toEntity = entity.position().subtract(finalCenter);
                    double dot = entity.getDeltaMovement().normalize().dot(toEntity.normalize());
                    return dot < -0.3; // 越负说明越正对护盾飞来（-0.3~0.6 之间调节手感）
                }).forEach(entity -> {

                    Vec3 toEntity = entity.position().subtract(finalCenter);
                    double distSq = toEntity.lengthSqr();

                    if (distSq > controlseatData.shieldradius * controlseatData.shieldradius || distSq < 0.25) return;
                    if(controlseatData.avalibleshield>0)
                    {
                        // 拦截
                        entity.discard();
                        // 粒子交点
                        Vec3 hitDir = toEntity.normalize();
                        Vec3 hitPoint = finalCenter.add(hitDir.scale(controlseatData.shieldradius));
                        ShieldHandler.spawnRippleParticles((ServerLevel) level, hitPoint, finalCenter);

                        // 可选：播放音效
                        level.playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS,
                                1.0f, 1.2f + level.random.nextFloat() * 0.4f);
                        SubtractShieldEnergy((int) controlseatData.shieldcostperprojectile);
                    }
                    else {
                        controlseatData.shieldcooldowntime = controlseatData.shieldmaxcooldowntime;
                    }
                });
                RegenerateShieldEnergy((int) controlseatData.shieldregeneratepertick);
            }
            else {
                controlseatData.shieldcooldowntime = currentcooldown - 1;
            }
        }

    }

    //0：推进器 1：主武器 2：护盾 3：炮塔 4：电池 5：燃料箱 6：弹药箱，务必不要写错
    public void updateEnergy() {//avalible：剩余值，非avalible：总值
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractEnergyBatteryBlockEntity battery) {
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
                // 先记下来，循环完了再删
                toRemove.add(pos);
            }
        }, 4);
        controlseatData.totalenergystorage = totalenergy;
        controlseatData.avalibleenergy = totalenergyavalible;
        //LogUtils.getLogger().warn("detected total energy:"+controlseatData.totalenergystorage+"avalible:"+controlseatData.avalibleenergy);
        // 循环结束后统一删除
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 4);
        }
    }

    public void updateThruster() {
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractThrusterBlockEntity thruster) {
                Logger LOGGER = LogUtils.getLogger();
                //LOGGER.warn("writing to thrusters:" +blockPos+ "torque:"+controlseatData.getFinaltorque()+"force:"+controlseatData.getFinalforce());
                thruster.setdata(controlseatData.getFinaltorque(), controlseatData.getFinalforce());
                this.calculatedstrength+=thruster.getMaxThrust();
                this.fuelspendcurrenttick += thruster.fuelconsumptionperthrottle()*thruster.getFuelThrottle();
            } else {
                // 先记下来，循环完了再删
                toRemove.add(pos);
            }
        }, 0);
        controlseatData.thruster_strength = this.calculatedstrength;
        // 循环结束后统一删除
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 0);
        }
    }

    public void updateWeapon() {
        // 每 tick 都向武器同步一次开火状态与频道，避免因丢包/状态不同步导致“左键按下但武器不发射”。
        // 仅在状态变化时才同步会出现武器端通道被重置后无法自动恢复的问题。
        previousfirestatus = controlseatData.isfiring;
        // 功能：提前计算控制椅当前激活频道编码，后续用于“武器-频道匹配”与同步开火输入。
        int activeSeatChannelEncode = 0;
        if (controlseatData.getChannel1()) activeSeatChannelEncode |= 1;
        if (controlseatData.getChannel2()) activeSeatChannelEncode |= 2;
        if (controlseatData.getChannel3()) activeSeatChannelEncode |= 4;
        if (controlseatData.getChannel4()) activeSeatChannelEncode |= 8;

        // 功能：缓存当前控制椅激活频道对应的武器 HUD 信息（名称+冷却进度），供 HUD 每行展示。
        List<ActiveWeaponHudInfo> activeWeaponHudInfos = new ArrayList<>();
        List<Vec3> toRemove = new ArrayList<>();
        int finalActiveSeatChannelEncode = activeSeatChannelEncode;
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractWeaponBlockEntity weapon) {
                // 功能：武器通过自身频道配置“告知”控制椅是否属于当前激活频道。
                if (isWeaponInAnyActiveChannel(weapon, finalActiveSeatChannelEncode)) {
                    // 功能：采集武器名称与冷却进度，供客户端绘制“油门样式”进度条。
                    activeWeaponHudInfos.add(new ActiveWeaponHudInfo(weapon.getDisplayName().getString(), weapon.currentTick, weapon.getcooldown()));
                }

                // 功能：按当前开火状态向武器同步控制椅频道输入。
                if (controlseatData.isfiring) {
                    weapon.receivechannel(finalActiveSeatChannelEncode);
                } else {
                    weapon.receivechannel(0);
                }
                if(!controlseatData.enemyshipsData.isEmpty()) {
                    Ship ship = controlseatData.enemyshipsData.get(controlseatData.lockedenemyindex);
                    if(ship!=null) {
                        weapon.receivetarget(ship);
                    }
                }
            } else {
                // 先记下来，循环完了再删
                toRemove.add(pos);
            }
        }, 1);

        // 功能：更新服务端缓存的激活武器 HUD 数据，供状态包同步到 HUD。
        controlseatData.activeWeaponHudInfos = activeWeaponHudInfos;

        // 循环结束后统一删除
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 1);
        }
    }

    // 功能：判断武器是否配置在控制椅当前激活频道中的任一频道。
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

    public void updateShield() {
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof ShieldGeneratorBlockEntity shield) {
                Logger LOGGER = LogUtils.getLogger();
            } else {
                // 先记下来，循环完了再删
                toRemove.add(pos);
            }
        }, 2);
        // 循环结束后统一删除
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 2);
        }
        double[] minmax = ShieldHandler.getMinMaxDistance(linkedShields);
        double max = minmax[0];
        double min = minmax[1];
        controlseatData.shieldmax = max;
        controlseatData.shieldmin = min;
        controlseatData.shieldradius = 0.75*max;
        controlseatData.totalshield = 100000 * linkedShields.size();
        controlseatData.shieldcostperprojectile = ((max*(max/min)*linkedShields.size()))*1000;
        controlseatData.shieldregeneratepertick = ((max*linkedShields.size()))*500;
        controlseatData.shieldmaxcooldowntime = (max/min)*100;
    }

    public void updateShieldEnergyAvalible() {
        avalibleshield = 0;
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof ShieldGeneratorBlockEntity shield) {
                Logger LOGGER = LogUtils.getLogger();
                avalibleshield += shield.getEnergy().getEnergyStored();
                shield.maxreceiverate = (int) (controlseatData.shieldregeneratepertick/linkedShields.size())+10;
            }
        }, 2);
        controlseatData.avalibleshield = avalibleshield;
    }

    public void SubtractShieldEnergy(int energy) {
        int eachsubtract = energy/linkedShields.size();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof ShieldGeneratorBlockEntity shield) {
                Logger LOGGER = LogUtils.getLogger();
                shield.getEnergy().extractEnergy(eachsubtract,false);
            }
        }, 2);
    }

    public void RegenerateShieldEnergy(int energy) {
        int eachregenerate = energy/linkedShields.size();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof ShieldGeneratorBlockEntity shield) {
                Logger LOGGER = LogUtils.getLogger();
                shield.getEnergy().receiveEnergy(eachregenerate,false);
            }
        }, 2);
    }

    public void updateTurret() {
        // 功能：先计算控制椅当前激活频道编码，用于向重型炮塔同步与主武器一致的频道输入。
        int activeSeatChannelEncode = 0;
        if (controlseatData.getChannel1()) activeSeatChannelEncode |= 1;
        if (controlseatData.getChannel2()) activeSeatChannelEncode |= 2;
        if (controlseatData.getChannel3()) activeSeatChannelEncode |= 4;
        if (controlseatData.getChannel4()) activeSeatChannelEncode |= 8;

        List<Vec3> toRemove = new ArrayList<>();
        int finalActiveSeatChannelEncode = activeSeatChannelEncode;
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractTurretBlockEntity turret) {
                this.energyspendpertick += turret.getenergypertick();
                if (be instanceof AbstractHeavyTurretBlockEntity heavyturret) {
                    // 功能：控制椅更新重型炮塔时，同步频道输入；仅在开火时下发激活频道。
                    if (controlseatData.isfiring) {
                        heavyturret.receivechannel(finalActiveSeatChannelEncode);
                    } else {
                        heavyturret.receivechannel(0);
                    }
                    // 功能：同步玩家视角状态时，额外下发控制椅方块朝向，供重型炮塔做方向转换后再头瞄。
                    Direction controlSeatFacing = this.getBlockState().getValue(BlockStateProperties.FACING);
                    heavyturret.updateplayerstatus(controlseatData.isviewlocked, controlseatData.playerrotx, controlseatData.playerroty, controlSeatFacing);
                    if (!controlseatData.enemyshipsData.isEmpty()) {
                        heavyturret.updatespecificenemy((Vector3d) controlseatData.enemyshipsData.get(controlseatData.lockedenemyindex).getTransform().getPositionInWorld());
                    }
                } else {
                    turret.updateenemy(controlseatData.enemyshipsData);
                }
            } else {
                // 先记下来，循环完了再删
                toRemove.add(pos);
            }
        }, 3);
        // 循环结束后统一删除
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 3);
        }
    }

    public void updateFuel() {
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractFuelTankBlockEntity fueltank) {
                FluidStack fluid = fueltank.getFluidTank().getFluid();
                int currenttankremain = fluid.getAmount();
                if(getFuelProperties(fluid.getRawFluid()) == null) {
                    totalfuel += fueltank.getFluidTank().getCapacity();
                    controlseatData.totalfuelstorage = totalfuel;
                    return;
                }
                float consumptionmultiplier = getFuelProperties(fluid.getRawFluid()).consumptionMultiplier;
                if(currenttankremain>=-this.capacitorfuel*consumptionmultiplier) {
                    fueltank.getFluidTank().drain((int) (-this.capacitorfuel*consumptionmultiplier), IFluidHandler.FluidAction.EXECUTE);
                    this.capacitorfuel = 0;
                }
                else {
                    fueltank.getFluidTank().drain(currenttankremain, IFluidHandler.FluidAction.EXECUTE);
                    this.capacitorfuel += (int) (currenttankremain/consumptionmultiplier);
                }
                totalfuel += fueltank.getFluidTank().getCapacity();
                totalfuelavalible += currenttankremain;
            } else {
                // 先记下来，循环完了再删
                toRemove.add(pos);
            }
        }, 5);
        controlseatData.totalfuelstorage = totalfuel;
        controlseatData.avaliblefuel = totalfuelavalible;
        //LogUtils.getLogger().warn("detected total energy:"+controlseatData.totalenergystorage+"avalible:"+controlseatData.avalibleenergy);
        // 循环结束后统一删除
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 5);
        }
    }

    public void updateScreen(){
        // 功能：每 tick 刷新控制椅世界坐标，供雷达投影使用。
        refreshWorldPosition();
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractScreenBlockEntity screen) {
                // 功能：当屏幕尚未绑定玩家时，绑定当前控制椅玩家。
                if (!screen.hasRadarPlayer() && controlseatData.getPlayer() != null) {
                    screen.setRadarPlayerUuid(controlseatData.getPlayer().getUUID());
                }
                // 功能：持续向屏幕同步控制椅世界坐标，保证雷达中心点实时更新。
                screen.setRadarControlSeatWorldPos(new Vector3d(currentworldpos));
                return;
            }
            // 功能：清理已经失效或被替换的屏幕链接。
            toRemove.add(pos);
        }, 7);
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 7);
        }
    }

    // 功能：仿照炮塔逻辑，刷新控制椅世界坐标（不在船上为 blockpos，在船上转世界坐标）。
    public void refreshWorldPosition() {
        boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, this.getBlockPos());
        if (onShip) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
            if (ship != null) {
                Vector3d center = VSGameUtilsKt.toWorldCoordinates(ship, this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
                currentworldpos = new Vector3d(center.x, center.y, center.z);
                return;
            }
        }
        currentworldpos = new Vector3d(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
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

    //public ControlSeatClientData getClientData() { return ControlSeatClientData; }

    public boolean sit(Player player, boolean force) {
        if (player.level().isClientSide) {
            return false;
        }
        final Logger LOGGER = LogUtils.getLogger();
        //player.displayClientMessage(Component.literal("server side, executing sit logic"), true);

        if (!force && player.getVehicle() != null && player.getVehicle().getType() == ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE && seats.contains(player.getVehicle())) {
            //player.displayClientMessage(Component.literal("already sitting, returning true"), true);
            return true;
        }

        ServerLevel serverLevel = (ServerLevel) player.level();
        controlseatData.setPlayer(player);
        //LOGGER.warn(String.valueOf(Component.literal("seated player detected:"+controlseatData.getPlayer()+" uuid:"+controlseatData.getPlayer().getUUID())));
        return startRiding(force, getBlockPos(), getBlockState(), serverLevel);
    }


    // 在移除座椅时清除控制记录
    @Override
    public void onRemove() {
        controlseatData.reset();
        if (level != null && !level.isClientSide()) {
            for (ShipMountingEntity seat : seats) {
                SeatRegistry.SEAT_TO_CONTROLSEAT.remove(seat.getUUID());
                seat.kill();
            }
            seats.clear();
        }
        // 移除玩家的 UUID 记录
        super.setRemoved();
    }


    ShipMountingEntity spawnSeat(BlockPos pos, BlockState state, ServerLevel level) {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        Vector3dc mounterPos;
        if (facing == Direction.NORTH) {
            mounterPos = new Vector3d(pos.getX()+0.5, pos.getY(), pos.getZ());
        } else if (facing == Direction.SOUTH) {
            mounterPos = new Vector3d(pos.getX()+0.5, pos.getY(), pos.getZ()+1);
        } else if (facing == Direction.EAST) {
            mounterPos = new Vector3d(pos.getX()+1, pos.getY(), pos.getZ()+0.5);
        } else {
            mounterPos = new Vector3d(pos.getX(), pos.getY(), pos.getZ()+0.5);
        }

        ShipMountingEntity entity = ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE.create(level);
        assert entity != null;
        entity.setPos(mounterPos.x(), mounterPos.y(), mounterPos.z());
        Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        lookAtEntityPos(entity, target);
        entity.setPos(mounterPos.x(), mounterPos.y(), mounterPos.z());
        entity.setDeltaMovement(0, 0, 0);
        entity.setController(true);
        level.addFreshEntityWithPassengers(entity);
        SeatRegistry.SEAT_TO_CONTROLSEAT.put(entity.getUUID(), pos);
        return entity;
    }

    // 修改 startRiding 方法，确保每个座椅控制与玩家 UUID 相关联
    public boolean startRiding(boolean force, BlockPos blockPos, BlockState state, ServerLevel level) {
        Player player = controlseatData.getPlayer();
        Initialize.initialize(level,blockPos,state);
        // 使用玩家的 UUID 来确定哪个玩家在这个座椅上
        // 清理空的座椅
        for (int i = seats.size() - 1; i >= 0; i--) {
            ShipMountingEntity seat = seats.get(i);
            if (!seat.isVehicle()) {
                seat.kill();
                seats.remove(i);

            } else if (!seat.isAlive()) {
                seats.remove(i);
            }
        }

        ShipMountingEntity seat = spawnSeat(blockPos, state, level);
        ride = player.startRiding(seat, force);

        if (ride) {
            seats.add(seat);
            // Initialize mouse handler when the player sits down
        }
        return ride;
    }

    private static double[] getAABBdcCenter(AABBdc aabb) {
        double width = aabb.maxX() - aabb.minX();
        double len = aabb.maxZ() - aabb.minZ();
        double hight = aabb.maxY() - aabb.minY();
        double centerX = aabb.minX() + width / 2;
        double centerY = aabb.minY() + hight / 2;
        double centerZ = aabb.minZ() + len / 2;
        return new double[]{centerX, centerY, centerZ};
    }

    public FluidThrusterProperties getFuelProperties(Fluid fluid) {
        return ThrusterFuelManager.getProperties(fluid);
    }
}
