package com.kodu16.vsie.content.vectorthruster;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.thruster.Initialize;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Matrix3d;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;

import java.util.List;

public abstract class AbstractVectorThrusterBlockEntity extends AbstractThrusterBlockEntity implements GeoBlockEntity {


    public AbstractVectorThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static SerializableDataTicket<Double> VECTOR_THRUSTER_YAW;
    public static SerializableDataTicket<Double> VECTOR_THRUSTER_PITCH;
    public static SerializableDataTicket<Boolean> VECTOR_THRUSTER_IS_SPINNING;
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
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,this.getBlockPos());

            if (subLevel != null) {
                if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
                    LOGGER.warn("vector thruster sublevel is not server side");
                    return;
                }

                // 鑸圭殑閲嶅績锛堜笘鐣屽潗鏍囷級鍜屾帹杩涘櫒鐨勪綅缃紙涓栫晫鍧愭爣锛夛紝璁＄畻鍑哄姏鑷傦紙鍗曚綅鍖栫殑锛?
                Vec3 shipCenterOfMass = ServerShipUtils.getCenterOfMassWorld(serverSubLevel);
                if (shipCenterOfMass == null) {
                    return;
                }

                Vec3 thrusterWorldPos = subLevel.logicalPose().transformPosition(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
                Vec3 leverArmWorld = thrusterWorldPos.subtract(shipCenterOfMass.x(), shipCenterOfMass.y(), shipCenterOfMass.z());
                if (leverArmWorld.lengthSqr() > 1.0E-6D) {
                    leverArmWorld = leverArmWorld.normalize();
                }

                //鑾峰彇鍩哄骇鏈濆悜骞惰绠楀熀搴ц酱鐨勪笘鐣屾湞鍚戯紙鍗曚綅鍖栫殑锛?                BlockState state = this.getBlockState();

                Vector3d blockdirection = Initialize.toVector3d(this.getBlockState().getValue(FACING));
                Vector3d worldfacing = subLevel.logicalPose().transformNormal(blockdirection);
                worldfacing.normalize();

                // ==================== 鐜╁杈撳叆 ====================
                Vector3d desiredForce = thrusterData.getInputforce() != null ? thrusterData.getInputforce() : new Vector3d();
                Vector3d desiredTorque = thrusterData.getInputtorque() != null ? thrusterData.getInputtorque() : new Vector3d();

                boolean hasInput = desiredForce.lengthSquared() > 1e-6 || desiredTorque.lengthSquared() > 1e-6;

                double throttle = 0.0;
                // 鍔熻兘锛氭瘡涓煝閲忔帹杩涘櫒姣?tick 閮界嫭绔嬭绠楄嚜宸辩殑鐩爣娆ф媺瑙掞紝榛樿淇濇寔涓綅锛堜笉鍋忚浆锛?
                double[] eulerAngle={0,0};
                if (hasInput) {
                    Vector3d worldXDirection;
                    Vector3d worldYDirection;
                    Vector3d worldZDirection;
                    Vector3d leverArm = new Vector3d(leverArmWorld.x, leverArmWorld.y, leverArmWorld.z);
                    Vector3d torqueforce = new Vector3d(desiredTorque).cross(leverArm);
                    Vector3d targetthrust = torqueforce.add(desiredForce);
                    if (targetthrust.lengthSquared() > 1.0E-6D) {
                        targetthrust.normalize();
                    }
                    worldYDirection = subLevel.logicalPose().transformNormal(thrusterData.getDirectionY(), new Vector3d()).normalize();
                    worldXDirection = subLevel.logicalPose().transformNormal(thrusterData.getDirectionX(), new Vector3d()).normalize();
                    worldZDirection = subLevel.logicalPose().transformNormal(thrusterData.getDirectionZ(), new Vector3d()).normalize();
                    // 鍔熻兘锛氭湁杈撳叆鏃舵寜鐩爣鎺ㄥ姏鏂瑰悜璁＄畻鍠峰彛鍋忚浆
                    eulerAngle = forceTransform(targetthrust,subLevel,thrusterData.getCoordAxis());

                    setChanged();
                    // 鏃ュ織璋冭瘯
                    //LOGGER.info("VectorThruster {}  worldY={}, worldX={}, worldZ={}, desiredVec={}, spin={}掳, pitch={}掳",
                    //        getBlockPos(), worldYDirection, worldXDirection, worldZDirection, targetthrust, spinrad, pitchrad);
                }

                // 鍔熻兘锛氭棤杈撳叆鏃朵繚鎸侀粯璁や腑浣嶏紝閬垮厤鏈粦瀹氭帹杩涘櫒鍑虹幇寮傚父鍋忚浆
                if(!hasInput){
                    eulerAngle = new double[]{0,0};
                    //eulerAngle = forceTransform(new Vector3d(1,1,1),transform,thrusterData.getCoordAxis());
                }

                this.spinrad = eulerAngle[0];   //yaw
                this.pitchrad = eulerAngle[1];  //pitch
                // 鏇存柊鏁版嵁
                thrusterData.setThrottle((float) throttle);
                setAnimData(VECTOR_THRUSTER_YAW, spinrad);
                setAnimData(VECTOR_THRUSTER_PITCH, pitchrad);

            }

        } else {
            LOGGER.warn(String.valueOf(Component.literal("detected uninitialized vector thruster, time to sweep valkyriie's ass")));

            BlockPos pos = getBlockPos();
            BlockState state = level.getBlockState(pos);
            Initialize.initialize(level, pos, state);

            // 鍔熻兘锛氳縼绉诲埌 NeoForge 1.21.1 鍚庯紝鏀逛负鍚?NeoForge GAME 浜嬩欢鎬荤嚎娉ㄥ唽鐭㈤噺鎺ㄨ繘鍣ㄧ洃鍚櫒銆?
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
        tag.putDouble("rotx",this.pitchrad);
        tag.putDouble("roty",this.spinrad);
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

    public double getSpinrad() {return this.spinrad;}

    // 鍔熻兘锛氳繑鍥炲柗鍙ｄ刊浠拌锛堝姬搴︼級渚涙覆鏌撹鍙?
    public double getPitchrad() {return this.pitchrad;}

    @Override
    public void onLoad() {
        super.onLoad();
        markUpdated();
    }


    // 杈撳叆浣犳兂瑕佺殑鍔犲姏鏂瑰悜 鎵€鍦ㄨ埞鐨則ransform 浠ュ強妯″瀷鑷韩鐨凜oordAxis
    // 鍚愬嚭妯″瀷搴旇杞殑鏂瑰悜
    public double[] forceTransform(
            Vector3d forceInWorld,
            SubLevel subLevel,
            Matrix3d modelCoordAxis
    ){
        Vector3d forceInShip=subLevel.logicalPose().transformNormalInverse(forceInWorld);
        Vector3d forceInModel=modelCoordAxis.transform(forceInShip);

        // 璇″紓鐨勫潗鏍囧彉鎹?鏍规嵁妯″瀷鏉ョ殑
        double yaw = Math.atan2(
                forceInModel.x,
                forceInModel.z
        );
        double pitch=Math.atan2(
                Math.sqrt(forceInModel.x * forceInModel.x + forceInModel.z * forceInModel.z),
                forceInModel.y
        );

        return new double[]{yaw,pitch};
    }
}
