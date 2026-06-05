package com.kodu16.vsie.content.item.shieldtool;

import com.kodu16.vsie.registries.ModMenuTypes;
import com.kodu16.vsie.utility.ItemStackNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class ShieldToolContainerMenu extends AbstractContainerMenu {
    private static final int DISTANCE_SCALE = 100;
    private static final int DATA_MAX_DISTANCE = 0;
    private static final int DATA_MIN_DISTANCE = 1;
    private static final int DATA_MAX_SHIELD = 2;
    private static final int DATA_RADIUS = 3;
    private static final int DATA_COST = 4;
    private static final int DATA_REGEN = 5;
    private static final int DATA_COOLDOWN = 6;
    private static final int DATA_COUNT = 7;

    // 保留物品栈引用，只用于 stillValid 与再次读取物品 tooltip 数据。
    private final ItemStack shieldToolStack;
    private final ContainerData data;

    public ShieldToolContainerMenu(int windowId, Inventory playerInventory, ItemStack shieldToolStack) {
        this(windowId, playerInventory, shieldToolStack, createSnapshotData(shieldToolStack));
    }

    private ShieldToolContainerMenu(int windowId, Inventory playerInventory, ItemStack shieldToolStack, ContainerData data) {
        super(ModMenuTypes.SHIELD_TOOL_MENU.get(), windowId);
        this.shieldToolStack = shieldToolStack;
        this.data = data;
        // 通过数据槽把护盾参数从服务端菜单同步给客户端，避免界面依赖物品 NBT 先同步成功。
        this.addDataSlots(this.data);
    }

    private static ContainerData createSnapshotData(ItemStack shieldToolStack) {
        SimpleContainerData data = new SimpleContainerData(DATA_COUNT);
        CompoundTag tag = ItemStackNbt.get(shieldToolStack);
        if (tag == null) {
            return data;
        }

        data.set(DATA_MAX_DISTANCE, scaleDistance(tag.getDouble(shieldtool.KEY_DISTANCE_MAX)));
        data.set(DATA_MIN_DISTANCE, scaleDistance(tag.getDouble(shieldtool.KEY_DISTANCE_MIN)));
        data.set(DATA_MAX_SHIELD, tag.getInt(shieldtool.KEY_MAX_SHIELD));
        data.set(DATA_RADIUS, tag.getInt(shieldtool.KEY_RADIUS));
        data.set(DATA_COST, tag.getInt(shieldtool.KEY_COST));
        data.set(DATA_REGEN, tag.getInt(shieldtool.KEY_REGEN));
        data.set(DATA_COOLDOWN, tag.getInt(shieldtool.KEY_COOLDOWN));
        return data;
    }

    private static int scaleDistance(double value) {
        return Mth.floor(value * DISTANCE_SCALE);
    }

    private static double unscaleDistance(int value) {
        return value / (double) DISTANCE_SCALE;
    }

    public ItemStack getShieldToolStack() {
        return shieldToolStack;
    }

    public double getMaxDistance() {
        return unscaleDistance(this.data.get(DATA_MAX_DISTANCE));
    }

    public double getMinDistance() {
        return unscaleDistance(this.data.get(DATA_MIN_DISTANCE));
    }

    public int getMaxShield() {
        return this.data.get(DATA_MAX_SHIELD);
    }

    public int getRadius() {
        return this.data.get(DATA_RADIUS);
    }

    public int getCostPerProjectile() {
        return this.data.get(DATA_COST);
    }

    public int getRegenPerTick() {
        return this.data.get(DATA_REGEN);
    }

    public int getMaxCooldown() {
        return this.data.get(DATA_COOLDOWN);
    }

    public boolean hasShieldData() {
        return getMaxShield() > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !shieldToolStack.isEmpty()
                && (player.getMainHandItem() == shieldToolStack || player.getOffhandItem() == shieldToolStack);
    }
}
