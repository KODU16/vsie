package com.kodu16.vsie.content.controlseat.server;


import com.kodu16.vsie.content.controlseat.functions.ScanNearByShips;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatInputS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatStatusS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.NearbyShipsS2CPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
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
        Vector3d invtorque = ship.getMomentOfInertia().transform(invomega);
        Vector3dc invforce = ship.getVelocity().negate(new Vector3d()).mul(mass);

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

            double torquescale = data.thruster_strength / (Math.sqrt(ship.getMass()));
            Vector3d controltorque = new Vector3d(torque.x*torquescale, torque.y*torquescale, torque.z*torquescale);
            if(controltorque.length()<0.1) {
                controltorque.mul(0);
            }

            Vector3d Invarianttorque = calculateWorldTorque(controltorque, worldXDirection, worldYDirection, worldZDirection);
            double forcescale = -1000 * data.getThrottle() * data.thruster_strength / (ship.getMass()*3);
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

}
