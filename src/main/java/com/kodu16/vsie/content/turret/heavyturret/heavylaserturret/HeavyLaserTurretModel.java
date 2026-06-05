package com.kodu16.vsie.content.turret.heavyturret.heavylaserturret;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings("removal")
public class HeavyLaserTurretModel extends DefaultedBlockGeoModel<HeavyLaserTurretBlockEntity> {
    public HeavyLaserTurretModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "heavy_laser_turret"));
    }

    @Override
    public ResourceLocation getModelResource(HeavyLaserTurretBlockEntity turret) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/heavy_laser_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HeavyLaserTurretBlockEntity turret) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/heavy_laser_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HeavyLaserTurretBlockEntity turret) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/turret/heavy_laser_turret_anim.json");
    }

    @Override
    public void setCustomAnimations(HeavyLaserTurretBlockEntity animatable, long instanceId, AnimationState<HeavyLaserTurretBlockEntity> animationState) {
        GeoBone turret = getAnimationProcessor().getBone("turret");
        GeoBone cannon = getAnimationProcessor().getBone("cannon");
        GeoBone locater = getAnimationProcessor().getBone("locater");
        if (locater != null) {
            // Function: track the custom locater bone so the server can raycast from the rendered muzzle.
            locater.setTrackingMatrices(true);
        }
        if (turret != null && cannon != null) {
            float xRot = lerp(animatable.prevxrot, getX(animatable));
            float yRot = lerp(animatable.prevyrot, getY(animatable));
            animatable.prevxrot = xRot;
            animatable.prevyrot = yRot;
            cannon.setRotX(-xRot);
            turret.setRotY((float) (yRot + Math.PI));
        }
    }

    private float lerp(float start, float end) {
        return Mth.rotLerp(0.1F, start * Mth.RAD_TO_DEG, end * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }

    private float getX(AbstractTurretBlockEntity animatable) {
        Float x = animatable.getAnimData(AbstractTurretBlockEntity.XROT);
        return x == null ? 0 : x;
    }

    private float getY(AbstractTurretBlockEntity animatable) {
        Float y = animatable.getAnimData(AbstractTurretBlockEntity.YROT);
        return y == null ? 0 : y;
    }
}
