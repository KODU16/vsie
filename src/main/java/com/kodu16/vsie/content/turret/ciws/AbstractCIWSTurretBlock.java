package com.kodu16.vsie.content.turret.ciws;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.heavyturret.HeavyTurretContainerMenu;
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

public abstract class AbstractCIWSTurretBlock extends AbstractTurretBlock {
    protected AbstractCIWSTurretBlock(Properties properties) {
        super(properties);
    }
}
