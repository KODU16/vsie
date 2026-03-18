package com.kodu16.vsie.content.misc.electromagnet_rail.core;


import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
@SuppressWarnings("removal")
public class ElectroMagnetRailCoreModel extends DefaultedBlockGeoModel<ElectroMagnetRailCoreBlockEntity> {
    public ElectroMagnetRailCoreModel() {
        super(new ResourceLocation(vsie.ID,"heavy_electromagnet_turret"));
    }
    @Override
    public ResourceLocation getModelResource(ElectroMagnetRailCoreBlockEntity core) {
        return new ResourceLocation(vsie.ID, "geo/block/electro_magnet_rail_core.geo.json");
    }
    public ResourceLocation getTextureResource(ElectroMagnetRailCoreBlockEntity core) {
        return new ResourceLocation(vsie.ID, "textures/block/electro_magnet_rail_core.png");
    }
    public ResourceLocation getAnimationResource(ElectroMagnetRailCoreBlockEntity core) {
        return new ResourceLocation(vsie.ID, "animations/block/electro_magnet_rail_core_anim.json");
    }

    @Override
    public void setCustomAnimations(ElectroMagnetRailCoreBlockEntity animatable, long instanceId, AnimationState<ElectroMagnetRailCoreBlockEntity> animationState){
        CoreGeoBone railleft = getAnimationProcessor().getBone("railleft");
        CoreGeoBone railright = getAnimationProcessor().getBone("railright");
        if(railleft != null && railright != null) {
            float extend = lerp(animatable.prevextend, 48);
            animatable.prevextend = extend;
            railleft.setRotX(-extend);
            railright.setRotY(-extend);
        }
    }

    private float lerp(float start, float end) {
        return Mth.rotLerp(0.1F, start * Mth.RAD_TO_DEG, end * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }
}
