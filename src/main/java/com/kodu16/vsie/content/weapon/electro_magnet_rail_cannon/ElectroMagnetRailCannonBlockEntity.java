package com.kodu16.vsie.content.weapon.electro_magnet_rail_cannon;

import com.kodu16.vsie.content.bullet.entity.ElectroMagnetRailCannonBulletEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreBlock;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.network.sound.RailCannonFireSoundS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieBlocks;
import com.kodu16.vsie.vsie;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    private static final ResourceLocation RAIL_CANNON_FIRE_ROOT_FX =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "electro_magnetic_rail_cannon_fire_root");
    private static final float RAIL_CANNON_FIRE_ROOT_AUTHORED_LENGTH = 20.0F;
    private static final float FIRE_SOUND_RANGE_PER_RAIL = 3.5F;
    private static final int FIRE_ROOT_TO_BULLET_DELAY_TICKS = 5;
    private static final String PENDING_FIRE_DELAY_TAG = "PendingRailCannonFireDelay";
    private int pendingFireDelayTicks;

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
        // Function: rail-cannon launches consume dedicated shell ammo instead of generic particle containers.
        return false;
    }

    @Override
    public Item getAmmoItem() {
        return vsieBlocks.ELECTRO_MAGNET_SHELL_BLOCK.asItem();
    }

    @Override
    public void tick() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        if (level.isClientSide()) {
            super.tick();
            return;
        }
        if (pendingFireDelayTicks > 0) {
            tickPendingFireDelay(level);
            return;
        }

        boolean fireRequested = needtofire();
        tickFireCooldown(fireRequested);
        if (!fireRequested) {
            setFiringState(false);
            return;
        }
        if (!isFireCooldownReady()) {
            setFiringState(false);
            return;
        }
        if (!hasInitialized) {
            return;
        }

        RailShotContext shotContext = resolveRailShotContext();
        if (shotContext == null || !consumeAmmoForShot()) {
            setFiringState(false);
            return;
        }

        consumeFireCooldown();
        setFiringState(true);
        weaponpos = shotContext.weaponPos();
        playFireSound(level, shotContext.effectiveRailLength());
        playFireFx(shotContext.subLevel(), shotContext.fireDirection(), shotContext.effectiveRailLength());
        pendingFireDelayTicks = FIRE_ROOT_TO_BULLET_DELAY_TICKS;
        setChanged();
    }

    @Override
    public void fire() {
        spawnDelayedBullet();
    }

    private void tickPendingFireDelay(Level level) {
        // Function: charging locks cooldown and heat recovery by skipping tickFireCooldown until the bullet is released.
        setFiringState(true);
        pendingFireDelayTicks--;
        if (pendingFireDelayTicks > 0) {
            return;
        }

        pendingFireDelayTicks = 0;
        spawnDelayedBullet();
        setFiringState(false);
        setChanged();
    }

    private void spawnDelayedBullet() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        RailShotContext shotContext = resolveRailShotContext();
        if (shotContext == null) {
            return;
        }

        Vec3 launchDirection = new Vec3(shotContext.fireDirection().x, shotContext.fireDirection().y, shotContext.fireDirection().z).normalize();
        ElectroMagnetRailCannonBulletEntity bullet = new ElectroMagnetRailCannonBulletEntity(vsieEntities.ELECTRO_MAGNET_RAIL_CANNON_BULLET.get(), level);
        bullet.configureRailCount(shotContext.core().getStoredRailCount());
        bullet.setPos(shotContext.spawnPos());
        // Function: keep the first client tick on the rail axis instead of vanilla's clamped motion vector.
        bullet.setLaunchSubLevel(shotContext.subLevel());
        bullet.setPreciseLaunchVelocity(launchDirection);
        bullet.setBreaksBlocksEnabled(breaksBlocksEnabled());
        level.addFreshEntity(bullet);
    }

    private RailShotContext resolveRailShotContext() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return null;
        }

        Direction weaponFacing = getBlockState().getValue(FACING);
        ElectroMagnetRailCoreBlockEntity core = resolveLinkedRailCore(weaponFacing);
        if (core == null) {
            return null;
        }
        int effectiveRailLength = core.getEffectiveRailLength();
        if (effectiveRailLength <= 0) {
            return null;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        Vector3d fireDirection = directionToVector(weaponFacing);
        Vec3 spawnPos = Vec3.atCenterOf(core.getTerminalPos().relative(weaponFacing, 3));
        Vec3 weaponWorldPos = Vec3.atCenterOf(getBlockPos());
        if (subLevel != null) {
            // Function: rail muzzle position and direction are stored in sublevel space and must fire in world space.
            subLevel.logicalPose().transformNormal(fireDirection, fireDirection).normalize();
            spawnPos = ServerShipUtils.getBlockCenterWorld(subLevel, core.getTerminalPos().relative(weaponFacing, 3));
            weaponWorldPos = ServerShipUtils.getBlockCenterWorld(subLevel, getBlockPos());
        } else {
            fireDirection.normalize();
        }

        return new RailShotContext(core, subLevel, fireDirection, spawnPos, weaponWorldPos, effectiveRailLength);
    }

    private static Vector3d directionToVector(Direction direction) {
        return new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private void playFireSound(Level level, int effectiveRailLength) {
        if (!(level instanceof ServerLevel serverLevel) || weaponpos == null) {
            return;
        }

        float soundRange = effectiveRailLength * FIRE_SOUND_RANGE_PER_RAIL;
        double soundRangeSqr = soundRange * soundRange;
        RailCannonFireSoundS2CPacket packet = new RailCannonFireSoundS2CPacket(
                weaponpos.x, weaponpos.y, weaponpos.z, soundRange
        );
        // Function: only players inside the rail-count-derived sound radius receive the one-shot sound.
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(weaponpos) <= soundRangeSqr) {
                ModNetworking.sendToPlayer(packet, player);
            }
        }
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
        // Function: the root FX is authored as 20 blocks along local +Y; scale that axis to the active rail length.
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                RAIL_CANNON_FIRE_ROOT_FX,
                fxPos.x, fxPos.y, fxPos.z,
                sublevelVelocity.x, sublevelVelocity.y, sublevelVelocity.z,
                rotation,
                new Vector3f(1.0F, Math.max(0.01F, effectiveRailLength / RAIL_CANNON_FIRE_ROOT_AUTHORED_LENGTH), 1.0F),
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
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt(PENDING_FIRE_DELAY_TAG, pendingFireDelayTicks);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        pendingFireDelayTicks = Math.max(0, tag.getInt(PENDING_FIRE_DELAY_TAG));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("EMRC");
    }

    @Override
    public String getweapontype() {
        return "electro_magnet_rail_cannon";
    }

    private record RailShotContext(ElectroMagnetRailCoreBlockEntity core, SubLevel subLevel, Vector3d fireDirection,
                                   Vec3 spawnPos, Vec3 weaponPos, int effectiveRailLength) {
    }
}
