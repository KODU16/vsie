package com.kodu16.vsie.content.weapon.client;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。

import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractWeaponModel extends DefaultedBlockGeoModel<AbstractWeaponBlockEntity> {
    public AbstractWeaponModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "weapon"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractWeaponBlockEntity weapon) {
        return switch (weapon.getweapontype()) {
            case "infra_knife_accelerator" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/infra_knife_accelerator.geo.json");
            case "arc_emitter" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/arc_emitter.geo.json");
            case "cenix_plasma_cannon" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/cenix_plasma_cannon.geo.json");
            case "electro_magnet_rail_cannon" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/electro_magnet_rail_cannon.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getweapontype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractWeaponBlockEntity weapon) {
        return switch (weapon.getweapontype()) {
            case "infra_knife_accelerator" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/infra_knife_accelerator.png");
            case "arc_emitter" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/arc_emitter.png");
            case "cenix_plasma_cannon" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/cenix_plasma_cannon.png");
            case "electro_magnet_rail_cannon" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/electro_magnet_rail_cannon.png");
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getweapontype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractWeaponBlockEntity weapon) {
        return switch (weapon.getweapontype()) {
            case "infra_knife_accelerator" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/infra_knife_accelerator_anim.json");
            case "arc_emitter" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/arc_emitter_anim.json");
            case "cenix_plasma_cannon" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/cenix_plasma_cannon_anim.json");
            case "electro_magnet_rail_cannon" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/electro_magnet_rail_cannon_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getweapontype());
        };
    }
}
