package com.kodu16.vsie.content.misc.enemy_autocannon;

import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class EnemyAutocannonBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<EnemyAutocannonBlock> CODEC = simpleCodec(EnemyAutocannonBlock::new);

    public EnemyAutocannonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (vsieBlockEntities.ENEMY_AUTOCANNON_BLOCK_ENTITY == null) {
            return null;
        }
        return new EnemyAutocannonBlockEntity(vsieBlockEntities.ENEMY_AUTOCANNON_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide || vsieBlockEntities.ENEMY_AUTOCANNON_BLOCK_ENTITY == null
                || type != vsieBlockEntities.ENEMY_AUTOCANNON_BLOCK_ENTITY.get()) {
            return null;
        }
        // Function: the block entity owns shot spacing after the enemy core starts a burst.
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof EnemyAutocannonBlockEntity autocannon) {
                autocannon.serverTick();
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof EnemyAutocannonBlockEntity autocannon) {
            serverPlayer.openMenu(autocannon, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeDouble(autocannon.getSpreadAngle());
                buffer.writeVarInt(autocannon.getShotIntervalTicks());
                buffer.writeVarInt(autocannon.getBurstShots());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
