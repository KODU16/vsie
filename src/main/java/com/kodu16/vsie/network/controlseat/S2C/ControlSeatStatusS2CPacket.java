package com.kodu16.vsie.network.controlseat.S2C;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.Input.ClientDataManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.util.function.Supplier;


public class ControlSeatStatusS2CPacket {
    //船只整体的各项状态，不需要快包那么快更新，但是也不能很慢
    private final BlockPos pos;
    public int energyavalible;
    public int energytotal;
    public int fuelavalible;
    public int fueltotal;
    public boolean shieldon;
    public int shieldavalible;
    public int shieldtotal;
    public boolean flightassiston;
    public boolean antigravityon;

    // 构造函数
    public ControlSeatStatusS2CPacket(BlockPos pos,
                                      int energyavalible, int energytotal,
                                      int fuelavalible,int fueltotal,
                                      boolean shieldon, int shieldavalible, int shieldtotal,
                                      boolean flightassiston, boolean antigravityon) {
        this.pos = pos;
        this.energyavalible = energyavalible;
        this.energytotal = energytotal;
        this.fuelavalible = fuelavalible;
        this.fueltotal = fueltotal;
        this.shieldon = shieldon;
        this.shieldavalible = shieldavalible;
        this.shieldtotal = shieldtotal;
        this.flightassiston = flightassiston;
        this.antigravityon = antigravityon;
    }

    // 编码（序列化）
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(energyavalible);
        buf.writeInt(energytotal);
        buf.writeInt(fuelavalible);
        buf.writeInt(fueltotal);
        buf.writeBoolean(shieldon);
        buf.writeInt(shieldavalible);
        buf.writeInt(shieldtotal);
        buf.writeBoolean(flightassiston);
        buf.writeBoolean(antigravityon);
    }

    // 解码（反序列化）
    public static ControlSeatStatusS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int energyavalible = buf.readInt();
        int energytotal = buf.readInt();
        int fuelavalible = buf.readInt();
        int fueltotal = buf.readInt();
        boolean shieldon = buf.readBoolean();
        int shieldavalible = buf.readInt();
        int shieldtotal = buf.readInt();
        boolean flightassiston = buf.readBoolean();
        boolean antigravityon = buf.readBoolean();
        return new ControlSeatStatusS2CPacket(pos, energyavalible, energytotal, fuelavalible, fueltotal, shieldon, shieldavalible, shieldtotal, flightassiston, antigravityon);
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
                return;
            }
            clientData.energyavalible = energyavalible;
            clientData.energytotal = energytotal;

            clientData.fuelavalible = fuelavalible;
            clientData.fueltotal = fueltotal;

            clientData.shieldon = shieldon;
            clientData.shieldavalible = shieldavalible;
            clientData.shieldtotal = shieldtotal;

            clientData.isflightassiston = flightassiston;
            clientData.isantigravityon = antigravityon;
        }));
        ctx.get().setPacketHandled(true);
    }
}
