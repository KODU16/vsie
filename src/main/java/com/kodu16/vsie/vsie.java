package com.kodu16.vsie;

import com.kodu16.vsie.content.screen.server.ServerInfoGetter;
import com.kodu16.vsie.registries.ModMenuTypes;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.ModParticleTypes;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.kodu16.vsie.registries.vsieBlocks;
import com.kodu16.vsie.registries.vsieCreativeTab;
import com.kodu16.vsie.registries.vsieDataTickets;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieFluids;
import com.kodu16.vsie.registries.vsieItems;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import software.bernie.geckolib.GeckoLib;

@Mod(vsie.ID)
@SuppressWarnings({"removal"})
public class vsie {
    public static final String ID = "vsie";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
    public static CreateRegistrate registrate() { return REGISTRATE; }

    public static boolean debug = false;
    public static final boolean constDebug = false;

    public vsie(IEventBus modBus) {
        REGISTRATE.registerEventListeners(modBus);

        vsieBlocks.register();
        vsieBlockEntities.register();
        vsieEntities.register();
        vsieFluids.register();
        vsieItems.register();
        vsieCreativeTab.register(modBus);
        vsieDataTickets.registerDataTickets();
        ModMenuTypes.MENUS.register(modBus);
        ModParticleTypes.register(modBus);
        modBus.addListener(vsieBlockEntities::registerCapabilities);
        ModNetworking.register(modBus);

        NeoForge.EVENT_BUS.addListener(ServerInfoGetter::onSablePostPhysicsTick);
    }
}
