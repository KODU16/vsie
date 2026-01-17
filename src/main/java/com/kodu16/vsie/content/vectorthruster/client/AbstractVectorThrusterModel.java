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

    private float lastSpin = 0f;
    private float lastPitch = 0f;

    @Override
    public void setCustomAnimations(AbstractVectorThrusterBlockEntity animatable, long instanceId, AnimationState<AbstractVectorThrusterBlockEntity> animationState) {
        CoreGeoBone spinner = getAnimationProcessor().getBone("spinner");
        CoreGeoBone nozzle = getAnimationProcessor().getBone("nozzle");

        if (spinner == null || nozzle == null) return;

        if (controlling(animatable)) {
            // 获取目标角度（弧度）
            double targetSpin = getspin(animatable);
            double targetPitch = getpitch(animatable);

            // 转换为度数进行插值（rotLerp 专门处理角度循环问题，如从359°到1°不会走长路）
            //float targetSpinDeg = (float) Math.toDegrees(targetSpinRad);
            //float targetPitchDeg = (float) Math.toDegrees(targetPitchRad);

            // 使用 rotLerp 平滑插值（0.1F ~ 0.3F 之间调节平滑程度，值越小越平滑但越慢）
            float smoothSpinDeg = Mth.rotLerp(0.05F, lastSpin, (float) targetSpin);
            float smoothPitchDeg = Mth.rotLerp(0.05F, lastPitch, (float) targetPitch);

            // 更新上次的值
            lastSpin = smoothSpinDeg;
            lastPitch = smoothPitchDeg;

            // 设置回骨骼（转回弧度）
            spinner.setRotZ(-1*(float) Math.toRadians(smoothSpinDeg));
            nozzle.setRotX((float) Math.toRadians(smoothPitchDeg));

        } else {
            // 不受控制时，可以选择缓慢归零或保持最后状态
            // 这里选择缓慢归零，显得更自然
            //lastSpin = Mth.rotLerp(0.15F, lastSpin, 0f);
            //lastPitch = Mth.rotLerp(0.15F, lastPitch, 0f);

            spinner.setRotZ((float) Math.toRadians(lastSpin));
            nozzle.setRotX((float) Math.toRadians(lastPitch));
        }
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
