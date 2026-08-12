package com.kodu16.vsie.content.custom_turret;

import com.kodu16.vsie.content.custom_turret.client.CustomTurretSelectorScreen;
import com.kodu16.vsie.registries.vsieBlocks;
import com.kodu16.vsie.utility.ItemStackNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/** Shift-selects a saved definition and places its passive Gecko block on a block face. */
public final class CustomTurretPlacerItem extends Item {
    public static final String DEFINITION_TAG = "CustomTurretDefinition";

    public CustomTurretPlacerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static String getDefinitionJson(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        return tag == null ? "" : tag.getString(DEFINITION_TAG);
    }

    public static void setDefinitionJson(ItemStack stack, String json) {
        CustomTurretDefinition definition = CustomTurretDefinition.fromJson(json);
        CompoundTag tag = ItemStackNbt.getOrCreate(stack);
        tag.putString(DEFINITION_TAG, definition.toJson());
        ItemStackNbt.set(stack, tag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && level.isClientSide) {
            ClientAccess.openSelector(hand);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player interactingPlayer = context.getPlayer();
        if (interactingPlayer != null && interactingPlayer.isShiftKeyDown()) {
            if (level.isClientSide) {
                ClientAccess.openSelector(context.getHand());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        String json = getDefinitionJson(context.getItemInHand());
        final CustomTurretDefinition definition;
        try {
            definition = CustomTurretDefinition.fromJson(json);
        } catch (IllegalArgumentException exception) {
            player.displayClientMessage(Component.translatable("item.vsie.custom_turret_placer.no_selection"), true);
            return InteractionResult.FAIL;
        }

        BlockPos clicked = context.getClickedPos();
        int[] targetCoordinates = CustomTurretAnchor.placementTarget(
                clicked.getX(), clicked.getY(), clicked.getZ(),
                context.getClickedFace().getStepX(), context.getClickedFace().getStepY(),
                context.getClickedFace().getStepZ());
        BlockPos target = new BlockPos(targetCoordinates[0], targetCoordinates[1], targetCoordinates[2]);
        BlockState state = vsieBlocks.CUSTOM_TURRET_BLOCK.getDefaultState();
        if (!player.mayUseItemAt(target, context.getClickedFace(), context.getItemInHand())
                || !level.getBlockState(target).canBeReplaced() || !level.setBlock(target, state, 3)) {
            return InteractionResult.FAIL;
        }
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity instanceof CustomTurretBlockEntity turret) {
            turret.setDefinitionJson(definition.toJson());
        }
        player.displayClientMessage(Component.translatable(
                "item.vsie.custom_turret_placer.placed", definition.name), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        try {
            CustomTurretDefinition definition = CustomTurretDefinition.fromJson(getDefinitionJson(stack));
            tooltip.add(Component.translatable("item.vsie.custom_turret_placer.selected", definition.name)
                    .withStyle(ChatFormatting.AQUA));
        } catch (IllegalArgumentException exception) {
            tooltip.add(Component.translatable("item.vsie.custom_turret_placer.unselected")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientAccess {
        private static void openSelector(InteractionHand hand) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new CustomTurretSelectorScreen(hand));
        }
    }
}
