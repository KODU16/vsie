package com.kodu16.vsie.content.weapon.client;

import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractWeaponModel extends DefaultedBlockGeoModel<AbstractWeaponBlockEntity> {
    public AbstractWeaponModel() {
        super(new ResourceLocation(vsie.ID,"weapon"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractWeaponBlockEntity weapon) {
        return switch (weapon.getweapontype()) {
            case "infra_knife_accelerator" -> new ResourceLocation(vsie.ID, "geo/block/infra_knife_accelerator.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getweapontype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractWeaponBlockEntity weapon) {
        return switch (weapon.getweapontype()) {
            case "infra_knife_accelerator" -> new ResourceLocation(vsie.ID, "textures/block/infra_knife_accelerator.png");
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getweapontype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractWeaponBlockEntity weapon) {
        return switch (weapon.getweapontype()) {
            case "infra_knife_accelerator" -> new ResourceLocation(vsie.ID, "animations/block/infra_knife_accelerator.png");
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getweapontype());
        };
    }
}
