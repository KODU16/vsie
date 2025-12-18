package com.kodu16.vsie.content.vectorthruster.client;


import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractVectorThrusterModel extends DefaultedBlockGeoModel<AbstractVectorThrusterBlockEntity> {

    public AbstractVectorThrusterModel() {
        super(new ResourceLocation(vsie.ID,"vector_thruster"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractVectorThrusterBlockEntity abstractVectorThrusterBlockEntity) {
        return switch (abstractVectorThrusterBlockEntity.getvectorthrustertype()) {
            case "basic" -> new ResourceLocation(vsie.ID, "geo/block/basic_vector_thruster.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + abstractVectorThrusterBlockEntity.getvectorthrustertype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractVectorThrusterBlockEntity abstractVectorThrusterBlockEntity) {
        return switch (abstractVectorThrusterBlockEntity.getvectorthrustertype()) {
            case "basic" -> new ResourceLocation(vsie.ID, "textures/block/basic_vector_thruster.png");
            default -> throw new IllegalStateException("Unexpected value: " + abstractVectorThrusterBlockEntity.getvectorthrustertype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractVectorThrusterBlockEntity abstractVectorThrusterBlockEntity) {
        return switch (abstractVectorThrusterBlockEntity.getvectorthrustertype()) {
            case "basic" -> new ResourceLocation(vsie.ID, "animations/block/basic_vector_thruster_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + abstractVectorThrusterBlockEntity.getvectorthrustertype());
        };
    }

    @Override
    public void setCustomAnimations(AbstractVectorThrusterBlockEntity animatable, long instanceId, AnimationState<AbstractVectorThrusterBlockEntity> animationState) {
        CoreGeoBone spinner = getAnimationProcessor().getBone("spinner");
        CoreGeoBone nozzle = getAnimationProcessor().getBone("nozzle");
        if(spinner != null && nozzle != null) {
            if(controlling(animatable)){
                spinner.setRotZ((float)getspin(animatable));
                nozzle.setRotX((float)getpitch(animatable));
            }
        }
    }

    private boolean controlling(AbstractVectorThrusterBlockEntity animatable) {
        return Boolean.TRUE.equals(animatable.getAnimData(AbstractVectorThrusterBlockEntity.IS_SPINNING));
    }

    private float lerp(float start, float end) {
        return Mth.rotLerp(0.1F, start * Mth.RAD_TO_DEG, end * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }

    private double getspin(AbstractVectorThrusterBlockEntity animatable) {
        Double spin = animatable.getAnimData(AbstractVectorThrusterBlockEntity.FINAL_SPIN);
        if(spin != null) {
            return spin;
        }
        return 0;
    }

    private double getpitch(AbstractVectorThrusterBlockEntity animatable) {
        Double pitch = animatable.getAnimData(AbstractVectorThrusterBlockEntity.FINAL_PITCH);
        if(pitch != null) {
            return pitch;
        }
        return 0;
    }
}
