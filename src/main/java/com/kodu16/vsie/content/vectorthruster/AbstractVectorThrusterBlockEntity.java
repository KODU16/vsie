package com.kodu16.vsie.content.vectorthruster;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.thruster.Initialize;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.network.SerializableDataTicket;

import java.util.List;

public abstract class AbstractVectorThrusterBlockEntity extends AbstractThrusterBlockEntity implements GeoBlockEntity {


    public AbstractVectorThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public static SerializableDataTicket<Double> FINAL_SPIN;
    public static SerializableDataTicket<Double> FINAL_PITCH;
    public static SerializableDataTicket<Boolean> IS_SPINNING;
    public static float MAX_GIMBAL_ANGLE = 30;


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public abstract String getvectorthrustertype();

    @Override
    public void tick() {
        super.tick();
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        Logger LOGGER = LogUtils.getLogger();

        if (hasInitialized) {
            BlockPos pos = this.getBlockPos();
            boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, pos);
            if (onShip) {
                ServerShip loadedShip = (ServerShip) VSGameUtilsKt.getShipObjectManagingPos(level, pos);
                if (loadedShip == null) return;

                ShipTransform transform = loadedShip.getTransform();

                Vector3d relativePosInShip = VectorConversionsMCKt.toJOMLD(pos)
                        .add(0.5, 0.5, 0.5)
                        .sub(transform.getPositionInShip());

                // 1. 默认推力方向（船坐标系下，假设默认负Z）
                Vector3d baseDirectionShip = thrusterData.getDirection(); // 应为单位向量，如 new Vector3d(0,0,-1)

                // 转为世界坐标的默认方向
                Vector3d baseDirectionWorld = new Vector3d();
                transform.getShipToWorldRotation().transform(baseDirectionShip, baseDirectionWorld);
                baseDirectionWorld.normalize();

                // 2. 计算力矩臂方向（世界坐标）
                Vector3d torqueArmWorld = new Vector3d();
                transform.getShipToWorldRotation().transform(relativePosInShip, torqueArmWorld);

                // 3. 获取玩家输入（世界坐标）
                Vector3d desiredForce = thrusterData.getInputforce() != null ? thrusterData.getInputforce() : new Vector3d(0, 0, 0);
                Vector3d desiredTorque = thrusterData.getInputtorque() != null ? thrusterData.getInputtorque() : new Vector3d(0, 0, 0);

                boolean hasInput = desiredForce.lengthSquared() > 1e-6 || desiredTorque.lengthSquared() > 1e-6;

                double spin = 0.0;  // yaw (spin) 角度，单位：度
                double pitch = 0.0; // pitch 角度，单位：度

                if (hasInput) {
                    // 归一化输入
                    Vector3d normDesiredForce = new Vector3d(desiredForce).normalize();
                    Vector3d normDesiredTorque = new Vector3d(desiredTorque).normalize();

                    // 计算最佳 gimbal 角度（投影法）
                    // 可能的力方向单位球面上的投影
                    double forceProj = baseDirectionWorld.dot(normDesiredForce);

                    // 可能的力矩方向（r × base_dir）
                    Vector3d baseTorqueDir = new Vector3d().cross(torqueArmWorld, baseDirectionWorld).normalize();
                    double torqueProj = baseTorqueDir.dot(normDesiredTorque);

                    // 合并贡献（可调整权重）
                    double totalBaseProj = forceProj + torqueProj;

                    if (Math.abs(totalBaseProj) > 1e-6) {
                        // 需要偏转来更好地匹配目标
                        // 计算所需偏转方向（简化二维 gimbal：spin 控制水平，pitch 控制垂直）

                        // spin (yaw)：围绕局部 Y 轴
                        Vector3d localUp = new Vector3d(0, 1, 0); // 假设推进器局部上方向为 Y
                        Vector3d spinAxisWorld = new Vector3d();
                        transform.getShipToWorldRotation().transform(localUp, spinAxisWorld);

                        // pitch (pitch)：围绕局部 X 轴（右手坐标）
                        Vector3d localRight = new Vector3d(1, 0, 0);
                        Vector3d pitchAxisWorld = new Vector3d();
                        transform.getShipToWorldRotation().transform(localRight, pitchAxisWorld);

                        // 目标方向偏移量
                        Vector3d targetDirOffset = new Vector3d(normDesiredForce).sub(baseDirectionWorld).mul(1.0 / Math.max(forceProj, 0.1));

                        // 计算所需角度（投影到轴上）
                        spin = targetDirOffset.dot(spinAxisWorld.cross(baseDirectionWorld)) * 0.5; // 简化系数
                        pitch = targetDirOffset.dot(pitchAxisWorld.cross(baseDirectionWorld)) * 0.5;

                        // 限制范围
                        spin = Math.max(-MAX_GIMBAL_ANGLE, Math.min(MAX_GIMBAL_ANGLE, spin));
                        pitch = Math.max(-MAX_GIMBAL_ANGLE, Math.min(MAX_GIMBAL_ANGLE, pitch));
                    }
                }

                // 转换为度数发送给动画
                spin = Math.toDegrees(spin);
                pitch = Math.toDegrees(pitch);

                // 发送动画数据（服务器 -> 客户端同步）
                setAnimData(FINAL_SPIN, spin);
                setAnimData(FINAL_PITCH, pitch);
                setAnimData(IS_SPINNING, hasInput);

                // 同时更新 throttle（保留原逻辑）
                Vector3d thrustDirectionWorld = new Vector3d(baseDirectionWorld);

                // 如果有偏转，应用到方向计算 throttle（可选增强）
                Quaterniond gimbalRot = new Quaterniond()
                        .rotateY(Math.toRadians(spin))
                        .rotateX(Math.toRadians(pitch));
                gimbalRot.transform(thrustDirectionWorld);

                thrustDirectionWorld.normalize();

                Vector3d forceContribution = thrustDirectionWorld;

                Vector3d torqueFromThisThruster = new Vector3d().cross(torqueArmWorld, thrustDirectionWorld);
                double torqueLength = torqueFromThisThruster.length();
                if (torqueLength > 1e-6) {
                    torqueFromThisThruster.mul(1.0 / torqueLength);
                } else {
                    torqueFromThisThruster.set(0, 0, 0);
                }

                Vector3d normDesiredForce = desiredForce.length() > 1e-6 ? new Vector3d(desiredForce).normalize() : new Vector3d();
                Vector3d normDesiredTorque = desiredTorque.length() > 1e-6 ? new Vector3d(desiredTorque).normalize() : new Vector3d();

                double forceAlignment = Math.max(0, forceContribution.dot(normDesiredForce));
                double torqueAlignment = Math.max(0, torqueFromThisThruster.dot(normDesiredTorque));

                double totalAlignment = forceAlignment + torqueAlignment; // 可调整权重

                double throttle = Math.max(0.0, Math.min(1.0, totalAlignment));
                thrusterData.setThrottle((float) throttle);

                // LOGGER.info("Thruster {}: spin={:.1f}°, pitch={:.1f}°, throttle={:.2f}", pos, spin, pitch, throttle);
            }
        } else {
            LOGGER.warn(String.valueOf(Component.literal("detected uninitialized vector thruster, time to sweep valkyriie's ass")));
            BlockPos pos = getBlockPos();
            BlockState state = level.getBlockState(pos);
            Initialize.initialize(level, pos, state);
            MinecraftForge.EVENT_BUS.register(this);
            hasInitialized = true;
            LOGGER.warn(String.valueOf(Component.literal("vector thruster Initialize complete:" + pos)));
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
