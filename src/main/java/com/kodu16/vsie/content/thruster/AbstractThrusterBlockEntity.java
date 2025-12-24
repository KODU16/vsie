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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.core.jmx.Server;
import org.joml.*;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import javax.annotation.Nonnull;
import java.lang.Math;
import java.util.EventListener;
import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractThrusterBlockEntity extends SmartBlockEntity {
    // Constants
    public ServerShip ship = VSGameUtilsKt.getShipManagingPos((ServerLevel) level, getBlockPos());

    // Common State
    public ThrusterData thrusterData;
    public boolean hasInitialized = false;//值得被写入abstract类被所有人学习！

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
        //LOGGER.warn(String.valueOf(Component.literal("receiving torque:"+thrusterData.getInputtorque()+"force:"+thrusterData.getInputforce())));
    }

    @SuppressWarnings("null")
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
                LoadedShip Ship = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
                if (Ship == null) return;
                final ShipTransform transform = Ship.getTransform();

                //Vector3dc shipCenterOfMassInShip = transform.getShipToWorld().transformPosition((Vector3d) transform.getPositionInShip()); // 世界坐标下的质心（可选）
                Vector3d relativePosInShip = VectorConversionsMCKt.toJOMLD(pos)
                        .add(0.5, 0.5, 0.5)
                        .sub(transform.getPositionInShip()); // 推进器相对于质心的船坐标位置

                // 1. 推进器在世界坐标系下的推力方向（单位向量）
                Vector3d thrustDirectionWorld = new Vector3d(thrusterData.getDirection());
                transform.getShipToWorldRotation().transform(thrustDirectionWorld); // 只转方向，不转位置
                thrustDirectionWorld.normalize();

                // 2. 力贡献方向（就是推力方向本身）
                Vector3d forceContribution = new Vector3d(thrustDirectionWorld);

                // 3. 力矩贡献方向：r × F_dir
                Vector3d torqueFromThisThruster = new Vector3d();
                torqueFromThisThruster.cross(relativePosInShip, thrustDirectionWorld);

                // 归一化力矩方向
                double torqueLength = torqueFromThisThruster.length();
                if (torqueLength > 1e-6) {
                    torqueFromThisThruster.mul(1.0 / torqueLength);
                } else {
                    torqueFromThisThruster.set(0, 0, 0);
                }


                // 5. 获取玩家/电脑输入的世界坐标目标力和目标力矩（如果为null则视为0）
                Vector3d desiredForce = thrusterData.getInputforce() != null ? thrusterData.getInputforce() : new Vector3d(0, 0, 0);
                Vector3d desiredTorque = thrusterData.getInputtorque() != null ? thrusterData.getInputtorque() : new Vector3d(0, 0, 0);

                // 归一化输入（防止数值太大）
                double desiredForceLen = desiredForce.length();
                double desiredTorqueLen = desiredTorque.length();

                Vector3d normDesiredForce = desiredForceLen > 1e-6 ? new Vector3d(desiredForce).mul(1.0 / desiredForceLen) : new Vector3d();
                Vector3d normDesiredTorque = desiredTorqueLen > 1e-6 ? new Vector3d(desiredTorque).mul(1.0 / desiredTorqueLen) : new Vector3d();

                // 6. 计算这个推进器对目标的“贡献度”（点积，越正越有帮助）
                double forceAlignment  = Math.max(0, forceContribution.dot(normDesiredForce));   // 只关心同向贡献
                double torqueAlignment = Math.max(0, torqueFromThisThruster.dot(normDesiredTorque));

                // 7. 合并力和力矩的贡献（你可以自行调整权重，这里力和力矩同等重要）
                double totalAlignment = forceAlignment + torqueAlignment;

                // 可选：如果你希望纯平动时侧面推进器完全不喷火，可以把 torqueAlignment 权重调高
                // 例如：double totalAlignment = forceAlignment + 2.0 * torqueAlignment;

                // 8. 最终油门 0~1（带平滑防止小抖动）
                double throttle = Math.max(0.0, Math.min(1.0, totalAlignment));

                thrusterData.setThrottle((float) throttle);

                //LOGGER.info("Thruster at {} throttle = {:.2f} (force: {:.2f}, torque: {:.2f})", pos, throttle, forceAlignment, torqueAlignment);
                LOGGER.warn("Thruster {}: transform={} throttle={} forceAlign={} torqueAlign={} | dir={} localdir={} force={} torque={} relPos={}",
                        pos, Ship.getTransform(), throttle, forceAlignment, torqueAlignment,
                        thrustDirectionWorld, thrusterData.getDirection(), normDesiredForce, normDesiredTorque, relativePosInShip);


            }
            else{
                LOGGER.warn("thruster not on ship");
            }
            performRaycast(level);
        }
        else {
            LOGGER.warn(String.valueOf(Component.literal("detected uninitialized thruster, time to sweep valkyrie's ass")));
            BlockPos pos = getBlockPos();
            BlockState state = level.getBlockState(pos);
            Initialize.initialize(level, pos, state);
            MinecraftForge.EVENT_BUS.register(this);
            hasInitialized = true;
            LOGGER.warn(String.valueOf(Component.literal("thruster Initialize complete:"+pos)));
        }
    }

    private void performRaycast(@Nonnull Level level) {
        Logger LOGGER = LogUtils.getLogger();
        BlockState state = this.getBlockState();
        //LOGGER.warn(String.valueOf(Component.literal("throttle:"+thrusterData.getThrottle())));
        //LOGGER.warn(String.valueOf(Component.literal("raycastdistance:"+-thrusterData.getThrottle()*getMaxFlameDistance())));
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

    public void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        //if(!this.level.isClientSide()) sendUpdatePacket();
    }
}
