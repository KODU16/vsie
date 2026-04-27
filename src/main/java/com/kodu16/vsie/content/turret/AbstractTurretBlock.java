package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.content.turret.block.ParticleTurretBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractTurretBlock extends DirectionalBlock implements EntityBlock  {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected AbstractTurretBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @org.jetbrains.annotations.Nullable LivingEntity placer, @NotNull ItemStack stack){
        super.setPlacedBy(world,pos,state,placer,stack);
    }
    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    public BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // 功能：1.21.1 中带物品右键时，继续回落到方块默认交互，保持炮塔 GUI 可打开。
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!level.isClientSide)
        {
            Logger LOGGER = LogUtils.getLogger();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractTurretBlockEntity turret && player instanceof ServerPlayer serverPlayer) {
                // 功能：NeoForge 1.21.1 使用 ServerPlayer#openMenu 取代旧的 NetworkHooks.openScreen。
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.vsie.turret");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new TurretContainerMenu(id, inv, turret);
                    }
                }, buf -> buf.writeBlockPos(pos)); // 功能：将方块坐标写入额外数据，供客户端菜单构造器读取。
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        Direction baseDirection = context.getNearestLookingDirection();
        Direction placeDirection;
        Player player = context.getPlayer();
        if (player != null) {
            placeDirection = !player.isShiftKeyDown() ? baseDirection : baseDirection.getOpposite();
        } else {
            placeDirection = baseDirection;
        }
        return this.defaultBlockState().setValue(FACING, placeDirection);
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        Block block = level.getBlockState(pos).getBlock();
        Logger LOGGER = LogUtils.getLogger();
        LOGGER.warn(String.valueOf(Component.literal("rendershape:"+block.getRenderShape(state))));
        /*if (blockEntity instanceof AbstractThrusterBlockEntity thrusterBlockEntity) {
            if (ship != null) {
                // Initialize thruster data for ValkyRien Skies
                ThrusterData data = thrusterBlockEntity.getThrusterData();
                data.setDirection(VectorConversionsMCKt.toJOMLD(state.getValue(FACING).getNormal()));
                data.setThrust(0);
                ThrusterForceApplier applier = new ThrusterForceApplier(data);
                ship.addApplier(pos, applier);
            }
            // Trigger an initial check for redstone power and obstruction
            doRedstoneCheck(level, state, pos);
        }*/
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        // 功能：当炮塔方块被替换/破坏时，先将粒子炮弹药仓内容物掉落到世界中。
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ParticleTurretBlockEntity particleTurret) {
                particleTurret.dropStoredContainers(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
        if (level.isClientSide()) return;

        //ThrusterForceAttachment ship = ThrusterForceAttachment.get(level, pos);
        //if (ship != null) {
        //    ship.removeApplier((ServerLevel) level, pos);
        //}
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide()) return;
    }
}
