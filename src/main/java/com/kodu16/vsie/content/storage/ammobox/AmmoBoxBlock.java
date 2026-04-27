package com.kodu16.vsie.content.storage.ammobox;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;


public class AmmoBoxBlock extends Block implements EntityBlock {
    public AmmoBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AmmoBoxBlockEntity(vsieBlockEntities.AMMO_BOX_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState p_152498_) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 功能：允许在 1.21.1 中从“物品交互阶段”回落到方块 GUI 打开逻辑。
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AmmoBoxBlockEntity ammobox && player instanceof ServerPlayer serverPlayer) {
                // 功能：将弹药箱菜单打开逻辑迁移到 NeoForge 1.21.1 的 openMenu API。
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.vsie.ammobox");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new AmmoBoxContainerMenu(id, inv, ammobox);
                    }
                }, buf -> buf.writeBlockPos(pos)); // 功能：同步弹药箱位置给客户端构建菜单。
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AmmoBoxBlockEntity chest) {
                Containers.dropContents(level, pos, (Container) chest.getInventory());  // 破坏时掉落物品
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
