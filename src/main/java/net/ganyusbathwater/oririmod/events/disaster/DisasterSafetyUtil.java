package net.ganyusbathwater.oririmod.events.disaster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * "Fresh land" safety check for destructive disasters.
 *
 * <p>Vanilla stores an {@code inhabitedTime} counter in every chunk. In 1.21.1,
 * {@code ServerChunkCache#tickChunks} increments it by one per tick for every entity-ticking chunk
 * that is within 128 blocks of a (non-spectator) player. A chunk around a base therefore quickly
 * reaches hundreds of thousands of ticks, while land a player only travels through stays at a few
 * hundred / thousand ticks.
 *
 * <p>A destructive disaster may only target a player if <b>every</b> loaded chunk within the scan
 * radius is below the threshold. Using the maximum (not the average) means a single "home" chunk
 * anywhere in the radius protects the whole area - including land around the base the player may
 * want to build on later.
 */
public final class DisasterSafetyUtil {

    private DisasterSafetyUtil() {
    }

    /**
     * Result of an area scan.
     *
     * @param maxInhabitedTicks highest inhabited time of all loaded chunks in the radius
     * @param scannedChunks     number of loaded chunks that were checked
     * @param skippedChunks     chunks inside the radius that were not loaded (unknown)
     * @param threshold         threshold used for the decision
     */
    public record AreaScan(long maxInhabitedTicks, int scannedChunks, int skippedChunks, long threshold) {
        public boolean isFresh() {
            return scannedChunks > 0 && maxInhabitedTicks <= threshold;
        }
    }

    /**
     * Scans all chunks whose center lies within {@code radiusBlocks} of {@code center}.
     * Never loads or generates chunks; chunks that are not fully loaded are skipped.
     */
    public static AreaScan scan(ServerLevel level, BlockPos center, int radiusBlocks, long threshold) {
        ChunkPos centerChunk = new ChunkPos(center);
        int chunkRadius = (radiusBlocks + 15) >> 4;
        double maxDistSq = (double) (radiusBlocks + 8) * (radiusBlocks + 8); // +8: include chunks the circle touches

        long max = 0L;
        int scanned = 0;
        int skipped = 0;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = centerChunk.x + dx;
                int cz = centerChunk.z + dz;

                double chunkCenterX = (cx << 4) + 8.0 - center.getX();
                double chunkCenterZ = (cz << 4) + 8.0 - center.getZ();
                if (chunkCenterX * chunkCenterX + chunkCenterZ * chunkCenterZ > maxDistSq) {
                    continue;
                }

                // getChunkNow never loads/generates and returns null if the chunk is not FULL
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    skipped++;
                    continue;
                }
                scanned++;
                long inhabited = chunk.getInhabitedTime();
                if (inhabited > max) {
                    max = inhabited;
                }
            }
        }
        return new AreaScan(max, scanned, skipped, threshold);
    }

    /**
     * Tight check for a single impact position: the target chunk and its 8 neighbours must be
     * loaded and fresh. Prevents crater edges from reaching into an unloaded or inhabited chunk.
     */
    public static boolean isImpactSiteFresh(ServerLevel level, BlockPos target, long threshold) {
        ChunkPos cp = new ChunkPos(target);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x + dx, cp.z + dz);
                if (chunk == null || chunk.getInhabitedTime() > threshold) {
                    return false;
                }
            }
        }
        return true;
    }
}

