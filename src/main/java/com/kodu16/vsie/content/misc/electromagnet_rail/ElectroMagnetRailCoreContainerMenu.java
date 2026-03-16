package com.kodu16.vsie.content.misc.electromagnet_rail;

import com.kodu16.vsie.registries.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ElectroMagnetRailCoreContainerMenu extends AbstractContainerMenu {

    private final IItemHandler coreInventory;
    private final BlockPos blockPosition;
    private final ContainerData data;

    public ElectroMagnetRailCoreContainerMenu(int id, Inventory playerInventory, IItemHandler coreInventory, BlockPos pos) {
        super(ModMenuTypes.ELECTRO_MAGNET_RAIL_CORE_MENU.get(), id);
        this.coreInventory = coreInventory;
        this.blockPosition = pos;

        // 4 个核心仓位：2x2 布局，对应 IFF GUI 中央区域。
        int slotStartX = 68;
        int slotStartY = 32;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int index = col + row * 2;
                this.addSlot(new SlotItemHandler(coreInventory, index, slotStartX + col * 18, slotStartY + row * 18));
            }
        }

        // 玩家背包与快捷栏，保持标准交互（拖拽/shift 转移）。
        int playerInvStartX = 8;
        int playerInvStartY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, index, playerInvStartX + col * 18, playerInvStartY + row * 18));
            }
        }

        int hotbarY = 142;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, playerInvStartX + col * 18, hotbarY));
        }

        // 通过 ContainerData 同步当前存储 rail 总数给客户端显示。
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 0 && ElectroMagnetRailCoreContainerMenu.this.coreInventory instanceof ElectroMagnetRailCoreBlockEntity blockEntity) {
                    return blockEntity.getStoredRailCount();
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                // 客户端只读该值，不需要回写。
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        this.addDataSlots(this.data);
    }

    public ElectroMagnetRailCoreContainerMenu(int id, Inventory playerInventory, ElectroMagnetRailCoreBlockEntity coreBlockEntity) {
        this(id, playerInventory, coreBlockEntity, coreBlockEntity.getBlockPos());
    }

    public int getRailCount() {
        return this.data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            // 0~3 是核心仓，4~39 是玩家背包+快捷栏。
            if (index < 4) {
                if (!this.moveItemStackTo(stack, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, 4, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stack, original);
        }

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.isRemoved()) {
            return false;
        }

        var be = player.level().getBlockEntity(blockPosition);
        if (!(be instanceof ElectroMagnetRailCoreBlockEntity)) {
            return false;
        }

        return player.distanceToSqr(blockPosition.getX() + 0.5,
                blockPosition.getY() + 0.5,
                blockPosition.getZ() + 0.5) <= 64.0;
    }
}
