package com.kodu16.vsie.network.controlseat.S2C;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

//按键的packet必须慢发包，否则按一下按键跳三下，所以单独出来了
//不止是按键，也包括IFF之类的不是随时更新的内容
public class ControlSeatInputS2CPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<ControlSeatInputS2CPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_s2c_controlseatinputs2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatInputS2CPacket> STREAM_CODEC = CustomPacketPayload.codec((buf, pkt) -> pkt.write(buf), ControlSeatInputS2CPacket::decode);

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
    // 功能：NeoForge 1.21.1 处理器入口，复用旧版实例方法逻辑。
    public static void handle(ControlSeatInputS2CPacket pkt, IPayloadContext context) {
        pkt.handle(() -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

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
                    }
                    else {
                        clientData.channel1 = false;
                    }
                }
                if(i==1){
                    if ((channelencode & 1<<i) != 0) {
                        clientData.channel2 = true;
                    }
                    else {
                        clientData.channel2 = false;
                    }
                }
                if(i==2){
                    if ((channelencode & 1<<i) != 0) {
                        clientData.channel3 = true;
                    }
                    else {
                        clientData.channel3 = false;
                    }
                }
                if(i==3){
                    if ((channelencode & 1<<i) != 0) {
                        clientData.channel4 = true;
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


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
