package com.kodu16.vsie;

import com.kodu16.vsie.content.controlseat.server.ControlSeatForceAttachment;
import com.kodu16.vsie.foundation.ModMenuTypes;
import com.kodu16.vsie.network.ModNetworking;
import com.kodu16.vsie.registries.vsieBlockEntities;
import com.kodu16.vsie.registries.vsieBlocks;
import com.kodu16.vsie.registries.vsieDataTickets;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.registries.vsieCreativeTab;
import com.kodu16.vsie.registries.vsieKeyMappings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.simibubi.create.foundation.data.CreateRegistrate;
import org.valkyrienskies.core.api.attachment.AttachmentRegistration;
import org.valkyrienskies.mod.api.ValkyrienSkies;
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
        REGISTRATE.registerEventListeners(modBus);
        //Content
        vsieBlocks.register();
        vsieBlockEntities.register();
        vsieKeyMappings.register(modBus); // 通过传递 modBus 来确保键位注册
        vsieItems.register();
        vsieCreativeTab.register(modBus);
        vsieDataTickets.registerDataTickets();
        ModMenuTypes.MENUS.register(modBus);
        ModNetworking.register();
        GeckoLib.initialize();

        AttachmentRegistration registration = ValkyrienSkies.api()
                .newAttachmentRegistrationBuilder(ControlSeatForceAttachment.class)
                .useTransientSerializer()
                .build();

        ValkyrienSkies.api().registerAttachment(registration);
    }
    /*// 这个方法会在初始化时调用
    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        // 做一些通用的设置
    }

    // 这个方法会在客户端初始化时调用
    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {
        // 做一些客户端的设置
    }*/
}
