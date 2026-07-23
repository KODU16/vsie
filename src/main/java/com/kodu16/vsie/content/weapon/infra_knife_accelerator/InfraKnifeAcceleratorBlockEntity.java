package com.kodu16.vsie.content.weapon.infra_knife_accelerator;

import com.kodu16.vsie.content.bullet.entity.InfraKnifeBulletEntity;
import com.kodu16.vsie.content.cooldown.FireCooldown;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import static com.kodu16.vsie.content.weapon.AbstractWeaponBlock.FACING;

public class InfraKnifeAcceleratorBlockEntity extends AbstractWeaponBlockEntity {
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.LOOP);
    private static final int MAX_COOLDOWN_VALUE = 30;
    private static final double IDLE_RECOVERY_PER_TICK = 0.5D;
    private static final double MAX_SPREAD_RADIANS = Math.toRadians(1.0D);

    public InfraKnifeAcceleratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        if (level.isClientSide()) {
            return;
        }
        boolean fireRequested = needtofire();
        tickFireCooldown(fireRequested);
        if (!fireRequested) {
            setFiringState(false);
            return;
        }
        if (!isFireCooldownReady()) {
            // Function: keep the looped firing animation alive between actual infra-knife shots while the trigger is held.
            setFiringState(hasStoredFiringCharge());
            return;
        }
        if (hasInitialized) {
            BlockPos pos = getBlockPos();
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, pos);
            weaponpos = subLevel != null ? ServerShipUtils.getBlockCenterWorld(subLevel, pos) : Vec3.atCenterOf(pos);
            consumeFireCooldown();
            setFiringState(true);
            fire();
        }
    }

    private boolean hasStoredFiringCharge() {
        FireCooldown cooldown = getFireCooldown();
        return !cooldown.usesValue() || fireCooldownValue > 0.0D;
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return 3;
    }

    @Override
    public FireCooldown getFireCooldown() {
        // Function: infra-knife uses stored firing charge, recovering quickly while the trigger is released.
        return FireCooldown.cool2(getcooldown(), MAX_COOLDOWN_VALUE, IDLE_RECOVERY_PER_TICK);
    }

    @Override
    public boolean isEnergyWeapon() {
        // Function: infra-knife shots are energy projectiles, so they do not consume ammo items.
        return true;
    }

    @Override
    public Item getAmmoItem() {
        return null;
    }

    @Override
    public void fire() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        Direction weaponFacing = getBlockState().getValue(FACING);
        Vector3d direction = directionToVector(weaponFacing);
        Vec3 spawnPos = Vec3.atCenterOf(getBlockPos());
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        if (subLevel != null) {
            // Function: convert the local weapon facing and muzzle center into world space before spawning the bullet.
            direction = subLevel.logicalPose()
                    .transformNormal(direction, new Vector3d())
                    .normalize();
            spawnPos = subLevel.logicalPose().transformPosition(Vec3.atCenterOf(getBlockPos()));
        } else {
            direction.normalize();
        }

        Vec3 launchDirection = new Vec3(direction.x, direction.y, direction.z).normalize();
        Vec3 spreadDirection = applySpread(launchDirection, level);
        InfraKnifeBulletEntity bullet = new InfraKnifeBulletEntity(vsieEntities.INFRA_KNIFE_BULLET.get(), level);
        bullet.setPos(spawnPos.add(launchDirection.scale(1.2D)));
        bullet.setLaunchSubLevel(subLevel);
        bullet.setPreciseLaunchVelocity(spreadDirection);
        bullet.setBreaksBlocksEnabled(breaksBlocksEnabled());
        if (level.addFreshEntity(bullet)) {
            // Function: infra-knife uses one short fire sound for each bullet entity actually spawned.
            playFireSound(level);
        }
    }

    @Override
    protected SoundEvent getFireSoundEvent() {
        return vsieSounds.INFRA_KNIFE_ACCELERATOR_FIRE.get();
    }

    private static Vec3 applySpread(Vec3 direction, Level level) {
        Vec3 forward = direction.normalize();
        double angle = Math.sqrt(level.random.nextDouble()) * MAX_SPREAD_RADIANS;
        double azimuth = level.random.nextDouble() * Math.PI * 2.0D;
        Vec3 reference = Math.abs(forward.y) > 0.99D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        // Function: infra-knife shots get a slight one-degree cone spread while preserving the muzzle axis.
        return forward.scale(Math.cos(angle))
                .add(right.scale(Math.cos(azimuth) * Math.sin(angle)))
                .add(up.scale(Math.sin(azimuth) * Math.sin(angle)))
                .normalize();
    }

    private static Vector3d directionToVector(Direction direction) {
        return new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Function: keep the authored barrel-spin loop running for every tick that the server marks this weapon as firing.
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (!getData().isfiring) {
                // Function: force a reset so the cannon snaps out of the loop instead of preserving the last running animation state.
                state.getController().forceAnimationReset();
                return PlayState.STOP;
            }
            state.setAnimation(SHOOT_ANIMATION);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("IFRA");
    }

    @Override
    public String getweapontype() {
        return "infra_knife_accelerator";
    }
}
