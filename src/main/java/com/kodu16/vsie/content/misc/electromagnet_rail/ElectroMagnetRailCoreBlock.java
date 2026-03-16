package com.kodu16.vsie.content.misc.electromagnet_rail;


import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ElectroMagnetRailCoreBlock extends DirectionalBlock implements EntityBlock {
    public ElectroMagnetRailCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ElectroMagnetRailCoreBlockEntity(vsieBlockEntities.ELECTRO_MAGNET_RAIL_CORE_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.ELECTRO_MAGNET_RAIL_CORE_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof ElectroMagnetRailCoreBlockEntity core) {
                    core.tick();
                }
            };
        }
        return null;
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction baseDirection = context.getHorizontalDirection();
        Direction placeDirection;
        Player player = context.getPlayer();
        if (player != null) {
            placeDirection = !player.isShiftKeyDown() ? baseDirection : baseDirection.getOpposite();
        } else {
            placeDirection = baseDirection.getOpposite();
        }

        return this.defaultBlockState().setValue(FACING, placeDirection);
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                          @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        // 右键方块时在服务端打开 GUI，客户端直接返回 SUCCESS 保证交互手感。
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ElectroMagnetRailCoreBlockEntity coreBlockEntity && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public @Nonnull Component getDisplayName() {
                    return Component.translatable("container.vsie.electro_magnet_rail_core");
                }

                @Override
                public @Nullable AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player menuPlayer) {
                    return new ElectroMagnetRailCoreContainerMenu(id, inventory, coreBlockEntity);
                }
            }, pos);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean hasAnalogOutputSignal(@Nonnull BlockState state) {
        // 让比较器可读取库存量（可选的红石反馈功能）。
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ElectroMagnetRailCoreBlockEntity coreBlockEntity) {
            int count = coreBlockEntity.getStoredRailCount();
            return Math.min(15, (int) Math.ceil(count / 16.0));
        }
        return 0;
    }
}
