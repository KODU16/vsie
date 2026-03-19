// ControlSeatInputC2SPacket.java
package com.kodu16.vsie.network.controlseat.C2S;

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

public class ControlSeatC2SPacket {
    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final float mousex;
    public final float mousey;
    public final float roll;
    public final int keys;   // bitmask
    public final boolean mouseLpress;

    public ControlSeatC2SPacket(BlockPos pos, float mousex, float mousey, float roll, int keys, boolean mouseLpress) {
        this.pos = pos;
        this.mousex = mousex;
        this.mousey = mousey;
        this.roll = roll;
        this.keys = keys;
        this.mouseLpress = mouseLpress;
    }

    public static void encode(ControlSeatC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeFloat(pkt.mousex);
        buf.writeFloat(pkt.mousey);
        buf.writeFloat(pkt.roll);
        buf.writeVarInt(pkt.keys);
        buf.writeBoolean(pkt.mouseLpress);
    }

    public static ControlSeatC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        float mousex = buf.readFloat();
        float mousey = buf.readFloat();
        float roll = buf.readFloat();
        int keys = buf.readVarInt();
        boolean mouseLpress = buf.readBoolean();
        return new ControlSeatC2SPacket(pos, mousex, mousey, roll, keys, mouseLpress);
    }

    public static void handle(ControlSeatC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
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
            boolean mouseLpress = pkt.mouseLpress;
            BlockEntity seat = level.getBlockEntity(pos);
            if (!(seat instanceof ControlSeatBlockEntity controlSeat)) {
                // Optionally log an error if the block entity is not found or is incorrect
                sender.sendSystemMessage(Component.literal("Invalid control seat at " + pos));
                return;
            }
            boolean isThrottlePressed = (keys & ControlSeatC2SPacket.Keys.THROTTLE) != 0;
            boolean isBrakePressed = (keys & ControlSeatC2SPacket.Keys.BRAKE) != 0;
            //boolean isPeripheralPressed = (keys & ControlSeatInputC2SPacket.Keys.SCAN_PERIPHERAL) != 0;
            int finalthrottledelta = isThrottlePressed ? 1 : (isBrakePressed ? -1 : 0);
            //LOGGER.warn(String.valueOf(Component.literal("delta throttle:"+finalthrottledelta)));

            if (Float.isNaN(pkt.mousex) || Float.isNaN(pkt.mousey) || Float.isNaN(pkt.roll)){
                sender.sendSystemMessage(Component.literal("Invalid torque input! check packet"));
                return;
            }
            else {
                ControlSeatServerData serverData = controlSeat.getServerData(); // Ensure this method exists
                if (serverData.isWarpPreparing) {
                    // 功能：warp 准备状态期间屏蔽玩家鼠标/键盘产生的姿态与推力输入，改由自动对准逻辑接管。
                    serverData.setTorque(new Vector3d(0, 0, 0));
                    serverData.setThrottle(0);
                } else {
                    int finalthrottle = Math.max(-100, Math.min(serverData.getThrottle()+finalthrottledelta, 100));
                    //LOGGER.warn(String.valueOf(Component.literal("final throttle:"+finalthrottle)));
                    serverData.setTorque(new Vector3d(0, -mousex, mousey));
                    serverData.setThrottle(finalthrottle);
                }

            }
            ControlSeatServerData serverData = controlSeat.getServerData(); // Ensure this method exists
            // 处理按键输入（使用 bitmask）
            if ((keys & Keys.THROTTLE) != 0) {
            //    controlSeat.moveForward(sender);
            }

            //处理鼠标左键
            serverData.isfiring = mouseLpress;

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
