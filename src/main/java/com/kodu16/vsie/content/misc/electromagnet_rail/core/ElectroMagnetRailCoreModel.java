package com.kodu16.vsie.content.misc.electromagnet_rail.core;


import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
@SuppressWarnings("removal")
public class ElectroMagnetRailCoreModel extends DefaultedBlockGeoModel<ElectroMagnetRailCoreBlockEntity> {
    public ElectroMagnetRailCoreModel() {
        super(new ResourceLocation(vsie.ID,"heavy_electromagnet_turret"));
    }
    @Override
    public ResourceLocation getModelResource(ElectroMagnetRailCoreBlockEntity core) {
        return new ResourceLocation(vsie.ID, "geo/block/electro_magnet_rail_core.geo.json");
    }
    public ResourceLocation getTextureResource(ElectroMagnetRailCoreBlockEntity core) {
        return new ResourceLocation(vsie.ID, "textures/block/electro_magnet_rail_core.png");
    }
    public ResourceLocation getAnimationResource(ElectroMagnetRailCoreBlockEntity core) {
        return new ResourceLocation(vsie.ID, "animations/block/electro_magnet_rail_core_anim.json");
    }
}
