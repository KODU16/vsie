package com.kodu16.vsie.content.turret.heavyturret;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public abstract class AbstractHeavyTurretBlock extends AbstractTurretBlock {
    protected AbstractHeavyTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!level.isClientSide)
        {
            Logger LOGGER = LogUtils.getLogger();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractHeavyTurretBlockEntity turret) {
                NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.vsie.heavy_turret");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new HeavyTurretContainerMenu(id, inv, turret);
                    }
                }, buf -> buf.writeBlockPos(pos)); // 关键：把 pos 写进去
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
