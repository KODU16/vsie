package com.kodu16.vsie.content.weapon.server;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class WeaponContainerMenu extends AbstractContainerMenu {
    //暂时不支持塞升级芯片，将来需要的话就加入物品栏容器
    private final AbstractWeaponBlockEntity blockEntity;

    public WeaponContainerMenu(int id, Inventory playerInv, AbstractWeaponBlockEntity be) {
        super(ModMenuTypes.WEAPON_MENU.get(), id);
        this.blockEntity = be;

        // 这里可以加玩家背包槽位等，一般至少加一下
        //addPlayerInventory(playerInv);
    }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved();
    }

    public AbstractWeaponBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 简单粗暴版
        // 或者上面完整的标准实现
    }
}
