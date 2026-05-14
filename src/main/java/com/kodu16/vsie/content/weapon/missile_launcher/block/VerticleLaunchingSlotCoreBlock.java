package com.kodu16.vsie.content.weapon.missile_launcher.block;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VerticleLaunchingSlotCoreBlock extends AbstractWeaponBlock {
    // Function: the ordinary Java-model core keeps its state serializable on NeoForge 1.21.1.
    public static final MapCodec<VerticleLaunchingSlotCoreBlock> CODEC = simpleCodec(VerticleLaunchingSlotCoreBlock::new);

    public VerticleLaunchingSlotCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends VerticleLaunchingSlotCoreBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new VerticleLaunchingSlotCoreBlockEntity(vsieBlockEntities.VERTICLE_LAUNCHING_SLOT_CORE_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.VERTICLE_LAUNCHING_SLOT_CORE_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof VerticleLaunchingSlotCoreBlockEntity core) {
                    core.tick();
                }
            };
        }
        return null;
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        // Function: keep loaded basic missiles recoverable when the core block is broken.
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof VerticleLaunchingSlotCoreBlockEntity core) {
            core.deactivateLinkedSlots(level);
            core.dropStoredMissiles(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
