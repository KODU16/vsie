package com.kodu16.vsie.content.vectorthruster;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.thruster.Initialize;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;

import java.util.List;

public abstract class AbstractVectorThrusterBlockEntity extends AbstractThrusterBlockEntity implements GeoBlockEntity {
    private static final double EPSILON = 1.0E-6D;
    private static final float MIN_VISIBLE_FLAME_DISTANCE = 0.06F;

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static SerializableDataTicket<Double> VECTOR_THRUSTER_YAW;
    public static SerializableDataTicket<Double> VECTOR_THRUSTER_PITCH;
    public static SerializableDataTicket<Boolean> VECTOR_THRUSTER_IS_SPINNING;
    public double spinrad = 0.0D;
    public double pitchrad = 0.0D;
    public static float MAX_GIMBAL_ANGLE = 30;

    public static long attachedShipId;

    public AbstractVectorThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {
    }

    @Override
    public void tick() {
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        if (level.isClientSide()) {
            tickClientAudio();
            tickClientTrail(level);
            return;
        }

        if (!hasInitialized) {
            BlockPos pos = getBlockPos();
            BlockState state = level.getBlockState(pos);
            Initialize.initialize(level, pos, state);
            hasInitialized = true;
            return;
        }

        BlockState state = this.getBlockState();
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
            applyVisualState(level, state, 0.0D, 0.0D, 0.0D);
            return;
        }

        Vec3 centerOfMassWorld = ServerShipUtils.getCenterOfMassWorld(serverSubLevel);
        if (centerOfMassWorld == null) {
            applyVisualState(level, state, 0.0D, 0.0D, 0.0D);
            return;
        }

        BlockPos pos = this.getBlockPos();
        Vec3 thrusterWorldPos = subLevel.logicalPose().transformPosition(new Vec3(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        ));
        Vector3d leverArmWorld = new Vector3d(
                thrusterWorldPos.x() - centerOfMassWorld.x(),
                thrusterWorldPos.y() - centerOfMassWorld.y(),
                thrusterWorldPos.z() - centerOfMassWorld.z()
        );

        Vector3d desiredForce = copyOrZero(thrusterData.getInputforce()).mul(-1);
        Vector3d desiredTorque = copyOrZero(thrusterData.getInputtorque());
        Vector3d visualDemand = calculateVisualDemandForce(desiredForce, desiredTorque, leverArmWorld);
        if (visualDemand.lengthSquared() <= EPSILON) {
            applyVisualState(level, state, 0.0D, 0.0D, 0.0D);
            return;
        }

        Vector3d installedForceAxisWorld = getInstalledForceDirectionWorld(subLevel, state);
        if (installedForceAxisWorld.lengthSquared() <= EPSILON) {
            applyVisualState(level, state, 0.0D, 0.0D, 0.0D);
            return;
        }
        installedForceAxisWorld.normalize();

        // Function: directionY matches fixed thrusters and stores the installed thrust direction.
        Vector3d installedForceDirectionWorld = new Vector3d(installedForceAxisWorld);
        Vector3d aimForceDirectionWorld = clampToGimbalCone(visualDemand, installedForceDirectionWorld);
        // Function: the renderer animates the nozzle's local +Y emission axis, not the produced force axis.
        Vector3d aimNozzleDirectionWorld = new Vector3d(aimForceDirectionWorld).negate();
        double[] eulerAngle = forceTransform(aimNozzleDirectionWorld, subLevel, thrusterData.getCoordAxis());

