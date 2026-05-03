package com.kodu16.vsie.content.storage.energybattery;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;

import java.util.List;

public abstract class AbstractEnergyBatteryBlockEntity extends SmartBlockEntity implements GeoBlockEntity {

    public AbstractEnergyBatteryBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public BlockPos linkedcontrolseatpos = new BlockPos(0,0,0);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public abstract int getcapacity();

    public abstract String getEnergyBatterytype();

    private final EnergyStorage energyStorage = new EnergyStorage(
            getcapacity(),    // 鏈€澶у閲?(capacity)
            Integer.MAX_VALUE,      // 鏈€澶ф帴鏀堕€熺巼 (max receive)   鍙互璁?Integer.MAX_VALUE 濡傛灉鎯虫棤闄愬埗
            Integer.MAX_VALUE,      // 鏈€澶ц緭鍑洪€熺巼 (max extract)
            0         // 鍒濆鑳介噺
    );

    public void tick() {

    }

    public void setLinkedcontrolseatpos(BlockPos pos){
        this.linkedcontrolseatpos = pos;
    }

    // 鍔熻兘锛氭彁渚涚粰 NeoForge 1.21.1 capability 娉ㄥ唽鍣ㄧ殑 FE 鍌ㄨ兘鎺ュ彛瀹炰緥銆?
    public IEnergyStorage getEnergyCapability() {
        return energyStorage;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.write(tag, registries, clientpacket);
        tag.putInt("Energy", getEnergy().getEnergyStored());
        writeVec3(tag, "controlpos", linkedcontrolseatpos);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.read(tag, registries, clientpacket);
        if (tag.contains("Energy")) {
            energyStorage.receiveEnergy(tag.getInt("Energy"), false);
        }
        readVec3(tag, "controlpos");
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

    // 鏂逛究澶栭儴鐩存帴璋冪敤锛堜緥濡?tick銆丟UI銆乄aila 绛夛級
    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // 鎴栫洿鎺ヨ繑鍥?IEnergyStorage 鎺ュ彛
    public IEnergyStorage getEnergy() {
        return energyStorage;
    }

    private void writeVec3(CompoundTag nbt, String key, BlockPos position) {
        CompoundTag vecTag = new CompoundTag();
        vecTag.putInt("x", position.getX());
        vecTag.putInt("y", position.getY());
        vecTag.putInt("z", position.getZ());
        nbt.put(key, vecTag);
    }

    private void readVec3(CompoundTag nbt, String key) {
        if (nbt.contains(key, Tag.TAG_COMPOUND)) {   // 鏀规垚 TAG_COMPOUND 鏇村噯纭?
            CompoundTag vecTag = nbt.getCompound(key);
            int x = vecTag.getInt("x");
            int y = vecTag.getInt("y");
            int z = vecTag.getInt("z");
            this.linkedcontrolseatpos = new BlockPos(x, y, z);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }
}
