package com.kodu16.vsie.content.controlseat.server;


import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.functions.ScanNearByShips;
import com.kodu16.vsie.content.warpprojectile.WarpProjecTileEntity;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatInputS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatStatusS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.NearbyShipsS2CPacket;
import net.minecraft.server.level.ServerLevel;
import org.joml.Matrix4dc;
import org.joml.Quaterniondc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.ServerShipWorld;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
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
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.VsCoreApi;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;

import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;


public class ServerShipHandler {
    // 功能：当控制椅前向与 warp 目标夹角小于 1 度时，视为已完成自动对准并触发 warp projectile。
    private static final double WARP_ALIGNMENT_THRESHOLD_DEGREES = 1.0D;
    // 功能：warp projectile 固定以 1 格/tick 飞行，对应用户要求的跃迁特效速度。
    private static final double WARP_PROJECTILE_SPEED_PER_TICK = 1.0D;
    // 功能：在 warp projectile 消失后额外多等 1 秒，再调用 teleportship 执行正式跃迁。
    private static final int WARP_TELEPORT_EXTRA_DELAY_TICKS = 20;
    //原先用于加力，现在改成综合的船只信息和行为处理
    //船只的四元数等数据也会被S2C传回用于视角控制之类的
    //说句实话我真想让你按alt直接固定在当前视角得了，但是考虑到我要做HUD我还是选择现在立刻马上就搞S2C
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
    private volatile Vector3d worldXDirection = new Vector3d();
    private volatile Vector3d worldYDirection = new Vector3d();
    private volatile Vector3d worldZDirection = new Vector3d();
    //这byd很可能就是死活不发包的原因
    public void getandsendshipdata(PhysShipImpl ship) {
        ShipTransform transform = ship.getTransform();
        Vector3d ForwardDirection = new Vector3d();
        transform.getShipToWorld().transformDirection(data.getDirectionForward(), ForwardDirection);
        Vector3d UpDirection = new Vector3d();
        transform.getShipToWorld().transformDirection(data.getDirectionUp(), UpDirection);
        BlockPos pos = convertToBlockPos(ship.getCenterOfMass());
        Level level = data.level;
        long now = System.currentTimeMillis();

        if (data.getPlayer() != null) {
            if (now - lastSendMs > 50) {//快包
                lastSendMs = now;
                QueryableShipData<Ship> qsd = VSGameUtilsKt.getAllShips(level);
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
                        data.getThrottle());
                ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) data.getPlayer()), packet);

                //扫描全部船只包（扫描敌人包只跑在服务器不用发送）
                NearbyShipsS2CPacket packetship = new NearbyShipsS2CPacket(data.shipsData);
                ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) data.getPlayer()), packetship);
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
                ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) data.getPlayer()),packetstatus);
            }

            if(now - lastSendInputMs > 250) {//按键包（慢包out）
                // 功能：独立输入包发送节流时间，避免与状态包共用计时器导致输入包条件永远不成立。
                lastSendInputMs = now;
                ControlSeatInputS2CPacket packet = new ControlSeatInputS2CPacket(pos, data.channelencode);
                ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) data.getPlayer()), packet);
            }
        }
    }

    public void applyForceAndTorque(PhysShipImpl ship) {
        // 功能：每 tick 先检查是否到了延迟跃迁触发时间，确保弹体寿命结束后能自动执行 teleportship。
        processPendingWarpTeleport();

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

        double mass = ship.getMass();
        final ShipTransform transform = ship.getTransform();

        Vector3d invomega = ship.getOmega().negate(new Vector3d()).mul(10);
        Vector3d invtorque = data.isWarpPreparing ? new Vector3d(0,0,0) : ship.getMomentOfInertia().transform(invomega);
        Vector3dc invforce = data.isWarpPreparing ? new Vector3d(0,0,0) : ship.getVelocity().negate(new Vector3d()).mul(mass);

        Vector3d finaltorque = new Vector3d();
        Vector3d finalforce  = new Vector3d();

        if (data.isflightassiston) {
            finaltorque.add(invtorque);
            finalforce.add(invforce);
        }
        if (data.isantigravityon) {
            finalforce.add(0, mass * 10, 0);
        }

        if(controlling) {
            Vector3dc force = data.getForce();
            Vector3d torque = data.getTorque();

            transform.getShipToWorld().transformDirection(data.getDirectionForward(), worldXDirection);
            worldXDirection.normalize();
            transform.getShipToWorld().transformDirection(data.getDirectionUp(), worldYDirection);
            worldYDirection.normalize();
            transform.getShipToWorld().transformDirection(data.getDirectionRight(), worldZDirection);
            worldZDirection.normalize();

            Vector3d steeringTorque = data.isWarpPreparing ? calculateWarpPreparationTorque(ship) : new Vector3d(torque);
            double torquescale = data.thruster_strength*5 / (Math.sqrt(ship.getMass()));
            Vector3d controltorque = new Vector3d(steeringTorque.x*torquescale, steeringTorque.y*torquescale, steeringTorque.z*torquescale);
            if(controltorque.length()<0.1) {
                controltorque.mul(0);
            }

            if (data.isWarpPreparing) {
                // 功能：一旦自动对准达到阈值，立即在船体位置生成 warp projectile，并退出准备状态防止重复生成。
                tryLaunchWarpProjectile(ship);
            }

            Vector3d Invarianttorque = calculateWorldTorque(controltorque, worldXDirection, worldYDirection, worldZDirection);
            double forcescale = -1000 * data.getThrottle() * (data.thruster_strength / (ship.getMass()));
            Vector3d Invariantforce = new Vector3d(worldXDirection.x * forcescale, worldXDirection.y * forcescale, worldXDirection.z * forcescale);

            // 计算反向阻尼力矩，与角速度成比例
            if (Double.isNaN(torque.x()) || Double.isNaN(torque.y()) || Double.isNaN(torque.z())) {
                return;
            }
            //data.getPlayer().displayClientMessage(Component.literal("torquex:" + torque.x() + "  torquey:" + torque.y() + "  worldx:" + worldXDirection + "  throttle:" + data.getThrottle()), true);
            finaltorque.add(Invarianttorque);
            finalforce.add(Invariantforce);
        }
        data.setFinaltorque(finaltorque);
        data.setFinalforce(finalforce);
        //到这才算施加真正的力
        ship.applyInvariantTorque(finaltorque);
        ship.applyInvariantForce(finalforce);


    }

    // 功能：warp 准备状态下根据控制椅前向与目标方向的夹角生成自动对准扭矩；结果被限制在手动鼠标控制的最大输入范围内。
    private Vector3d calculateWarpPreparationTorque(PhysShipImpl ship) {
        if (data.warpTargetName == null || data.warpTargetName.isEmpty() || data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return new Vector3d(0, 0, 0);
        }

        Vector3d targetDirection = getNormalizedWarpTargetDirection(ship);
        if (targetDirection == null) {
            return new Vector3d(0, 0, 0);
        }

        Vector3d currentForward = new Vector3d(worldXDirection).normalize();
        Vector3d rotationAxisWorld = currentForward.cross(targetDirection, new Vector3d());
        if (rotationAxisWorld.lengthSquared() < 1.0E-6) {
            return new Vector3d(0, 0, 0);
        }

        double alignment = Mth.clamp(currentForward.dot(targetDirection), -1.0D, 1.0D);
        double angleStrength = Mth.clamp((1.0D - alignment) * 2.0D, 0.0D, 1.0D);
        rotationAxisWorld.normalize(angleStrength);
        double factor = ship.getMass()/10;
        // 功能：只使用 yaw/pitch 两个轴进行自动对准，避免 warp 准备阶段给控制椅引入额外滚转。
        double localYawTorque = Mth.clamp(rotationAxisWorld.dot(worldYDirection), -factor, factor);
        double localPitchTorque = Mth.clamp(rotationAxisWorld.dot(worldZDirection), -factor, factor);
        return new Vector3d(0, localYawTorque, localPitchTorque);
    }

    // 功能：检查当前船首是否已对准 warp 目标；若夹角小于 1 度，则按船体最大包围盒尺寸生成 warp projectile。
    private void tryLaunchWarpProjectile(PhysShipImpl ship) {
        Level level = data.level;
        if (level == null || level.isClientSide()) {
            return;
        }
        if (data.hasPendingWarpTeleport) {
            return;
        }
        if (data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return;
        }

        Vector3d launchDirection = getNormalizedWarpTargetDirection(ship);
        if (launchDirection == null) {
            return;
        }

        Vector3d currentForward = new Vector3d(worldXDirection);
        if (currentForward.lengthSquared() < 1.0E-6D) {
            return;
        }
        currentForward.normalize();

        double alignment = Mth.clamp(currentForward.dot(launchDirection), -1.0D, 1.0D);
        double angleDegrees = Math.toDegrees(Math.acos(alignment));
        if (angleDegrees >= WARP_ALIGNMENT_THRESHOLD_DEGREES) {
            return;
        }

        AABBdc shipAabb = ship.getWorldAABB();
        double sizeX = shipAabb.maxX() - shipAabb.minX();
        double sizeY = shipAabb.maxY() - shipAabb.minY();
        double sizeZ = shipAabb.maxZ() - shipAabb.minZ();
        double k = Math.max(sizeX, Math.max(sizeY, sizeZ));
        if (k <= 0.0D) {
            return;
        }

        Vector3dc shipPos = ship.getTransform().getPositionInWorld();
        WarpProjecTileEntity warpProjectile = new WarpProjecTileEntity(vsieEntities.WARP_PROJECTILE.get(), level);
        // 功能：在船只 world pos 处生成特效弹体，并让其以 1 格/tick 朝目标飞行 k tick。
        warpProjectile.setPos(shipPos.x(), shipPos.y(), shipPos.z());
        warpProjectile.configureLaunch(
                new net.minecraft.world.phys.Vec3(launchDirection.x, launchDirection.y, launchDirection.z),
                WARP_PROJECTILE_SPEED_PER_TICK,
                k
        );
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
    private void processPendingWarpTeleport() {
        Level level = data.level;
        if (level == null || level.isClientSide() || !data.hasPendingWarpTeleport) {
            return;
        }
        if (level.getGameTime() < data.pendingWarpTeleportGameTime) {
            return;
        }
        Vector3d destination = new Vector3d(data.pendingWarpTeleportPos);
        data.clearPendingWarpTeleport();
        teleportship(data, destination);
    }

    // 功能：复用控制椅到目标点的归一化方向计算，供自动对准与 warp projectile 发射共用同一方向基准。
    private Vector3d getNormalizedWarpTargetDirection(PhysShipImpl ship) {
        Vector3d seatWorldPos = convertSeatToWorldPosition(ship);
        Vector3d targetDirection = new Vector3d(
                data.warpTargetPos.getX() + 0.5 - seatWorldPos.x,
                data.warpTargetPos.getY() + 0.5 - seatWorldPos.y,
                data.warpTargetPos.getZ() + 0.5 - seatWorldPos.z
        );
        if (targetDirection.lengthSquared() < 1.0E-6D) {
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
        controlSeat.sendData();
    }

    // 功能：把控制椅方块坐标转换为世界空间中心点，用于计算“控制椅前向 -> warp 目标”的真实方向向量。
    private Vector3d convertSeatToWorldPosition(PhysShipImpl ship) {
        BlockPos controlSeatPos = data.controlSeatPos;
        if (controlSeatPos == null) {
            controlSeatPos = BlockPos.ZERO;
        }
        if (VSGameUtilsKt.isBlockInShipyard(data.level, controlSeatPos)) {
            Vector3d worldCenter = new Vector3d();
            ship.getTransform().getShipToWorld().transformPosition(
                    new Vector3d(controlSeatPos.getX() + 0.5, controlSeatPos.getY() + 0.5, controlSeatPos.getZ() + 0.5),
                    worldCenter
            );
            return worldCenter;
        }
        return new Vector3d(controlSeatPos.getX() + 0.5, controlSeatPos.getY() + 0.5, controlSeatPos.getZ() + 0.5);
    }

    public static BlockPos convertToBlockPos(Vector3dc vector) {
        // 获取 Vector3dc 的坐标
        int x = (int) Math.floor(vector.x());
        int y = (int) Math.floor(vector.y());
        int z = (int) Math.floor(vector.z());

        // 创建并返回 BlockPos 对象
        //我讨厌vector3dc
        return new BlockPos(x, y, z);
    }

    public static Vector3d calculateWorldTorque(Vector3d localTorque, Vector3d worldDirectionX, Vector3d worldDirectionY, Vector3d worldDirectionZ) {
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
        return new Vector3d(a,b,c);
        // 返回世界坐标系下d(a, b, c);
    }

    public static void teleportship(ControlSeatServerData data, Vector3d destpos) {
        ServerShip ship = data.serverShip;
        Level level = data.level;
        Quaterniondc rot = ship.getTransform().getShipToWorldRotation();
        Vector3dc vel = ship.getVelocity();
        Vector3dc omega = ship.getAngularVelocity();
        Vector3dc centerofmass = ship.getTransform().getPositionInModel();
        // 功能：只有在控制椅明确记录了目标维度时才透传给 VS；否则留空交给 teleportship 自动决定维度。
        String dimension = data.warpTargetDimension == null || data.warpTargetDimension.isBlank() ? null : data.warpTargetDimension;
        ServerShipWorld ssw = (ServerShipWorld) VSGameUtilsKt.getShipObjectWorld(level);
        double scale = ship.getTransform().getShipToWorldScaling().x();
        var teleportData = new ShipTeleportDataImpl(destpos, rot, vel, omega, dimension, scale,centerofmass);
        ValkyrienSkiesMod.getVsCore().teleportShip(ssw, ship,teleportData);
    }

}
