package com.kodu16.vsie.content.controlseat.block;

import com.kodu16.vsie.registries.vsieBlockEntities;
import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.server.level.ServerPlayer;
import com.kodu16.vsie.content.controlseat.gui.ControlSeatWarpContainerMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class ControlSeatBlock extends AbstractControlSeatBlock {
    public ControlSeatBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ControlSeatBlockEntity(vsieBlockEntities.CONTROL_SEAT_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.CONTROL_SEAT_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof ControlSeatBlockEntity controlSeat) {
                    controlSeat.clientTick();
                    controlSeat.tick();
                }
            };
        }
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS; // Early exit for client-side, as no further action is needed here
        }

        ControlSeatBlockEntity blockEntity = (ControlSeatBlockEntity) level.getBlockEntity(pos);

        if (player.isSecondaryUseActive()) {
            // 功能：Shift+右键时打开控制椅专用的 warp data chip 仓储 GUI，而不是只显示提示文本。
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.vsie.control_seat_warp");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new ControlSeatWarpContainerMenu(id, inv, blockEntity);
                }
            }, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }

        // Ensure the correct player sits and can interact with the control seat
        if (blockEntity.sit(player, false)) {
            return InteractionResult.CONSUME; // Return CONSUME to indicate successful interaction
        } else {
            return InteractionResult.PASS; // Return PASS if interaction was not successful
        }
    }


}
