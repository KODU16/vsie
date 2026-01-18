package com.kodu16.vsie.network.controlseat;

import com.kodu16.vsie.content.controlseat.client.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

import org.joml.Vector3d;
import org.slf4j.Logger;


public class ControlSeatInputS2CPacket implements Packet<ClientGamePacketListener> {
    private final BlockPos pos;
    private final Vector3d shipFacing;

    // 构造函数
    public ControlSeatInputS2CPacket(BlockPos pos, Vector3d shipFacing) {
        this.pos = pos;
        this.shipFacing = shipFacing;
    }

    // 编码（序列化）
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeDouble(shipFacing.x);
        buf.writeDouble(shipFacing.y);
        buf.writeDouble(shipFacing.z);
    }

    // 解码（反序列化）
    public static ControlSeatInputS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        double facingX = buf.readDouble();
        double facingY = buf.readDouble();
        double facingZ = buf.readDouble();
        Vector3d shipFacing = new Vector3d(facingX, facingY, facingZ);
        return new ControlSeatInputS2CPacket(pos, shipFacing);
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
            ControlSeatClientData clientData = ClientDataManager.getClientData(player);
            //LOGGER.warn(String.valueOf(Component.literal("writing S2C data to:"+player+" uuid:"+player.getUUID())));
            // 更新四元数
            clientData.setShipFacing(shipFacing);
            clientData.setUserUUID(player.getUUID());

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
