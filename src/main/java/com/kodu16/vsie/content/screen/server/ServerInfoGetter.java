package com.kodu16.vsie.content.screen.server;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class ServerInfoGetter {


    public static double getServerTPS(Level level) {
        if (level.isClientSide()) return 0;

        MinecraftServer server = level.getServer();
        if (server == null) return 0;

        float mspt = server.getAverageTickTime(); // 每tick耗时(ns)

        return 1000/mspt;
    }

    public static int getServerPhysTPS(Level level) {
        var currentServer = ValkyrienSkiesMod.getCurrentServer();
        if (currentServer == null) return 0;
        var pipeline = VSGameUtilsKt.getVsPipeline(currentServer);
        return (int) pipeline.computePhysTps();
    }

    public static long[] getJVM() {//跑在服务器就是服务端JVM，跑在客户端就是客户端JVM内存
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return new long[]{used,max};
    }
}
