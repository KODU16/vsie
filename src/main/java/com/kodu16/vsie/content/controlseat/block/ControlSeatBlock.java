package com.kodu16.vsie.content.controlseat.block;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlock;
import com.kodu16.vsie.content.controlseat.gui.ControlSeatWarpContainerMenu;
import com.kodu16.vsie.content.item.IFF.iff;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ControlSeatBlock extends AbstractControlSeatBlock {

    public static final MapCodec<ControlSeatBlock> CODEC = simpleCodec(ControlSeatBlock::new);

    public ControlSeatBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ControlSeatBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ControlSeatBlockEntity(vsieBlockEntities.CONTROL_SEAT_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type != vsieBlockEntities.CONTROL_SEAT_BLOCK_ENTITY.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof ControlSeatBlockEntity controlSeat) {
                    controlSeat.clientTick();
                }
            };
        }
        return (world, pos, state1, blockEntity) -> {
            if (blockEntity instanceof ControlSeatBlockEntity controlSeat) {
                controlSeat.tick();
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof iff) {
            // Function: let the IFF tool own control-seat clicks so the seat does not also mount the player or open the warp GUI.
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ControlSeatBlockEntity blockEntity = (ControlSeatBlockEntity) level.getBlockEntity(pos);
        if (isHoldingIff(player)) {
            // Function: if control-seat fallback interaction still runs during an IFF click, suppress the seat action and keep the item authoritative.
            return InteractionResult.CONSUME;
        }

        if (player.isSecondaryUseActive()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.vsie.control_seat_warp");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new ControlSeatWarpContainerMenu(id, inv, blockEntity);
                    }
                }, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }

        if (blockEntity.sit(player, false)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static boolean isHoldingIff(Player player) {
        // Function: check both hands so IFF clicks never fall through to control-seat behavior.
        return player.getMainHandItem().getItem() instanceof iff || player.getOffhandItem().getItem() instanceof iff;
    }
}
