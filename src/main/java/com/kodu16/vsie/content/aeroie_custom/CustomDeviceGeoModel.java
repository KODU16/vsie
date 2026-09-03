package com.kodu16.vsie.content.aeroie_custom;

import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

/** Supplies the invisible Gecko shell used by external custom OBJ skeletons. */
public final class CustomDeviceGeoModel<T extends net.minecraft.world.level.block.entity.BlockEntity
        & software.bernie.geckolib.animatable.GeoAnimatable>
        extends DefaultedBlockGeoModel<T> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/custom_turret.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/control_seat/gray_white_light_blue.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/custom_turret.animation.json");

    public CustomDeviceGeoModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "custom_turret"));
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        if (animatable instanceof CustomDeviceBlockEntity customDevice) {
            CustomDeviceDefinition definition = customDevice.getDefinition();
            if (!definition.geckoModel.isBlank()
                    && CustomDeviceGeoResources.ensureLoaded(definition.deviceType, definition.geckoModel)) {
                return CustomDeviceGeoResources.toResourceLocation(definition.deviceType, definition.geckoModel);
            }
        }
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        if (animatable instanceof CustomDeviceBlockEntity customDevice) {
            String fireAnimation = customDevice.getDefinition().fireAnimation;
            if (!fireAnimation.isBlank()) {
                if (CustomTurretAnimationResources.ensureLoaded(fireAnimation)) {
                    return CustomTurretAnimationResources.toResourceLocation(fireAnimation);
                }
            }
        }
        return ANIMATION;
    }
}
