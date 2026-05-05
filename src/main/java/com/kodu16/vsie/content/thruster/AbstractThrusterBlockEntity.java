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

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // Common State
    public ThrusterData thrusterData;
    public boolean hasInitialized = false;//鍊煎緱琚啓鍏bstract绫昏鎵€鏈変汉瀛︿範锛?
    public int throttle;//璁＄畻娑堣€楁补閲忕敤

    private float raycastDistance = 0.0f;//娉ㄦ剰锛岃繖灏辨槸鏈€閲嶈鐨勬牳蹇冪殑raycast璺濈


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

    // 鍔熻兘锛氭帴鏀舵帶鍒舵涓嬪彂鐨勭洰鏍囧姏/鍔涚煩锛屼互鍙娾€滀笌鏈帹杩涘櫒鍚屾湞鍚戔€濈殑鏈€澶ф帹鍔涙€诲拰銆?
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
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,pos);
            if (subLevel!=null) {
                if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
                    LOGGER.warn("thruster sublevel is not server side");
                    performRaycast(level);
                    return;
                }

                Vector3d thrusterPosInShip = JOMLConversion.atCenterOf(pos);
                Vector3d thrusterWorldPos = subLevel.logicalPose().transformPosition(thrusterPosInShip, new Vector3d());
                Vector3d relativePosWorld = thrusterWorldPos.sub(getCenterOfMassWorld(serverSubLevel), new Vector3d());

                // 1. 鎺ㄨ繘鍣ㄥ湪涓栫晫鍧愭爣绯讳笅鐨勬帹鍔涙柟鍚戯紙鍗曚綅鍚戦噺锛?
                Vector3d thrustDirectionWorld = subLevel.logicalPose()
                        .transformNormal(thrusterData.getDirectionY(), new Vector3d())
                        .normalize()
                        .mul(-1.0);

                // 2. 鍔涜础鐚柟鍚戯紙灏辨槸鎺ㄥ姏鏂瑰悜鏈韩锛?
                Vector3d forceContribution = new Vector3d(thrustDirectionWorld);

                // 3. 鍔涚煩璐＄尞鏂瑰悜锛歳 脳 F_dir
                Vector3d torqueFromThisThruster = new Vector3d(relativePosWorld).cross(thrustDirectionWorld);

                // 褰掍竴鍖栧姏鐭╂柟鍚?
                double torqueLength = torqueFromThisThruster.length();
                if (torqueLength > 1e-6) {
                    torqueFromThisThruster.mul(1.0 / torqueLength);
                } else {
                    torqueFromThisThruster.set(0, 0, 0);
                }


                // 5. 鑾峰彇鐜╁/鐢佃剳杈撳叆鐨勪笘鐣屽潗鏍囩洰鏍囧姏鍜岀洰鏍囧姏鐭╋紙濡傛灉涓簄ull鍒欒涓?锛?
                Vector3d desiredForce = thrusterData.getInputforce() != null ? thrusterData.getInputforce() : new Vector3d(0, 0, 0);
                Vector3d desiredTorque = thrusterData.getInputtorque() != null ? thrusterData.getInputtorque() : new Vector3d(0, 0, 0);

                // 褰掍竴鍖栬緭鍏ワ紙闃叉鏁板€煎お澶э級
                double desiredForceLen = desiredForce.length();
                double desiredTorqueLen = desiredTorque.length();

                Vector3d normDesiredForce = desiredForceLen > 1e-6 ? new Vector3d(desiredForce).mul(1.0 / desiredForceLen) : new Vector3d();
                Vector3d normDesiredTorque = desiredTorqueLen > 1e-6 ? new Vector3d(desiredTorque).mul(1.0 / desiredTorqueLen) : new Vector3d();

                // 6. 璁＄畻杩欎釜鎺ㄨ繘鍣ㄥ鐩爣鐨勨€滆础鐚害鈥濓紙鐐圭Н锛岃秺姝ｈ秺鏈夊府鍔╋級
                double forceAlignment  = Math.max(0, forceContribution.dot(normDesiredForce));   // 鍙叧蹇冨悓鍚戣础鐚?
                double torqueAlignment = Math.max(0, torqueFromThisThruster.dot(normDesiredTorque));

                // 鍔熻兘锛氬姏鍒嗛厤鏀逛负鈥滃悓鏈濆悜鎺ㄨ繘鍣ㄦ寜鏈€澶ф帹鍔涘崰姣斺€濆垎鎽婏紝鍚屾椂鑰冭檻璇ユ柟鍚戞€绘帹鍔涘褰撳墠鐩爣鍔涚殑闇€姹傜▼搴︺€?
                double sameFacingThrust = thrusterData.getSameFacingMaxThrustSum();
                double selfMaxThrust = getMaxThrust();
                // 鍔熻兘锛氬厹搴曪紝閬垮厤鍚屽悜鎬绘帹鍔涚己澶辨椂鍑虹幇闄ら浂锛岃嚦灏戠敤鑷韩鏈€澶ф帹鍔涘弬涓庤绠椼€?
                double safeSameFacingThrust = Math.max(sameFacingThrust, selfMaxThrust);
                // 鍔熻兘锛氭湰鎺ㄨ繘鍣ㄥ湪鈥滃悓鏈濆悜鎺ㄨ繘鍣ㄧ粍鈥濆唴鐨勬帹鍔涘崰姣斻€?
                double sameFacingShare = safeSameFacingThrust > 1e-6 ? (selfMaxThrust / safeSameFacingThrust) : 0;
                // 鍔熻兘锛氳鏈濆悜涓婄洰鏍囧姏闇€姹傚崰鎬诲彲鐢ㄦ帹鍔涚殑姣斾緥锛?~1锛夈€?
                double forceDemandRatio = desiredForceLen > 1e-6 ? Math.min(1.0, desiredForceLen / safeSameFacingThrust) : 0;
                // 鍔熻兘锛氭渶缁堝姏璐＄尞 = 鏂瑰悜鍖归厤搴?脳 缁勫唴鎺ㄥ姏鍗犳瘮 脳 褰撳墠闇€姹傛瘮渚嬨€?
                double forceContributionWeighted = forceAlignment * sameFacingShare * forceDemandRatio;

                // 7. 鍚堝苟鍔涘拰鍔涚煩鐨勮础鐚紙淇濇寔鍔涚煩鐩稿叧閫昏緫涓嶅彉锛屼粎鏇挎崲鍔涜础鐚绠楋級
                double totalAlignment = forceContributionWeighted + torqueAlignment;

                // 鍙€夛細濡傛灉浣犲笇鏈涚函骞冲姩鏃朵晶闈㈡帹杩涘櫒瀹屽叏涓嶅柗鐏紝鍙互鎶?torqueAlignment 鏉冮噸璋冮珮
                // 渚嬪锛歞ouble totalAlignment = forceAlignment + 2.0 * torqueAlignment;

                // 8. 鏈€缁堟补闂?0~1锛堝甫骞虫粦闃叉灏忔姈鍔級
                double throttle = Math.max(0.0, Math.min(1.0, totalAlignment));

                this.throttle = (int) (throttle*100);

                thrusterData.setThrottle((float) throttle);

                /*LOGGER.warn("Thruster {}: transform={} throttle={} forceAlign={} torqueAlign={} | dir={} localdir={} force={} torque={} relPos={}",
                        pos, Ship.getTransform(), throttle, forceAlignment, torqueAlignment,
                        thrustDirectionWorld, thrusterData.getDirection(), normDesiredForce, normDesiredTorque, relativePosWorld);*/


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
            // 鍔熻兘锛氳縼绉诲埌 NeoForge 1.21.1 鍚庯紝鏀逛负鍚?NeoForge GAME 浜嬩欢鎬荤嚎娉ㄥ唽褰撳墠鎺ㄨ繘鍣ㄧ洃鍚櫒銆?
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

    public abstract int fuelconsumptionperthrottle();//姣弔ick锛屾瘡鐧惧垎姣旀补闂ㄦ秷鑰楃殑娌归噺锛屼竴绉掓秷鑰?0娆★紝鍒嬁鑴氬～锛?


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
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        // 鍔熻兘锛氶€傞厤 1.21.1 NeoForge 鐨?NBT 绫诲瀷妫€鏌ュ父閲忥紝浣跨敤 Tag.TAG_FLOAT 璇诲彇娴偣灏勭嚎璺濈銆?
        if (tag.contains("raycastDistance", Tag.TAG_FLOAT)) {
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
