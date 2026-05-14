package com.kodu16.vsie.network.controlseat.C2S;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.item.warpdatachip.warp_data_chip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ControlSeatWarpTargetC2SPacket implements CustomPacketPayload {
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

    public static void handle(ControlSeatWarpTargetC2SPacket pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
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
                sender.sendSystemMessage(Component.literal("\u9009\u4e2d\u7684 warp data chip \u5c1a\u672a\u8bb0\u5f55\u5750\u6807"));
                return;
            }

            ControlSeatServerData serverData = controlSeat.getServerData();
            serverData.warpTargetPos = storedWarpData.pos();
            serverData.warpTargetDimension = storedWarpData.dimensionId();
            serverData.warpTargetName = stack.getHoverName().getString();
            // Function: keep warp launch math anchored to the selected control seat even before the next block tick refreshes cached data.
            serverData.controlSeatPos = pkt.controlSeatPos;
            serverData.startWarpPreparation();

            controlSeat.setChanged();
            controlSeat.sendData();
            sender.sendSystemMessage(Component.literal("\u5df2\u5c06\u4e0b\u4e00\u6b21\u8dc3\u8fc1\u76ee\u6807\u8bbe\u4e3a " + serverData.warpTargetName + " -> " + serverData.warpTargetDimension + " " + serverData.warpTargetPos + "\uff0c\u63a7\u5236\u6905\u8fdb\u5165 warp \u51c6\u5907\u72b6\u6001"));
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
