package com.kodu16.vsie.content.turret.heavyturret;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.Initialize;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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

import java.util.List;
import java.util.Objects;

//0:鎵嬪姩 1:鑷姩 2:鏅鸿兘
public abstract class AbstractHeavyTurretBlockEntity extends AbstractTurretBlockEntity {
    protected AbstractHeavyTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 鍒濆鍖?turretData
        this.turretData = new TurretData();
    }

    private volatile Vec3 targetPos = new Vec3(0,0,0);


    public abstract int getmaxpitchdowndegrees();

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public void tick() {
        if (this.getLevel() == null || this.getLevel().isClientSide()) { return; }

        // 鍔熻兘锛氬喎鍗存椂闂翠粎鐢ㄤ簬闄愬埗寮€鐏紝涓嶅啀闃绘柇鐐鐨勬寔缁浆鍚戯紝淇鈥滄墜鍔ㄦā寮忛棿姝囨€у崱椤库€濄€?
        if (idleTicks > 0) {
            idleTicks = idleTicks - 1;
        }

        if (!hasInitialized){
            BlockPos pos = this.getBlockPos();
            BlockState state = this.getBlockState();
            Initialize.initialize(this.getLevel(),pos,state,pivotPoint);

            hasInitialized = true;
            return;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(this.getLevel(), this.getBlockPos());
        onShip = subLevel != null;

        if (subLevel != null) {
            Vec3 center = subLevel.logicalPose().transformPosition(new Vec3(this.getBlockPos().getX(), this.getBlockPos().getY()+getYAxisOffset(), this.getBlockPos().getZ()));
            currentworldpos = new Vec3(center.x, center.y, center.z);
        }
        else {
            currentworldpos = new Vec3(Math.round(this.getBlockPos().getX()*10)/10.0, Math.round((this.getBlockPos().getY()+getYAxisOffset())*10)/10.0, Math.round(this.getBlockPos().getZ()*10)/10.0);
        }

        // 鍔熻兘锛氬綋鐩爣鐐规湁鏁堟椂锛屾棤璁烘墜鍔?鑷姩/鏅鸿兘妯″紡閮芥瘡 tick 鏇存柊涓€娆＄洰鏍囪搴︼紝淇鈥滆嚜鍔ㄦā寮忓畬鍏ㄤ笉杞悜鈥濄€?
        boolean hasTargetPos = !Objects.equals(targetPos, new Vector3d(0, 0, 0));
        if (hasTargetPos) {
            updateTargetRot();
            LogUtils.getLogger().warn("setting target:"+targetPos);
            this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
            this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
            setAnimData(TURRET_HAS_TARGET, true);

            // 鍔熻兘锛氫粎鍦ㄥ凡瀵瑰噯涓斿喎鍗村畬鎴愭椂寮€鐏紝閬垮厤杞悜涓庡紑鐏€昏緫浜掔浉闃诲銆?
            if (xOK && yOK && idleTicks <= 0) {
                targetDistance = Vec.Distance(currentworldpos, targetPos);
                shootship();
                idleTicks = getCoolDown();
            }
        } else {
            LogUtils.getLogger().warn("target is null");
            setAnimData(TURRET_HAS_TARGET, false);
            targetDistance = 0;
            xRot0 = 0;
            yRot0 = 0;
            targetPreVelocity.clear();
        }

        this.setAnimData(XROT, xRot0);
        this.setAnimData(YROT, yRot0);
        this.markUpdated();
    }

    //heavy turret only
    public void modifyFireType(int type) {
        Level currentLevel = this.getLevel();
        if (currentLevel == null || currentLevel.isClientSide) { return; }// 瀹㈡埛绔畬鍏ㄤ笉璁告敼锛?
        getData().fireType = type;
    }

    // 鍔熻兘锛氫负閲嶅瀷鐐鎻愪緵涓庝富姝﹀櫒涓€鑷寸殑棰戦亾鍒囨崲閫昏緫锛堝洓閫変竴锛夈€?
    public void modifyChannel(int channel) {
        // 鍔熻兘锛氬疄鏃惰幏鍙栧綋鍓?level锛屼慨澶嶅洜鐖剁被缂撳瓨 level 涓虹┖鑰屽鑷撮閬撳垏鎹㈣姹傝鎻愬墠 return 鐨勯棶棰樸€?
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

    public boolean needupdateenemy(){
        return getData().fireType ==1 || getData().fireType==2 && getData().isViewLocked;
    }

    // 鍔熻兘锛氭帴鏀舵帶鍒舵涓嬪彂鐨勯閬撶紪鐮侊紝渚涢噸鍨嬬偖濉斿垽瀹氭槸鍚﹀厑璁稿紑鐏€?
    public void channelFromCtrl(int channel) { getData().channelOfCtrl = channel; }

    // 鍔熻兘锛氬垽鏂噸鍨嬬偖濉斾笌鎺у埗妞呴閬撴槸鍚﹀尮閰嶏紝閫昏緫涓庝富姝﹀櫒淇濇寔涓€鑷淬€?
    public boolean isChannelMatch() {
        TurretData data = getData();
        int channel = data.getChannelStatus();
        return (channel & data.channelOfCtrl) != 0;
    }

    public void updatespecificenemy(Vec3 pos) {
        LogUtils.getLogger().warn("update:controlseat setting turret data to:"+targetPos);
            this.targetPos = pos;
    }

    public void updateplayerstatus(boolean isviewlocked, Vec3 manualAimTargetPos) {
        // 鍔熻兘锛氫繚瀛樻帶鍒舵瑙嗚閿佺姸鎬侊紝骞跺皢杈撳叆绔笂浼犵殑鎵嬪姩鐩爣鐐圭洿鎺ヤ綔涓洪噸鍨嬬偖濉?targetPos銆?
        this.getData().isViewLocked = isviewlocked;
        this.targetPos = manualAimTargetPos;
    }

    private void updateTargetRot() {
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        // 1. 鑾峰彇鐐褰撳墠鐨勬湞鍚戯紙鏂瑰潡鐨刦acing锛?
        Vec3 localUp  = Vec3.atLowerCornerOf(facing.getOpposite().getNormal());
        // 2. 鑾峰彇鐐鏈湴鍧愭爣绯荤殑 "鍓? 鍜?"涓? 鍚戦噺锛堜笘鐣屽潗鏍囷級
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

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(this.getLevel(), this.getBlockPos());
        if(subLevel != null) {
            worldXDirection = subLevel.logicalPose().transformNormal(new Vector3d(localForward.x,localForward.y,localForward.z)).normalize();
            worldYDirection = subLevel.logicalPose().transformNormal(new Vector3d(localUp.x,localUp.y,localUp.z)).normalize();
            worldZDirection = subLevel.logicalPose().transformNormal(new Vector3d(localRight.x,localRight.y,localRight.z)).normalize();
        }
        else {
            worldXDirection = new Vector3d(localForward.x,localForward.y,localForward.z);
            worldYDirection = new Vector3d(localUp.x,localUp.y,localUp.z);
            worldZDirection = new Vector3d(localRight.x,localRight.y,localRight.z);

        }

        // 4. 鐩爣鐩稿鐐涓績鐨勫悜閲忥紙涓栫晫鍧愭爣锛?
        Vec3 toTargetWorld = new Vec3(
                targetPos.x - currentworldpos.x,
                targetPos.y - currentworldpos.y,
                targetPos.z - currentworldpos.z
        ).normalize();   // 寤鸿鍏坣ormalize锛屽噺灏戞诞鐐硅宸奖鍝?

        if (toTargetWorld.lengthSqr() < 1e-6) return; // 鐩爣鍦ㄦ涓績锛屾斁寮冭绠?

        // 5. 鎶婁笘鐣屽悜閲忚浆鎹㈠埌鐐鏈湴鍧愭爣绯伙紙鐢ㄥ熀鍚戦噺鍋氱偣绉級
        double localX = toTargetWorld.dot(new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z));     // 鏈湴鍙?
        double localY = toTargetWorld.dot(new Vec3(worldYDirection.x, worldYDirection.y, worldYDirection.z));        // 鏈湴鍚戜笂
        double localZ = toTargetWorld.dot(new Vec3(worldXDirection.x, worldXDirection.y, worldXDirection.z));   // 鏈湴鍚戝墠

        // 6. 鐜板湪灏卞湪鏈湴鍧愭爣绯讳簡锛岃绠楄搴︼紙缁忓吀鍐欐硶锛?
        // yaw   : 宸﹀彸瑙掑害锛宎tan2(x, z)
        // pitch : 涓婁笅瑙掑害锛宎tan2(y, 骞抽潰璺濈)
        double yaw   = Math.atan2(localX, localZ);           // 娉ㄦ剰atan2椤哄簭
        double pitch = Math.atan2(localY, Math.sqrt(localX * localX + localZ * localZ));

        this.targetyrot = (float) -yaw;

        this.targetxrot = (float) pitch;
        //LogUtils.getLogger().warn("X:"+worldXDirection+"Y:"+worldYDirection+"Z:"+worldZDirection+"target:"+targetPos+"turret:"+currentworldpos +"yaw:"+yaw+"pitch:"+pitch);
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
        tag.putInt("firetype", getData().fireType);
        tag.putDouble("distance", this.getTargetDistance());
        tag.putInt("playerxrot",this.getData().playerAngleX);
        tag.putInt("playeryrot",this.getData().playerAngleY);
        tag.putFloat("xrot",this.targetxrot);
        tag.putFloat("yrot",this.targetyrot);
        tag.putDouble("targetX", targetPos.x);
        tag.putDouble("targetY", targetPos.y);
        tag.putDouble("targetZ", targetPos.z);
        // 鍔熻兘锛氬悓姝ラ噸鍨嬬偖濉旈厤缃瘎瀛樺櫒鍜屾帴鏀堕閬撶紪鐮侊紝纭繚 GUI 涓庤仈鍔ㄧ姸鎬佷竴鑷淬€?
        // 棰戦亾閮ㄥ垎琚噸鍐欙紒
        tag.putInt("configregister",this.getData().configRegister);
        tag.putInt("channelofctrl", getData().channelOfCtrl);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        // 纭繚 turretData 涓嶄负 null
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        if (tag.contains("firetype")) {this.getData().fireType = tag.getInt("firetype");}
        if (tag.contains("distance")) {this.targetDistance = tag.getDouble("distance");}
        if(tag.contains("playerxrot")) {this.getData().playerAngleX = tag.getInt("playerxrot");}
        if(tag.contains("playeryrot")) {this.getData().playerAngleY = tag.getInt("playeryrot");}
        if (tag.contains("xrot")) {this.targetxrot = tag.getFloat("xrot");}
        if (tag.contains("yrot")) {this.targetyrot = tag.getFloat("yrot");}
        if (tag.contains("targetX")) targetPos = new Vec3(
                tag.getDouble("targetX"),
                tag.getDouble("targetY"),
                tag.getDouble("targetZ")
        );
        // 鍔熻兘锛氳鍙栭噸鍨嬬偖濉旈厤缃瘎瀛樺櫒鍜屾帴鏀堕閬撶紪鐮侊紝鎭㈠棰戦亾鑱斿姩閰嶇疆銆?
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
