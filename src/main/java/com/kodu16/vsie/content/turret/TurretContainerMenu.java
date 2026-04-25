package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.content.turret.block.ParticleTurretBlockEntity;
import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

// MyContainerMenu.java
public class TurretContainerMenu extends AbstractContainerMenu {
    //服务端容器，和客户端那边的screen对接
    //暂时不支持塞升级芯片，将来需要的话就加入物品栏容器
    private final AbstractTurretBlockEntity blockEntity;

    public TurretContainerMenu(int id, Inventory playerInv, AbstractTurretBlockEntity be) {
        super(ModMenuTypes.TURRET_MENU.get(), id);
        this.blockEntity = be;

        if (be instanceof ParticleTurretBlockEntity particleTurretBlockEntity) {
            // 功能：为粒子炮添加 3x3 弹药槽，仅接受 particle_container。
            int slotStartX = 59;
            int slotStartY = 17;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int slotIndex = col + row * 3;
                    this.addSlot(new SlotItemHandler(particleTurretBlockEntity, slotIndex, slotStartX + col * 18, slotStartY + row * 18));
                }
            }
        }
    }

    // 标准添加玩家背包代码
    //private void addPlayerInventory(Inventory playerInv) { ... }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved();
    }

    public AbstractTurretBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            // 功能：当前容器仅包含粒子炮 3x3 弹药槽，不包含玩家背包槽，故禁用 shift 快速转移。
            return ItemStack.EMPTY;
        }
        return original;
    }
}
