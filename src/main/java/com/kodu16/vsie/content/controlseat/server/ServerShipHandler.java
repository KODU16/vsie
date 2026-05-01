package com.kodu16.vsie.content.controlseat.server;


import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.warpprojectile.WarpProjecTileEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatInputS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatStatusS2CPacket;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3dc;
import org.joml.Quaterniondc;
import com.kodu16.vsie.registries.vsieEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.kodu16.vsie.registries.ModNetworking;

import org.slf4j.Logger;

import javax.annotation.Nullable;


public class ServerShipHandler {
    // 鍔熻兘锛氬綋鎺у埗妞呭墠鍚戜笌 warp 鐩爣澶硅灏忎簬 1 搴︽椂锛岃涓哄凡瀹屾垚鑷姩瀵瑰噯骞惰Е鍙?warp projectile銆?
    private static final double WARP_ALIGNMENT_THRESHOLD_DEGREES = 1.0D;
    // 鍔熻兘锛歸arp projectile 鍥哄畾浠?1 鏍?tick 椋炶锛屽搴旂敤鎴疯姹傜殑璺冭縼鐗规晥閫熷害銆?
    private static final double WARP_PROJECTILE_SPEED_PER_TICK = 1.0D;
    // 鍔熻兘锛氬湪 warp projectile 娑堝け鍚庨澶栧绛?1 绉掞紝鍐嶈皟鐢?teleportship 鎵ц姝ｅ紡璺冭縼銆?
    private static final int WARP_TELEPORT_EXTRA_DELAY_TICKS = 100;
    private ControlSeatServerData data;
    public static final Logger LOGGER = LogUtils.getLogger();

