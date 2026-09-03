package com.kodu16.vsie.content.aeroie_custom.export;

import com.kodu16.vsie.content.aeroie_custom.export.client.RegionObjExportScreen;
import dev.ryanhcode.sable.Sable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/** Selects an inclusive block cuboid and opens the client-side OBJ export screen. */
public final class RegionObjExportToolItem extends Item {
    public RegionObjExportToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                RegionExportSelection.clear(stack);
                player.displayClientMessage(Component.translatable("message.vsie.region_export.cleared"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (Sable.HELPER.getContaining(level, context.getClickedPos()) != null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.vsie.region_export.sublevel_rejected"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        var completeSelection = RegionExportSelection.complete(stack);
        if (completeSelection.isPresent()) {
            if (!completeSelection.get().isIn(level)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.vsie.region_export.dimension_mismatch"), true);
                }
            } else if (level.isClientSide) {
                ClientAccess.open(stack);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            if (RegionExportSelection.first(stack).isEmpty()) {
                RegionExportSelection.setFirst(stack, level, context.getClickedPos());
                player.displayClientMessage(Component.translatable(
                        "message.vsie.region_export.first", context.getClickedPos().toShortString()), true);
            } else if (RegionExportSelection.dimension(stack).filter(level.dimension().location()::equals).isPresent()) {
                RegionExportSelection.setSecond(stack, context.getClickedPos());
                player.displayClientMessage(Component.translatable(
                        "message.vsie.region_export.second", context.getClickedPos().toShortString()), true);
            } else {
                RegionExportSelection.setFirst(stack, level, context.getClickedPos());
                player.displayClientMessage(Component.translatable("message.vsie.region_export.dimension_reset"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                RegionExportSelection.clear(stack);
                player.displayClientMessage(Component.translatable("message.vsie.region_export.cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (RegionExportSelection.subLevelId(stack).isPresent()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.vsie.region_export.sublevel_rejected"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level.isClientSide && RegionExportSelection.complete(stack).filter(selection -> selection.isIn(level)).isPresent()) {
            ClientAccess.open(stack);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.vsie.region_obj_export_tool.hint").withStyle(ChatFormatting.GRAY));
        RegionExportSelection.complete(stack).ifPresentOrElse(
                selection -> tooltip.add(Component.translatable(
                        "item.vsie.region_obj_export_tool.selection",
                        selection.first().toShortString(), selection.second().toShortString(), selection.volume()
                ).withStyle(ChatFormatting.AQUA)),
                () -> RegionExportSelection.first(stack).ifPresent(pos -> tooltip.add(Component.translatable(
                        "item.vsie.region_obj_export_tool.first", pos.toShortString()).withStyle(ChatFormatting.AQUA)))
        );
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientAccess {
        private static void open(ItemStack stack) {
            RegionExportSelection.complete(stack).ifPresent(selection ->
                    net.minecraft.client.Minecraft.getInstance().setScreen(new RegionObjExportScreen(selection)));
        }
    }
}
