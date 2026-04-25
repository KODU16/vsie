package com.kodu16.vsie.content.storage.fueltank;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。

import com.kodu16.vsie.content.storage.energybattery.AbstractEnergyBatteryBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractFuelTankModel extends DefaultedBlockGeoModel<AbstractFuelTankBlockEntity> {
    public AbstractFuelTankModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "fueltank"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractFuelTankBlockEntity be) {
        return switch (be.getFuelTanktype()) {
            case "small" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/small_fueltank.geo.json");
            case "medium" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/medium_fueltank.geo.json");
            case "large" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/large_fueltank.geo.json");
            default -> throw new IllegalStateException("Unexpected value for fueltank");
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractFuelTankBlockEntity be) {
        return switch (be.getFuelTanktype()) {
            case "small" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/small_fueltank.png");
            case "medium" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/medium_fueltank.png");
            case "large" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/large_fueltank.png");
            default -> throw new IllegalStateException("Unexpected value for fueltank");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractFuelTankBlockEntity be) {
        return switch (be.getFuelTanktype()) {
            case "small" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/small_fueltank_anim.json");
            case "medium" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/medium_fueltank_anim.json");
            case "large" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/large_fueltank_anim.json");
            default -> throw new IllegalStateException("Unexpected value for fueltank");
        };
    }
}
