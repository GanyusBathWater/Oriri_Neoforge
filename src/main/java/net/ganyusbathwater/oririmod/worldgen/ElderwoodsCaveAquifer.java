package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.ganyusbathwater.oririmod.util.FastNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.apache.commons.lang3.mutable.MutableDouble;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.IntBinaryOperator;

/**
 * Vanilla-inspired domain-warped Voronoi aquifer system for the Elderwoods dimension.
 * <p>
 * Generates well-spaced, organic underground water lakes (Blood Water inside Scarlet Caves)
 * and deep lava pools separated by smooth curved stone/deepslate dams, while strictly avoiding
 * ocean basalt crusts, the Elysian Abyss cavern, and Global Aether River corridors.
 */
public final class ElderwoodsCaveAquifer implements Aquifer {

    @FunctionalInterface
    public interface IntCoordsToDouble {
        double applyAsDouble(int x, int z);
    }

    @FunctionalInterface
    public interface Int3Predicate {
        boolean test(int x, int y, int z);
    }

    public static final int WAY_BELOW_MIN_Y = -512;
    public static final int LAVA_GLOBAL_FLOOR_Y = -98;

    private static final int X_RANGE = 10;
    private static final int Y_RANGE = 9;
    private static final int Z_RANGE = 10;
    private static final int X_SPACING = 16;
    private static final int Y_SPACING = 12;
    private static final int Z_SPACING = 16;
    private static final double WARP_CLAMP_XZ = 8.0;
    private static final double WARP_CLAMP_Y = 4.0;
    private static final double SIMILARITY_SCALE = 56.0;
    private static final double FLOWING_UPDATE_SIMILARITY = similarity(100.0, 144.0);

    private static final int[][] SURFACE_OFFSETS = {
            { 0, 0 }, { -32, -16 }, { -16, -16 }, { 0, -16 }, { 16, -16 },
            { -48, 0 }, { -32, 0 }, { -16, 0 }, { 16, 0 },
            { -32, 16 }, { -16, 16 }, { 0, 16 }, { 16, 16 }
    };

    private static final int[][] RIVER_BUFFER_OFFSETS = {
            { 0, 0 }, { 12, 0 }, { -12, 0 }, { 0, 12 }, { 0, -12 },
            { 22, 0 }, { -22, 0 }, { 0, 22 }, { 0, -22 },
            { 16, 16 }, { -16, 16 }, { 16, -16 }, { -16, -16 }
    };

    private static final int[][] ABYSS_BUFFER_OFFSETS = {
            { 0, 0 }, { 14, 0 }, { -14, 0 }, { 0, 14 }, { 0, -14 },
            { 26, 0 }, { -26, 0 }, { 0, 26 }, { 0, -26 },
            { 18, 18 }, { -18, 18 }, { 18, -18 }, { -18, -18 }
    };

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState BLOOD_WATER = ModFluids.BLOOD_WATER_BLOCK.get().defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();

    private final long seed;
    private final int minBuildY;
    private final int maxBuildY;
    private final IntBinaryOperator surfaceHeightFn;
    private final IntCoordsToDouble abyssNoiseFn;
    private final Int3Predicate isScarletCaveFn;
    private final double floodSeedOffset;
    private final double spreadSeedOffset;
    private final double lavaSeedOffset;
    private final double barrierSeedOffset;
    private final double warpSeedOffset;

    private final int minGridX;
    private final int minGridY;
    private final int minGridZ;
    private final int gridSizeX;
    private final int gridSizeZ;
    private final long[] locationCache;
    private final int[] levelCache;
    private final BlockState[] typeCache;
    private final boolean[] statusComputed;

    private boolean shouldScheduleFluidUpdate;
    private final MutableDouble barrierScratch = new MutableDouble(Double.NaN);
    private final MutablePointContext pointScratch = new MutablePointContext();

    public static final class MutablePointContext implements DensityFunction.FunctionContext {
        private int x;
        private int y;
        private int z;

        public MutablePointContext set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public int blockX() {
            return x;
        }

        @Override
        public int blockY() {
            return y;
        }

