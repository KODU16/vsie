package com.kodu16.vsie.content.thruster.block.basicthruster;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BasicThrusterBlock extends AbstractThrusterBlock {
    public BasicThrusterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new BasicThrusterBlockEntity(vsieBlockEntities.BASIC_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.BASIC_THRUSTER_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof BasicThrusterBlockEntity thruster) {
                    thruster.tick();
                }
            };
        }
        return null;
    }


    /*@Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide()) return;
        doInputCheck(level, state, pos);
    }

    private void doInputCheck(Level level, BlockState state, BlockPos pos) {
        int newRedstonePower = level.getBestNeighborSignal(pos);
        //int oldRedstonePower = state.getValue(POWER);
        //if (newRedstonePower == oldRedstonePower) return;

        BlockState newState = state.setValue(POWER, newRedstonePower);
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractControlSeatBlockEntity controlseatBlockEntity) {
            //thrusterBlockEntity.calculateObstruction(level, pos, state.getValue(FACING));
            controlseatBlockEntity.updateThrustAndTorque(newState);
            controlseatBlockEntity.setChanged();
        }
    }*/
}
