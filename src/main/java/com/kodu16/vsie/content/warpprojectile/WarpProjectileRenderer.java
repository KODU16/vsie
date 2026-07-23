package com.kodu16.vsie.content.warpprojectile;

import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings({"removal"})
public class WarpProjectileRenderer<T extends WarpProjecTileEntity> extends EntityRenderer<T> {

    public static final ResourceLocation LASER_TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/entity/bullet.png");

    public WarpProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public boolean shouldRender(T warpProjectile, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        // Function: warp projectile FX must stay visible regardless of entity render distance or frustum culling.
        return true;
    }

    @Override
    public void render(T pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T pEntity) {
        return LASER_TEXTURE;
    }
}
