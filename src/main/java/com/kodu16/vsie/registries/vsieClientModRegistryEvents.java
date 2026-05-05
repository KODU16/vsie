package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntityRenderer;
import com.kodu16.vsie.content.controlseat.gui.ControlSeatWarpScreen;
import com.kodu16.vsie.content.item.IFF.IFFScreen;
import com.kodu16.vsie.content.item.shieldtool.shieldtoolScreen;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreScreen;
import com.kodu16.vsie.content.missile.AbstractMissileGeoRenderer;
import com.kodu16.vsie.content.particle.ShieldParticle;
import com.kodu16.vsie.content.screen.client.ScreenScreen;
import com.kodu16.vsie.content.storage.ammobox.AmmoBoxScreen;
import com.kodu16.vsie.content.turret.client.TurretScreen;
import com.kodu16.vsie.content.turret.heavyturret.HeavyTurretScreen;
import com.kodu16.vsie.content.warpprojectile.WarpProjectileRenderer;
import com.kodu16.vsie.content.weapon.client.WeaponScreen;
import com.kodu16.vsie.vsie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
@SuppressWarnings("removal")
@EventBusSubscriber(modid = vsie.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class vsieClientModRegistryEvents {

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
    }
}
