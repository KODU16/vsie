package com.kodu16.vsie.content.storage.fueltank;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.List;

public abstract class AbstractFuelTankBlockEntity extends SmartBlockEntity implements GeoBlockEntity {
    private HolderLookup.Provider nbtRegistries;

    public AbstractFuelTankBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {
        // 濡傛灉鍚庣画瑕佸姞 Create 鐨勬祦浣撴帴鍙ｇ瓑琛屼负锛屽彲浠ュ湪杩欓噷娣诲姞
    }

    // 瀹归噺寤鸿浣跨敤 getCapacity() 鏉ヨ缃紝鑰屼笉鏄啓姝?

    public abstract int getCapacity();

    private final FluidTank fluidTank = new FluidTank(getCapacity()) {
        @Override
        protected void onContentsChanged() {
            setChanged();           // 閲嶈锛氬唴瀹瑰彉鏇存椂鏍囪鑴忔暟鎹?
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return true;            // 鍙寜闇€鏀逛负鍙帴鍙楃壒瀹氭祦浣?
        }
    };

    public BlockPos linkedcontrolseatpos = new BlockPos(0, 0, 0);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public abstract String getFuelTanktype();

    public void tick() {
        // 濡傛灉鏈夋瘡 tick 閫昏緫鍙互鍐欏湪杩欓噷
    }

    public void setLinkedcontrolseatpos(BlockPos pos) {
        this.linkedcontrolseatpos = pos;
        setChanged();
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    // 鍔熻兘锛氭彁渚涚粰 NeoForge 1.21.1 capability 娉ㄥ唽鍣ㄧ殑娴佷綋澶勭悊鍣ㄥ疄渚嬨€?
    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    // 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
    //             鏈€鍏抽敭鐨?NBT 璇诲啓閮ㄥ垎
    // 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

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
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        // 淇濆瓨娴佷綋鏁版嵁锛堟渶鎺ㄨ崘鐨勬柟寮忥級
        CompoundTag fluidTag = new CompoundTag();
        fluidTank.writeToNBT(registries, fluidTag);
        tag.put("Tank", fluidTag);

        // 淇濆瓨鎺у埗搴ф浣嶇疆
        writeVec3(tag, "controlpos", linkedcontrolseatpos);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        // 璇诲彇娴佷綋鏁版嵁
        if (tag.contains("Tank", Tag.TAG_COMPOUND)) {
            CompoundTag fluidTag = tag.getCompound("Tank");
            fluidTank.readFromNBT(registries, fluidTag);
        }

        // 璇诲彇鎺у埗搴ф浣嶇疆
        readVec3(tag, "controlpos");
    }

    // 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
    //          鍚屾鐩稿叧鏂规硶锛堥€氬父淇濇寔杩欐牱鍗冲彲锛?
    // 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

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
        withNbtRegistries(registries, () -> read(tag, registries, true));
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

    // GeckoLib 鐩稿叧
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 濡傛灉鏈夊姩鐢?controller 鍦ㄦ娉ㄥ唽
    }
}
