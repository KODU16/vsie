package com.kodu16.vsie.content.controlseat.entity;

import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ControlSeatMountEntityRenderer extends EntityRenderer<ControlSeatMountEntity> {

    public ControlSeatMountEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            ControlSeatMountEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        // invisible seat
    }

    @Override
    public ResourceLocation getTextureLocation(ControlSeatMountEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/entity/sublevel_seat.png");
    }
}
