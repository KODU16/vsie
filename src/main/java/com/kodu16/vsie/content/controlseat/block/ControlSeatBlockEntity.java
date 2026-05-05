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
    private HolderLookup.Provider nbtRegistries;

    // 閸旂喕鍏橀敍姘辩处鐎涙ɑ甯堕崚鑸殿槳瑜版挸澧犳稉鏍櫕閸ф劖鐖ｉ敍灞肩返鐏炲繐绠烽梿鐤彧娴ｈ法鏁ら妴?
    private Vector3d currentworldpos = new Vector3d();


    //閸楀厖濞囬幋鎴滅瑝閹啿鍟撻惃鍕箹娑斿牊浼撹箛鍐跨礉娑撹桨绨＄捄銊ф樊鎼达附鍨滄潻妯绘Ц瀵版鍏?
    //閺堝琚辨稉鐚shmap閿涘瞼顑囨禍灞奸嚋閺勵垯璐熸禍鍡樿閺屾弻UD閻ㄥ嫭妞傞崐娆戞暏閺夈儱寮介弻顧﹐ntrolseat
    private List<ControlSeatMountEntity> seats = new ArrayList<>();
    private final ServerShipHandler serverShipHandler;

    public SmartFluidTankBehaviour tank;

    // 閸旂喕鍏橀敍姘礋 Shift+閸欐娊鏁幍鎾崇磻閻ㄥ嫭甯堕崚鑸殿槳娑撴挾鏁?GUI 閹绘劒绶?27 閺?warp data chip 鐎涙ê鍋嶉妴?
    private final ItemStackHandler warpChipInventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 閸旂喕鍏橀敍姘舵閸掕埖甯堕崚鑸殿槳娴犳挷缍呴崣顏呭复閺€?warp data chip閵?
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


    //閸忓牊甯撮弨绂糽ient閺囧瓨鏌婇敍灞藉建client閸氭垶婀囬崝锛勵伂閸欐垵瀵?
    public void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        BlockPos pos = getBlockPos();
        // 閸欘亝婀佽ぐ鎾存拱閸︽壆甯虹€硅泛姘ㄩ弰顖濈箹瀵姴楠囧鍛畱娑旀ê顓归弮鑸靛閻㈢喐鏅?
        //鏉╂瑦妲告稉顏堟饯閹焦鏌熷▔鏇礉閺堚偓婵傝姤褰侀崜宥団€樼€规艾銈芥担鐘叉躬server鐎涙ê銈芥禍鍡曠铂娑撳﹣绔村▎锛勬畱姒х姵鐖ｆ担宥囩枂閸滃奔绮稉濠佺濞嗏剝鎼锋担婊勬闂?
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
        // 閸旂喕鍏橀敍姘瘮娑斿懎瀵查幒褍鍩楀?GUI 娑擃厼鐡ㄩ弨鍓ф畱 warp data chip閵?
        tag.put("WarpChipInventory", warpChipInventory.serializeNBT(registries));
        // 閸旂喕鍏橀敍姘Ω瑜版挸澧犻柅澶夎厬閻ㄥ嫯绌潻浣烘窗閺嶅洣绔撮獮璺哄晸閸?NBT閿涘奔绻氱拠浣稿隘閸ф宓忔潪钘夋倵 control seat 娴犲秷顔囧妞剧瑓娑撯偓濞喡ょ┈鏉╀礁娼楅弽鍥モ偓?
        tag.putInt("WarpTargetX", controlseatData.warpTargetPos.getX());
        tag.putInt("WarpTargetY", controlseatData.warpTargetPos.getY());
        tag.putInt("WarpTargetZ", controlseatData.warpTargetPos.getZ());
        tag.putString("WarpTargetDimension", controlseatData.warpTargetDimension);
        tag.putString("WarpTargetName", controlseatData.warpTargetName);
        // 閸旂喕鍏橀敍姘倱濮?warp 閸戝棗顦悩鑸碘偓浣稿煂鐎广垺鍩涚粩顖ょ礉鐠佲晜瀵滄稉?P 閺冩儼鍏樺锝団€樼挧鎵斥偓婊冨絿濞戝牆鍣径鍥ｂ偓婵娾偓灞肩瑝閺勵垰鍟€濞嗏€崇磻閼挎粌宕熼妴?
        tag.putBoolean("IsWarpPreparing", controlseatData.isWarpPreparing);
        // 閸旂喕鍏橀敍姘瘮娑斿懎瀵查垾婊嗩潒鐟欐帡鏀ｇ€规埃鈧繂绱戦崗绛圭礉娣囨繆鐦夐悳鈺侇啀闁插秷绻樻稉鏍櫕娑撴柧绮涢崷銊ラ獓濡炲懍绗傞弮璺哄讲閹垹顦查幒褍鍩楅幀浣碘偓?
        tag.putBoolean("IsViewLocked", controlseatData.isviewlocked);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("WarpChipInventory")) {
            // 閸旂喕鍏橀敍姘躬閸栧搫娼￠崝鐘烘祰/閸氬本顒為弮鑸典划婢跺秵甯堕崚鑸殿槳 GUI 娑擃厺绻氱€涙娈?warp data chip閵?
            warpChipInventory.deserializeNBT(registries, tag.getCompound("WarpChipInventory"));
        }
        // 閸旂喕鍏橀敍姘躬閸栧搫娼￠崝鐘烘祰/閸氬本顒為弮鑸典划婢跺秵甯堕崚鑸殿槳瀹歌尙绮￠柅澶娿偨閻ㄥ嫯绌潻浣烘窗閺嶅洢鈧?
        controlseatData.warpTargetPos = new BlockPos(tag.getInt("WarpTargetX"), tag.getInt("WarpTargetY"), tag.getInt("WarpTargetZ"));
        controlseatData.warpTargetDimension = tag.getString("WarpTargetDimension");
        controlseatData.warpTargetName = tag.getString("WarpTargetName");
        controlseatData.isWarpPreparing = tag.getBoolean("IsWarpPreparing");
        controlseatData.isviewlocked = tag.getBoolean("IsViewLocked");
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
            // 閸旂喕鍏橀敍姘槨 tick 娴犲簼绗橀悾灞艰厬閻ㄥ嫮婀＄€圭偛楠囧鍛杽娴ｆ挸寮介弻銉ョ秼閸撳秳绠婚崸鎰负鐎硅绱濇穱顔碱槻闁插秷绻樻稉鏍櫕閸?ride/player 娑撱垹銇戠€佃壈鍤ч弮鐘崇《閹貉冨煑閻ㄥ嫰妫舵０妯糕偓?
            refreshSeatOccupancyFromWorld();

            // 閸旂喕鍏橀敍姘槨 tick 閸掗攱鏌婇幒褍鍩楀鍛板殰闊偅鏌熼崸妤€娼楅弽鍥风礉娓?warp 閼奉亜濮╃€电懓鍣幎濠勬窗閺嶅洣缍呯純顔挎祮閹诡澀璐熼幒褍鍩楀鍛秼閸撳秶娈戞稉鏍櫕閺堟繂鎮滈崺鍝勫櫙閵?
            controlseatData.controlSeatPos = getBlockPos();

            //update
            if (!ride) {
                controlseatData.reset();
                serverShipHandler.resetControlInput();
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

        //閹躲倗娴?
        if(controlseatData.isshieldon) {//婵″倹鐏夐幎銈囨禈瀵偓閸?
            updateShieldEnergyAvalible();
            int currentcooldown = (int) controlseatData.shieldcooldowntime;
            if(controlseatData.shieldcooldowntime <= 0) {
                SubLevel sublevel = ServerShipUtils.getSubLevelAtBlockPos(level,this.getBlockPos());
                if (sublevel == null) {
                    return;
                }
                Vec3 center = ServerShipUtils.getStructureCenterWorld(sublevel);
                if (center == null || linkedShields.isEmpty() || controlseatData.shieldradius <= 0.0D) {
                    return;
                }
                AABB searchBox = new AABB(this.getBlockPos()).inflate(controlseatData.shieldradius + 3.0); // 婢舵碍鎮虫稉鈧悙鐧哥礉闂冨弶顒涙姗€鈧喎鐤勬担鎾茬鐢呪敍鏉╁洤骞?
                // 閺嶇绺鹃敍姘涧缁涙盯鈧鈧粍鐥呴張澶屾晸閸涜棄鈧?+ 闁喎瀹虫径鐔锋彥 + 娑撳秵妲搁悳鈺侇啀娑旂喍绗夐弰顖滄磮閻㈠弶鐏﹂垾婵呯缁崵娈戠€圭偘缍?
                Vec3 finalCenter = center;
                level.getEntitiesOfClass(Entity.class, searchBox, entity -> {
                    if (entity.isRemoved() || entity instanceof LivingEntity)
                        return false;

                    // 闁喎瀹抽梼鍫濃偓纭风礉閸欘垵鐨熼敍鍫濆礋娴ｅ稄绱伴弬鐟版健/閸掍紮绱?
                    double speed = entity.getDeltaMovement().length();
                    if (speed < 0.25) return false; // 婢额亝鍙冮惃鍕纯閹恒儱鎷烽悾銉礄濮ｆ柨顩у鍌涜癁閻ㄥ嫮澧块崫渚婄礆

                    // 鐠侊紕鐣婚弰顖氭儊閺堟繃濮㈤惄楣冾棧閺?
                    Vec3 toEntity = entity.position().subtract(finalCenter);
                    double dot = entity.getDeltaMovement().normalize().dot(toEntity.normalize());
                    return dot < -0.3; // 鐡掑﹨绀嬬拠瀛樻鐡掑﹥顒滅€佃濮㈤惄楣冾棧閺夈儻绱?0.3~0.6 娑斿妫跨拫鍐Ν閹靛鍔呴敍?
                }).forEach(entity -> {

                    Vec3 toEntity = entity.position().subtract(finalCenter);
                    double distSq = toEntity.lengthSqr();

                    if (distSq > controlseatData.shieldradius * controlseatData.shieldradius || distSq < 0.25) return;
                    if(controlseatData.avalibleshield>0)
                    {
                        // 閹凤附鍩?
                        entity.discard();
                        // 缁帒鐡欐禍銈囧仯
                        Vec3 hitDir = toEntity.normalize();
                        Vec3 hitPoint = finalCenter.add(hitDir.scale(controlseatData.shieldradius));
                        ShieldHandler.spawnRippleParticles((ServerLevel) level, hitPoint, finalCenter);

                        // 閸欘垶鈧绱伴幘顓熸杹闂婅櫕鏅?
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

    //0閿涙碍甯规潻娑樻珤 1閿涙矮瀵屽锕€娅?2閿涙碍濮㈤惄?3閿涙氨鍋栨繅?4閿涙氨鏁稿Ч?5閿涙氨鍣ч弬娆戭唸 6閿涙艾鑴婇懡顖滎唸閿涘苯濮熻箛鍛瑝鐟曚礁鍟撻柨?
    public void updateEnergy() {//avalible閿涙艾澧挎担娆忊偓纭风礉闂堢€塿alible閿涙碍鈧鈧?
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
                // 閸忓牐顔囨稉瀣降閿涘苯鎯婇悳顖氱暚娴滃棗鍟€閸?
                toRemove.add(pos);
            }
        }, 4);
        controlseatData.totalenergystorage = totalenergy;
        controlseatData.avalibleenergy = totalenergyavalible;
        //LogUtils.getLogger().warn("detected total energy:"+controlseatData.totalenergystorage+"avalible:"+controlseatData.avalibleenergy);
        // 瀵邦亞骞嗙紒鎾存将閸氬海绮烘稉鈧崚鐘绘珟
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 4);
        }
    }

    public void updateThruster() {
        List<Vec3> toRemove = new ArrayList<>();
        // 閸旂喕鍏橀敍姘槨濞嗏剝娲块弬鐗堝腹鏉╂稑娅掗崜宥夊櫢缂冾喒鈧粈绗㈤崡妤勩偪閸栨ぞ绗傛稉瀣р偓婵嗗彋閺傜懓鎮滈張鈧径褎甯归崝娑欌偓璇叉嫲閿涘矂浼╅崗宥嗛儴閻劋绗傛稉鈧?tick 缂傛挸鐡ㄩ妴?
        float[] facingMaxThrustSum = new float[6];
        // 閸旂喕鍏橀敍姘辩处鐎涙ɑ婀?tick 閸愬懍绮涢崷銊у殠閻ㄥ嫭甯规潻娑樻珤閸掓銆冮敍宀€绮虹拋鈥崇暚閹存劕鎮楅崘宥囩埠娑撯偓娑撳褰傞垾婊冩倱閺堟繂鎮滈幀缁樺腹閸旀稈鈧縿鈧?
        List<AbstractThrusterBlockEntity> activeThrusters = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractThrusterBlockEntity thruster) {
                Logger LOGGER = LogUtils.getLogger();
                //LOGGER.warn("writing to thrusters:" +blockPos+ "torque:"+controlseatData.getFinaltorque()+"force:"+controlseatData.getFinalforce());
                this.calculatedstrength+=thruster.getMaxThrust();
                // 閸旂喕鍏橀敍姘瘻閹恒劏绻橀崳銊︽煙閸?FACING 缂佺喕顓哥拠銉︽煙閸氭垹娈戦張鈧径褎甯归崝娑欌偓璇叉嫲閿涘牅绗㈤崡妤勩偪閸栨ぞ绗傛稉瀣剁礆閵?
                Direction thrusterFacing = thruster.getBlockState().getValue(BlockStateProperties.FACING);
                int facingIndex = getFacingThrustIndex(thrusterFacing);
                if (facingIndex >= 0) {
                    facingMaxThrustSum[facingIndex] += thruster.getMaxThrust();
                }
                // 閸旂喕鍏橀敍姘愁唶瑜版洘甯规潻娑樻珤鐎圭偘绶ラ敍灞界窡閺傜懓鎮滈幀缁樺腹閸旀稓绮虹拋鈥崇暚閹存劕鎮楅崘宥嗗Ω缂佹挻鐏夌划鍓р€橀崶鐐插晸缂佹瑥顕惔鏃€甯规潻娑樻珤閵?
                activeThrusters.add(thruster);
                this.fuelspendcurrenttick += thruster.fuelconsumptionperthrottle()*thruster.getFuelThrottle();
            } else {
                // 閸忓牐顔囨稉瀣降閿涘苯鎯婇悳顖氱暚娴滃棗鍟€閸?
                toRemove.add(pos);
            }
        }, 0);
        // 閸旂喕鍏橀敍姘Ω閳ユ粌鎮撻張婵嗘倻閹恒劏绻橀崳銊︹偓缁樺腹閸旀稈鈧繀绗岄幒褍鍩楁潏鎾冲弳娑撯偓鐠ц渹绗呴崣鎴犵舶濮ｅ繋閲滈幒銊ㄧ箻閸ｎ煉绱濇笟娑樺従鐠侊紕鐣婚崝娑滅閻氼喗娼堥柌宥冣偓?
        for (AbstractThrusterBlockEntity thruster : activeThrusters) {
            Direction thrusterFacing = thruster.getBlockState().getValue(BlockStateProperties.FACING);
            int facingIndex = getFacingThrustIndex(thrusterFacing);
            double sameFacingSum = facingIndex >= 0 ? facingMaxThrustSum[facingIndex] : thruster.getMaxThrust();
            thruster.setdata(controlseatData.getFinaltorque(), controlseatData.getFinalforce(), sameFacingSum);
        }
        controlseatData.thruster_strength = this.calculatedstrength;
        // 閸旂喕鍏橀敍姘殺閳ユ粈绗㈤崡妤勩偪閸栨ぞ绗傛稉瀣р偓婵嗗彋閺傜懓鎮滈幒銊ュ缂佺喕顓哥紒鎾寸亯閸愭瑥鍙嗛幒褍鍩楀鍛箛閸旓紕顏弫鐗堝祦閿涘奔绶甸崥搴ｇ敾闁槒绶拠璇插絿閵?
        controlseatData.facingMaxThrustSum = facingMaxThrustSum;
        // 瀵邦亞骞嗙紒鎾存将閸氬海绮烘稉鈧崚鐘绘珟
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 0);
        }
    }

    // 閸旂喕鍏橀敍姘Ω Direction 閺勭姴鐨犻崚鐗堝腹閸旀稓绮虹拋鈩冩殶缂佸嫮鍌ㄥ鏇礄娑?閵嗕礁宕?閵嗕浇銈?閵嗕礁瀵?閵嗕椒绗?閵嗕椒绗?閿涘鈧?
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
        // 濮?tick 闁棄鎮滃锕€娅掗崥灞绢劄娑撯偓濞嗏€崇磻閻忣偆濮搁幀浣风瑢妫版垿浜鹃敍宀勪缉閸忓秴娲滄稉銏犲瘶/閻樿埖鈧椒绗夐崥灞绢劄鐎佃壈鍤ч垾婊冧箯闁款喗瀵滄稉瀣╃稻濮濓箑娅掓稉宥呭絺鐏忓嫧鈧縿鈧?
        // 娴犲懎婀悩鑸碘偓浣稿綁閸栨牗妞傞幍宥呮倱濮濄儰绱伴崙铏瑰箛濮濓箑娅掔粩顖炩偓姘朵壕鐞氼偊鍣哥純顔兼倵閺冪姵纭堕懛顏勫З閹垹顦查惃鍕６妫版ǜ鈧?
        previousfirestatus = controlseatData.isfiring;
        // 閸旂喕鍏橀敍姘絹閸撳秷顓哥粻妤佸付閸掕埖顦ぐ鎾冲濠碘偓濞插顣堕柆鎾剁椽閻緤绱濋崥搴ｇ敾閻劋绨垾婊勵劅閸?妫版垿浜鹃崠褰掑帳閳ユ繀绗岄崥灞绢劄瀵偓閻忣偉绶崗銉ｂ偓?
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
                // 閸旂喕鍏橀敍姘劅閸ｃ劑鈧俺绻冮懛顏囬煩妫版垿浜鹃柊宥囩枂閳ユ粌鎲￠惌銉⑩偓婵囧付閸掕埖顦弰顖氭儊鐏炵偘绨ぐ鎾冲濠碘偓濞插顣堕柆鎾扁偓?
                if (isWeaponInAnyActiveChannel(weapon, finalActiveSeatChannelEncode)) {
                    // 閸旂喕鍏橀敍姘跺櫚闂嗗棙顒熼崳銊ユ倳缁夐绗岄崘宄板祱鏉╂稑瀹抽敍灞肩返鐎广垺鍩涚粩顖滅帛閸掑灈鈧粍琛ラ梻銊︾壉瀵繆鈧繆绻樻惔锔芥蒋閵?
                    activeWeaponHudInfos.add(new ActiveWeaponHudInfo(weapon.getDisplayName().getString(), weapon.currentTick, weapon.getcooldown()));
                }

                // 閸旂喕鍏橀敍姘瘻瑜版挸澧犲鈧悘顐ゅЦ閹礁鎮滃锕€娅掗崥灞绢劄閹貉冨煑濡炲懘顣堕柆鎾圭翻閸忋儯鈧?
                if (controlseatData.isfiring) {
                    weapon.receivechannel(finalActiveSeatChannelEncode);
                } else {
                    weapon.receivechannel(0);
                }
            } else {
                // 閸忓牐顔囨稉瀣降閿涘苯鎯婇悳顖氱暚娴滃棗鍟€閸?
                toRemove.add(pos);
            }
        }, 1);

        // 閸旂喕鍏橀敍姘纯閺傜増婀囬崝锛勵伂缂傛挸鐡ㄩ惃鍕负濞茬粯顒熼崳?HUD 閺佺増宓侀敍灞肩返閻樿埖鈧礁瀵橀崥灞绢劄閸?HUD閵?
        controlseatData.activeWeaponHudInfos = activeWeaponHudInfos;

        // 瀵邦亞骞嗙紒鎾存将閸氬海绮烘稉鈧崚鐘绘珟
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 1);
        }
    }

    // 閸旂喕鍏橀敍姘灲閺傤厽顒熼崳銊︽Ц閸氾箓鍘ょ純顔兼躬閹貉冨煑濡炲懎缍嬮崜宥嗙负濞插顣堕柆鎾茶厬閻ㄥ嫪鎹㈡稉鈧０鎴︿壕閵?
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
                // 閸忓牐顔囨稉瀣降閿涘苯鎯婇悳顖氱暚娴滃棗鍟€閸?
                toRemove.add(pos);
            }
        }, 2);
        // 瀵邦亞骞嗙紒鎾存将閸氬海绮烘稉鈧崚鐘绘珟
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 2);
        }
        if (linkedShields.isEmpty()) {
            resetShieldStats();
            return;
        }
        double[] minmax = ShieldHandler.getMinMaxDistance(linkedShields);
        double max = minmax[0];
        double min = minmax[1];
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
        controlseatData.shieldmaxcooldowntime = (max/min)*100;
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
                Logger LOGGER = LogUtils.getLogger();
                shield.getEnergy().extractEnergy(eachsubtract,false);
            }
        }, 2);
    }

    public void RegenerateShieldEnergy(int energy) {
        if (energy <= 0 || linkedShields.isEmpty()) {
            return;
        }
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
        // 閸旂喕鍏橀敍姘帥鐠侊紕鐣婚幒褍鍩楀鍛秼閸撳秵绺哄ú濠氼暥闁挾绱惍渚婄礉閻劋绨崥鎴﹀櫢閸ㄥ鍋栨繅鏂挎倱濮濄儰绗屾稉缁橆劅閸ｃ劋绔撮懛瀵告畱妫版垿浜炬潏鎾冲弳閵?
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
                    // 閸旂喕鍏橀敍姘付閸掕埖顦弴瀛樻煀闁插秴鐎烽悙顔碱敊閺冭绱濋崥灞绢劄妫版垿浜炬潏鎾冲弳閿涙稐绮庨崷銊ョ磻閻忣偅妞傛稉瀣絺濠碘偓濞插顣堕柆鎾扁偓?
                    if (controlseatData.isfiring) {
                        heavyturret.channelFromCtrl(finalActiveSeatChannelEncode);
                    } else {
                        heavyturret.channelFromCtrl(0);
                    }
                    //閼奉亜濮╁Ο鈥崇础閻╁瓨甯撮弴瀛樻煀閻╊喗鐖ｉ敍灞炬￥鐟欏棛甯虹€规湹缍呯純?
                    if (!controlseatData.enemyshipsData.isEmpty() && heavyturret.needupdateenemy()) {
                        int targetIndex = Math.floorMod(controlseatData.lockedenemyindex, controlseatData.enemyshipsData.size());
                        heavyturret.updatespecificenemy(controlseatData.enemyshipsData.get(targetIndex));
                    }
                    //閹靛濮╁Ο鈥崇础閺囧瓨鏌婇悳鈺侇啀娴ｅ秶鐤嗛敍宀冣偓灞肩瑝閺勵垱鏅€靛湱娲伴弽?
                    else{
                        // 閸旂喕鍏橀敍姘倱濮濄儳甯虹€规儼顫嬬憴鎺楁敚閻樿埖鈧緤绱濋獮鍓佹纯閹恒儰绗呴崣鎴濐吂閹撮顏拋锛勭暬閻ㄥ嫭澧滈崝銊х€崙鍡欐窗閺嶅洨鍋ｇ紒娆撳櫢閸ㄥ鍋栨繅鏂烩偓?
                        heavyturret.updateplayerstatus(
                                controlseatData.isviewlocked,
                                new Vec3(controlseatData.manualAimTargetX, controlseatData.manualAimTargetY, controlseatData.manualAimTargetZ)
                        );
                    }
                }
            } else {
                // 閸忓牐顔囨稉瀣降閿涘苯鎯婇悳顖氱暚娴滃棗鍟€閸?
                toRemove.add(pos);
            }
        }, 3);
        // 瀵邦亞骞嗙紒鎾存将閸氬海绮烘稉鈧崚鐘绘珟
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
                // 閸忓牐顔囨稉瀣降閿涘苯鎯婇悳顖氱暚娴滃棗鍟€閸?
                toRemove.add(pos);
            }
        }, 5);
        controlseatData.totalfuelstorage = totalfuel;
        controlseatData.avaliblefuel = totalfuelavalible;
        //LogUtils.getLogger().warn("detected total energy:"+controlseatData.totalenergystorage+"avalible:"+controlseatData.avalibleenergy);
        // 瀵邦亞骞嗙紒鎾存将閸氬海绮烘稉鈧崚鐘绘珟
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 5);
        }
    }

    public void updateScreen(){
        // 閸旂喕鍏橀敍姘槨 tick 閸掗攱鏌婇幒褍鍩楀鍛瑯閻ｅ苯娼楅弽鍥风礉娓氭盯娴勬潏鐐瑜板彉濞囬悽銊ｂ偓?
        refreshWorldPosition();
        List<Vec3> toRemove = new ArrayList<>();
        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AbstractScreenBlockEntity screen) {
                // 閸旂喕鍏橀敍姘秼鐏炲繐绠风亸姘弓缂佹垵鐣鹃悳鈺侇啀閺冭绱濈紒鎴濈暰瑜版挸澧犻幒褍鍩楀鍛负鐎硅翰鈧?
                if (!screen.hasRadarPlayer() && controlseatData.getPlayer() != null) {
                    screen.setRadarPlayerUuid(controlseatData.getPlayer().getUUID());
                }
                // 閸旂喕鍏橀敍姘瘮缂侇厼鎮滅仦蹇撶閸氬本顒為幒褍鍩楀鍛瑯閻ｅ苯娼楅弽鍥风礉娣囨繆鐦夐梿鐤彧娑擃厼绺鹃悙鐟扮杽閺冭埖娲块弬鑸偓?
                screen.setRadarControlSeatWorldPos(new Vector3d(currentworldpos));
                return;
            }
            // 閸旂喕鍏橀敍姘閻炲棗鍑＄紒蹇撱亼閺佸牊鍨ㄧ悮顐ｆ禌閹广垻娈戠仦蹇撶闁剧偓甯撮妴?
            toRemove.add(pos);
        }, 7);
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 7);
        }
    }

    // 閸旂喕鍏橀敍姘雹閻撗呭仏婵夋棃鈧槒绶敍灞藉煕閺傜増甯堕崚鑸殿槳娑撴牜鏅崸鎰垼閿涘牅绗夐崷銊ㄥ煘娑撳﹣璐?blockpos閿涘苯婀懜閫涚瑐鏉烆兛绗橀悾灞芥綏閺嶅浄绱氶妴?
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


    // 閸︺劎些闂勩倕楠囧鍛濞撳懘娅庨幒褍鍩楃拋鏉跨秿
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
        // 缁夊娅庨悳鈺侇啀閻?UUID 鐠佹澘缍?
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

    // 娣囶喗鏁?startRiding 閺傝纭堕敍宀€鈥樻穱婵囩槨娑擃亜楠囧鍛付閸掓湹绗岄悳鈺侇啀 UUID 閻╃鍙ч懕?
    public boolean startRiding(boolean force, BlockPos blockPos, BlockState state, ServerLevel level) {
        Player player = controlseatData.getPlayer();
        Initialize.initialize(level,blockPos,state);
        // 娴ｈ法鏁ら悳鈺侇啀閻?UUID 閺夈儳鈥樼€规艾鎽㈡稉顏嗗负鐎硅泛婀潻娆庨嚋鎼囱勵槳娑?
        // 濞撳懐鎮婄粚铏规畱鎼囱勵槳
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

    // 閸旂喕鍏橀敍姘壌閹诡喗甯堕崚鑸殿槳閺傜懓娼￠張婵嗘倻鐠侊紕鐣?ControlSeatMountEntity 鎼存柨婀惃鍕瘯鏉炶棄娼楅弽鍥风礉娓氭盯鍣告潻鐐叉倵閻ㄥ嫬楠囧鍛杽娴ｆ挻澹傞幓蹇擃槻閻劊鈧?
    private Vec3 getSeatMountPosition(BlockPos pos, BlockState state) {
        return ControlSeatMountEntity.getSeatMountPosition(pos, state);
    }

    // 閸旂喕鍏橀敍姘躬閺堝秴濮熺粩?tick 娑擃參鍣稿琛♀偓婊勫付閸掕埖顦?-> 鎼囱勵槳鐎圭偘缍?-> 閻溾晛顔嶉垾婵嗗彠缁紮绱濇穱婵婄槈閻溾晛顔嶉柌宥堢箻閸?HUD 娑撳氦绶崗銉╂懠鐠侯垵鍤滈崝銊︿划婢跺秲鈧?
    private void refreshSeatOccupancyFromWorld() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState state = getBlockState();
        Vec3 mountPos = getSeatMountPosition(getBlockPos(), state);
        AABB searchBox = new AABB(mountPos, mountPos).inflate(1.25D, 1.25D, 1.25D);

        // 閸旂喕鍏橀敍姘帥閹稿鈧粍妲搁崥锕佺箷濞茶崵娼冮垾婵囩閻炲棙妫紓鎾崇摠閿涘矂妲诲顫箽鐎涙ü绨℃径杈ㄦ櫏 seat UUID 瑜板崬鎼?HUD 閸欏秵鐓￠妴?
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

        // 閸旂喕鍏橀敍姘Ω娑撴牜鏅稉顓犳畱鐎圭偞妞傛稊妯烘綏閻樿埖鈧礁娲栭崘娆忓煂 controlseatData閿涘矂浼╅崗宥夊櫢鏉╂稑鎮楃悮顐㈢秼娴ｆ壕鈧粍妫ゆ禍鐑樺付閸掑灈鈧繆鈧?reset閵?
        if (seatedPlayer != null) {
            ride = true;
            controlseatData.setPlayer(seatedPlayer);
        } else {
            ride = false;
            controlseatData.setPlayer(null);
            serverShipHandler.resetControlInput();
            // 閸旂喕鍏橀敍姘￥娴滆桨绠婚崸鎰姒涙顓荤憴锝夋敚鐟欏棜顫楅敍宀勬Щ濮濄垺妫悳鈺侇啀閻ｆ瑥婀柨浣哥暰閹礁濂栭崫宥呮倵缂侇厺绠婚崸鎰偓鍛秼妤犲被鈧?
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
