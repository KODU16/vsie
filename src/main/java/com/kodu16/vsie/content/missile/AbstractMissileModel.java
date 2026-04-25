package com.kodu16.vsie.content.missile;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。

import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.vsie;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

@SuppressWarnings({"removal"})
public class AbstractMissileModel extends DefaultedEntityGeoModel<AbstractMissileEntity> {
    public AbstractMissileModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "missile"));
    }
    @Override
    public ResourceLocation getModelResource(AbstractMissileEntity missile) {
        return switch (missile.getmissiletype()) {
            case "basic_missile" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/entity/basic_missile.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + missile.getmissiletype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractMissileEntity missile) {
        return switch (missile.getmissiletype()) {
            case "basic_missile" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/entity/basic_missile.png");
            default -> throw new IllegalStateException("Unexpected value: " + missile.getmissiletype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractMissileEntity missile) {
        return switch (missile.getmissiletype()) {
            case "basic_missile" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/entity/basic_missile_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + missile.getmissiletype());
        };
    }

    @Override
    public void setCustomAnimations(AbstractMissileEntity missile, long instanceId, AnimationState<AbstractMissileEntity> animationState) {
        CoreGeoBone base = this.getAnimationProcessor().getBone("base");
        if (base == null) return;

        Vec3 axisx_positive = new Vec3(0,0,-1);

        // 获取目标位置
        double dx = getx(missile);
        double dy = gety(missile);
        double dz = getz(missile);

        Vec3 horizonal_targetvec = new Vec3(dx, 0, dz);
        Vec3 vertical_targetvec = new Vec3(0,dy,dz);

        float horizonal_angle = -Mth.PI + Vec.angleBetween(horizonal_targetvec,axisx_positive);
        float vertical_angle = -Mth.HALF_PI + Vec.angleBetween(vertical_targetvec,axisx_positive);
        //LogUtils.getLogger().warn("missile is at:"+new Vec3(missile.getX(),missile.getY(),missile.getZ())+"target:"+tx+ty+tz);
        LogUtils.getLogger().warn("vertical: targetvec:"+vertical_targetvec+"angle:"+vertical_angle);

        base.setRotY(horizonal_angle);
        base.setRotX(vertical_angle);



        // 调试用（上线可删除）
        //LogUtils.getLogger().info("yaw:" + Math.toDegrees(smoothedYaw) + "pitch:"+ Math.toDegrees(smoothedPitch));
    }

    private double getx(AbstractMissileEntity animatable) {
        Double x = animatable.getAnimData(AbstractMissileEntity.MISSILE_MOMENTUM_X);
        if(x != null) {
            return x;
        }
        return 0;
    }

    private double gety(AbstractMissileEntity animatable) {
        Double y = animatable.getAnimData(AbstractMissileEntity.MISSILE_MOMENTUM_Y);
        if(y != null) {
            return y;
        }
        return 0;
    }

    private double getz(AbstractMissileEntity animatable) {
        Double z = animatable.getAnimData(AbstractMissileEntity.MISSILE_MOMENTUM_Z);
        if(z != null) {
            return z;
        }
        return 0;
    }

}
