package com.kodu16.vsie.content.storage.fueltank;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractFuelTankBlock extends DirectionalBlock implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public AbstractFuelTankBlock(Properties properties) {
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
        builder.add(FACING);
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    public RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        // 功能：1.21.1 中“带物品右键方块”交互入口，燃料罐桶交互逻辑迁移到这里。
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        // 如果拿着扳手（Create常用）或其他特殊物品，交给Create逻辑或打开GUI等
        if (held.getItem() instanceof WrenchItem) {
            // 你原来的扳手逻辑...
            return ItemInteractionResult.SUCCESS;
        }

        // 其他特殊交互...

        // 重要：让 FluidUtil 处理桶交互
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IFluidHandler tank) {
            // 这个方法会自动处理：
            //   - 空桶 → 从罐子抽液体装桶
            //   - 满桶 → 把桶里的液体灌进罐子
            //   - 带NBT的桶、其他流体容器等也支持
            if (FluidUtil.interactWithFluidHandler(player, hand, tank)) {
                return ItemInteractionResult.SUCCESS;
            }
        }

        // 如果上面没处理（比如没拿桶），可以打开GUI或其他交互
        // player.openMenu(...) 或 return InteractionResult.PASS;

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;   // 让其他逻辑继续（如果有）
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // 功能：空手交互暂不处理，返回 PASS 交给其他系统（例如事件）继续判定。
        return InteractionResult.PASS;
    }

}
