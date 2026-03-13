package com.kodu16.vsie.content.turret.heavyturret;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
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
    protected AbstractHeavyTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 初始化 turretData
        this.turretData = new TurretData();
    }

    private volatile Vector3d targetPos = new Vector3d(0,0,0);

    public abstract int getmaxpitchdowndegrees();

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public void tick() {
        if(idleTicks > 1) {
            idleTicks = idleTicks-1;
            return;
        }
        onShip = VSGameUtilsKt.isBlockInShipyard(level, this.getBlockPos());
        if (onShip) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
            Vector3d center = VSGameUtilsKt.toWorldCoordinates(ship, this.getBlockPos().getX(), this.getBlockPos().getY()+getYAxisOffset(), this.getBlockPos().getZ());
            currentworldpos = new Vector3d(center.x, center.y, center.z);
        }
        else {
            currentworldpos = new Vector3d(Math.round(this.getBlockPos().getX()*10)/10.0, Math.round((this.getBlockPos().getY()+getYAxisOffset())*10)/10.0, Math.round(this.getBlockPos().getZ()*10)/10.0);
        }

        boolean canTrackBySeatView = needtofire() && !getData().isviewlocked && (getData().firetype == 0 || getData().firetype == 2);
        // 功能：当控制椅视角未锁定且重炮为手动/智能模式时，按玩家视角驱动炮塔实现头瞄。
        if (canTrackBySeatView) {
            targetxrot = (float) Math.toRadians(getData().playerangleX);
            targetyrot = (float) Math.toRadians(-getData().playerangleY);
            this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
            this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
            setAnimData(HAS_TARGET, true);
        }

        // 功能：重型炮塔只有在频道匹配时才响应自动/智能射击，行为与主武器一致。
        if (needtofire() && (getData().firetype == 1 || (getData().firetype == 2 && getData().isviewlocked && !targetPos.equals(new Vector3d(0,0,0))))) {
            LogUtils.getLogger().warn("setting target:"+targetPos);
            updateTargetRot();
            this.xRot0 = closestReachableX(xRot0,getMaxSpinSpeed(),targetxrot);
            this.yRot0 = closestReachableY(yRot0,getMaxSpinSpeed(),targetyrot);
            if(!Objects.equals(targetPos, new Vector3d(0, 0, 0))){
                if(xOK && yOK) {
                    targetdistance = Vec.Distance(currentworldpos, targetPos);
                    shootship();
                    idleTicks = getCoolDown();
                }
            }
            else {
                LogUtils.getLogger().warn("target is null");
                setAnimData(HAS_TARGET, false);
                targetdistance = 0;
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
    public void modifyheavytargettype(int type) {
        if (level == null || level.isClientSide) {
            return; // 客户端完全不许改！
        }
        getData().firetype = type;
    }

    // 功能：为重型炮塔提供与主武器一致的频道切换逻辑（四选一）。
    public void modifychannel(int type) {
        if (level == null || level.isClientSide) {
            return;
        }
        TurretData data = getData();
        if (type == 1) {
            data.channel1 = !data.getChannel1();
            if (data.channel1) {
                data.channel2 = false;
                data.channel3 = false;
                data.channel4 = false;
            }
        }
        if (type == 2) {
            data.channel2 = !data.getChannel2();
            if (data.channel2) {
                data.channel1 = false;
                data.channel3 = false;
                data.channel4 = false;
            }
        }
        if (type == 3) {
            data.channel3 = !data.getChannel3();
            if (data.channel3) {
                data.channel1 = false;
                data.channel2 = false;
                data.channel4 = false;
            }
        }
        if (type == 4) {
            data.channel4 = !data.getChannel4();
            if (data.channel4) {
                data.channel1 = false;
                data.channel2 = false;
                data.channel3 = false;
            }
        }
    }

    // 功能：接收控制椅下发的频道编码，供重型炮塔判定是否允许开火。
    public void receivechannel(int encode) {
        getData().receivingchannel = encode;
    }

    // 功能：判断重型炮塔与控制椅频道是否匹配，逻辑与主武器保持一致。
    public boolean needtofire() {
        for (int i = 0; i < 4; i++) {
            boolean flag = ((getData().receivingchannel >> i) & 1) == 1;
            if (flag && i == 0 && getData().channel1) return true;
            if (flag && i == 1 && getData().channel2) return true;
            if (flag && i == 2 && getData().channel3) return true;
            if (flag && i == 3 && getData().channel4) return true;
        }
        return false;
    }

    public void updatespecificenemy(Vector3d pos) {
        LogUtils.getLogger().warn("update:controlseat setting turret data to:"+targetPos);
            this.targetPos = pos;
    }

    public void updateplayerstatus(boolean isviewlocked, int rotx, int roty) {
        this.getData().isviewlocked = isviewlocked;
        this.getData().playerangleX = rotx;
        this.getData().playerangleY = roty;
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

        boolean onship = VSGameUtilsKt.isBlockInShipyard(level,this.getBlockPos());
        if(onship) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
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
        tag.putInt("firetype", getData().firetype);
        tag.putDouble("distance", this.getTargetdistance());
        tag.putInt("playerxrot",this.getData().playerangleX);
        tag.putInt("playeryrot",this.getData().playerangleY);
        tag.putFloat("xrot",this.targetxrot);
        tag.putFloat("yrot",this.targetyrot);
        tag.putDouble("targetX", targetPos.x);
        tag.putDouble("targetY", targetPos.y);
        tag.putDouble("targetZ", targetPos.z);
        // 功能：同步重型炮塔频道状态与接收频道编码，确保 GUI 与联动状态一致。
        tag.putBoolean("channel1", getData().channel1);
        tag.putBoolean("channel2", getData().channel2);
        tag.putBoolean("channel3", getData().channel3);
        tag.putBoolean("channel4", getData().channel4);
        tag.putInt("receivingchannel", getData().receivingchannel);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        // 确保 turretData 不为 null
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        if (tag.contains("firetype")) {this.getData().firetype = tag.getInt("firetype");}
        if (tag.contains("distance")) {this.targetdistance = tag.getDouble("distance");}
        if(tag.contains("playerxrot")) {this.getData().playerangleX = tag.getInt("playerxrot");}
        if(tag.contains("playeryrot")) {this.getData().playerangleY = tag.getInt("playeryrot");}
        if (tag.contains("xrot")) {this.targetxrot = tag.getFloat("xrot");}
        if (tag.contains("yrot")) {this.targetyrot = tag.getFloat("yrot");}
        if (tag.contains("targetX")) targetPos = new Vector3d(
                tag.getDouble("targetX"),
                tag.getDouble("targetY"),
                tag.getDouble("targetZ")
        );
        // 功能：读取重型炮塔四个频道和接收频道编码，恢复频道联动配置。
        if (tag.contains("channel1")) { this.getData().channel1 = tag.getBoolean("channel1"); }
        if (tag.contains("channel2")) { this.getData().channel2 = tag.getBoolean("channel2"); }
        if (tag.contains("channel3")) { this.getData().channel3 = tag.getBoolean("channel3"); }
        if (tag.contains("channel4")) { this.getData().channel4 = tag.getBoolean("channel4"); }
        if (tag.contains("receivingchannel")) { this.getData().receivingchannel = tag.getInt("receivingchannel"); }
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
