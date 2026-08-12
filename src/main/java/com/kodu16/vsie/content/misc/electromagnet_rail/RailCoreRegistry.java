package com.kodu16.vsie.content.misc.electromagnet_rail;

import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.weapon.electro_magnet_rail_accelerator.ElectromagnetRailAcceleratorBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves a persistent rail binding ID to the core's current dimension and plot position. */
public final class RailCoreRegistry {
    private static final Map<UUID, Map<CoreKey, WeakReference<ElectroMagnetRailCoreBlockEntity>>> CORES =
            new ConcurrentHashMap<>();
    private static final Map<UUID, Map<ClientAcceleratorKey, WeakReference<ElectromagnetRailAcceleratorBlockEntity>>> CLIENT_ACCELERATORS =
            new ConcurrentHashMap<>();

    private RailCoreRegistry() {
    }

    public static void register(ElectroMagnetRailCoreBlockEntity core) {
        if (!(core.getLevel() instanceof ServerLevel level) || core.getRailBindingId() == null) {
            return;
        }
        CoreKey key = new CoreKey(level.dimension(), core.getBlockPos());
        Map<CoreKey, WeakReference<ElectroMagnetRailCoreBlockEntity>> candidates =
                CORES.computeIfAbsent(core.getRailBindingId(), ignored -> new ConcurrentHashMap<>());
        candidates.entrySet().removeIf(entry -> entry.getValue().get() == core && !entry.getKey().equals(key));
        candidates.put(key, new WeakReference<>(core));
    }

    public static void unregister(ElectroMagnetRailCoreBlockEntity core) {
        UUID bindingId = core.getRailBindingId();
        if (bindingId == null) {
            return;
        }
        Map<CoreKey, WeakReference<ElectroMagnetRailCoreBlockEntity>> candidates = CORES.get(bindingId);
        if (candidates == null) {
            return;
        }
        candidates.entrySet().removeIf(entry -> entry.getValue().get() == core || entry.getValue().get() == null);
        if (candidates.isEmpty()) {
            CORES.remove(bindingId, candidates);
        }
    }

    public static void registerClientAccelerator(ElectromagnetRailAcceleratorBlockEntity accelerator) {
        Level level = accelerator.getLevel();
        UUID bindingId = accelerator.getLinkedCoreBindingId();
        if (level == null || !level.isClientSide() || bindingId == null) {
            return;
        }
        ClientAcceleratorKey key = new ClientAcceleratorKey(level.dimension(), accelerator.getBlockPos());
        Map<ClientAcceleratorKey, WeakReference<ElectromagnetRailAcceleratorBlockEntity>> candidates =
                CLIENT_ACCELERATORS.computeIfAbsent(bindingId, ignored -> new ConcurrentHashMap<>());
        candidates.entrySet().removeIf(entry -> entry.getValue().get() == accelerator && !entry.getKey().equals(key));
        candidates.put(key, new WeakReference<>(accelerator));
    }

    public static List<ElectromagnetRailAcceleratorBlockEntity> getClientAccelerators(UUID bindingId, Level level) {
        Map<ClientAcceleratorKey, WeakReference<ElectromagnetRailAcceleratorBlockEntity>> candidates =
                CLIENT_ACCELERATORS.get(bindingId);
        if (candidates == null) {
            return List.of();
        }
        List<ElectromagnetRailAcceleratorBlockEntity> result = new java.util.ArrayList<>();
        for (Map.Entry<ClientAcceleratorKey, WeakReference<ElectromagnetRailAcceleratorBlockEntity>> entry : candidates.entrySet()) {
            ElectromagnetRailAcceleratorBlockEntity accelerator = entry.getValue().get();
            if (accelerator == null || accelerator.isRemoved() || accelerator.getLevel() != level
                    || !entry.getKey().dimension().equals(level.dimension())
                    || !accelerator.getBlockPos().equals(entry.getKey().pos())
                    || level.getBlockEntity(entry.getKey().pos()) != accelerator
                    || !bindingId.equals(accelerator.getLinkedCoreBindingId())) {
                candidates.remove(entry.getKey(), entry.getValue());
                continue;
            }
            result.add(accelerator);
        }
        if (candidates.isEmpty()) {
            CLIENT_ACCELERATORS.remove(bindingId, candidates);
        }
        return result;
    }

