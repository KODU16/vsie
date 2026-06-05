package com.kodu16.vsie.content.missile.entity;

import com.kodu16.vsie.content.missile.AbstractMissileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class BasicMissileEntity extends AbstractMissileEntity {
    public BasicMissileEntity(EntityType<? extends BasicMissileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String getmissiletype() {
        return "basic_missile";
    }
}
