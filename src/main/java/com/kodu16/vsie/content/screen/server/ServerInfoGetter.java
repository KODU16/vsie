package com.kodu16.vsie.content.screen.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import com.kodu16.vsie.compat.SimulatedProjectCompat;


public class ServerInfoGetter {


    public static double getServerTPS(Level level) {
        MinecraftServer server = level.getServer();
        long[] times = server.tickTimes;
        if (times == null) return 0;

        long total = 0;
        for (long time : times) {
            total += time;
        }

        double mspt = (total / (double) times.length) / 1_000_000.0;

        return Math.min(20.0, 1000.0 / mspt);
    }

    public static int getServerPhysTPS(Level level) {
        // 功能：改为从 Simulated-Project 兼容层读取物理 TPS。
        return SimulatedProjectCompat.getPhysicsTps(level.getServer());
    }

    public static long[] getJVM() {//跑在服务器就是服务端JVM，跑在客户端就是客户端JVM内存
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return new long[]{used,max};
    }
}
