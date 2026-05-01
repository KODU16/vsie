package com.kodu16.vsie.content.controlseat.block;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;
import com.kodu16.vsie.content.controlseat.Initialize;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.controlseat.functions.ShieldHandler;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.controlseat.server.ServerShipHandler;
import com.kodu16.vsie.content.controlseat.client.Input.ClientMouseHandler;

import com.kodu16.vsie.content.controlseat.server.SeatRegistry;
import com.kodu16.vsie.foundation.ServerShipUtils;
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
import com.kodu16.vsie.network.fuel.FluidThrusterProperties;
import com.kodu16.vsie.registries.fuel.ThrusterFuelManager;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.nbt.CompoundTag;
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
import org.joml.Vector3d;
import net.neoforged.neoforge.items.ItemStackHandler;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.ArrayList;
import java.util.List;

public class ControlSeatBlockEntity extends AbstractControlSeatBlockEntity implements BlockEntitySubLevelActor {
    //private final ControlSeatServerData serverData = new ControlSeatServerData();
    public volatile boolean ride = false;
    private boolean hasInitialized = false;
    public boolean previousfirestatus = false;

    // 鍔熻兘锛氱紦瀛樻帶鍒舵褰撳墠涓栫晫鍧愭爣锛屼緵灞忓箷闆疯揪浣跨敤銆?
    private Vector3d currentworldpos = new Vector3d();


    //鍗充娇鎴戜笉鎯冲啓鐨勮繖涔堟伓蹇冿紝涓轰簡璺ㄧ淮搴︽垜杩樻槸寰楀共
    //鏈変袱涓猦ashmap锛岀浜屼釜鏄负浜嗘覆鏌揌UD鐨勬椂鍊欑敤鏉ュ弽鏌ontrolseat
    private List<ControlSeatMountEntity> seats = new ArrayList<>();
    private final ServerShipHandler serverShipHandler;

    public SmartFluidTankBehaviour tank;

