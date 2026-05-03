package com.kodu16.vsie.content.misc.electromagnet_rail.top;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.List;

public class ElectroMagnetRailTopBlockEntity extends SmartBlockEntity implements GeoBlockEntity {
    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    // 鍔熻兘锛氱紦瀛樹笂涓€甯у乏鍙虫粦杞ㄧ殑 X 鍋忕Щ锛岀敤浜庡鎴风娓叉煋鏃跺仛骞虫粦鎻掑€笺€?
    public float prevRailOffsetX = 0.0f;
    // 鍔熻兘锛氳褰曞綋鍓?top 鏄惁宸茶 core 鎴愬姛妫€娴嬪苟缁戝畾锛岀敤浜庢帶鍒堕楠煎睍寮€/鏀跺洖銆?
    private boolean boundToCore = false;

    public ElectroMagnetRailTopBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    // 鍔熻兘锛氱敱 core 鍦ㄧ粦瀹氱姸鎬佸彉鍖栨椂璋冪敤锛屽悓姝?top 鐨勫睍寮€鐘舵€佸苟瑙﹀彂瀹㈡埛绔埛鏂般€?
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

    // 鍔熻兘锛氭彁渚涚粰妯″瀷灞傚垽鏂綋鍓嶆槸鍚﹂渶瑕佸睍寮€宸﹀彸楠ㄩ銆?
    public boolean isBoundToCore() {
        return this.boundToCore;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        // 鍔熻兘锛氭寔涔呭寲骞跺悓姝?top 鐨勭粦瀹氱姸鎬侊紝淇濊瘉瀹㈡埛绔姩鐢讳笌鏈嶅姟绔竴鑷淬€?
        tag.putBoolean("BoundToCore", this.boundToCore);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("BoundToCore")) {
            // 鍔熻兘锛氳鍙栧悓姝ヨ繃鏉ョ殑缁戝畾鐘舵€侊紝鍦ㄥ鎴风椹卞姩楠ㄩ灞曞紑/鏀跺洖鍔ㄧ敾銆?
            this.boundToCore = tag.getBoolean("BoundToCore");
        }
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
