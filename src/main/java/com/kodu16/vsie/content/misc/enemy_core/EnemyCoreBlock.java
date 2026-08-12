package com.kodu16.vsie.content.misc.enemy_core;

import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class EnemyCoreBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<EnemyCoreBlock> CODEC = simpleCodec(EnemyCoreBlock::new);

    public EnemyCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Function: the lectern front marks the autonomous ship's forward reference.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnemyCoreBlockEntity(vsieBlockEntities.ENEMY_CORE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide || type != vsieBlockEntities.ENEMY_CORE_BLOCK_ENTITY.get()) {
            return null;
        }
        // Function: orbit guidance is authoritative on the server physics thread.
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof EnemyCoreBlockEntity controller) {
                controller.serverTick();
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof EnemyCoreBlockEntity controller) {
            // Function: include current settings in the menu payload so the client never depends on stale block-entity data.
            serverPlayer.openMenu(controller, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeUtf(controller.getEnemyPattern(), EnemyCoreBlockEntity.MAX_PATTERN_LENGTH);
                buffer.writeUtf(controller.getAllyPattern(), EnemyCoreBlockEntity.MAX_PATTERN_LENGTH);
                buffer.writeDouble(controller.getOrbitRadius());
                buffer.writeDouble(controller.getOrbitSpeed());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
