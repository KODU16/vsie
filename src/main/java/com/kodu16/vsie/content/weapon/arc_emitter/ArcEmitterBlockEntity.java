package com.kodu16.vsie.content.weapon.arc_emitter;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ArcEmitterBlockEntity extends AbstractWeaponBlockEntity {
    public ArcEmitterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return 20;
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
        return Component.literal("ARCE");
    }

    @Override
    public String getweapontype() {
        return "arc_emitter";
    }
}
