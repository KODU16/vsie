package com.kodu16.vsie.content.storage.energybattery;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capability;
import net.neoforged.neoforge.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> this.energyStorage);
    private final EnergyStorage energyStorage = new EnergyStorage(
            getcapacity(),    // 最大容量 (capacity)
            Integer.MAX_VALUE,      // 最大接收速率 (max receive)   可以设 Integer.MAX_VALUE 如果想无限制
            Integer.MAX_VALUE,      // 最大输出速率 (max extract)
            0         // 初始能量
    );

    public void tick() {

    }

    public void setLinkedcontrolseatpos(BlockPos pos){
        this.linkedcontrolseatpos = pos;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        // 功能：适配 NeoForge 1.21.1 的能源能力命名空间，继续向外部设备暴露 FE 储能接口。
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();           // 必须！方块实体移除/区块卸载时调用
    }

    @Override
    protected void write(CompoundTag tag, boolean clientpacket) {
        super.write(tag, clientpacket);
        tag.putInt("Energy", getEnergy().getEnergyStored());
        writeVec3(tag, "controlpos", linkedcontrolseatpos);
    }

    @Override
    public void read(CompoundTag tag, boolean clientpacket) {
        super.read(tag, clientpacket);
        if (tag.contains("Energy")) {
            energyStorage.receiveEnergy(tag.getInt("Energy"), false);
        }
        readVec3(tag, "controlpos");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        write(tag, true);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        read(tag, true);
    }

    // 方便外部直接调用（例如 tick、GUI、Waila 等）
    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // 或直接返回 IEnergyStorage 接口
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
        if (nbt.contains(key, Tag.TAG_COMPOUND)) {   // 改成 TAG_COMPOUND 更准确
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
