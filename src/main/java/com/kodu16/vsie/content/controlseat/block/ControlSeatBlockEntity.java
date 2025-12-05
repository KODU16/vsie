package com.kodu16.vsie.content.controlseat.block;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.Initialize;
import com.kodu16.vsie.content.controlseat.ShipControlEvent;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.controlseat.client.ClientInputHandler;

import com.kodu16.vsie.content.controlseat.server.SeatRegistry;
import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.turret.TurretData;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
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
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.entity.ShipMountingEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ControlSeatBlockEntity extends AbstractControlSeatBlockEntity {
    //private final ControlSeatServerData serverData = new ControlSeatServerData();
    public static boolean ride = false;
    private boolean hasInitialized = false;
    public int throttle = 0;
    //即使我不想写的这么恶心，为了跨维度我还是得干
    //有两个hashmap，第二个是为了渲染HUD的时候用来反查controlseat
    private List<ShipMountingEntity> seats = new ArrayList<>();

    public SmartFluidTankBehaviour tank;

    public ControlSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
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
        //考虑到现在用鼠标劫持，也许可以把鼠标位置存在mixin里头
        ClientInputHandler.handle(lp, pos);
    }

    //再从服务端更新推力和力矩
    //窝草你发包怎么不告诉我对应不了服务端
    @Override
    public void serverTick() {
        if (!ride) {
            controlseatData.reset();
        }
    }

    public void commonTick() {
        //我是个sb，现在不用死脑筋的一个个扒方块检测推进器，直接用forge广播输入的力和输入的力矩，需要的就接收
        //其它外设同理，这样才能实现按下鼠标同时开火
        //至于fabric，我自己才懒得做支持
        Logger LOGGER = LogUtils.getLogger();
        if (level.isClientSide)
            return;
        if (hasInitialized) {
            broadcastControlInput();
            this.throttle = getControlSeatData().throttle;
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

    public void broadcastControlInput() {
        if (level.isClientSide) return;
        if (controlseatData.getPlayer() == null) return;

        ServerShip ship = VSGameUtilsKt.getShipManagingPos((ServerLevel) level, getBlockPos());
        if (ship == null) return;

        // 你已经算好的最终力和力矩
        Vector3d finalForce = controlseatData.getFinalforce();   // 假设是世界坐标
        Vector3d finalTorque = controlseatData.getFinaltorque();
        float throttle = controlseatData.getThrottle(); // 自己加一个

        ShipControlEvent event = new ShipControlEvent(
                ship, controlseatData.getPlayer(), controlseatData,
                finalForce, finalTorque, throttle
        );

        // 使用 MinecraftForge 事件总线广播（跨模组最通用）
        MinecraftForge.EVENT_BUS.post(event);
    }


    protected boolean isWorking() {
        return true;
    }

    public int getThrottle() {return this.throttle;}


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
        LOGGER.warn(String.valueOf(Component.literal("seated player detected:"+controlseatData.getPlayer()+" uuid:"+controlseatData.getPlayer().getUUID())));
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
            mounterPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.125, pos.getZ() + 1.3125);
        } else if (facing == Direction.SOUTH) {
            mounterPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.125, pos.getZ() - 0.3125);
        } else if (facing == Direction.EAST) {
            mounterPos = new Vector3d(pos.getX() - 0.3125, pos.getY() + 0.125, pos.getZ() + .5);
        } else {
            mounterPos = new Vector3d(pos.getX() + 1.3125, pos.getY() + 0.125, pos.getZ() + .5);
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
            player.displayClientMessage(Component.literal("ride = true"), true);
            seats.add(seat);
            // Initialize mouse handler when the player sits down
        } else {
            player.displayClientMessage(Component.literal("ride = false"), true);
        }
        return ride;
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
        tag.putInt("throttle", this.getThrottle());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("throttle")) {this.throttle = tag.getInt("throttle");}
    }
}
