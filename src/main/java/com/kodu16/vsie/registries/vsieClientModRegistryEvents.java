package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntityRenderer;
import com.kodu16.vsie.content.controlseat.gui.ControlSeatWarpScreen;
import com.kodu16.vsie.content.item.IFF.IFFScreen;
import com.kodu16.vsie.content.item.shieldtool.shieldtoolScreen;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreScreen;
import com.kodu16.vsie.content.missile.AbstractMissileGeoRenderer;
import com.kodu16.vsie.content.particle.CannonMuzzleSmokeParticle;
import com.kodu16.vsie.content.particle.ShieldParticle;
import com.kodu16.vsie.content.screen.client.ScreenScreen;
import com.kodu16.vsie.content.storage.ammobox.AmmoBoxScreen;
import com.kodu16.vsie.content.thruster.client.ThrusterScreen;
import com.kodu16.vsie.content.turret.client.TurretScreen;
import com.kodu16.vsie.content.turret.heavyturret.HeavyTurretScreen;
import com.kodu16.vsie.content.warpprojectile.WarpProjectileRenderer;
import com.kodu16.vsie.content.weapon.client.WeaponScreen;
import com.kodu16.vsie.foundation.client.model.GuiInventoryIconModel;
import com.kodu16.vsie.vsie;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
@SuppressWarnings("removal")
@EventBusSubscriber(modid = vsie.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class vsieClientModRegistryEvents {
    private static final Map<String, String> SIMPLIFIED_INVENTORY_ICON_ITEMS = Map.ofEntries(
            Map.entry("arc_emitter", "red"),
            Map.entry("basic_ciws", "green"),
            Map.entry("basic_missile_launcher", "red"),
            Map.entry("basic_thruster", "blue"),
            Map.entry("basic_vector_thruster", "blue"),
            Map.entry("cenix_plasma_cannon", "red"),
            Map.entry("control_seat", "blue"),
            Map.entry("electro_magnet_rail_accelerator", "blue"),
            Map.entry("electro_magnet_rail_cannon", "red"),
            Map.entry("electro_magnet_rail_core", "blue"),
            Map.entry("electro_magnet_rail_top", "blue"),
            Map.entry("heavy_electromagnet_turret", "green"),
            Map.entry("heavy_laser_turret", "green"),
            Map.entry("infra_knife_accelerator", "red"),
            Map.entry("large_energy_battery", "yellow"),
            Map.entry("large_fueltank", "yellow"),
            Map.entry("large_thruster", "blue"),
            Map.entry("medium_energy_battery", "yellow"),
            Map.entry("medium_fueltank", "yellow"),
            Map.entry("medium_laser_turret", "green"),
            Map.entry("medium_thruster", "blue"),
            Map.entry("particle_turret", "green"),
            Map.entry("redstone_relay", "red"),
            Map.entry("small_energy_battery", "yellow"),
            Map.entry("small_fueltank", "yellow"),
            Map.entry("small_laser_turret", "green"),
            Map.entry("verticle_launching_slot", "red")
    );
    private static final List<String> HELD_CONTAINER_COLORS = List.of("red", "blue", "green", "yellow");

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.TURRET_MENU.get(), TurretScreen::new);
        event.register(ModMenuTypes.HEAVY_TURRET_MENU.get(), HeavyTurretScreen::new);
        event.register(ModMenuTypes.WEAPON_MENU.get(), WeaponScreen::new);
        event.register(ModMenuTypes.IFF_MENU.get(), IFFScreen::new);
        event.register(ModMenuTypes.SHIELD_TOOL_MENU.get(), shieldtoolScreen::new);
        event.register(ModMenuTypes.SCREEN_MENU.get(), ScreenScreen::new);
        event.register(ModMenuTypes.AMMO_BOX_MENU.get(), AmmoBoxScreen::new);
        event.register(ModMenuTypes.CONTROL_SEAT_WARP_MENU.get(), ControlSeatWarpScreen::new);
        event.register(ModMenuTypes.ELECTRO_MAGNET_RAIL_CORE_MENU.get(), ElectroMagnetRailCoreScreen::new);
        event.register(ModMenuTypes.THRUSTER_MENU.get(), ThrusterScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(vsieEntities.BASIC_MISSILE.get(), AbstractMissileGeoRenderer::new);
        event.registerEntityRenderer(vsieEntities.WARP_PROJECTILE.get(), WarpProjectileRenderer::new);
        event.registerEntityRenderer(vsieEntities.CONTROL_SEAT_MOUNT_ENTITY.get(), ControlSeatMountEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.SHIELD.get(), ShieldParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.CANNON_MUZZLE_SMOKE.get(), CannonMuzzleSmokeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (String itemId : SIMPLIFIED_INVENTORY_ICON_ITEMS.keySet()) {
            event.register(ModelResourceLocation.standalone(guiIconModelId(itemId)));
        }
        for (String color : HELD_CONTAINER_COLORS) {
            event.register(ModelResourceLocation.standalone(heldContainerModelId(color)));
        }
    }

    @SubscribeEvent
    public static void replaceInventoryModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();

        // Function: GUI uses simplified icons, while hand rendering uses shared plain color containers.
        for (Map.Entry<String, String> entry : SIMPLIFIED_INVENTORY_ICON_ITEMS.entrySet()) {
            String itemId = entry.getKey();
            ModelResourceLocation heldLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(vsie.ID, itemId));
            ModelResourceLocation guiLocation = ModelResourceLocation.standalone(guiIconModelId(itemId));
            ModelResourceLocation heldContainerLocation = ModelResourceLocation.standalone(heldContainerModelId(entry.getValue()));
            BakedModel heldModel = models.get(heldLocation);
            BakedModel guiModel = models.get(guiLocation);
            BakedModel heldContainerModel = models.get(heldContainerLocation);
            if (heldModel != null && guiModel != null && heldContainerModel != null) {
                models.put(heldLocation, new GuiInventoryIconModel(heldModel, guiModel, heldContainerModel));
            }
        }
    }

    private static ResourceLocation guiIconModelId(String itemId) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "item/gui_icons/" + itemId);
    }

    private static ResourceLocation heldContainerModelId(String color) {
        return ResourceLocation.fromNamespaceAndPath(vsie.ID, "block/containers/general/" + color + "_container");
    }
}
