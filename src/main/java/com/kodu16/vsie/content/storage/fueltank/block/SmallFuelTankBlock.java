package com.kodu16.vsie.content.storage.fueltank.block;

import com.mojang.serialization.MapCodec;

import com.kodu16.vsie.content.storage.fueltank.AbstractFuelTankBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SmallFuelTankBlock extends AbstractFuelTankBlock {
    // 功能：为 NeoForge 1.21.1 的方块序列化系统提供当前方向方块的 Codec。
    public static final MapCodec<SmallFuelTankBlock> CODEC = simpleCodec(SmallFuelTankBlock::new);

    public SmallFuelTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmallFuelTankBlockEntity(vsieBlockEntities.SMALL_FUELTANK_BLOCK_ENTITY.get(), pos,state);
    }

    // 功能：返回当前方向方块的 Codec，供注册表和数据驱动系统反序列化使用。
    @Override
    protected MapCodec<? extends SmallFuelTankBlock> codec() {
        return CODEC;
    }

}
