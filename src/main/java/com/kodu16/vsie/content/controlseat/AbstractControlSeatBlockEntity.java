package com.kodu16.vsie.content.controlseat;


import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractControlSeatBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, GeoBlockEntity {

    // Common State
    protected ControlSeatServerData controlseatData;
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public float calculatedstrength = 0;

    //energy
    public int energyspendpertick = 10;
    public int capacitorenergy = 0;//鎺у埗妞呰褰曠殑褰搕ick闇€瑕佹秷鑰楃殑鐢甸噺锛屽鏋滄娊涓嶅鐢甸噺鍒欏仠鏈?
    public int totalenergy = 100;
    public int totalenergyavalible = 0;//缂撳瓨鐨勮兘閲?

    //fuel
    public int fuelspendcurrenttick = 0;
    public int capacitorfuel = 0;//鎺у埗妞呰褰曠殑褰搕ick闇€瑕佹秷鑰楃殑娌癸紝濡傛灉鎶戒笉澶熸补鍒欏紩鎿庡仠鏈猴紙涓嶅仠鐢碉級
    public int totalfuel = 100;
    public int totalfuelavalible = 0;

    //shield
    public double avalibleshield = 0;//缂撳瓨鐨勬姢鐩捐兘閲?

    //Links(nbt:true)
    private final List<Vec3> linkedThrusters = new ArrayList<>();
    public final List<Vec3> linkedWeapons = new ArrayList<>();
    public final List<Vec3> linkedShields = new ArrayList<>();
    private final List<Vec3> linkedTurrets = new ArrayList<>();
    private final List<Vec3> linkedBatteries = new ArrayList<>();
    public final List<Vec3> linkedFuelTanks = new ArrayList<>();
    public final List<Vec3> linkedAmmoboxes = new ArrayList<>();
    public final List<Vec3> linkedScreens = new ArrayList<>();
    //鎺у埗妞呰繛鐨勭數姹狅紝寮硅嵂搴擄紝鐕冩枡搴?
    //涓嶅姞浜嗭紝鍐嶅姞鏈夌偣澶氫簡



    public AbstractControlSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        controlseatData = new ControlSeatServerData();
    }

    protected abstract boolean isWorking();

    public ControlSeatServerData getControlSeatData() {
        return controlseatData;
    }

    // 鍦ㄧЩ闄ゅ骇妞呮椂娓呴櫎鎺у埗璁板綍
    public abstract void onRemove();

    public abstract String getcontrolseattype();

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.putString("enemy", controlseatData.enemy);
        nbt.putString("ally", controlseatData.ally);

        writeVec3List(nbt, "Thrusters", linkedThrusters);
        writeVec3List(nbt,"Weapons",linkedWeapons);
        writeVec3List(nbt, "Shields", linkedShields);
        writeVec3List(nbt, "Turrets", linkedTurrets);
        writeVec3List(nbt, "Batteries",linkedBatteries);
        writeVec3List(nbt, "Fueltanks", linkedFuelTanks);
        writeVec3List(nbt, "Ammoboxes", linkedAmmoboxes);
        writeVec3List(nbt, "Screens", linkedScreens);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        if(this.controlseatData == null) {
            this.controlseatData = new ControlSeatServerData();
        }
        if(nbt.contains("enemy")) {this.controlseatData.enemy = nbt.getString("enemy");}
        if(nbt.contains("ally")) {this.controlseatData.ally = nbt.getString("ally");}

        linkedThrusters.clear();
        linkedWeapons.clear();
        linkedShields.clear();
        linkedTurrets.clear();
        linkedBatteries.clear();
        linkedFuelTanks.clear();
        linkedAmmoboxes.clear();
        linkedScreens.clear();

        readVec3List(nbt, "Thrusters", linkedThrusters);
        readVec3List(nbt, "Weapons",linkedWeapons);
        readVec3List(nbt, "Shields",linkedShields);
        readVec3List(nbt, "Turrets",linkedTurrets);
        readVec3List(nbt, "Batteries",linkedBatteries);
        readVec3List(nbt, "Fueltanks" ,linkedFuelTanks);
        readVec3List(nbt, "Ammoboxes", linkedAmmoboxes);
        readVec3List(nbt, "Screens",linkedScreens);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        read(tag, registries, true);
    }

    public void setEnemy(String str) {controlseatData.enemy = str;}

    public void setAlly(String str) {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            serverSubLevel.setName(processSlug(serverSubLevel.getName(), str));
        }
        controlseatData.ally = str;
    }

    public void addLinkedPeripheral(Vec3 pos, int type) { //0锛氭帹杩涘櫒 1锛氫富姝﹀櫒 2锛氭姢鐩?3锛氱偖濉?4锛氱數姹?5锛氱噧鏂欑 6锛氬脊鑽锛?锛氬睆骞曪紝鍔″繀涓嶈鍐欓敊
        Logger LOGGER = LogUtils.getLogger();
        if (type == 0 && !linkedThrusters.contains(pos)) {
            linkedThrusters.add(pos);
            LOGGER.warn("adding thruster to controlseat: " + pos);
            setChanged(); // 鏍囪鏂瑰潡瀹炰綋鑴忎簡锛屽己鍒朵繚瀛?
        }
        else if (type == 1 && !linkedWeapons.contains(pos)) {
            linkedWeapons.add(pos);
            LOGGER.warn("adding weapon to controlseat: " + pos);
            setChanged(); // 鏍囪鏂瑰潡瀹炰綋鑴忎簡锛屽己鍒朵繚瀛?
        }
        else if(type == 2 && !linkedShields.contains(pos)) {
            linkedShields.add(pos);
            LOGGER.warn("adding shield to controlseat: " + pos);
            setChanged();
        }

        else if(type == 3 && !linkedTurrets.contains(pos)) {
            linkedTurrets.add(pos);
            LOGGER.warn("adding turret to controlseat: " + pos);
            setChanged();
        }

        else if(type == 4 && !linkedBatteries.contains(pos)) {
            linkedBatteries.add(pos);
            LOGGER.warn("adding battery to controlseat: " + pos);
            setChanged();
        }

        else if(type == 5 && !linkedFuelTanks.contains(pos)) {
            linkedFuelTanks.add(pos);
            LOGGER.warn("adding fueltank to controlseat: " + pos);
            setChanged();
        }
        else if(type == 7 && !linkedScreens.contains(pos)) {
            linkedScreens.add(pos);
            LOGGER.warn("adding screen to controlseat: " + pos);
            setChanged();
        }
    }

    public void removeLinkedPeripheral(Vec3 pos, int type) {
        if (type==0 && linkedThrusters.contains(pos)) {
            linkedThrusters.remove(pos);
            setChanged(); // 鏍囪鏂瑰潡瀹炰綋鑴忎簡锛屽己鍒朵繚瀛?
        }
        else if (type==1 && linkedWeapons.contains(pos)) {
            linkedWeapons.remove(pos);
            setChanged(); // 鏍囪鏂瑰潡瀹炰綋鑴忎簡锛屽己鍒朵繚瀛?
        }
        else if (type==2 && linkedShields.contains(pos)) {
            linkedShields.remove(pos);
            setChanged(); // 鏍囪鏂瑰潡瀹炰綋鑴忎簡锛屽己鍒朵繚瀛?
        }
        else if (type==3 && linkedTurrets.contains(pos)) {
            linkedTurrets.remove(pos);
            setChanged(); // 鏍囪鏂瑰潡瀹炰綋鑴忎簡锛屽己鍒朵繚瀛?
        }
        else if (type==4 && linkedBatteries.contains(pos)) {
            linkedBatteries.remove(pos);
            setChanged(); // 鏍囪鏂瑰潡瀹炰綋鑴忎簡锛屽己鍒朵繚瀛?
        }

        else if (type==5 && linkedFuelTanks.contains(pos)) {
            linkedFuelTanks.remove(pos);
            setChanged(); // 鏍囪鏂瑰潡瀹炰綋鑴忎簡锛屽己鍒朵繚瀛?
        }
        else if (type==7 && linkedScreens.contains(pos)) {
            // 鍔熻兘锛氭敮鎸佺Щ闄ゆ棤鏁堢殑灞忓箷缁戝畾锛岄伩鍏嶆帶鍒舵淇濈暀鑴忛摼鎺ャ€?
            linkedScreens.remove(pos);
            setChanged();
        }
    }

    public void forEachLinkedPeripheral(Consumer<Vec3> action, int type) {
        if(type==0) {
            linkedThrusters.forEach(action);
        }
        if(type==1) {
            linkedWeapons.forEach(action);
        }
        if(type==2) {
            linkedShields.forEach(action);
        }
        if(type==3) {
            linkedTurrets.forEach(action);
        }
        if(type==4) {
            linkedBatteries.forEach(action);
        }
        if(type==5) {
            linkedFuelTanks.forEach(action);
        }
        if(type==7) {
            // 鍔熻兘锛氭彁渚涘睆骞曞璁鹃亶鍘嗚兘鍔涳紝鐢ㄤ簬姣?tick 鍚戝睆骞曞悓姝ラ浄杈炬暟鎹€?
            linkedScreens.forEach(action);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static String processSlug(String a, String b) {
        // 鍖归厤鏍煎紡锛氬紑澶存槸 [浠绘剰鍐呭] + 鍚庨潰浠绘剰瀛楃
        if (a != null && a.matches("^\\[.*?\\].*")) {
            // 鎵惧埌绗竴涓?] 鐨勪綅缃?
            int endIndex = a.indexOf(']');
            if (endIndex != -1) {
                String suffix = a.substring(endIndex + 1);
                return "[" + b + "]" + suffix;
            }
        }
        // 涓嶇鍚?[xxx]yyy 鏍煎紡锛屾垨鑰?a 鏄?null
        return "[" + b + "]" + a;
    }

    private void writeVec3List(CompoundTag nbt, String key, List<Vec3> positions) {
        ListTag list = new ListTag();
        for (Vec3 vec : positions) {
            CompoundTag vecTag = new CompoundTag();
            vecTag.putDouble("x", vec.x);
            vecTag.putDouble("y", vec.y);
            vecTag.putDouble("z", vec.z);
            list.add(vecTag);
        }
        nbt.put(key, list);
    }

    private void readVec3List(CompoundTag nbt, String key, List<Vec3> targetList) {
        if (!nbt.contains(key, Tag.TAG_LIST)) return;

        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag vecTag = list.getCompound(i);
            double x = vecTag.getDouble("x");
            double y = vecTag.getDouble("y");
            double z = vecTag.getDouble("z");
            targetList.add(new Vec3(x, y, z));
        }
    }

}
