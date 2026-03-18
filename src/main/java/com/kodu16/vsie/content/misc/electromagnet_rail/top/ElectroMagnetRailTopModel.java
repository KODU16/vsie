package com.kodu16.vsie.content.misc.electromagnet_rail.top;


import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
@SuppressWarnings("removal")
public class ElectroMagnetRailTopModel extends DefaultedBlockGeoModel<ElectroMagnetRailTopBlockEntity> {
    public ElectroMagnetRailTopModel() {
        super(new ResourceLocation(vsie.ID,"heavy_electromagnet_turret"));
    }
    @Override
    public ResourceLocation getModelResource(ElectroMagnetRailTopBlockEntity Top) {
        return new ResourceLocation(vsie.ID, "geo/block/electro_magnet_rail_top.geo.json");
    }
    public ResourceLocation getTextureResource(ElectroMagnetRailTopBlockEntity Top) {
        return new ResourceLocation(vsie.ID, "textures/block/electro_magnet_rail_top.png");
    }
    public ResourceLocation getAnimationResource(ElectroMagnetRailTopBlockEntity Top) {
        return new ResourceLocation(vsie.ID, "animations/block/electro_magnet_rail_top_anim.json");
    }

    @Override
    public void setCustomAnimations(ElectroMagnetRailTopBlockEntity animatable, long instanceId, AnimationState<ElectroMagnetRailTopBlockEntity> animationState){
        CoreGeoBone railleft = getAnimationProcessor().getBone("railleft");
        CoreGeoBone railright = getAnimationProcessor().getBone("railright");
        if(railleft != null && railright != null) {
            // 功能：仅在 top 被 core 成功检测并绑定时向两侧展开滑轨，否则平滑回收到中心位置。
            float targetOffsetX = animatable.isBoundToCore() ? 58.0f : 0.0f;
            float smoothOffsetX = lerpOffset(animatable.prevRailOffsetX, targetOffsetX);
            animatable.prevRailOffsetX = smoothOffsetX;

            // 功能：left/right 同名骨骼沿 X 轴对称移动，与 core 保持一致的展开/收回动画。
            railleft.setPosX(-smoothOffsetX);
            railright.setPosX(smoothOffsetX);
        }
    }

    // 功能：使用线性插值平滑 top 滑轨位移，避免绑定和解绑时骨骼瞬间跳变。
    private float lerpOffset(float start, float end) {
        return Mth.lerp(0.12F, start, end);
    }
}
