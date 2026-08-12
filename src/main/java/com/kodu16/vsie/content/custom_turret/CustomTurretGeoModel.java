package com.kodu16.vsie.content.custom_turret;

import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

/** Supplies the required invisible root/turret/cannon/long_cannon Gecko bone-group hierarchy. */
public final class CustomTurretGeoModel extends DefaultedBlockGeoModel<CustomTurretBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/custom_turret.geo.json");
    // 空骨骼模型沿用当前控制椅纹理，避免缺少外部 OBJ 时出现紫黑缺失纹理。
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            vsie.ID, "textures/block/control_seat/gray_white_light_blue.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/custom_turret.animation.json");

    public CustomTurretGeoModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "custom_turret"));
    }

    @Override
    public ResourceLocation getModelResource(CustomTurretBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CustomTurretBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CustomTurretBlockEntity animatable) {
        return ANIMATION;
    }
}
