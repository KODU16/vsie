package com.kodu16.vsie.content.controlseat.client;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractControlSeatModel extends DefaultedBlockGeoModel<AbstractControlSeatBlockEntity> {
    public AbstractControlSeatModel() {
        super(new ResourceLocation(vsie.ID,"control_seat"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractControlSeatBlockEntity be) {
        return switch (be.getcontrolseattype()) {
            case "control_seat" -> new ResourceLocation(vsie.ID, "geo/block/control_seat.geo.json");
            default -> throw new IllegalStateException("Unexpected value for controlseat");
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractControlSeatBlockEntity be) {
        return switch (be.getcontrolseattype()) {
            case "control_seat" -> new ResourceLocation(vsie.ID, "textures/block/control_seat.png");
            default -> throw new IllegalStateException("Unexpected value for controlseat");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractControlSeatBlockEntity be) {
        return switch (be.getcontrolseattype()) {
            case "control_seat" -> new ResourceLocation(vsie.ID, "animations/block/control_seat_anim.json");
            default -> throw new IllegalStateException("Unexpected value for controlseat");
        };
    }
}
