package com.kodu16.vsie.content.thruster;

import com.kodu16.vsie.foundation.ServerShipUtils;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.joml.*;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import javax.annotation.Nonnull;
import java.lang.Math;
import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractThrusterBlockEntity extends SmartBlockEntity implements GeoBlockEntity {
    protected static final double THROTTLE_EPSILON = 1.0E-6D;
    private static final float DEFAULT_FLAME_LENGTH_CHANGE_SPEED_LIMIT = 0.1F;
    protected static final int DEFAULT_CONTROL_SEAT_ENERGY_COST_PER_TICK = 50;

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // Common State
    public ThrusterData thrusterData;
    public boolean hasInitialized = false;
    public int throttle;
    private int forceLimitPercent = 100;
    private int torqueLimitPercent = 100;
    private float flameLengthChangeSpeedLimit = DEFAULT_FLAME_LENGTH_CHANGE_SPEED_LIMIT;

    private float raycastDistance = 0.0f;


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

    public int getControlSeatEnergyCostPerTick() {
        // Function: linked thrusters consume a baseline control-seat FE upkeep even before subclasses tune it.
        return DEFAULT_CONTROL_SEAT_ENERGY_COST_PER_TICK;
    }

    public int getForceLimitPercent() {
        return forceLimitPercent;
    }

    public int getTorqueLimitPercent() {
        return torqueLimitPercent;
    }

    public float getFlameLengthChangeSpeedLimit() {
        return flameLengthChangeSpeedLimit;
    }

    public double getForceCoefficient() {
        return getMaxThrust() * (forceLimitPercent / 100.0D);
    }

    public double getTorqueCoefficient() {
        return getMaxThrust() * (torqueLimitPercent / 100.0D);
    }

    public void setOutputLimitPercents(int forcePercent, int torquePercent) {
        // Function: keep GUI/server inputs inside the advertised 0-100 percent range.
        this.forceLimitPercent = clampPercent(forcePercent);
        this.torqueLimitPercent = clampPercent(torquePercent);
    }

    public void setFlameLengthChangeSpeedLimit(float flameLengthChangeSpeedLimit) {
        // Function: cap rendered flame length changes per tick so throttle steps do not pop visually.
        this.flameLengthChangeSpeedLimit = Math.max(0.0F, finiteOrDefault(
                flameLengthChangeSpeedLimit,
                DEFAULT_FLAME_LENGTH_CHANGE_SPEED_LIMIT
        ));
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static float finiteOrDefault(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    //public abstract int getConsumetick();

    public void setdata(Vector3d inputtorque, Vector3d inputforce, double sameFacingMaxThrustSum)
    {
        thrusterData.setInputtorque(inputtorque);
        thrusterData.setInputforce(inputforce);
        thrusterData.setSameFacingMaxThrustSum(sameFacingMaxThrustSum);
    }

    @SuppressWarnings("null")
    public void tick() {
        super.tick();
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        if (level.isClientSide()) {
            tickClientAudio();
            return;
        }
        Logger LOGGER = LogUtils.getLogger();
        if (hasInitialized)
        {
            BlockPos pos = this.getBlockPos();
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,pos);
            if (subLevel!=null) {
                if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
                    performRaycast(level);
                    return;
                }

                Vector3d thrusterPosInShip = JOMLConversion.atCenterOf(pos);
                Vector3d thrusterWorldPos = subLevel.logicalPose().transformPosition(thrusterPosInShip, new Vector3d());
                Vector3d relativePosWorld = thrusterWorldPos.sub(getCenterOfMassWorld(serverSubLevel), new Vector3d());

                // Function: directionY is the installed thrust direction; flame/nozzle direction is opposite.
                Vector3d thrustDirectionWorld = subLevel.logicalPose()
                        .transformNormal(thrusterData.getDirectionY(), new Vector3d())
                        .normalize();

                Vector3d forceContribution = new Vector3d(thrustDirectionWorld);

                Vector3d torqueFromThisThruster = new Vector3d(relativePosWorld).cross(thrustDirectionWorld);

                // 瑜版帊绔撮崠鏍у閻晜鏌熼崥?
                double torqueLength = torqueFromThisThruster.length();
                if (torqueLength > 1e-6) {
                    torqueFromThisThruster.mul(1.0 / torqueLength);
                } else {
                    torqueFromThisThruster.set(0, 0, 0);
                }


                Vector3d desiredForce = thrusterData.getInputforce() != null ? thrusterData.getInputforce() : new Vector3d(0, 0, 0);
                Vector3d desiredTorque = thrusterData.getInputtorque() != null ? thrusterData.getInputtorque() : new Vector3d(0, 0, 0);

                double desiredForceLen = desiredForce.length();
                double desiredTorqueLen = desiredTorque.length();

                Vector3d normDesiredForce = desiredForceLen > 1e-6 ? new Vector3d(desiredForce).mul(-1.0 / desiredForceLen) : new Vector3d();
                Vector3d normDesiredTorque = desiredTorqueLen > 1e-6 ? new Vector3d(desiredTorque).mul(1.0 / desiredTorqueLen) : new Vector3d();

                double forceAlignment  = Math.max(0, forceContribution.dot(normDesiredForce));   // 閸欘亜鍙ц箛鍐ㄦ倱閸氭垼纭€閻?
                double torqueAlignment = Math.max(0, torqueFromThisThruster.dot(normDesiredTorque));

                double sameFacingThrust = thrusterData.getSameFacingMaxThrustSum();
                double selfMaxThrust = getForceCoefficient();
                double safeSameFacingThrust = Math.max(sameFacingThrust, selfMaxThrust);
                double forceDemandRatio = desiredForceLen > 1e-6 ? Math.min(1.0, desiredForceLen / safeSameFacingThrust) : 0;
                // Function: same-facing thrust already normalizes demand; do not divide each thruster's visual throttle by its share again.
                double forceContributionWeighted = forceAlignment * forceDemandRatio;
                // Function: a thruster with zero torque authority should not spend throttle on pure torque demand.
                double torqueContributionWeighted = torqueAlignment * (getMaxThrust() > 1e-6 ? getTorqueCoefficient() / getMaxThrust() : 0.0);

                double totalAlignment = forceContributionWeighted + torqueContributionWeighted;
                //LogUtils.getLogger().warn("forcecontribution:"+forceContributionWeighted+"torque:"+torqueContributionWeighted);

                double throttle = Math.max(0.0, Math.min(1.0, totalAlignment));

                applyThrottleDemand(throttle);

                /*LOGGER.warn("Thruster {}: transform={} throttle={} forceAlign={} torqueAlign={} | dir={} localdir={} force={} torque={} relPos={}",
                        pos, Ship.getTransform(), throttle, forceAlignment, torqueAlignment,
                        thrustDirectionWorld, thrusterData.getDirection(), normDesiredForce, normDesiredTorque, relativePosWorld);*/


            }
            else{
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

    private Vector3d getCenterOfMassWorld(ServerSubLevel subLevel) {
        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return new Vector3d(subLevel.logicalPose().position());
        }

        return subLevel.logicalPose().transformPosition(massData.getCenterOfMass(), new Vector3d());
    }

    protected void performRaycast(@Nonnull Level level) {
        Logger LOGGER = LogUtils.getLogger();
        BlockState state = this.getBlockState();
        //LOGGER.warn(String.valueOf(Component.literal("throttle:"+thrusterData.getThrottle())));
        //LOGGER.warn(String.valueOf(Component.literal("raycastdistance:"+-thrusterData.getThrottle()*getMaxFlameDistance())));
        updateRaycastDistance(level, state, (float) (thrusterData.getThrottle()*getMaxFlameDistance()));
    }

    protected void updateRaycastDistance(@Nonnull Level level, @Nonnull BlockState state, float distance) {
        this.raycastDistance = limitRaycastDistanceChange(distance);
        setChanged();
        if (!level.isClientSide()) {
            level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    private float limitRaycastDistanceChange(float targetDistance) {
        float safeTargetDistance = Math.max(0.0F, finiteOrDefault(targetDistance, 0.0F));
        float safeCurrentDistance = Math.max(0.0F, finiteOrDefault(this.raycastDistance, 0.0F));
        float maxStep = Math.max(0.0F, finiteOrDefault(
                this.flameLengthChangeSpeedLimit,
                DEFAULT_FLAME_LENGTH_CHANGE_SPEED_LIMIT
        ));
        float delta = safeTargetDistance - safeCurrentDistance;
        if (Math.abs(delta) <= maxStep) {
            return safeTargetDistance;
        }
        // Function: limit only rendered length; throttle and fuel cost still use this tick's demand.
        return safeCurrentDistance + Math.signum(delta) * maxStep;
    }

    public abstract int fuelconsumptionperthrottle();//濮ｅ紨ick閿涘本鐦￠惂鎯у瀻濮ｆ梹琛ラ梻銊︾Х閼版娈戝▽褰掑櫤閿涘奔绔寸粔鎺撶Х閼?0濞嗏槄绱濋崚顐ｅ瑏閼存艾锝為敍?


    protected static int toThrottlePercent(double throttle) {
        double safeThrottle = Math.max(0.0D, Math.min(1.0D, throttle));
        if (safeThrottle <= THROTTLE_EPSILON) {
            return 0;
        }
        // Function: very small but valid force demand must still consume and display at least one percent throttle.
        return Math.max(1, (int) Math.ceil(safeThrottle * 100.0D));
    }

    protected double applyThrottleDemand(double throttleDemand) {
        this.throttle = toThrottlePercent(throttleDemand);
        // Function: renderer state must use the same percent throttle floor as fuel consumption.
        double effectiveThrottle = this.throttle / 100.0D;
        thrusterData.setThrottle(effectiveThrottle);
        return effectiveThrottle;
    }

    protected void tickClientAudio() {
        // Function: client audio follows the synchronized visual throttle so loop and boost sounds match rendered flames.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.kodu16.vsie.content.thruster.client.ThrusterSoundManager.updateThruster(this);
        }
    }

    protected abstract boolean isWorking();

    public int getFuelThrottle(){return this.throttle;}

    // Networking and nbt

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
        tag.putFloat("raycastDistance", this.raycastDistance);
        tag.putInt("forceLimitPercent", this.forceLimitPercent);
        tag.putInt("torqueLimitPercent", this.torqueLimitPercent);
        tag.putFloat("flameLengthChangeSpeedLimit", this.flameLengthChangeSpeedLimit);
        tag.putDouble("visualThrottle", this.thrusterData.getThrottle());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        if (tag.contains("raycastDistance", Tag.TAG_FLOAT)) {
            this.raycastDistance = tag.getFloat("raycastDistance");
        } else {
            this.raycastDistance = 0;
        }
        if (tag.contains("forceLimitPercent", Tag.TAG_INT)) {
            this.forceLimitPercent = clampPercent(tag.getInt("forceLimitPercent"));
        }
        if (tag.contains("torqueLimitPercent", Tag.TAG_INT)) {
            this.torqueLimitPercent = clampPercent(tag.getInt("torqueLimitPercent"));
        }
        if (tag.contains("flameLengthChangeSpeedLimit", Tag.TAG_FLOAT)) {
            setFlameLengthChangeSpeedLimit(tag.getFloat("flameLengthChangeSpeedLimit"));
        } else {
            this.flameLengthChangeSpeedLimit = DEFAULT_FLAME_LENGTH_CHANGE_SPEED_LIMIT;
        }
        if (tag.contains("visualThrottle", Tag.TAG_DOUBLE)) {
            this.thrusterData.setThrottle(tag.getDouble("visualThrottle"));
        } else {
            this.thrusterData.setThrottle(0.0D);
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