    // 鍔熻兘锛氫负 Shift+鍙抽敭鎵撳紑鐨勬帶鍒舵涓撶敤 GUI 鎻愪緵 27 鏍?warp data chip 瀛樺偍銆?
    private final ItemStackHandler warpChipInventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 鍔熻兘锛氶檺鍒舵帶鍒舵浠撲綅鍙帴鏀?warp data chip銆?
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
        serverShipHandler.applyForceAndTorque(subLevel, getBlockPos());
    }

    public String getcontrolseattype() {
        return "control_seat";
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }


    //鍏堟帴鏀禼lient鏇存柊锛屽彨client鍚戞湇鍔＄鍙戝寘
    public void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        BlockPos pos = getBlockPos();
        // 鍙湁褰撴湰鍦扮帺瀹跺氨鏄繖寮犲骇妞呯殑涔樺鏃舵墠鐢熸晥
        //杩欐槸涓潤鎬佹柟娉曪紝鏈€濂芥彁鍓嶇‘瀹氬ソ浣犲湪server瀛樺ソ浜嗕粬涓婁竴娆＄殑榧犳爣浣嶇疆鍜屼粬涓婁竴娆℃搷浣滄椂闂?
        ClientMouseHandler.handle(lp, pos);
    }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        // 鍔熻兘锛氭寔涔呭寲鎺у埗妞?GUI 涓瓨鏀剧殑 warp data chip銆?
        //tag.put("WarpChipInventory", warpChipInventory.serializeNBT(registries));
        // 鍔熻兘锛氭妸褰撳墠閫変腑鐨勮穬杩佺洰鏍囦竴骞跺啓鍏?NBT锛屼繚璇佸尯鍧楀嵏杞藉悗 control seat 浠嶈寰椾笅涓€娆¤穬杩佸潗鏍囥€?
        tag.putInt("WarpTargetX", controlseatData.warpTargetPos.getX());
        tag.putInt("WarpTargetY", controlseatData.warpTargetPos.getY());
        tag.putInt("WarpTargetZ", controlseatData.warpTargetPos.getZ());
        tag.putString("WarpTargetDimension", controlseatData.warpTargetDimension);
        tag.putString("WarpTargetName", controlseatData.warpTargetName);
        // 鍔熻兘锛氬悓姝?warp 鍑嗗鐘舵€佸埌瀹㈡埛绔紝璁╂寜涓?P 鏃惰兘姝ｇ‘璧扳€滃彇娑堝噯澶団€濊€屼笉鏄啀娆″紑鑿滃崟銆?
        tag.putBoolean("IsWarpPreparing", controlseatData.isWarpPreparing);
        // 鍔熻兘锛氭寔涔呭寲鈥滆瑙掗攣瀹氣€濆紑鍏筹紝淇濊瘉鐜╁閲嶈繘涓栫晫涓斾粛鍦ㄥ骇妞呬笂鏃跺彲鎭㈠鎺у埗鎬併€?
        tag.putBoolean("IsViewLocked", controlseatData.isviewlocked);
    }

    @Override
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("WarpChipInventory")) {
            // 鍔熻兘锛氬湪鍖哄潡鍔犺浇/鍚屾鏃舵仮澶嶆帶鍒舵 GUI 涓繚瀛樼殑 warp data chip銆?
            warpChipInventory.deserializeNBT(registries, tag.getCompound("WarpChipInventory"));
        }
        // 鍔熻兘锛氬湪鍖哄潡鍔犺浇/鍚屾鏃舵仮澶嶆帶鍒舵宸茬粡閫夊ソ鐨勮穬杩佺洰鏍囥€?
        controlseatData.warpTargetPos = new BlockPos(tag.getInt("WarpTargetX"), tag.getInt("WarpTargetY"), tag.getInt("WarpTargetZ"));
        controlseatData.warpTargetDimension = tag.getString("WarpTargetDimension");
        controlseatData.warpTargetName = tag.getString("WarpTargetName");
        controlseatData.isWarpPreparing = tag.getBoolean("IsWarpPreparing");
        controlseatData.isviewlocked = tag.getBoolean("IsViewLocked");
    }

    public void tick() {
        Logger LOGGER = LogUtils.getLogger();
        if (level.isClientSide)
            return;
        if (hasInitialized) {
            // 鍔熻兘锛氭瘡 tick 浠庝笘鐣屼腑鐨勭湡瀹炲骇妞呭疄浣撳弽鏌ュ綋鍓嶄箻鍧愮帺瀹讹紝淇閲嶈繘涓栫晫鍚?ride/player 涓㈠け瀵艰嚧鏃犳硶鎺у埗鐨勯棶棰樸€?
            refreshSeatOccupancyFromWorld();

            // 鍔熻兘锛氭瘡 tick 鍒锋柊鎺у埗妞呰嚜韬柟鍧楀潗鏍囷紝渚?warp 鑷姩瀵瑰噯鎶婄洰鏍囦綅缃浆鎹负鎺у埗妞呭綋鍓嶇殑涓栫晫鏈濆悜鍩哄噯銆?
            controlseatData.controlSeatPos = getBlockPos();

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

        //鎶ょ浘
        if(controlseatData.isshieldon) {//濡傛灉鎶ょ浘寮€鍚?
            updateShieldEnergyAvalible();
            int currentcooldown = (int) controlseatData.shieldcooldowntime;
            if(controlseatData.shieldcooldowntime <= 0) {
                SubLevel sublevel = ServerShipUtils.getSubLevelAtBlockPos(level,this.getBlockPos());
                if (sublevel == null) {
                    return;
                }
                Vec3 center = ServerShipUtils.getStructureCenterWorld(sublevel);
                AABB searchBox = new AABB(this.getBlockPos()).inflate(controlseatData.shieldradius + 3.0); // 澶氭悳涓€鐐癸紝闃叉楂橀€熷疄浣撲竴甯х┛杩囧幓
                // 鏍稿績锛氬彧绛涢€夆€滄病鏈夌敓鍛藉€?+ 閫熷害澶熷揩 + 涓嶆槸鐜╁涔熶笉鏄洈鐢叉灦鈥濅箣绫荤殑瀹炰綋
                Vec3 finalCenter = center;
                level.getEntitiesOfClass(Entity.class, searchBox, entity -> {
                    if (entity.isRemoved() || entity instanceof LivingEntity)
                        return false;

                    // 閫熷害闃堝€硷紝鍙皟锛堝崟浣嶏細鏂瑰潡/鍒伙級
                    double speed = entity.getDeltaMovement().length();
                    if (speed < 0.25) return false; // 澶參鐨勭洿鎺ュ拷鐣ワ紙姣斿婕傛诞鐨勭墿鍝侊級

                    // 璁＄畻鏄惁鏈濇姢鐩鹃鏉?
                    Vec3 toEntity = entity.position().subtract(finalCenter);
                    double dot = entity.getDeltaMovement().normalize().dot(toEntity.normalize());
                    return dot < -0.3; // 瓒婅礋璇存槑瓒婃瀵规姢鐩鹃鏉ワ紙-0.3~0.6 涔嬮棿璋冭妭鎵嬫劅锛?
                }).forEach(entity -> {

                    Vec3 toEntity = entity.position().subtract(finalCenter);
                    double distSq = toEntity.lengthSqr();

                    if (distSq > controlseatData.shieldradius * controlseatData.shieldradius || distSq < 0.25) return;
                    if(controlseatData.avalibleshield>0)
                    {
                        // 鎷︽埅
                        entity.discard();
                        // 绮掑瓙浜ょ偣
                        Vec3 hitDir = toEntity.normalize();
                        Vec3 hitPoint = finalCenter.add(hitDir.scale(controlseatData.shieldradius));
                        ShieldHandler.spawnRippleParticles((ServerLevel) level, hitPoint, finalCenter);

                        // 鍙€夛細鎾斁闊虫晥
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

    //0锛氭帹杩涘櫒 1锛氫富姝﹀櫒 2锛氭姢鐩?3锛氱偖濉?4锛氱數姹?5锛氱噧鏂欑 6锛氬脊鑽锛屽姟蹇呬笉瑕佸啓閿?
    public void updateEnergy() {//avalible锛氬墿浣欏€硷紝闈瀉valible锛氭€诲€?
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
                // 鍏堣涓嬫潵锛屽惊鐜畬浜嗗啀鍒?
                toRemove.add(pos);
            }
        }, 4);
        controlseatData.totalenergystorage = totalenergy;
        controlseatData.avalibleenergy = totalenergyavalible;
        //LogUtils.getLogger().warn("detected total energy:"+controlseatData.totalenergystorage+"avalible:"+controlseatData.avalibleenergy);
        // 寰幆缁撴潫鍚庣粺涓€鍒犻櫎
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 4);
        }
    }

    public void updateThruster() {
        List<Vec3> toRemove = new ArrayList<>();
        // 鍔熻兘锛氭瘡娆℃洿鏂版帹杩涘櫒鍓嶉噸缃€滀笢鍗楄タ鍖椾笂涓嬧€濆叚鏂瑰悜鏈€澶ф帹鍔涙€诲拰锛岄伩鍏嶆部鐢ㄤ笂涓€ tick 缂撳瓨銆?
        float[] facingMaxThrustSum = new float[6];
        // 鍔熻兘锛氱紦瀛樻湰 tick 鍐呬粛鍦ㄧ嚎鐨勬帹杩涘櫒鍒楄〃锛岀粺璁″畬鎴愬悗鍐嶇粺涓€涓嬪彂鈥滃悓鏈濆悜鎬绘帹鍔涒€濄€?
        List<AbstractThrusterBlockEntity> activeThrusters = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractThrusterBlockEntity thruster) {
                Logger LOGGER = LogUtils.getLogger();
                //LOGGER.warn("writing to thrusters:" +blockPos+ "torque:"+controlseatData.getFinaltorque()+"force:"+controlseatData.getFinalforce());
                this.calculatedstrength+=thruster.getMaxThrust();
                // 鍔熻兘锛氭寜鎺ㄨ繘鍣ㄦ柟鍧?FACING 缁熻璇ユ柟鍚戠殑鏈€澶ф帹鍔涙€诲拰锛堜笢鍗楄タ鍖椾笂涓嬶級銆?
                Direction thrusterFacing = thruster.getBlockState().getValue(BlockStateProperties.FACING);
                int facingIndex = getFacingThrustIndex(thrusterFacing);
                if (facingIndex >= 0) {
                    facingMaxThrustSum[facingIndex] += thruster.getMaxThrust();
                }
                // 鍔熻兘锛氳褰曟帹杩涘櫒瀹炰緥锛屽緟鏂瑰悜鎬绘帹鍔涚粺璁″畬鎴愬悗鍐嶆妸缁撴灉绮剧‘鍥炲啓缁欏搴旀帹杩涘櫒銆?
                activeThrusters.add(thruster);
                this.fuelspendcurrenttick += thruster.fuelconsumptionperthrottle()*thruster.getFuelThrottle();
            } else {
                // 鍏堣涓嬫潵锛屽惊鐜畬浜嗗啀鍒?
                toRemove.add(pos);
            }
        }, 0);
        // 鍔熻兘锛氭妸鈥滃悓鏈濆悜鎺ㄨ繘鍣ㄦ€绘帹鍔涒€濅笌鎺у埗杈撳叆涓€璧蜂笅鍙戠粰姣忎釜鎺ㄨ繘鍣紝渚涘叾璁＄畻鍔涜础鐚潈閲嶃€?
        for (AbstractThrusterBlockEntity thruster : activeThrusters) {
            Direction thrusterFacing = thruster.getBlockState().getValue(BlockStateProperties.FACING);
            int facingIndex = getFacingThrustIndex(thrusterFacing);
            double sameFacingSum = facingIndex >= 0 ? facingMaxThrustSum[facingIndex] : thruster.getMaxThrust();
            thruster.setdata(controlseatData.getFinaltorque(), controlseatData.getFinalforce(), sameFacingSum);
        }
        controlseatData.thruster_strength = this.calculatedstrength;
        // 鍔熻兘锛氬皢鈥滀笢鍗楄タ鍖椾笂涓嬧€濆叚鏂瑰悜鎺ㄥ姏缁熻缁撴灉鍐欏叆鎺у埗妞呮湇鍔＄鏁版嵁锛屼緵鍚庣画閫昏緫璇诲彇銆?
        controlseatData.facingMaxThrustSum = facingMaxThrustSum;
        // 寰幆缁撴潫鍚庣粺涓€鍒犻櫎
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 0);
        }
    }

    // 鍔熻兘锛氭妸 Direction 鏄犲皠鍒版帹鍔涚粺璁℃暟缁勭储寮曪紙涓?銆佸崡1銆佽タ2銆佸寳3銆佷笂4銆佷笅5锛夈€?
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
        // 姣?tick 閮藉悜姝﹀櫒鍚屾涓€娆″紑鐏姸鎬佷笌棰戦亾锛岄伩鍏嶅洜涓㈠寘/鐘舵€佷笉鍚屾瀵艰嚧鈥滃乏閿寜涓嬩絾姝﹀櫒涓嶅彂灏勨€濄€?
        // 浠呭湪鐘舵€佸彉鍖栨椂鎵嶅悓姝ヤ細鍑虹幇姝﹀櫒绔€氶亾琚噸缃悗鏃犳硶鑷姩鎭㈠鐨勯棶棰樸€?
        previousfirestatus = controlseatData.isfiring;
        // 鍔熻兘锛氭彁鍓嶈绠楁帶鍒舵褰撳墠婵€娲婚閬撶紪鐮侊紝鍚庣画鐢ㄤ簬鈥滄鍣?棰戦亾鍖归厤鈥濅笌鍚屾寮€鐏緭鍏ャ€?
        int activeSeatChannelEncode = 0;
        if (controlseatData.getChannel1()) activeSeatChannelEncode |= 1;
        if (controlseatData.getChannel2()) activeSeatChannelEncode |= 2;
        if (controlseatData.getChannel3()) activeSeatChannelEncode |= 4;
        if (controlseatData.getChannel4()) activeSeatChannelEncode |= 8;

        List<ActiveWeaponHudInfo> activeWeaponHudInfos = new ArrayList<>();
        List<Vec3> toRemove = new ArrayList<>();
        int finalActiveSeatChannelEncode = activeSeatChannelEncode;
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractWeaponBlockEntity weapon) {
                // 鍔熻兘锛氭鍣ㄩ€氳繃鑷韩棰戦亾閰嶇疆鈥滃憡鐭モ€濇帶鍒舵鏄惁灞炰簬褰撳墠婵€娲婚閬撱€?
                if (isWeaponInAnyActiveChannel(weapon, finalActiveSeatChannelEncode)) {
                    // 鍔熻兘锛氶噰闆嗘鍣ㄥ悕绉颁笌鍐峰嵈杩涘害锛屼緵瀹㈡埛绔粯鍒垛€滄补闂ㄦ牱寮忊€濊繘搴︽潯銆?
                    activeWeaponHudInfos.add(new ActiveWeaponHudInfo(weapon.getDisplayName().getString(), weapon.currentTick, weapon.getcooldown()));
                }

                // 鍔熻兘锛氭寜褰撳墠寮€鐏姸鎬佸悜姝﹀櫒鍚屾鎺у埗妞呴閬撹緭鍏ャ€?
                if (controlseatData.isfiring) {
                    weapon.receivechannel(finalActiveSeatChannelEncode);
                } else {
                    weapon.receivechannel(0);
                }
            } else {
                // 鍏堣涓嬫潵锛屽惊鐜畬浜嗗啀鍒?
                toRemove.add(pos);
            }
        }, 1);

        // 鍔熻兘锛氭洿鏂版湇鍔＄缂撳瓨鐨勬縺娲绘鍣?HUD 鏁版嵁锛屼緵鐘舵€佸寘鍚屾鍒?HUD銆?
        controlseatData.activeWeaponHudInfos = activeWeaponHudInfos;

        // 寰幆缁撴潫鍚庣粺涓€鍒犻櫎
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 1);
        }
    }

    // 鍔熻兘锛氬垽鏂鍣ㄦ槸鍚﹂厤缃湪鎺у埗妞呭綋鍓嶆縺娲婚閬撲腑鐨勪换涓€棰戦亾銆?
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
                // 鍏堣涓嬫潵锛屽惊鐜畬浜嗗啀鍒?
                toRemove.add(pos);
            }
        }, 2);
        // 寰幆缁撴潫鍚庣粺涓€鍒犻櫎
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
        // 鍔熻兘锛氬厛璁＄畻鎺у埗妞呭綋鍓嶆縺娲婚閬撶紪鐮侊紝鐢ㄤ簬鍚戦噸鍨嬬偖濉斿悓姝ヤ笌涓绘鍣ㄤ竴鑷寸殑棰戦亾杈撳叆銆?
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
                    // 鍔熻兘锛氭帶鍒舵鏇存柊閲嶅瀷鐐鏃讹紝鍚屾棰戦亾杈撳叆锛涗粎鍦ㄥ紑鐏椂涓嬪彂婵€娲婚閬撱€?
                    if (controlseatData.isfiring) {
                        heavyturret.channelFromCtrl(finalActiveSeatChannelEncode);
                    } else {
                        heavyturret.channelFromCtrl(0);
                    }
                    //鑷姩妯″紡鐩存帴鏇存柊鐩爣锛屾棤瑙嗙帺瀹朵綅缃?
                    if (!controlseatData.enemyshipsData.isEmpty() && heavyturret.needupdateenemy()) {
                        int targetIndex = Math.floorMod(controlseatData.lockedenemyindex, controlseatData.enemyshipsData.size());
                        heavyturret.updatespecificenemy(controlseatData.enemyshipsData.get(targetIndex));
                    }
                    //鎵嬪姩妯″紡鏇存柊鐜╁浣嶇疆锛岃€屼笉鏄晫瀵圭洰鏍?
                    else{
                        // 鍔熻兘锛氬悓姝ョ帺瀹惰瑙掗攣鐘舵€侊紝骞剁洿鎺ヤ笅鍙戝鎴风璁＄畻鐨勬墜鍔ㄧ瀯鍑嗙洰鏍囩偣缁欓噸鍨嬬偖濉斻€?
                        heavyturret.updateplayerstatus(
                                controlseatData.isviewlocked,
                                new Vector3d(controlseatData.manualAimTargetX, controlseatData.manualAimTargetY, controlseatData.manualAimTargetZ)
                        );
                    }
                }
            } else {
                // 鍏堣涓嬫潵锛屽惊鐜畬浜嗗啀鍒?
                toRemove.add(pos);
            }
        }, 3);
        // 寰幆缁撴潫鍚庣粺涓€鍒犻櫎
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
                if(getFuelProperties(fluid.getFluid()) == null) {
                    totalfuel += fueltank.getFluidTank().getCapacity();
                    controlseatData.totalfuelstorage = totalfuel;
                    return;
                }
                float consumptionmultiplier = getFuelProperties(fluid.getFluid()).consumptionMultiplier;
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
                // 鍏堣涓嬫潵锛屽惊鐜畬浜嗗啀鍒?
                toRemove.add(pos);
            }
        }, 5);
        controlseatData.totalfuelstorage = totalfuel;
        controlseatData.avaliblefuel = totalfuelavalible;
        //LogUtils.getLogger().warn("detected total energy:"+controlseatData.totalenergystorage+"avalible:"+controlseatData.avalibleenergy);
        // 寰幆缁撴潫鍚庣粺涓€鍒犻櫎
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 5);
        }
    }

    public void updateScreen(){
        // 鍔熻兘锛氭瘡 tick 鍒锋柊鎺у埗妞呬笘鐣屽潗鏍囷紝渚涢浄杈炬姇褰变娇鐢ㄣ€?
        refreshWorldPosition();
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractScreenBlockEntity screen) {
                // 鍔熻兘锛氬綋灞忓箷灏氭湭缁戝畾鐜╁鏃讹紝缁戝畾褰撳墠鎺у埗妞呯帺瀹躲€?
                if (!screen.hasRadarPlayer() && controlseatData.getPlayer() != null) {
                    screen.setRadarPlayerUuid(controlseatData.getPlayer().getUUID());
                }
                // 鍔熻兘锛氭寔缁悜灞忓箷鍚屾鎺у埗妞呬笘鐣屽潗鏍囷紝淇濊瘉闆疯揪涓績鐐瑰疄鏃舵洿鏂般€?
                screen.setRadarControlSeatWorldPos(new Vector3d(currentworldpos));
                return;
            }
            // 鍔熻兘锛氭竻鐞嗗凡缁忓け鏁堟垨琚浛鎹㈢殑灞忓箷閾炬帴銆?
            toRemove.add(pos);
        }, 7);
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 7);
        }
    }

    // 鍔熻兘锛氫豢鐓х偖濉旈€昏緫锛屽埛鏂版帶鍒舵涓栫晫鍧愭爣锛堜笉鍦ㄨ埞涓婁负 blockpos锛屽湪鑸逛笂杞笘鐣屽潗鏍囷級銆?
    public void refreshWorldPosition() {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        if (subLevel != null) {
            if (subLevel instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel serverSubLevel) {
                controlseatData.serverShip = serverSubLevel;
            }
            Vec3 worldPos = subLevel.logicalPose().transformPosition(Vec3.atLowerCornerOf(this.getBlockPos()));
            currentworldpos = new Vector3d(worldPos.x, worldPos.y, worldPos.z);
            return;
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

        if (!force && player.getVehicle() instanceof ControlSeatMountEntity seat && seats.contains(seat)) {
            //player.displayClientMessage(Component.literal("already sitting, returning true"), true);
            return true;
        }

        ServerLevel serverLevel = (ServerLevel) player.level();
        controlseatData.setPlayer(player);
        //LOGGER.warn(String.valueOf(Component.literal("seated player detected:"+controlseatData.getPlayer()+" uuid:"+controlseatData.getPlayer().getUUID())));
        return startRiding(force, getBlockPos(), getBlockState(), serverLevel);
    }


    // 鍦ㄧЩ闄ゅ骇妞呮椂娓呴櫎鎺у埗璁板綍
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
        // 绉婚櫎鐜╁鐨?UUID 璁板綍
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

    // 淇敼 startRiding 鏂规硶锛岀‘淇濇瘡涓骇妞呮帶鍒朵笌鐜╁ UUID 鐩稿叧鑱?
    public boolean startRiding(boolean force, BlockPos blockPos, BlockState state, ServerLevel level) {
        Player player = controlseatData.getPlayer();
        Initialize.initialize(level,blockPos,state);
        // 浣跨敤鐜╁鐨?UUID 鏉ョ‘瀹氬摢涓帺瀹跺湪杩欎釜搴ф涓?
        // 娓呯悊绌虹殑搴ф
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

    // 鍔熻兘锛氭牴鎹帶鍒舵鏂瑰潡鏈濆悜璁＄畻 ControlSeatMountEntity 搴斿湪鐨勬寕杞藉潗鏍囷紝渚涢噸杩炲悗鐨勫骇妞呭疄浣撴壂鎻忓鐢ㄣ€?
    private Vec3 getSeatMountPosition(BlockPos pos, BlockState state) {
        return ControlSeatMountEntity.getSeatMountPosition(pos, state);
    }

    // 鍔熻兘锛氬湪鏈嶅姟绔?tick 涓噸寤衡€滄帶鍒舵 -> 搴ф瀹炰綋 -> 鐜╁鈥濆叧绯伙紝淇濊瘉鐜╁閲嶈繘鍚?HUD 涓庤緭鍏ラ摼璺嚜鍔ㄦ仮澶嶃€?
    private void refreshSeatOccupancyFromWorld() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState state = getBlockState();
        Vec3 mountPos = getSeatMountPosition(getBlockPos(), state);
        AABB searchBox = new AABB(mountPos, mountPos).inflate(1.25D, 1.25D, 1.25D);

        // 鍔熻兘锛氬厛鎸夆€滄槸鍚﹁繕娲荤潃鈥濇竻鐞嗘棫缂撳瓨锛岄槻姝繚瀛樹簡澶辨晥 seat UUID 褰卞搷 HUD 鍙嶆煡銆?
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

        // 鍔熻兘锛氭妸涓栫晫涓殑瀹炴椂涔樺潗鐘舵€佸洖鍐欏埌 controlseatData锛岄伩鍏嶉噸杩涘悗琚綋浣溾€滄棤浜烘帶鍒垛€濊€?reset銆?
        if (seatedPlayer != null) {
            ride = true;
            controlseatData.setPlayer(seatedPlayer);
        } else {
            ride = false;
            controlseatData.setPlayer(null);
            // 鍔熻兘锛氭棤浜轰箻鍧愭椂榛樿瑙ｉ攣瑙嗚锛岄槻姝㈡棫鐜╁鐣欏湪閿佸畾鎬佸奖鍝嶅悗缁箻鍧愯€呬綋楠屻€?
            controlseatData.isviewlocked = false;
        }
    }

    public FluidThrusterProperties getFuelProperties(Fluid fluid) {
        return ThrusterFuelManager.getProperties(fluid);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }
}
