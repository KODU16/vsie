package com.kodu16.vsie.content.weapon.missile_launcher;

import com.kodu16.vsie.content.missile.entity.BasicMissileEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.WeaponData;
import com.kodu16.vsie.registries.vsieEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

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

    public Vec3 getworldpos() {
        Level level = this.getLevel();
        boolean onship = VSGameUtilsKt.isBlockInShipyard(level, this.getBlockPos());
        if(onship) {
            return VSGameUtilsKt.toWorldCoordinates(level, this.getBlockPos());
        }
        return new Vec3(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
    }

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
        Vector3d offsetship = new Vector3d();
        Vec3 spawnpos = this.getworldpos();
        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos());
        offsetship = ship.getTransform().getShipToWorld().transformDirection(VectorConversionsMCKt.toJOMLD(offset));
        offsetship.normalize();
        spawnpos.add(new Vec3(offsetship.x(),offsetship.y(),offsetship.z()));
        BasicMissileEntity missile = new BasicMissileEntity(
                vsieEntities.BASIC_MISSILE.get(), level
        );
        Ship target = getData().targetship;
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

    private static double[] getAABBdcCenter(AABBdc aabb) {
        double width = aabb.maxX() - aabb.minX();
        double len = aabb.maxZ() - aabb.minZ();
        double hight = aabb.maxY() - aabb.minY();
        double centerX = aabb.minX() + width / 2;
        double centerY = aabb.minY() + hight / 2;
        double centerZ = aabb.minZ() + len / 2;
        return new double[]{centerX, centerY, centerZ};
    }
}
