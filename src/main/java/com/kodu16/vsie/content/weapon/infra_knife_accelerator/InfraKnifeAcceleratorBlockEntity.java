package com.kodu16.vsie.content.weapon.infra_knife_accelerator;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.logging.Logger;

public class InfraKnifeAcceleratorBlockEntity extends AbstractWeaponBlockEntity {
    public InfraKnifeAcceleratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return 4;
    }

    public void fire() {
        if(getRaycastDistance()<getmaxrange())
        {
            LogUtils.getLogger().warn("explode at:"+targetpos);
            level.explode(
                    null, // 爆炸源实体，可为null
                    targetpos.x, targetpos.y, targetpos.z, // 爆炸坐标
                    3, // 爆炸半径
                    true,// 是否点燃火焰
                    Level.ExplosionInteraction.NONE // 不破坏方块
            );
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Infra Knife Accelerator");
    }

    @Override
    public String getweapontype() {
        return "infra_knife_accelerator";
    }
}
