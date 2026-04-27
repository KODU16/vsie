package com.kodu16.vsie.content.storage.energybattery.block;

import com.mojang.serialization.MapCodec;

import com.kodu16.vsie.content.storage.energybattery.AbstractEnergyBatteryBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LargeEnergyBatteryBlock extends AbstractEnergyBatteryBlock {
    // 功能：为 NeoForge 1.21.1 的方块序列化系统提供当前方向方块的 Codec。
    public static final MapCodec<LargeEnergyBatteryBlock> CODEC = simpleCodec(LargeEnergyBatteryBlock::new);

    public LargeEnergyBatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeEnergyBatteryBlockEntity(vsieBlockEntities.LARGE_ENERGY_BATTERY_BLOCK_ENTITY.get(), pos,state);
    }

    // 功能：返回当前方向方块的 Codec，供注册表和数据驱动系统反序列化使用。
    @Override
    protected MapCodec<? extends LargeEnergyBatteryBlock> codec() {
        return CODEC;
    }

}
