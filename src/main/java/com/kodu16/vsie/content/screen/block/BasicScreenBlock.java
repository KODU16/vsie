package com.kodu16.vsie.content.screen.block;

import com.kodu16.vsie.content.screen.AbstractScreenBlock;
import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.thruster.block.BasicThrusterBlockEntity;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.kodu16.vsie.registries.vsieShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicScreenBlock extends AbstractScreenBlock {

    public BasicScreenBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BasicScreenBlockEntity(vsieBlockEntities.BASIC_SCREEN_BLOCK_ENTITY.get(),pos,state){
        };
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @javax.annotation.Nullable CollisionContext pContext) {
        if (pState == null) {
            return vsieShapes.BASIC_SCREEN.get(Direction.NORTH);
        }
        Direction direction = pState.getValue(FACING);
        if (direction == Direction.UP || direction == Direction.DOWN) direction = direction.getOpposite();
        return vsieShapes.BASIC_SCREEN.get(direction);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.BASIC_SCREEN_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof BasicScreenBlockEntity screen) {
                    screen.tick();
                }
            };
        }
        return null;
    }
}
