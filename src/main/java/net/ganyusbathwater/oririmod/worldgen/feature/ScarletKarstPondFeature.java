package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Detects natural flat Karst plateau caps/terraces via 2D flood-fill, carves
 * organic Blood Water ponds recessed below the natural rim (with zero artificial
 * raised walls), and optionally opens at most one steep, non-branching cliff
 * waterfall outlet into a sealed rock plunge pocket.
 */
public class ScarletKarstPondFeature extends Feature<NoneFeatureConfiguration> {

    private static final int GRID_RADIUS = 13;
    private static final int GRID_SIZE = GRID_RADIUS * 2 + 1;
    private static final int UNEVALUATED = Integer.MAX_VALUE;

    private static final int BFS_RADIUS = 7;
    private static final int MAX_FLAT_REGION = 48;
    private static final int MIN_INTERIOR_BLOCKS = 4;
    private static final int MAX_INTERIOR_BLOCKS = 32;

    public ScarletKarstPondFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        TopGrid grid = new TopGrid(level, origin.getX(), origin.getZ());

        // 1. Find the flattest valid plateau/terrace seed near origin
        int[] seed = findBestFlatSeed(grid, origin);
        if (seed == null) {
            return false;
        }
        int seedX = seed[0];
        int seedZ = seed[1];
        int baseH = grid.getY(seedX, seedZ);
        int waterY = baseH;

        // 2. Flood-fill connected flat surface columns in [baseH, baseH + 1]
        boolean[][] inFlat = collectFlatRegion(grid, origin, seedX, seedZ, baseH);

        // 3. Extract natural interior polygon whose cardinal and diagonal neighbors preserve a rim >= baseH
        boolean[][] inInterior = extractLargestInterior(grid, origin, inFlat, baseH);
        if (inInterior == null) {
            return false;
        }

        // Collect strictly adjacent rim columns (cardinal neighbors of interior not inside interior)
        boolean[][] inRim = new boolean[GRID_SIZE][GRID_SIZE];
        List<int[]> interiorCells = new ArrayList<>();
        List<int[]> rimCells = new ArrayList<>();

