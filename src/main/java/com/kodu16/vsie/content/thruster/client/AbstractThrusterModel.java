package com.kodu16.vsie.content.thruster.client;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。


import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractThrusterModel extends DefaultedBlockGeoModel<AbstractThrusterBlockEntity> {

    public AbstractThrusterModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "thruster"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/basic_thruster.geo.json");
            case "medium" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/medium_thruster.geo.json");
            case "large" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/large_thruster.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/basic_thruster.png");
            case "medium" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/medium_thruster.png");
            case "large" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/large_thruster.png");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/basic_thruster_anim.json");
            case "medium" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/medium_thruster_anim.json");
            case "large" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/large_thruster_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }
}
