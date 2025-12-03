package com.kodu16.vsie;

import com.kodu16.vsie.foundation.ModMenuTypes;
import com.kodu16.vsie.network.ModNetworking;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.kodu16.vsie.registries.vsieBlocks;
import com.kodu16.vsie.registries.vsieDataTickets;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.registries.vsieCreativeTab;
import com.kodu16.vsie.registries.vsieKeyMappings;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.simibubi.create.foundation.data.CreateRegistrate;
import software.bernie.geckolib.GeckoLib;

@Mod(vsie.ID)
@SuppressWarnings({"removal"})
public class vsie {
    public static final String ID = "vsie";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);
    public static CreateRegistrate registrate() { return REGISTRATE; }

    public static boolean debug = false;
    public static final boolean constDebug = false; //To produce debug and non-debug builds :P

    public vsie() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        //Content
        GeckoLib.initialize();
        vsieKeyMappings.register(modBus); // 通过传递 modBus 来确保键位注册
        vsieBlocks.register();
        vsieBlockEntities.register();
        vsieItems.register();
        vsieCreativeTab.register(modBus);
        vsieDataTickets.registerDataTickets();
        ModMenuTypes.MENUS.register(modBus);
        ModNetworking.register();
        REGISTRATE.registerEventListeners(modBus);
    }
}
