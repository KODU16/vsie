package com.kodu16.vsie.content.weapon.missile_launcher.block;

import com.mojang.serialization.MapCodec;

import com.kodu16.vsie.content.weapon.missile_launcher.AbstractMissileLauncherBlock;

import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BasicMissileLauncherBlock extends AbstractMissileLauncherBlock {
    // 功能：为 NeoForge 1.21.1 的方块序列化系统提供当前方向方块的 Codec。
    public static final MapCodec<BasicMissileLauncherBlock> CODEC = simpleCodec(BasicMissileLauncherBlock::new);

    public BasicMissileLauncherBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new BasicMissileLauncherBlockEntity(vsieBlockEntities.BASIC_MISSILE_LAUNCHER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.BASIC_MISSILE_LAUNCHER_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof BasicMissileLauncherBlockEntity weapon) {
                    weapon.tick();
                }
            };
        }
        return null;
    }

    // 功能：返回当前方向方块的 Codec，供注册表和数据驱动系统反序列化使用。
    @Override
    protected MapCodec<? extends BasicMissileLauncherBlock> codec() {
        return CODEC;
    }

}
