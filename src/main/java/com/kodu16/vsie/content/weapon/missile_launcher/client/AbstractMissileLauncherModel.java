package com.kodu16.vsie.content.weapon.missile_launcher.client;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。


import com.kodu16.vsie.content.weapon.missile_launcher.AbstractMissileLauncherBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractMissileLauncherModel extends DefaultedBlockGeoModel<AbstractMissileLauncherBlockEntity> {
    public AbstractMissileLauncherModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "missile_launcher"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractMissileLauncherBlockEntity be) {
        return switch (be.getmissilelaunchertype()) {
            case "basic_missile_launcher" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/basic_missile_launcher.geo.json");
            default -> throw new IllegalStateException("Unexpected valuet");
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractMissileLauncherBlockEntity be) {
        return switch (be.getmissilelaunchertype()) {
            case "basic_missile_launcher" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/basic_missile_launcher.png");
            default -> throw new IllegalStateException("Unexpected value");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractMissileLauncherBlockEntity be) {
        return switch (be.getmissilelaunchertype()) {
            case "basic_missile_launcher" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/basic_missile_launcher_anim.json");
            default -> throw new IllegalStateException("Unexpected value");
        };
    }
}
