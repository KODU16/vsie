package com.kodu16.vsie.content.misc.electromagnet_rail.core;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。


import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
@SuppressWarnings("removal")
public class ElectroMagnetRailCoreModel extends DefaultedBlockGeoModel<ElectroMagnetRailCoreBlockEntity> {
    public ElectroMagnetRailCoreModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "heavy_electromagnet_turret"));
    }
    @Override
    public ResourceLocation getModelResource(ElectroMagnetRailCoreBlockEntity core) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/electro_magnet_rail_core.geo.json");
    }
    public ResourceLocation getTextureResource(ElectroMagnetRailCoreBlockEntity core) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/electro_magnet_rail_core.png");
    }
    public ResourceLocation getAnimationResource(ElectroMagnetRailCoreBlockEntity core) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/electro_magnet_rail_core_anim.json");
    }

    @Override
    public void setCustomAnimations(ElectroMagnetRailCoreBlockEntity animatable, long instanceId, AnimationState<ElectroMagnetRailCoreBlockEntity> animationState){
        CoreGeoBone railleft = getAnimationProcessor().getBone("railleft");
        CoreGeoBone railright = getAnimationProcessor().getBone("railright");
        if(railleft != null && railright != null) {
            // 功能：仅在成功绑定到 top 时向两侧展开滑轨，否则平滑回收到中心位置。
            float targetOffsetX = animatable.hasValidTerminalBinding() ? 58.0f : 0.0f;
            float smoothOffsetX = lerpOffset(animatable.prevRailOffsetX, targetOffsetX);
            animatable.prevRailOffsetX = smoothOffsetX;

            // 功能：左/右骨骼沿 X 轴对称移动，形成平滑展开到 -60 / 60 的动画效果。
            railleft.setPosX(-smoothOffsetX);
            railright.setPosX(smoothOffsetX);
        }
    }

    // 功能：使用线性插值平滑滑轨位移，避免绑定成功时瞬间弹开。
    private float lerpOffset(float start, float end) {
        return Mth.lerp(0.12F, start, end);
    }
}
