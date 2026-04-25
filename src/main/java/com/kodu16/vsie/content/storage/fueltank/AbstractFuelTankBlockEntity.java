package com.kodu16.vsie.content.storage.fueltank;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capability;
import net.neoforged.neoforge.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class AbstractFuelTankBlockEntity extends SmartBlockEntity implements GeoBlockEntity {

    public AbstractFuelTankBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {
        // 如果后续要加 Create 的流体接口等行为，可以在这里添加
    }

    // 容量建议使用 getCapacity() 来设置，而不是写死

    public abstract int getCapacity();

    private final FluidTank fluidTank = new FluidTank(getCapacity()) {
        @Override
        protected void onContentsChanged() {
            setChanged();           // 重要：内容变更时标记脏数据
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return true;            // 可按需改为只接受特定流体
        }
    };

    private LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> fluidTank);

    public BlockPos linkedcontrolseatpos = new BlockPos(0, 0, 0);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public abstract String getFuelTanktype();

    public void tick() {
        // 如果有每 tick 逻辑可以写在这里
    }

    public void setLinkedcontrolseatpos(BlockPos pos) {
        this.linkedcontrolseatpos = pos;
        setChanged();
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        // 功能：适配 NeoForge 1.21.1 的能力命名空间，继续暴露流体处理能力给桶与管道等交互方。
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return holder.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        holder.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        holder = LazyOptional.of(() -> fluidTank);
    }

    // ───────────────────────────────────────────────
    //             最关键的 NBT 读写部分
    // ───────────────────────────────────────────────

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        // 保存流体数据（最推荐的方式）
        CompoundTag fluidTag = new CompoundTag();
        fluidTank.writeToNBT(fluidTag);
        tag.put("Tank", fluidTag);

        // 保存控制座椅位置
        writeVec3(tag, "controlpos", linkedcontrolseatpos);
    }

    @Override
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        // 读取流体数据
        if (tag.contains("Tank", Tag.TAG_COMPOUND)) {
            CompoundTag fluidTag = tag.getCompound("Tank");
            fluidTank.readFromNBT(fluidTag);
        }

        // 读取控制座椅位置
        readVec3(tag, "controlpos");
    }

    // ───────────────────────────────────────────────
    //          同步相关方法（通常保持这样即可）
    // ───────────────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        write(tag, true);           // 把流体和位置都写进去
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

    // GeckoLib 相关
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 如果有动画 controller 在此注册
    }
}