    public ServerShipHandler(ControlSeatServerData data){
        this.data = data;
    }
    private long lastSendMs = 0;
    private long lastSendStatusMs = 0;
    private long lastSendInputMs = 0;
    int lastSentEncode = 0;
    int current=0;
    private volatile Vec3 worldXDirection;
    private volatile Vec3 worldYDirection;
    private volatile Vec3 worldZDirection;
    //杩檅yd寰堝彲鑳藉氨鏄娲讳笉鍙戝寘鐨勫師鍥?
    public void getandsendshipdata(ServerSubLevel subLevel,BlockPos pos) {
        if (data.getDirectionForward() == null || data.getDirectionUp() == null || data.getDirectionRight() == null) {
            return;
        }

        Vec3 ForwardDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionForward()));
        Vec3 UpDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionUp()));
        Vec3 RightDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionRight()));
        Level level = data.level;
        long now = System.currentTimeMillis();
        if (data.getPlayer() != null) {
            if (now - lastSendMs > 50) {//蹇寘
                lastSendMs = now;
            }

            if(now - lastSendStatusMs > 250) {//鐘舵€佸寘锛堟參鍖卭ut锛?
                lastSendStatusMs = now;
                ControlSeatStatusS2CPacket packetstatus = new ControlSeatStatusS2CPacket(pos,
                        data.avalibleenergy,data.totalenergystorage,
                        data.avaliblefuel,data.totalfuelstorage,
                        data.isshieldon, (int) data.avalibleshield, (int) data.totalshield,
                        data.isflightassiston, data.isantigravityon,
                        data.activeWeaponHudInfos);
                //LogUtils.getLogger().warn("shieldtotal:"+data.totalshield+"avalible:"+data.avalibleshield);
                ModNetworking.sendToPlayer(packetstatus, (ServerPlayer) data.getPlayer());
            }

            if(now - lastSendInputMs > 250) {//鎸夐敭鍖咃紙鎱㈠寘out锛?
                // 鍔熻兘锛氱嫭绔嬭緭鍏ュ寘鍙戦€佽妭娴佹椂闂达紝閬垮厤涓庣姸鎬佸寘鍏辩敤璁℃椂鍣ㄥ鑷磋緭鍏ュ寘鏉′欢姘歌繙涓嶆垚绔嬨€?
                lastSendInputMs = now;
                ControlSeatInputS2CPacket packet = new ControlSeatInputS2CPacket(pos, data.channelencode);
                ModNetworking.sendToPlayer(packet, (ServerPlayer) data.getPlayer());
            }
        }
    }

    public void applyForceAndTorque(ServerSubLevel subLevel,BlockPos pos) {
        // 鍔熻兘锛氭瘡 tick 鍏堟鏌ユ槸鍚﹀埌浜嗗欢杩熻穬杩佽Е鍙戞椂闂达紝纭繚寮逛綋瀵垮懡缁撴潫鍚庤兘鑷姩鎵ц teleportship銆?
        processPendingWarpTeleport(subLevel);
        if (data.getDirectionForward() == null || data.getDirectionUp() == null || data.getDirectionRight() == null) {
            return;
        }

        Player player = data.getPlayer();
        boolean controlling = true;
        // 1. 鐜╁涓虹┖鎴栧凡缁忔浜嗭紝鐩存帴鍟ラ兘涓嶅共
        if (player == null || !player.isAlive() || player.isRemoved()) {
            data.reset();
            controlling = false;
        }
        // 2. 鐜╁褰撳墠涔樺潗鐨勫疄浣撲负绌猴紝鎴栬€呬笉鏄?VS2 鐨勮埞鎸傝浇瀹炰綋
        Entity vehicle = null;
        if (player != null) {
            vehicle = player.getVehicle();
        }
        if (!(vehicle instanceof ControlSeatMountEntity)) {
            data.reset();
            controlling = false;
        }

        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid()) {
            return;
        }
        double mass = massData.getMass();

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || !handle.isValid()) {
            return;
        }
        Vector3d omega = handle.getAngularVelocity(new Vector3d());
        Matrix3dc momentOfInertia = massData.getInertiaTensor();
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());

        Vector3d invomega = omega.negate(new Vector3d()).mul(10);
        Vector3d invtorque = momentOfInertia.transform(invomega);
        Vector3dc invforce = velocity.negate(new Vector3d()).mul(mass);

        Vector3d finaltorque = new Vector3d(0,0,0);
        Vector3d finalforce  = new Vector3d(0,0,0);

        if (data.isflightassiston) {
            finaltorque.add(invtorque);
            finalforce.add(invforce);
        }
        if (data.isantigravityon) {
            finalforce.add(0, mass * 10, 0);
        }

        if(controlling) {
            Vec3 force = data.getForce();
            Vec3 torque = data.getTorque();
            worldXDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionForward())).normalize();
            worldYDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionUp())).normalize();
            worldZDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionRight())).normalize();

            Vec3 steeringTorque = data.isWarpPreparing ? calculateWarpPreparationTorque(subLevel,pos) : torque;
            double torquescale = data.thruster_strength*5 / (Math.sqrt(mass));
            Vector3d controltorque = new Vector3d(steeringTorque.x*torquescale, steeringTorque.y*torquescale, steeringTorque.z*torquescale);
            if(controltorque.length()<0.1) {
                controltorque.mul(0);
            }

            if (data.isWarpPreparing) {
                // 鍔熻兘锛氫竴鏃﹁嚜鍔ㄥ鍑嗚揪鍒伴槇鍊硷紝绔嬪嵆鍦ㄨ埞浣撲綅缃敓鎴?warp projectile锛屽苟閫€鍑哄噯澶囩姸鎬侀槻姝㈤噸澶嶇敓鎴愩€?
                LogUtils.getLogger().warn("preparing warp...");
                tryLaunchWarpProjectile(subLevel);
            }

            Vec3 Invarianttorque = calculateWorldTorque(controltorque, worldXDirection, worldYDirection, worldZDirection);
            double forcescale = data.getThrottle() * (data.thruster_strength / mass);
            Vec3 Invariantforce = new Vec3(worldXDirection.x * forcescale, worldXDirection.y * forcescale, worldXDirection.z * forcescale);

            // 璁＄畻鍙嶅悜闃诲凹鍔涚煩锛屼笌瑙掗€熷害鎴愭瘮渚?
            if (Double.isNaN(torque.x()) || Double.isNaN(torque.y()) || Double.isNaN(torque.z())) {
                return;
            }
            finaltorque.add(Vec.toVector3d(Invarianttorque));
            finalforce.add(Vec.toVector3d(Invariantforce));
        }
        data.setFinaltorque(finaltorque);
        data.setFinalforce(finalforce);
        //鍒拌繖鎵嶇畻鏂藉姞鐪熸鐨勫姏
        ServerShipUtils.applyWorldForceAndTorqueAtCenterOfMass(subLevel,finalforce,finaltorque);
    }

    // 鍔熻兘锛歸arp 鍑嗗鐘舵€佷笅鏍规嵁鎺у埗妞呭墠鍚戜笌鐩爣鏂瑰悜鐨勫す瑙掔敓鎴愯嚜鍔ㄥ鍑嗘壄鐭╋紱缁撴灉琚檺鍒跺湪鎵嬪姩榧犳爣鎺у埗鐨勬渶澶ц緭鍏ヨ寖鍥村唴銆?
    private Vec3 calculateWarpPreparationTorque(ServerSubLevel subLevel,BlockPos pos) {
        if (data.warpTargetName == null || data.warpTargetName.isEmpty() || data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return new Vec3(0, 0, 0);
        }

        Vec3 targetDirection = getNormalizedWarpTargetDirection(pos);
        if (targetDirection == null) {
            return new Vec3(0, 0, 0);
        }

        Vec3 currentForward = worldXDirection.normalize();
        // 鍔熻兘锛氳嚜鍔ㄥ鍑嗛渶瑕佺敓鎴愨€滀粠褰撳墠鏈濆悜杞埌鐩爣鏈濆悜鈥濈殑鍙虫墜鏃嬭浆杞达紱浣跨敤 target x current 浼氭妸鎵煩鏂瑰悜鍙嶈繃鏉ワ紝瀵艰嚧鎺у埗妞呭洿缁曠洰鏍囧弽鏂瑰悜鎽嗗姩銆?
        Vec3 rotationAxisWorld = targetDirection.cross(currentForward);
        if (rotationAxisWorld.lengthSqr() < 1.0E-6) {
            return new Vec3(0, 0, 0);
        }

        double alignment = Mth.clamp(currentForward.dot(targetDirection), -1.0D, 1.0D);
        double angleStrength = Mth.clamp((1.0D - alignment) * 2.0D, 0.0D, 1.0D);
        rotationAxisWorld.normalize();
        rotationAxisWorld.scale(angleStrength);
        double factor = subLevel.getMassTracker().getMass();
        // 鍔熻兘锛氬彧浣跨敤 yaw/pitch 涓や釜杞磋繘琛岃嚜鍔ㄥ鍑嗭紝閬垮厤 warp 鍑嗗闃舵缁欐帶鍒舵寮曞叆棰濆婊氳浆銆?
        double localYawTorque = Mth.clamp(rotationAxisWorld.dot(worldYDirection)*20, -factor, factor);
        double localPitchTorque = Mth.clamp(rotationAxisWorld.dot(worldZDirection)*20, -factor, factor);
        return new Vec3(0, localYawTorque, localPitchTorque);
    }

    // 鍔熻兘锛氭鏌ュ綋鍓嶈埞棣栨槸鍚﹀凡瀵瑰噯 warp 鐩爣锛涜嫢澶硅灏忎簬 1 搴︼紝鍒欐寜鑸逛綋鏈€澶у寘鍥寸洅灏哄鐢熸垚 warp projectile銆?
    private void tryLaunchWarpProjectile(ServerSubLevel subLevel) {
        if (data.hasPendingWarpTeleport) {
            return;
        }
        if (data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return;
        }

        Vec3 launchDirection = getNormalizedWarpTargetDirection(data.controlSeatPos);
        if (launchDirection == null) {
            return;
        }

        double alignment = Mth.clamp(worldXDirection.dot(launchDirection), -1.0D, 1.0D);
        double angleDegrees = Math.toDegrees(Math.acos(alignment));
        if (Math.abs(angleDegrees-180) >= WARP_ALIGNMENT_THRESHOLD_DEGREES) {
            return;
        }
        LogUtils.getLogger().warn(String.valueOf(Component.literal("鍑嗗璺冭縼")));
        MassData massData = subLevel.getMassTracker();
        double mass = massData.getMass();
        double k = Math.pow(mass, (double) 1 /3);
        Level level = data.level;
        Vec3 shipPos = ServerShipUtils.getStructureCenterWorld(subLevel);
        WarpProjecTileEntity warpProjectile = new WarpProjecTileEntity(vsieEntities.WARP_PROJECTILE.get(), level);
        // 鍔熻兘锛氬湪鑸瑰彧 world pos 澶勭敓鎴愮壒鏁堝脊浣擄紝骞惰鍏朵互 1 鏍?tick 鏈濈洰鏍囬琛?k tick銆?
        warpProjectile.setPos(shipPos.x(), shipPos.y(), shipPos.z());
        warpProjectile.configureLaunch(
                new net.minecraft.world.phys.Vec3(launchDirection.x, launchDirection.y, launchDirection.z),
                k
        );
        LogUtils.getLogger().warn("adding projectile at:"+launchDirection+"pos:"+shipPos+"life:"+k);
        level.addFreshEntity(warpProjectile);

        // 鍔熻兘锛氭寜鈥滃脊浣撳鍛?k tick + 1 绉掆€濈殑瑙勫垯瀹夋帓鍚庣画浼犻€侊紝鐩爣鐐瑰彇鐜╁鎵€閫夊潗鏍囦腑蹇冦€?
        long executeGameTime = level.getGameTime() + (long) Math.ceil(k) + WARP_TELEPORT_EXTRA_DELAY_TICKS;
        data.schedulePendingWarpTeleport(new Vector3d(
                data.warpTargetPos.getX() + 0.5D,
                data.warpTargetPos.getY() + 0.5D,
                data.warpTargetPos.getZ() + 0.5D
        ), executeGameTime);
        data.clearWarpPreparation();
        syncWarpPreparationState();
    }

    // 鍔熻兘锛氬湪鏈嶅姟鍣?tick 鍒拌揪棰勫畾鏃堕棿鏃惰皟鐢?teleportship锛屾妸鑸瑰彧浼犻€佸埌涔嬪墠閿佸畾鐨勮穬杩佺洰鏍囥€?
    private void processPendingWarpTeleport(ServerSubLevel subLevel) {
        Level level = data.level;
        if (level == null || level.isClientSide() || !data.hasPendingWarpTeleport) {
            return;
        }
        if (level.getGameTime() < data.pendingWarpTeleportGameTime) {
            return;
        }
        data.clearPendingWarpTeleport();
        ServerShipUtils.teleportKeepOrientation(subLevel,data.pendingWarpTeleportPos);
    }

    // 鍔熻兘锛氬鐢ㄦ帶鍒舵鍒扮洰鏍囩偣鐨勫綊涓€鍖栨柟鍚戣绠楋紝渚涜嚜鍔ㄥ鍑嗕笌 warp projectile 鍙戝皠鍏辩敤鍚屼竴鏂瑰悜鍩哄噯銆?
    private Vec3 getNormalizedWarpTargetDirection(BlockPos pos) {
        SubLevel sublevel = ServerShipUtils.getSubLevelAtBlockPos(data.level,pos);
        Vec3 seatWorldPos = sublevel.logicalPose().transformPosition(Vec3.atLowerCornerOf(pos));
        Vec3 targetDirection = new Vec3(
                data.warpTargetPos.getX() + 0.5 - seatWorldPos.x,
                data.warpTargetPos.getY() + 0.5 - seatWorldPos.y,
                data.warpTargetPos.getZ() + 0.5 - seatWorldPos.z
        );
        if (targetDirection.lengthSqr() < 1.0E-6D) {
            return null;
        }
        return targetDirection.normalize();
    }

    // 鍔熻兘锛歸arp 鍑嗗鐘舵€佺粨鏉熷悗绔嬪埢鎶婃帶鍒舵鏂瑰潡瀹炰綋鍚屾缁欏鎴风锛岄伩鍏嶅鎴风浠嶆樉绀烘棫鐨勫噯澶囩姸鎬併€?
    private void syncWarpPreparationState() {
        if (data.level == null || data.controlSeatPos == null) {
            return;
        }
        if (!(data.level.getBlockEntity(data.controlSeatPos) instanceof ControlSeatBlockEntity controlSeat)) {
            return;
        }
        controlSeat.setChanged();
    }

    public static Vec3 calculateWorldTorque(Vector3d localTorque, Vec3 worldDirectionX, Vec3 worldDirectionY, Vec3 worldDirectionZ) {
        // 鏃嬭浆鐭╅樀鏄敱鎺у埗妞匵, Y, Z杞村湪涓栫晫鍧愭爣绯讳笅鐨勫崟浣嶅悜閲忔瀯鎴愮殑
        // 鏋勫缓鏃嬭浆鐭╅樀
        double[][] rotationMatrix = new double[3][3];
        rotationMatrix[0][0] = worldDirectionX.x;
        rotationMatrix[0][1] = worldDirectionY.x;
        rotationMatrix[0][2] = worldDirectionZ.x;

        rotationMatrix[1][0] = worldDirectionX.y;
        rotationMatrix[1][1] = worldDirectionY.y;
        rotationMatrix[1][2] = worldDirectionZ.y;

        rotationMatrix[2][0] = worldDirectionX.z;
        rotationMatrix[2][1] = worldDirectionY.z;
        rotationMatrix[2][2] = worldDirectionZ.z;

        // 鏍规嵁鏃嬭浆鐭╅樀鍜屽眬閮ㄥ潗鏍囩郴鐨勬壄鐭╂潵璁＄畻涓栫晫鍧愭爣绯讳笅鐨勬壄鐭?
        double a = rotationMatrix[0][0] * localTorque.x + rotationMatrix[0][1] * localTorque.y + rotationMatrix[0][2] * localTorque.z;
        double b = rotationMatrix[1][0] * localTorque.x + rotationMatrix[1][1] * localTorque.y + rotationMatrix[1][2] * localTorque.z;
        double c = rotationMatrix[2][0] * localTorque.x + rotationMatrix[2][1] * localTorque.y + rotationMatrix[2][2] * localTorque.z;
        return new Vec3(a,b,c);
        // 杩斿洖涓栫晫鍧愭爣绯讳笅d(a, b, c);
    }

}
