package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;

public abstract class AbstractTurretBlockEntity extends SmartBlockEntity implements GeoBlockEntity, MenuProvider {
    Logger LOGGER = LogUtils.getLogger();

    public static SerializableDataTicket<Boolean> TURRET_HAS_TARGET;

    public boolean hasInitialized = false;//鍊煎緱琚啓鍏bstract绫昏鎵€鏈変汉瀛︿範锛?
    public Level level = this.getLevel();
    public BlockPos pos = this.getBlockPos();
    public BlockState state = this.getBlockState();
    public boolean onShip = false;

    public Vec3 targetPos = new Vec3(0,0,0); //杩欐槸琚€夋嫨鐨勯偅涓洰鏍囩殑浣嶇疆
    public double targetDistance;

    public double getTargetDistance() {
        return targetDistance;
    }

    public @Nullable LivingEntity targetentity;
    private @Nullable SubLevel selectedtargetShip;
    public List<Vector3d> targetPreVelocity = new ArrayList<Vector3d>();

    public int aimtype = 0; //0锛氱┖ 1锛氬疄浣?2锛氳埞鍙?

    public static SerializableDataTicket<Float> XROT; //杩欐槸鍔ㄧ敾璁＄畻鐢ㄧ殑
    public static SerializableDataTicket<Float> YROT;
    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this); // 鍔熻兘锛氫繚鐣欌€滄槸鍚︽湁鐩爣鈥濈殑鍔ㄧ敾鍚屾鏍囪锛屽幓闄ゅ Mekanism 鐢佃剳闆嗘垚娉ㄨВ鐨勪緷璧栥€?


    public static Vector3d pivotPoint = new Vector3d(); // 妯″瀷涓殑鏋㈣酱鐐?姝ゅ悗浼氭嵁姝よ嚜鍔ㄨ绠楁灑杞寸偣鐨勫亸绉?

    public int idleTicks = 0;
    // 鍔熻兘锛氳褰曠偖鍙ｇ伀鐒板墿浣欐樉绀烘椂闂达紙鍗曚綅锛歵ick锛夛紝鐢ㄤ簬瀹炵幇鈥滃紑鐏悗寤惰繜鐔勭伃鈥濇晥鏋溿€?
    public int muzzleFlashTicks = 0;

    // 鍔熻兘锛氳鍙栫矑瀛愮偖 firepoint 鍧愭爣锛岃繑鍥炲壇鏈伩鍏嶅閮ㄦ剰澶栦慨鏀瑰唴閮ㄧ姸鎬併€?
    // 鍔熻兘锛氫繚瀛樺鎴风涓婁紶鐨?firepoint 鍧愭爣锛岀矑瀛愮偖寮€鐏椂鐩存帴浣滀负瀛愬脊鐢熸垚鐐逛娇鐢ㄣ€?
    private Vector3d FirePoint = null;
    // 鍔熻兘锛氳褰曗€滃鑸拌埞寮€鐏€濇椂娌垮綋鍓嶆湞鍚戝皠绾挎娴嬪埌鐨勬柟鍧楀潗鏍囷紝浠呬繚鐣欐渶杩戜竴娆＄粨鏋滐紙鏃犻渶 NBT 鍚屾锛夈€?
    private BlockPos lastShipShotHitBlockPos = BlockPos.ZERO;
    // 鍔熻兘锛氭爣璁扳€滄湰娆″鑸拌埞灏勭嚎鏄惁鍏堝懡涓簡鑷韩鎵€鍦ㄨ埞浣撯€濓紝鐢ㄤ簬闃绘璇激鑷韩鑸颁綋鐨勫紑鐏€?
    private boolean shipShotBlockedBySelfShip = false;

    public Vector3d getFirePoint() {
        return FirePoint;
    }

    public BlockPos getLastShipShotHitBlockPos() {
        return lastShipShotHitBlockPos;
    }

    private static final double SEARCH_RADIUS = 128.0;

    public Vec3 currentworldpos = new Vec3(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
    protected TurretData turretData;

    public Vector3d worldXDirection = new Vector3d(0,0,0);
    public Vector3d worldYDirection = new Vector3d(0,0,0);
    public Vector3d worldZDirection = new Vector3d(0,0,0);

    protected AbstractTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 鍒濆鍖?turretData
        this.turretData = new TurretData();
    }

    public TurretData getData() {
        if (turretData == null) { turretData = new TurretData(); }
        return turretData;
    }

    public void modifyTargetType(int type) {
        // 鍔熻兘锛氭瘡娆′慨鏀规椂閮藉疄鏃惰鍙栨柟鍧楀疄浣撳綋鍓?level锛岄伩鍏嶄娇鐢ㄦ瀯閫犳湡缂撳瓨鐨勭┖ level 瀵艰嚧鎸夐挳鏃犳晥銆?
        this.level = this.getLevel();
        if (level == null || level.isClientSide) { return; }

        TurretData data = getData();

        if(type==4){
            this.aimtype = 2;
            data.flip(data.TARGET_SHIP);
            if ( data.isTargetsShip() ) { data.reset(( data.TARGET_HOSTILE | data.TARGET_PASSIVE | data.TARGET_PLAYER )); }
        }
        else{
            this.aimtype = 1;
            if(type==1){ data.flip(data.TARGET_HOSTILE); }
            if(type==2){ data.flip(data.TARGET_PASSIVE); }
            if(type==3){ data.flip(data.TARGET_PLAYER); }
        }
        if (data.getTargetStatus()==data.TARGET_MANUAL) { this.aimtype = 0; }

        else if ((data.getTargetStatus()&(~data.TARGET_SHIP))!=0) { data.reset(data.TARGET_SHIP); }
    }

    public void modifydefaultspin(int spinx, int spiny) {
        this.defaultspinx = spinx;
        this.defaultspiny = spiny;
    }

    public void tick() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) { return; }

        if (!hasInitialized){
            BlockPos pos = this.getBlockPos();
            BlockState state = this.getBlockState();
            Initialize.initialize(level,pos,state,pivotPoint);

            hasInitialized = true;
            return;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,pos);

        if (subLevel != null) {
            Vector3d pivotoffsetworld = subLevel.logicalPose().transformNormal(this.turretData.getBasePivotOffset().normalize().mul(this.turretData.getBasePivotOffset().length())
            );
            this.turretData.setWorldPivotOffset(pivotoffsetworld);

            // 鍔熻兘锛氬喎鍗存椂闂翠粎鐢ㄤ簬绂佹寮€鐏紝涓嶅啀闃绘柇绱㈡晫涓庤浆鍚戦€昏緫銆俿
            if (idleTicks > 0) {
                idleTicks = idleTicks - 1;
            }
            // 鍔熻兘锛氭瘡 tick 琛板噺鐐彛鐏劙鏄剧ず璁℃椂锛岃鏃剁粨鏉熷悗鑷姩闅愯棌鐏劙灞傘€?
            if (muzzleFlashTicks > 0) {
                muzzleFlashTicks = muzzleFlashTicks - 1;
            }
            // 鍔熻兘锛氱粺涓€鍒锋柊鐐涓栫晫鍧愭爣锛屽噺灏?tick 涓绘祦绋嬪垎鏀鏉傚害銆?
            currentworldpos = subLevel.logicalPose().transformPosition(Vec3.atLowerCornerOf(pos));
            // 鍔熻兘锛氱粺涓€澶勭悊鐩爣鎼滅储锛岃嫢鏃犳湁鏁堢洰鏍囧垯璁╃偖濉斿洖褰掗粯璁よ搴︺€?
            acquireTargetByAimType();
            tryInvalidateTarget();

            if (hasValidTarget()) {
                // 鍔熻兘锛氱淮鎶ら€熷害閲囨牱绐楀彛锛屼负寮归亾棰勬祴鎻愪緵鏈€杩戠Щ鍔ㄨ秼鍔裤€?
                appendTargetVelocitySample();
                // 鍔熻兘锛氱粺涓€鏇存柊褰撳墠鐩爣鐐癸紝閬垮厤瀹炰綋/鑸拌埞閲嶅鍒嗘敮浠ｇ爜銆?
                updateCurrentTargetPos();

                targetPos = getShootLocation(targetPos, targetPreVelocity, level, currentworldpos);
                updateTargetRot();
                this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
                this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
                if (xOK && yOK) {
                    fireWhenLocked();
                }
            } else {
                // 鍔熻兘锛氬綋鍛ㄥ洿娌℃湁鏈夋晥鏁屼汉鏃讹紝骞虫粦鍥炲埌鐢ㄦ埛璁剧疆鐨勯粯璁や刊浠?鍋忚埅瑙掋€?
                returnToDefaultRotation();
            }
            //LogUtils.getLogger().warn("targetx:"+targetxrot+"y:"+targetyrot+"currentx:"+xRot0+"y:"+yRot0+"OK?"+xOK+yOK);
            this.setAnimData(XROT, xRot0);
            this.setAnimData(YROT, yRot0);
            this.markUpdated();
        }
    }

    // 鍔熻兘锛氭寜褰撳墠绱㈡晫妯″紡灏濊瘯鑾峰彇鐩爣锛岄伩鍏嶅湪 tick 涓婚€昏緫涓暎钀藉灞?if銆?
    private void acquireTargetByAimType() {
        if (aimtype == 1) {
            tryFindTargetEntity();
        } else if (aimtype == 2) {
            // 鍔熻兘锛氳埌鑸圭储鏁屾瘡 tick 閮芥墽琛屼竴娆★紝浠ヤ究鍦ㄦ晫鑸板垪琛ㄦ竻绌烘椂绔嬪嵆閫€鍑虹储鏁屽苟鍥炲綊榛樿濮挎€併€?
            tryFindtargetShip();
        }
    }

    // 鍔熻兘锛氱粺涓€鍒ゆ柇鈥滃綋鍓嶆槸鍚︽寔鏈夋湁鏁堢洰鏍団€濄€?
    private boolean hasValidTarget() {
        return (aimtype == 1 && isValidTargetEntity(targetentity))
                || (aimtype == 2 && isValidTargetShip(selectedtargetShip));
    }

    // 鍔熻兘锛氱淮鎶ゆ渶澶?5 鏉＄洰鏍囬€熷害鍘嗗彶锛屼緵棰勬祴寮归亾鏃朵娇鐢ㄣ€?
    private void appendTargetVelocitySample() {
        if (targetPreVelocity.size() >= 5) {
            targetPreVelocity.remove(0);
        }
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPreVelocity.add(new Vector3d(targetentity.getDeltaMovement().x, targetentity.getDeltaMovement().y, targetentity.getDeltaMovement().z));
        } else if (aimtype == 2 && isValidTargetShip(selectedtargetShip)) {
            RigidBodyHandle rigidBodyHandle = RigidBodyHandle.of((ServerSubLevel) selectedtargetShip);
            targetPreVelocity.add(rigidBodyHandle.getLinearVelocity(new Vector3d()));
        }
    }

    // 鍔熻兘锛氭牴鎹洰鏍囩被鍨嬫洿鏂板綋鍓嶇瀯鍑嗙偣锛屼緵鍚庣画寮归亾棰勬祴涓庢棆杞绠椾娇鐢ㄣ€?
    private void updateCurrentTargetPos() {
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPos = new Vec3(
                    targetentity.getX(),
                    targetentity.getY(),
                    targetentity.getZ()
            );
            return;
        }
        if (aimtype == 2 && isValidTargetShip(selectedtargetShip)) {
            // 鍔熻兘锛氬鑸拌埞鐩爣姣?tick 閮介噸鏂伴€夋嫨鈥滃彲瑙佸琛ㄩ潰鐐光€濓紝閬垮厤濮嬬粓鍥炲啓涓鸿埞鍙腑蹇冪偣銆?
            targetPos = getShipAimPoint(selectedtargetShip);
        }
    }

    // 鍔熻兘锛氬湪鐐彛瀹屾垚瀵瑰噯鏃惰Е鍙戝紑鐏紝骞惰缃粺涓€鍐峰嵈銆?
    private void fireWhenLocked() {
        //LogUtils.getLogger().warn("shooting");
        // 鍔熻兘锛氬喎鍗存湡闂村厑璁哥户缁储鏁屼笌鏃嬭浆锛屼絾绂佹閲嶅寮€鐏€?
        if (idleTicks > 0) {
            return;
        }
        // 鍔熻兘锛氬厑璁稿瓙绫诲０鏄庘€滃綋鍓嶆槸鍚︽弧瓒冲紑鐏祫婧愭潯浠垛€濓紙濡傚脊鑽粨锛夛紝涓嶆弧瓒虫椂浠呯瀯鍑嗕笉灏勫嚮銆?
        if (!canShootCurrentTarget()) {
            return;
        }
        if (aimtype == 1) {
            targetDistance = Vec.Distance(currentworldpos, targetPos);
            shootentity();
            idleTicks = getCoolDown();
            // 鍔熻兘锛氬疄浣撶洰鏍囧紑鐏悗淇濇寔 0.5 绉掔偖鍙ｇ伀鐒版樉绀猴紙20tick/s * 0.5s = 10tick锛夈€?
            muzzleFlashTicks = 10;
        } else if (aimtype == 2) {
            targetDistance = Vec.Distance(currentworldpos, targetPos);
            // 鍔熻兘锛氫粎鍦ㄥ鑸拌埞灏勫嚮鏃舵墽琛屼竴娆?clip 妫€娴嬶紝骞惰褰曞綋鍓嶆湞鍚戝懡涓殑鏂瑰潡鍧愭爣銆?
            recordShipShotHitBlockPos();
            // 鍔熻兘锛氬鏋滃皠绾垮厛鍛戒腑鑷韩鎵€鍦ㄨ埞浣擄紝鍒欏垽瀹氱洰鏍囦笉鍙骞剁洿鎺ュ彇娑堟湰娆″紑鐏€?
            if (shipShotBlockedBySelfShip) {
                return;
            }
            shootship();
            idleTicks = getCoolDown();
            // 鍔熻兘锛氳埌鑸圭洰鏍囧紑鐏悗鍚屾牱淇濇寔 0.5 绉掔偖鍙ｇ伀鐒版樉绀恒€?
            muzzleFlashTicks = 10;
        }
    }



    //use 5 ticks' velocity data to predict movement,providing more accurate prediction
    public abstract Vec3 getShootLocation(Vec3 vec, List<Vector3d> preV, Level lv, Vec3 pos);

    //鑸逛笉鏄疄浣擄紝闇€瑕佷竴濂楀崟鐙殑绱㈡晫閫昏緫锛岃繖涔熸槸涓哄暐涓嶈兘鍚屾椂绱㈡晫瀹炰綋鍜岃埞

    public abstract String getturrettype();

    public abstract double getYAxisOffset();

    public abstract double getcannonlength();//鐢ㄤ簬鐐彛鐗规晥鍙戝皠浣嶇疆鐨勮绠楋紙鐪熸伓蹇冿級

    public abstract float getMaxSpinSpeed();

    public abstract int getCoolDown();

    public abstract int getenergypertick();

    public abstract void shootentity();

    public abstract void shootship();

    // 鍔熻兘锛氬瓙绫诲彲瑕嗗啓璇ユ柟娉曟帶鍒垛€滄槸鍚﹀厑璁告湰 tick 寮€鐏€濓紝榛樿濮嬬粓鍏佽銆?
    protected boolean canShootCurrentTarget() {
        return true;
    }

    // 鍔熻兘锛氬厑璁稿瓙绫诲０鏄?Geo 妯″瀷涓?turret 楠ㄩ鐨勬灑杞寸偣锛堝崟浣嶏細妯″瀷鍍忕礌锛屽師鐐逛负鏂瑰潡鏈湴鍘熺偣锛夈€?
    protected Vector3d getTurretPivotInGeoPixels() {
        return new Vector3d(0.0, 0.0, 0.0);
    }

    // 鍔熻兘锛氬厑璁稿瓙绫诲０鏄?Geo 妯″瀷涓?cannon 楠ㄩ鐨勬灑杞寸偣锛堝崟浣嶏細妯″瀷鍍忕礌锛屽師鐐逛负鏂瑰潡鏈湴鍘熺偣锛夈€?
    protected Vector3d getCannonPivotInGeoPixels() {
        return new Vector3d(0.0, 0.0, 0.0);
    }


    public void updateenemy(ArrayList<SubLevel> enemyshipsData) {
        this.getData().enemyShipsData = enemyshipsData;
    }

    private void tryInvalidateTarget() {
        if(aimtype==1) {
            if(!isValidTargetEntity(targetentity)) {
                setAnimData(TURRET_HAS_TARGET, false);
                targetentity = null;
                targetDistance = 0;
                targetPreVelocity.clear();
            }
        }
        else if(aimtype==2) {
            if(!isValidTargetShip(selectedtargetShip)) {
                setAnimData(TURRET_HAS_TARGET, false);
                selectedtargetShip = null;
                targetDistance = 0;
                targetPreVelocity.clear();
            }
        }
    }

    public void tryFindTargetEntity() {
        // 鍔熻兘锛氱储鏁岄樁娈典笉鍐嶆敼鍔ㄥ紑鐏喎鍗达紝閬垮厤鍐峰嵈涓庣储鏁屽叡鐢ㄨ鏁板櫒瀵艰嚧鎶栧姩銆?
        if (targetentity != null && targetentity.isAlive()) return; // 鏈夋椿鐩爣灏变笉閲嶅鎵?

        if ((this.getLevel().getGameTime() + this.hashCode()) % 5 != 0) return;

        AABB searchBox = new AABB(
                currentworldpos.x - SEARCH_RADIUS,
                currentworldpos.y - SEARCH_RADIUS,
                currentworldpos.z - SEARCH_RADIUS,
                currentworldpos.x + SEARCH_RADIUS,
                currentworldpos.y + SEARCH_RADIUS,
                currentworldpos.z + SEARCH_RADIUS
        );

        List<LivingEntity> candidates = this.getLevel().getEntitiesOfClass(LivingEntity.class, searchBox, this::isValidTargetEntity);

        if (candidates.isEmpty()) {
            return;
        }

        targetentity = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z)))
                .orElse(null);
        // 鍏抽敭锛氳繖閲屼竴瀹氳鍚屾鏇存柊 targetPos锛侊紒
        this.targetPos = new Vec3(
                targetentity.getX(),
                targetentity.getY(),
                targetentity.getZ()
        );
        setChanged();
    }

    // 鍔熻兘锛氱储鏁岄樁娈典笉鍐嶆敼鍔ㄥ紑鐏喎鍗达紝閬垮厤鍐峰嵈涓庣储鏁屽叡鐢ㄨ鏁板櫒瀵艰嚧鎶栧姩銆?
    public void tryFindtargetShip() {
        ArrayList<SubLevel> enemylist = getData().enemyShipsData;
        // 鍔熻兘锛氳嫢鏁岃埌鍒楄〃涓虹┖锛岀珛鍒绘竻鐞嗗綋鍓嶈埌鑸圭洰鏍囷紝纭繚鐐鍋滄缁х画杩借釜宸插け鏁堢洰鏍囥€?
        if (enemylist.isEmpty()) {
            selectedtargetShip = null;
            targetDistance = 0;
            targetPreVelocity.clear();
            setAnimData(TURRET_HAS_TARGET, false);
            return;
        }
        // 鍔熻兘锛氬綋褰撳墠鐩爣鑸板凡涓嶅湪鏈€鏂版晫鑸板垪琛ㄤ腑鏃讹紝瑙嗕负澶辨晥鐩爣锛岃Е鍙戦噸閫夈€?
        if (selectedtargetShip != null && !enemylist.contains(selectedtargetShip)) {
            selectedtargetShip = null;
        }
        // 鍔熻兘锛氬綋鍓嶇洰鏍囦粛鍙涓旀湁鏁堟椂淇濇寔閿佸畾锛岄伩鍏嶆棤鎰忎箟鎶栧姩鍒囨崲鐩爣銆?
        if (isValidTargetShip(selectedtargetShip)) return;

        this.selectedtargetShip = enemylist.stream()
                .filter(this::isValidTargetShip)
                .min(Comparator.comparingDouble(ship -> {
                    Vec3 shipPos = ServerShipUtils.getStructureCenterWorld(ship);
                    return Vec.Distance(currentworldpos,shipPos);
                }))
                .orElse(null);

        if (this.selectedtargetShip != null) {
            // 鍔熻兘锛氳埌鑸圭洰鏍囨敼涓衡€滃彲瑙佺瀯鍑嗙偣鈥濓紙浼樺厛鍙澶栬〃闈級锛岄伩鍏嶇洰鏍囩偣钀藉湪鑸逛綋鍐呴儴瀵艰嚧姘歌繙鏃犳硶閿佸畾銆?
            this.targetPos = getShipAimPoint(this.selectedtargetShip);
            setChanged();
        } else {
            // 鍔熻兘锛氬綋鎵€鏈夋晫鑸伴兘涓嶅彲瑙?涓嶅彲鐢ㄦ椂锛屾竻绌洪攣瀹氾紝浜ょ敱涓绘祦绋嬪洖鍒伴粯璁よ搴︺€?
            targetDistance = 0;
            targetPreVelocity.clear();
            setAnimData(TURRET_HAS_TARGET, false);
        }
    }

    // 鍙礋璐ｅ疄浣撳垽鏂紝杈撳叆鐨勫彧鏈夊疄浣?
    public boolean isValidTargetEntity(@Nullable LivingEntity e) {

        if (e == null) {
            return false;
        }
        if (!e.isAlive()) {
            return false;
        }
        MobCategory category = e.getType().getCategory();
        if (    getData().isTargetsHostile() && category.isFriendly() ||
                getData().isTargetsPassive() && !category.isFriendly() ||
                getData().isTargetsPlayers() && e instanceof Player player && player.isCreative()) {
            return false;
        }

        // 璺濈鍒ゆ柇锛堢敤涓栫晫鍧愭爣锛?
        double distSq = e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        if (distSq > SEARCH_RADIUS * SEARCH_RADIUS) {
            return false;
        }
        // 瑙嗙嚎鍒ゆ柇锛堢溂鐫涗綅缃洿鍑嗭級
        return canSeeTarget(new Vec3(e.getX(), e.getY(), e.getZ()));
    }

    private boolean isValidTargetShip(SubLevel ship) {
        if(ship == null) {
            return false;
        }
        Vec3 shippos = ServerShipUtils.getStructureCenterWorld(ship);
        Vec3 pos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        double distance = Vec.Distance(pos, shippos);
        if(distance > 1280) {
            return false;
        }
        // 鍔熻兘锛氳埌鑸逛娇鐢ㄤ笓闂ㄧ殑鍙鎬у垽瀹氾紙澶氶噰鏍风偣锛夛紝閬垮厤鍙娴嬭川蹇冩椂琚埞浣撹嚜韬伄鎸°€?
        return canSeeShipTarget(ship);
    }

    // 鍔熻兘锛氬鑸拌埞AABB鐨勫涓琛ㄩ潰鐐瑰仛瑙嗙嚎妫€娴嬶紝鍙鏈変竴涓彲瑙佺偣鍗冲垽瀹氬彲瑙併€?
    private boolean canSeeShipTarget(SubLevel ship) {
        // 鍔熻兘锛氬鐢ㄧ粺涓€閲囨牱鐐归泦鍚堬紙澶栬〃闈?涓績锛夊仛鍙鎬у垽瀹氾紝淇濊瘉绱㈡晫涓庣瀯鍑嗚鍒欎竴鑷淬€?
        for (Vec3 samplePoint : getShipAimCandidates(ship)) {
            if (canSeeTarget(samplePoint)) {
                return true;
            }
        }
        return false;
    }

    // 鍔熻兘锛氳繑鍥炰竴涓紭鍏堝彲瑙佺殑鑸拌埞鐬勫噯鐐癸紝鍑忓皯鐐鍦ㄢ€滀笉鍙璐ㄥ績鈥濅笂鍙嶅閲嶉€夌洰鏍囩殑鎶芥悙銆?
    private Vec3 getShipAimPoint(SubLevel ship) {
        // 鍔熻兘锛氭寜鈥滃琛ㄩ潰鐐逛紭鍏堛€佷腑蹇冪偣鍏滃簳鈥濈殑椤哄簭杩斿洖鐬勫噯鐐癸紝浼樺厛鍑绘墦鑳界湅瑙佺殑浣嶇疆銆?
        for (Vec3 samplePoint : getShipAimCandidates(ship)) {
            if (canSeeTarget(samplePoint)) {
                return samplePoint;
            }
        }
        // 鍔熻兘锛氬綋鍏ㄩ儴鐐归兘涓嶅彲瑙佹椂锛屽洖閫€鍒拌埌鑸逛腑蹇冿紝閬垮厤杩斿洖绌哄€煎鑷村悗缁棆杞紓甯搞€?
        return ServerShipUtils.getStructureCenterWorld(ship);
    }

    // 鍔熻兘锛氱敓鎴愯埌鑸笰ABB鐨勫€欓€夌瀯鍑嗙偣锛堝澶栬〃闈㈢偣+涓績鍏滃簳锛夛紝鐢ㄤ簬鍙鎬ф娴嬪拰灏勫嚮灏濊瘯銆?
    private List<Vec3> getShipAimCandidates(SubLevel ship) {
        BoundingBox3dc worldAabb = ship.boundingBox();
        double minX = worldAabb.minX();
        double minY = worldAabb.minY();
        double minZ = worldAabb.minZ();
        double maxX = worldAabb.maxX();
        double maxY = worldAabb.maxY();
        double maxZ = worldAabb.maxZ();
        double centerX = (minX + maxX) * 0.5;
        double centerY = (minY + maxY) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;

        List<Vec3> samplePoints = new ArrayList<>();
        // 鍔熻兘锛氬厛灏濊瘯鍏釜闈腑蹇冪偣锛岃绠楁垚鏈綆涓旇兘瑕嗙洊澶у鏁板彲瑙佹儏鍐点€?
        samplePoints.add(new Vec3(minX, centerY, centerZ));
        samplePoints.add(new Vec3(maxX, centerY, centerZ));
        samplePoints.add(new Vec3(centerX, minY, centerZ));
        samplePoints.add(new Vec3(centerX, maxY, centerZ));
        samplePoints.add(new Vec3(centerX, centerY, minZ));
        samplePoints.add(new Vec3(centerX, centerY, maxZ));
        // 鍔熻兘锛氬啀灏濊瘯鍥涗釜涓婅〃闈㈣鐐癸紝鎻愬崌閬尅鍦烘櫙涓嬫壘鍒板彲瑙佺偣鐨勬鐜囥€?
        samplePoints.add(new Vec3(minX, maxY, minZ));
        samplePoints.add(new Vec3(minX, maxY, maxZ));
        samplePoints.add(new Vec3(maxX, maxY, minZ));
        samplePoints.add(new Vec3(maxX, maxY, maxZ));
        // 鍔熻兘锛氭渶鍚庡姞鍏ヤ腑蹇冪偣浣滀负鍏滃簳鐩爣锛岄伩鍏嶅畬鍏ㄥけ鍘荤洰鏍囥€?
        samplePoints.add(new Vec3(centerX, centerY, centerZ));
        return samplePoints;
    }

    private boolean canSeeTarget(Vec3 pos) {
        Vec3 turretpos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        // 鍔熻兘锛氱Щ闄ゅ潗鏍囧洓鑸嶄簲鍏ワ紝閬垮厤瑙嗙嚎鍒ゆ柇鍦ㄨ竟鐣屽鎶栧姩瀵艰嚧鐐鎶芥悙銆?
        Vec3 targetPos = new Vec3(pos.x(), pos.y(), pos.z());
        Vec3 lookVec = turretpos.vectorTo(targetPos).normalize().scale(0.75F);
        ClipContext ctx = new ClipContext(turretpos.add(lookVec), targetPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty());
        return this.getLevel().clip(ctx).getType().equals(HitResult.Type.MISS);
    }

    // 鍔熻兘锛氬垽鏂寚瀹氭柟鍧楁槸鍚﹀睘浜庣偖濉旇嚜韬墍鍦ㄨ埞浣擄紝鐢ㄤ簬鎷︽埅鈥滆灏嗚嚜韬埞浣撳綋浣滃彲鏀诲嚮鐩爣鈥濈殑鎯呭喌銆?
    private boolean isBlockOnSameShipAsTurret(BlockPos blockPos) {
        Level level = this.getLevel();
        if (level == null) {
            return false;
        }
        SubLevel turretShip = ServerShipUtils.getSubLevelAtBlockPos(level,pos);
        if (turretShip == null) {
            return false;
        }
        SubLevel hitShip = ServerShipUtils.getSubLevelAtBlockPos(level,blockPos);
        return hitShip != null && hitShip.hashCode() == turretShip.hashCode();
    }

    // 鍔熻兘锛氬鑸拌埞鐩爣寮€鐏椂锛屽熀浜庘€滅偖鍙ｅ綋鍓嶄綅缃?-> 褰撳墠鐩爣鐐光€濇墽琛?clip锛岃褰曞懡涓殑鏂瑰潡 BlockPos銆?
    private void recordShipShotHitBlockPos() {
        Level level = this.getLevel();
        // 鍔熻兘锛氭瘡娆″紑鐏墠鍏堥噸缃€滆鑷韩鑸逛綋閬尅鈥濇爣璁帮紝閬垮厤娌跨敤涓婁竴娆＄粨鏋滃鑷磋鍒ゃ€?
        this.shipShotBlockedBySelfShip = false;
        if (level == null) {
            this.lastShipShotHitBlockPos = BlockPos.ZERO;
            return;
        }
        Vec3 from = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        List<Vec3> shotCandidates = new ArrayList<>();
        // 鍔熻兘锛氫紭鍏堝皾璇曞綋鍓嶅凡閿佸畾鐩爣鐐癸紝淇濊瘉鐐彛瑙嗚涓庣湡瀹炲皠绾夸竴鑷淬€?
        shotCandidates.add(targetPos);
        if (isValidTargetShip(selectedtargetShip)) {
            // 鍔熻兘锛氳嫢棣栦釜鐩爣鐐瑰皠绾胯惤绌猴紝鍒欑户缁皾璇曡埌鑸瑰叾浣欏琛ㄩ潰閲囨牱鐐广€?
            for (Vec3 candidate : getShipAimCandidates(selectedtargetShip)) {
                if (Vec.Distance(candidate,targetPos) > 1.0e-6) {
                    shotCandidates.add(candidate);
                }
            }
        }

        for (Vec3 shotPoint : shotCandidates) {
            Vec3 to = new Vec3(shotPoint.x, shotPoint.y, shotPoint.z);
            // 鍔熻兘锛氭瀯閫犱笌姝﹀櫒绫讳技鐨勬柟鍧楃鎾炲皠绾匡紝鍙娴嬫柟鍧楋紝涓嶆娴嬪疄浣撱€?
            ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
            BlockHitResult hitResult = level.clip(context);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hitResult.getBlockPos();
                // 鍔熻兘锛氳嫢灏勭嚎鍏堝懡涓嚜韬埞浣擄紝鍒欑洿鎺ュ垽瀹氣€滅洰鏍囪鑷韩閬尅鈥濓紝缁堟鍚庣画寮€鐏€?
                if (isBlockOnSameShipAsTurret(hitPos)) {
                    this.shipShotBlockedBySelfShip = true;
                    this.lastShipShotHitBlockPos = BlockPos.ZERO;
                    return;
                }
                // 鍔熻兘锛氬懡涓悗鍚屾鏇存柊褰撳墠鐬勫噯鐐癸紝浣垮悗缁紑鐏寔缁鍑嗗彲鎵撳嚮浣嶇疆銆?
                this.targetPos = shotPoint;
                this.lastShipShotHitBlockPos = hitPos;
                return;
            }
        }
        // 鍔熻兘锛氭墍鏈夊€欓€夌偣閮芥湭鍛戒腑鏂瑰潡鏃堕噸缃负 ZERO锛岄伩鍏嶄繚鐣欐棫鏁版嵁璇垽銆?
        this.lastShipShotHitBlockPos = BlockPos.ZERO;
    }

    @Override
    public double getTick(Object BlockEntity) {
        return RenderUtil.getCurrentTick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 纭繚 turretData 琚纭垵濮嬪寲
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        markUpdated();
    }

    public void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        //if(!this.level.isClientSide()) sendUpdatePacket();
    }


    @Override
    public Component getDisplayName() {
        return Component.literal("Turret Screen");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TurretContainerMenu(containerId, inv, this);
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
        tag.putInt("aimtype", aimtype);
        tag.putInt("configregister",turretData.configRegister);
        tag.putDouble("distance", this.getTargetDistance());
        tag.putFloat("xrot",this.targetxrot);
        tag.putFloat("yrot",this.targetyrot);
        tag.putInt("defaultxrot",this.defaultspinx);
        tag.putInt("defaultyrot",this.defaultspiny);
        // 鍔熻兘锛氬悓姝ョ偖鍙ｇ伀鐒板墿浣欐椂闂村埌瀹㈡埛绔紝纭繚娓叉煋灞傚彲鎸夋椂鏄剧ず/鐔勭伃銆?
        tag.putInt("muzzleFlashTicks", this.muzzleFlashTicks);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        // 纭繚 turretData 涓嶄负 null
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        if (tag.contains("aimtype")) {this.aimtype = tag.getInt("aimtype");}
        if (tag.contains("configregister")) {turretData.configRegister=tag.getInt("configregister");}
        if (tag.contains("distance")) {this.targetDistance = tag.getDouble("distance");}
        if (tag.contains("xrot")) {this.targetxrot = tag.getFloat("xrot");}
        if (tag.contains("yrot")) {this.targetyrot = tag.getFloat("yrot");}
        if (tag.contains("defaultyrot")) {this.defaultspiny = tag.getInt("defaultyrot");}
        if (tag.contains("defaultxrot")) {this.defaultspinx = tag.getInt("defaultxrot");}
        if (tag.contains("muzzleFlashTicks")) {this.muzzleFlashTicks = tag.getInt("muzzleFlashTicks");}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public float closestReachableX(float current, float maxChange, float target) {
        // 鍏堟妸 target 鎷夊埌 current 卤180掳 鑼冨洿鍐?
        float delta = target - current;
        delta = (delta + Mth.PI) % (Mth.TWO_PI) - Mth.PI;  // -蟺 ~ +蟺

        float minAllowed = -maxChange;
        float maxAllowed = maxChange;

        float move;
        if (delta < minAllowed) {
            move = minAllowed;
            this.xOK = false;
        } else if (delta > maxAllowed) {
            move = maxAllowed;
            this.xOK = false;
        } else {
            move = delta;
            this.xOK = true;
        }

        return current + move;
    }

    public float closestReachableY(float current, float maxChange, float target) {
        // 鍏堟妸 target 鎷夊埌 current 卤180掳 鑼冨洿鍐?
        float delta = target - current;
        delta = (delta + Mth.PI) % (Mth.TWO_PI) - Mth.PI;  // -蟺 ~ +蟺

        float minAllowed = -maxChange;
        float maxAllowed = maxChange;

        float move;
        if (delta < minAllowed) {
            move = minAllowed;
            this.yOK = false;
        } else if (delta > maxAllowed) {
            move = maxAllowed;
            this.yOK = false;
        } else {
            move = delta;
            this.yOK = true;
        }

        return current + move;
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

        SubLevel ship = ServerShipUtils.getSubLevelAtBlockPos(this.getLevel(), this.getBlockPos());
        worldXDirection = ship.logicalPose().transformNormal(new Vector3d(localForward.x,localForward.y,localForward.z));
        worldXDirection.normalize();
        worldYDirection = ship.logicalPose().transformNormal(new Vector3d(localUp.x,localUp.y,localUp.z));
        worldYDirection.normalize();
        worldZDirection = ship.logicalPose().transformNormal(new Vector3d(localRight.x,localRight.y,localRight.z));
        worldZDirection.normalize();

        // 4. 鐩爣鐩稿鐐涓績鐨勫悜閲忥紙涓栫晫鍧愭爣锛?
        Vec3 toTargetWorld = new Vec3(
                targetPos.x - currentworldpos.x,
                targetPos.y - currentworldpos.y,
                targetPos.z - currentworldpos.z
        ).normalize();   // 寤鸿鍏坣ormalize锛屽噺灏戞诞鐐硅宸奖鍝?

        if (toTargetWorld.lengthSqr() < 1e-6) return; // 鐩爣鍦ㄦ涓績锛屾斁寮冭绠?

        // 5. 鎶婁笘鐣屽悜閲忚浆鎹㈠埌鐐鏈湴鍧愭爣绯伙紙鐢ㄥ熀鍚戦噺鍋氱偣绉級
        double localZ = toTargetWorld.dot(new Vec3(worldXDirection.x,worldXDirection.y,worldXDirection.z));     // 鏈湴鍙?
        double localY = toTargetWorld.dot(new Vec3(worldYDirection.x,worldYDirection.y,worldYDirection.z));        // 鏈湴鍚戜笂
        double localX = toTargetWorld.dot(new Vec3(worldZDirection.x,worldZDirection.y,worldZDirection.z));

        // 6. 鐜板湪灏卞湪鏈湴鍧愭爣绯讳簡锛岃绠楄搴︼紙缁忓吀鍐欐硶锛?
        // yaw   : 宸﹀彸瑙掑害锛宎tan2(x, z)
        // pitch : 涓婁笅瑙掑害锛宎tan2(y, 骞抽潰璺濈)
        double yaw   = Math.atan2(localX, localZ);           // 娉ㄦ剰atan2椤哄簭
        double pitch = Math.atan2(localY, Math.sqrt(localX * localX + localZ * localZ));

        this.targetyrot = (float) -yaw;

        this.targetxrot = (float) pitch;
        //LogUtils.getLogger().warn("X:"+worldXDirection+"Y:"+worldYDirection+"Z:"+worldZDirection+"target:"+targetPos+"turret:"+currentworldpos +"yaw:"+yaw+"pitch:"+pitch);
    }

    // 鍔熻兘锛氭棤鏈夋晥鐩爣鏃跺皢鐐鏈濆悜骞虫粦鍥炲綊鍒伴粯璁よ搴︼紙defaultxrot/defaultyrot锛夈€?
    public void returnToDefaultRotation() {
        LogUtils.getLogger().warn("returning to:x:"+this.defaultspinx+"y:"+this.defaultspiny);
        this.targetxrot = this.defaultspinx;
        this.targetyrot = this.defaultspiny;
        this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot*Mth.PI/180);
        this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot*Mth.PI/180);
    }

    // 鍔熻兘锛氱敱 C2S 鏁版嵁鍖呭啓鍏ョ矑瀛愮偖 firepoint 鍧愭爣锛岄伩鍏嶆湇鍔＄鍐嶈绠?pivot 涓栫晫鍧愭爣銆?
    public void setFirePoint(Vector3d postofire) {
        if (postofire == null) {
            this.FirePoint = null;
            return;
        }
        this.FirePoint = new Vector3d(postofire);
    }

    public float xRot0 = 0;
    public float yRot0 = 0;
    public float prevxrot = 0;
    public float prevyrot = 0;
    public boolean xOK = false;
    public boolean yOK = false;
    public float targetxrot = 0;
    public float targetyrot = 0;
    public int defaultspinx = 0;
    public int defaultspiny = 0;


    // 浠ヤ笅涓烘柊閮ㄥ垎

    // 涓や釜鏂瑰悜鐨勬渶澶ц閫熷害 rad/s
    protected final float MAX_OMEGA_YAW = 1;
    protected final float MAX_OMEGA_PITCH = 1;

    protected float defaultYaw = 0;
    protected float defaultPitch = 0;

    protected float currentYaw = 0;
    protected float currentPitch = 0;


    public class servo{
        // d^2/dt^2 angle = Kp * (target - angle) - Kd * d/dt angle
        // Phi = Kp/(s^2 + Kd*s + Kp)
        // Omega_N = sqrt(Kp)
        // Epsilon = Kd / ( 2*sqrt(Kp) )

        public float angle = 0; // rad
        public float omega = 0;
        public float beta  = 0;
        private float Kp;
        private float Kd;
        private final float dt = 1f / 20;
        private static final float PI = (float) Math.PI;

        public boolean isStable = false;

        public void servoInitial(float Kp, float Kd){ // 鍒濆鍖栵紝鍏跺疄寤鸿鐢ㄤ笅闈㈤偅涓?
            this.Kp = Kp;
            this.Kd = Kd;
        }

        public void servoAutoInitial(int stableTick){ // 杈撳叆绋冲畾鏃堕棿锛坱icks锛夊氨琛岋紝娉ㄦ剰涓嶈杩囦綆锛佸缓璁嚦灏戜负2ticks
            // 濡傛灉浣犺緭鍏?tick 鍙兘浼氱湅瑙佸ぇ椋庤溅
            // 澶ч杞︼紒锛侊紒
            float second = stableTick * dt;
            this.Kp = 32f / (second * second);
            this.Kd = 8f / second;
        }

        private static float angleNormalize(float angle) {
            angle %= 2 * PI;
            if (angle > PI) angle -= 2 * PI;
            else if (angle < PI) angle += 2 * PI;
            return angle;
        }

        public boolean updateServo(float target){ // 杩斿洖"鏄惁璺熼殢绋冲畾"
            // 鎺у埗绯荤粺
            float error = angleNormalize( target - this.angle );
            this.beta = Kp * error - Kd* this.omega;

            // 绯荤粺鍔ㄥ姏瀛︾姸鎬?
            this.omega += this.beta * dt;
            this.angle += this.omega * dt;

            this.angle = angleNormalize(this.angle);
            this.isStable = (error <=0.034);; // 2搴?

            return this.isStable;
        }
    }
    
    // 鐩爣瀹氫綅鏂规硶鍜屽畠鐨勪笁涓皝瑁?
    // 浣犲簲璇ヤ娇鐢ㄥ皝瑁?
    private double[] doSightTransform(
            Vector3d dirInWorld,
            SubLevel subLevel
    ){
        Vector3d dirInShip=subLevel.logicalPose().transformNormalInverse(dirInWorld);
        Vector3d dirInModel=this.turretData.getCoordAxis().transform(dirInShip);

        // 璇″紓鐨勫潗鏍囧彉鎹?鏍规嵁妯″瀷鏉ョ殑
        double yaw = Math.atan2(
                dirInModel.x,
                dirInModel.z
        );
        double pitch=Math.atan2(
                Math.sqrt(dirInModel.x * dirInModel.x + dirInModel.z * dirInModel.z),
                dirInModel.y
        );

        return new double[]{yaw,pitch};
    }

    public double[] sightTransformByDir(
            Vector3d dirInWorld,
            SubLevel subLevel
    ){
        return doSightTransform(dirInWorld, subLevel);
    }

    public double[] sightTransformByVec3Pos(
            Vector3d TargetPosInWorld,
            SubLevel subLevel
    ){
        Vector3d TurretPos = new Vector3d(this.getBlockPos().getX(),this.getBlockPos().getY(),this.getBlockPos().getZ());
        TurretPos.add(this.getData().basePivotOffset);
        Vector3d dirInWorld = TargetPosInWorld.sub(TurretPos);

        return doSightTransform(dirInWorld, subLevel);
    }
    public double[] sightTransformByBlockPos(
            BlockPos TargetBlockPosInWorld,
            SubLevel subLevel
    ){
        Vector3d TurretPos = new Vector3d(this.getBlockPos().getX(),this.getBlockPos().getY(),this.getBlockPos().getZ());
        TurretPos.add(this.getData().basePivotOffset);
        Vector3d TargetPosInWorld = new Vector3d(TargetBlockPosInWorld.getX(),TargetBlockPosInWorld.getY(),TargetBlockPosInWorld.getZ());
        Vector3d dirInWorld = TargetPosInWorld.sub(TurretPos);

        return doSightTransform(dirInWorld, subLevel);
    }



}
