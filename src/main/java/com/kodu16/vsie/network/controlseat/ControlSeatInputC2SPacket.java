// ControlSeatInputC2SPacket.java
package com.kodu16.vsie.network.controlseat;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.chat.Component;
import org.joml.Vector3d;

import java.util.function.Supplier;
import org.slf4j.Logger;

public class ControlSeatInputC2SPacket {
    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final float mousex;
    public final float mousey;
    public final float roll;
    public final int keys;   // bitmask

    public ControlSeatInputC2SPacket(BlockPos pos, float mousex, float mousey, float roll, int keys) {
        this.pos = pos;
        this.mousex = mousex;
        this.mousey = mousey;
        this.roll = roll;
        this.keys = keys;
    }

    public static void encode(ControlSeatInputC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeFloat(pkt.mousex);
        buf.writeFloat(pkt.mousey);
        buf.writeFloat(pkt.roll);
        buf.writeVarInt(pkt.keys);
    }

    public static ControlSeatInputC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        float mousex = buf.readFloat();
        float mousey = buf.readFloat();
        float roll = buf.readFloat();
        int keys = buf.readVarInt();
        return new ControlSeatInputC2SPacket(pos, mousex, mousey, roll, keys);
    }

    public static void handle(ControlSeatInputC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        //事情跟我想象的有点不一样
        //我得把拿到的这些存在服务端的Data里
        //我得好好想想怎么保证一对一
        //也就是玩家客户端发的包能存到他坐的座椅的的Data里
        //说不定用pos做桥梁可以，pos服务端和客户端一样
        //现在不在这计算力矩了，在shiphandler里结合船只信息计算
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            // 读取玩家输入
            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            float mousex = pkt.mousex;
            float mousey = pkt.mousey;
            float roll = pkt.roll;
            int keys = pkt.keys;
            BlockEntity seat = level.getBlockEntity(pos);
            if (!(seat instanceof ControlSeatBlockEntity controlSeat)) {
                // Optionally log an error if the block entity is not found or is incorrect
                sender.sendSystemMessage(Component.literal("Invalid control seat at " + pos));
                return;
            }
            boolean isThrottlePressed = (keys & ControlSeatInputC2SPacket.Keys.THROTTLE) != 0;
            boolean isBrakePressed = (keys & ControlSeatInputC2SPacket.Keys.BRAKE) != 0;
            boolean isPeripheralPressed = (keys & ControlSeatInputC2SPacket.Keys.SCAN_PERIPHERAL) != 0;
            int finalthrottledelta = isThrottlePressed ? 1 : (isBrakePressed ? -1 : 0);
            //LOGGER.warn(String.valueOf(Component.literal("delta throttle:"+finalthrottledelta)));

            if (Float.isNaN(pkt.mousex) || Float.isNaN(pkt.mousey) || Float.isNaN(pkt.roll)){
                sender.sendSystemMessage(Component.literal("Invalid torque input! check packet"));
                return;
            }
            else {
                ControlSeatServerData serverData = controlSeat.getServerData(); // Ensure this method exists
                int finalthrottle = Math.max(-100, Math.min(serverData.getThrottle()+finalthrottledelta, 100));
                //LOGGER.warn(String.valueOf(Component.literal("final throttle:"+finalthrottle)));

                serverData.setTorque(new Vector3d(0, -mousex, mousey));
                serverData.setThrottle(finalthrottle);
            }


            // 处理按键输入（使用 bitmask）
            if ((keys & Keys.THROTTLE) != 0) {
            //    controlSeat.moveForward(sender);
            }
            // 可选：标记方块实体为脏以保存更改
            controlSeat.setChanged();
        });
        ctx.setPacketHandled(true);
    }


    /** 按键 bitmask 的位定义（客户端/服务端共享同一份定义以避免错位） */
    public static final class Keys {
        public static final int THROTTLE = 1 << 0;
        public static final int BRAKE    = 1 << 1;
        public static final int SCAN_PERIPHERAL = 1 << 2;
        public static final int ROLLL    = 1 << 3;
        public static final int ROLLR   = 1 << 4;
        public static final int SPACE    = 1 << 5;
        public static final int SHIFT   = 1 << 6;
        public static final int CTRL  = 1 << 7;
        public static final int MOUSEL = 1 << 8;  // 鼠标左
        public static final int MOUSER  = 1 << 9;  // 鼠标右
    }
}
