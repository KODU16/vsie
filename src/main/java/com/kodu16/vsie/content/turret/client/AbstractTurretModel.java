package com.kodu16.vsie.content.turret.client;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.network.turret.TurretFirePointC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractTurretModel extends DefaultedBlockGeoModel<AbstractTurretBlockEntity> {
    private static final float DEFAULT_ROTATION_LERP = 0.1F;
    private static final float CIWS_ROTATION_LERP = 0.45F;

    public AbstractTurretModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "turret"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractTurretBlockEntity abstractTurretBlockEntity) {
        return switch (abstractTurretBlockEntity.getturrettype()) {
            case "medium_laser" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/medium_laser_turret.geo.json");
            case "particle" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/particle_turret.geo.json");
            case "basic_ciws" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/basic_ciws.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + abstractTurretBlockEntity.getturrettype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractTurretBlockEntity abstractTurretBlockEntity) {
        return switch (abstractTurretBlockEntity.getturrettype()) {
            case "medium_laser" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/medium_laser_turret.png");
            case "particle" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/particle_turret.png");
            case "basic_ciws" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/basic_ciws.png");
            default -> throw new IllegalStateException("Unexpected value: " + abstractTurretBlockEntity.getturrettype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractTurretBlockEntity abstractTurretBlockEntity) {
        return switch (abstractTurretBlockEntity.getturrettype()) {
            case "medium_laser" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/medium_laser_anim.json");
            case "particle" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/turret/particle_turret_anim.json");
            case "basic_ciws" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/turret/basic_ciws_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + abstractTurretBlockEntity.getturrettype());
        };
    }

    @Override
    public void setCustomAnimations(AbstractTurretBlockEntity animatable, long instanceId, AnimationState<AbstractTurretBlockEntity> animationState) {
        GeoBone turret = getAnimationProcessor().getBone("turret");
        GeoBone cannon = getAnimationProcessor().getBone("cannon");
        GeoBone firepoint = getAnimationProcessor().getBone("firepoint");
        if (firepoint != null) {
            // Track firepoint matrices so the render layer can send the muzzle position to the server.
            firepoint.setTrackingMatrices(true);
        }
        if(turret != null && cannon != null) {
            float rotationLerp = getRotationLerp(animatable);
            float xRot = lerp(animatable.prevxrot, getX(animatable), rotationLerp);
            float yRot = lerp(animatable.prevyrot, getY(animatable), rotationLerp);
            animatable.prevxrot = xRot;
            animatable.prevyrot = yRot;
            cannon.setRotX(xRot);
            turret.setRotY(yRot);
        }
    }

    private float getRotationLerp(AbstractTurretBlockEntity animatable) {
        // Function: CIWS needs a faster visual response than heavier turrets to avoid muzzle lag while tracking.
        return "basic_ciws".equals(animatable.getturrettype()) ? CIWS_ROTATION_LERP : DEFAULT_ROTATION_LERP;
    }

    private float lerp(float start, float end, float amount) {
        return Mth.rotLerp(amount, start * Mth.RAD_TO_DEG, end * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }

    private float getX(AbstractTurretBlockEntity animatable) {
        Float x = animatable.getAnimData(AbstractTurretBlockEntity.XROT);
        if(x != null) {
            return x;
        }
        return 0;
    }

    private float getY(AbstractTurretBlockEntity animatable) {
        Float y = animatable.getAnimData(AbstractTurretBlockEntity.YROT);
        if(y != null) {
            return y;
        }
        return 0;
    }

}
