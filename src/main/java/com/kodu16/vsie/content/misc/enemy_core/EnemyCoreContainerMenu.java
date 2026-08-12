package com.kodu16.vsie.content.misc.enemy_core;

import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.ModMenuTypes;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class EnemyCoreContainerMenu extends AbstractContainerMenu {
    private final BlockPos blockPosition;
    private final String enemyPattern;
    private final String allyPattern;
    private final double orbitRadius;
    private final double orbitSpeed;

    public EnemyCoreContainerMenu(
            int id,
            Inventory inventory,
            BlockPos blockPosition,
            String enemyPattern,
            String allyPattern,
            double orbitRadius,
            double orbitSpeed
    ) {
        super(ModMenuTypes.ENEMY_CORE_MENU.get(), id);
        this.blockPosition = blockPosition;
        this.enemyPattern = enemyPattern;
        this.allyPattern = allyPattern;
        this.orbitRadius = orbitRadius;
        this.orbitSpeed = orbitSpeed;
    }

    public BlockPos getBlockPosition() {
        return blockPosition;
    }

    public String getEnemyPattern() {
        return enemyPattern;
    }

    public String getAllyPattern() {
        return allyPattern;
    }

    public double getOrbitRadius() {
        return orbitRadius;
    }

    public double getOrbitSpeed() {
        return orbitSpeed;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(blockPosition) instanceof EnemyCoreBlockEntity)) {
            return false;
        }
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(player.level(), blockPosition);
        // Function: menu reach checks use world space because Sable block positions remain in plot space.
        return player.position().distanceToSqr(ServerShipUtils.getBlockCenterWorld(subLevel, blockPosition)) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
