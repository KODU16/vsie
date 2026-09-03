package com.kodu16.vsie.content.aeroie_custom;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlock;
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

/** Single-block custom weapon anchor using AeroIE's shared weapon menu and binding lifecycle. */
public final class CustomWeaponBlock extends AbstractWeaponBlock {
    public static final MapCodec<CustomWeaponBlock> CODEC = simpleCodec(CustomWeaponBlock::new);

    public CustomWeaponBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CustomWeaponBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new CustomWeaponBlockEntity(vsieBlockEntities.CUSTOM_WEAPON_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
                                                                  @Nonnull BlockEntityType<T> type) {
        if (type != vsieBlockEntities.CUSTOM_WEAPON_BLOCK_ENTITY.get()) {
            return null;
        }
        return (world, pos, currentState, blockEntity) -> {
            if (blockEntity instanceof CustomWeaponBlockEntity weapon) {
                weapon.tick();
            }
        };
    }
}