        double throttle = calculateThrottleDemand(visualDemand, aimForceDirectionWorld);
        applyVisualState(level, state, eulerAngle[0], eulerAngle[1], throttle);
    }

    private Vector3d getInstalledForceDirectionWorld(SubLevel subLevel, BlockState state) {
        // Function: block FACING is the nozzle direction; produced thrust points the opposite way.
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        Direction forceDirection = facing.getOpposite();
        Vector3d localForceDirection = new Vector3d(
                forceDirection.getStepX(),
                forceDirection.getStepY(),
                forceDirection.getStepZ()
        );
        return subLevel.logicalPose().transformNormal(localForceDirection, new Vector3d());
    }

    private double calculateThrottleDemand(Vector3d visualDemand, Vector3d aimForceDirectionWorld) {
        double selfThrust = getForceCoefficient();
        if (selfThrust <= EPSILON) {
            return 0.0D;
        }

        // Function: match fixed thruster normalization so same-facing vector thrusters share one force demand.
        double sameFacingThrust = Math.max(thrusterData.getSameFacingMaxThrustSum(), selfThrust);
        double projectedDemand = Math.max(0.0D, visualDemand.dot(aimForceDirectionWorld));
        return clamp(projectedDemand / sameFacingThrust, 0.0D, 1.0D);
    }

    private void applyVisualState(Level level, BlockState state, double yaw, double pitch, double throttle) {
        double safeThrottle = clamp(throttle, 0.0D, 1.0D);
        this.spinrad = yaw;
        this.pitchrad = pitch;
        double effectiveThrottle = applyThrottleDemand(safeThrottle);
        float flameDistance = (float) (effectiveThrottle * getMaxFlameDistance());
        // Function: the vector flame renderer ignores sub-0.05 lengths, so preserve visible output for any nonzero throttle.
        if (effectiveThrottle > 0.0D) {
            flameDistance = Math.max(flameDistance, MIN_VISIBLE_FLAME_DISTANCE);
        }
        updateRaycastDistance(level, state, flameDistance);
        setAnimData(VECTOR_THRUSTER_YAW, spinrad);
        setAnimData(VECTOR_THRUSTER_PITCH, pitchrad);
    }

    private Vector3d calculateVisualDemandForce(Vector3d desiredForce, Vector3d desiredTorque, Vector3d leverArmWorld) {
        Vector3d demand = new Vector3d(desiredForce);
        double leverLengthSquared = leverArmWorld.lengthSquared();
        if (leverLengthSquared > EPSILON && desiredTorque.lengthSquared() > EPSILON) {
            // Function: convert requested torque into the closest force this offset thruster can visually explain.
            Vector3d torqueForce = new Vector3d(desiredTorque).cross(leverArmWorld).div(leverLengthSquared);
            demand.add(torqueForce);
        }
        return demand;
    }

    private Vector3d clampToGimbalCone(Vector3d targetForceWorld, Vector3d installDirectionWorld) {
        Vector3d targetDirection = new Vector3d(targetForceWorld).normalize();
        Vector3d baseDirection = new Vector3d(installDirectionWorld).normalize();
        double maxAngleRad = Math.toRadians(MAX_GIMBAL_ANGLE);
        double dot = clamp(baseDirection.dot(targetDirection), -1.0D, 1.0D);
        if (dot >= Math.cos(maxAngleRad)) {
            return targetDirection;
        }

        Vector3d tangent = targetDirection.sub(new Vector3d(baseDirection).mul(dot), new Vector3d());
        if (tangent.lengthSquared() <= EPSILON) {
            return baseDirection;
        }
        tangent.normalize();
        return baseDirection.mul(Math.cos(maxAngleRad)).add(tangent.mul(Math.sin(maxAngleRad))).normalize();
    }

    private static Vector3d copyOrZero(Vector3d vector) {
        return vector == null ? new Vector3d() : new Vector3d(vector);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
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
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("rotx", this.pitchrad);
        tag.putDouble("roty", this.spinrad);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("rotx")) {
            this.pitchrad = tag.getDouble("rotx");
        }
        if (tag.contains("roty")) {
            this.spinrad = tag.getDouble("roty");
        }
    }

    public double getSpinrad() {
        return this.spinrad;
    }

    public double getPitchrad() {
        return this.pitchrad;
    }

    @Override
    protected double getTrailOffsetLength() {
        return getTrailVisualLength();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        markUpdated();
    }

    public double[] forceTransform(
            Vector3d forceInWorld,
            SubLevel subLevel,
            Matrix3d modelCoordAxis
    ) {
        Vector3d forceInShip = subLevel.logicalPose().transformNormalInverse(forceInWorld);
        // Function: coordAxis stores model axes in ship space, so transpose it to read a ship vector in model space.
        Vector3d forceInModel = new Matrix3d(modelCoordAxis).transpose().transform(forceInShip, new Vector3d());
        if (forceInModel.lengthSquared() <= EPSILON) {
            return new double[]{0.0D, 0.0D};
        }
        forceInModel.normalize();

        double yaw = Math.atan2(
                forceInModel.x,
                forceInModel.z
        );
        double pitch = Math.atan2(
                Math.sqrt(forceInModel.x * forceInModel.x + forceInModel.z * forceInModel.z),
                forceInModel.y
        );

        return new double[]{yaw, pitch};
    }
}
