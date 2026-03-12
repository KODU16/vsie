package com.kodu16.vsie.content.turret.block;

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

    public double getYAxisOffset() {return 2.0d;}

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
        triggerAnim("controller", "shoot");
        Vec3 center = getBlockPos().getCenter();
        ParticleBulletEntity bullet = new ParticleBulletEntity(vsieEntities.PARTICLE_BULLET.get(), level);
        bullet.setPos(new Vec3(this.currentworldpos.x,this.currentworldpos.y,this.currentworldpos.z));
        bullet.setDeltaMovement(center.vectorTo(new Vec3(targetPos.x,targetPos.y,targetPos.z)).normalize().scale(20.0F));
        level.addFreshEntity(bullet);
        // 功能：开火后由服务端发送 S2C 包触发子弹 awake FX，避免 shootentity 仅服务端执行时客户端无法进入旧的 isClientSide 分支。
        vsieFxHelper.extractFxUnit(getData().fxData, FxData::getAwakeFx)
                .map(FxData.FxUnit::getId)
                .ifPresent(fxId -> {
                    // 功能：仅在服务端广播给跟踪该子弹的客户端（含自身），让所有可见玩家都能稳定看到初始特效。
                    if (!this.level.isClientSide()) {
                        ModNetworking.CHANNEL.send(
                                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> bullet),
                                new FxEntityS2CPacket(fxId, bullet.getId(), true)
                        );
                    }
                });
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
