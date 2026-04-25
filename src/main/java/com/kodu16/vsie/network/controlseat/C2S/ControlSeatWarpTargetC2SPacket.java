package com.kodu16.vsie.network.controlseat.C2S;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.item.warpdatachip.warp_data_chip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ControlSeatWarpTargetC2SPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<ControlSeatWarpTargetC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_c2s_controlseatwarptargetc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatWarpTargetC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ControlSeatWarpTargetC2SPacket::encode, ControlSeatWarpTargetC2SPacket::decode);

    public final BlockPos controlSeatPos;
    public final int slot;

    public ControlSeatWarpTargetC2SPacket(BlockPos controlSeatPos, int slot) {
        this.controlSeatPos = controlSeatPos;
        this.slot = slot;
    }

    public static void encode(ControlSeatWarpTargetC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.controlSeatPos);
        buf.writeVarInt(pkt.slot);
    }

    public static ControlSeatWarpTargetC2SPacket decode(FriendlyByteBuf buf) {
        return new ControlSeatWarpTargetC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版 Supplier<NetworkEvent.Context> 逻辑。
    public static void handle(ControlSeatWarpTargetC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(ControlSeatWarpTargetC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }
            ServerLevel level = sender.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(pkt.controlSeatPos);
            if (!(blockEntity instanceof ControlSeatBlockEntity controlSeat)) {
                sender.sendSystemMessage(Component.literal("Invalid control seat at " + pkt.controlSeatPos));
                return;
            }
            if (pkt.slot < 0 || pkt.slot >= controlSeat.getWarpChipInventory().getSlots()) {
                return;
            }
            ItemStack stack = controlSeat.getWarpChipInventory().getStackInSlot(pkt.slot);
            warp_data_chip.StoredWarpData storedWarpData = warp_data_chip.readStoredWarpData(stack);
            if (storedWarpData == null) {
                sender.sendSystemMessage(Component.literal("选中的 warp data chip 尚未记录坐标"));
                return;
            }
            ControlSeatServerData serverData = controlSeat.getServerData();
            // 功能：把玩家选中的芯片目标写入 control seat，并立刻切入 warp 准备状态以开始自动对准。
            serverData.warpTargetPos = storedWarpData.pos();
            serverData.warpTargetDimension = storedWarpData.dimensionId();
            serverData.warpTargetName = stack.getHoverName().getString();
            serverData.startWarpPreparation();
            controlSeat.setChanged();
            controlSeat.sendData();
            sender.sendSystemMessage(Component.literal("已将下一次跃迁目标设为: " + serverData.warpTargetName + " -> " + serverData.warpTargetDimension + " " + serverData.warpTargetPos + "，控制椅进入 warp 准备状态"));
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
