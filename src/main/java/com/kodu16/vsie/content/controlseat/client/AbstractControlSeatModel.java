package com.kodu16.vsie.content.controlseat.client;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractControlSeatModel extends DefaultedBlockGeoModel<ControlSeatBlockEntity> {
    // 当前使用灰白浅蓝配色，其余控制椅配色保留在同目录供后续切换。
    private static final ResourceLocation ACTIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            vsie.ID, "textures/block/control_seat/gray_white_light_blue.png");

    public AbstractControlSeatModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "control_seat"));
    }

    @Override
    public ResourceLocation getModelResource(ControlSeatBlockEntity blockEntity) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/control_seat.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ControlSeatBlockEntity blockEntity) {
        return ACTIVE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ControlSeatBlockEntity blockEntity) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/control_seat_anim.json");
    }
}
