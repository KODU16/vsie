package com.kodu16.vsie.content.vectorthruster.block;

import com.mojang.serialization.MapCodec;
import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BasicVectorThrusterBlock extends AbstractVectorThrusterBlock {

    // 功能：为 NeoForge 1.21.1 提供该方块的序列化 Codec。
    public static final MapCodec<BasicVectorThrusterBlock> CODEC = simpleCodec(BasicVectorThrusterBlock::new);
    public BasicVectorThrusterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BasicVectorThrusterBlock> codec() {
        // 功能：返回方块 Codec，确保方块状态可被数据驱动系统正确反序列化。
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new BasicVectorThrusterBlockEntity(vsieBlockEntities.BASIC_VECTOR_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.BASIC_VECTOR_THRUSTER_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof BasicVectorThrusterBlockEntity thruster) {
                    thruster.tick();
                }
            };
        }
        return null;
    }
}
