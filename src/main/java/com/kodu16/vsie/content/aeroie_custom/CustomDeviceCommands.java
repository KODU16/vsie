package com.kodu16.vsie.content.aeroie_custom;

import com.kodu16.vsie.vsie;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Registers manual maintenance commands for external custom device resources. */
@EventBusSubscriber(modid = vsie.ID)
public final class CustomDeviceCommands {
    private static final int DEFAULT_REFRESH_RADIUS = 64;
    private static final int MAX_REFRESH_RADIUS = 128;

    private CustomDeviceCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("aeroie")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("refresh_custom_devices")
                        .executes(context -> refresh(context.getSource(), DEFAULT_REFRESH_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_REFRESH_RADIUS))
                                .executes(context -> refresh(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "radius"))))));
    }

    private static int refresh(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int removed = CustomDeviceResourceGuard.refreshLoadedDevices(level, center, radius);
        source.sendSuccess(() -> Component.translatable(
                "message.vsie.custom_device.refresh_complete", removed, radius).withStyle(ChatFormatting.RED), false);
        return removed == 0 ? Command.SINGLE_SUCCESS : removed;
    }
}
