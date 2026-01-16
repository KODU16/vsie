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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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

                double spinDegrees = 0.0;
                double pitchDegrees = 0.0;
                double throttle = 0.0;

                if (hasInput) {
                    Vector3d torqueforce = desiredTorque.cross(leverArmWorld);
                    Vector3d targetthrust = torqueforce.add(desiredForce);
                    targetthrust.normalize();
                }

                // 更新数据
                thrusterData.setThrottle((float) throttle);
                setAnimData(FINAL_SPIN, spinDegrees);
                setAnimData(FINAL_PITCH, pitchDegrees);
                setAnimData(IS_SPINNING, hasInput);

                // 日志调试
                LOGGER.info("VectorThruster {} facing {}: throttle={}, spin={}°, pitch={}°",
                        getBlockPos(), worldfacing, throttle, spinDegrees, pitchDegrees);

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
