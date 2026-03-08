package com.kodu16.vsie.network.controlseat.S2C;

import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.function.Supplier;

import org.joml.Vector3d;
import org.slf4j.Logger;


public class ControlSeatS2CPacket {
    private final BlockPos pos;
    private final Vector3d shipFacing;
    private final Vector3d shipUp;
    public String enemy;
    public String ally;
    public String lockedenemyslug;
    public int throttle;

    // 构造函数
    public ControlSeatS2CPacket(BlockPos pos, Vector3d shipFacing, Vector3d shipUp, String enemy, String ally, String lockedenemyslug, int throttle) {
        this.pos = pos;
        this.shipFacing = shipFacing;
        this.shipUp = shipUp;
        this.enemy = enemy;
        this.ally = ally;
        this.lockedenemyslug = lockedenemyslug;
        this.throttle = throttle;
    }

    // 编码（序列化）
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeDouble(shipFacing.x);
        buf.writeDouble(shipFacing.y);
        buf.writeDouble(shipFacing.z);
        buf.writeDouble(shipUp.x);
        buf.writeDouble(shipUp.y);
        buf.writeDouble(shipUp.z);
        buf.writeUtf(enemy, 64);   // 建议限制长度，防止恶意超长字符串
        buf.writeUtf(ally, 64);
        buf.writeUtf(lockedenemyslug,64);
        buf.writeInt(throttle);
    }

    // 解码（反序列化）
    public static ControlSeatS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        double facingX = buf.readDouble();
        double facingY = buf.readDouble();
        double facingZ = buf.readDouble();
        double upX = buf.readDouble();
        double upY = buf.readDouble();
        double upZ = buf.readDouble();
        String enemy = buf.readUtf(64);
        String ally = buf.readUtf(64);
        String lockedenemyslug = buf.readUtf(64);
        Vector3d shipFacing = new Vector3d(facingX, facingY, facingZ);
        Vector3d shipUp = new Vector3d(upX, upY, upZ);
        int throttle = buf.readInt();
        return new ControlSeatS2CPacket(pos, shipFacing, shipUp, enemy, ally, lockedenemyslug,throttle);
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
                //LogUtils.getLogger().warn("Received ControlSeatS2C but clientData is null for player {}",
                //mc.player.getName().getString());
                return;
            }
            //LOGGER.warn(String.valueOf(Component.literal("writing S2C data to:"+player+" channelencode:"+channelencode)));
            clientData.updateShipVectors(shipFacing, shipUp);
            clientData.setUserUUID(player.getUUID());

            clientData.enemy = enemy;
            clientData.ally = ally;
            clientData.lockedenemyslug = lockedenemyslug;
            clientData.throttle = throttle;
            //LOGGER.warn(String.valueOf(Component.literal("S2C data:enemy:"+clientData.enemy+"ally:"+clientData.ally)));
            // 这里可以进一步根据需要应用旋转到某个实体或者更新视角
        }));
        ctx.get().setPacketHandled(true);
    }
}
