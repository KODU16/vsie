package com.kodu16.vsie.foundation.projectile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Keeps projectile corridors advancing independently of projectile entity ticks. */
public final class ProjectileCorridorManager {
    private static final Map<MinecraftServer, ProjectileCorridorManager> MANAGERS = new WeakHashMap<>();
    private final Map<UUID, TrackedProjectile> projectiles = new HashMap<>();

    private ProjectileCorridorManager() {
    }

    public static void update(Entity projectile, Vec3 position, Vec3 velocity) {
        if (!(projectile.level() instanceof ServerLevel level) || projectile.isRemoved()) {
            return;
        }
        ProjectileCorridorManager manager = MANAGERS.computeIfAbsent(level.getServer(), ignored -> new ProjectileCorridorManager());
        TrackedProjectile tracked = manager.projectiles.computeIfAbsent(
                projectile.getUUID(), ignored -> new TrackedProjectile(projectile)
        );
        tracked.projectile = projectile;
        tracked.controller.update(level, position, velocity);
    }

    public static void untrack(Entity projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        ProjectileCorridorManager manager = MANAGERS.get(level.getServer());
        if (manager == null) {
            return;
        }
        TrackedProjectile tracked = manager.projectiles.remove(projectile.getUUID());
        if (tracked != null) {
            tracked.controller.release();
        }
    }

    public static boolean isCollisionEligible(ServerLevel level, int chunkX, int chunkZ) {
        return ForwardChunkLoadController.isCollisionEligible(level, chunkX, chunkZ);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ProjectileCorridorManager manager = MANAGERS.get(event.getServer());
        if (manager != null) {
            manager.tick();
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        ProjectileCorridorManager manager = MANAGERS.remove(event.getServer());
        if (manager != null) {
            manager.releaseAll();
        }
    }

    private void tick() {
        Iterator<TrackedProjectile> iterator = projectiles.values().iterator();
        while (iterator.hasNext()) {
            TrackedProjectile tracked = iterator.next();
            Entity projectile = tracked.projectile;
            if (projectile.isRemoved() || !(projectile.level() instanceof ServerLevel level)) {
                tracked.controller.release();
                iterator.remove();
                continue;
            }
            tracked.controller.update(level, projectile.position(), projectile.getDeltaMovement());
        }
    }

    private void releaseAll() {
        projectiles.values().forEach(tracked -> tracked.controller.release());
        projectiles.clear();
    }

    private static final class TrackedProjectile {
        private Entity projectile;
        private final ForwardChunkLoadController controller = new ForwardChunkLoadController();

        private TrackedProjectile(Entity projectile) {
            this.projectile = projectile;
        }
    }
}
