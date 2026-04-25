package com.kodu16.vsie;

import com.kodu16.vsie.registries.ModMenuTypes;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.ModParticleTypes;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.kodu16.vsie.registries.vsieBlocks;
import com.kodu16.vsie.registries.vsieDataTickets;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieFluids;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.registries.vsieCreativeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.kodu16.vsie.compat.SimulatedProjectCompat;
import software.bernie.geckolib.GeckoLib;

@Mod(vsie.ID)
@SuppressWarnings({"removal"})
public class vsie {
    public static final String ID = "vsie";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);
    public static CreateRegistrate registrate() { return REGISTRATE; }

    public static boolean debug = false;
    public static final boolean constDebug = false; //To produce debug and non-debug builds :P

    // 功能：NeoForge 1.21.1 推荐直接在 Mod 构造器注入 IEventBus，避免依赖旧的 FMLJavaModLoadingContext 取总线方式。
    public vsie(IEventBus modBus) {
        // 功能：将 Registrate 的所有注册监听器挂到 NeoForge 的 Mod 事件总线。
        REGISTRATE.registerEventListeners(modBus);
        //Content
        vsieBlocks.register();
        vsieBlockEntities.register();
        vsieEntities.register();
        //vsieKeyMappings.register(modBus); // 不要重复注册，keymappings里面是注册好的
        vsieFluids.register();
        vsieItems.register();
        vsieCreativeTab.register(modBus);
        vsieDataTickets.registerDataTickets();
        ModMenuTypes.MENUS.register(modBus);
        ModParticleTypes.register(modBus);
        ModNetworking.register();
        GeckoLib.initialize();

        // 功能：通过兼容层统一注册物理附件，迁移阶段由 Simulated-Project 接管实现。
        SimulatedProjectCompat.registerControlSeatAttachment();
    }
}
