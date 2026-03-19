package com.kodu16.vsie.content.warpprojectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

public class WarpProjecTileEntity extends Projectile {
    @Override
    protected void defineSynchedData() {

    }

    public WarpProjecTileEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;           // 保持高速、无重力
        this.setNoGravity(true);         // 推荐一起设置
    }
}
