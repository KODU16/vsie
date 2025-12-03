package com.kodu16.vsie.content.thruster;


import com.kodu16.vsie.content.controlseat.ShipControlEvent;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.*;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import javax.annotation.Nonnull;
import java.lang.Math;
import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractThrusterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    // Constants
    protected static final int OBSTRUCTION_LENGTH = 10;
    protected static final int TICKS_PER_ENTITY_CHECK = 5;
    protected static final int LOWEST_POWER_THRSHOLD = 5;
    private static final float PARTICLE_VELOCITY = 4;
    private static final double NOZZLE_OFFSET_FROM_CENTER = 0.9;
    private static final double SHIP_VELOCITY_INHERITANCE = 0.5;
    public ServerShip ship = VSGameUtilsKt.getShipManagingPos((ServerLevel) level, getBlockPos());

    // Common State
    protected ThrusterData thrusterData;
    private boolean hasInitialized = false;//值得被写入abstract类被所有人学习！

    private float raycastDistance = 0.0f;//注意，这就是最重要的核心的raycast距离


    public abstract float getMaxFlameDistance();


    public AbstractThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        thrusterData = new ThrusterData();
    }

    public ThrusterData getData()
    {
        return thrusterData;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        /*if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }*/
    }

    public float getRaycastDistance() {
        return raycastDistance;
    }

    public abstract float getZAxisOffset();

    public abstract float getMaxThrust();

    public void setdata(Vector3d inputtorque, Vector3d inputforce)
    {
        Logger LOGGER = LogUtils.getLogger();
        thrusterData.setInputtorque(inputtorque);
        thrusterData.setInputforce(inputforce);
        LOGGER.warn(String.valueOf(Component.literal("receiving torque:"+thrusterData.getInputtorque()+"force:"+thrusterData.getInputforce())));
    }

    // 任意推进器、舵、炮塔、反应堆等方块实体
    @SubscribeEvent
    public void onShipControl(ShipControlEvent event) {
        // 只处理自己这艘船的事件
        if (event.getShip() != this.ship) return;  // this.ship 是你缓存的当前船对象

        // 直接读取最终计算好的值，超级简单
        Vector3d force = event.getForce();
        Vector3d torque = event.getTorque();
        float throttle = event.getThrottle();

        // 根据自己的朝向、位置算局部推力（非常快）
        Vector3d localForce = event.getShip().getTransform().getShipToWorldRotation().transform(force);
        // 或者直接用 event.getRawInput() 自己再算一次也行

        this.setdata(torque, force);
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        Logger LOGGER = LogUtils.getLogger();
        if (hasInitialized)
        {
            BlockPos pos = this.getBlockPos();
            boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, pos);
            if (onShip) {
                LoadedShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
                ShipTransform transform = ship.getTransform();
                Vector3d worldthrusterdirection = new Vector3d();
                transform.getShipToWorld().transformDirection(thrusterData.getDirection(), worldthrusterdirection);
                worldthrusterdirection.normalize();
                Vector3d worldinputforce = thrusterData.getInputforce();
                if (worldinputforce != null) {
                    worldinputforce.normalize();
                    double projectionLength = worldthrusterdirection.dot(worldinputforce);
                    //这应该是一个介于0和1之间的小数
                    //（理论上）
                    thrusterData.setThrottle(projectionLength);
                }
                else {
                    LOGGER.warn(String.valueOf(Component.literal("worldforce:null")));
                    thrusterData.setThrottle(0);
                }
            }
            performRaycast(level);
        }
        else {
            LOGGER.warn(String.valueOf(Component.literal("detected uninitialized thruster, time to sweep valkyrie's ass")));
            BlockPos pos = getBlockPos();
            BlockState state = level.getBlockState(pos);
            Initialize.initialize(level, pos, state);
            hasInitialized = true;
            LOGGER.warn(String.valueOf(Component.literal("thruster Initialize complete:"+pos)));
        }
    }

    private void performRaycast(@Nonnull Level level) {
        Logger LOGGER = LogUtils.getLogger();
        BlockState state = this.getBlockState();
        LOGGER.warn(String.valueOf(Component.literal("throttle:"+thrusterData.getThrottle())));
        LOGGER.warn(String.valueOf(Component.literal("raycastdistance:"+-thrusterData.getThrottle()*getMaxFlameDistance())));
        updateRaycastDistance(level, state, (float) (-thrusterData.getThrottle()*getMaxFlameDistance()));
    }

    private void updateRaycastDistance(@Nonnull Level level, @Nonnull BlockState state, float distance) {
        if (Math.abs(this.raycastDistance - distance) > 0.01f) {
            this.raycastDistance = distance;
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(this.worldPosition, state, state, 3);
            }
        }
    }


    protected abstract boolean isWorking();

    // Networking and nbt

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
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("raycastDistance", this.raycastDistance);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if (tag.contains("raycastDistance", CompoundTag.TAG_FLOAT)) {
            this.raycastDistance = tag.getFloat("raycastDistance");
        } else {
            this.raycastDistance = 0;
        }
    }


}
