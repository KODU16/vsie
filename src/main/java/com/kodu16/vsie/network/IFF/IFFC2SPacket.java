package com.kodu16.vsie.network.IFF;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class IFFC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<IFFC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "iff_iffc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, IFFC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(IFFC2SPacket::encode, IFFC2SPacket::decode);


    private static final Logger LOGGER = LogUtils.getLogger();

    public final String enemy;
    public final String ally;

    public IFFC2SPacket(String teamA, String teamB) {
        this.enemy = teamA != null ? teamA : "";
        this.ally = teamB != null ? teamB : "";
    }

    public static void encode(IFFC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.enemy, 64);   // 建议限制长度，防止恶意超长字符串
        buf.writeUtf(msg.ally, 64);
    }

    public static IFFC2SPacket decode(FriendlyByteBuf buf) {
        String a = buf.readUtf(64);
        String b = buf.readUtf(64);
        return new IFFC2SPacket(a, b);
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(IFFC2SPacket msg, IPayloadContext context) {
        handle(msg, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(IFFC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // 必须在 enqueueWork 里处理服务端逻辑
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // =======================
            //    最常见的三种目标物品选择方式（选一种即可）
            // =======================

            // 方式1：修改玩家主手物品（最常用）
            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                //player.sendSystemMessage(Component.literal("§c主手必须持有物品"));
                return;
            }

            // 方式2：修改副手（如果你想用副手）
            // ItemStack stack = player.getOffhandItem();

            // 方式3：修改打开的容器第0个槽位（比如自定义GUI）
            // if (player.containerMenu != null) {
            //     stack = player.containerMenu.getSlot(0).getItem();
            // }

            CompoundTag tag = stack.getOrCreateTag();

            // 存入 NBT（你可以改成你喜欢的 key 名）
            tag.putString("enemy", msg.enemy);
            tag.putString("ally", msg.ally);

            // 可选：告诉客户端物品有变化（同步 NBT）
            player.getInventory().setChanged();

            // 可选：给玩家反馈
            player.sendSystemMessage(Component.literal("§a已更新 IFF 设置"));
            player.sendSystemMessage(Component.literal("enemy: " + msg.enemy));
            player.sendSystemMessage(Component.literal("ally: " + msg.ally));

        });

        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
