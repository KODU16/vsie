package com.kodu16.vsie.content.turret.server;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.foundation.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

// MyContainerMenu.java
public class TurretContainerMenu extends AbstractContainerMenu {
    //服务端容器，和客户端那边的screen对接
    //暂时不支持塞升级芯片，将来需要的话就加入物品栏容器
    private final AbstractTurretBlockEntity blockEntity;

    public TurretContainerMenu(int id, Inventory playerInv, AbstractTurretBlockEntity be) {
        super(ModMenuTypes.TURRET_MENU.get(), id);
        this.blockEntity = be;

        // 这里可以加玩家背包槽位等，一般至少加一下
        //addPlayerInventory(playerInv);
    }

    // 标准添加玩家背包代码
    //private void addPlayerInventory(Inventory playerInv) { ... }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved() && player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64;
    }

    public AbstractTurretBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 简单粗暴版
        // 或者上面完整的标准实现
    }
}

