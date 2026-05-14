package com.kodu16.vsie.content.weapon.missile_launcher.block;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlock;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VerticleLaunchingSlotBlock extends AbstractWeaponBlock {
    // Function: NeoForge 1.21.1 requires blocks to expose a stable codec for state serialization.
    public static final MapCodec<VerticleLaunchingSlotBlock> CODEC = simpleCodec(VerticleLaunchingSlotBlock::new);

    public VerticleLaunchingSlotBlock(Properties properties) {
        super(properties);
        // Function: vertical launch slots use one fixed render state; missile launch direction still comes from sublevel up.
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(POWER, 0));
    }

    @Override
    protected @NotNull MapCodec<? extends VerticleLaunchingSlotBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        // Function: this launcher slot has no horizontal placement variants, preventing the generic weapon renderer from tilting it.
        return this.defaultBlockState().setValue(FACING, Direction.UP).setValue(POWER, 0);
    }

    @Override
    public BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rot) {
        return state.setValue(FACING, Direction.UP);
    }

    @Override
    public BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirrorIn) {
        return state.setValue(FACING, Direction.UP);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new VerticleLaunchingSlotBlockEntity(vsieBlockEntities.VERTICLE_LAUNCHING_SLOT_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.VERTICLE_LAUNCHING_SLOT_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof VerticleLaunchingSlotBlockEntity slot) {
                    slot.tick();
                }
            };
        }
        return null;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // Function: vertical launch slots are passive endpoints, so right-clicking them never opens a weapon GUI.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        // Function: empty-hand interaction is intentionally inert for linked launch slots.
        return InteractionResult.PASS;
    }
}
