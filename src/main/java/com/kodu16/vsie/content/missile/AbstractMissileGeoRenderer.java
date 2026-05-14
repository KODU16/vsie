package com.kodu16.vsie.content.missile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AbstractMissileGeoRenderer extends GeoEntityRenderer<AbstractMissileEntity> {
    public AbstractMissileGeoRenderer(EntityRendererProvider.Context context){
        super(context, new AbstractMissileModel());
    }

    @Override
    protected void applyRotations(AbstractMissileEntity missile, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float scale) {
        Vec3 movement = missile.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 direction = movement.normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(
                new Vector3f(0.0F, 0.0F, -1.0F),
                new Vector3f((float) direction.x, (float) direction.y, (float) direction.z)
        );
        // Function: basic missile Geo models point forward on local -Z, so rotate the whole model toward velocity.
        poseStack.mulPose(rotation);
    }
}
