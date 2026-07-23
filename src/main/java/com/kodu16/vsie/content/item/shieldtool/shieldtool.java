package com.kodu16.vsie.content.item.shieldtool;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.content.shield.ShieldGeneratorBlockEntity;
import com.kodu16.vsie.utility.ItemStackNbt;
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
import java.util.Locale;

public class shieldtool extends Item {

    public static final String KEY_MAX_SHIELD = "MaxShield";
    public static final String KEY_RADIUS = "ShieldRadius";
    public static final String KEY_COST = "CostPerProjectile";
    public static final String KEY_REGEN = "RegenPerTick";
    public static final String KEY_COOLDOWN = "MaxCooldownTime";
    public static final String KEY_DISTANCE_MAX = "MaxDistance";
    public static final String KEY_DISTANCE_MIN = "MinDistance";

    public shieldtool(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        CompoundTag tag = ItemStackNbt.get(stack);
        if (tag == null) {
            return;
        }

        int max = tag.getInt(KEY_MAX_SHIELD);
        int radius = tag.getInt(KEY_RADIUS);
        int cost = tag.getInt(KEY_COST);
        int regen = tag.getInt(KEY_REGEN);
        int cd = tag.getInt(KEY_COOLDOWN);
        double dmax = tag.getDouble(KEY_DISTANCE_MAX);
        double dmin = tag.getDouble(KEY_DISTANCE_MIN);

        if (max == 0) {
            tooltip.add(Component.translatable("gui.vsie.shield_tool.no_data"));
            tooltip.add(Component.translatable("gui.vsie.shield_tool.refresh_hint"));
            return;
        }

        tooltip.add(Component.translatable("gui.vsie.shield_tool.max_distance.label", formatDistance(dmax)));
        tooltip.add(Component.translatable("gui.vsie.shield_tool.min_distance.label", formatDistance(dmin)));
        tooltip.add(Component.translatable("gui.vsie.shield_tool.max_shield.label", max));
        tooltip.add(Component.translatable("gui.vsie.shield_tool.radius.label", radius));
        tooltip.add(Component.translatable("gui.vsie.shield_tool.cost.label", cost));
        tooltip.add(Component.translatable("gui.vsie.shield_tool.regen.label", regen));
        tooltip.add(Component.translatable("gui.vsie.shield_tool.cooldown.label", cd));
        tooltip.add(Component.translatable("gui.vsie.shield_tool.refresh_hint"));
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
                    return Component.translatable("container.vsie.shield_tool");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
                    return new ShieldToolContainerMenu(windowId, inv, stack);
                }
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        BlockEntity be = level.getBlockEntity(clickedPos);
        if (!(be instanceof ShieldGeneratorBlockEntity shieldGen)) {
            player.displayClientMessage(Component.translatable("gui.vsie.shield_tool.invalid_generator", clickedPos.toShortString()), true);
            return InteractionResult.FAIL;
        }

        BlockEntity seatBe = level.getBlockEntity(shieldGen.linkedcontrolseatpos);
        if (!(seatBe instanceof ControlSeatBlockEntity controlSeat)) {
            player.displayClientMessage(Component.translatable("gui.vsie.shield_tool.no_linked_control_seat"), true);
            return InteractionResult.FAIL;
        }

        ControlSeatServerData data = controlSeat.getControlSeatData();
        controlSeat.updateShield();
        controlSeat.updateShieldEnergyAvalible();

        writeShieldSnapshot(stack, data);

        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.vsie.shield_tool");
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player currentPlayer) {
                return new ShieldToolContainerMenu(windowId, inv, stack);
            }
        });

        return InteractionResult.CONSUME;
    }

    private static void writeShieldSnapshot(ItemStack stack, ControlSeatServerData data) {
        ItemStackNbt.update(stack, tag -> {
            tag.putDouble(KEY_DISTANCE_MAX, data.shieldmax);
            tag.putDouble(KEY_DISTANCE_MIN, data.shieldmin);
            tag.putInt(KEY_MAX_SHIELD, (int) data.totalshield);
            tag.putInt(KEY_RADIUS, (int) data.shieldradius);
            tag.putInt(KEY_COST, (int) data.shieldcostperprojectile);
            tag.putInt(KEY_REGEN, (int) data.shieldregeneratepertick);
            tag.putInt(KEY_COOLDOWN, (int) data.shieldmaxcooldowntime);
        });
    }

    private static String formatDistance(double distance) {
        return String.format(Locale.ROOT, "%.2f", distance);
    }
}
