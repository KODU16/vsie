package com.kodu16.vsie.content.storage.energybattery;

import com.kodu16.vsie.foundation.RelativeBlockPosNbt;

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

    public IEnergyStorage getEnergyCapability() {
        return energyStorage;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.write(tag, registries, clientpacket);
        tag.putInt("Energy", getEnergy().getEnergyStored());
        RelativeBlockPosNbt.write(tag, "controlpos", getBlockPos(), linkedcontrolseatpos);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.read(tag, registries, clientpacket);
        if (tag.contains("Energy")) {
            energyStorage.receiveEnergy(tag.getInt("Energy"), false);
        }
        linkedcontrolseatpos = RelativeBlockPosNbt.read(tag, "controlpos", getBlockPos(), false);
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

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IEnergyStorage getEnergy() {
        return energyStorage;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }
}
