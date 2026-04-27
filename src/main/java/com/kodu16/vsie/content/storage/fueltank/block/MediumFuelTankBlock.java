package com.kodu16.vsie.content.storage.fueltank.block;

import com.mojang.serialization.MapCodec;
import com.kodu16.vsie.content.storage.fueltank.AbstractFuelTankBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MediumFuelTankBlock extends AbstractFuelTankBlock {

    // 功能：为 NeoForge 1.21.1 提供该方块的序列化 Codec。
    public static final MapCodec<MediumFuelTankBlock> CODEC = simpleCodec(MediumFuelTankBlock::new);
    public MediumFuelTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MediumFuelTankBlock> codec() {
        // 功能：返回方块 Codec，确保方块状态可被数据驱动系统正确反序列化。
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MediumFuelTankBlockEntity(vsieBlockEntities.MEDIUM_FUELTANK_BLOCK_ENTITY.get(), pos,state);
    }
}
