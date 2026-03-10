package com.kodu16.vsie.content.vectorthruster.client;


import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"removal"})
public class AbstractVectorThrusterModel extends DefaultedBlockGeoModel<AbstractVectorThrusterBlockEntity> {

    public AbstractVectorThrusterModel() {
        super(new ResourceLocation(vsie.ID,"vector_thruster"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractVectorThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic_vector" -> new ResourceLocation(vsie.ID, "geo/block/basic_vector_thruster.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractVectorThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic_vector" -> new ResourceLocation(vsie.ID, "textures/block/basic_vector_thruster.png");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractVectorThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic_vector" -> new ResourceLocation(vsie.ID, "animations/block/basic_vector_thruster_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    // 功能：按实例缓存平滑插值状态，避免多个矢量推进器共享同一组插值变量导致互相串扰
    private final Map<Long, float[]> smoothStateByInstance = new HashMap<>();

    @Override
    public void setCustomAnimations(AbstractVectorThrusterBlockEntity animatable, long instanceId, AnimationState<AbstractVectorThrusterBlockEntity> animationState) {
        CoreGeoBone spinner = getAnimationProcessor().getBone("spinner");
        CoreGeoBone nozzle = getAnimationProcessor().getBone("nozzle");

        if (spinner == null || nozzle == null)
        {
            return;
        }
        //double targetSpin = animatable.getSpinDegrees();
        //double targetPitch = animatable.getPitchDegrees();
        double targetSpin = getspin(animatable);
        double targetPitch = getpitch(animatable);
        //LogUtils.getLogger().warn("receiving spin:"+targetSpin+"pitch:"+targetPitch);

        // 转换为度数进行插值（rotLerp 专门处理角度循环问题，如从359°到1°不会走长路）
        //float targetSpinDeg = (float) Math.toDegrees(targetSpinRad);
        //float targetPitchDeg = (float) Math.toDegrees(targetPitchRad);

        // 使用 rotLerp 平滑插值（0.1F ~ 0.3F 之间调节平滑程度，值越小越平滑但越慢）
        // 功能：每个方块实体都有独立的 lastSpin/lastPitch，修复“放下第二个后第一个也偏转”的问题
        float[] state = smoothStateByInstance.computeIfAbsent(instanceId, id -> new float[]{0f, 0f});
        float smoothSpinrad = Mth.rotLerp(0.05F, state[0], (float) targetSpin);
        float smoothPitchrad = Mth.rotLerp(0.05F, state[1], (float) targetPitch);

        // 功能：回写当前实例的平滑状态，供下一帧该实例继续插值
        state[0] = smoothSpinrad;
        state[1] = smoothPitchrad;

        // 设置回骨骼（转回弧度）
        spinner.setRotY(smoothSpinrad);
        nozzle.setRotX(smoothPitchrad);
    }

    private boolean controlling(AbstractVectorThrusterBlockEntity animatable) {
        return true;
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
