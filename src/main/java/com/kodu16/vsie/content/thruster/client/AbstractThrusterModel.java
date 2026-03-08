package com.kodu16.vsie.content.thruster.client;


import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractThrusterModel extends DefaultedBlockGeoModel<AbstractThrusterBlockEntity> {

    public AbstractThrusterModel() {
        super(new ResourceLocation(vsie.ID,"thruster"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic" -> new ResourceLocation(vsie.ID, "geo/block/basic_thruster.geo.json");
            case "medium" -> new ResourceLocation(vsie.ID, "geo/block/medium_thruster.geo.json");
            case "large" -> new ResourceLocation(vsie.ID, "geo/block/large_thruster.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic" -> new ResourceLocation(vsie.ID, "textures/block/basic_thruster.png");
            case "medium" -> new ResourceLocation(vsie.ID, "textures/block/medium_thruster.png");
            case "large" -> new ResourceLocation(vsie.ID, "textures/block/large_thruster.png");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractThrusterBlockEntity thruster) {
        return switch (thruster.getthrustertype()) {
            case "basic" -> new ResourceLocation(vsie.ID, "animations/block/basic_thruster_anim.json");
            case "medium" -> new ResourceLocation(vsie.ID, "animations/block/medium_thruster_anim.json");
            case "large" -> new ResourceLocation(vsie.ID, "animations/block/large_thruster_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + thruster.getthrustertype());
        };
    }
}
