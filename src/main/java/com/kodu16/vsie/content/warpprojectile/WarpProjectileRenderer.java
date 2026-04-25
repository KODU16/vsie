package com.kodu16.vsie.content.warpprojectile;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。

import com.kodu16.vsie.foundation.translucentbeamrendertype;
import com.kodu16.vsie.vsie;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@SuppressWarnings({"removal"})
public class WarpProjectileRenderer<T extends WarpProjecTileEntity> extends EntityRenderer<T> {

    public static final ResourceLocation LASER_TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/entity/bullet.png");
    // 激光拖尾横向切分数量，值越大越圆滑。
    private static final int TRAIL_SEGMENTS = 8;
    // 激光拖尾长度切分数量，值越大渐变越平滑。
    private static final int TRAIL_LENGTH_SEGMENTS = 10;
    // 激光拖尾起始半径（靠近离子弹本体）。
    private static final float TRAIL_START_RADIUS = 3.6F;
    // 激光拖尾末端半径（远离离子弹）。
    private static final float TRAIL_END_RADIUS = 0.5F;
    private static final float M_2PI = (float) (Math.PI * 2.0);

    public WarpProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(T pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);
    }


    public void vertex(Matrix4f pMatrix, Matrix3f pNormal, VertexConsumer pConsumer, int pX, int pY, int pZ, float pU, float pV, int pNormalX, int pNormalZ, int pNormalY, int pPackedLight) {
        pConsumer.vertex(pMatrix, pX, pY, pZ).color(128, 192, 128, 192).uv(pU, pV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(pPackedLight).normal(pNormal, (float)pNormalX, (float)pNormalY, (float)pNormalZ).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(WarpProjecTileEntity pEntity) {
        return LASER_TEXTURE;
    }
}
