package com.kodu16.vsie.content.weapon.redstone_relay;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RedstoneRelayBlock extends AbstractWeaponBlock {
    public static final MapCodec<RedstoneRelayBlock> CODEC = simpleCodec(RedstoneRelayBlock::new);

    public RedstoneRelayBlock(Properties properties) {
        super(properties);
        // Function: redstone relay starts silent and only drives power while the linked fire channel is held.
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWER, 0));
    }

    @Override
    public MapCodec<RedstoneRelayBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new RedstoneRelayBlockEntity(vsieBlockEntities.REDSTONE_RELAY_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.REDSTONE_RELAY_BLOCK_ENTITY.get()) {
            return (world, pos, blockState, blockEntity) -> {
                if (blockEntity instanceof RedstoneRelayBlockEntity relay) {
                    relay.tick();
                }
            };
        }
        return null;
    }

    @Override
    protected boolean isSignalSource(@Nonnull BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.BlockGetter level, @Nonnull BlockPos pos, @Nonnull Direction direction) {
        return state.getValue(POWER);
    }

    @Override
    protected int getDirectSignal(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.BlockGetter level, @Nonnull BlockPos pos, @Nonnull Direction direction) {
        return state.getValue(POWER);
    }
}
