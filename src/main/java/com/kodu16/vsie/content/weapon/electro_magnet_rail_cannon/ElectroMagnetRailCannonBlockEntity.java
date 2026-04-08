package com.kodu16.vsie.content.weapon.electro_magnet_rail_cannon;

import com.kodu16.vsie.content.bullet.entity.CenixPlasmaBulletEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreBlock;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.registries.vsieEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import static com.kodu16.vsie.content.weapon.AbstractWeaponBlock.FACING;

public class ElectroMagnetRailCannonBlockEntity extends AbstractWeaponBlockEntity {
    public ElectroMagnetRailCannonBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return 40;
    }

    @Override
    public void fire() {
        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos());
        if (ship == null) {
            return;
        }

        Direction weaponFacing = this.getBlockState().getValue(FACING);
        // 功能：读取“武器背后紧邻一格”的同朝向 core，计算导轨倍率 k。
        int railMultiplier = resolveRailMultiplier(weaponFacing);

        Vector3d currentFacing = new Vector3d(0, 1, 0);
        ship.getTransform().getShipToWorld().transformDirection(
                VectorConversionsMCKt.toJOMLD(weaponFacing.getNormal()),
                currentFacing
        );

        CenixPlasmaBulletEntity bullet = new CenixPlasmaBulletEntity(vsieEntities.CENIX_PLASMA_BULLET.get(), level);
        bullet.setPos(new Vec3(this.weaponpos.x, this.weaponpos.y, this.weaponpos.z));

        Vector3dc shipSpeed = ship.getVelocity();
        // 功能：弹丸基础速度沿用 CenixPlasmaCannon（4），并乘以导轨倍率 k。
        double speed = 4.0 * railMultiplier;
        bullet.setDeltaMovement(new Vec3(
                currentFacing.x * speed + shipSpeed.x(),
                currentFacing.y * speed + shipSpeed.y(),
                currentFacing.z * speed + shipSpeed.z()
        ));
        level.addFreshEntity(bullet);
    }

    // 功能：计算威力 k；当未匹配到合法 core 或 core 结构无效时返回 0。
    private int resolveRailMultiplier(Direction weaponFacing) {
        BlockPos corePos = this.getBlockPos().relative(weaponFacing.getOpposite());
        BlockEntity blockEntity = this.level.getBlockEntity(corePos);
        if (!(blockEntity instanceof ElectroMagnetRailCoreBlockEntity core)) {
            return 0;
        }

        BlockState coreState = core.getBlockState();
        if (!coreState.hasProperty(ElectroMagnetRailCoreBlock.FACING)) {
            return 0;
        }

        if (coreState.getValue(ElectroMagnetRailCoreBlock.FACING) != weaponFacing) {
            return 0;
        }

        return core.hasValidTerminalBinding() ? core.getStoredRailCount() : 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("EMRC");
    }

    @Override
    public String getweapontype() {
        return "electro_magnet_rail_cannon";
    }
}
