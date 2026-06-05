package com.kodu16.vsie.content.turret.heavyturret.heavylaserturret;

import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class HeavyLaserTurretBlock extends AbstractHeavyTurretBlock {
    // Function: provide NeoForge's data-driven codec for this heavy laser turret block.
    public static final MapCodec<HeavyLaserTurretBlock> CODEC = simpleCodec(HeavyLaserTurretBlock::new);

    public HeavyLaserTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<HeavyLaserTurretBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeavyLaserTurretBlockEntity(vsieBlockEntities.HEAVY_LASER_TURRET_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.HEAVY_LASER_TURRET_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof HeavyLaserTurretBlockEntity turret) {
                    turret.tick();
                }
            };
        }
        return null;
    }
}
