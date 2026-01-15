package com.kodu16.vsie.content.vectorthruster;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.thruster.Initialize;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
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
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        Logger LOGGER = LogUtils.getLogger();

        if (hasInitialized) {
            BlockPos pos = this.getBlockPos();
            boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, pos);
            ServerShip ship = VSGameUtilsKt.getShipManagingPos((ServerLevel) level, getBlockPos());
            if (onShip && ship != null) {
                final ShipTransform transform = ship.getTransform();

                // 船的重心（世界坐标）和推进器相对船只本身的相对位置差
                final Vector3dc shipCenterOfMass = transform.getPositionInWorld();
                Vector3d thrusterrelativePos = VectorConversionsMCKt.toJOMLD(pos)
                        .add(0.5, 0.5, 0.5)
                        .sub(shipCenterOfMass);

                Vector3d leverArmWorld = thrusterWorldPos.sub(shipCenterOfMass.x(), shipCenterOfMass.y(), shipCenterOfMass.z());

                // ==================== 获取块朝向并计算局部轴 ====================
                BlockState state = this.getBlockState();
                Direction facing = state.getValue(BlockStateProperties.FACING);

                // 模型局部坐标系的三个轴（默认模型朝 +Y 喷射，+Y 上，-X 右）
                Vector3d localThrust = new Vector3d(0, 1, 0);  // 喷射方向（模型局部）
                Vector3d localUp      = new Vector3d(0, 1,  0);  // spin 轴（绕自身转）
                Vector3d localRight   = new Vector3d(1, 0,  0);  // pitch 轴（上下偏）

                // 根据实际 facing 旋转局部轴到世界坐标
                Quaterniond blockRotation = quaternionFromFacing(facing);

                Vector3d baseDirectionWorld = new Vector3d(localThrust);
                blockRotation.transform(baseDirectionWorld);
                transform.getShipToWorldRotation().transform(baseDirectionWorld); // 再转到世界
                baseDirectionWorld.normalize();

                Vector3d pitchAxisWorld = new Vector3d(localRight);
                blockRotation.transform(pitchAxisWorld);
                transform.getShipToWorldRotation().transform(pitchAxisWorld);
                pitchAxisWorld.normalize();

                // ==================== 玩家输入 ====================
                Vector3d desiredForce = thrusterData.getInputforce() != null ? thrusterData.getInputforce() : new Vector3d();
                Vector3d desiredTorque = thrusterData.getInputtorque() != null ? thrusterData.getInputtorque() : new Vector3d();

                boolean hasInput = desiredForce.lengthSquared() > 1e-6 || desiredTorque.lengthSquared() > 1e-6;

                double spinDegrees = 0.0;
                double pitchDegrees = 0.0;
                double throttle = 0.0;

                if (hasInput) {
                    Vector3d normForce = new Vector3d(desiredForce).normalize();
                    Vector3d normTorque = desiredTorque.lengthSquared() > 1e-6 ? new Vector3d(desiredTorque).normalize() : new Vector3d();

                    // 当前配置最大力矩方向（r × base_dir）
                    Vector3d maxTorqueDir = new Vector3d(leverArmWorld).cross(baseDirectionWorld);
                    double maxTorqueMag = maxTorqueDir.length();
                    if (maxTorqueMag > 1e-6) maxTorqueDir.normalize();

                    // 当前对齐度
                    double forceAlign = Math.max(0, baseDirectionWorld.dot(normForce));
                    double torqueAlign = maxTorqueMag > 1e-6 ? Math.max(0, maxTorqueDir.dot(normTorque)) : 0;
                    double currentAlignment = forceAlign + torqueAlign;

                    // 理想推力方向 = 力和最大力矩方向的加权和
                    Vector3d idealDir = new Vector3d(normForce).mul(1.0).add(maxTorqueDir, new Vector3d().mul(1.0));
                    if (idealDir.lengthSquared() > 1e-6) idealDir.normalize();

                    // 计算从 base 到 ideal 需要的偏转向量（在垂直于 base 的平面内）
                    Vector3d deflection = new Vector3d(idealDir).sub(baseDirectionWorld);
                    // 投影掉沿 baseDirection 的分量（gimbal 无法沿自身轴偏）
                    deflection.sub(baseDirectionWorld.mul(deflection.dot(baseDirectionWorld)));

                    // 投影到两个 gimbal 轴上
                    double spinRad  = deflection.dot(baseDirectionWorld);   // 注意：这里用点积得到带符号的幅度
                    double pitchRad = deflection.dot(pitchAxisWorld);

                    // 限制最大万向节角度
                    double maxRad = Math.toRadians(MAX_GIMBAL_ANGLE);
                    spinRad  = Math.signum(spinRad)  * Math.min(Math.abs(spinRad),  maxRad);
                    pitchRad = Math.signum(pitchRad) * Math.min(Math.abs(pitchRad), maxRad);

                    spinDegrees  = Math.toDegrees(spinRad);
                    pitchDegrees = Math.toDegrees(pitchRad);

                    // ==================== 应用 gimbal 后的实际方向 ====================
                    Vector3d actualDir = new Vector3d(baseDirectionWorld);

                    // 注意旋转顺序
                    Quaterniond gimbal = new Quaterniond()
                            .rotateAxis(spinRad, baseDirectionWorld)    //  spin（绕局部Y）
                            .rotateAxis(pitchRad, pitchAxisWorld);  //  pitch（绕局部X）

                    gimbal.transform(actualDir);
                    actualDir.normalize();

                    // 重新计算实际贡献
                    double actualForceAlign = Math.max(0, actualDir.dot(normForce));

                    Vector3d actualTorqueVec = new Vector3d(leverArmWorld).cross(actualDir);
                    double actualTorqueMag = actualTorqueVec.length();
                    Vector3d actualTorqueNorm = actualTorqueMag > 1e-6 ? actualTorqueVec.normalize() : new Vector3d();
                    double actualTorqueAlign = actualTorqueMag > 1e-6 ? Math.max(0, actualTorqueNorm.dot(normTorque)) : 0;

                    throttle = Math.min(1.0, actualForceAlign + actualTorqueAlign);
                    LOGGER.info("baseDir: {}", baseDirectionWorld);
                    LOGGER.info("pitchAxis: {}", pitchAxisWorld);
                    LOGGER.info("deflection: {}", deflection);
                    LOGGER.info("spinRad: {}, pitchRad: {}", spinRad, pitchRad);
                }

                // 更新数据
                thrusterData.setThrottle((float) throttle);
                setAnimData(FINAL_SPIN, spinDegrees);
                setAnimData(FINAL_PITCH, pitchDegrees);
                setAnimData(IS_SPINNING, hasInput);

                // 日志调试
                LOGGER.info("VectorThruster {} facing {}: throttle={}, spin={}°, pitch={}°",
                        getBlockPos(), facing, throttle, spinDegrees, pitchDegrees);

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

    private static Quaterniond quaternionFromFacing(Direction facing) {
        return switch (facing) {
            case DOWN  -> new Quaterniond().rotateX(Math.toRadians(90));   // +Y 朝下
            case UP    -> new Quaterniond().rotateX(Math.toRadians(-90));
            case NORTH -> new Quaterniond().rotateX(Math.toRadians(180));  // -Z
            case SOUTH -> new Quaterniond();                               // +Z (默认 -Z 喷射时需旋转180)
            case WEST  -> new Quaterniond().rotateY(Math.toRadians(90));
            case EAST  -> new Quaterniond().rotateY(Math.toRadians(-90));
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
