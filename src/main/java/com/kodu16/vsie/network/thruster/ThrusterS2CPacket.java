package com.kodu16.vsie.network.thruster;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

import org.slf4j.Logger;


public class ThrusterS2CPacket implements Packet<ClientGamePacketListener> {
    private final BlockPos pos;
    private final float raylength;

    // 构造函数
    public ThrusterS2CPacket(BlockPos pos, float raylength) {
        this.pos = pos;
        this.raylength = raylength;
    }

    // 编码（序列化）
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(raylength);
    }

    // 解码（反序列化）
    public static ThrusterS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        float raylength = buf.readFloat();
        return new ThrusterS2CPacket(pos, raylength);
    }

    // 处理客户端接收到的数据包
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        Logger LOGGER = LogUtils.getLogger();
        //LOGGER.warn(String.valueOf(Component.literal("S2C packet created")));
        ctx.get().enqueueWork(() -> {
            // 获取当前客户端的玩家
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            // 获取对应玩家的 ControlSeatClientData
            // 我差点忘了这clientdatamanager里有获取准确的玩家对应的clientdata的方法，我的问题

            // 这里可以进一步根据需要应用旋转到某个实体或者更新视角
        });
        ctx.get().setPacketHandled(true);
    }

    // 实现 Packet 接口的方法
    @Override
    public void handle(ClientGamePacketListener listener) {
        // Forge 网络通常使用 handle(Supplier<NetworkEvent.Context>)，此处留空
    }
}
