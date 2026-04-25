package com.kodu16.vsie.network.screen;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ScreentypeC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<ScreentypeC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "screen_screentypec2spacket"));
    public static final StreamCodec<FriendlyByteBuf, ScreentypeC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ScreentypeC2SPacket::encode, ScreentypeC2SPacket::decode);


    private static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final int changetype;


    public ScreentypeC2SPacket(BlockPos blockPos, int changetype) {
        this.pos = blockPos;
        this.changetype = changetype;
    }

    public static void encode(ScreentypeC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.changetype);   // 建议限制长度，防止恶意超长字符串
    }

    public static ScreentypeC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int changetype = buf.readInt();
        return new ScreentypeC2SPacket(pos, changetype);
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(ScreentypeC2SPacket msg, IPayloadContext context) {
        handle(msg, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(ScreentypeC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // 必须在 enqueueWork 里处理服务端逻辑
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            // 读取玩家输入
            ServerLevel level = sender.serverLevel();
            BlockEntity BE = level.getBlockEntity(msg.pos);
            if(BE instanceof AbstractScreenBlockEntity screen) {
                screen.setscreendisplaytype(msg.changetype);
            }
        });

        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
