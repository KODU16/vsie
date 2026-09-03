package com.kodu16.vsie.foundation.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Maintains a rolling forced corridor while RPL handles the exact traversed path. */
public final class ForwardChunkLoadController {
    private static final Map<ServerLevel, Map<Long, ChunkReference>> REFERENCES = new WeakHashMap<>();
    private final Set<Long> requestedChunks = new HashSet<>();
    private ServerLevel requestedLevel;
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;
    private Vec3 lastVelocity;

    public void update(ServerLevel level, Vec3 position, Vec3 velocity) {
        if (requestedLevel != null && requestedLevel != level) {
            release();
        }
        requestedLevel = level;

        int chunkX = SectionPos.blockToSectionCoord((int) Math.floor(position.x));
        int chunkZ = SectionPos.blockToSectionCoord((int) Math.floor(position.z));
        if (chunkX == lastChunkX && chunkZ == lastChunkZ && velocity.equals(lastVelocity)) {
            return;
        }
        lastChunkX = chunkX;
        lastChunkZ = chunkZ;
        lastVelocity = velocity;

        Set<Long> nextChunks = new LinkedHashSet<>();
        for (ProjectileChunkLoadingPlan.ChunkCoordinate chunk : ProjectileChunkLoadingPlan.plan(
                position.x, position.z, velocity.x, velocity.z)) {
            nextChunks.add(ChunkPos.asLong(chunk.x(), chunk.z()));
        }

        // Match firecontrol's cheap path: only changed corridor tickets touch the chunk system.
        for (long chunkKey : requestedChunks) {
            if (!nextChunks.contains(chunkKey)) {
                unforceChunk(level, chunkKey);
            }
        }
        for (long chunkKey : nextChunks) {
            if (!requestedChunks.contains(chunkKey)) {
                forceChunk(level, chunkKey);
            }
        }
        requestedChunks.clear();
        requestedChunks.addAll(nextChunks);
    }

    public void release() {
        if (requestedLevel != null) {
            for (long chunkKey : requestedChunks) {
                unforceChunk(requestedLevel, chunkKey);
            }
        }
        requestedChunks.clear();
        requestedLevel = null;
        lastChunkX = Integer.MIN_VALUE;
        lastChunkZ = Integer.MIN_VALUE;
        lastVelocity = null;
    }

    public static boolean isCollisionEligible(ServerLevel level, int chunkX, int chunkZ) {
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        Map<Long, ChunkReference> levelReferences = REFERENCES.get(level);
        ChunkReference reference = levelReferences == null ? null : levelReferences.get(chunkKey);
        boolean naturallyLoaded = reference == null
                || reference.baselineNaturalLoader
                || isInsidePlayerView(level, chunkX, chunkZ);
        return naturallyLoaded && isEntityTicking(level, chunkX, chunkZ);
    }

    private static void forceChunk(ServerLevel level, long chunkKey) {
        Map<Long, ChunkReference> levelReferences = REFERENCES.computeIfAbsent(level, ignored -> new HashMap<>());
        ChunkReference reference = levelReferences.get(chunkKey);
        if (reference == null) {
            boolean owned = !level.getForcedChunks().contains(chunkKey);
            int chunkX = ChunkPos.getX(chunkKey);
            int chunkZ = ChunkPos.getZ(chunkKey);
            boolean baselineNaturalLoader = !owned
                    || isEntityTicking(level, chunkX, chunkZ) && !isInsidePlayerView(level, chunkX, chunkZ);
            reference = new ChunkReference(owned, baselineNaturalLoader);
            levelReferences.put(chunkKey, reference);
            if (owned) {
                level.setChunkForced(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey), true);
            }
        }
        reference.count++;
    }

    private static void unforceChunk(ServerLevel level, long chunkKey) {
        Map<Long, ChunkReference> levelReferences = REFERENCES.get(level);
        if (levelReferences == null) {
            return;
        }
        ChunkReference reference = levelReferences.get(chunkKey);
        if (reference == null || --reference.count > 0) {
            return;
        }
        levelReferences.remove(chunkKey);
        if (reference.owned) {
            level.setChunkForced(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey), false);
        }
        if (levelReferences.isEmpty()) {
            REFERENCES.remove(level);
        }
    }

    private static boolean isEntityTicking(ServerLevel level, int chunkX, int chunkZ) {
        if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            return false;
        }
        BlockPos origin = new BlockPos(SectionPos.sectionToBlockCoord(chunkX), 0, SectionPos.sectionToBlockCoord(chunkZ));
        return level.isPositionEntityTicking(origin);
    }

    private static boolean isInsidePlayerView(ServerLevel level, int chunkX, int chunkZ) {
        int viewDistance = level.getServer().getPlayerList().getViewDistance();
        return level.players().stream().anyMatch(player -> {
            ChunkPos playerChunk = player.chunkPosition();
            int majorDistance = Math.max(0, Math.max(
                    Math.abs(chunkX - playerChunk.x) - 1,
                    Math.abs(chunkZ - playerChunk.z) - 1
            ));
            int minorDistance = Math.max(0, Math.min(
                    Math.abs(chunkX - playerChunk.x) - 1,
                    Math.abs(chunkZ - playerChunk.z) - 1
            ));
            // Match vanilla's rounded chunk-view boundary without asking the chunk system to load anything.
            return (long) majorDistance * majorDistance + (long) minorDistance * minorDistance
                    < (long) viewDistance * viewDistance;
        });
    }

    private static final class ChunkReference {
        private int count;
        private final boolean owned;
        private final boolean baselineNaturalLoader;

        private ChunkReference(boolean owned, boolean baselineNaturalLoader) {
            this.owned = owned;
            this.baselineNaturalLoader = baselineNaturalLoader;
        }
    }
}
