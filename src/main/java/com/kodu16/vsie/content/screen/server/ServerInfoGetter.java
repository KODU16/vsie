package com.kodu16.vsie.content.screen.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public class ServerInfoGetter {
    private static final long PHYSICS_RATE_WINDOW_NANOS = 1_000_000_000L;
    private static final long PHYSICS_RATE_STALE_NANOS = 2_000_000_000L;
    private static final Map<ResourceKey<Level>, PhysicsRateSample> PHYSICS_RATE_SAMPLES = new ConcurrentHashMap<>();

    public static double getServerTPS(Level level) {
        if (level == null)
            return 0;

        MinecraftServer server = level.getServer();
        if (server == null)
            return 0;

        long averageTickNanos = server.getAverageTickTimeNanos();
        double targetTps = server.tickRateManager().tickrate();

        if (averageTickNanos <= 0)
            return targetTps;

        return Math.min(targetTps, 1_000_000_000.0 / averageTickNanos);
    }

    public static int getServerPhysTPS(Level level) {
        if (level == null || level.getServer() == null)
            return 0;

        int fallbackRate = estimateConfiguredPhysicsRate(level);
        PhysicsRateSample sample = PHYSICS_RATE_SAMPLES.get(level.dimension());
        if (sample == null)
            return fallbackRate;

        return sample.getRate(System.nanoTime(), fallbackRate);
    }

    public static void onSablePostPhysicsTick(ForgeSablePostPhysicsTickEvent event) {
        PHYSICS_RATE_SAMPLES
            .computeIfAbsent(event.getPhysicsSystem().getLevel().dimension(), key -> new PhysicsRateSample())
            .record(System.nanoTime());
    }

    public static long[] getJVM() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return new long[]{used, max};
    }

    private static int estimateConfiguredPhysicsRate(Level level) {
        SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
        if (physicsSystem == null || physicsSystem.getPaused())
            return 0;

        return Math.max(0, (int) Math.round(getServerTPS(level) * physicsSystem.getConfig().substepsPerTick));
    }

    private static class PhysicsRateSample {
        private long windowStartNanos;
        private long lastEventNanos;
        private int ticksInWindow;
        private int lastCompletedRate;

        synchronized void record(long nowNanos) {
            if (windowStartNanos == 0)
                windowStartNanos = nowNanos;

            long elapsedNanos = nowNanos - windowStartNanos;
            if (elapsedNanos >= PHYSICS_RATE_WINDOW_NANOS) {
                lastCompletedRate = (int) Math.round(ticksInWindow * (double) PHYSICS_RATE_WINDOW_NANOS / elapsedNanos);
                ticksInWindow = 0;
                windowStartNanos = nowNanos;
            }

            ticksInWindow++;
            lastEventNanos = nowNanos;
        }

        synchronized int getRate(long nowNanos, int fallbackRate) {
            if (lastEventNanos == 0 || nowNanos - lastEventNanos > PHYSICS_RATE_STALE_NANOS)
                return fallbackRate;

            long elapsedNanos = nowNanos - windowStartNanos;
            if (elapsedNanos >= PHYSICS_RATE_WINDOW_NANOS) {
                lastCompletedRate = (int) Math.round(ticksInWindow * (double) PHYSICS_RATE_WINDOW_NANOS / elapsedNanos);
                ticksInWindow = 0;
                windowStartNanos = nowNanos;
            }

            if (lastCompletedRate > 0)
                return lastCompletedRate;

            if (elapsedNanos > 250_000_000L)
                return (int) Math.round(ticksInWindow * (double) PHYSICS_RATE_WINDOW_NANOS / elapsedNanos);

            return fallbackRate;
        }
    }
}
