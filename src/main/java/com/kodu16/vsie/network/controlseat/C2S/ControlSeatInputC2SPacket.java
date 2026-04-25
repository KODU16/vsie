// ControlSeatInputC2SPacket.java
package com.kodu16.vsie.network.controlseat.C2S;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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

public class ControlSeatInputC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<ControlSeatInputC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_c2s_controlseatinputc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatInputC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ControlSeatInputC2SPacket::encode, ControlSeatInputC2SPacket::decode);

    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final int keys;   // bitmask
    public final boolean isviewlock;
    // 功能：客户端预计算后的手动瞄准目标点（玩家视线延伸 1024 格的世界坐标）。
    public final double aimTargetX;
    public final double aimTargetY;
    public final double aimTargetZ;

    public ControlSeatInputC2SPacket(BlockPos pos, int keys, boolean isviewlock, double aimTargetX, double aimTargetY, double aimTargetZ) {
        this.pos = pos;
        this.keys = keys;
        this.isviewlock = isviewlock;
        this.aimTargetX = aimTargetX;
        this.aimTargetY = aimTargetY;
        this.aimTargetZ = aimTargetZ;
    }

    public static void encode(ControlSeatInputC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.keys);
        buf.writeBoolean(pkt.isviewlock);
        buf.writeDouble(pkt.aimTargetX);
        buf.writeDouble(pkt.aimTargetY);
        buf.writeDouble(pkt.aimTargetZ);
    }

    public static ControlSeatInputC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int keys = buf.readVarInt();
        Boolean isviewlock = buf.readBoolean();
        double aimTargetX = buf.readDouble();
        double aimTargetY = buf.readDouble();
        double aimTargetZ = buf.readDouble();
        return new ControlSeatInputC2SPacket(pos, keys, isviewlock, aimTargetX, aimTargetY, aimTargetZ);
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(ControlSeatInputC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
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
            if((keys & KeysInput.CHANNEL1) !=0) {
                serverData.channel1 = !serverData.getChannel1();
            }
            if((keys & KeysInput.CHANNEL2) !=0) {
                serverData.channel2 = !serverData.getChannel2();
            }
            if((keys & KeysInput.CHANNEL3) !=0) {
                serverData.channel3 = !serverData.getChannel3();
            }
            if((keys & KeysInput.CHANNEL4) !=0) {
                serverData.channel4 = !serverData.getChannel4();
            }
            // 功能：在服务端输入处理后立即重建频道位掩码，确保 S2C/HUD 使用的是最新频道状态。
            serverData.channelencode =
                    (serverData.channel1 ? (1 << 0) : 0)
                    | (serverData.channel2 ? (1 << 1) : 0)
                    | (serverData.channel3 ? (1 << 2) : 0)
                    | (serverData.channel4 ? (1 << 3) : 0);
            if((keys & KeysInput.SWITCHENEMY) !=0) {
                if(!serverData.enemyshipsData.isEmpty()) {
                    int index = serverData.lockedenemyindex+1;
                    serverData.lockedenemyindex = index%serverData.enemyshipsData.size();
                }
            }
            if((keys & KeysInput.TOGGLESHIELD) !=0) {
                boolean shield = serverData.isshieldon;
                serverData.isshieldon = !shield;
            }
            if((keys & KeysInput.TOGGLEFLIGHTASSIST) !=0) {
                boolean assist = serverData.isflightassiston;
                serverData.isflightassiston = !assist;
            }
            if((keys & KeysInput.TOGGLEANTIGRAVITY) !=0) {
                boolean antigravity = serverData.isantigravityon;
                serverData.isantigravityon = !antigravity;
            }
            // 可选：标记方块实体为脏以保存更改
            serverData.isviewlocked = pkt.isviewlock;
            // 功能：服务端缓存客户端上传的手动瞄准目标点，供重型炮塔直接作为 targetPos 使用。
            serverData.manualAimTargetX = pkt.aimTargetX;
            serverData.manualAimTargetY = pkt.aimTargetY;
            serverData.manualAimTargetZ = pkt.aimTargetZ;
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
        public static final int SWITCHENEMY = 1 << 4; //切换锁定敌人
        public static final int TOGGLESHIELD = 1 << 5; //切换开关护盾
        public static final int TOGGLEFLIGHTASSIST = 1 << 6; //切换飞行辅助
        public static final int TOGGLEANTIGRAVITY = 1 << 7; //切换反重力
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
