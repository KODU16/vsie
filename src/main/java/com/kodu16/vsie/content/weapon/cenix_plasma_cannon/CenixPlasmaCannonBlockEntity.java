package com.kodu16.vsie.content.weapon.cenix_plasma_cannon;

import com.kodu16.vsie.content.bullet.entity.ParticleBulletEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.registries.vsieEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

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
        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos());
        Vector3d currentfacing = new Vector3d(0,1,0);
        ship.getTransform().getShipToWorld().transformDirection(VectorConversionsMCKt.toJOMLD(this.getBlockState().getValue(FACING).getNormal()),currentfacing);
        ParticleBulletEntity bullet = new ParticleBulletEntity(vsieEntities.PARTICLE_BULLET.get(), level);
        bullet.setPos(new Vec3(this.weaponpos.x,this.weaponpos.y,this.weaponpos.z));
        bullet.setDeltaMovement(new Vec3(currentfacing.x*20,currentfacing.y*20,currentfacing.z*20));
        level.addFreshEntity(bullet);
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
