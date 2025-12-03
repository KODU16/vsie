package com.kodu16.vsie.content.turret.block;

import com.kodu16.vsie.content.thruster.block.basicthruster.BasicThrusterBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.server.TurretContainerMenu;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.mojang.logging.LogUtils;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.resource.BlockResourceInfo;
import mekanism.common.tile.base.WrenchResult;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MediumLaserTurretBlock extends AbstractTurretBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static VoxelShape SHAPE_NORTH;
    private static VoxelShape SHAPE_EAST;
    private static VoxelShape SHAPE_SOUTH;
    private static VoxelShape SHAPE_WEST;
    private static VoxelShape SHAPE_UP;
    private static VoxelShape SHAPE_DOWN;
    public MediumLaserTurretBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @org.jetbrains.annotations.Nullable LivingEntity placer, @NotNull ItemStack stack){
        super.setPlacedBy(world,pos,state,placer,stack);
        if (stack.hasTag() && stack.getTag().contains("mekData")) {
            CompoundTag nbtData = stack.getTag().getCompound("mekData");
            MediumLaserTurretBlockEntity tileEntity = (MediumLaserTurretBlockEntity) world.getBlockEntity(pos);
            if (tileEntity != null) {
                tileEntity.load(nbtData);
            }
        }

    }
    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    public BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!level.isClientSide)
        {
            Logger LOGGER = LogUtils.getLogger();
            LOGGER.info("Turret right-clicked at {} by {}, BE = {}", pos, player.getName().getString(),
                    level.getBlockEntity(pos));
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractTurretBlockEntity turret) {
                NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.vsie.turret");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new TurretContainerMenu(id, inv, turret);
                    }
                }, buf -> buf.writeBlockPos(pos)); // 关键：把 pos 写进去
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @org.jetbrains.annotations.Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new MediumLaserTurretBlockEntity(vsieBlockEntities.MEDIUM_LASER_TURRET_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == vsieBlockEntities.MEDIUM_LASER_TURRET_BLOCK_ENTITY.get()) {
            return (world, pos, state1, blockEntity) -> {
                if (blockEntity instanceof MediumLaserTurretBlockEntity turret) {
                    turret.tick();
                }
            };
        }
        return null;
    }

/*@Override
public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
    if (level.isClientSide()) return;
    doInputCheck(level, state, pos);
}

private void doInputCheck(Level level, BlockState state, BlockPos pos) {
    int newRedstonePower = level.getBestNeighborSignal(pos);
    //int oldRedstonePower = state.getValue(POWER);
    //if (newRedstonePower == oldRedstonePower) return;

    BlockState newState = state.setValue(POWER, newRedstonePower);
    level.setBlock(pos, newState, Block.UPDATE_ALL);

    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (blockEntity instanceof AbstractControlSeatBlockEntity controlseatBlockEntity) {
        //thrusterBlockEntity.calculateObstruction(level, pos, state.getValue(FACING));
        controlseatBlockEntity.updateThrustAndTorque(newState);
        controlseatBlockEntity.setChanged();
    }
}*/
}

