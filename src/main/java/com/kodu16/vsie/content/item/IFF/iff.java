package com.kodu16.vsie.content.item.IFF;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.utility.ItemStackNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class iff extends Item {

    private static final String KEY_ENEMY = "enemy";
    private static final String KEY_ALLY = "ally";

    public iff(Properties pProperties) {
        super(pProperties);
    }

    public static String getEnemy(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        return tag == null ? "" : tag.getString(KEY_ENEMY);
    }

    public static String getAlly(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        return tag == null ? "" : tag.getString(KEY_ALLY);
    }

    public static boolean hasEnemy(ItemStack stack) {
        return !getEnemy(stack).isEmpty();
    }

    public static boolean hasAlly(ItemStack stack) {
        return !getAlly(stack).isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        String enemy = getEnemy(stack);
        String ally = getAlly(stack);

        if (!enemy.isEmpty()) {
            tooltip.add(Component.translatable("item.vsie.iff.tooltip.enemy", enemy).withStyle(ChatFormatting.RED));
        }
        if (!ally.isEmpty()) {
            tooltip.add(Component.translatable("item.vsie.iff.tooltip.ally", ally).withStyle(ChatFormatting.GREEN));
        }
        if (enemy.isEmpty() && ally.isEmpty()) {
            tooltip.add(Component.translatable("item.vsie.iff.tooltip.unset").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.vsie.iff");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
                    return new IFFContainerMenu(windowId, inv, player.getMainHandItem());
                }
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof AbstractControlSeatBlockEntity controlSeat) {
            CompoundTag tag = ItemStackNbt.get(stack);
            boolean hasChange = false;

            if (tag != null && tag.contains(KEY_ENEMY)) {
                controlSeat.setEnemy(tag.getString(KEY_ENEMY));
                hasChange = true;
            }
            if (tag != null && tag.contains(KEY_ALLY)) {
                controlSeat.setAlly(tag.getString(KEY_ALLY));
                hasChange = true;
            }

            if (hasChange) {
                player.displayClientMessage(Component.translatable("item.vsie.iff.applied", getEnemy(stack), getAlly(stack)), true);
            } else {
                player.displayClientMessage(Component.translatable("item.vsie.iff.no_data"), true);
            }
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.translatable("item.vsie.iff.target_not_control_seat"), true);
        return InteractionResult.CONSUME;
    }
}
