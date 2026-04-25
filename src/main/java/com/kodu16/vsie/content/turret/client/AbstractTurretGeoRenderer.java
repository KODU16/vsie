package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.network.turret.TurretFirePointC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

import java.util.Optional;

public class AbstractTurretGeoRenderer extends GeoBlockRenderer<AbstractTurretBlockEntity>  {

    public AbstractTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new AbstractTurretModel());

        // 注册 Layer！这是关键，不会触发任何 override
        this.addRenderLayer(new TurretLaserLayer(this));
    }
    @Override
    public void preRender(PoseStack poseStack, AbstractTurretBlockEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue,
                          float alpha) {
        super.preRender(poseStack,animatable,model,bufferSource,buffer,isReRender,partialTick,packedLight,packedOverlay,red,green,blue,alpha);
        Optional<GeoBone> bone = model.getBone("cannonend");
        if(bone.isPresent()){
            GeoBone cannonend = bone.get();
            Vector3d cannonpos = cannonend.getLocalPosition();
            ModNetworking.sendToServer(new TurretFirePointC2SPacket(animatable.getBlockPos(), cannonpos));
        }
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> {
                poseStack.translate(0, 0.5, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(270));
            }
            case WEST -> {
                poseStack.translate(-0.5, 0.5, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            }
            case NORTH -> {
                poseStack.translate(0, 0.5, -0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
            }
            case EAST -> {
                poseStack.translate(0.5, 0.5, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            }
            case UP -> {
                poseStack.translate(0, 1, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
            case DOWN -> {

            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(AbstractTurretBlockEntity be) {
        return true;   // 或者 return distanceSq < 某个超大值 的平方
    }

    @Override
    public boolean shouldRender(AbstractTurretBlockEntity be, Vec3 cameraPos) {
        // 自己写距离判断，比如 256 格以内都渲染
        return true;
    }


}
