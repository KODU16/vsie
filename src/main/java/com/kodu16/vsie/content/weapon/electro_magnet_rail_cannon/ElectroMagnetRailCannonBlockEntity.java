package com.kodu16.vsie.content.weapon.electro_magnet_rail_cannon;

import com.kodu16.vsie.content.bullet.entity.ElectroMagnetRailCannonBulletEntity;
import com.kodu16.vsie.content.bullet.entity.HeavyElectroMagnetBulletEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreBlock;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import static com.kodu16.vsie.content.weapon.AbstractWeaponBlock.FACING;

public class ElectroMagnetRailCannonBlockEntity extends AbstractWeaponBlockEntity {
    private static final ResourceLocation RAIL_CANNON_FIRE_FX =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "electro_magnetic_rail_cannon_fire");

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
    public boolean isEnergyWeapon() {
        // Function: rail-cannon launches are treated as ammo-fed slugs until a dedicated projectile item is added.
        return false;
    }

    @Override
    public Item getAmmoItem() {
        return vsieItems.PARTICLE_CONTAINER.get();
    }

    @Override
    public void fire() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        Direction weaponFacing = getBlockState().getValue(FACING);
        ElectroMagnetRailCoreBlockEntity core = resolveLinkedRailCore(weaponFacing);
        if (core == null) {
            return;
        }
        int effectiveRailLength = core.getEffectiveRailLength();
        if (effectiveRailLength <= 0) {
            return;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        Vector3d fireDirection = directionToVector(weaponFacing);
        Vec3 spawnPos = Vec3.atCenterOf(core.getTerminalPos().relative(weaponFacing, 3));
        if (subLevel != null) {
            // Function: rail muzzle position and direction are stored in sublevel space and must fire in world space.
            subLevel.logicalPose().transformNormal(fireDirection, fireDirection).normalize();
            spawnPos = ServerShipUtils.getBlockCenterWorld(subLevel, core.getTerminalPos().relative(weaponFacing, 3));
        } else {
            fireDirection.normalize();
        }

        Vec3 launchDirection = new Vec3(fireDirection.x, fireDirection.y, fireDirection.z).normalize();
        playFireFx(subLevel, fireDirection, effectiveRailLength);
        ElectroMagnetRailCannonBulletEntity bullet = new ElectroMagnetRailCannonBulletEntity(vsieEntities.ELECTRO_MAGNET_RAIL_CANNON_BULLET.get(), level);
        bullet.configureRailCount(core.getStoredRailCount());
        bullet.setPos(spawnPos);
        // Function: keep the first client tick on the rail axis instead of vanilla's clamped motion vector.
        bullet.setPreciseLaunchVelocity(launchDirection);
        bullet.setBreaksBlocksEnabled(breaksBlocksEnabled());
        level.addFreshEntity(bullet);
    }

    private static Vector3d directionToVector(Direction direction) {
        return new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private void playFireFx(SubLevel subLevel, Vector3d fireDirection, int effectiveRailLength) {
        Vec3 fxPos = subLevel != null
                ? ServerShipUtils.getBlockCenterWorld(subLevel, getBlockPos())
                : Vec3.atCenterOf(getBlockPos());
        Vector3d direction = new Vector3d(fireDirection);
        if (direction.lengthSquared() <= 1.0E-6D) {
            return;
        }

        direction.normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(
                0.0F, 1.0F, 0.0F,
                (float) direction.x, (float) direction.y, (float) direction.z
        );
        Vector3d sublevelVelocity = getSublevelLinearVelocity(subLevel);
        // Function: the rail cannon fire FX is authored along local +Y, so local Y carries the active rail length.
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                RAIL_CANNON_FIRE_FX,
                fxPos.x, fxPos.y, fxPos.z,
                sublevelVelocity.x, sublevelVelocity.y, sublevelVelocity.z,
                rotation,
                new Vector3f(0.05F, Math.max(0.01F, effectiveRailLength * 1.5F), 0.05F),
                false,
                true
        ));
    }

    private Vector3d getSublevelLinearVelocity(SubLevel subLevel) {
        if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
            return new Vector3d();
        }
        RigidBodyHandle handle = RigidBodyHandle.of(serverSubLevel);
        if (handle == null || !handle.isValid()) {
            return new Vector3d();
        }
        // Function: keep the one-shot muzzle FX visually attached while the sublevel is moving.
        return handle.getLinearVelocity(new Vector3d());
    }

    private ElectroMagnetRailCoreBlockEntity resolveLinkedRailCore(Direction weaponFacing) {
        BlockPos corePos = this.getBlockPos().relative(weaponFacing.getOpposite());
        BlockEntity blockEntity = this.level.getBlockEntity(corePos);
        if (!(blockEntity instanceof ElectroMagnetRailCoreBlockEntity core)) {
            return null;
        }

        BlockState coreState = core.getBlockState();
        if (!coreState.hasProperty(ElectroMagnetRailCoreBlock.FACING)) {
            return null;
        }

        if (coreState.getValue(ElectroMagnetRailCoreBlock.FACING) != weaponFacing) {
            return null;
        }

        // Function: cannon firing is gated by the adjacent core's validated rail-top binding.
        return core.hasValidTerminalBinding() ? core : null;
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
