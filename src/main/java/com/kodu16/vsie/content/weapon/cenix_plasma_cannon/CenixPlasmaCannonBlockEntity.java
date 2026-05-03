package com.kodu16.vsie.content.weapon.cenix_plasma_cannon;

import com.kodu16.vsie.content.bullet.entity.CenixPlasmaBulletEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import static com.kodu16.vsie.content.weapon.AbstractWeaponBlock.FACING;

public class CenixPlasmaCannonBlockEntity extends AbstractWeaponBlockEntity {
    public CenixPlasmaCannonBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
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
        Vector3d currentFacing = subLevel.logicalPose()
                .transformNormal(directionToVector(weaponFacing), new Vector3d())
                .normalize();
        CenixPlasmaBulletEntity bullet = new CenixPlasmaBulletEntity(vsieEntities.CENIX_PLASMA_BULLET.get(), level);
        bullet.setPos(new Vec3(this.weaponpos.x,this.weaponpos.y,this.weaponpos.z));
        Vector3d shipSpeed = getLinearVelocity(serverSubLevel);
        bullet.setDeltaMovement(new Vec3(currentFacing.x * 4 + shipSpeed.x(), currentFacing.y * 4 + shipSpeed.y(), currentFacing.z * 4 + shipSpeed.z()));
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

    @Override
    public Component getDisplayName() {
        return Component.literal("CNXP");
    }

    @Override
    public String getweapontype() {
        return "cenix_plasma_cannon";
    }
}
