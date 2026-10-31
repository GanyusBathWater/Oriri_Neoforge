package net.ganyusbathwater.oririmod.events.disaster;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Evaluates whether an entity or position is sheltered from the lethal elements of a Snowstorm disaster.
 * Provides granular biome detection, deep cave protection, and a bounded 3D BFS room enclosure check
 * with leak tolerance (allowing windows, open doors, and chimneys) as well as heat source overrides.
 */
public final class SnowstormShelterEvaluator {

    private SnowstormShelterEvaluator() {}

    /**
     * Determines whether the given position is located in a cold biome where freezing snow precipitates.
     */
    public static boolean isColdBiome(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        return level.getBiome(pos).value().coldEnoughToSnow(pos);
    }

    /**
     * Determines whether a position is safely insulated deep underground in a subterranean cave.
     */
    public static boolean isCaveSheltered(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;

        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        if (skyLight > 4) {
            return false;
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        int caveDepthThreshold = OririConfig.COMMON.naturalDisasters.snowstormCaveDepthThreshold.get();
        return pos.getY() <= (surfaceY - caveDepthThreshold);
    }

    /**
     * Evaluates whether a position is protected inside a man-made or enclosed structure.
     * Considers:
     * 1. Direct sky exposure (exposed if can see sky).
     * 2. Warm micro-climate from nearby heat sources (campfires, furnaces, torches) under a solid roof.
     * 3. Bounded 3D flood fill enclosure check with leak tolerance for windows, doors, and chimneys.
     */
    public static boolean isStructureSheltered(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;

        // If directly exposed to open sky, cannot be sheltered
        if (level.canSeeSky(pos)) {
            return false;
        }

        // Heat source micro-climate check under roof
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int minHeatLight = OririConfig.COMMON.naturalDisasters.snowstormHeatSourceMinLight.get();
        if (blockLight >= minHeatLight) {
            return true;
        }

        // Bounded 3D flood fill enclosure verification
        int leakTolerance = OririConfig.COMMON.naturalDisasters.snowstormLeakTolerance.get();
        return checkRoomEnclosure(level, pos, leakTolerance);
    }

    /**
     * Checks if the position is within an enclosed room using a bounded BFS.
     * If the number of reachable air nodes that can see the sky exceeds leakTolerance, it is treated as exposed.
     */
    private static boolean checkRoomEnclosure(Level level, BlockPos startPos, int leakTolerance) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        int skyLeaksFound = 0;
        final int maxNodes = 64;
        final int maxManhattanDist = 7;

        while (!queue.isEmpty() && visited.size() < maxNodes) {
            BlockPos current = queue.poll();

            // Check if this reachable node sees open sky
            if (level.canSeeSky(current)) {
                skyLeaksFound++;
                if (skyLeaksFound > leakTolerance) {
                    return false; // Too many leaks / open to elements
                }
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (visited.contains(neighbor)) continue;
                if (neighbor.distManhattan(startPos) > maxManhattanDist) continue;

                BlockState state = level.getBlockState(neighbor);
                // If passable / no collision, air flows through it
                if (state.isAir() || state.getCollisionShape(level, neighbor).isEmpty()) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    if (visited.size() >= maxNodes) break;
                }
            }
        }

        return skyLeaksFound <= leakTolerance;
    }

    /**
     * Comprehensive shelter check: true if either deep cave sheltered or structure sheltered.
     */
    public static boolean isSheltered(Level level, BlockPos pos) {
        if (isCaveSheltered(level, pos)) {
            return true;
        }
        return isStructureSheltered(level, pos);
    }
}
