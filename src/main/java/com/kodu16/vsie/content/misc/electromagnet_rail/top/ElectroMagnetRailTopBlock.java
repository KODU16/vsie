package com.kodu16.vsie.content.misc.electromagnet_rail.top;

import com.mojang.serialization.MapCodec;

import com.kodu16.vsie.content.misc.electromagnet_rail.top.ElectroMagnetRailTopBlockEntity;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ElectroMagnetRailTopBlock extends DirectionalBlock implements EntityBlock {
    // 功能：为 NeoForge 1.21.1 的方块序列化系统提供当前方向方块的 Codec。
    public static final MapCodec<ElectroMagnetRailTopBlock> CODEC = simpleCodec(ElectroMagnetRailTopBlock::new);

    public ElectroMagnetRailTopBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    @Override
    public RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ElectroMagnetRailTopBlockEntity(vsieBlockEntities.ELECTRO_MAGNET_RAIL_TOP_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.ELECTRO_MAGNET_RAIL_TOP_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof ElectroMagnetRailTopBlockEntity Top) {
                    Top.tick();
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

    // 功能：返回当前方向方块的 Codec，供注册表和数据驱动系统反序列化使用。
    @Override
    protected MapCodec<? extends ElectroMagnetRailTopBlock> codec() {
        return CODEC;
    }

}
