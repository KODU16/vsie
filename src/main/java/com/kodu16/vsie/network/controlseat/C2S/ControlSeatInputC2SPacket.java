// ControlSeatInputC2SPacket.java
package com.kodu16.vsie.network.controlseat.C2S;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ControlSeatInputC2SPacket {
    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final int keys;   // bitmask

    public ControlSeatInputC2SPacket(BlockPos pos, int keys) {
        this.pos = pos;
        this.keys = keys;
    }

    public static void encode(ControlSeatInputC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.keys);
    }

    public static ControlSeatInputC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int keys = buf.readVarInt();
        return new ControlSeatInputC2SPacket(pos, keys);
    }

    public static void handle(ControlSeatInputC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            // 读取玩家输入
            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            int keys = pkt.keys;
            BlockEntity seat = level.getBlockEntity(pos);
            if (!(seat instanceof ControlSeatBlockEntity controlSeat)) {
                // Optionally log an error if the block entity is not found or is incorrect
                sender.sendSystemMessage(Component.literal("Invalid control seat at " + pos));
                return;
            }
            ControlSeatServerData serverData = controlSeat.getServerData();
            if((keys & ControlSeatInputC2SPacket.KeysInput.CHANNEL1) !=0) {
                serverData.channel1 = !serverData.getChannel1();
            }
            if((keys & ControlSeatInputC2SPacket.KeysInput.CHANNEL2) !=0) {
                serverData.channel2 = !serverData.getChannel2();
            }
            if((keys & ControlSeatInputC2SPacket.KeysInput.CHANNEL3) !=0) {
                serverData.channel3 = !serverData.getChannel3();
            }
            if((keys & ControlSeatInputC2SPacket.KeysInput.CHANNEL4) !=0) {
                serverData.channel4 = !serverData.getChannel4();
            }
            // 可选：标记方块实体为脏以保存更改
            controlSeat.setChanged();
        });
        ctx.setPacketHandled(true);
    }


    /** 按键 bitmask 的位定义（客户端/服务端共享同一份定义以避免错位） */
    public static final class KeysInput {
        public static final int CHANNEL1 = 1 << 0;
        public static final int CHANNEL2 = 1 << 1;
        public static final int CHANNEL3 = 1 << 2;
        public static final int CHANNEL4 = 1 << 3;
    }
}
