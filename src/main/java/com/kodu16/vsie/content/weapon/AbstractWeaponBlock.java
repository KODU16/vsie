package com.kodu16.vsie.content.weapon;


import com.kodu16.vsie.content.weapon.server.WeaponContainerMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public abstract class AbstractWeaponBlock extends DirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = IntegerProperty.create("redstone_power", 0, 15);

    protected AbstractWeaponBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction baseDirection = context.getHorizontalDirection();
        Direction placeDirection;
        Player player = context.getPlayer();
        if (player != null) {
            placeDirection = !player.isShiftKeyDown() ? baseDirection : baseDirection.getOpposite();
        } else {
            placeDirection = baseDirection.getOpposite();
        }

        return this.defaultBlockState().setValue(FACING, placeDirection);
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER);
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state);


    @Nullable
    @Override
    public abstract <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type);

    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide()) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractWeaponBlockEntity weaponBlockEntity) {
            LoadedShip Ship = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
            if (Ship != null) {
            }
        }
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (level.isClientSide()) return;

    }

    @Override
    public RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide()) return;
    }


    @Override
    public BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // 功能：1.21.1 的方块交互流程中，允许继续走无物品交互逻辑以打开武器 GUI。
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!level.isClientSide)
        {
            Logger LOGGER = LogUtils.getLogger();
            LOGGER.info("weapon right-clicked at {} by {}, BE = {}", pos, player.getName().getString(),
                    level.getBlockEntity(pos));
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractWeaponBlockEntity weapon && player instanceof ServerPlayer serverPlayer) {
                // 功能：NeoForge 1.21.1 改为通过 ServerPlayer#openMenu 打开服务端菜单。
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.vsie.weapon");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new WeaponContainerMenu(id, inv, weapon);
                    }
                }, buf -> buf.writeBlockPos(pos)); // 功能：同步武器方块坐标给客户端菜单。
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
