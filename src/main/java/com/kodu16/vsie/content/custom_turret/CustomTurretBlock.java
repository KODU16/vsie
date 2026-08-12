package com.kodu16.vsie.content.custom_turret;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/** Single-block custom turret anchor with AeroIE's common menu, binding and tick lifecycle. */
public final class CustomTurretBlock extends AbstractTurretBlock {
    public static final MapCodec<CustomTurretBlock> CODEC = simpleCodec(CustomTurretBlock::new);

    public CustomTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CustomTurretBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new CustomTurretBlockEntity(vsieBlockEntities.CUSTOM_TURRET_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
                                                                  @Nonnull BlockEntityType<T> type) {
        if (type != vsieBlockEntities.CUSTOM_TURRET_BLOCK_ENTITY.get()) {
            return null;
        }
        // Function: route both normal-world and Sable-restored ticks through the common turret loop.
        return (world, pos, currentState, blockEntity) -> {
            if (blockEntity instanceof CustomTurretBlockEntity turret) {
                turret.tick();
            }
        };
    }
}
