package com.kodu16.vsie.content.weapon.missile_launcher.block;

import com.kodu16.vsie.content.weapon.missile_launcher.AbstractMissileLauncherBlockEntity;
import com.kodu16.vsie.registries.vsieItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BasicMissileLauncherBlockEntity extends AbstractMissileLauncherBlockEntity {
    public BasicMissileLauncherBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public boolean isEnergyWeapon() {
        return false;
    }

    @Override
    public Item getAmmoItem() {
        return vsieItems.BASIC_MISSILE_ITEM.get();
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return 20;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("BMSL");
    }

    @Override
    public String getmissilelaunchertype() {
        return "basic_missile_launcher";
    }

    @Override
    public String getweapontype() {
        return "basic_missile_launcher";
    }

    @Override
    protected boolean hasMissileAmmo() {
        return hasAmmoReady();
    }

    @Override
    protected boolean consumeMissileAmmo() {
        return consumeAmmoForShot();
    }
}
