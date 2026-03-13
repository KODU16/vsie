package com.kodu16.vsie.content.turret.block;

import com.kodu16.vsie.content.bullet.BulletData;
import com.kodu16.vsie.content.bullet.entity.ParticleBulletEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.network.fx.FxEntityS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.utility.FxData;
import com.kodu16.vsie.utility.vsieFxHelper;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3d;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class ParticleTurretBlockEntity extends AbstractTurretBlockEntity {
    public ParticleTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public Vector3d getShootLocation(Vector3d vec, List<Vector3d> preV, Level lv, Vector3d pos) {
        return vec;
    }

    public String getturrettype() {
        return "particle";
    }

    public double getYAxisOffset() {return 2.45d;}

    @Override
    protected Vector3d getTurretPivotInGeoPixels() {
        // 功能：返回 particle turret 模型中 turret 骨骼的枢轴点，用于计算真实世界炮口基准点。
        return new Vector3d(0.03509, 13.00917, -0.0829);
    }

    @Override
    protected Vector3d getCannonPivotInGeoPixels() {
        // 功能：返回 particle turret 模型中 cannon 骨骼的枢轴点，用于让子弹从炮管实际旋转中心射出。
        return new Vector3d(0.0, 46.0, 7.0);
    }

    @Override
    public double getcannonlength() {
        return 4;
    }

    @Override
    public float getMaxSpinSpeed() {
        return Mth.PI/64;
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
        // 功能：仅允许服务端执行开火逻辑，避免客户端在索敌/预测分支误触发一次射击动画。
        if (level == null || level.isClientSide) {
            return;
        }
        // 功能：射击动画与真实发射绑定，保证“生成炮弹后”才播放且每次开火仅触发一次。
        triggerAnim("controller", "shoot");
        // 功能：使用 GeckoLib cannon 骨骼枢轴的真实世界坐标作为粒子炮子弹发射点，修正模型偏移导致的出膛误差。
        Vector3d muzzleWorldPos = this.getCannonPivotWorldPos();
        Vec3 center = new Vec3(muzzleWorldPos.x, muzzleWorldPos.y, muzzleWorldPos.z);
        ParticleBulletEntity bullet = new ParticleBulletEntity(vsieEntities.PARTICLE_BULLET.get(), level);
        // 功能：为粒子炮子弹写入标准 data，确保子弹第 1 tick 使用 particle_cannon_fire 触发 awake FX。
        bullet.setDataBase(BulletData.createParticleCannonDefault());
        bullet.setPos(center);
        bullet.setDeltaMovement(center.vectorTo(new Vec3(targetPos.x,targetPos.y,targetPos.z)).normalize().scale(1.0F));
        level.addFreshEntity(bullet);
    }

    @Override
    public void shootship() {

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
