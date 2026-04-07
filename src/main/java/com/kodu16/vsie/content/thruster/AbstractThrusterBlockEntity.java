package com.kodu16.vsie.content.thruster;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.MinecraftForge;
import org.joml.*;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import javax.annotation.Nonnull;
import java.lang.Math;
import java.util.EventListener;
import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractThrusterBlockEntity extends SmartBlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // Constants
    public ServerShip ship = VSGameUtilsKt.getShipManagingPos((ServerLevel) level, getBlockPos());

    // Common State
    public ThrusterData thrusterData;
    public boolean hasInitialized = false;//值得被写入abstract类被所有人学习！
    public int throttle;//计算消耗油量用

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

    public abstract float getflamewidth();

    //public abstract int getConsumetick();

    // 功能：接收控制椅下发的目标力/力矩，以及“与本推进器同朝向”的最大推力总和。
    public void setdata(Vector3d inputtorque, Vector3d inputforce, double sameFacingMaxThrustSum)
    {
        Logger LOGGER = LogUtils.getLogger();
        thrusterData.setInputtorque(inputtorque);
        thrusterData.setInputforce(inputforce);
        thrusterData.setSameFacingMaxThrustSum(sameFacingMaxThrustSum);
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
                Vector3d thrustDirectionWorld = new Vector3d(thrusterData.getDirectionY());
                transform.getShipToWorldRotation().transform(thrustDirectionWorld); // 只转方向，不转位置
                thrustDirectionWorld.normalize();

                // 2. 力贡献方向（就是推力方向本身）
                Vector3d forceContribution = new Vector3d(thrustDirectionWorld.mul(-1));

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

                // 功能：力分配改为“同朝向推进器按最大推力占比”分摊，同时考虑该方向总推力对当前目标力的需求程度。
                double sameFacingThrust = thrusterData.getSameFacingMaxThrustSum();
                double selfMaxThrust = getMaxThrust();
                // 功能：兜底，避免同向总推力缺失时出现除零，至少用自身最大推力参与计算。
                double safeSameFacingThrust = Math.max(sameFacingThrust, selfMaxThrust);
                // 功能：本推进器在“同朝向推进器组”内的推力占比。
                double sameFacingShare = safeSameFacingThrust > 1e-6 ? (selfMaxThrust / safeSameFacingThrust) : 0;
                // 功能：该朝向上目标力需求占总可用推力的比例（0~1）。
                double forceDemandRatio = desiredForceLen > 1e-6 ? Math.min(1.0, desiredForceLen / safeSameFacingThrust) : 0;
                // 功能：最终力贡献 = 方向匹配度 × 组内推力占比 × 当前需求比例。
                double forceContributionWeighted = forceAlignment * sameFacingShare * forceDemandRatio;

                // 7. 合并力和力矩的贡献（保持力矩相关逻辑不变，仅替换力贡献计算）
                double totalAlignment = forceContributionWeighted + torqueAlignment;

                // 可选：如果你希望纯平动时侧面推进器完全不喷火，可以把 torqueAlignment 权重调高
                // 例如：double totalAlignment = forceAlignment + 2.0 * torqueAlignment;

                // 8. 最终油门 0~1（带平滑防止小抖动）
                double throttle = Math.max(0.0, Math.min(1.0, totalAlignment));

                this.throttle = (int) (throttle*100);

                thrusterData.setThrottle((float) throttle);

                /*LOGGER.warn("Thruster {}: transform={} throttle={} forceAlign={} torqueAlign={} | dir={} localdir={} force={} torque={} relPos={}",
                        pos, Ship.getTransform(), throttle, forceAlignment, torqueAlignment,
                        thrustDirectionWorld, thrusterData.getDirection(), normDesiredForce, normDesiredTorque, relativePosInShip);*/


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
        updateRaycastDistance(level, state, (float) (thrusterData.getThrottle()*getMaxFlameDistance()));
    }

    private void updateRaycastDistance(@Nonnull Level level, @Nonnull BlockState state, float distance) {
        this.raycastDistance = distance;
        setChanged();
        if (!level.isClientSide()) {
            level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    public abstract int fuelconsumptionperthrottle();//每tick，每百分比油门消耗的油量，一秒消耗20次，别拿脚填！


    protected abstract boolean isWorking();

    public int getFuelThrottle(){return this.throttle;}

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

    public abstract String getthrustertype();

    public void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        //if(!this.level.isClientSide()) sendUpdatePacket();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
