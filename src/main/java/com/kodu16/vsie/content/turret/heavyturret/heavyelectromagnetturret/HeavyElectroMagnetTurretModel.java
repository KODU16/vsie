package com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings("removal")
public class HeavyElectroMagnetTurretModel extends DefaultedBlockGeoModel<HeavyElectroMagnetTurretBlockEntity> {
    public HeavyElectroMagnetTurretModel() {
        super(new ResourceLocation(vsie.ID,"heavy_electromagnet_turret"));
    }
    @Override
    public ResourceLocation getModelResource(HeavyElectroMagnetTurretBlockEntity turret) {
        return new ResourceLocation(vsie.ID, "geo/block/heavy_electromagnet_turret.geo.json");
    }
    public ResourceLocation getTextureResource(HeavyElectroMagnetTurretBlockEntity turret) {
        return new ResourceLocation(vsie.ID, "textures/block/heavy_electromagnet_turret.png");
    }
    public ResourceLocation getAnimationResource(HeavyElectroMagnetTurretBlockEntity turret) {
        return new ResourceLocation(vsie.ID, "animations/block/heavy_electromagnet_turret_anim.json");
    }

    @Override
    public void setCustomAnimations(HeavyElectroMagnetTurretBlockEntity animatable, long instanceId, AnimationState<HeavyElectroMagnetTurretBlockEntity> animationState) {
        CoreGeoBone turret = getAnimationProcessor().getBone("turret");
        CoreGeoBone cannon = getAnimationProcessor().getBone("cannon");
        if(turret != null && cannon != null) {
            float xRot = lerp(animatable.prevxrot, getX(animatable));
            float yRot = lerp(animatable.prevyrot, getY(animatable));
            animatable.prevxrot = xRot;
            animatable.prevyrot = yRot;
            cannon.setRotZ(xRot);
            turret.setRotY(yRot);
        }
    }
    private float lerp(float start, float end) {
        return Mth.rotLerp(0.1F, start * Mth.RAD_TO_DEG, end * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }

    private float getX(AbstractTurretBlockEntity animatable) {
        Float x = animatable.getAnimData(AbstractTurretBlockEntity.XROT);
        if(x != null) {
            return x;
        }
        return 0;
    }

    private float getY(AbstractTurretBlockEntity animatable) {
        Float y = animatable.getAnimData(AbstractTurretBlockEntity.YROT);
        if(y != null) {
            return y;
        }
        return 0;
    }
}
