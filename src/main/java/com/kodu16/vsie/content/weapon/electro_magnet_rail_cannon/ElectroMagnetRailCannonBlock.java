package com.kodu16.vsie.content.weapon.electro_magnet_rail_cannon;

import com.mojang.serialization.MapCodec;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlock;
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

public class ElectroMagnetRailCannonBlock extends AbstractWeaponBlock {
    // 功能：为 NeoForge 1.21.1 的方块序列化系统提供当前方向方块的 Codec。
    public static final MapCodec<ElectroMagnetRailCannonBlock> CODEC = simpleCodec(ElectroMagnetRailCannonBlock::new);

    public ElectroMagnetRailCannonBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        // 功能：创建电磁导轨炮对应的方块实体。
        return new ElectroMagnetRailCannonBlockEntity(vsieBlockEntities.ELECTRO_MAGNET_RAIL_CANNON_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.ELECTRO_MAGNET_RAIL_CANNON_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof ElectroMagnetRailCannonBlockEntity weapon) {
                    // 功能：每 tick 执行武器逻辑（含冷却与开火判定）。
                    weapon.tick();
                }
            };
        }
        return null;
    }

    // 功能：返回当前方向方块的 Codec，供注册表和数据驱动系统反序列化使用。
    @Override
    protected MapCodec<? extends ElectroMagnetRailCannonBlock> codec() {
        return CODEC;
    }

}
