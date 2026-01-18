package com.kodu16.vsie;

import com.kodu16.vsie.content.turret.client.TurretScreen;
import com.kodu16.vsie.content.weapon.client.WeaponScreen;
import com.kodu16.vsie.foundation.ModMenuTypes;
import com.kodu16.vsie.registries.vsieBlockEntities;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

@Mod.EventBusSubscriber(modid = vsie.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class vsieClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.TURRET_MENU.get(), TurretScreen::new)
        );
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.WEAPON_MENU.get(), WeaponScreen::new)
        );
    }
}

