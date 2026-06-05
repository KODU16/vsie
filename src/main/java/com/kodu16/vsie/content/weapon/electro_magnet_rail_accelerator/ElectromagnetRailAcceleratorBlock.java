package com.kodu16.vsie.content.weapon.electro_magnet_rail_accelerator;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ElectromagnetRailAcceleratorBlock extends AbstractWeaponBlock {
    // Function: NeoForge 1.21.1 block registration requires an explicit codec for this block type.
    public static final MapCodec<ElectromagnetRailAcceleratorBlock> CODEC = simpleCodec(ElectromagnetRailAcceleratorBlock::new);

    public ElectromagnetRailAcceleratorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ElectromagnetRailAcceleratorBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ElectromagnetRailAcceleratorBlockEntity(
                vsieBlockEntities.ELECTRO_MAGNET_RAIL_ACCELERATOR_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.ELECTRO_MAGNET_RAIL_ACCELERATOR_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof ElectromagnetRailAcceleratorBlockEntity weapon) {
                    // Function: keep continuous force application on the server in the same tick loop as other weapons.
                    weapon.tick();
                }
            };
        }
        return null;
    }
}
