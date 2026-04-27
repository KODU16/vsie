package com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret;

import com.mojang.serialization.MapCodec;

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
    // 功能：为 NeoForge 1.21.1 的方块序列化系统提供当前方向方块的 Codec。
    public static final MapCodec<HeavyElectroMagnetTurretBlock> CODEC = simpleCodec(HeavyElectroMagnetTurretBlock::new);

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

    // 功能：返回当前方向方块的 Codec，供注册表和数据驱动系统反序列化使用。
    @Override
    protected MapCodec<? extends HeavyElectroMagnetTurretBlock> codec() {
        return CODEC;
    }

}
