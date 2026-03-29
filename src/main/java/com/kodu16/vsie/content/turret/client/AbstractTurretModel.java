package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.network.turret.TurretFirePointC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractTurretModel extends DefaultedBlockGeoModel<AbstractTurretBlockEntity> {

    public AbstractTurretModel() {
        super(new ResourceLocation(vsie.ID,"turret"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractTurretBlockEntity abstractTurretBlockEntity) {
        return switch (abstractTurretBlockEntity.getturrettype()) {
            case "medium_laser" -> new ResourceLocation(vsie.ID, "geo/block/medium_laser_turret.geo.json");
            case "particle" -> new ResourceLocation(vsie.ID, "geo/block/particle_turret.geo.json");
            case "basic_ciws" -> new ResourceLocation(vsie.ID, "geo/block/basic_ciws.geo.json");
            default -> throw new IllegalStateException("Unexpected value: " + abstractTurretBlockEntity.getturrettype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractTurretBlockEntity abstractTurretBlockEntity) {
        return switch (abstractTurretBlockEntity.getturrettype()) {
            case "medium_laser" -> new ResourceLocation(vsie.ID, "textures/block/medium_laser_turret.png");
            case "particle" -> new ResourceLocation(vsie.ID, "textures/block/particle_turret.png");
            case "basic_ciws" -> new ResourceLocation(vsie.ID, "textures/block/basic_ciws.png");
            default -> throw new IllegalStateException("Unexpected value: " + abstractTurretBlockEntity.getturrettype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractTurretBlockEntity abstractTurretBlockEntity) {
        return switch (abstractTurretBlockEntity.getturrettype()) {
            case "medium_laser" -> new ResourceLocation(vsie.ID, "animations/block/medium_laser_anim.json");
            case "particle" -> new ResourceLocation(vsie.ID, "animations/block/turret/particle_turret_anim.json");
            case "basic_ciws" -> new ResourceLocation(vsie.ID, "animations/block/basic_ciws_anim.json");
            default -> throw new IllegalStateException("Unexpected value: " + abstractTurretBlockEntity.getturrettype());
        };
    }

    @Override
    public void setCustomAnimations(AbstractTurretBlockEntity animatable, long instanceId, AnimationState<AbstractTurretBlockEntity> animationState) {
        CoreGeoBone turret = getAnimationProcessor().getBone("turret");
        CoreGeoBone cannon = getAnimationProcessor().getBone("cannon");
        if(turret != null && cannon != null) {
            float xRot = lerp(animatable.prevxrot, getX(animatable));
            float yRot = lerp(animatable.prevyrot, getY(animatable));
            animatable.prevxrot = xRot;
            animatable.prevyrot = yRot;
            cannon.setRotX(xRot);
            turret.setRotY(yRot);
        }
        CoreGeoBone firepoint = getAnimationProcessor().getBone("cannonend");
        if (firepoint != null && "particle".equals(animatable.getturrettype())) {
            // 功能：读取 cannonend 骨骼坐标并通过 C2S 发给服务端，作为粒子炮子弹生成点。
            Vector3d postofire = new Vector3d(firepoint.getPosX(), firepoint.getPosY(), firepoint.getPosZ());
            ModNetworking.CHANNEL.sendToServer(new TurretFirePointC2SPacket(animatable.getBlockPos(), postofire));
        }
    }
    private float lerp(float start, float end) {
        return Mth.rotLerp(0.1F, start * Mth.RAD_TO_DEG, end * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
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
