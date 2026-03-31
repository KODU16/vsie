package com.kodu16.vsie.content.turret.heavyturret;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.Initialize;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.foundation.Vec;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;
import java.util.Objects;

public abstract class AbstractHeavyTurretBlockEntity extends AbstractTurretBlockEntity {
    // 功能：手动头瞄时，不再直接沿视线角度旋转，而是瞄准视线方向前方固定 1024 格处的空间点。
    private static final double MANUAL_AIM_DISTANCE = 1024.0D;

    protected AbstractHeavyTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 初始化 turretData
        this.turretData = new TurretData();
    }

    private volatile Vector3d targetPos = new Vector3d(0,0,0);
    // 功能：记录控制椅的方块朝向，头瞄时将玩家视角从控制椅坐标系转换到重炮坐标系。
    private volatile Direction controlSeatFacing = Direction.NORTH;


    public abstract int getmaxpitchdowndegrees();

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public void tick() {
        if (this.getLevel() == null || this.getLevel().isClientSide()) { return; }

        if (idleTicks-- > 1) { return; }

        if (!hasInitialized){
            BlockPos pos = this.getBlockPos();
            BlockState state = this.getBlockState();
            Initialize.initialize(this.getLevel(),pos,state,pivotPoint);

            hasInitialized = true;
            return;
        }

        onShip = VSGameUtilsKt.isBlockInShipyard(this.getLevel(), this.getBlockPos());

        if (onShip) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(this.getLevel(), this.getBlockPos());
            Vector3d center = VSGameUtilsKt.toWorldCoordinates(ship, this.getBlockPos().getX(), this.getBlockPos().getY()+getYAxisOffset(), this.getBlockPos().getZ());
            currentworldpos = new Vector3d(center.x, center.y, center.z);
        }
        else {
            currentworldpos = new Vector3d(Math.round(this.getBlockPos().getX()*10)/10.0, Math.round((this.getBlockPos().getY()+getYAxisOffset())*10)/10.0, Math.round(this.getBlockPos().getZ()*10)/10.0);
        }

        boolean canTrackBySeatView = !getData().isViewLocked && (getData().fireType == 0 || getData().fireType == 2);
        // 功能：当控制椅视角未锁定且重炮为手动/智能模式时，先把视线延长 1024 格得到目标点，再解算重炮旋转角度。
        if (canTrackBySeatView) {
            updateSeatViewTargetPosAndRot();
            this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
            this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
            setAnimData(TURRET_HAS_TARGET, true);
        }

        // 功能：重型炮塔只有在频道匹配时才响应自动/智能射击，行为与主武器一致。
        if ((getData().fireType == 1 || (getData().fireType == 2 && getData().isViewLocked && !targetPos.equals(new Vector3d(0,0,0))))) {
            LogUtils.getLogger().warn("setting target:"+targetPos);
            updateTargetRot();
            this.xRot0 = closestReachableX(xRot0,getMaxSpinSpeed(),targetxrot);
            this.yRot0 = closestReachableY(yRot0,getMaxSpinSpeed(),targetyrot);
            if(!Objects.equals(targetPos, new Vector3d(0, 0, 0))){
                if(xOK && yOK) {
                    targetDistance = Vec.Distance(currentworldpos, targetPos);
                    shootship();
                    idleTicks = getCoolDown();
                }
            }
            else {
                LogUtils.getLogger().warn("target is null");
                setAnimData(TURRET_HAS_TARGET, false);
                targetDistance = 0;
                xRot0 = 0;
                yRot0 = 0;
                targetPreVelocity.clear();
            }
        }

        this.setAnimData(XROT, xRot0);
        this.setAnimData(YROT, yRot0);
        this.markUpdated();
    }

    //heavy turret only
    public void modifyFireType(int type) {
        // 功能：实时获取当前 level，修复因父类缓存 level 为空而导致重炮 GUI 按钮不生效的问题。
        Level currentLevel = this.getLevel();
        if (currentLevel == null || currentLevel.isClientSide) { return; }// 客户端完全不许改！
        getData().fireType = type;
    }

    // 功能：为重型炮塔提供与主武器一致的频道切换逻辑（四选一）。
    public void modifyChannel(int channel) {
        // 功能：实时获取当前 level，修复因父类缓存 level 为空而导致频道切换请求被提前 return 的问题。
        Level currentLevel = this.getLevel();
        if (currentLevel == null || currentLevel.isClientSide) { return; }

        TurretData data = getData();

        if (channel == 1) {
            if ( data.isChannel1() )  { data.reset(data.CHANNEL_HIDE); }
            else {
                data.reset(data.CHANNEL_HIDE);
                data.set(data.CHANNEL_1);
            }
        }
        if (channel == 2) {
            if ( data.isChannel2() )  { data.reset(data.CHANNEL_HIDE); }
            else {
                data.reset(data.CHANNEL_HIDE);
                data.set(data.CHANNEL_2);
            }
        }
        if (channel == 3) {
            if ( data.isChannel3() )  { data.reset(data.CHANNEL_HIDE); }
            else {
                data.reset(data.CHANNEL_HIDE);
                data.set(data.CHANNEL_3);
            }
        }
        if (channel == 4) {
            if ( data.isChannel4() )  { data.reset(data.CHANNEL_HIDE); }
            else {
                data.reset(data.CHANNEL_HIDE);
                data.set(data.CHANNEL_4);
            }
        }
    }

    // 功能：接收控制椅下发的频道编码，供重型炮塔判定是否允许开火。
    public void channelFromCtrl(int channel) { getData().channelOfCtrl = channel; }

    // 功能：判断重型炮塔与控制椅频道是否匹配，逻辑与主武器保持一致。
    public boolean isChannelMatch() {
        TurretData data = getData();
        int channel = data.getChannelStatus();
        return (channel & data.channelOfCtrl) != 0;
    }

    public void updatespecificenemy(Vector3d pos) {
        LogUtils.getLogger().warn("update:controlseat setting turret data to:"+targetPos);
            this.targetPos = pos;
    }

    public void updateplayerstatus(boolean isviewlocked, int rotx, int roty, Direction seatFacing) {
        // 功能：保存控制椅实时视角与控制椅朝向，供重型炮塔在头瞄时进行坐标系转换。
        this.getData().isViewLocked = isviewlocked;
        this.getData().playerAngleX = rotx;
        this.getData().playerAngleY = roty;
        this.controlSeatFacing = seatFacing;
    }// 这应该写在控制椅里

    private void updateSeatViewTargetPosAndRot() {
        // 功能：根据控制椅朝向与重炮朝向差值，修正玩家 yaw，并计算“视线前方 1024 格”的世界目标点。
        float convertedYaw = convertSeatYawToTurretYaw(getData().playerAngleY, controlSeatFacing, this.getBlockState().getValue(AbstractTurretBlock.FACING));
        double pitchRad = Math.toRadians(getData().playerAngleX);
        double yawRad = Math.toRadians(convertedYaw);

        // 功能：在重炮本地坐标系中根据 yaw/pitch 还原视线方向向量（右/上/前）。
        Vector3d localAimDirection = new Vector3d(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        );

        // 功能：将本地方向转换为世界方向，并生成视线前方固定距离（1024 格）目标点。
        Vector3d worldAimDirection = getTurretWorldDirection(localAimDirection).normalize();
        this.targetPos = new Vector3d(
                currentworldpos.x + worldAimDirection.x * MANUAL_AIM_DISTANCE,
                currentworldpos.y + worldAimDirection.y * MANUAL_AIM_DISTANCE,
                currentworldpos.z + worldAimDirection.z * MANUAL_AIM_DISTANCE
        );

        // 功能：统一复用目标点转角逻辑，保证手动模式与自动模式的炮塔解算路径一致。
        updateTargetRot();
    }

    private Vector3d getTurretWorldDirection(Vector3d localDirection) {
        // 功能：按炮塔朝向构建本地基向量（前/上/右），供本地视线方向转换到世界坐标。
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        Vec3 localUp = VectorConversionsMCKt.toMinecraft(VectorConversionsMCKt.toJOMLD(facing.getOpposite().getNormal()));
        Vec3 localForward = switch (facing) {
            case NORTH -> new Vec3(0, 1, 0);
            case SOUTH -> new Vec3(0, -1, 0);
            case WEST, EAST, UP, DOWN -> new Vec3(0, 0, -1);
        };
        Vec3 localRight = switch (facing) {
            case NORTH, DOWN, SOUTH -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(0, -1, 0);
            case EAST -> new Vec3(0, 1, 0);
            case UP -> new Vec3(-1, 0, 0);
        };

        // 功能：若炮塔在船体上，先把本地基向量转换到世界坐标；否则直接使用本地方向作为世界方向。
        Vector3d worldForward = new Vector3d(localForward.x, localForward.y, localForward.z);
        Vector3d worldUp = new Vector3d(localUp.x, localUp.y, localUp.z);
        Vector3d worldRight = new Vector3d(localRight.x, localRight.y, localRight.z);
        if (VSGameUtilsKt.isBlockInShipyard(this.getLevel(), this.getBlockPos())) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(this.getLevel(), this.getBlockPos());
            ShipTransform transform = ship.getTransform();
            worldForward = transform.getShipToWorld().transformDirection(worldForward, new Vector3d());
            worldUp = transform.getShipToWorld().transformDirection(worldUp, new Vector3d());
            worldRight = transform.getShipToWorld().transformDirection(worldRight, new Vector3d());
        }

        // 功能：将“本地右/上/前”方向按分量叠加成最终世界方向向量。
        return new Vector3d(
                worldRight.x * localDirection.x + worldUp.x * localDirection.y + worldForward.x * localDirection.z,
                worldRight.y * localDirection.x + worldUp.y * localDirection.y + worldForward.y * localDirection.z,
                worldRight.z * localDirection.x + worldUp.z * localDirection.y + worldForward.z * localDirection.z
        );
    }

    private float convertSeatYawToTurretYaw(float seatYaw, Direction seatFacing, Direction turretFacing) {
        // 功能：计算控制椅/重炮朝向对应的水平角偏移，得到重炮坐标系下的玩家 yaw。
        float seatBaseYaw = directionToHorizontalYaw(seatFacing);
        float turretBaseYaw = directionToHorizontalYaw(turretFacing);
        float yawDelta = turretBaseYaw - seatBaseYaw;
        return normalizeDegrees(seatYaw + yawDelta);
    }

    private float directionToHorizontalYaw(Direction facing) {
        // 功能：将方块水平朝向映射到与玩家 yaw 一致的角度定义（南=0，西=90，北=180，东=-90）。
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }

    private float normalizeDegrees(float degrees) {
        // 功能：将角度归一化到 [-180, 180) 区间，避免跨边界时旋转突变。
        float normalized = degrees % 360.0F;
        if (normalized >= 180.0F) normalized -= 360.0F;
        if (normalized < -180.0F) normalized += 360.0F;
        return normalized;
    }

    private void updateTargetRot() {
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        // 1. 获取炮塔当前的朝向（方块的facing）
        Vec3 localUp  = VectorConversionsMCKt.toMinecraft(VectorConversionsMCKt.toJOMLD(facing.getOpposite().getNormal()));;
        // 2. 获取炮塔本地坐标系的 "前" 和 "上" 向量（世界坐标）
        Vec3 localForward = switch (facing) {
            case NORTH -> new Vec3(0, 1, 0);
            case SOUTH -> new Vec3(0, -1, 0);
            case WEST,EAST,UP,DOWN -> new Vec3(0,0,-1);
        };

        Vec3 localRight  = switch (facing) {
            case NORTH, DOWN, SOUTH -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(0, -1, 0);
            case EAST -> new Vec3(0, 1, 0);
            case UP -> new Vec3(-1,0,0);
        };

        boolean onship = VSGameUtilsKt.isBlockInShipyard(this.getLevel(),this.getBlockPos());
        if(onship) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(this.getLevel(), this.getBlockPos());
            final ShipTransform transform = ship.getTransform();
            transform.getShipToWorld().transformDirection(new Vector3d(localForward.x,localForward.y,localForward.z), worldXDirection);
            worldXDirection.normalize();
            transform.getShipToWorld().transformDirection(new Vector3d(localUp.x,localUp.y,localUp.z), worldYDirection);
            worldYDirection.normalize();
            transform.getShipToWorld().transformDirection(new Vector3d(localRight.x,localRight.y,localRight.z), worldZDirection);
            worldZDirection.normalize();
        }
        else {
            worldXDirection = new Vector3d(localForward.x,localForward.y,localForward.z);
            worldYDirection = new Vector3d(localUp.x,localUp.y,localUp.z);
            worldZDirection = new Vector3d(localRight.x,localRight.y,localRight.z);

        }

        // 4. 目标相对炮塔中心的向量（世界坐标）
        Vec3 toTargetWorld = new Vec3(
                targetPos.x - currentworldpos.x,
                targetPos.y - currentworldpos.y,
                targetPos.z - currentworldpos.z
        ).normalize();   // 建议先normalize，减少浮点误差影响

        if (toTargetWorld.lengthSqr() < 1e-6) return; // 目标在正中心，放弃计算

        // 5. 把世界向量转换到炮塔本地坐标系（用基向量做点积）
        double localX = toTargetWorld.dot(VectorConversionsMCKt.toMinecraft(worldZDirection));     // 本地右
        double localY = toTargetWorld.dot(VectorConversionsMCKt.toMinecraft(worldYDirection));        // 本地向上
        double localZ = toTargetWorld.dot(VectorConversionsMCKt.toMinecraft(worldXDirection));   // 本地向前

        // 6. 现在就在本地坐标系了，计算角度（经典写法）
        // yaw   : 左右角度，atan2(x, z)
        // pitch : 上下角度，atan2(y, 平面距离)
        double yaw   = Math.atan2(localX, localZ);           // 注意atan2顺序
        double pitch = Math.atan2(localY, Math.sqrt(localX * localX + localZ * localZ));

        this.targetyrot = (float) -yaw;

        this.targetxrot = (float) pitch;
        //LogUtils.getLogger().warn("X:"+worldXDirection+"Y:"+worldYDirection+"Z:"+worldZDirection+"target:"+targetPos+"turret:"+currentworldpos +"yaw:"+yaw+"pitch:"+pitch);
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
        tag.putInt("firetype", getData().fireType);
        tag.putDouble("distance", this.getTargetDistance());
        tag.putInt("playerxrot",this.getData().playerAngleX);
        tag.putInt("playeryrot",this.getData().playerAngleY);
        tag.putFloat("xrot",this.targetxrot);
        tag.putFloat("yrot",this.targetyrot);
        tag.putDouble("targetX", targetPos.x);
        tag.putDouble("targetY", targetPos.y);
        tag.putDouble("targetZ", targetPos.z);
        // 功能：同步重型炮塔配置寄存器和接收频道编码，确保 GUI 与联动状态一致。
        // 频道部分被重写！
        tag.putInt("configregister",this.getData().configRegister);
        tag.putInt("channelofctrl", getData().channelOfCtrl);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        // 确保 turretData 不为 null
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        if (tag.contains("firetype")) {this.getData().fireType = tag.getInt("firetype");}
        if (tag.contains("distance")) {this.targetDistance = tag.getDouble("distance");}
        if(tag.contains("playerxrot")) {this.getData().playerAngleX = tag.getInt("playerxrot");}
        if(tag.contains("playeryrot")) {this.getData().playerAngleY = tag.getInt("playeryrot");}
        if (tag.contains("xrot")) {this.targetxrot = tag.getFloat("xrot");}
        if (tag.contains("yrot")) {this.targetyrot = tag.getFloat("yrot");}
        if (tag.contains("targetX")) targetPos = new Vector3d(
                tag.getDouble("targetX"),
                tag.getDouble("targetY"),
                tag.getDouble("targetZ")
        );
        // 功能：读取重型炮塔配置寄存器和接收频道编码，恢复频道联动配置。
        if (tag.contains("configregister")) { this.getData().configRegister = (byte)tag.getInt("configregister"); }
        if (tag.contains("channelofctrl")) { this.getData().channelOfCtrl = tag.getInt("channelofctrl"); }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Heavy Turret Screen");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new HeavyTurretContainerMenu(containerId, inv, this);
    }
}
