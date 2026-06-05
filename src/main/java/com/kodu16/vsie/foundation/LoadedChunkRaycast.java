package com.kodu16.vsie.foundation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.function.Consumer;

public final class LoadedChunkRaycast {
    private static final Consumer<ClipContext> NO_CONTEXT_CONFIGURATION = context -> {
    };

    private LoadedChunkRaycast() {
    }

    public static BlockHitResult clipIgnoringUnloadedChunks(
            Level level,
            Vec3 from,
            Vec3 to,
            ClipContext.Block blockMode,
            ClipContext.Fluid fluidMode,
            CollisionContext collisionContext
    ) {
        return clipIgnoringUnloadedChunks(level, from, to, blockMode, fluidMode, collisionContext, NO_CONTEXT_CONFIGURATION);
    }

    public static BlockHitResult clipIgnoringUnloadedChunks(
            Level level,
            Vec3 from,
            Vec3 to,
            ClipContext.Block blockMode,
            ClipContext.Fluid fluidMode,
            CollisionContext collisionContext,
            Consumer<ClipContext> contextConfiguration
    ) {
        if (from.equals(to)) {
            return miss(from, to);
        }

        double dx = to.x - from.x;
        double dz = to.z - from.z;
        int chunkX = SectionPos.blockToSectionCoord(Mth.floor(from.x));
        int chunkZ = SectionPos.blockToSectionCoord(Mth.floor(from.z));

        if (Math.abs(dx) < 1.0E-10D && Math.abs(dz) < 1.0E-10D) {
            return clipChunkSegmentIfLoaded(level, chunkX, chunkZ, from, to, blockMode, fluidMode, collisionContext, contextConfiguration);
        }

        int stepX = dx > 0.0D ? 1 : dx < 0.0D ? -1 : 0;
        int stepZ = dz > 0.0D ? 1 : dz < 0.0D ? -1 : 0;
        double nextX = stepX == 0 ? Double.POSITIVE_INFINITY : firstChunkBoundaryT(from.x, dx, chunkX, stepX);
        double nextZ = stepZ == 0 ? Double.POSITIVE_INFINITY : firstChunkBoundaryT(from.z, dz, chunkZ, stepZ);
        double deltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 16.0D / Math.abs(dx);
        double deltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 16.0D / Math.abs(dz);
        double segmentStart = 0.0D;

        while (segmentStart <= 1.0D) {
            double segmentEnd = Math.min(1.0D, Math.min(nextX, nextZ));
            BlockHitResult hit = clipChunkSegmentIfLoaded(
                    level,
                    chunkX,
                    chunkZ,
                    from.lerp(to, segmentStart),
                    from.lerp(to, segmentEnd),
                    blockMode,
                    fluidMode,
                    collisionContext,
                    contextConfiguration
            );
            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit;
            }
            if (segmentEnd >= 1.0D) {
                break;
            }

            // Step both axes when the ray exits through a chunk corner.
            boolean advanceX = nextX <= nextZ;
            boolean advanceZ = nextZ <= nextX;
            if (advanceX) {
                chunkX += stepX;
                nextX += deltaX;
            }
            if (advanceZ) {
                chunkZ += stepZ;
                nextZ += deltaZ;
            }
            segmentStart = segmentEnd;
        }

        return miss(from, to);
    }

    private static BlockHitResult clipChunkSegmentIfLoaded(
            Level level,
            int chunkX,
            int chunkZ,
            Vec3 from,
            Vec3 to,
            ClipContext.Block blockMode,
            ClipContext.Fluid fluidMode,
            CollisionContext collisionContext,
            Consumer<ClipContext> contextConfiguration
    ) {
        if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            return miss(from, to);
        }

        ClipContext context = new ClipContext(from, to, blockMode, fluidMode, collisionContext);
        // Keep caller-specific clip context metadata, such as Sable's ignored sublevel.
        contextConfiguration.accept(context);
        return level.clip(context);
    }

    private static double firstChunkBoundaryT(double start, double delta, int chunk, int step) {
        double boundary = step > 0 ? (chunk + 1) * 16.0D : chunk * 16.0D;
        return (boundary - start) / delta;
    }

    private static BlockHitResult miss(Vec3 from, Vec3 to) {
        Vec3 direction = from.subtract(to);
        return BlockHitResult.miss(to, Direction.getNearest(direction.x, direction.y, direction.z), BlockPos.containing(to));
    }
}
