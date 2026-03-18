package com.kodu16.vsie.content.item.warpdatachip;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class warp_data_chip extends Item {

    // 功能：把 warp data chip 内保存的一组跃迁坐标、维度与名称整理成统一结构，供 UI/网络逻辑复用。
    public record StoredWarpData(BlockPos pos, String dimensionId, String displayName) {
    }

    // 功能：定义曲速数据芯片在 NBT 中存储坐标与维度时使用的键名，避免硬编码字符串散落。
    private static final String KEY_WARP_DATA = "WarpData";
    private static final String KEY_POS_X = "PosX";
    private static final String KEY_POS_Y = "PosY";
    private static final String KEY_POS_Z = "PosZ";
    private static final String KEY_DIMENSION = "Dimension";

    public warp_data_chip(Properties pProperties) {
        super(pProperties);
    }

    // 功能：判断一个物品堆内是否已经写入可用的跃迁坐标，供控制椅选单过滤空芯片。
    public static boolean hasStoredWarpData(ItemStack stack) {
        return readStoredWarpData(stack) != null;
    }

    // 功能：从 warp data chip 的 NBT 中解析目标坐标、维度和自定义名称，供 HUD/选单/服务端统一读取。
    public static StoredWarpData readStoredWarpData(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY_WARP_DATA, CompoundTag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag warpDataTag = tag.getCompound(KEY_WARP_DATA);
        BlockPos recordedPos = new BlockPos(
                warpDataTag.getInt(KEY_POS_X),
                warpDataTag.getInt(KEY_POS_Y),
                warpDataTag.getInt(KEY_POS_Z)
        );
        String dimensionId = warpDataTag.getString(KEY_DIMENSION);
        return new StoredWarpData(recordedPos, dimensionId, stack.getHoverName().getString());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 功能：玩家右键时，将当前所在方块坐标与维度写入物品 NBT，供后续读取与展示。
        if (!level.isClientSide) {
            BlockPos currentPos = player.blockPosition();
            String dimensionId = level.dimension().location().toString();

            CompoundTag warpDataTag = stack.getOrCreateTagElement(KEY_WARP_DATA);
            warpDataTag.putInt(KEY_POS_X, currentPos.getX());
            warpDataTag.putInt(KEY_POS_Y, currentPos.getY());
            warpDataTag.putInt(KEY_POS_Z, currentPos.getZ());
            warpDataTag.putString(KEY_DIMENSION, dimensionId);

            player.displayClientMessage(Component.literal("§b已记录当前位置: " + currentPos + " @ " + dimensionId), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        StoredWarpData storedWarpData = readStoredWarpData(stack);
        if (storedWarpData == null) {
            // 功能：当芯片尚未写入坐标时，在 tooltip 中提示玩家使用方式。
            tooltip.add(Component.literal("§7右键记录当前位置与维度"));
            return;
        }

        // 功能：在 tooltip 中展示已记录的目标坐标与维度，方便玩家确认芯片内容。
        tooltip.add(Component.literal("§b记录坐标: " + storedWarpData.pos()));
        tooltip.add(Component.literal("§b记录维度: " + storedWarpData.dimensionId()));
    }
}
