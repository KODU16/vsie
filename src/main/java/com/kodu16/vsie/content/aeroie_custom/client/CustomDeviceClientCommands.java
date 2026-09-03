package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.vsie;
import com.mojang.brigadier.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Registers the client-only authoring command without requiring server permissions or packets. */
@EventBusSubscriber(modid = vsie.ID, value = Dist.CLIENT)
public final class CustomDeviceClientCommands {
    private CustomDeviceClientCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("aeroie")
                .then(Commands.literal("open_editor")
                        .executes(context -> {
                            Minecraft.getInstance().execute(() ->
                                    Minecraft.getInstance().setScreen(new CustomDeviceEditorScreen()));
                            return Command.SINGLE_SUCCESS;
                        })));
    }
}
