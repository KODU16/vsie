package com.kodu16.vsie.content.vectorthruster;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.thruster.Initialize;
import com.mojang.logging.LogUtils;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Matrix3d;
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

import static com.kodu16.vsie.foundation.Vec.toVector3d;

public abstract class AbstractVectorThrusterBlockEntity extends AbstractThrusterBlockEntity implements GeoBlockEntity {


    public AbstractVectorThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static SerializableDataTicket<Double> FINAL_SPIN;
    public static SerializableDataTicket<Double> FINAL_PITCH;
    public static SerializableDataTicket<Boolean> IS_SPINNING;
    public double spinrad = 0.0;
    public double pitchrad = 0.0;
    public static float MAX_GIMBAL_ANGLE = 30;

    public static long attachedShipId;


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    @Override
    public void tick() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) { return; }
        Logger LOGGER = LogUtils.getLogger();

        if (hasInitialized) {
            BlockPos pos = this.getBlockPos();
            boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, pos);
            ServerShip ship = VSGameUtilsKt.getShipManagingPos((ServerLevel) level, getBlockPos());

            if (onShip && ship != null) {
                final ShipTransform transform = ship.getTransform();

                // 船的重心（世界坐标）和推进器的位置（世界坐标），计算出力臂（单位化的）
                Vector3dc shipCenterOfMass = transform.getPositionInWorld();
                Vector3d thrusterWorldPos = toVector3d(VSGameUtilsKt.toWorldCoordinates(level, pos));
                Vector3d leverArmWorld = thrusterWorldPos.sub(shipCenterOfMass.x(), shipCenterOfMass.y(), shipCenterOfMass.z());
                leverArmWorld.normalize();

                //获取基座朝向并计算基座轴的世界朝向（单位化的）
                BlockState state = this.getBlockState();

                Vector3d blockdirection = VectorConversionsMCKt.toJOMLD(state.getValue(FACING).getNormal());
                Vector3d worldfacing = new Vector3d();

                transform.getShipToWorld().transformDirection(blockdirection, worldfacing);
                worldfacing.normalize();

                // ==================== 玩家输入 ====================
                Vector3d desiredForce = thrusterData.getInputforce() != null ? thrusterData.getInputforce() : new Vector3d();
                Vector3d desiredTorque = thrusterData.getInputtorque() != null ? thrusterData.getInputtorque() : new Vector3d();

                boolean hasInput = desiredForce.lengthSquared() > 1e-6 || desiredTorque.lengthSquared() > 1e-6;

                double throttle = 0.0;
                // 功能：每个矢量推进器每 tick 都独立计算自己的目标欧拉角，默认保持中位（不偏转）
                double[] eulerAngle={0,0};
                if (hasInput) {
                    Vector3d worldXDirection = new Vector3d();
                    Vector3d worldYDirection = new Vector3d();
                    Vector3d worldZDirection = new Vector3d();
                    Vector3d torqueforce = desiredTorque.cross(leverArmWorld);
                    Vector3d targetthrust = torqueforce.add(desiredForce);
                    targetthrust.normalize();

                    transform.getShipToWorld().transformDirection(thrusterData.getDirectionY(), worldYDirection);
                    worldYDirection.normalize();
                    transform.getShipToWorld().transformDirection(thrusterData.getDirectionX(), worldXDirection);
                    worldXDirection.normalize();
                    transform.getShipToWorld().transformDirection(thrusterData.getDirectionZ(), worldZDirection);
                    worldZDirection.normalize();

                    // 功能：有输入时按目标推力方向计算喷口偏转
                    eulerAngle = forceTransform(targetthrust,transform,thrusterData.getCoordAxis());

                    setChanged();
                    // 日志调试
                    //LOGGER.info("VectorThruster {}  worldY={}, worldX={}, worldZ={}, desiredVec={}, spin={}°, pitch={}°",
                    //        getBlockPos(), worldYDirection, worldXDirection, worldZDirection, targetthrust, spinrad, pitchrad);
                }

                // 功能：无输入时保持默认中位，避免未绑定推进器出现异常偏转
                if(!hasInput){
                    eulerAngle = new double[]{0,0};
                }

                this.spinrad = eulerAngle[0];   //yaw
                this.pitchrad = eulerAngle[1];  //pitch
                // 更新数据
                thrusterData.setThrottle((float) throttle);
                setAnimData(FINAL_SPIN, spinrad);
                setAnimData(FINAL_PITCH, pitchrad);

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
        tag.putDouble("rotx",this.pitchrad);
        tag.putDouble("roty",this.spinrad);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("rotx")) {
            this.pitchrad = tag.getDouble("rotx");
        }
        if (tag.contains("roty")) {
            this.spinrad = tag.getDouble("roty");
        }
    }

    public double getSpinrad() {return this.spinrad;}

    // 功能：返回喷口俯仰角（弧度）供渲染读取
    public double getPitchrad() {return this.pitchrad;}

    @Override
    public void onLoad() {
        super.onLoad();
        markUpdated();
    }

    //A在B和C的平面上投影与B的夹角
    public static double projectionAngleToB_rad_signed(
            Vector3d A,
            Vector3d B_unit,    // 已单位化
            Vector3d C_unit     // 已单位化，不与 B 平行
    ) {
        // 1. 构造平面内的正交基
        Vector3d u = new Vector3d(B_unit);

        // 计算 C 在 B 方向上的投影长度
        double cProjLen = C_unit.dot(u);

        // v = C - (C·u) u
        Vector3d v = new Vector3d(C_unit).sub(u.x * cProjLen, u.y * cProjLen, u.z * cProjLen);

        // 单位化 v
        double vLen = v.length();
        v.div(vLen);   // 现在 v 是单位向量，且垂直于 u
        // 2. 计算 A 在平面上的投影（其实就是原点到 A 的向量在平面上的分量）
        double x = A.dot(u);   // 在 B 方向上的分量
        double y = A.dot(v);   // 在垂直方向上的分量（v 方向）
        // 3. 用 atan2 得到带符号角度（弧度）
        double angleRad = Math.atan2(y, x);

        return angleRad;
    }

    // 输入你想要的加力方向 所在船的transform 以及模型自身的CoordAxis
    // 吐出模型应该转的方向
    public static double[] forceTransform(
            Vector3d forceInWorld,
            ShipTransform transform,
            Matrix3d modelCoordAxis
    ){
        Vector3d forceInShip=transform.getWorldToShip().transformDirection(forceInWorld);
        Vector3d forceInModel=modelCoordAxis.transform(forceInShip);

        // 诡异的坐标变换 根据模型来的
        double yaw = Math.atan2(
                -forceInModel.x,
                forceInModel.z
        );
        double pitch=Math.atan2(
                Math.sqrt(forceInModel.x * forceInModel.x + forceInModel.z * forceInModel.z),
                forceInModel.y
        );

        return new double[]{yaw,pitch};
    }
}
