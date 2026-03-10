package com.kodu16.vsie.content.screen.client;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractScreenModel extends DefaultedBlockGeoModel<AbstractScreenBlockEntity> {
    public AbstractScreenModel() {
        super(new ResourceLocation(vsie.ID,"screen"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractScreenBlockEntity be) {
        return switch (be.getDisplaytype()) {
            case "basic" -> new ResourceLocation(vsie.ID, "geo/block/basic_screen.geo.json");
            default -> throw new IllegalStateException("Unexpected value for Screen");
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractScreenBlockEntity be) {
        return switch (be.getDisplaytype()) {
            case "basic" -> new ResourceLocation(vsie.ID, "textures/block/basic_screen.png");
            default -> throw new IllegalStateException("Unexpected value for Screen");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractScreenBlockEntity be) {
        return switch (be.getDisplaytype()) {
            case "basic" -> new ResourceLocation(vsie.ID, "animations/block/basic_screen_anim.json");
            default -> throw new IllegalStateException("Unexpected value for Screen");
        };
    }

    @Override
    public void setCustomAnimations(AbstractScreenBlockEntity animatable, long instanceId, AnimationState<AbstractScreenBlockEntity> animationState) {
        CoreGeoBone screen = getAnimationProcessor().getBone("screen");
        if(screen!=null) {
            screen.setRotX(getspinX(animatable)/(180/ Mth.PI));
            screen.setRotY(getspinY(animatable)/(180/ Mth.PI));
            screen.setPosX(getoffsetX(animatable));
            screen.setPosY(getoffsetY(animatable));
            screen.setPosZ(getoffsetZ(animatable));
        }
    }

    private Integer getspinX(AbstractScreenBlockEntity animatable) {
        Integer x = animatable.getAnimData(AbstractScreenBlockEntity.SPINX);
        if(x != null) {
            return x;
        }
        return 0;
    }
    private Integer getspinY(AbstractScreenBlockEntity animatable) {
        Integer y = animatable.getAnimData(AbstractScreenBlockEntity.SPINY);
        if(y != null) {
            return y;
        }
        return 0;
    }
    private Integer getoffsetX(AbstractScreenBlockEntity animatable) {
        Integer ox = animatable.getAnimData(AbstractScreenBlockEntity.OFFSETX);
        if(ox != null) {
            return ox;
        }
        return 0;
    }
    private Integer getoffsetY(AbstractScreenBlockEntity animatable) {
        Integer oy = animatable.getAnimData(AbstractScreenBlockEntity.OFFSETY);
        if(oy != null) {
            return oy;
        }
        return 0;
    }
    private Integer getoffsetZ(AbstractScreenBlockEntity animatable) {
        Integer oz = animatable.getAnimData(AbstractScreenBlockEntity.OFFSETZ);
        if(oz != null) {
            return oz;
        }
        return 0;
    }
}
