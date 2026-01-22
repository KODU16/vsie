package com.kodu16.vsie.content.weapon.infra_knife_accelerator;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

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
        return 5;
    }

    @Override
    public void fire() {

    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

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
