package com.kodu16.vsie.content.aeroie_custom;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Single-block custom thruster anchor using AeroIE's shared thruster control lifecycle. */
public final class CustomThrusterBlock extends AbstractThrusterBlock {
    public static final MapCodec<CustomThrusterBlock> CODEC = simpleCodec(CustomThrusterBlock::new);

    public CustomThrusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CustomThrusterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new CustomThrusterBlockEntity(vsieBlockEntities.CUSTOM_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
                                                                  @Nonnull BlockEntityType<T> type) {
        if (type != vsieBlockEntities.CUSTOM_THRUSTER_BLOCK_ENTITY.get()) {
            return null;
        }
        return (world, pos, currentState, blockEntity) -> {
            if (blockEntity instanceof CustomThrusterBlockEntity thruster) {
                thruster.tick();
            }
        };
    }
}
