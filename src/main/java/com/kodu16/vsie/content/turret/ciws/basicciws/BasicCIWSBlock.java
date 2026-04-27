package com.kodu16.vsie.content.turret.ciws.basicciws;

import com.mojang.serialization.MapCodec;
import com.kodu16.vsie.content.turret.ciws.AbstractCIWSBlock;
import com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret.HeavyElectroMagnetTurretBlockEntity;
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

public class BasicCIWSBlock extends AbstractCIWSBlock {

    // 功能：为 NeoForge 1.21.1 提供该方块的序列化 Codec。
    public static final MapCodec<BasicCIWSBlock> CODEC = simpleCodec(BasicCIWSBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public BasicCIWSBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BasicCIWSBlock> codec() {
        // 功能：返回方块 Codec，确保方块状态可被数据驱动系统正确反序列化。
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BasicCIWSBlockEntity(vsieBlockEntities.BASIC_CIWS_BLOCK_ENTITY.get(),pos,state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.BASIC_CIWS_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof BasicCIWSBlockEntity turret) {
                    turret.tick();
                }
            };
        }
        return null;
    }
}