        @Override
        public int blockZ() {
            return z;
        }
    }

    public ElderwoodsCaveAquifer(
            long seed,
            ChunkPos chunkPos,
            IntBinaryOperator surfaceHeightFn,
            IntCoordsToDouble abyssNoiseFn,
            Int3Predicate isScarletCaveFn,
            int minY,
            int height) {
        this.seed = seed;
        this.minBuildY = minY;
        this.maxBuildY = minY + height - 1;
        this.surfaceHeightFn = surfaceHeightFn;
        this.abyssNoiseFn = abyssNoiseFn;
        this.isScarletCaveFn = isScarletCaveFn != null ? isScarletCaveFn : (x, y, z) -> false;
        this.floodSeedOffset = (double) (Math.floorMod(seed, 9973L)) * 1.37;
        this.spreadSeedOffset = (double) (Math.floorMod(seed >>> 16, 8191L)) * 0.73;
        this.lavaSeedOffset = (double) (Math.floorMod(seed >>> 32, 6551L)) * 1.19;
        this.barrierSeedOffset = (double) (Math.floorMod(seed >>> 48, 4093L)) * 0.91;
        this.warpSeedOffset = (double) (Math.floorMod(seed >>> 24, 7919L)) * 1.13;

        // Pad grid bounds by +-2 chunks (32+ blocks) to safely index domain warps and cross-border probes
        this.minGridX = Math.floorDiv(chunkPos.getMinBlockX(), X_SPACING) - 2;
        int maxGridX = Math.floorDiv(chunkPos.getMaxBlockX(), X_SPACING) + 2;
        this.gridSizeX = maxGridX - this.minGridX + 1;

        this.minGridY = Math.floorDiv(minY, Y_SPACING) - 2;
        int maxGridY = Math.floorDiv(minY + height, Y_SPACING) + 2;
        int gridSizeY = maxGridY - this.minGridY + 1;

        this.minGridZ = Math.floorDiv(chunkPos.getMinBlockZ(), Z_SPACING) - 2;
        int maxGridZ = Math.floorDiv(chunkPos.getMaxBlockZ(), Z_SPACING) + 2;
        this.gridSizeZ = maxGridZ - this.minGridZ + 1;

        int cacheLen = this.gridSizeX * gridSizeY * this.gridSizeZ;
        this.locationCache = new long[cacheLen];
        this.levelCache = new int[cacheLen];
        this.typeCache = new BlockState[cacheLen];
        this.statusComputed = new boolean[cacheLen];
        Arrays.fill(this.locationCache, Long.MAX_VALUE);
    }

    /**
     * Checks whether any block within the specified radii inside the given chunk already contains
     * non-empty fluid (e.g. water, blood water, or lava from an already generated cave lake).
     */
    public static boolean hasAdjacentFluidInChunk(ChunkAccess chunk, int bx, int by, int bz, int radiusXZ, int radiusY) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int startX = Math.max(chunkMinX, bx - radiusXZ);
        int endX = Math.min(chunkMaxX, bx + radiusXZ);
        int startY = Math.max(minY, by - radiusY);
        int endY = Math.min(maxY, by + radiusY);
        int startZ = Math.max(chunkMinZ, bz - radiusXZ);
        int endZ = Math.min(chunkMaxZ, bz + radiusXZ);

