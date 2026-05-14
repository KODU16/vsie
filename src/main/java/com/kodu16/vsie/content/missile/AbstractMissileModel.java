package com.kodu16.vsie.content.missile;

import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

@SuppressWarnings({"removal"})
public class AbstractMissileModel extends DefaultedEntityGeoModel<AbstractMissileEntity> {
    public AbstractMissileModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "missile"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractMissileEntity missile) {
        return switch (missile.getmissiletype()) {
            case "basic_missile" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/entity/basic_missile.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + missile.getmissiletype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractMissileEntity missile) {
        return switch (missile.getmissiletype()) {
            case "basic_missile" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/entity/basic_missile.png");
            default -> throw new IllegalStateException("Unexpected value: " + missile.getmissiletype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractMissileEntity missile) {
        return switch (missile.getmissiletype()) {
            case "basic_missile" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/entity/basic_missile_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + missile.getmissiletype());
        };
    }

}
