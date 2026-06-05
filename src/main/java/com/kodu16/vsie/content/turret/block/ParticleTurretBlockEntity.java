package com.kodu16.vsie.content.turret.block;

import com.kodu16.vsie.content.bullet.entity.ParticleBulletEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieItems;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class ParticleTurretBlockEntity extends AbstractTurretBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);

    public ParticleTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {
    }

    @Override
    public Vec3 getShootLocation(Vec3 vec, List<Vector3d> preV, Level lv, Vec3 pos) {
        return vec;
    }

    @Override
    public String getturrettype() {
        return "particle";
    }

    @Override
    public boolean isEnergyTurret() {
        return false;
    }

    @Override
    public Item getAmmoItem() {
        return vsieItems.PARTICLE_CONTAINER.get();
    }

    @Override
    public double getYAxisOffset() {
        return 2.8;
    }

    @Override
    public double getcannonlength() {
        return 4;
    }

    @Override
    public float getMaxSpinSpeed() {
        return (float) (Math.PI / 64.0D);
    }

    @Override
    public int getCoolDown() {
        return 60;
    }

    @Override
    public int getenergypertick() {
        return 100;
    }

    @Override
    public void shootentity() {
        launchParticleBullet(false);
    }

    @Override
    public void shootship() {
        launchParticleBullet(true);
    }

    private void launchParticleBullet(boolean explodesOnBlockHit) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        Vec3 firepoint = getCannonMuzzleWorld(targetPos);
        if (firepoint == null) {
            return;
        }

        Vec3 direction = targetPos.subtract(firepoint);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        triggerAnim("controller", "shoot");
        ParticleBulletEntity bullet = new ParticleBulletEntity(vsieEntities.PARTICLE_BULLET.get(), level);
        // Function: start behind the muzzle on the same axis, matching CBC's stable out-of-barrel launch.
        bullet.setPos(ParticleBulletEntity.spawnBehindMuzzle(firepoint, direction));
        bullet.setPreciseLaunchVelocity(direction);
        // Function: ship shots reuse particle bullets but enable block-breaking impact behavior.
        bullet.setExplodesOnBlockHit(explodesOnBlockHit);
        bullet.setBreaksBlocksEnabled(breaksBlocksEnabled());
        level.addFreshEntity(bullet);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE)
                .triggerableAnim("shoot", SHOOT_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
