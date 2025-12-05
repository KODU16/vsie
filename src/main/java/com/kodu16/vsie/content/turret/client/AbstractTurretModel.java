package com.kodu16.vsie.content.turret.client;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings({"removal"})
public class AbstractTurretModel extends DefaultedBlockGeoModel<AbstractTurretBlockEntity> {

    private static final ResourceLocation MODEL_MEDIUM_LASER_TURRET = new ResourceLocation(vsie.ID, "geo/block/medium_laser_turret.geo.json");
    private static final ResourceLocation TEXTURE_MEDIUM_LASER_TURRET = new ResourceLocation(vsie.ID, "textures/block/medium_laser_turret.png");
    private static final ResourceLocation ANIMATION_MEDIUM_LASER_TURRET = new ResourceLocation(vsie.ID, "animations/block/medium_laser_anim.json");
    /*private static final ResourceLocation MODEL_ADVANCED = new ResourceLocation(vsie.MOD_ID, "geo/block/advanced_laser_turret.geo.json");
    private static final ResourceLocation MODEL_ELITE = new ResourceLocation(MekanismTurrets.MOD_ID, "geo/block/elite_laser_turret.geo.json");
    private static final ResourceLocation MODEL_ULTIMATE = new ResourceLocation(MekanismTurrets.MOD_ID, "geo/block/ultimate_laser_turret.geo.json");

    private static final ResourceLocation TEXTURE_ADVANCED = new ResourceLocation(MekanismTurrets.MOD_ID, "textures/block/advanced_laser_turret.png");
    private static final ResourceLocation TEXTURE_ELITE = new ResourceLocation(MekanismTurrets.MOD_ID, "textures/block/elite_laser_turret.png");
    private static final ResourceLocation TEXTURE_ULTIMATE = new ResourceLocation(MekanismTurrets.MOD_ID, "textures/block/ultimate_laser_turret.png");

    private static final ResourceLocation ANIMATION_BASIC = new ResourceLocation(MekanismTurrets.MOD_ID, "animations/block/basic_anim.json");
    private static final ResourceLocation ANIMATION_ADVANCED = new ResourceLocation(MekanismTurrets.MOD_ID, "animations/block/advanced_anim.json");
    private static final ResourceLocation ANIMATION_ELITE = new ResourceLocation(MekanismTurrets.MOD_ID, "animations/block/elite_anim.json");
    private static final ResourceLocation ANIMATION_ULTIMATE = new ResourceLocation(MekanismTurrets.MOD_ID, "animations/block/ultimate_anim.json");*/
    public AbstractTurretModel() {
        super(new ResourceLocation(vsie.ID,"turret"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractTurretBlockEntity abstractTurretBlockEntity) {
        return new ResourceLocation(vsie.ID, "geo/block/medium_laser_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AbstractTurretBlockEntity abstractTurretBlockEntity) {
        return new ResourceLocation(vsie.ID, "textures/block/medium_laser_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractTurretBlockEntity abstractTurretBlockEntity) {
        return new ResourceLocation(vsie.ID, "animations/block/medium_laser_anim.json");
    }

    /*@Override
    public ResourceLocation getModelResource(AbstractTurretBlockEntity animatable) {
        return switch (animatable.getturrettype()) {
            case "medium_laser" -> MODEL_MEDIUM_LASER_TURRET;
            default -> throw new IllegalStateException("Unexpected value: " + animatable.getturrettype());
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractTurretBlockEntity animatable) {
        return switch (animatable.getturrettype()) {
            case "medium_laser" -> TEXTURE_MEDIUM_LASER_TURRET;
            default -> throw new IllegalStateException("Unexpected value: " + animatable.getturrettype());
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractTurretBlockEntity animatable) {
        return switch (animatable.getturrettype()) {
            case "medium_laser" -> ANIMATION_MEDIUM_LASER_TURRET;
            default -> throw new IllegalStateException("Unexpected value: " + animatable.getturrettype());
        };
    }*/

    @Override
    public void setCustomAnimations(AbstractTurretBlockEntity animatable, long instanceId, AnimationState<AbstractTurretBlockEntity> animationState) {
        CoreGeoBone turret = getAnimationProcessor().getBone("turret");
        CoreGeoBone cannon = getAnimationProcessor().getBone("cannon");
        if(turret != null && cannon != null) {
            if(hasTarget(animatable)) {
                Vec3 targetPos = new Vec3(targetX(animatable), targetY(animatable), targetZ(animatable));
                Direction direction = animatable.getBlockState().getValue(AbstractTurretBlock.FACING);
                Vec3 center = new Vec3(animatable.currentworldpos.x, animatable.currentworldpos.y, animatable.currentworldpos.z);
                Vector3f deltaPos = getTransform(direction).transform(new Vec3(targetPos.x - center.x, targetPos.y - center.y, targetPos.z - center.z).toVector3f());
                double deltaHorizontal = Math.sqrt(deltaPos.x * deltaPos.x + deltaPos.z * deltaPos.z);
                float xRot = lerp(animatable.xRot0, (float) ((3 * Mth.HALF_PI) + Math.atan2(deltaPos.y, deltaHorizontal)));
                float yRot = lerp(animatable.yRot0, (float) ((3 * Mth.HALF_PI) - Math.atan2(deltaPos.z, deltaPos.x)));
                animatable.xRot0 = xRot;
                animatable.yRot0 = yRot;
                cannon.setRotX(xRot+Mth.HALF_PI);
                turret.setRotY(yRot);
            } else {
                float xRot = lerp(animatable.xRot0, -Mth.HALF_PI);
                float yRot = lerp(animatable.yRot0, 0);
                animatable.xRot0 = xRot;
                animatable.yRot0 = yRot;
                cannon.setRotX(xRot+Mth.HALF_PI);
                turret.setRotY(yRot);
            }
        }
    }

    private Quaternionf getTransform(Direction direction) {
        switch (direction) {
            case NORTH -> {
                return new Quaternionf().rotationX((float) (-Math.PI/2));
            }
            case EAST -> {
                return new Quaternionf().rotationZ((float) (-Math.PI/2));
            }
            case SOUTH -> {
                return new Quaternionf().rotationX((float) (Math.PI/2));
            }
            case WEST -> {
                return new Quaternionf().rotationZ((float) (Math.PI/2));
            }
            case UP -> {
                return new Quaternionf().rotationZ((float) Math.PI);
            }
            case DOWN -> {
                return new Quaternionf();
            }
        }
        return new Quaternionf();
    }
    private float lerp(float start, float end) {
        return Mth.rotLerp(0.1F, start * Mth.RAD_TO_DEG, end * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }

    private boolean hasTarget(AbstractTurretBlockEntity animatable) {
        return Boolean.TRUE.equals(animatable.getAnimData(AbstractTurretBlockEntity.HAS_TARGET));
    }

    private double targetX(AbstractTurretBlockEntity animatable) {
        Double targetX = animatable.getAnimData(AbstractTurretBlockEntity.TARGET_POS_X);
        if(targetX != null) {
            return targetX;
        }
        return 0;
    }
    private double targetY(AbstractTurretBlockEntity animatable) {
        Double targetY = animatable.getAnimData(AbstractTurretBlockEntity.TARGET_POS_Y);
        if(targetY != null) {
            return targetY;
        }
        return 0;
    }
    private double targetZ(AbstractTurretBlockEntity animatable) {
        Double targetZ = animatable.getAnimData(AbstractTurretBlockEntity.TARGET_POS_Z);
        if(targetZ != null) {
            return targetZ;
        }
        return 0;
    }
}
