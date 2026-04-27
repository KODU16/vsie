package com.kodu16.vsie.content.thruster.block;

import com.mojang.serialization.MapCodec;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.kodu16.vsie.registries.vsieShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BasicThrusterBlock extends AbstractThrusterBlock {
    // 功能：为 NeoForge 1.21.1 的方块序列化系统提供当前方向方块的 Codec。
    public static final MapCodec<BasicThrusterBlock> CODEC = simpleCodec(BasicThrusterBlock::new);

    public BasicThrusterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new BasicThrusterBlockEntity(vsieBlockEntities.BASIC_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.BASIC_THRUSTER_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof BasicThrusterBlockEntity thruster) {
                    thruster.tick();
                }
            };
        }
        return null;
    }

    // 功能：返回当前方向方块的 Codec，供注册表和数据驱动系统反序列化使用。
    @Override
    protected MapCodec<? extends BasicThrusterBlock> codec() {
        return CODEC;
    }

}
