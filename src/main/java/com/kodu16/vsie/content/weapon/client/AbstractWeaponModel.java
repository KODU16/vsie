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
            case "arc_emitter" -> new ResourceLocation(vsie.ID, "geo/block/arc_emitter.geo.json");
            case "cenix_plasma_cannon" -> new ResourceLocation(vsie.ID, "geo/block/cenix_plasma_cannon.geo.json");
            // 功能：为新武器预留资源路径；模型文件后续由用户补全。
            case "electro_magnet_rail_cannon" -> new ResourceLocation(vsie.ID, "geo/block/electro_magnet_rail_cannon.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getweapontype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractWeaponBlockEntity weapon) {
        return switch (weapon.getweapontype()) {
            case "infra_knife_accelerator" -> new ResourceLocation(vsie.ID, "textures/block/infra_knife_accelerator.png");
            case "arc_emitter" -> new ResourceLocation(vsie.ID, "textures/block/arc_emitter.png");
            case "cenix_plasma_cannon" -> new ResourceLocation(vsie.ID, "textures/block/cenix_plasma_cannon.png");
            // 功能：为新武器预留纹理路径；纹理文件后续由用户补全。
            case "electro_magnet_rail_cannon" -> new ResourceLocation(vsie.ID, "textures/block/electro_magnet_rail_cannon.png");
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getweapontype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractWeaponBlockEntity weapon) {
        return switch (weapon.getweapontype()) {
            case "infra_knife_accelerator" -> new ResourceLocation(vsie.ID, "animations/block/infra_knife_accelerator_anim.json");
            case "arc_emitter" -> new ResourceLocation(vsie.ID, "animations/block/arc_emitter_anim.json");
            case "cenix_plasma_cannon" -> new ResourceLocation(vsie.ID, "animations/block/cenix_plasma_cannon_anim.json");
            // 功能：为新武器预留动画路径；动画文件后续由用户补全。
            case "electro_magnet_rail_cannon" -> new ResourceLocation(vsie.ID, "animations/block/electro_magnet_rail_cannon_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getweapontype());
        };
    }
}
