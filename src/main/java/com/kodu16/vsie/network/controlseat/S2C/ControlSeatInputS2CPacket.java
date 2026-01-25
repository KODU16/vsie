package com.kodu16.vsie.network.controlseat.S2C;

import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.Input.ControlSeatClientData;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

//按键的packet必须慢发包，否则按一下按键跳三下，所以单独出来了
public class ControlSeatInputS2CPacket {
    private final BlockPos pos;
    private final int channelencode;

    // 构造函数
    public ControlSeatInputS2CPacket(BlockPos pos, int channelencode) {
        this.pos = pos;
        this.channelencode = channelencode;
    }

    // 编码（序列化）
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(channelencode);
    }

    // 解码（反序列化）
    public static ControlSeatInputS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int channelencode = buf.readInt();
        return new ControlSeatInputS2CPacket(pos, channelencode);
    }

    // 处理客户端接收到的数据包
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        Logger LOGGER = LogUtils.getLogger();
        //LOGGER.warn(String.valueOf(Component.literal("S2C packet created")));
        ctx.get().enqueueWork(() ->
                // 确保只在物理客户端执行以下代码
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            // 获取当前客户端的玩家
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            // 获取对应玩家的 ControlSeatClientData
            ControlSeatClientData clientData = ClientDataManager.getClientData(player);
            if (clientData == null) {
                // 至少先打日志，方便定位
                LogUtils.getLogger().warn("Received ControlSeatS2C but clientData is null for player {}",
                        mc.player.getName().getString());
                return;
            }
            //LOGGER.warn(String.valueOf(Component.literal("writing S2C data to:"+player+" channelencode:"+channelencode)));
            for (int i = 0; i < 4; i++) {
                if(i==0){
                    if ((channelencode & 1<<i) != 0) {
                        clientData.channel1 = true;
                        LOGGER.warn(String.valueOf(Component.literal("channel open:1")));
                    }
                    else {
                        clientData.channel1 = false;
                    }
                }
                if(i==1){
                    if ((channelencode & 1<<i) != 0) {
                        clientData.channel2 = true;
                        LOGGER.warn(String.valueOf(Component.literal("channel open:2")));
                    }
                    else {
                        clientData.channel2 = false;
                    }
                }
                if(i==2){
                    if ((channelencode & 1<<i) != 0) {
                        clientData.channel3 = true;
                        LOGGER.warn(String.valueOf(Component.literal("channel open:3")));
                    }
                    else {
                        clientData.channel3 = false;
                    }
                }
                if(i==3){
                    if ((channelencode & 1<<i) != 0) {
                        clientData.channel4 = true;
                        LOGGER.warn(String.valueOf(Component.literal("channel open:4")));
                    }
                    else {
                        clientData.channel4 = false;
                    }
                }
            }

            // 这里可以进一步根据需要应用旋转到某个实体或者更新视角
        }));
        ctx.get().setPacketHandled(true);
    }

    // 实现 Packet 接口的方法
}
