package com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret;

import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class HeavyElectroMagnetTurretBlock extends AbstractHeavyTurretBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public HeavyElectroMagnetTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeavyElectroMagnetTurretBlockEntity(vsieBlockEntities.HEAVY_ELECTROMAGNET_TURRET_BLOCK_ENTITY.get(),pos,state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.HEAVY_ELECTROMAGNET_TURRET_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof HeavyElectroMagnetTurretBlockEntity turret) {
                    turret.tick();
                }
            };
        }
        return null;
    }
}
