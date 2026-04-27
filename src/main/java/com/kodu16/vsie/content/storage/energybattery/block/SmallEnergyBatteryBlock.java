package com.kodu16.vsie.content.storage.energybattery.block;

import com.mojang.serialization.MapCodec;

import com.kodu16.vsie.content.storage.energybattery.AbstractEnergyBatteryBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class SmallEnergyBatteryBlock extends AbstractEnergyBatteryBlock {
    // 功能：为 NeoForge 1.21.1 的方块序列化系统提供当前方向方块的 Codec。
    public static final MapCodec<SmallEnergyBatteryBlock> CODEC = simpleCodec(SmallEnergyBatteryBlock::new);

    public SmallEnergyBatteryBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmallEnergyBatteryBlockEntity(vsieBlockEntities.SMALL_ENERGY_BATTERY_BLOCK_ENTITY.get(), pos,state);
    }

    // 功能：返回当前方向方块的 Codec，供注册表和数据驱动系统反序列化使用。
    @Override
    protected MapCodec<? extends SmallEnergyBatteryBlock> codec() {
        return CODEC;
    }

}
