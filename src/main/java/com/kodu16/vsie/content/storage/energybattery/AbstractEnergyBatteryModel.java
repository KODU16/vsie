package com.kodu16.vsie.content.storage.energybattery;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。

import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

@SuppressWarnings({"removal"})
public class AbstractEnergyBatteryModel extends DefaultedBlockGeoModel<AbstractEnergyBatteryBlockEntity> {
    public AbstractEnergyBatteryModel() {
        super(ResourceLocation.fromNamespaceAndPath(vsie.ID, "energy_battery"));
    }

    @Override
    public ResourceLocation getModelResource(AbstractEnergyBatteryBlockEntity be) {
        return switch (be.getEnergyBatterytype()) {
            case "small" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/small_energy_battery.geo.json");
            case "medium" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/medium_energy_battery.geo.json");
            case "large" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "geo/block/large_energy_battery.geo.json");
            default -> throw new IllegalStateException("Unexpected value for EnergyBattery");
        };
    }

    @Override
    public ResourceLocation getTextureResource(AbstractEnergyBatteryBlockEntity be) {
        return switch (be.getEnergyBatterytype()) {
            case "small" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/small_energy_battery.png");
            case "medium" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/medium_energy_battery.png");
            case "large" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/block/large_energy_battery.png");
            default -> throw new IllegalStateException("Unexpected value for EnergyBattery");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractEnergyBatteryBlockEntity be) {
        return switch (be.getEnergyBatterytype()) {
            case "small" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/small_energy_battery_anim.json");
            case "medium" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/medium_energy_battery_anim.json");
            case "large" -> ResourceLocation.fromNamespaceAndPath(vsie.ID, "animations/block/large_energy_battery_anim.json");
            default -> throw new IllegalStateException("Unexpected value for EnergyBattery");
        };
    }
}
