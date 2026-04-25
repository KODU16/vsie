package com.kodu16.vsie.utility;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.kodu16.vsie.network.fuel.SyncThrusterFuelsPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.fuel.ThrusterFuelManager;
import com.kodu16.vsie.vsie;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

// 功能：在 NeoForge 1.21.1 的 GAME 总线上处理资源重载与玩家登录同步事件。
@EventBusSubscriber(modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
public class vsieForgeEvents {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ThrusterFuelManager());
    }

    //Sync thruster fuels for goggles & particles on client side
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.sendToPlayer(SyncThrusterFuelsPacket.create(ThrusterFuelManager.getFuelPropertiesMap()), player);
        }
    }
}
