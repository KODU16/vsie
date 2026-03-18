package com.kodu16.vsie.content.misc.electromagnet_rail.top;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.List;

public class ElectroMagnetRailTopBlockEntity extends SmartBlockEntity implements GeoBlockEntity {
    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    // 功能：缓存上一帧左右滑轨的 X 偏移，用于客户端渲染时做平滑插值。
    public float prevRailOffsetX = 0.0f;
    // 功能：记录当前 top 是否已被 core 成功检测并绑定，用于控制骨骼展开/收回。
    private boolean boundToCore = false;

    public ElectroMagnetRailTopBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    // 功能：由 core 在绑定状态变化时调用，同步 top 的展开状态并触发客户端刷新。
    public void setBoundToCore(boolean boundToCore) {
        if (this.boundToCore == boundToCore) {
            return;
        }
        this.boundToCore = boundToCore;
        setChanged();
        if (this.level != null) {
            this.sendData();
        }
    }

    // 功能：提供给模型层判断当前是否需要展开左右骨骼。
    public boolean isBoundToCore() {
        return this.boundToCore;
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        // 功能：持久化并同步 top 的绑定状态，保证客户端动画与服务端一致。
        tag.putBoolean("BoundToCore", this.boundToCore);
    }

    @Override
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("BoundToCore")) {
            // 功能：读取同步过来的绑定状态，在客户端驱动骨骼展开/收回动画。
            this.boundToCore = tag.getBoolean("BoundToCore");
        }
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
