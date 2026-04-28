package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntityRenderer;
import com.kodu16.vsie.content.turret.heavyturret.HeavyTurretScreen;
import com.kodu16.vsie.content.item.IFF.IFFScreen;
import com.kodu16.vsie.content.item.shieldtool.shieldtoolScreen;
import com.kodu16.vsie.content.controlseat.gui.ControlSeatWarpScreen;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreScreen;
import com.kodu16.vsie.content.missile.AbstractMissileGeoRenderer;
import com.kodu16.vsie.content.warpprojectile.WarpProjectileRenderer;
import com.kodu16.vsie.content.screen.client.ScreenScreen;
import com.kodu16.vsie.content.storage.ammobox.AmmoBoxScreen;
import com.kodu16.vsie.content.turret.client.TurretScreen;
import com.kodu16.vsie.content.weapon.client.WeaponScreen;
// 新增导入：粒子相关
import com.kodu16.vsie.content.particle.ShieldParticle;
import com.kodu16.vsie.vsie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

// 功能：在 NeoForge 1.21.1 的 MOD 总线上集中注册客户端菜单、渲染器和粒子工厂。
@EventBusSubscriber(modid = vsie.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class vsieClientModRegistryEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.TURRET_MENU.get(), TurretScreen::new)
        );
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.HEAVY_TURRET_MENU.get(), HeavyTurretScreen::new)
        );
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.WEAPON_MENU.get(), WeaponScreen::new)
        );
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.IFF_MENU.get(), IFFScreen::new)
        );
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.SHIELD_TOOL_MENU.get(), shieldtoolScreen::new)
        );
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.SCREEN_MENU.get(), ScreenScreen::new)
        );
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.AMMO_BOX_MENU.get(), AmmoBoxScreen::new)
        );
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.CONTROL_SEAT_WARP_MENU.get(), ControlSeatWarpScreen::new)
        );
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.ELECTRO_MAGNET_RAIL_CORE_MENU.get(), ElectroMagnetRailCoreScreen::new)
        );
        event.enqueueWork(() ->
                EntityRenderers.register(vsieEntities.BASIC_MISSILE.get(), AbstractMissileGeoRenderer::new)
        );
        event.enqueueWork(() ->
                EntityRenderers.register(vsieEntities.WARP_PROJECTILE.get(), WarpProjectileRenderer::new)
        );
        event.enqueueWork(() ->
                EntityRenderers.register(vsieEntities.CONTROL_SEAT_MOUNT_ENTITY.get(), ControlSeatMountEntityRenderer::new)
        );
        event.enqueueWork(() -> {
            Minecraft.getInstance().particleEngine.register(
                    ModParticleTypes.SHIELD.get(),
                    ShieldParticle.Provider::new   // 你的 Provider 类
            );
        });
    }
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // 关键：注册你的 Provider！
        event.registerSpriteSet(ModParticleTypes.SHIELD.get(), ShieldParticle.Provider::new);
    }


}
