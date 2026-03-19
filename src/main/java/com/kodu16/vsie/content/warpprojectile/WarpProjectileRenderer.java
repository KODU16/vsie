package com.kodu16.vsie.content.warpprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

// 功能：提供一个空渲染器，让 warp projectile 仅依赖 Photon 特效显示，避免缺失实体渲染注册导致客户端报错。
public class WarpProjectileRenderer extends EntityRenderer<WarpProjecTileEntity> {

    public WarpProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WarpProjecTileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 功能：不绘制实体模型，只保留实体生命周期与特效驱动。
    }

    @Override
    public ResourceLocation getTextureLocation(WarpProjecTileEntity entity) {
        // 功能：空渲染器不使用贴图，返回原版缺失贴图占位资源即可。
        return new ResourceLocation("minecraft", "textures/misc/unknown_pack.png");
    }
}