        for (int gx = 0; gx < GRID_SIZE; gx++) {
            for (int gz = 0; gz < GRID_SIZE; gz++) {
                if (!inInterior[gx][gz]) {
                    continue;
                }
                int wx = origin.getX() + gx - GRID_RADIUS;
                int wz = origin.getZ() + gz - GRID_RADIUS;
                interiorCells.add(new int[]{wx, wz});

                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    int ngx = gx + dir.getStepX();
                    int ngz = gz + dir.getStepZ();
                    if (!inInterior[ngx][ngz] && !inRim[ngx][ngz]) {
                        inRim[ngx][ngz] = true;
                        rimCells.add(new int[]{origin.getX() + ngx - GRID_RADIUS, origin.getZ() + ngz - GRID_RADIUS});
                    }
                }
            }
        }

        if (interiorCells.size() < MIN_INTERIOR_BLOCKS) {
            return false;
        }

        // 4. Evaluate rim blocks for a steep, strictly non-branching cliff outlet
        OutletCandidate outlet = findBestSteepOutlet(grid, origin, inInterior, baseH, waterY);

        // 5. Seal natural rim (never raising blocks above groundY) and convert exposed rim edge dirt to rock
        for (int[] cell : rimCells) {
            int rx = cell[0];
            int rz = cell[1];
            int rGroundY = grid.getY(rx, rz);
            if (rGroundY == Integer.MIN_VALUE) {
                rGroundY = baseH;
            }
            sealRimColumn(level, random, rx, rz, baseH, rGroundY);
        }

        // 6. Carve organic pond interior (2 layers deep) and fill Blood Water flush with baseH
        BlockState bloodWater = ModFluids.BLOOD_WATER_BLOCK.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int[] cell : interiorCells) {
            int ix = cell[0];
            int iz = cell[1];
            int iGroundY = grid.getY(ix, iz);

            // Solid karst rock basin floor at baseH - 3 .. baseH - 2
            fillRockRange(level, random, ix, baseH - 3, baseH - 2, iz);
            // Carve open sky clearance above waterY = baseH
            clearAirRange(level, ix, baseH + 1, Math.max(iGroundY + 2, baseH + 3), iz);
            level.setBlock(pos.set(ix, baseH - 1, iz), bloodWater, 2);
            level.setBlock(pos.set(ix, baseH, iz), bloodWater, 2);
        }

        // 7. Carve single steep cliff outlet & recessed plunge pocket if found
        if (outlet != null) {
            int ox = outlet.rimX;
            int oz = outlet.rimZ;
            fillRockRange(level, random, ox, baseH - 2, baseH - 1, oz);
            clearAirRange(level, ox, baseH + 1, baseH + 2, oz);
            pos.set(ox, baseH, oz);
            level.setBlock(pos, bloodWater, 2);
            level.scheduleTick(pos, ModFluids.BLOOD_WATER_SOURCE.get(), 2);

            // Carve recessed 1-block plunge basin at cliff base so falling cascade never spreads out
            int lx = outlet.landX;
            int lz = outlet.landZ;
            int ly = outlet.landY;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                int nx = lx + dir.getStepX();
                int nz = lz + dir.getStepZ();
                int ngy = grid.getY(nx, nz);
                if (ngy != Integer.MIN_VALUE && ngy < ly) {
                    fillRockRange(level, random, nx, ngy, ly, nz);
                }
            }
            fillRockRange(level, random, lx, ly - 3, ly - 2, lz);
            clearAirRange(level, lx, ly, ly + 2, lz);
            level.setBlock(pos.set(lx, ly - 1, lz), bloodWater, 2);
        }

        return true;
    }

    private int[] findBestFlatSeed(TopGrid grid, BlockPos origin) {
        int bestScore = -1;
        int bestDistSq = Integer.MAX_VALUE;
        int bestX = 0;
        int bestZ = 0;

        for (int dx = -4; dx <= 4; dx += 2) {
            for (int dz = -4; dz <= 4; dz += 2) {
                int cx = origin.getX() + dx;
                int cz = origin.getZ() + dz;
                int h = grid.getY(cx, cz);
                if (h < 74 || h > 138 || grid.hasFluidOrLog(cx, cz)) {
                    continue;
                }

                int flatNeighbors = 0;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    int nx = cx + dir.getStepX();
                    int nz = cz + dir.getStepZ();
                    int ny = grid.getY(nx, nz);
                    if (ny >= h && ny <= h + 1 && !grid.hasFluidOrLog(nx, nz)) {
                        flatNeighbors++;
                    }
                }
                if (flatNeighbors < 3) {
                    continue;
                }

                int distSq = dx * dx + dz * dz;
                if (flatNeighbors > bestScore || (flatNeighbors == bestScore && distSq < bestDistSq)) {
                    bestScore = flatNeighbors;
                    bestDistSq = distSq;
                    bestX = cx;
                    bestZ = cz;
                }
            }
        }

        return bestScore >= 3 ? new int[]{bestX, bestZ} : null;
    }

    private boolean[][] collectFlatRegion(TopGrid grid, BlockPos origin, int seedX, int seedZ, int baseH) {
        boolean[][] inFlat = new boolean[GRID_SIZE][GRID_SIZE];
        ArrayDeque<int[]> queue = new ArrayDeque<>();

        int startGx = seedX - origin.getX() + GRID_RADIUS;
        int startGz = seedZ - origin.getZ() + GRID_RADIUS;
        inFlat[startGx][startGz] = true;
        queue.add(new int[]{startGx, startGz});
        int count = 1;

        while (!queue.isEmpty() && count < MAX_FLAT_REGION) {
            int[] curr = queue.poll();
            int gx = curr[0];
            int gz = curr[1];

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                int ngx = gx + dir.getStepX();
                int ngz = gz + dir.getStepZ();
                int dx = ngx - GRID_RADIUS;
                int dz = ngz - GRID_RADIUS;
                if (Math.abs(dx) > BFS_RADIUS || Math.abs(dz) > BFS_RADIUS || inFlat[ngx][ngz]) {
                    continue;
                }
                int wx = origin.getX() + dx;
                int wz = origin.getZ() + dz;
                int ny = grid.getY(wx, wz);
                if (ny >= baseH && ny <= baseH + 1 && !grid.hasFluidOrLog(wx, wz)) {
                    inFlat[ngx][ngz] = true;
                    queue.add(new int[]{ngx, ngz});
                    count++;
                    if (count >= MAX_FLAT_REGION) {
                        break;
                    }
                }
            }
        }
        return inFlat;
    }

    private boolean[][] extractLargestInterior(TopGrid grid, BlockPos origin, boolean[][] inFlat, int baseH) {
        boolean[][] candidate = new boolean[GRID_SIZE][GRID_SIZE];
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int gx = dx + GRID_RADIUS;
                int gz = dz + GRID_RADIUS;
                if (!inFlat[gx][gz]) {
                    continue;
                }

                boolean validInterior = true;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    int wx = origin.getX() + dx + dir.getStepX();
                    int wz = origin.getZ() + dz + dir.getStepZ();
                    int ny = grid.getY(wx, wz);
                    if (ny < baseH || grid.hasFluidOrLog(wx, wz)) {
                        validInterior = false;
                        break;
                    }
                }
                if (!validInterior) {
                    continue;
                }

                for (int ox : new int[]{-1, 1}) {
                    for (int oz : new int[]{-1, 1}) {
                        int wx = origin.getX() + dx + ox;
                        int wz = origin.getZ() + dz + oz;
                        int dy = grid.getY(wx, wz);
                        if (dy == Integer.MIN_VALUE || dy < baseH - 1) {
                            validInterior = false;
                            break;
                        }
                    }
                    if (!validInterior) {
                        break;
                    }
                }

                if (validInterior) {
                    candidate[gx][gz] = true;
                }
            }
        }

        // Select the largest 4-connected interior cluster and cap to MAX_INTERIOR_BLOCKS while staying connected
        boolean[][] visited = new boolean[GRID_SIZE][GRID_SIZE];
        List<int[]> bestCluster = null;

        for (int gx = 0; gx < GRID_SIZE; gx++) {
            for (int gz = 0; gz < GRID_SIZE; gz++) {
                if (!candidate[gx][gz] || visited[gx][gz]) {
                    continue;
                }
                List<int[]> cluster = new ArrayList<>();
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                visited[gx][gz] = true;
                queue.add(new int[]{gx, gz});

                while (!queue.isEmpty()) {
                    int[] curr = queue.poll();
                    if (cluster.size() < MAX_INTERIOR_BLOCKS) {
                        cluster.add(curr);
                    }
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int ngx = curr[0] + dir.getStepX();
                        int ngz = curr[1] + dir.getStepZ();
                        if (ngx >= 0 && ngx < GRID_SIZE && ngz >= 0 && ngz < GRID_SIZE
                                && candidate[ngx][ngz] && !visited[ngx][ngz]) {
                            visited[ngx][ngz] = true;
                            queue.add(new int[]{ngx, ngz});
                        }
                    }
                }

                if (bestCluster == null || cluster.size() > bestCluster.size()) {
                    bestCluster = cluster;
                }
            }
        }

        if (bestCluster == null || bestCluster.size() < MIN_INTERIOR_BLOCKS) {
            return null;
        }

        boolean[][] result = new boolean[GRID_SIZE][GRID_SIZE];
        for (int[] cell : bestCluster) {
            result[cell[0]][cell[1]] = true;
        }
        return result;
    }

    private OutletCandidate findBestSteepOutlet(TopGrid grid, BlockPos origin, boolean[][] inInterior,
                                                int baseH, int waterY) {
        OutletCandidate best = null;

        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int gx = dx + GRID_RADIUS;
                int gz = dz + GRID_RADIUS;
                if (inInterior[gx][gz]) {
                    continue;
                }

                int rx = origin.getX() + dx;
                int rz = origin.getZ() + dz;
                int ry = grid.getY(rx, rz);
                if (ry != baseH || grid.hasFluidOrLog(rx, rz)) {
                    continue;
                }

                // Identify unique outward cardinal direction away from carved pond interior
                Direction outDir = null;
                int interiorNeighbors = 0;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    int igx = gx - dir.getStepX();
                    int igz = gz - dir.getStepZ();
                    if (igx >= 0 && igx < GRID_SIZE && igz >= 0 && igz < GRID_SIZE && inInterior[igx][igz]) {
                        outDir = dir;
                        interiorNeighbors++;
                    }
                }
                if (interiorNeighbors != 1 || outDir == null) {
                    continue;
                }

                Direction perpDir = outDir.getClockWise();
                // Both lateral rim neighbors must be solid natural ground >= baseH and uncarved
                int lgx = gx + perpDir.getStepX();
                int lgz = gz + perpDir.getStepZ();
                int rgx = gx - perpDir.getStepX();
                int rgz = gz - perpDir.getStepZ();
                if (inInterior[lgx][lgz] || inInterior[rgx][rgz]) {
                    continue;
                }
                int lyRim = grid.getY(rx + perpDir.getStepX(), rz + perpDir.getStepZ());
                int ryRim = grid.getY(rx - perpDir.getStepX(), rz - perpDir.getStepZ());
                if (lyRim < baseH || ryRim < baseH) {
                    continue;
                }

                // Immediate cell outside rim must drop steeply >= 2 blocks below waterY with higher/guarded sides
                int x1 = rx + outDir.getStepX();
                int z1 = rz + outDir.getStepZ();
                int y1 = grid.getY(x1, z1);
                if (y1 == Integer.MIN_VALUE || y1 > waterY - 2 || grid.hasFluidOrLog(x1, z1)) {
                    continue;
                }

                int leftWallY = grid.getY(x1 + perpDir.getStepX(), z1 + perpDir.getStepZ());
                int rightWallY = grid.getY(x1 - perpDir.getStepX(), z1 - perpDir.getStepZ());
                if (leftWallY == Integer.MIN_VALUE || rightWallY == Integer.MIN_VALUE
                        || leftWallY < y1 + 1 || rightWallY < y1 + 1) {
                    continue;
                }

                // Trace steepest downhill trajectory for 1..4 steps, rejecting any split/branch risk
                int cx = x1;
                int cz = z1;
                int cy = y1;
                int stepsUsed = 0;
                boolean validTrace = true;

                for (int step = 0; step < 4; step++) {
                    int minY = cy;
                    int bx = cx;
                    int bz = cz;
                    int lowerCount = 0;

                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int nx = cx + dir.getStepX();
                        int nz = cz + dir.getStepZ();
                        if (Math.abs(nx - origin.getX()) > GRID_RADIUS - 1
                                || Math.abs(nz - origin.getZ()) > GRID_RADIUS - 1) {
                            validTrace = false;
                            break;
                        }
                        int ny = grid.getY(nx, nz);
                        if (ny == Integer.MIN_VALUE) {
                            validTrace = false;
                            break;
                        }
                        if (ny < cy) {
                            lowerCount++;
                            if (ny < minY) {
                                minY = ny;
                                bx = nx;
                                bz = nz;
                            }
                        }
                    }

                    if (!validTrace) {
                        break;
                    }

                    int drop = cy - minY;
                    if (drop == 0) {
                        break; // Reached basin floor
                    }
                    if (drop == 1 && lowerCount > 1) {
                        validTrace = false;
                        break;
                    }

                    int nearMinCount = 0;
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int nx = cx + dir.getStepX();
                        int nz = cz + dir.getStepZ();
                        int ny = grid.getY(nx, nz);
                        if (ny < cy && ny <= minY + 1) {
                            nearMinCount++;
                        }
                    }
                    if (nearMinCount >= 2 || grid.hasFluidOrLog(bx, bz)) {
                        validTrace = false;
                        break;
                    }

                    cx = bx;
                    cz = bz;
                    cy = minY;
                    stepsUsed++;
                }

                int totalDrop = waterY - cy;
                if (!validTrace || totalDrop < 4 || cy < 70 || cy - 3 < origin.getY() - 32) {
                    continue;
                }

                // Ensure the 1-block plunge basin at cliff bottom is grounded above min world bounds
                if (grid.getY(cx, cz) == Integer.MIN_VALUE) {
                    continue;
                }

                int score = totalDrop * 10 - stepsUsed * 3;
                if (best == null || score > best.score) {
                    best = new OutletCandidate(rx, rz, cx, cz, cy, score);
                }
            }
        }

        return best;
    }

    private void sealRimColumn(WorldGenLevel level, RandomSource random, int x, int z,
                               int baseH, int groundY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // Seal basin bed & water wall levels (baseH - 2 .. baseH) so 2-layer blood water cannot leak
        for (int y = baseH - 2; y <= baseH; y++) {
            pos.set(x, y, z);
            BlockState existing = level.getBlockState(pos);
            if (existing.isAir() || existing.canBeReplaced() || isSoilOrSurfaceRock(existing)) {
                level.setBlock(pos, pickKarstRock(random), 2);
            }
        }
    }

    private static void fillRockRange(WorldGenLevel level, RandomSource random, int x, int fromY, int toY, int z) {
        if (toY < fromY) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = fromY; y <= toY; y++) {
            pos.set(x, y, z);
            BlockState existing = level.getBlockState(pos);
            if (existing.isAir()
                    || existing.canBeReplaced()
                    || existing.is(ModBlocks.POINTED_SCARLET_DRIPSTONE.get())
                    || isSoilOrSurfaceRock(existing)) {
                level.setBlock(pos, pickKarstRock(random), 2);
            }
        }
    }

    private static void clearAirRange(WorldGenLevel level, int x, int fromY, int toY, int z) {
        if (toY < fromY) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = fromY; y <= toY; y++) {
            pos.set(x, y, z);
            BlockState existing = level.getBlockState(pos);
            if (!existing.isAir() && !existing.is(BlockTags.LOGS) && !existing.is(BlockTags.LEAVES)) {
                level.setBlock(pos, air, 2);
            }
        }
    }

    private static boolean isSoilOrSurfaceRock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(ModBlocks.SCARLET_STONE.get())
                || state.is(ModBlocks.SCARLET_DRIPSTONE_BLOCK.get());
    }

    private static BlockState pickKarstRock(RandomSource random) {
        if (random.nextFloat() < 0.35f) {
            return ModBlocks.SCARLET_DRIPSTONE_BLOCK.get().defaultBlockState();
        }
        return ModBlocks.SCARLET_STONE.get().defaultBlockState();
    }

    private static final class TopGrid {
        private final WorldGenLevel level;
        private final int originX;
        private final int originZ;
        private final int[][] heights = new int[GRID_SIZE][GRID_SIZE];
        private final byte[][] hazards = new byte[GRID_SIZE][GRID_SIZE]; // 0 = unchecked, 1 = clean, 2 = fluid/log
        private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        TopGrid(WorldGenLevel level, int originX, int originZ) {
            this.level = level;
            this.originX = originX;
            this.originZ = originZ;
            for (int[] row : heights) {
                Arrays.fill(row, UNEVALUATED);
            }
        }

        int getY(int wx, int wz) {
            int gx = wx - originX + GRID_RADIUS;
            int gz = wz - originZ + GRID_RADIUS;
            if (gx < 0 || gx >= GRID_SIZE || gz < 0 || gz >= GRID_SIZE) {
                return Integer.MIN_VALUE;
            }
            int cached = heights[gx][gz];
            if (cached != UNEVALUATED) {
                return cached;
            }
            int computed = scanColumnY(wx, wz);
            heights[gx][gz] = computed;
            return computed;
        }

        boolean hasFluidOrLog(int wx, int wz) {
            int gx = wx - originX + GRID_RADIUS;
            int gz = wz - originZ + GRID_RADIUS;
            if (gx < 0 || gx >= GRID_SIZE || gz < 0 || gz >= GRID_SIZE) {
                return true;
            }
            byte cached = hazards[gx][gz];
            if (cached != 0) {
                return cached == 2;
            }
            int y = getY(wx, wz);
            boolean hazard = (y == Integer.MIN_VALUE);
            if (!hazard) {
                for (int cy = y; cy <= y + 3; cy++) {
                    BlockState state = level.getBlockState(probe.set(wx, cy, wz));
                    if (!state.getFluidState().isEmpty() || state.is(BlockTags.LOGS)) {
                        hazard = true;
                        break;
                    }
                }
            }
            hazards[gx][gz] = (byte) (hazard ? 2 : 1);
            return hazard;
        }

        private int scanColumnY(int wx, int wz) {
            int wgTop = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, wx, wz);
            int startY = Math.min(level.getMaxBuildHeight() - 3, wgTop + 5);
            int minY = Math.max(level.getMinBuildHeight() + 4, startY - 24);
            for (int y = startY; y >= minY; y--) {
                BlockState state = level.getBlockState(probe.set(wx, y, wz));
                if (!state.getFluidState().isEmpty()) {
                    return Integer.MIN_VALUE;
                }
                if (isSoilOrSurfaceRock(state)) {
                    return y;
                }
                if (!state.isAir()
                        && !state.canBeReplaced()
                        && !state.is(ModBlocks.POINTED_SCARLET_DRIPSTONE.get())
                        && !state.is(BlockTags.LEAVES)
                        && !state.is(BlockTags.LOGS)) {
                    return Integer.MIN_VALUE;
                }
            }
            return Integer.MIN_VALUE;
        }
    }

    private record OutletCandidate(int rimX, int rimZ, int landX, int landZ, int landY, int score) {
    }

    /**
     * Places a single Blood Water source block embedded inside a steep Scarlet Karst
     * cliff face so it drops straight down into a 1-block recessed catch pocket at the
     * cliff base without spreading horizontally across slopes.
     */
    public static class CliffSpringFeature extends Feature<NoneFeatureConfiguration> {

        public CliffSpringFeature(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            RandomSource random = context.random();
            TopGrid grid = new TopGrid(level, origin.getX(), origin.getZ());

            int bestScore = -1;
            int bestDistSq = Integer.MAX_VALUE;
            int bestCx = 0;
            int bestCz = 0;
            int bestTopY = 0;
            int bestLandingY = 0;
            Direction bestDir = null;

            for (int dx = -4; dx <= 4; dx += 2) {
                for (int dz = -4; dz <= 4; dz += 2) {
                    int cx = origin.getX() + dx;
                    int cz = origin.getZ() + dz;
                    int topY = grid.getY(cx, cz);
                    if (topY < 80 || topY > 142 || grid.hasFluidOrLog(cx, cz)) {
                        continue;
                    }

                    for (Direction outDir : Direction.Plane.HORIZONTAL) {
                        int x1 = cx + outDir.getStepX();
                        int z1 = cz + outDir.getStepZ();
                        int y1 = grid.getY(x1, z1);
                        if (y1 == Integer.MIN_VALUE || y1 > topY - 3 || y1 < 70 || grid.hasFluidOrLog(x1, z1)) {
                            continue;
                        }

                        Direction perp = outDir.getClockWise();
                        int backY = grid.getY(cx - outDir.getStepX(), cz - outDir.getStepZ());
                        int leftY = grid.getY(cx + perp.getStepX(), cz + perp.getStepZ());
                        int rightY = grid.getY(cx - perp.getStepX(), cz - perp.getStepZ());
                        if (backY < topY - 1 || leftY < topY - 1 || rightY < topY - 1) {
                            continue;
                        }

                        int fwdY = grid.getY(x1 + outDir.getStepX(), z1 + outDir.getStepZ());
                        int lLeftY = grid.getY(x1 + perp.getStepX(), z1 + perp.getStepZ());
                        int lRightY = grid.getY(x1 - perp.getStepX(), z1 - perp.getStepZ());
                        if (fwdY < y1 - 1 || lLeftY < y1 - 1 || lRightY < y1 - 1
                                || grid.hasFluidOrLog(x1 + outDir.getStepX(), z1 + outDir.getStepZ())
                                || grid.hasFluidOrLog(x1 + perp.getStepX(), z1 + perp.getStepZ())
                                || grid.hasFluidOrLog(x1 - perp.getStepX(), z1 - perp.getStepZ())) {
                            continue;
                        }

                        int score = Math.min(10, topY - y1);
                        int distSq = dx * dx + dz * dz;
                        if (score > bestScore || (score == bestScore && distSq < bestDistSq)) {
                            bestScore = score;
                            bestDistSq = distSq;
                            bestCx = cx;
                            bestCz = cz;
                            bestTopY = topY;
                            bestLandingY = y1;
                            bestDir = outDir;
                        }
                    }
                }
            }

            if (bestDir == null) {
                return false;
            }

            Direction perp = bestDir.getClockWise();
            int x1 = bestCx + bestDir.getStepX();
            int z1 = bestCz + bestDir.getStepZ();
            int springY = Math.min(bestTopY - 1, bestLandingY + 6);

            // Seal all 5 rock faces enclosing the cliff spring so fluid only exits toward bestDir
            fillRockRange(level, random, bestCx, springY - 1, springY - 1, bestCz);
            fillRockRange(level, random, bestCx, springY + 1, springY + 1, bestCz);
            fillRockRange(level, random, bestCx - bestDir.getStepX(), springY - 1, springY, bestCz - bestDir.getStepZ());
            fillRockRange(level, random, bestCx + perp.getStepX(), springY - 1, springY, bestCz + perp.getStepZ());
            fillRockRange(level, random, bestCx - perp.getStepX(), springY - 1, springY, bestCz - perp.getStepZ());

            // Clear vertical drop chute from landingY + 1 up to springY + 1
            clearAirRange(level, x1, bestLandingY + 1, springY + 1, z1);

            // Carve a 1-block recessed catch pocket at the cliff base (y = bestLandingY)
            fillRockRange(level, random, x1, bestLandingY - 2, bestLandingY - 1, z1);
            for (BlockPos rimNeighbor : new BlockPos[]{
                    new BlockPos(x1 + bestDir.getStepX(), 0, z1 + bestDir.getStepZ()),
                    new BlockPos(x1 + perp.getStepX(), 0, z1 + perp.getStepZ()),
                    new BlockPos(x1 - perp.getStepX(), 0, z1 - perp.getStepZ())
            }) {
                int rgy = grid.getY(rimNeighbor.getX(), rimNeighbor.getZ());
                if (rgy != Integer.MIN_VALUE && rgy < bestLandingY) {
                    fillRockRange(level, random, rimNeighbor.getX(), rgy, bestLandingY, rimNeighbor.getZ());
                } else {
                    fillRockRange(level, random, rimNeighbor.getX(), bestLandingY - 1, bestLandingY - 1, rimNeighbor.getZ());
                }
            }
            clearAirRange(level, x1, bestLandingY, bestLandingY, z1);

            BlockPos springPos = new BlockPos(bestCx, springY, bestCz);
            level.setBlock(springPos, ModFluids.BLOOD_WATER_BLOCK.get().defaultBlockState(), 2);
            level.scheduleTick(springPos, ModFluids.BLOOD_WATER_SOURCE.get(), 2);
            return true;
        }
    }
}
