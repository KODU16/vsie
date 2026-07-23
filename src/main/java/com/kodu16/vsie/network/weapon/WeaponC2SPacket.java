package com.kodu16.vsie.network.weapon;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.network.turret.TurretC2SPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class WeaponC2SPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WeaponC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "weapon_weaponc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, WeaponC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(WeaponC2SPacket::encode, WeaponC2SPacket::decode);

    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final int channelchange;
    public WeaponC2SPacket(BlockPos pos, int channelchange) {
        this.pos = pos;
        this.channelchange = channelchange;
    }
    public static void encode(WeaponC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.channelchange);
    }
    public static WeaponC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int channelchange = buf.readVarInt();
        return new WeaponC2SPacket(pos,channelchange);
    }

    public static void handle(WeaponC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public static void handle(WeaponC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            int channelchange = pkt.channelchange;
            BlockEntity BE = level.getBlockEntity(pos);
            if (!(BE instanceof AbstractWeaponBlockEntity weapon)) {
                // Function: system chat packet encoding rejects raw BlockPos translation args.
                sender.sendSystemMessage(Component.translatable("message.vsie.network.invalid_weapon", pos.toShortString()));
                return;
            }
            if (channelchange == 5) {
                // Function: the shared weapon GUI toggles terrain damage independently from fire-channel assignment.
                weapon.toggleBreaksBlocksEnabled();
            } else {
                weapon.modifychannel(channelchange);
                LogUtils.getLogger().warn(String.valueOf(Component.literal("changing weapon channel"+channelchange)));
            }
            weapon.setChanged();
            weapon.getLevel().sendBlockUpdated(     // 鍚戦檮杩戠帺瀹跺悓姝?BE
                    weapon.getBlockPos(),
                    weapon.getBlockState(),
                    weapon.getBlockState(),
                    Block.UPDATE_CLIENTS   // 3
            );
        });
        ctx.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
