package com.kodu16.vsie.content.controlseat.gui;

import com.kodu16.vsie.registries.vsieItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ControlSeatWarpSlot extends SlotItemHandler {
    public ControlSeatWarpSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 功能：限制控制椅仓位只允许放入 warp data chip，避免其他物品占用专用槽位。
        return stack.is(vsieItems.WARP_DATA_CHIP.get());
    }
}