        for (int nx = startX; nx <= endX; nx++) {
            for (int nz = startZ; nz <= endZ; nz++) {
                for (int ny = startY; ny <= endY; ny++) {
                    cursor.set(nx, ny, nz);
                    if (!chunk.getFluidState(cursor).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Cross-chunk safe fluid proximity check: checks already placed blocks inside the chunk and
     * mathematically predicts Voronoi aquifer fluid across chunk boundaries so carvers never
     * slice open neighboring aquifer stone dams at chunk seams.
     */
    public boolean hasAdjacentOrPredictedFluid(ChunkAccess chunk, int bx, int by, int bz, int radiusXZ, int radiusY) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int startX = bx - radiusXZ;
        int endX = bx + radiusXZ;
        int startY = Math.max(minY, by - radiusY);
        int endY = Math.min(maxY, by + radiusY);
        int startZ = bz - radiusXZ;
        int endZ = bz + radiusXZ;

        boolean prevUpdate = this.shouldScheduleFluidUpdate;
        try {
            for (int nx = startX; nx <= endX; nx++) {
                for (int nz = startZ; nz <= endZ; nz++) {
                    boolean inChunk = nx >= chunkMinX && nx <= chunkMaxX && nz >= chunkMinZ && nz <= chunkMaxZ;
                    for (int ny = startY; ny <= endY; ny++) {
                        if (inChunk) {
                            cursor.set(nx, ny, nz);
                            if (!chunk.getFluidState(cursor).isEmpty()) {
                                return true;
                            }
                        } else if (isPredictedFluidAt(nx, ny, nz)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } finally {
            this.shouldScheduleFluidUpdate = prevUpdate;
        }
    }

    private boolean isPredictedFluidAt(int x, int y, int z) {
        BlockState state = computeSubstance(this.pointScratch.set(x, y, z), -0.35);
        return state != null && !state.getFluidState().isEmpty();
    }

    @Nullable
    @Override
    public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
        if (substance > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return null;
        }

        int bx = context.blockX();
        int by = context.blockY();
        int bz = context.blockZ();

        // Smooth continuous double-precision domain warp eliminates integer quantization steps and 1-block underside holes
        double warpX = Mth.clamp(FastNoise.fbm3D(
                (float) ((bx + warpSeedOffset) * 0.018f),
                (float) (by * 0.018f),
                (float) ((bz - warpSeedOffset) * 0.018f),
                2) * 12.0f, -WARP_CLAMP_XZ, WARP_CLAMP_XZ);
        double warpY = Mth.clamp(FastNoise.fbm3D(
                (float) ((bx - warpSeedOffset) * 0.022f),
                (float) ((by + 317) * 0.022f),
                (float) ((bz + warpSeedOffset) * 0.022f),
                2) * 6.0f, -WARP_CLAMP_Y, WARP_CLAMP_Y);
        double warpZ = Mth.clamp(FastNoise.fbm3D(
                (float) ((bx + warpSeedOffset + 509) * 0.018f),
                (float) ((by - 211) * 0.018f),
                (float) ((bz + warpSeedOffset + 509) * 0.018f),
                2) * 12.0f, -WARP_CLAMP_XZ, WARP_CLAMP_XZ);

        double wx = bx + warpX;
        double wy = Mth.clamp(by + warpY, minBuildY, maxBuildY);
        double wz = bz + warpZ;

        int cellX = Math.floorDiv(Mth.floor(wx) - 5, X_SPACING);
        int cellY = Math.floorDiv(Mth.floor(wy) + 1, Y_SPACING);
        int cellZ = Math.floorDiv(Mth.floor(wz) - 5, Z_SPACING);

        double dist1 = Double.MAX_VALUE;
        double dist2 = Double.MAX_VALUE;
        double dist3 = Double.MAX_VALUE;
        long pos1 = 0L;
        long pos2 = 0L;
        long pos3 = 0L;

        for (int ox = 0; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                for (int oz = 0; oz <= 1; oz++) {
                    int gx = cellX + ox;
                    int gy = cellY + oy;
                    int gz = cellZ + oz;
                    int idx = getIndex(gx, gy, gz);
                    long centerPacked = this.locationCache[idx];
                    if (centerPacked == Long.MAX_VALUE) {
                        centerPacked = pickGridCenter(gx, gy, gz);
                        this.locationCache[idx] = centerPacked;
                    }

                    double dx = BlockPos.getX(centerPacked) - wx;
                    double dy = BlockPos.getY(centerPacked) - wy;
                    double dz = BlockPos.getZ(centerPacked) - wz;
                    double distSq = dx * dx + dy * dy + dz * dz;

                    if (dist1 >= distSq) {
                        dist3 = dist2;
                        pos3 = pos2;
                        dist2 = dist1;
                        pos2 = pos1;
                        dist1 = distSq;
                        pos1 = centerPacked;
                    } else if (dist2 >= distSq) {
                        dist3 = dist2;
                        pos3 = pos2;
                        dist2 = distSq;
                        pos2 = centerPacked;
                    } else if (dist3 >= distSq) {
                        dist3 = distSq;
                        pos3 = centerPacked;
                    }
                }
            }
        }

        int idx1 = indexFromPacked(pos1);
        ensureStatus(idx1, pos1);
        int level1 = this.levelCache[idx1];
        BlockState type1 = this.typeCache[idx1];
        // Fluid top surfaces remain flat horizontal planes at the true unwarped Y coordinate
        BlockState primaryState = stateAt(level1, type1, by);

        if (!primaryState.getFluidState().isEmpty()) {
            // Hard rock safety seal preventing any aquifer cell edge from opening directly onto Elysian Abyss
            if (abyssNoiseFn.applyAsDouble(bx, bz) > 0.10) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            }
            // Prevent outer normal-water cells from bleeding plain water into Scarlet Cave cavities
            if (primaryState.is(Blocks.WATER) && isScarletCaveFn.test(bx, by, bz)) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            }
        }

        double sim12 = similarity(dist1, dist2);
        if (sim12 <= 0.0) {
            this.shouldScheduleFluidUpdate = sim12 >= FLOWING_UPDATE_SIMILARITY && !primaryState.getFluidState().isEmpty();
            return primaryState;
        }

        MutableDouble cachedBarrier = this.barrierScratch;
        cachedBarrier.setValue(Double.NaN);

        int idx2 = indexFromPacked(pos2);
        ensureStatus(idx2, pos2);
        int level2 = this.levelCache[idx2];
        BlockState type2 = this.typeCache[idx2];

        double pressure12 = sim12 * calculatePressure(bx, by, bz, cachedBarrier, level1, type1, level2, type2);
        if (substance + pressure12 > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return null;
        }

        double sim13 = similarity(dist1, dist3);
        int idx3 = indexFromPacked(pos3);
        ensureStatus(idx3, pos3);
        int level3 = this.levelCache[idx3];
        BlockState type3 = this.typeCache[idx3];

        if (sim13 > 0.0) {
            double pressure13 = sim12 * sim13 * calculatePressure(bx, by, bz, cachedBarrier, level1, type1, level3, type3);
            if (substance + pressure13 > 0.0) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            }
        }

        double sim23 = similarity(dist2, dist3);
        if (sim23 > 0.0) {
            double pressure23 = sim12 * sim23 * calculatePressure(bx, by, bz, cachedBarrier, level2, type2, level3, type3);
            if (substance + pressure23 > 0.0) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            }
        }

        this.shouldScheduleFluidUpdate = !primaryState.getFluidState().isEmpty() && level1 > level2;
        return primaryState;
    }

    @Override
    public boolean shouldScheduleFluidUpdate() {
        return this.shouldScheduleFluidUpdate;
    }

    private int getIndex(int gx, int gy, int gz) {
        int i = gx - this.minGridX;
        int j = gy - this.minGridY;
        int k = gz - this.minGridZ;
        return (j * this.gridSizeZ + k) * this.gridSizeX + i;
    }

    private int indexFromPacked(long packed) {
        return getIndex(
                Math.floorDiv(BlockPos.getX(packed), X_SPACING),
                Math.floorDiv(BlockPos.getY(packed), Y_SPACING),
                Math.floorDiv(BlockPos.getZ(packed), Z_SPACING));
    }

    private long pickGridCenter(int gx, int gy, int gz) {
        long h = seed ^ (gx * 341873128712L) ^ (gy * 132897987541L) ^ (gz * 42317861L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h = h ^ (h >>> 31);

        int rx = Math.floorMod((int) h, X_RANGE);
        int ry = Math.floorMod((int) (h >>> 12), Y_RANGE);
        int rz = Math.floorMod((int) (h >>> 24), Z_RANGE);
        return BlockPos.asLong(gx * X_SPACING + rx, gy * Y_SPACING + ry, gz * Z_SPACING + rz);
    }

    private void ensureStatus(int idx, long packedCenter) {
        if (this.statusComputed[idx]) {
            return;
        }
        int cx = BlockPos.getX(packedCenter);
        int cy = BlockPos.getY(packedCenter);
        int cz = BlockPos.getZ(packedCenter);

        if (isNearAbyss(cx, cz)) {
            this.levelCache[idx] = WAY_BELOW_MIN_Y;
            this.typeCache[idx] = AIR;
            this.statusComputed[idx] = true;
            return;
        }

        // Leave a wide protective rock buffer around lower Aether River ravines so rivers never slice through aquifers
        if (cy <= -48 && isNearAetherRiver(cx, cz)) {
            this.levelCache[idx] = WAY_BELOW_MIN_Y;
            this.typeCache[idx] = AIR;
            this.statusComputed[idx] = true;
            return;
        }

        int localSurface = surfaceHeightFn.applyAsInt(cx, cz);

        // Keep aquifers well away from open air and protect the ocean floor basalt crust
        int minNeighborSurface = Integer.MAX_VALUE;
        for (int[] offset : SURFACE_OFFSETS) {
            int s = surfaceHeightFn.applyAsInt(cx + offset[0], cz + offset[1]);
            if (s < minNeighborSurface) {
                minNeighborSurface = s;
            }
        }

        boolean underOceanOrBeach = minNeighborSurface < ElderwoodsChunkGenerator.BASE_HEIGHT;
        int maxSafeLevel = underOceanOrBeach
                ? Math.min(minNeighborSurface - 16, 26)
                : (minNeighborSurface - 11);

        if (cy > maxSafeLevel + 10 || maxSafeLevel <= ElderwoodsChunkGenerator.MIN_Y + 8) {
            this.levelCache[idx] = WAY_BELOW_MIN_Y;
            this.typeCache[idx] = AIR;
            this.statusComputed[idx] = true;
            return;
        }

        // Sparse, cohesive regional reservoirs (~12-16% of deep cave volume instead of flooded grids)
        int depthBelowCap = Math.max(0, maxSafeLevel - cy);
        double depthT = Mth.clampedMap(depthBelowCap, 0.0, 56.0, 1.0, 0.0);
        double regionalBasin = FastNoise.fbm3D(
                (float) ((cx + floodSeedOffset) * 0.0082f),
                (float) (cy * 0.011f),
                (float) ((cz - floodSeedOffset) * 0.0082f),
                2) / 0.28;
        double localPocket = FastNoise.fbm3D(
                (float) ((cx - floodSeedOffset) * 0.020f),
                (float) (cy * 0.026f),
                (float) ((cz + floodSeedOffset) * 0.020f),
                2) / 0.28;
        double floodedness = Mth.clamp(regionalBasin * 0.65 + localPocket * 0.35, -1.0, 1.0);

        double randomFloodThreshold = Mth.lerp(depthT, 0.44, 0.88);

        int resolvedLevel;
        if (floodedness > randomFloodThreshold) {
            resolvedLevel = computeContinuousSurfaceLevel(cx, cy, cz, maxSafeLevel, localSurface, underOceanOrBeach);
            if (resolvedLevel <= -54 && isNearAetherRiver(cx, cz)) {
                resolvedLevel = WAY_BELOW_MIN_Y;
            }
        } else {
            resolvedLevel = WAY_BELOW_MIN_Y;
        }

        BlockState resolvedType = computeFluidType(cx, cy, cz, resolvedLevel, underOceanOrBeach);
        this.levelCache[idx] = resolvedLevel;
        this.typeCache[idx] = resolvedType;
        this.statusComputed[idx] = true;
    }

    private boolean isNearAbyss(int x, int z) {
        for (int[] offset : ABYSS_BUFFER_OFFSETS) {
            if (abyssNoiseFn.applyAsDouble(x + offset[0], z + offset[1]) > 0.06) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNearAetherRiver(int x, int z) {
        for (int[] offset : RIVER_BUFFER_OFFSETS) {
            if (ElderwoodsChunkGenerator.getRiverIntensity(x + offset[0], z + offset[1]) > 0.0) {
                return true;
            }
        }
        return false;
    }

    private int computeContinuousSurfaceLevel(int x, int y, int z, int maxSafeLevel, int localSurface, boolean underOcean) {
        // Continuous low-frequency 3D noise eliminates 16x16/36-block integer box steps so pockets form smooth pools
        double levelNoise = Mth.clamp(FastNoise.fbm3D(
                (float) ((x + spreadSeedOffset) * 0.0085f),
                (float) (y * 0.011f),
                (float) ((z - spreadSeedOffset) * 0.0085f),
                2) / 0.32, -1.0, 1.0);
        int cap = Math.min(maxSafeLevel, localSurface - 11);
        int floorMin = underOcean ? -104 : -106;
        // Center lake surface slightly above the Voronoi cell center (typically 3-11 blocks deep pools)
        int rawLevel = y + 6 + Mth.quantize(levelNoise * 9.0, 3);
        return Math.max(floorMin, Math.min(cap, rawLevel));
    }

    private BlockState computeFluidType(int x, int y, int z, int surfaceLevel, boolean underOcean) {
        if (surfaceLevel <= WAY_BELOW_MIN_Y) {
            return AIR;
        }
        // Keep deep sub-ocean caves purely basalt/water rather than spawning lava directly under deep seafloors
        if (!underOcean && surfaceLevel <= -24) {
            double lavaSample = FastNoise.fbm3D(
                    (float) ((x + lavaSeedOffset) * 0.009f),
                    (float) (y * 0.014f),
                    (float) ((z - lavaSeedOffset) * 0.009f),
                    2) / 0.32;
            double threshold = Mth.clampedMap(surfaceLevel, -96.0, -24.0, 0.22, 0.58);
            if (Math.abs(lavaSample) > threshold) {
                return LAVA;
            }
        }
        if (isScarletCaveFn.test(x, y, z) || isScarletCaveFn.test(x, surfaceLevel - 1, z)) {
            return BLOOD_WATER;
        }
        return WATER;
    }

    private static BlockState stateAt(int fluidLevel, BlockState fluidType, int y) {
        return y < fluidLevel ? fluidType : AIR;
    }

    private static double similarity(double firstDistSq, double secondDistSq) {
        return 1.0 - Math.abs(secondDistSq - firstDistSq) / SIMILARITY_SCALE;
    }

    private double calculatePressure(
            int bx, int by, int bz,
            MutableDouble cachedBarrier,
            int level1, BlockState type1,
            int level2, BlockState type2) {
        BlockState state1 = stateAt(level1, type1, by);
        BlockState state2 = stateAt(level2, type2, by);
        if (!state1.getFluidState().isEmpty() && !state2.getFluidState().isEmpty()
                && !state1.getBlock().equals(state2.getBlock())) {
            return 2.0;
        }

        int levelDiff = Math.abs(level1 - level2);
        if (levelDiff == 0) {
            return 0.0;
        }

        double midLevel = 0.5 * (double) (level1 + level2);
        double signedOffset = (double) by + 0.5 - midLevel;
        double halfDiff = (double) levelDiff / 2.0;
        double overlap = halfDiff - Math.abs(signedOffset);

        double levelTerm;
        if (signedOffset > 0.0) {
            levelTerm = overlap > 0.0 ? overlap / 1.5 : overlap / 2.5;
        } else {
            double airGap = 3.0 + overlap;
            levelTerm = airGap > 0.0 ? airGap / 3.0 : airGap / 10.0;
        }

        double barrier = 0.0;
        if (levelTerm >= -2.0 && levelTerm <= 2.0) {
            double stored = cachedBarrier.getValue();
            if (Double.isNaN(stored)) {
                stored = Mth.clamp(FastNoise.fbm3D(
                        (float) ((bx + barrierSeedOffset) * 0.018f),
                        (float) (by * 0.018f),
                        (float) ((bz - barrierSeedOffset) * 0.018f),
                        2) / 0.32, -1.0, 1.0);
                cachedBarrier.setValue(stored);
            }
            // Smooth low-frequency thickness bias keeps lake bottoms and walls solid without 1-block shell holes
            barrier = stored * 0.14 + 0.46;
        }

        return 2.0 * (barrier + levelTerm);
    }
}
