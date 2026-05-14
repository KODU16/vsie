package com.kodu16.vsie.content.item.warpdatachip;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class warp_data_chip extends Item {
    public record StoredWarpData(BlockPos pos, String dimensionId, String displayName) {
    }

    private static final String KEY_WARP_DATA = "WarpData";
    private static final String KEY_POS_X = "PosX";
    private static final String KEY_POS_Y = "PosY";
    private static final String KEY_POS_Z = "PosZ";
    private static final String KEY_DIMENSION = "Dimension";
    private static final String KEY_LEGACY_DIMENSION = "dimension";

    public warp_data_chip(Properties pProperties) {
        super(pProperties);
    }

    public static boolean hasStoredWarpData(ItemStack stack) {
        return readStoredWarpData(stack) != null;
    }

    public static StoredWarpData readStoredWarpData(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        CompoundTag tag = customData.copyTag();
        if (!tag.contains(KEY_WARP_DATA, Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag warpDataTag = tag.getCompound(KEY_WARP_DATA);
        // 功能：优先读取当前字段，同时兼容早期小写字段，避免旧芯片因为键名差异失效。
        String dimensionId = warpDataTag.contains(KEY_DIMENSION, Tag.TAG_STRING)
                ? warpDataTag.getString(KEY_DIMENSION)
                : warpDataTag.getString(KEY_LEGACY_DIMENSION);
        if (dimensionId.isBlank()) {
            return null;
        }

        BlockPos recordedPos = new BlockPos(
                warpDataTag.getInt(KEY_POS_X),
                warpDataTag.getInt(KEY_POS_Y),
                warpDataTag.getInt(KEY_POS_Z)
        );
        return new StoredWarpData(recordedPos, dimensionId, stack.getHoverName().getString());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            BlockPos currentPos = player.blockPosition();
            String dimensionId = level.dimension().location().toString();

            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                CompoundTag warpDataTag = new CompoundTag();
                warpDataTag.putInt(KEY_POS_X, currentPos.getX());
                warpDataTag.putInt(KEY_POS_Y, currentPos.getY());
                warpDataTag.putInt(KEY_POS_Z, currentPos.getZ());
                warpDataTag.putString(KEY_DIMENSION, dimensionId);
                tag.put(KEY_WARP_DATA, warpDataTag);
            });

            player.displayClientMessage(Component.literal("已记录当前位置: " + currentPos + " @ " + dimensionId).withStyle(ChatFormatting.AQUA), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        StoredWarpData storedWarpData = readStoredWarpData(stack);
        if (storedWarpData == null) {
            tooltip.add(Component.literal("右键记录当前位置与维度").withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.literal("记录坐标: " + storedWarpData.pos()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("记录维度: " + storedWarpData.dimensionId()).withStyle(ChatFormatting.AQUA));
    }
}
