package com.kodu16.vsie.content.controlseat.server;


import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
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
    // 功能：当控制椅前向与 warp 目标夹角小于 1 度时，视为已完成自动对准并触发 warp projectile。
    private static final double WARP_ALIGNMENT_THRESHOLD_DEGREES = 1.0D;
    // 功能：warp projectile 固定以 1 格/tick 飞行，对应用户要求的跃迁特效速度。
    private static final double WARP_PROJECTILE_SPEED_PER_TICK = 1.0D;
    // 功能：在 warp projectile 消失后额外多等 1 秒，再调用 teleportship 执行正式跃迁。
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
    //这byd很可能就是死活不发包的原因
    public void getandsendshipdata(ServerSubLevel subLevel,BlockPos pos) {
        Vec3 ForwardDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionForward()));
        Vec3 UpDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionUp()));
        Vec3 RightDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionRight()));
        Level level = data.level;
        long now = System.currentTimeMillis();
        if (data.getPlayer() != null) {
            if (now - lastSendMs > 50) {//快包
                lastSendMs = now;
                /*QueryableShipData<Su> qsd = VSGameUtilsKt.getAllShips(level);
                data.shipsData = ScanNearByShips.scanships(qsd,pos,level);
                data.enemyshipsData = ScanNearByShips.scanenemyships(qsd,pos,level, data.enemy, data.ally);
                //信息包
                String slug = "";
                if(!data.enemyshipsData.isEmpty()) {
                    Ship targetenemyship = data.enemyshipsData.get(data.lockedenemyindex);
                    slug = targetenemyship.getSlug();
                }
                ControlSeatS2CPacket packet = new ControlSeatS2CPacket(pos,
                        ForwardDirection, UpDirection,
                        data.enemy,data.ally,slug,
                        data.getThrottle(),
                        // 功能：快包携带服务端当前视角锁状态，确保重进世界后的客户端能立即恢复锁定与输入行为。
                        data.isviewlocked);
                ModNetworking.sendToPlayer(packet, (ServerPlayer) data.getPlayer());

                //扫描全部船只包（扫描敌人包只跑在服务器不用发送）
                NearbyShipsS2CPacket packetship = new NearbyShipsS2CPacket(data.shipsData);
                ModNetworking.sendToPlayer(packetship, (ServerPlayer) data.getPlayer());*/
            }

            if(now - lastSendStatusMs > 250) {//状态包（慢包out）
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

            if(now - lastSendInputMs > 250) {//按键包（慢包out）
                // 功能：独立输入包发送节流时间，避免与状态包共用计时器导致输入包条件永远不成立。
                lastSendInputMs = now;
                ControlSeatInputS2CPacket packet = new ControlSeatInputS2CPacket(pos, data.channelencode);
                ModNetworking.sendToPlayer(packet, (ServerPlayer) data.getPlayer());
            }
        }
    }

    public void applyForceAndTorque(ServerSubLevel subLevel,BlockPos pos) {
        // 功能：每 tick 先检查是否到了延迟跃迁触发时间，确保弹体寿命结束后能自动执行 teleportship。
        processPendingWarpTeleport(subLevel);

        Player player = data.getPlayer();
        boolean controlling = true;
        // 1. 玩家为空或已经死了，直接啥都不干
        if (player == null || !player.isAlive() || player.isRemoved()) {
            data.reset();
            controlling = false;
        }
        // 2. 玩家当前乘坐的实体为空，或者不是 VS2 的船挂载实体
        Entity vehicle = null;
        if (player != null) {
            vehicle = player.getVehicle();
        }
        if (vehicle == null || vehicle.getType() != ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE) {
            data.reset();
            controlling = false;
        }

        MassData massData = subLevel.getMassTracker();
        double mass = massData.getMass();

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
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
                // 功能：一旦自动对准达到阈值，立即在船体位置生成 warp projectile，并退出准备状态防止重复生成。
                LogUtils.getLogger().warn("preparing warp...");
                tryLaunchWarpProjectile(subLevel);
            }

            Vec3 Invarianttorque = calculateWorldTorque(controltorque, worldXDirection, worldYDirection, worldZDirection);
            double forcescale = data.getThrottle() * (data.thruster_strength / mass);
            Vec3 Invariantforce = new Vec3(worldXDirection.x * forcescale, worldXDirection.y * forcescale, worldXDirection.z * forcescale);

            // 计算反向阻尼力矩，与角速度成比例
            if (Double.isNaN(torque.x()) || Double.isNaN(torque.y()) || Double.isNaN(torque.z())) {
                return;
            }
            finaltorque.add(Vec.toVector3d(Invarianttorque));
            finalforce.add(Vec.toVector3d(Invariantforce));
        }
        data.setFinaltorque(finaltorque);
        data.setFinalforce(finalforce);
        //到这才算施加真正的力
        ServerShipUtils.applyWorldForceAndTorqueAtCenterOfMass(subLevel,finalforce,finaltorque);
    }

    // 功能：warp 准备状态下根据控制椅前向与目标方向的夹角生成自动对准扭矩；结果被限制在手动鼠标控制的最大输入范围内。
    private Vec3 calculateWarpPreparationTorque(ServerSubLevel subLevel,BlockPos pos) {
        if (data.warpTargetName == null || data.warpTargetName.isEmpty() || data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return new Vec3(0, 0, 0);
        }

        Vec3 targetDirection = getNormalizedWarpTargetDirection(pos);
        if (targetDirection == null) {
            return new Vec3(0, 0, 0);
        }

        Vec3 currentForward = worldXDirection.normalize();
        // 功能：自动对准需要生成“从当前朝向转到目标朝向”的右手旋转轴；使用 target x current 会把扭矩方向反过来，导致控制椅围绕目标反方向摆动。
        Vec3 rotationAxisWorld = targetDirection.cross(currentForward);
        if (rotationAxisWorld.lengthSqr() < 1.0E-6) {
            return new Vec3(0, 0, 0);
        }

        double alignment = Mth.clamp(currentForward.dot(targetDirection), -1.0D, 1.0D);
        double angleStrength = Mth.clamp((1.0D - alignment) * 2.0D, 0.0D, 1.0D);
        rotationAxisWorld.normalize();
        rotationAxisWorld.scale(angleStrength);
        double factor = subLevel.getMassTracker().getMass();
        // 功能：只使用 yaw/pitch 两个轴进行自动对准，避免 warp 准备阶段给控制椅引入额外滚转。
        double localYawTorque = Mth.clamp(rotationAxisWorld.dot(worldYDirection)*20, -factor, factor);
        double localPitchTorque = Mth.clamp(rotationAxisWorld.dot(worldZDirection)*20, -factor, factor);
        return new Vec3(0, localYawTorque, localPitchTorque);
    }

    // 功能：检查当前船首是否已对准 warp 目标；若夹角小于 1 度，则按船体最大包围盒尺寸生成 warp projectile。
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
        LogUtils.getLogger().warn(String.valueOf(Component.literal("准备跃迁")));
        MassData massData = subLevel.getMassTracker();
        double mass = massData.getMass();
        double k = Math.pow(mass, (double) 1 /3);
        Level level = data.level;
        Vec3 shipPos = ServerShipUtils.getStructureCenterWorld(subLevel);
        WarpProjecTileEntity warpProjectile = new WarpProjecTileEntity(vsieEntities.WARP_PROJECTILE.get(), level);
        // 功能：在船只 world pos 处生成特效弹体，并让其以 1 格/tick 朝目标飞行 k tick。
        warpProjectile.setPos(shipPos.x(), shipPos.y(), shipPos.z());
        warpProjectile.configureLaunch(
                new net.minecraft.world.phys.Vec3(launchDirection.x, launchDirection.y, launchDirection.z),
                k
        );
        LogUtils.getLogger().warn("adding projectile at:"+launchDirection+"pos:"+shipPos+"life:"+k);
        level.addFreshEntity(warpProjectile);

        // 功能：按“弹体寿命 k tick + 1 秒”的规则安排后续传送，目标点取玩家所选坐标中心。
        long executeGameTime = level.getGameTime() + (long) Math.ceil(k) + WARP_TELEPORT_EXTRA_DELAY_TICKS;
        data.schedulePendingWarpTeleport(new Vector3d(
                data.warpTargetPos.getX() + 0.5D,
                data.warpTargetPos.getY() + 0.5D,
                data.warpTargetPos.getZ() + 0.5D
        ), executeGameTime);
        data.clearWarpPreparation();
        syncWarpPreparationState();
    }

    // 功能：在服务器 tick 到达预定时间时调用 teleportship，把船只传送到之前锁定的跃迁目标。
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

    // 功能：复用控制椅到目标点的归一化方向计算，供自动对准与 warp projectile 发射共用同一方向基准。
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

    // 功能：warp 准备状态结束后立刻把控制椅方块实体同步给客户端，避免客户端仍显示旧的准备状态。
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
        // 旋转矩阵是由控制椅X, Y, Z轴在世界坐标系下的单位向量构成的
        // 构建旋转矩阵
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

        // 根据旋转矩阵和局部坐标系的扭矩来计算世界坐标系下的扭矩
        double a = rotationMatrix[0][0] * localTorque.x + rotationMatrix[0][1] * localTorque.y + rotationMatrix[0][2] * localTorque.z;
        double b = rotationMatrix[1][0] * localTorque.x + rotationMatrix[1][1] * localTorque.y + rotationMatrix[1][2] * localTorque.z;
        double c = rotationMatrix[2][0] * localTorque.x + rotationMatrix[2][1] * localTorque.y + rotationMatrix[2][2] * localTorque.z;
        return new Vec3(a,b,c);
        // 返回世界坐标系下d(a, b, c);
    }

}
