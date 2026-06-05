package com.kodu16.vsie.content.thruster;

import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ThrusterContainerMenu extends AbstractContainerMenu {
    private final AbstractThrusterBlockEntity blockEntity;

    public ThrusterContainerMenu(int id, Inventory playerInv, AbstractThrusterBlockEntity blockEntity) {
        super(ModMenuTypes.THRUSTER_MENU.get(), id);
        this.blockEntity = blockEntity;
    }

    public AbstractThrusterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
