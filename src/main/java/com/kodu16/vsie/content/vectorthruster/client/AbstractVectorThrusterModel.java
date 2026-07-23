package com.kodu16.vsie.content.vectorthruster.client;

import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"removal"})
public class AbstractVectorThrusterModel extends DefaultedBlockGeoModel<AbstractVectorThrusterBlockEntity> {

    // Function: keep smoothing state per rendered instance so vector thrusters do not share rotation interpolation.
    private final Map<Long, float[]> smoothStateByInstance = new HashMap<>();

    public AbstractVectorThrusterModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "vector_thruster"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractVectorThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic_vector" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/basic_vector_thruster.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractVectorThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic_vector" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/basic_vector_thruster.png");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractVectorThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic_vector" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/basic_vector_thruster_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    @Override
    public void setCustomAnimations(AbstractVectorThrusterBlockEntity animatable, long instanceId, AnimationState<AbstractVectorThrusterBlockEntity> animationState) {
        GeoBone spinner = getAnimationProcessor().getBone("spinner");
        GeoBone nozzle = getAnimationProcessor().getBone("nozzle");
        if (spinner == null || nozzle == null) {
            return;
        }

        double targetSpin = getSpin(animatable);
        double targetPitch = getPitch(animatable);
        float[] state = smoothStateByInstance.computeIfAbsent(instanceId, id -> new float[]{0f, 0f});
        float smoothSpinRad = Mth.rotLerp(0.05F, state[0], (float) targetSpin);
        float smoothPitchRad = Mth.rotLerp(0.05F, state[1], (float) targetPitch);

        state[0] = smoothSpinRad;
        state[1] = smoothPitchRad;

        spinner.setRotY((float) (Math.PI + smoothSpinRad));
        nozzle.setRotX((float) (Math.PI + smoothPitchRad));
    }

    private double getSpin(AbstractVectorThrusterBlockEntity animatable) {
        Double spin = animatable.getAnimData(AbstractVectorThrusterBlockEntity.VECTOR_THRUSTER_YAW);
        return spin != null ? spin : 0.0D;
    }

    private double getPitch(AbstractVectorThrusterBlockEntity animatable) {
        Double pitch = animatable.getAnimData(AbstractVectorThrusterBlockEntity.VECTOR_THRUSTER_PITCH);
        return pitch != null ? pitch : 0.0D;
    }
}
