package com.kodu16.vsie.content.weapon.missile_launcher;

import com.kodu16.vsie.content.missile.entity.BasicMissileEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.WeaponData;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractMissileLauncherBlockEntity extends AbstractWeaponBlockEntity {

    public AbstractMissileLauncherBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.weaponData = new WeaponData();
        this.hasInitialized = true;
    }


    @Override
    public abstract float getmaxrange();

    @Override
    public abstract int getcooldown();

    @Override
    public abstract String getweapontype();

    @Override
    public void tick() {
        super.tick();
        currentTick++;
        if (currentTick<=getcooldown()) return;
        currentTick = 0;
        if(!needtofire()) {
            getData().isfiring = false;
            return;
        }
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (hasInitialized)
        {
            getData().isfiring = true;
            fire();
        }
    }

    @Override
    public void fire() {
        if(getData().targetship == null || level.isClientSide()) {
            LogUtils.getLogger().warn("target is empty");
            return;
        }
        // 导弹生成位置（可以是你方块实体位置 + 偏移）
        BlockState state = this.getBlockState();
        Direction facing = state.getValue(BlockStateProperties.FACING);
        Vec3i offset = switch (facing) {
            case NORTH -> new Vec3i(1, 0, 0);   // 朝北，模型正X为东
            case SOUTH -> new Vec3i(-1, 0, 0);  // 朝南，模型正X为西
            case WEST -> new Vec3i(0, -1, 0);   // 朝西，模型正X为下
            case EAST -> new Vec3i(0, 1, 0);    // 朝东，模型正X为上
            case UP -> new Vec3i(-1,0,0); //朝上，模型正X为西
            case DOWN -> new Vec3i(1,0,0); //朝下，模型正X为东
        };
        Vec3 offsetship = new Vec3(0,0,0);
        Vec3 spawnpos = this.getWeaponPos();
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,this.getBlockPos());
        offsetship = subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(offset));
        offsetship.normalize();
        spawnpos.add(new Vec3(offsetship.x(),offsetship.y(),offsetship.z()));
        BasicMissileEntity missile = new BasicMissileEntity(
                vsieEntities.BASIC_MISSILE.get(), level
        );
        SubLevel target = getData().targetship;
        missile.setTarget(target);
        missile.setPos(spawnpos);
        LogUtils.getLogger().warn("firing missile at:"+getData().targetship);
        level.addFreshEntity(missile);
    }

    @Override
    public Component getDisplayName() {
        return null;
    }

    public abstract String getmissilelaunchertype();
}
