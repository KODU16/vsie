package com.kodu16.vsie.content.controlseat;


import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.mojang.logging.LogUtils;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractControlSeatBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, GeoBlockEntity {

    // Common State
    protected ControlSeatServerData controlseatData;
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    //Links
    private final List<Vec3> linkedThrusters = new ArrayList<>();
    public final List<AbstractWeaponBlockEntity> WeaponCache = new ArrayList<>();
    private final List<Vec3> linkedShields = new ArrayList<>();
    private final List<Vec3> linkedTurrets = new ArrayList<>();

    //scans
    public QueryableShipData<Ship> qsd = VSGameUtilsKt.getAllShips(level);
    // Ticking

    // Particles


    public AbstractControlSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        controlseatData = new ControlSeatServerData();
    }


    // 修改 tick 方法，在此方法中确保座椅输入与对应玩家的 UUID 匹配

    protected abstract boolean isWorking();

    public ControlSeatServerData getControlSeatData() {
        return controlseatData;
    }

    // 在移除座椅时清除控制记录
    public abstract void onRemove();

    public abstract String getcontrolseattype();

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
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

    public void addLinkedPeripheral(Vec3 pos, int type) { //0：推进器 1：主武器 2：护盾 3：炮塔，务必不要写错
        Logger LOGGER = LogUtils.getLogger();
        if (type == 0 && !linkedThrusters.contains(pos)) {
            linkedThrusters.add(pos);
            LOGGER.warn("adding thruster to controlseat: " + pos);
            setChanged(); // 标记方块实体脏了，强制保存
        }
    }
    public void addWeapon(Vec3 pos, AbstractWeaponBlockEntity weapon) {
        Logger LOGGER = LogUtils.getLogger();
        WeaponCache.add(weapon);
        LOGGER.warn("adding weapon to controlseat: "+pos);
        setChanged(); // 标记方块实体脏了，强制保存
    }

    public void removeLinkedPeripheral(Vec3 pos, int type) {
        if (type==0 && linkedThrusters.contains(pos)) {
            linkedThrusters.remove(pos);
            setChanged(); // 标记方块实体脏了，强制保存
        }
    }

    public void forEachLinkedPeripheral(Consumer<Vec3> action, int type) { //0：推进器 1：主武器 2：护盾 3：炮塔，务必不要写错
        if(type==0) {
            linkedThrusters.forEach(action);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
