package com.kodu16.vsie.content.weapon.electro_magnet_rail_cannon;

import com.kodu16.vsie.content.bullet.entity.CenixPlasmaBulletEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreBlock;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

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
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
            return;
        }

        Direction weaponFacing = this.getBlockState().getValue(FACING);
        int railMultiplier = resolveRailMultiplier(weaponFacing);
        Vector3d currentFacing = subLevel.logicalPose()
                .transformNormal(directionToVector(weaponFacing), new Vector3d())
                .normalize();

        CenixPlasmaBulletEntity bullet = new CenixPlasmaBulletEntity(vsieEntities.CENIX_PLASMA_BULLET.get(), level);
        bullet.setPos(new Vec3(this.weaponpos.x, this.weaponpos.y, this.weaponpos.z));

        Vector3d shipSpeed = getLinearVelocity(serverSubLevel);
        double speed = 4.0 * railMultiplier;
        bullet.setDeltaMovement(new Vec3(
                currentFacing.x * speed + shipSpeed.x(),
                currentFacing.y * speed + shipSpeed.y(),
                currentFacing.z * speed + shipSpeed.z()
        ));
        level.addFreshEntity(bullet);
    }

    private static Vector3d directionToVector(Direction direction) {
        return new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static Vector3d getLinearVelocity(ServerSubLevel subLevel) {
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || !handle.isValid()) {
            return new Vector3d();
        }

        return handle.getLinearVelocity(new Vector3d());
    }

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
