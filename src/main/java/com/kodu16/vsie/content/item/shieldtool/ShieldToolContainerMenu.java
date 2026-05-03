package com.kodu16.vsie.content.item.shieldtool;

import com.kodu16.vsie.registries.ModMenuTypes;
import com.kodu16.vsie.utility.ItemStackNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
// ShieldToolContainerMenu.java

public class ShieldToolContainerMenu extends AbstractContainerMenu {

    // 直接持有物品栈引用（最推荐）
    private final ItemStack shieldToolStack;

    // 下面这些字段用来给客户端显示 / 同步用
    public final int maxShield;
    public final int radius;
    public final int costPerProjectile;
    public final int regenPerTick;
    public final int maxCooldown;
    public final double dmax;
    public final double dmin;

    public ShieldToolContainerMenu(int windowId, Inventory playerInventory, ItemStack shieldToolStack) {
        super(ModMenuTypes.SHIELD_TOOL_MENU.get(), windowId);  // 假设你有注册 MenuType

        this.shieldToolStack = shieldToolStack;

        // 在构造时就读取（服务端和客户端都会执行这一步）
        CompoundTag tag = ItemStackNbt.get(shieldToolStack);
        if (tag != null && tag.contains(shieldtool.KEY_MAX_SHIELD)) {
            this.maxShield        = tag.getInt(shieldtool.KEY_MAX_SHIELD);
            this.radius           = tag.getInt(shieldtool.KEY_RADIUS);
            this.costPerProjectile = tag.getInt(shieldtool.KEY_COST);
            this.regenPerTick     = tag.getInt(shieldtool.KEY_REGEN);
            this.maxCooldown      = tag.getInt(shieldtool.KEY_COOLDOWN);
            this.dmax = tag.getDouble(shieldtool.KEY_DISTANCE_MAX);
            this.dmin = tag.getDouble(shieldtool.KEY_DISTANCE_MIN);
        } else {
            // 没数据给默认值，或者抛异常/设为0都可以
            this.maxShield = this.radius = this.costPerProjectile =
                    this.regenPerTick = this.maxCooldown = 0;
            this.dmax = this.dmin = 0;
        }

        // ... 其他的槽位、addPlayerInventory 等代码
    }

    // 如果你想后续还能拿到物品栈（比如有按钮要修改值）
    public ItemStack getShieldToolStack() {
        return shieldToolStack;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    // 必须实现的抽象方法
    @Override
    public boolean stillValid(Player player) {
        return !shieldToolStack.isEmpty() && player.getMainHandItem() == shieldToolStack;
    }

    // ... 其他你需要的代码
}

