package com.kodu16.vsie.content.screen.server;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ScreenContainerMenu extends AbstractContainerMenu {
    //服务端容器，和客户端那边的screen对接
    //暂时不支持塞升级芯片，将来需要的话就加入物品栏容器
    private final AbstractScreenBlockEntity blockEntity;

    public ScreenContainerMenu(int id, Inventory playerInv, AbstractScreenBlockEntity be) {
        super(ModMenuTypes.SCREEN_MENU.get(), id);
        this.blockEntity = be;

        // 这里可以加玩家背包槽位等，一般至少加一下
        //addPlayerInventory(playerInv);
    }

    // 标准添加玩家背包代码
    //private void addPlayerInventory(Inventory playerInv) { ... }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved();
    }

    public AbstractScreenBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 简单粗暴版
        // 或者上面完整的标准实现
    }
}