    public static @Nullable ElectroMagnetRailCoreBlockEntity resolve(
            MinecraftServer server,
            UUID bindingId,
            ServerSubLevel sourceSubLevel,
            BlockPos sourcePos
    ) {
        return selectNearest(server, CORES.get(bindingId), sourceSubLevel, sourcePos, Double.POSITIVE_INFINITY);
    }

    public static @Nullable ElectroMagnetRailCoreBlockEntity resolveNearest(
            MinecraftServer server,
            ServerSubLevel sourceSubLevel,
            BlockPos sourcePos,
            double maxDistance
    ) {
        ElectroMagnetRailCoreBlockEntity nearest = null;
        double nearestDistance = maxDistance;
        for (Map<CoreKey, WeakReference<ElectroMagnetRailCoreBlockEntity>> candidates : CORES.values()) {
            ElectroMagnetRailCoreBlockEntity candidate =
                    selectNearest(server, candidates, sourceSubLevel, sourcePos, nearestDistance);
            if (candidate == null) {
                continue;
            }
            double distance = worldDistance(sourceSubLevel, sourcePos, candidate);
            if (distance <= nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static @Nullable ElectroMagnetRailCoreBlockEntity selectNearest(
            MinecraftServer server,
            @Nullable Map<CoreKey, WeakReference<ElectroMagnetRailCoreBlockEntity>> candidates,
            ServerSubLevel sourceSubLevel,
            BlockPos sourcePos,
            double maxDistance
    ) {
        if (candidates == null) {
            return null;
        }
        ElectroMagnetRailCoreBlockEntity nearest = null;
        double nearestDistance = maxDistance;
        for (Map.Entry<CoreKey, WeakReference<ElectroMagnetRailCoreBlockEntity>> entry : candidates.entrySet()) {
            ElectroMagnetRailCoreBlockEntity core = validate(server, entry.getKey(), entry.getValue());
            if (core == null) {
                candidates.remove(entry.getKey(), entry.getValue());
                continue;
            }
            if (!sourceSubLevel.getLevel().dimension().equals(core.getLevel().dimension())) {
                continue;
            }
            SubLevel coreSubLevel = ServerShipUtils.getSubLevelAtBlockPos(core.getLevel(), core.getBlockPos());
            if (coreSubLevel == null || sourceSubLevel.getUniqueId().equals(coreSubLevel.getUniqueId())) {
                continue;
            }
            double distance = worldDistance(sourceSubLevel, sourcePos, core);
            if (distance <= nearestDistance) {
                nearest = core;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static @Nullable ElectroMagnetRailCoreBlockEntity validate(
            MinecraftServer server,
            CoreKey key,
            WeakReference<ElectroMagnetRailCoreBlockEntity> reference
    ) {
        ServerLevel level = server.getLevel(key.dimension());
        ElectroMagnetRailCoreBlockEntity core = reference.get();
        if (level == null || core == null || core.isRemoved() || core.getLevel() != level
                || !core.getBlockPos().equals(key.pos()) || level.getBlockEntity(key.pos()) != core) {
            return null;
        }
        return core;
    }

    private static double worldDistance(
            ServerSubLevel sourceSubLevel,
            BlockPos sourcePos,
            ElectroMagnetRailCoreBlockEntity core
    ) {
        SubLevel coreSubLevel = ServerShipUtils.getSubLevelAtBlockPos(core.getLevel(), core.getBlockPos());
        Vec3 sourceWorldPos = ServerShipUtils.getBlockCenterWorld(sourceSubLevel, sourcePos);
        Vec3 coreWorldPos = ServerShipUtils.getBlockCenterWorld(coreSubLevel, core.getBlockPos());
        return sourceWorldPos == null || coreWorldPos == null
                ? Double.POSITIVE_INFINITY
                : sourceWorldPos.distanceTo(coreWorldPos);
    }

    private record CoreKey(ResourceKey<Level> dimension, BlockPos pos) {
        private CoreKey {
            pos = pos.immutable();
        }
    }

    private record ClientAcceleratorKey(ResourceKey<Level> dimension, BlockPos pos) {
        private ClientAcceleratorKey {
            pos = pos.immutable();
        }
    }
}
