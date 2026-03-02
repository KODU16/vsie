package com.kodu16.vsie.network.screen;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ScreenC2SPacket {

    private static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final int spinX;
    public final int spinY;
    public final int offsetX;
    public final int offsetY;
    public final int offsetZ;


    public ScreenC2SPacket(BlockPos blockPos, int spinx, int spiny, int offsetX, int offsetY, int offsetZ) {
        this.pos = blockPos;
        this.spinX = spinx;
        this.spinY = spiny;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public static void encode(ScreenC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.spinX);   // 建议限制长度，防止恶意超长字符串
        buf.writeInt(msg.spinY);
        buf.writeInt(msg.offsetX);
        buf.writeInt(msg.offsetY);
        buf.writeInt(msg.offsetZ);
    }

    public static ScreenC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int spinx = buf.readInt();
        int spiny = buf.readInt();
        int offsetx = buf.readInt();
        int offsety = buf.readInt();
        int offsetz = buf.readInt();
        return new ScreenC2SPacket(pos, spinx, spiny, offsetx, offsety, offsetz);
    }

    public static void handle(ScreenC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // 必须在 enqueueWork 里处理服务端逻辑
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            // 读取玩家输入
            ServerLevel level = sender.serverLevel();
            BlockEntity BE = level.getBlockEntity(msg.pos);
            if(BE instanceof AbstractScreenBlockEntity screen) {
                screen.setdata(msg.spinX,msg.spinY,msg.offsetX,msg.offsetY,msg.offsetZ);
            }
        });

        ctx.setPacketHandled(true);
    }
}
