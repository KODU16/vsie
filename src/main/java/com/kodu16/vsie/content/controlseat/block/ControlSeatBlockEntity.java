package com.kodu16.vsie.content.controlseat.block;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.Initialize;
import com.kodu16.vsie.content.controlseat.functions.ScanNearByShips;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientMouseHandler;

import com.kodu16.vsie.content.controlseat.server.SeatRegistry;
import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.entity.ShipMountingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ControlSeatBlockEntity extends AbstractControlSeatBlockEntity {
    //private final ControlSeatServerData serverData = new ControlSeatServerData();
    public volatile boolean ride = false;
    private boolean hasInitialized = false;
    public boolean previousfirestatus = false;
    //即使我不想写的这么恶心，为了跨维度我还是得干
    //有两个hashmap，第二个是为了渲染HUD的时候用来反查controlseat
    private List<ShipMountingEntity> seats = new ArrayList<>();

    public SmartFluidTankBehaviour tank;

    public ControlSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public String getcontrolseattype() {
        return "control_seat";
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }


    //先接收client更新，叫client向服务端发包
    public void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        BlockPos pos = getBlockPos();
        // 只有当本地玩家就是这张座椅的乘客时才生效
        //这是个静态方法，最好提前确定好你在server存好了他上一次的鼠标位置和他上一次操作时间
        ClientMouseHandler.handle(lp, pos);
    }

    //再从服务端更新推力和力矩
    //窝草你发包怎么不告诉我对应不了服务端

    public void tick() {
        Logger LOGGER = LogUtils.getLogger();
        if (level.isClientSide)
            return;
        if (hasInitialized) {

            //update
            if (!ride) {
                controlseatData.reset();
                controlseatData.setPlayer(null);
            }
            updateThruster();
            updateWeapon();

            //scanships
            qsd = VSGameUtilsKt.getAllShips(level);
            BlockPos pos = this.getBlockPos();
            Map<String, Object> shipmapper = ScanNearByShips.scanships(qsd,pos,level);
            // 遍历所有扫描到的船
            for (Map.Entry<String, Object> entry : shipmapper.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> shipData = (Map<String, Object>) entry.getValue();

                String id    = (String)   shipData.get("id");     // 通常是 Long.toString()
                String name  = (String)   shipData.get("slug");   // 船的名字
                Double x     = (Double)   shipData.get("x");
                Double y     = (Double)   shipData.get("y");
                Double z     = (Double)   shipData.get("z");

                // 安全检查（防止 null）
                if (name == null) name = "Unnamed ship";
                if (x == null || y == null || z == null) continue;

                System.out.printf("船 %-20s  ID: %s   位置: %.1f, %.1f, %.1f%n",
                        name, id, x, y, z);

                // 或收集到你自己的结构里
                // yourShipList.add(new ShipInfo(name, x, y, z));
            }
        }
        else {
            LOGGER.warn(String.valueOf(Component.literal("detected uninitialized controlseat, time to sweep valkyrie's ass")));
            BlockPos pos = getBlockPos();
            BlockState state = null;
            if (level != null) {
                state = level.getBlockState(pos);
            }
            if (state != null) {
                Initialize.initialize(level, pos, state);
                hasInitialized = true;
                LOGGER.warn(String.valueOf(Component.literal("controlseat Initialize complete:"+pos)));
            }
        }

    }

    public void updateThruster() {
        List<Vec3> toRemove = new ArrayList<>();

        this.forEachLinkedPeripheral(pos -> {
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity be = level.getBlockEntity(blockPos);

            if (be instanceof AbstractThrusterBlockEntity thruster) {
                Logger LOGGER = LogUtils.getLogger();
                //LOGGER.warn("writing to thrusters:" +blockPos+ "torque:"+controlseatData.getFinaltorque()+"force:"+controlseatData.getFinalforce());
                thruster.setdata(controlseatData.getFinaltorque(), controlseatData.getFinalforce());
            } else {
                // 先记下来，循环完了再删
                toRemove.add(pos);
            }
        }, 0);

        // 循环结束后统一删除
        for (Vec3 pos : toRemove) {
            removeLinkedPeripheral(pos, 0);
        }
    }

    public void updateWeapon() {
        if(previousfirestatus == controlseatData.isfiring) return;
        previousfirestatus = controlseatData.isfiring;
        for (int i = WeaponCache.size() - 1; i >= 0; i--) {
            AbstractWeaponBlockEntity weapon = WeaponCache.get(i);

            if (weapon == null || weapon.isRemoved() || !level.isLoaded(weapon.getBlockPos())) {
                WeaponCache.remove(i);
                // 记得同步清理 weaponRefs 和 linked peripheral
                continue;
            }

            // 正常发信号
            int encoded = 0;
            if (controlseatData.getChannel1()) encoded |= 1;
            if (controlseatData.getChannel2()) encoded |= 2;
            if (controlseatData.getChannel3()) encoded |= 4;
            if (controlseatData.getChannel4()) encoded |= 8;
            if (controlseatData.isfiring) {
                weapon.receivechannel(encoded);
            } else {
                weapon.receivechannel(0);
            }
        }
    }



    protected boolean isWorking() {
        return true;
    }


    public static void lookAtEntityPos(Entity entity, Vec3 target) {
        Vec3 entityPos = entity.getEyePosition();
        double dx = target.x - entityPos.x;
        double dy = target.y - entityPos.y;
        double dz = target.z - entityPos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        float pitch = (float) (-(Mth.atan2(dy, distXZ) * (180F / Math.PI)));

        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;

        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
            living.yHeadRotO = yaw;
            living.setYBodyRot(yaw);
            living.yBodyRotO = yaw;
        }
    }

    public ControlSeatServerData getServerData() { return controlseatData; }

    //public ControlSeatClientData getClientData() { return ControlSeatClientData; }

    public boolean sit(Player player, boolean force) {
        if (player.level().isClientSide) {
            return false;
        }
        final Logger LOGGER = LogUtils.getLogger();
        //player.displayClientMessage(Component.literal("server side, executing sit logic"), true);

        if (!force && player.getVehicle() != null && player.getVehicle().getType() == ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE && seats.contains(player.getVehicle())) {
            //player.displayClientMessage(Component.literal("already sitting, returning true"), true);
            return true;
        }

        ServerLevel serverLevel = (ServerLevel) player.level();
        controlseatData.setPlayer(player);
        //LOGGER.warn(String.valueOf(Component.literal("seated player detected:"+controlseatData.getPlayer()+" uuid:"+controlseatData.getPlayer().getUUID())));
        return startRiding(force, getBlockPos(), getBlockState(), serverLevel);
    }


    // 在移除座椅时清除控制记录
    @Override
    public void onRemove() {
        controlseatData.reset();
        if (level != null && !level.isClientSide()) {
            for (ShipMountingEntity seat : seats) {
                SeatRegistry.SEAT_TO_CONTROLSEAT.remove(seat.getUUID());
                seat.kill();
            }
            seats.clear();
        }
        // 移除玩家的 UUID 记录
        super.setRemoved();
    }


    ShipMountingEntity spawnSeat(BlockPos pos, BlockState state, ServerLevel level) {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        Vector3dc mounterPos;
        if (facing == Direction.NORTH) {
            mounterPos = new Vector3d(pos.getX()+0.5, pos.getY(), pos.getZ());
        } else if (facing == Direction.SOUTH) {
            mounterPos = new Vector3d(pos.getX()+0.5, pos.getY(), pos.getZ()+1);
        } else if (facing == Direction.EAST) {
            mounterPos = new Vector3d(pos.getX()+1, pos.getY(), pos.getZ()+0.5);
        } else {
            mounterPos = new Vector3d(pos.getX(), pos.getY(), pos.getZ()+0.5);
        }

        ShipMountingEntity entity = ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE.create(level);
        assert entity != null;
        entity.setPos(mounterPos.x(), mounterPos.y(), mounterPos.z());
        Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        lookAtEntityPos(entity, target);
        entity.setPos(mounterPos.x(), mounterPos.y(), mounterPos.z());
        entity.setDeltaMovement(0, 0, 0);
        entity.setController(true);
        level.addFreshEntityWithPassengers(entity);
        SeatRegistry.SEAT_TO_CONTROLSEAT.put(entity.getUUID(), pos);
        return entity;
    }

    // 修改 startRiding 方法，确保每个座椅控制与玩家 UUID 相关联
    public boolean startRiding(boolean force, BlockPos blockPos, BlockState state, ServerLevel level) {
        Player player = controlseatData.getPlayer();
        Initialize.initialize(level,blockPos,state);
        // 使用玩家的 UUID 来确定哪个玩家在这个座椅上
        // 清理空的座椅
        for (int i = seats.size() - 1; i >= 0; i--) {
            ShipMountingEntity seat = seats.get(i);
            if (!seat.isVehicle()) {
                seat.kill();
                seats.remove(i);

            } else if (!seat.isAlive()) {
                seats.remove(i);
            }
        }

        ShipMountingEntity seat = spawnSeat(blockPos, state, level);
        ride = player.startRiding(seat, force);

        if (ride) {
            seats.add(seat);
            // Initialize mouse handler when the player sits down
        }
        return ride;
    }
}
