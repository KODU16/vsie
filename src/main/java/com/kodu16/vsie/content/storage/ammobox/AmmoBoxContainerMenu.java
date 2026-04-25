package com.kodu16.vsie.content.storage.ammobox;

import com.kodu16.vsie.registries.ModMenuTypes;
import com.kodu16.vsie.registries.ModMenuTypes; // 假设你在这里注册了 MenuType
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AmmoBoxContainerMenu extends AbstractContainerMenu {

    private final IItemHandler ammoBoxInventory;
    private final BlockPos blockPosition;

    // 服务器端和客户端都会调用这个构造
    public AmmoBoxContainerMenu(int id, Inventory playerInventory, IItemHandler ammoBoxInventory, BlockPos pos) {
        super(ModMenuTypes.AMMO_BOX_MENU.get(), id); // 替换成你注册的 MenuType
        this.ammoBoxInventory = ammoBoxInventory;
        this.blockPosition = pos;

        // ===============================
        //   AmmoBox 的 27 个槽位 (3行 × 9列)
        // ===============================
        int startX = 8;   // 界面左边距
        int startY = 18;  // 上方到第一个槽的距离（可微调）

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                int x = startX + col * 18;
                int y = startY + row * 18;

                this.addSlot(new SlotItemHandler(ammoBoxInventory, index, x, y));
            }
        }

        // ===============================
        //   玩家背包（3行9列）
        // ===============================
        int playerInvY = startY + 4 * 18-6; // 箱子下面留点空隙

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9; // 9 hotbar + 背包前27格
                int x = startX + col * 18;
                int y = playerInvY + row * 18;

                this.addSlot(new Slot(playerInventory, index, x, y));
            }
        }

        // ===============================
        //   玩家快捷栏（最下方一排）
        // ===============================
        int hotbarY = playerInvY + 3 * 18; // 再往下一点

        for (int col = 0; col < 9; col++) {
            int index = col; // 快捷栏是 0~8
            int x = startX + col * 18;
            int y = hotbarY;

            this.addSlot(new Slot(playerInventory, index, x, y));
        }
    }

    // 客户端从 BlockEntity 打开时使用的构造器
    public AmmoBoxContainerMenu(int id, Inventory playerInventory, AmmoBoxBlockEntity ammoBox) {
        this(id, playerInventory, ammoBox.getInventory(), ammoBox.getBlockPos());
    }

    // 玩家是否还能继续使用这个箱子（距离、箱子是否还在等）

    // 快速移动物品（Shift + 单击）的逻辑
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            // 箱子槽位 (0~26) → 尝试移到玩家背包/快捷栏
            if (index < 27) {
                if (!this.moveItemStackTo(stack, 27, 63, true)) { // 27~62 是玩家背包+快捷栏
                    return ItemStack.EMPTY;
                }
            }
            // 玩家背包/快捷栏 → 尝试移到箱子
            else {
                if (!this.moveItemStackTo(stack, 0, 27, false)) {
                    // 如果箱子放不下，再尝试在玩家背包内整理
                    if (index < 54) { // 主背包 27~53
                        if (!this.moveItemStackTo(stack, 54, 63, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else { // 快捷栏 54~62
                        if (!this.moveItemStackTo(stack, 27, 54, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
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

    // 可选：关闭时做的清理（通常不需要）
    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.isRemoved()) {
            return false;
        }

        var be = player.level().getBlockEntity(blockPosition);
        if (!(be instanceof AmmoBoxBlockEntity)) {
            return false;  // 方块被替换/破坏了
        }

        double maxDistSq = 8.0 * 8.0; // 通常 8 格
        return player.distanceToSqr(blockPosition.getX() + 0.5,
                blockPosition.getY() + 0.5,
                blockPosition.getZ() + 0.5) <= maxDistSq;
    }

}
