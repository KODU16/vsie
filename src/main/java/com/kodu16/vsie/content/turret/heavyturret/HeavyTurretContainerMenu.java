package com.kodu16.vsie.content.turret.heavyturret;

import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

// MyContainerMenu.java
public class HeavyTurretContainerMenu extends AbstractContainerMenu {
    //服务端容器，和客户端那边的screen对接
    //暂时不支持塞升级芯片，将来需要的话就加入物品栏容器
    private final AbstractHeavyTurretBlockEntity blockEntity;

    public HeavyTurretContainerMenu(int id, Inventory playerInv, AbstractHeavyTurretBlockEntity be) {
        super(ModMenuTypes.HEAVY_TURRET_MENU.get(), id);
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

    public AbstractHeavyTurretBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 简单粗暴版
        // 或者上面完整的标准实现
    }
}

