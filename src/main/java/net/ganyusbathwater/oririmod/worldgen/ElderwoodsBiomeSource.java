package net.ganyusbathwater.oririmod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Custom BiomeSource for the Elderwoods dimension.
 * Implements the same noise logic as ElderwoodsChunkGenerator.getComputedBiome
 * so that /locate biome works correctly.
 */
public class ElderwoodsBiomeSource extends BiomeSource {

    public static final MapCodec<ElderwoodsBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.listOf().fieldOf("biomes").forGetter(src -> src.biomes))
            .apply(instance, ElderwoodsBiomeSource::new));

    // Biome resource keys (same as in ElderwoodsChunkGenerator)
    private static final ResourceKey<Biome> ELDERWOODS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods"));
    private static final ResourceKey<Biome> SCARLET_SWAMP_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_swamp"));
    private static final ResourceKey<Biome> SCARLET_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest"));
    private static final ResourceKey<Biome> SCARLET_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_caves"));
    private static final ResourceKey<Biome> CRYSTAL_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "crystal_caves"));
    private static final ResourceKey<Biome> ELDERWOODS_CAVE_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_cave"));
    private static final ResourceKey<Biome> ELYSIAN_ABYSS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));
    private static final ResourceKey<Biome> GOLDEN_DESERT_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "golden_desert"));

    // Elder Highlands biomes
    public static final ResourceKey<Biome> ELDER_HIGHLANDS_MEADOW_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highlands_meadow"));
    public static final ResourceKey<Biome> ELDER_HIGHLAND_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_forest"));
    public static final ResourceKey<Biome> ELDER_HIGHLAND_SUMMIT_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_summit"));

    // Deep Ocean & Coastal Biomes
    public static final ResourceKey<Biome> ELDER_BEACH_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_beach"));
    public static final ResourceKey<Biome> THE_SILENT_OCEANS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_oceans"));
    public static final ResourceKey<Biome> THE_SILENT_DEPTHS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_depths"));
    public static final ResourceKey<Biome> THE_SUNKEN_TRENCHES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_sunken_trenches"));

    private final List<Holder<Biome>> biomes;

    // Pre-resolved biome holders eliminate O(N) list scans in hot worldgen loops
    private final Holder<Biome> elderwoodsBiome;
    private final Holder<Biome> scarletSwampBiome;
    private final Holder<Biome> scarletForestBiome;
    private final Holder<Biome> scarletCavesBiome;
    private final Holder<Biome> crystalCavesBiome;
    private final Holder<Biome> elderwoodsCaveBiome;
    private final Holder<Biome> elysianAbyssBiome;
    private final Holder<Biome> goldenDesertBiome;
    private final Holder<Biome> highlandMeadowBiome;
    private final Holder<Biome> highlandForestBiome;
    private final Holder<Biome> highlandSummitBiome;
    private final Holder<Biome> elderBeachBiome;
    private final Holder<Biome> silentOceansBiome;
    private final Holder<Biome> silentDepthsBiome;
    private final Holder<Biome> sunkenTrenchesBiome;

    private static volatile ElderwoodsBiomeSource activeInstance;

    // Seed offsets - injected by the chunk generator when it learns the world seed
    private double seedOffsetX = 0;
    private double seedOffsetZ = 0;
    private double seedOffsetCave = 0;

    // MultiNoise Offsets
    private double tempOffsetX = 0;
    private double tempOffsetZ = 0;
    private double humOffsetX = 0;
    private double humOffsetZ = 0;
    private double highlandOffsetX = 0;
    private double highlandOffsetZ = 0;
    private double ridgeOffsetX = 0;
    private double ridgeOffsetZ = 0;
    private double contOffsetX = 0;
    private double contOffsetZ = 0;
    private double trenchOffsetX = 0;
    private double trenchOffsetZ = 0;
    private double islandOffsetX = 0;
    private double islandOffsetZ = 0;

    private volatile boolean seedInitialized = false;
    private volatile long syncedSeed = Long.MIN_VALUE;

    private static final int HEIGHT_CACHE_SIZE = 8192;
    private static final int HEIGHT_CACHE_MASK = HEIGHT_CACHE_SIZE - 1;

    private static final class SurfaceHeightCache {
        long seed = Long.MIN_VALUE;
        final long[] keys = new long[HEIGHT_CACHE_SIZE];
        final int[] heights = new int[HEIGHT_CACHE_SIZE];
        final boolean[] occupied = new boolean[HEIGHT_CACHE_SIZE];
    }

    private final ThreadLocal<SurfaceHeightCache> surfaceHeightCache = ThreadLocal.withInitial(SurfaceHeightCache::new);
    private final ThreadLocal<SurfaceHeightCache> rawHeightCache = ThreadLocal.withInitial(SurfaceHeightCache::new);

    private static final int BIOME_CACHE_SIZE = 8192;
    private static final int BIOME_CACHE_MASK = BIOME_CACHE_SIZE - 1;

    private static final class SurfaceBiomeCache {
        long seed = Long.MIN_VALUE;
        final long[] keys = new long[BIOME_CACHE_SIZE];
        @SuppressWarnings("unchecked")
        final Holder<Biome>[] values = new Holder[BIOME_CACHE_SIZE];
    }

    private final ThreadLocal<SurfaceBiomeCache> surfaceBiomeCache = ThreadLocal.withInitial(SurfaceBiomeCache::new);

    private static final int UNDERGROUND_CACHE_SIZE = 1024;
    private static final int UNDERGROUND_CACHE_MASK = UNDERGROUND_CACHE_SIZE - 1;

    private static final class UndergroundColumnEntry {
        long key;
        boolean valid;
        Holder<Biome> surfaceBiome;
        int surfaceY;
        double contNoise;
        double abyssThreshold;
        double caveNoise;
        double humNoise;
        int scarletBottomY;
        boolean isHighland;
    }

    private static final class UndergroundColumnCache {
        long seed = Long.MIN_VALUE;
        final UndergroundColumnEntry[] entries = new UndergroundColumnEntry[UNDERGROUND_CACHE_SIZE];

        UndergroundColumnCache() {
            for (int i = 0; i < entries.length; i++) {
                entries[i] = new UndergroundColumnEntry();
            }
        }
    }

    private final ThreadLocal<UndergroundColumnCache> undergroundCache = ThreadLocal.withInitial(UndergroundColumnCache::new);

    private static final int FLAT_BEACH_GRID = 16;

    // Morphological opening parameters for filtering tiny Scarlet biome slivers & micro-swamps.
    private static final int MORPH_QUART_STEP = 4;
    private static final int SCARLET_CORE_RADIUS = 26;
    private static final int SCARLET_DILATE_QUARTS = 7; // 7 * 4 = 28 blocks (>= 26 + diagonal grid step)
    private static final int SWAMP_CORE_RADIUS = 14;
    private static final int SWAMP_DILATE_QUARTS = 4;   // 4 * 4 = 16 blocks
    private static final int MORPH_CACHE_SIZE = 8192;
    private static final int MORPH_CACHE_MASK = MORPH_CACHE_SIZE - 1;

    private static final int[][] SCARLET_DILATE_OFFSETS = buildSortedQuartOffsets(SCARLET_DILATE_QUARTS);
    private static final int[][] SWAMP_DILATE_OFFSETS = buildSortedQuartOffsets(SWAMP_DILATE_QUARTS);
    private static final int[][] SCARLET_CORE_PROBES = buildCoreProbes(SCARLET_CORE_RADIUS, true);
    private static final int[][] SWAMP_CORE_PROBES = buildCoreProbes(SWAMP_CORE_RADIUS, false);
    private static final int[][] BORDER_SAMPLE_DIRS = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
            { 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 }
    };
    private static final int[] BORDER_SAMPLE_RADII = { 14, 28, 44, 64 };

    private static final class MorphCoreCache {
        long seed = Long.MIN_VALUE;
        final long[] keys = new long[MORPH_CACHE_SIZE];
        // 0 = empty, 1 = not core, 2 = core
        final byte[] states = new byte[MORPH_CACHE_SIZE];
    }

    private final ThreadLocal<MorphCoreCache> scarletCoreCache = ThreadLocal.withInitial(MorphCoreCache::new);
    private final ThreadLocal<MorphCoreCache> swampCoreCache = ThreadLocal.withInitial(MorphCoreCache::new);

    private static int[][] buildSortedQuartOffsets(int maxQuarts) {
        int maxSq = maxQuarts * maxQuarts;
        java.util.List<int[]> list = new java.util.ArrayList<>();
        for (int dx = -maxQuarts; dx <= maxQuarts; dx++) {
            for (int dz = -maxQuarts; dz <= maxQuarts; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= maxSq) {
                    list.add(new int[] { dx, dz, d2 });
                }
            }
        }
        list.sort(java.util.Comparator.comparingInt(a -> a[2]));
        int[][] arr = new int[list.size()][2];
        for (int i = 0; i < list.size(); i++) {
            arr[i][0] = list.get(i)[0];
            arr[i][1] = list.get(i)[1];
        }
        return arr;
    }

    private static int[][] buildCoreProbes(int r, boolean includeMidRing) {
        int diag = (int) Math.round(r * 0.7071);
        int mid = r / 2;
        if (includeMidRing) {
            return new int[][] {
                    { 0, 0 },
                    { r, 0 }, { -r, 0 }, { 0, r }, { 0, -r },
                    { diag, diag }, { -diag, diag }, { diag, -diag }, { -diag, -diag },
                    { mid, 0 }, { -mid, 0 }, { 0, mid }, { 0, -mid }
            };
        }
        return new int[][] {
                { 0, 0 },
                { r, 0 }, { -r, 0 }, { 0, r }, { 0, -r },
                { diag, diag }, { -diag, diag }, { diag, -diag }, { -diag, -diag }
        };
    }
    private static final int FLAT_BEACH_CACHE_SIZE = 4096;
    private static final int FLAT_BEACH_CACHE_MASK = FLAT_BEACH_CACHE_SIZE - 1;
    private static final int[][] FLAT_BEACH_PROBES = {
            { 48, 0 }, { -48, 0 }, { 0, 48 }, { 0, -48 },
            { 34, 34 }, { -34, 34 }, { 34, -34 }, { -34, -34 },
            { 96, 0 }, { -96, 0 }, { 0, 96 }, { 0, -96 },
            { 68, 68 }, { -68, 68 }, { 68, -68 }, { -68, -68 }
    };

    private static final class FlatBeachCache {
        long seed = Long.MIN_VALUE;
        final long[] keys = new long[FLAT_BEACH_CACHE_SIZE];
        final double[] values = new double[FLAT_BEACH_CACHE_SIZE];
        final boolean[] occupied = new boolean[FLAT_BEACH_CACHE_SIZE];
    }

    private final ThreadLocal<FlatBeachCache> flatBeachCache = ThreadLocal.withInitial(FlatBeachCache::new);

    public ElderwoodsBiomeSource(List<Holder<Biome>> biomes) {
        this.biomes = biomes;
        this.elderwoodsBiome = findBiome(ELDERWOODS_KEY);
        this.scarletSwampBiome = findBiome(SCARLET_SWAMP_KEY);
        this.scarletForestBiome = findBiome(SCARLET_FOREST_KEY);
        this.scarletCavesBiome = findBiome(SCARLET_CAVES_KEY);
        this.crystalCavesBiome = findBiome(CRYSTAL_CAVES_KEY);
        this.elderwoodsCaveBiome = findBiome(ELDERWOODS_CAVE_KEY);
        this.elysianAbyssBiome = findBiome(ELYSIAN_ABYSS_KEY);
        this.goldenDesertBiome = findBiome(GOLDEN_DESERT_KEY);
        this.highlandMeadowBiome = findBiome(ELDER_HIGHLANDS_MEADOW_KEY);
        this.highlandForestBiome = findBiome(ELDER_HIGHLAND_FOREST_KEY);
        this.highlandSummitBiome = findBiome(ELDER_HIGHLAND_SUMMIT_KEY);
        this.elderBeachBiome = findBiome(ELDER_BEACH_KEY);
        this.silentOceansBiome = findBiome(THE_SILENT_OCEANS_KEY);
        this.silentDepthsBiome = findBiome(THE_SILENT_DEPTHS_KEY);
        this.sunkenTrenchesBiome = findBiome(THE_SUNKEN_TRENCHES_KEY);
        activeInstance = this;
    }

    /**
     * Static surface-height probe used by cross-chunk carvers (e.g. {@code ScarletCaveEntranceCarver}).
     */
    public static int sampleStaticSurfaceHeight(int x, int z) {
        ElderwoodsBiomeSource instance = activeInstance;
        if (instance != null) {
            return instance.computeSurfaceHeight(x, z, null);
        }
        return ElderwoodsChunkGenerator.BASE_HEIGHT;
    }

    /**
     * Ensures this BiomeSource matches the given seed, re-initializing if not yet initialized
     * or if a different world seed is provided.
     */
    public synchronized void syncSeed(long seed) {
        activeInstance = this;
        if (!seedInitialized || syncedSeed != seed) {
            net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(seed);
            seedOffsetX = random.nextDouble() * 10000.0;
            seedOffsetZ = random.nextDouble() * 10000.0;
            seedOffsetCave = random.nextDouble() * 10000.0;

            tempOffsetX = random.nextDouble() * 10000.0;
            tempOffsetZ = random.nextDouble() * 10000.0;
            humOffsetX = random.nextDouble() * 10000.0;
            humOffsetZ = random.nextDouble() * 10000.0;
            highlandOffsetX = random.nextDouble() * 10000.0;
            highlandOffsetZ = random.nextDouble() * 10000.0;
            ridgeOffsetX = random.nextDouble() * 10000.0;
            ridgeOffsetZ = random.nextDouble() * 10000.0;
            contOffsetX = random.nextDouble() * 10000.0;
            contOffsetZ = random.nextDouble() * 10000.0;
            trenchOffsetX = random.nextDouble() * 10000.0;
            trenchOffsetZ = random.nextDouble() * 10000.0;
            islandOffsetX = random.nextDouble() * 10000.0;
            islandOffsetZ = random.nextDouble() * 10000.0;

            syncedSeed = seed;
            seedInitialized = true;
        }
    }

    private void ensureSeedSynced() {
        long targetSeed = net.ganyusbathwater.oririmod.OririMod.globalWorldSeed;
        if (!seedInitialized || syncedSeed != targetSeed) {
            syncSeed(targetSeed);
        }
    }

    public boolean isSeedInitialized() {
        return seedInitialized;
    }

    public long getSyncedSeed() {
        return syncedSeed;
    }

    public double getSeedOffsetX() {
        return seedOffsetX;
    }

    public double getSeedOffsetZ() {
        return seedOffsetZ;
    }

    public double getSeedOffsetCave() {
        return seedOffsetCave;
    }

    public double getHumOffsetX() {
        return humOffsetX;
    }

    public double getHumOffsetZ() {
        return humOffsetZ;
    }

    public double getTempOffsetX() {
        return tempOffsetX;
    }

    public double getTempOffsetZ() {
        return tempOffsetZ;
    }

    public double getContOffsetX() {
        return contOffsetX;
    }

    public double getContOffsetZ() {
        return contOffsetZ;
    }

    // Broad continents + stronger positive landmass bias keep oceans smaller and rarer (~18-22% coverage)
    // while macro climate/highland/cave frequencies enlarge all biomes coherently.
    private static final float CONTINENT_FREQUENCY = 0.00042f;
    private static final double CONTINENT_LAND_BIAS = 0.35;
    private static final double OCEAN_BATHYMETRY_SPAN = 0.36;
    public static final float CLIMATE_NOISE_FREQUENCY = 0.0011f;
    // Lower frequency + 2-octave macro lobes form broad, cohesive Scarlet regions instead of thin slivers
    public static final float SCARLET_HUMIDITY_NOISE_FREQUENCY = 0.00034f;
    private static final float HIGHLAND_NOISE_FREQUENCY = 0.00025f;
    public static final float CAVE_NOISE_FREQUENCY = 0.0022f;
    public static final int SCARLET_CAVE_BASE_BOTTOM_Y = -56;

    /**
     * Smoothly undulating lower boundary of Scarlet Caves, keeping them safely above
     * the deep Aether River zone while preventing flat horizontal biome seams.
     */
    public static int getScarletCaveBottomY(int x, int z) {
        double floorWave = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) (x * 0.006f), (float) (z * 0.006f), 2) / 0.32;
        return SCARLET_CAVE_BASE_BOTTOM_Y + (int) Math.round(net.minecraft.util.Mth.clamp(floorWave, -1.0, 1.0) * 6.0);
    }

    public double sampleHumidityNoise(int x, int z) {
        double rawHum = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + humOffsetX) * SCARLET_HUMIDITY_NOISE_FREQUENCY),
                (float) ((z + humOffsetZ) * SCARLET_HUMIDITY_NOISE_FREQUENCY), 2);
        return rawHum / 0.32;
    }

    public double sampleSunkenTrenchAbsCrack(int x, int z) {
        ensureSeedSynced();
        double crackNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + trenchOffsetX) * 0.012f), (float) ((z + trenchOffsetZ) * 0.012f), 2);
        return Math.abs(crackNoise);
    }

    public double sampleCaveNoise(int x, int z) {
        return net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + seedOffsetCave) * CAVE_NOISE_FREQUENCY),
                (float) ((z + seedOffsetCave) * CAVE_NOISE_FREQUENCY),
                3);
    }

    private double sampleTemperatureNoise(int x, int z) {
        return net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + tempOffsetX) * CLIMATE_NOISE_FREQUENCY),
                (float) ((z + tempOffsetZ) * CLIMATE_NOISE_FREQUENCY), 3);
    }

    public double sampleContinentalness(int x, int z) {
        double rawCont = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + contOffsetX) * CONTINENT_FREQUENCY),
                (float) ((z + contOffsetZ) * CONTINENT_FREQUENCY), 3);
        return (rawCont / 0.32) + CONTINENT_LAND_BIAS;
    }

    public boolean isMainland(int x, int z) {
        ensureSeedSynced();
        return sampleContinentalness(x, z) >= 0.02;
    }

    /**
     * Smooth inland margin factor [0..1] keeping deep Aether Rivers cleanly inside mainland rock
     * well before reaching coastal beaches or ocean shelves.
     */
    public double getMainlandRiverFade(int x, int z) {
        ensureSeedSynced();
        double cont = sampleContinentalness(x, z);
        double t = net.minecraft.util.Mth.clamp((cont - 0.035) / 0.055, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Detects abnormally wide, flat Elder Beach / shallow coastal basins (where height barely varies
     * across hundreds of blocks) and returns a smooth [0..1] Swamp-Mangrove transformation factor.
     */
    public double getFlatBeachMangroveFactor(int x, int z) {
        ensureSeedSynced();
        int gx0 = Math.floorDiv(x, FLAT_BEACH_GRID) * FLAT_BEACH_GRID;
        int gz0 = Math.floorDiv(z, FLAT_BEACH_GRID) * FLAT_BEACH_GRID;
        double tx = (double) (x - gx0) / FLAT_BEACH_GRID;
        double tz = (double) (z - gz0) / FLAT_BEACH_GRID;

        double f00 = getGridFlatBeachFactor(gx0, gz0);
        double f10 = getGridFlatBeachFactor(gx0 + FLAT_BEACH_GRID, gz0);
        double f01 = getGridFlatBeachFactor(gx0, gz0 + FLAT_BEACH_GRID);
        double f11 = getGridFlatBeachFactor(gx0 + FLAT_BEACH_GRID, gz0 + FLAT_BEACH_GRID);

        double base = net.minecraft.util.Mth.lerp(tz,
                net.minecraft.util.Mth.lerp(tx, f00, f10),
                net.minecraft.util.Mth.lerp(tx, f01, f11));
        if (base <= 0.0) {
            return 0.0;
        }
        if (base < 0.92) {
            double wobble = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((x + seedOffsetX) * 0.035),
                    (float) ((z + seedOffsetZ) * 0.035), 2) * 0.20;
            base = net.minecraft.util.Mth.clamp(base + wobble, 0.0, 1.0);
        }
        return base;
    }

    private double getGridFlatBeachFactor(int gx, int gz) {
        FlatBeachCache cache = flatBeachCache.get();
        if (cache.seed != syncedSeed) {
            java.util.Arrays.fill(cache.occupied, false);
            cache.seed = syncedSeed;
        }
        long key = (((long) gx) << 32) ^ (gz & 0xFFFFFFFFL);
        int slot = ((gx * 73856093) ^ (gz * 19349663)) & FLAT_BEACH_CACHE_MASK;
        if (cache.occupied[slot] && cache.keys[slot] == key) {
            return cache.values[slot];
        }
        double val = computeRawFlatBeachFactor(gx, gz);
        cache.keys[slot] = key;
        cache.values[slot] = val;
        cache.occupied[slot] = true;
        return val;
    }

    private double computeRawFlatBeachFactor(int gx, int gz) {
        int centerH = getRawSurfaceHeight(gx, gz);
        if (centerH < 61 || centerH > 75) {
            return 0.0;
        }
        int flatCount = 0;
        int minH = centerH;
        int maxH = centerH;
        int remaining = FLAT_BEACH_PROBES.length;
        for (int[] probe : FLAT_BEACH_PROBES) {
            int ph = getRawSurfaceHeight(gx + probe[0], gz + probe[1]);
            remaining--;
            if (ph >= 61 && ph <= 75) {
                flatCount++;
                if (ph < minH) minH = ph;
                if (ph > maxH) maxH = ph;
                if (maxH - minH > 10) {
                    return 0.0;
                }
            }
            if (flatCount + remaining < 11) {
                return 0.0;
            }
        }
        int spread = maxH - minH;
        double countFactor = net.minecraft.util.Mth.clamp((flatCount - 10.0) / 5.0, 0.0, 1.0);
        double spreadFactor = net.minecraft.util.Mth.clamp((11.0 - spread) / 4.0, 0.0, 1.0);
        double t = countFactor * spreadFactor;
        return t * t * (3.0 - 2.0 * t);
    }

    private double sampleWarpedX(int x, int z) {
        return x + net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + seedOffsetX) * 0.0030f), (float) ((z + seedOffsetZ) * 0.0030f), 2) * 56.0;
    }

    private double sampleWarpedZ(int x, int z) {
        return z + net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + seedOffsetZ + 317) * 0.0030f), (float) ((z + seedOffsetX + 317) * 0.0030f), 2) * 56.0;
    }

    private double sampleWarpedHighlandNoise(double wx, double wz) {
        double rawHighland = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((wx + highlandOffsetX) * HIGHLAND_NOISE_FREQUENCY),
                (float) ((wz + highlandOffsetZ) * HIGHLAND_NOISE_FREQUENCY), 3);
        return rawHighland / 0.32;
    }

    private boolean isHighlandBiome(Holder<Biome> biome) {
        return biome.is(ELDER_HIGHLANDS_MEADOW_KEY)
                || biome.is(ELDER_HIGHLAND_FOREST_KEY)
                || biome.is(ELDER_HIGHLAND_SUMMIT_KEY);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        return resolveBiomeAtBlock(quartX * 4, quartY * 4, quartZ * 4);
    }

    /**
     * Evaluates the biome precisely at the given 1x1x1 block coordinate.
     * Prevents the 4x4 chunky block rendering caused by getNoiseBiome.
     */
    public Holder<Biome> getBlockBiome(int x, int y, int z) {
        return resolveBiomeAtBlock(x, y, z);
    }

    /**
     * Returns true for underground points belonging to the Scarlet Caves stratum (including shallow
     * upper aquifer chambers directly beneath mainland Scarlet Forest/Swamp before stone transitions).
     */
    public boolean isScarletCaveAt(int x, int y, int z) {
        ensureSeedSynced();
        if (sampleContinentalness(x, z) < 0.0 || sampleTemperatureNoise(x, z) > 0.15) {
            return false;
        }
        Holder<Biome> surfaceBiome = getSurfaceBiome(x, z);
        if (isHighlandBiome(surfaceBiome)
                || !(surfaceBiome.is(SCARLET_SWAMP_KEY)
                        || surfaceBiome.is(SCARLET_FOREST_KEY)
                        || surfaceBiome.is(ELDER_BEACH_KEY))) {
            return false;
        }
        int surfaceY = computeSurfaceHeight(x, z, surfaceBiome);
        if (surfaceY < 62 || y >= surfaceY - 10 || y < getScarletCaveBottomY(x, z)) {
            return false;
        }
        return sampleHumidityNoise(x, z) > 0.48;
    }

    private Holder<Biome> resolveBiomeAtBlock(int x, int y, int z) {
        ensureSeedSynced();

        UndergroundColumnCache uCache = undergroundCache.get();
        if (uCache.seed != syncedSeed) {
            for (UndergroundColumnEntry entry : uCache.entries) {
                entry.valid = false;
            }
            uCache.seed = syncedSeed;
        }

        long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        int slot = ((x * 73856093) ^ (z * 19349663)) & UNDERGROUND_CACHE_MASK;
        UndergroundColumnEntry col = uCache.entries[slot];

        if (!col.valid || col.key != key) {
            col.key = key;
            col.surfaceBiome = getSurfaceBiome(x, z);
            col.surfaceY = computeSurfaceHeight(x, z, col.surfaceBiome);
            col.contNoise = sampleContinentalness(x, z);
            col.abyssThreshold = getDynamicAbyssThreshold(x, z);
            col.isHighland = isHighlandBiome(col.surfaceBiome);
            if (col.surfaceY >= 58) {
                col.caveNoise = sampleCaveNoise(x, z);
                boolean eligibleScarlet = !col.isHighland
                        && col.surfaceBiome != goldenDesertBiome
                        && col.contNoise >= 0.0;
                if (eligibleScarlet) {
                    col.humNoise = sampleHumidityNoise(x, z);
                    col.scarletBottomY = getScarletCaveBottomY(x, z);
                } else {
                    col.humNoise = -2.0;
                    col.scarletBottomY = Integer.MAX_VALUE;
                }
            } else {
                col.humNoise = -2.0;
                col.scarletBottomY = Integer.MAX_VALUE;
            }
            col.valid = true;
        }

        if (y >= col.surfaceY - 16) {
            return col.surfaceBiome;
        }

        if (col.surfaceY < 58) {
            if (col.surfaceY <= -112) {
                return sunkenTrenchesBiome;
            } else if (col.contNoise < col.abyssThreshold && col.surfaceY < 35) {
                return silentDepthsBiome;
            } else {
                return col.surfaceBiome;
            }
        }

        // Strictly zero Elysian Abyss or other biomes inside Scarlet Caves territory.
        // Elysian Abyss caverns taper closed smoothly in rock before humNoise reaches 0.44.
        if (col.humNoise > 0.48 && y >= col.scarletBottomY) {
            return scarletCavesBiome;
        }
        if (col.caveNoise > 0.20 && col.humNoise <= 0.44) {
            return elysianAbyssBiome;
        }
        if (col.caveNoise < -0.15 && col.caveNoise > -0.2 && col.humNoise <= 0.46) {
            return crystalCavesBiome;
        }
        return elderwoodsCaveBiome;
    }

    /**
     * Unified 2D MultiNoise map for surface biomes (Temperature/Humidity).
     * Enforces realistic climatic zonation and strict concentric biome containment:
     * Golden Desert (Hot) -> Lowlands (Temperate) -> Highland Meadow (Cool) ->
     * Highland Forest (Cold) -> Highland Summit (Freezing Alpine Crest).
     */
    public Holder<Biome> getSurfaceBiome(int x, int z) {
        ensureSeedSynced();
        SurfaceBiomeCache cache = surfaceBiomeCache.get();
        if (cache.seed != syncedSeed) {
            Arrays.fill(cache.values, null);
            cache.seed = syncedSeed;
        }
        long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        int slot = ((x * 73856093) ^ (z * 19349663)) & BIOME_CACHE_MASK;
        Holder<Biome> cached = cache.values[slot];
        if (cached != null && cache.keys[slot] == key) {
            return cached;
        }

        Holder<Biome> resolved = computeSurfaceBiome(x, z);
        cache.keys[slot] = key;
        cache.values[slot] = resolved;
        return resolved;
    }

    private Holder<Biome> computeSurfaceBiome(int x, int z) {
        Holder<Biome> raw = computeRawSurfaceBiome(x, z);

        if (raw == scarletSwampBiome) {
            if (!hasMorphCoreNear(x, z, true, swampCoreCache.get(), SWAMP_DILATE_OFFSETS, SWAMP_CORE_PROBES)) {
                raw = scarletForestBiome;
            }
        }

        if (raw == scarletForestBiome || raw == scarletSwampBiome) {
            if (!hasMorphCoreNear(x, z, false, scarletCoreCache.get(), SCARLET_DILATE_OFFSETS, SCARLET_CORE_PROBES)) {
                int surfaceY = computeSurfaceHeight(x, z, null);
                return pickBorderingNonScarletBiome(x, z, surfaceY);
            }
        }

        return raw;
    }

    private Holder<Biome> computeRawSurfaceBiome(int x, int z) {
        // 0. Master Continentalness Axis: Oceanic Domain vs Coastal Beach vs Continental Landmass
        double contNoise = sampleContinentalness(x, z);
        int surfaceY = computeSurfaceHeight(x, z, null);

        // 3. Oceanic Domain (Y < 58)
        if (surfaceY < 58) {
            double abyssThreshold = getDynamicAbyssThreshold(x, z);
            if (contNoise < abyssThreshold && surfaceY < 35) {
                double absCrack = sampleSunkenTrenchAbsCrack(x, z);
                if (absCrack < 0.038 && surfaceY <= -112) {
                    return sunkenTrenchesBiome;
                }
                return silentDepthsBiome;
            }
            return silentOceansBiome;
        }

        double tempNoise = sampleTemperatureNoise(x, z);

        // 1. Coastal Beach Band (Y=58 to 73): allow Scarlet Forest to directly border Elder Beach
        // without injecting an artificial Elderwoods strip, while shielding coastal sand from inland swamps.
        if (surfaceY < 74) {
            if (tempNoise <= 0.15) {
                double humNoise = sampleHumidityNoise(x, z);
                boolean inHighlandFringe = false;
                if (contNoise > 0.24 && tempNoise < -0.02 && humNoise <= 0.46) {
                    double wx = sampleWarpedX(x, z);
                    double wz = sampleWarpedZ(x, z);
                    inHighlandFringe = sampleWarpedHighlandNoise(wx, wz) > -0.08;
                }
                if (!inHighlandFringe) {
                    if (contNoise >= 0.06 && humNoise > 0.72) {
                        return scarletSwampBiome;
                    }
                    if (humNoise > 0.46 && surfaceY >= ElderwoodsChunkGenerator.BASE_HEIGHT + 1) {
                        return scarletForestBiome;
                    }
                }
            }
            return elderBeachBiome;
        }

        // 2. Hot Climate: Golden Desert (T > 0.15)
        if (tempNoise > 0.15) {
            return goldenDesertBiome;
        }

        double humNoise = sampleHumidityNoise(x, z);

        // 3. Elder Highlands Continental System (strictly deep continental interior:
        // contNoise > 0.28, Cold/Temperate: T < -0.02, away from desert buffer and Scarlet macro basins)
        // Use the exact same domain-warped coordinates (wx, wz) as computeSurfaceHeight so
        // physical mountain flanks and surface biomes never drift out of alignment.
        double wx = sampleWarpedX(x, z);
        double wz = sampleWarpedZ(x, z);
        double highlandNoise = sampleWarpedHighlandNoise(wx, wz);

        // Highlands yield cohesive Scarlet humidity zones (humNoise >= 0.44) while slightly broadening
        // highlandNoise (-0.04) so overall Highland coverage stays unchanged without slicing Scarlet into slivers.
        if (contNoise > 0.28 && tempNoise < -0.02 && humNoise < 0.44 && highlandNoise > -0.04) {
            double r1 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + ridgeOffsetX) * 0.0011f), (float) ((wz + ridgeOffsetZ) * 0.0011f), 2));
            double r2 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + ridgeOffsetX + 333) * 0.0022f), (float) ((wz + ridgeOffsetZ + 333) * 0.0022f), 2));
            double ridgeNoise = r1 * 0.72 + r2 * 0.28;

            // Summits sit strictly inside full climate support (humNoise < 0.36, contNoise > 0.31, tempNoise < -0.12)
            // so mountain peaks always reach full elevation instead of deflating near borders.
            if (highlandNoise > 0.66 && ridgeNoise > 0.72 && tempNoise < -0.12 && humNoise < 0.36 && contNoise > 0.31) {
                return highlandSummitBiome;
            } else if (highlandNoise > 0.30 && humNoise < 0.40) {
                return highlandForestBiome;
            } else {
                return highlandMeadowBiome;
            }
        }

        // Thin cold foothill separator ring when humidity is below core Scarlet range
        if (contNoise > 0.24 && tempNoise < -0.02 && highlandNoise > -0.08 && humNoise <= 0.46) {
            return elderwoodsBiome;
        }

        // 4. Lowland Woodland Biomes (Normalized macro Humidity controlled; keep Scarlet Swamp inland behind forest berm)
        if (contNoise >= 0.06 && humNoise > 0.72) {
            return scarletSwampBiome;
        } else if (humNoise > 0.46) {
            return scarletForestBiome;
        } else {
            return elderwoodsBiome;
        }
    }

    private boolean matchesMorphTarget(Holder<Biome> biome, boolean swampOnly) {
        return swampOnly
                ? biome == scarletSwampBiome
                : (biome == scarletForestBiome || biome == scarletSwampBiome);
    }

    private boolean isMorphCoreQuart(int qx, int qz, boolean swampOnly, MorphCoreCache cache, int[][] probes) {
        if (cache.seed != syncedSeed) {
            Arrays.fill(cache.states, (byte) 0);
            cache.seed = syncedSeed;
        }
        long key = (((long) qx) << 32) ^ (qz & 0xFFFFFFFFL);
        int slot = ((qx * 73856093) ^ (qz * 19349663)) & MORPH_CACHE_MASK;
        byte cached = cache.states[slot];
        if (cached != 0 && cache.keys[slot] == key) {
            return cached == 2;
        }

        int bx = qx * MORPH_QUART_STEP;
        int bz = qz * MORPH_QUART_STEP;
        boolean isCore = true;
        for (int i = 0; i < probes.length; i++) {
            if (!matchesMorphTarget(computeRawSurfaceBiome(bx + probes[i][0], bz + probes[i][1]), swampOnly)) {
                isCore = false;
                break;
            }
        }
        cache.keys[slot] = key;
        cache.states[slot] = (byte) (isCore ? 2 : 1);
        return isCore;
    }

    private boolean hasMorphCoreNear(int x, int z, boolean swampOnly, MorphCoreCache cache,
                                     int[][] offsets, int[][] probes) {
        int baseQx = Math.floorDiv(x, MORPH_QUART_STEP);
        int baseQz = Math.floorDiv(z, MORPH_QUART_STEP);
        for (int i = 0; i < offsets.length; i++) {
            int qx = baseQx + offsets[i][0];
            int qz = baseQz + offsets[i][1];
            if (isMorphCoreQuart(qx, qz, swampOnly, cache, probes)) {
                int cx = qx * MORPH_QUART_STEP;
                int cz = qz * MORPH_QUART_STEP;
                int dx = cx - x;
                int dz = cz - z;
                if (dx * dx + dz * dz <= 64
                        || matchesMorphTarget(computeRawSurfaceBiome((x + cx) >> 1, (z + cz) >> 1), swampOnly)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Holder<Biome> pickBorderingNonScarletBiome(int x, int z, int surfaceY) {
        int elderwoodsCount = 0;
        int beachCount = 0;
        int desertCount = 0;
        int meadowCount = 0;
        int forestCount = 0;
        int bestNonScarletDist = Integer.MAX_VALUE;
        Holder<Biome> nearestLandBiome = null;

        for (int rIdx = 0; rIdx < BORDER_SAMPLE_RADII.length; rIdx++) {
            int r = BORDER_SAMPLE_RADII[rIdx];
            int diagR = (int) Math.round(r * 0.7071);
            for (int d = 0; d < BORDER_SAMPLE_DIRS.length; d++) {
                int step = (d < 4) ? r : diagR;
                int px = x + BORDER_SAMPLE_DIRS[d][0] * step;
                int pz = z + BORDER_SAMPLE_DIRS[d][1] * step;
                Holder<Biome> b = computeRawSurfaceBiome(px, pz);
                if (b == scarletForestBiome || b == scarletSwampBiome
                        || b == silentOceansBiome || b == silentDepthsBiome || b == sunkenTrenchesBiome) {
                    continue;
                }
                if (b == elderwoodsBiome) {
                    elderwoodsCount++;
                } else if (b == elderBeachBiome) {
                    beachCount++;
                } else if (b == goldenDesertBiome) {
                    desertCount++;
                } else if (b == highlandMeadowBiome) {
                    meadowCount++;
                } else if (b == highlandForestBiome) {
                    forestCount++;
                }
                if (r < bestNonScarletDist) {
                    bestNonScarletDist = r;
                    nearestLandBiome = b;
                }
            }
        }

        if (surfaceY < 74 && beachCount > 0) {
            return elderBeachBiome;
        }
        if (elderwoodsCount > 0) {
            return elderwoodsBiome;
        }

        Holder<Biome> dominant = nearestLandBiome != null ? nearestLandBiome : elderwoodsBiome;
        int bestCount = 0;
        if (meadowCount > bestCount) { bestCount = meadowCount; dominant = highlandMeadowBiome; }
        if (forestCount > bestCount) { bestCount = forestCount; dominant = highlandForestBiome; }
        if (desertCount > bestCount) { bestCount = desertCount; dominant = goldenDesertBiome; }
        if (beachCount > bestCount) { bestCount = beachCount; dominant = elderBeachBiome; }
        return dominant;
    }

    /**
     * Unified Terrain Shaping: Uses domain warping, multi-octave alpine fractals,
     * and continuous climate blending.
     * Eliminates artificial terracing steps and ensures mountains never form
     * vertical cliffs into deserts or oceans.
     */
    public int computeSurfaceHeight(int x, int z, Holder<Biome> surfaceBiome) {
        ensureSeedSynced();
        return getCachedOrComputeHeight(surfaceHeightCache.get(), x, z, true);
    }

    private int getRawSurfaceHeight(int x, int z) {
        return getCachedOrComputeHeight(rawHeightCache.get(), x, z, false);
    }

    private int getCachedOrComputeHeight(SurfaceHeightCache cache, int x, int z, boolean smoothed) {
        if (cache.seed != syncedSeed) {
            java.util.Arrays.fill(cache.occupied, false);
            cache.seed = syncedSeed;
        }
        long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        int slot = ((x * 73856093) ^ (z * 19349663)) & HEIGHT_CACHE_MASK;
        if (cache.occupied[slot] && cache.keys[slot] == key) {
            return cache.heights[slot];
        }

        int h = smoothed ? computeSmoothedSurfaceHeight(x, z) : computeRawSurfaceHeight(x, z);
        cache.keys[slot] = key;
        cache.heights[slot] = h;
        cache.occupied[slot] = true;
        return h;
    }

    private int computeSmoothedSurfaceHeight(int x, int z) {
        int h = getRawSurfaceHeight(x, z);

        // Relax steep underwater coastal / island aprons using a C1 distance-faded multi-tier cone.
        // Smooth radial and lower-depth attenuation guarantee zero circular edge steps or cutoff cliffs.
        boolean coneApplied = false;
        double contNoise = sampleContinentalness(x, z);
        if (contNoise < 0.22 && h >= -8 && h < 70) {
            final double maxAllowedSlope = 1.15;
            final double shoreCap = 71.0;
            double coneH = extendConeOnLattice(x, z, h, h, 1, 0, 4, shoreCap, maxAllowedSlope);
            coneH = extendConeOnLattice(x, z, h, coneH, 2, 16, 12, shoreCap, maxAllowedSlope);
            coneH = extendConeOnLattice(x, z, h, coneH, 4, 144, 28, shoreCap, maxAllowedSlope);
            double depthWeight = hermite01((h + 8.0) / 28.0);
            double blendedH = net.minecraft.util.Mth.lerp(depthWeight, h, coneH);
            if (blendedH > h) {
                h = (int) Math.round(blendedH);
                coneApplied = true;
            }
        }

        // Suppress isolated 1x1 sand block pimples and 1x1 water pinholes along flat beach terraces
        // deterministically across chunk borders without affecting hills, cone ramps, or ocean basins.
        if (!coneApplied && h >= ElderwoodsChunkGenerator.BASE_HEIGHT - 1 && h <= ElderwoodsChunkGenerator.BASE_HEIGHT + 3) {
            int n1 = getRawSurfaceHeight(x + 1, z);
            int n2 = getRawSurfaceHeight(x - 1, z);
            int n3 = getRawSurfaceHeight(x, z + 1);
            int n4 = getRawSurfaceHeight(x, z - 1);
            int maxNeighbor = Math.max(Math.max(n1, n2), Math.max(n3, n4));
            int minNeighbor = Math.min(Math.min(n1, n2), Math.min(n3, n4));
            if (h > maxNeighbor) {
                h = maxNeighbor;
            } else if (h < minNeighbor) {
                h = minNeighbor;
            }
        }
        return h;
    }

    private double extendConeOnLattice(int x, int z, int baseRawH, double coneH, int step, int minDistSq, int maxRadius,
                                       double shoreCap, double maxAllowedSlope) {
        double maxR = (shoreCap - baseRawH) / maxAllowedSlope;
        if (maxR <= 0.0 || maxR * maxR <= minDistSq) {
            return coneH;
        }
        int limit = Math.min(maxRadius, (int) Math.ceil(maxR));
        int alignMask = ~(step - 1);
        int minGx = (x - limit + (step - 1)) & alignMask;
        int maxGx = (x + limit) & alignMask;
        int minGz = (z - limit + (step - 1)) & alignMask;
        int maxGz = (z + limit) & alignMask;
        double maxDistSq = Math.min((double) maxRadius * maxRadius, maxR * maxR);

        for (int gx = minGx; gx <= maxGx; gx += step) {
            int dx = gx - x;
            int dx2 = dx * dx;
            for (int gz = minGz; gz <= maxGz; gz += step) {
                int dz = gz - z;
                int d2 = dx2 + dz * dz;
                if (d2 <= minDistSq || d2 > maxDistSq) {
                    continue;
                }
                double dist = Math.sqrt(d2);
                double maxRawLift = (shoreCap - dist * maxAllowedSlope) - baseRawH;
                if (maxRawLift <= 0.0) {
                    continue;
                }
                double distFade = (dist <= 20.0) ? 1.0 : hermite01((28.0 - dist) / 8.0);
                // Exact branch-and-bound: even at shoreCap, this point cannot beat current coneH
                if (baseRawH + maxRawLift * distFade <= coneH) {
                    continue;
                }
                double sampleH = Math.min((double) getRawSurfaceHeight(gx, gz), shoreCap);
                double rawLift = (sampleH - dist * maxAllowedSlope) - baseRawH;
                if (rawLift > 0.0) {
                    double candidate = baseRawH + rawLift * distFade;
                    if (candidate > coneH) {
                        coneH = candidate;
                    }
                }
            }
        }
        return coneH;
    }

    public static double hermite01(double v) {
        double c = net.minecraft.util.Mth.clamp(v, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    private int computeRawSurfaceHeight(int x, int z) {
        // Sample MultiNoise for Biome Placement
        double tempNoise = sampleTemperatureNoise(x, z);
        double humNoise = sampleHumidityNoise(x, z);
        double contNoise = sampleContinentalness(x, z);

        // Blending weights centered on Biome thresholds; restrict Scarlet Swamp basin depression inland
        // so coastal Scarlet rims form dry Scarlet Forest shelves right onto the beach instead of leaking water steps.
        double desertWeight = net.minecraft.util.Mth.clamp((tempNoise - 0.05) / 0.20, 0.0, 1.0);
        double swampInland = hermite01((contNoise - 0.055) / 0.045);
        double swampWeight = net.minecraft.util.Mth.clamp((humNoise - 0.50) / 0.24, 0.0, 1.0) * swampInland;

        // Domain Warping for organic, non-symmetrical mountain and hill contours
        double wx = sampleWarpedX(x, z);
        double wz = sampleWarpedZ(x, z);

        // 1. Base Rolling Hills (Elderwoods / Scarlet Forest)
        double nx = (wx + seedOffsetX) * 0.0021;
        double nz = (wz + seedOffsetZ) * 0.0021;
        // Elevated baseline (centered at +8.0) ensures valleys naturally curve smoothly
        // above water level
        double baseNoise = 8.0 + Math.sin(nx) * Math.cos(nz) * 9.0;
        baseNoise += Math.sin(nx * 0.5 + 2.0) * Math.cos(nz * 0.6 + 1.1) * 4.5;

        // 2. Swamp Basin
        double bumpyNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + seedOffsetX) * 0.05), (float) ((z + seedOffsetZ) * 0.05), 2) * 3.0;
        double swampHeight = -3.0 + bumpyNoise;

        // Blend Swamp into Base Hills
        double terrainHeight = net.minecraft.util.Mth.lerp(swampWeight, baseNoise, swampHeight);

        // 3. Golden Desert Dunes
        double duneNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + seedOffsetX) * 0.03), (float) ((z + seedOffsetZ) * 0.03), 2) * 5.0;
        // Keep interior/coastal desert dunes safely above sea level so dune troughs never punch saltwater craters into dunes
        double desertHeight = Math.max((terrainHeight * 0.45) + 2.8 + duneNoise * 0.85, 2.4);

        // Blend Desert over base terrain
        double finalNoise = net.minecraft.util.Mth.lerp(desertWeight, terrainHeight, desertHeight);

        // 4. Elder Highlands Continental Mountain System (Massive ranges Y=80 to Y=215)
        // Ramps reach 1.0 strictly outside Forest/Summit thresholds so peaks never deflate near biome edges
        double tempFade = net.minecraft.util.Mth.clamp((0.02 - tempNoise) / 0.10, 0.0, 1.0);
        double humFade = net.minecraft.util.Mth.clamp((0.50 - humNoise) / 0.12, 0.0, 1.0);
        double contFade = net.minecraft.util.Mth.clamp((contNoise - 0.20) / 0.10, 0.0, 1.0);
        double climateSupport = tempFade * humFade * contFade;

        double highlandNoise = sampleWarpedHighlandNoise(wx, wz);

        double highlandWeight = net.minecraft.util.Mth.clamp((highlandNoise - (-0.10)) / 0.18, 0.0, 1.0);
        double effectiveHighlandWeight = highlandWeight * climateSupport;

        if (effectiveHighlandWeight > 0.0) {
            double smoothT = effectiveHighlandWeight * effectiveHighlandWeight * (3.0 - 2.0 * effectiveHighlandWeight);

            // A. Base Elevation Lift: elevates Highlands Meadows noticeably above Elderwoods baseline
            double baseLift = 18.0 + smoothT * 16.0;

            // B. Asymmetrical Alpine Slopes & Glacial Valleys
            double alpineSlope = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + seedOffsetX) * 0.0018f), (float) ((wz + seedOffsetZ) * 0.0018f), 3) * 26.0;
            double cragDetail = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) (x * 0.015f), (float) (z * 0.015f), 2) * 7.0;
            double microRoughness = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) (x * 0.04f), (float) (z * 0.04f), 2) * 3.0;

            // C. Mountain Flank / Pine Forest Lift (Y=115..155)
            double forestFactor = net.minecraft.util.Mth.clamp((highlandNoise - 0.26) / 0.35, 0.0, 1.0);
            double forestLift = forestFactor * 38.0;

            // D. Alpine Ridged Peak Formation (narrow razor-sharp crests thrusting to Y=175..220)
            double r1 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + ridgeOffsetX) * 0.0011f), (float) ((wz + ridgeOffsetZ) * 0.0011f), 2));
            double r2 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + ridgeOffsetX + 333) * 0.0022f), (float) ((wz + ridgeOffsetZ + 333) * 0.0022f), 2));
            double ridgeCombined = r1 * 0.72 + r2 * 0.28;
            double ridgeVal = Math.max(0.0, ridgeCombined - 0.32) / 0.68;
            double ridgePeak = ridgeVal * ridgeVal * ridgeVal;

            double summitFactor = net.minecraft.util.Mth.clamp((highlandNoise - 0.56) / 0.24, 0.0, 1.0);
            double summitRidgeBoost = summitFactor * net.minecraft.util.Mth.clamp((ridgeCombined - 0.64) / 0.14, 0.0, 1.0) * 24.0;
            double peakLift = ridgePeak * 88.0 * summitFactor + summitRidgeBoost;

            double valleyClampedSlope = net.minecraft.util.Mth.lerp(summitFactor * 0.75, alpineSlope, Math.max(alpineSlope, -4.0));
            double totalHighlandBonus = baseLift + valleyClampedSlope + cragDetail + microRoughness + forestLift + peakLift;
            finalNoise = net.minecraft.util.Mth.lerp(smoothT, finalNoise, terrainHeight + totalHighlandBonus);
        }

        // 5. Continental Protection, Coastline Unification & Bathymetric Carving
        // Sample Island Noise with restrained domain-warped coordinates + multi-scale coastline jaggedness
        // so islands form organic bays without folding contours into shear cliffs.
        double islandWarpX = wx + net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + islandOffsetX + 2713) * 0.0018f), (float) ((z + islandOffsetZ - 1831) * 0.0018f), 2) * 68.0;
        double islandWarpZ = wz + net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + islandOffsetX - 3917) * 0.0018f), (float) ((z + islandOffsetZ + 2243) * 0.0018f), 2) * 68.0;

        double rawIsland = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((islandWarpX + islandOffsetX) * 0.00068f), (float) ((islandWarpZ + islandOffsetZ) * 0.00068f), 3);
        double coastDetail = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((wx + islandOffsetX + 641) * 0.0032f), (float) ((wz + islandOffsetZ - 487) * 0.0032f), 2) * 0.14
                + net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + islandOffsetX) * 0.0070f), (float) ((z + islandOffsetZ) * 0.0070f), 1) * 0.04;
        // Fade coastline detail cleanly so deep-sea shelves stay broad while emerged island shores carve gentle bays
        double detailFade = net.minecraft.util.Mth.clamp(((rawIsland / 0.32) + 0.15) / 0.55, 0.25, 1.0);
        double islandNoise = (rawIsland / 0.32) + coastDetail * detailFade;

        // A. Mainland Coastal Slope & Shoreline (tight natural beaches transitioning crisply into water)
        double mainlandY = ElderwoodsChunkGenerator.BASE_HEIGHT + finalNoise;

        // Inland-to-Coast Smooth Taper (contNoise from 0.050 down to 0.012):
        double landCoastT = net.minecraft.util.Mth.clamp((contNoise - 0.012) / 0.038, 0.0, 1.0);
        double smoothLandCoast = landCoastT * landCoastT * (3.0 - 2.0 * landCoastT);
        double targetCoastY = ElderwoodsChunkGenerator.BASE_HEIGHT
                + net.minecraft.util.Mth.clamp(finalNoise * 0.45 + 1.5, 2.2, 4.2);
        double taperedMainlandY = net.minecraft.util.Mth.lerp(smoothLandCoast, targetCoastY, mainlandY);

        // Crisp Coastal Beach (contNoise from 0.012 down to -0.014):
        // Steps quickly from forest grade across thin sand beach down to submerged shallow water (Y=66.5)
        double beachT = net.minecraft.util.Mth.clamp((0.012 - contNoise) / 0.026, 0.0, 1.0);
        double smoothBeach = beachT * beachT * (3.0 - 2.0 * beachT);
        double mainlandShoreY = net.minecraft.util.Mth.lerp(smoothBeach, taperedMainlandY, ElderwoodsChunkGenerator.BASE_HEIGHT - 5.5);

        // B. Ocean Bathymetry (Shallow photic shelf -> Continental slope -> Deep abyssal plains)
        // oceanProgress: 0.0 at coast (contNoise = 0.0), 1.0 at deep abyss (contNoise = -OCEAN_BATHYMETRY_SPAN)
        double oceanProgress = net.minecraft.util.Mth.clamp((-contNoise) / OCEAN_BATHYMETRY_SPAN, 0.0, 1.0);

        // Continental Shelf Dynamic Modulation:
        double dynamicShelfEdge = computeDynamicShelfEdge(x, z);
        double dynamicAbyssEdge = computeDynamicAbyssEdge(dynamicShelfEdge);

        double oceanY;
        if (oceanProgress < dynamicShelfEdge) {
            // Photic Continental Shelf (Silent Oceans): slopes gently from shallow water (Y=58) to shelf floor (Y=42)
            double shelfFrac = oceanProgress / dynamicShelfEdge;
            double s = shelfFrac * shelfFrac * (3.0 - 2.0 * shelfFrac);
            oceanY = net.minecraft.util.Mth.lerp(s, ElderwoodsChunkGenerator.BASE_HEIGHT - 4.0, 42.0);
            // Rolling sandbanks and dunes across the expansive ocean floor (warped organic FBM, zero periodic grids)
            double sandbankNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + islandOffsetX + 1103) * 0.011f), (float) ((wz + islandOffsetZ - 881) * 0.011f), 2);
            oceanY += (sandbankNoise / 0.32) * 2.2 * (1.0 - shelfFrac);
        } else if (oceanProgress < dynamicAbyssEdge) {
            // Continental Slope: gradual descent from photic shelf floor (Y=42) down to abyssal floor (Y=-98)
            double slopeFrac = (oceanProgress - dynamicShelfEdge) / Math.max(0.18, dynamicAbyssEdge - dynamicShelfEdge);
            double hermite = slopeFrac * slopeFrac * (3.0 - 2.0 * slopeFrac);
            double s = 0.72 * hermite + 0.28 * slopeFrac;

            // Gentle crag roughness vanishing cleanly at top (Y=42) and bottom (Y=-98)
            double slopeEnvelope = Math.sin(slopeFrac * Math.PI);
            double j1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) (x * 0.025f), (float) (z * 0.025f), 2) * 3.0;
            double j2 = (1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.noise2D(
                    (float) ((x + 417) * 0.055f), (float) ((z + 417) * 0.055f)))) * 1.8;
            double slopeJagged = (j1 + j2) * slopeEnvelope;

            oceanY = net.minecraft.util.Mth.lerp(s, 42.0, -98.0) + slopeJagged;
        } else {
            // Deep Abyssal Rift Valleys & Spiked Mountain Chains (Silent Depths):
            double abyssalFrac = (oceanProgress - dynamicAbyssEdge) / Math.max(0.01, 1.0 - dynamicAbyssEdge);
            abyssalFrac = net.minecraft.util.Mth.clamp(abyssalFrac, 0.0, 1.0);
            double smoothAbyssal = abyssalFrac * abyssalFrac * (3.0 - 2.0 * abyssalFrac);

            // 1. Deep Rift Valley Basin Axis:
            double valleyBaseY = -104.0;
            double vNoise1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((x + trenchOffsetX + 1983) * 0.0028f), (float) ((z + trenchOffsetZ + 1983) * 0.0028f), 2);
            double valleyAxis = Math.abs(vNoise1); // 0 along central rift floor, > 0.18 at valley walls

            // 2. Dense Spiked Mountain Ridges & Needle Pinnacles:
            double r1 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((x + seedOffsetX + 555) * 0.0055f), (float) ((z + seedOffsetZ + 555) * 0.0055f), 2));
            double r2 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((x + seedOffsetZ + 999) * 0.0110f), (float) ((z + seedOffsetX + 999) * 0.0110f), 2));
            double r3 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.noise2D(
                    (float) ((x + 333) * 0.0180f), (float) ((z + 333) * 0.0180f)));
            double combinedRidge = r1 * 0.55 + r2 * 0.32 + r3 * 0.13;

            // Power exponent creates needle-sharp mountain crests and steep pyramid flanks
            double spikeFactor = Math.pow(Math.max(0.0, (combinedRidge - 0.28) / 0.72), 2.8);

            // Spiked mountains flank the rift valleys and rise in high amounts: up to 72 blocks above valley floor!
            double valleyWallBoost = net.minecraft.util.Mth.clamp(valleyAxis / 0.18, 0.25, 1.0);
            double spikedMountainLift = spikeFactor * 72.0 * valleyWallBoost;

            // Gentle floor undulation in deep valley troughs
            double valleyFloorCurve = (net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + trenchOffsetX) * 0.0085f), (float) ((wz + trenchOffsetZ) * 0.0085f), 2) / 0.32) * 3.8;

            // Combine into full abyssal valley topography
            double abyssalTerrainY = valleyBaseY + valleyFloorCurve + spikedMountainLift;
            oceanY = net.minecraft.util.Mth.lerp(smoothAbyssal, -98.0, abyssalTerrainY);

            // Seamounts in the open ocean (Underwater Mountain Ranges rising to photic zone)
            double m1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) (x * 0.0014f), (float) (z * 0.0014f), 3) / 0.32;
            double m2 = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((x + 719) * 0.0030f), (float) ((z + 719) * 0.0030f), 2) / 0.32;
            double combinedMount = m1 * 0.75 + m2 * 0.25;

            if (combinedMount > -0.22) {
                double mtT = net.minecraft.util.Mth.clamp((combinedMount - (-0.22)) / 0.85, 0.0, 1.0);
                double mtCurve = mtT * mtT * (3.0 - 2.0 * mtT);
                double mtLift = mtCurve * 142.0; // rises up to Y=44!
                oceanY += mtLift * smoothAbyssal;
            }

            // Sunken Trenches / Bedrock fault cracks: smooth Hermite drop down to bedrock layer (-118)
            double crackNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((x + trenchOffsetX) * 0.012f), (float) ((z + trenchOffsetZ) * 0.012f), 2);
            double absCrack = Math.abs(crackNoise);
            if (absCrack < 0.038) {
                double crackT = absCrack / 0.038;
                double dropFactor = (1.0 - crackT) * (1.0 - crackT) * (3.0 - 2.0 * (1.0 - crackT));
                double depthFade = net.minecraft.util.Mth.clamp((-75.0 - oceanY) / 20.0, 0.0, 1.0);
                double crackTargetY = -118.0;
                oceanY = net.minecraft.util.Mth.lerp(dropFactor * depthFade, oceanY, crackTargetY);
            }
        }

        // C. Continuous Coastal Blending (Seamless unification of mainland and ocean)
        // Coastline transition zone: contNoise between -0.018 and 0.018
        double coastWeight = net.minecraft.util.Mth.clamp((contNoise - (-0.018)) / 0.036, 0.0, 1.0);
        double smoothCoast = coastWeight * coastWeight * (3.0 - 2.0 * coastWeight);
        double baseTerrainY = net.minecraft.util.Mth.lerp(smoothCoast, oceanY, mainlandShoreY);

        // D. Island Seamounts & Dynamic Expansive Island Marine Shelves
        // Samples regional marine shelf noise so every island tapers gradually into surrounding seabed with zero sheer cliffs.
        double rawIslandShelfMod = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + islandOffsetX + 7821) * 0.00065f), (float) ((z + islandOffsetZ + 7821) * 0.00065f), 2);
        double islandShelfMod = net.minecraft.util.Mth.clamp(rawIslandShelfMod / 0.32, -0.75, 1.0);

        double shelfNoiseStart = Math.min(-0.14, -0.34 - 0.24 * islandShelfMod);
        double apronNoiseStart = shelfNoiseStart - (0.34 + 0.12 * net.minecraft.util.Mth.clamp(islandShelfMod, -0.6, 1.0));

        double finalTerrainY = baseTerrainY;
        if (islandNoise > apronNoiseStart) {
            double islandTargetY;
            if (islandNoise > 0.50) {
                // Island Interior (Y=74+): wider, softer rolling hills rather than steep pointed cones
                double t = net.minecraft.util.Mth.clamp((islandNoise - 0.50) / 0.42, 0.0, 1.0);
                double smoothT = t * t * (3.0 - 2.0 * t);
                double islandRoll = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                        (float) ((wx + seedOffsetX + 219) * 0.0068f), (float) ((wz + seedOffsetZ - 411) * 0.0068f), 3) / 0.32;
                double organicRelief = Math.max(0.8, 3.2 + Math.max(finalNoise * 0.28, -0.8) + islandRoll * 2.1);
                islandTargetY = ElderwoodsChunkGenerator.BASE_HEIGHT + 2.0 + smoothT * organicRelief;
            } else if (islandNoise > 0.34) {
                // Beach Zone (Y=56 to 74) - broad sandy shore passing smoothly through sea level (Y=72)
                double t = (islandNoise - 0.34) / 0.16;
                double smoothT = t * t * (3.0 - 2.0 * t);
                islandTargetY = net.minecraft.util.Mth.lerp(smoothT, 56.0, 74.0);
            } else if (islandNoise > shelfNoiseStart) {
                // Photic Silent Ocean Shelf (Y=40 to 56) - gentle marine shelf around the island
                double shelfT = (islandNoise - shelfNoiseStart) / (0.34 - shelfNoiseStart);
                double smoothShelf = shelfT * shelfT * (3.0 - 2.0 * shelfT);
                double shelfRollNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                        (float) ((wx + islandOffsetX + 1423) * 0.013f), (float) ((wz + islandOffsetZ - 997) * 0.013f), 2) / 0.32;
                double shelfRoll = shelfRollNoise * 2.8 * (shelfT * (1.0 - shelfT));
                islandTargetY = net.minecraft.util.Mth.lerp(smoothShelf, 40.0, 56.0) + shelfRoll;
            } else {
                // Submarine apron / ramp (sloping smoothly from surrounding seafloor baseTerrainY up to Y=40)
                double apronT = (islandNoise - apronNoiseStart) / (shelfNoiseStart - apronNoiseStart);
                double smoothApron = apronT * apronT * (3.0 - 2.0 * apronT);
                islandTargetY = net.minecraft.util.Mth.lerp(smoothApron, baseTerrainY, 40.0);
            }

            // Smooth fillet radius k fades cleanly across the outer submarine apron
            double k = 6.5 * net.minecraft.util.Mth.clamp((islandNoise - apronNoiseStart) / 0.14, 0.0, 1.0);
            finalTerrainY = smoothMax(baseTerrainY, islandTargetY, k);
        }

        // D2. Photic-Shelf Archipelagos, Island Arcs & Atoll Shoals (gated strictly off coasts & abyssal slopes)
        finalTerrainY = ArchipelagoTerrainHelper.applyArchipelagoTerrain(
                x, z, contNoise, oceanProgress, dynamicShelfEdge, finalTerrainY);

        // E. Sheltered Coastal Lagoons: strictly excluded near Scarlet regions so Y=72 lagoons never border
        // Y=70 Scarlet Swamps, with macro-scale committed basins eliminating <10-block micro-puddles.
        double lagoonScarletFade = hermite01((0.32 - humNoise) / 0.14);
        double lagoonCoastWeight = hermite01((0.010 - contNoise) / 0.022) * lagoonScarletFade;
        if (lagoonCoastWeight > 0.0 && finalTerrainY > 58.0 && finalTerrainY < 74.8) {
            double rawLagoonZone = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + islandOffsetX + 5209) * 0.0017f), (float) ((wz + islandOffsetZ - 4127) * 0.0017f), 2);
            double lagoonZone = hermite01(((rawLagoonZone / 0.32) - 0.18) / 0.14);
            double bandWeight = hermite01((finalTerrainY - 58.0) / 5.0)
                    * hermite01((74.8 - finalTerrainY) / 1.8);
            double basinMask = lagoonCoastWeight * lagoonZone * bandWeight;
            if (basinMask > 0.52) {
                double lagoonField = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                        (float) ((wx + seedOffsetX + 7919) * 0.0038f), (float) ((wz + seedOffsetZ - 6131) * 0.0038f), 2) / 0.32;
                if (lagoonField < -0.22) {
                    double bankCurve = hermite01((-0.22 - lagoonField) / 0.16)
                            * hermite01((basinMask - 0.52) / 0.26);
                    double bedUndulation = (net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                            (float) (x * 0.038f), (float) (z * 0.038f), 2) / 0.32) * 1.4;
                    double lagoonBedY = 62.5 + bedUndulation;
                    finalTerrainY = net.minecraft.util.Mth.lerp(bankCurve, finalTerrainY, Math.min(finalTerrainY, lagoonBedY));
                }
            }
        }

        // --- DYNAMIC SWAMP SHORELINE (SPILL PREVENTION) ---
        // Strictly applies only on deep inland mainland where Scarlet Swamp meets adjacent biomes
        if (contNoise > 0.05) {
            double landFade = net.minecraft.util.Mth.clamp((contNoise - 0.05) / 0.08, 0.0, 1.0);
            double distToHumBorder = Math.abs(swampWeight - 0.5) * 2.0;
            double distToTempBorder = Math.abs(desertWeight - 0.5) * 2.0;

            double ridgeForce = 0.0;
            if (swampWeight > 0.5) {
                double closestEdge = Math.min(distToHumBorder, distToTempBorder);
                ridgeForce = Math.max(0.0, 1.0 - (closestEdge * closestEdge));
            } else {
                ridgeForce = Math.max(0.0, 1.0 - (distToHumBorder * distToHumBorder));
            }

            if (ridgeForce > 0.0) {
                double ridgeMinimum = ElderwoodsChunkGenerator.BASE_HEIGHT + 0.5;
                finalTerrainY = net.minecraft.util.Mth.lerp(ridgeForce * landFade, finalTerrainY, Math.max(finalTerrainY, ridgeMinimum));
            }
        }

        return (int) Math.round(finalTerrainY);
    }

    /**
     * Smooth Polynomial Maximum (smax) using a quadratic fillet.
     * Guarantees C1 continuity when blending intersecting terrain surfaces,
     * eliminating hard cut-offs, sharp creases, and stepped cliffs.
     */
    public static double smoothMax(double a, double b, double k) {
        if (k <= 0.0) {
            return Math.max(a, b);
        }
        double diff = a - b;
        if (diff >= k) {
            return a;
        }
        if (diff <= -k) {
            return b;
        }
        double h = 0.5 + 0.5 * diff / k;
        return net.minecraft.util.Mth.lerp(h, b, a) + k * h * (1.0 - h);
    }

    private double computeDynamicShelfEdge(int x, int z) {
        double rawShelfMod = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + contOffsetX + 4391) * 0.00040f), (float) ((z + contOffsetZ + 4391) * 0.00040f), 2);
        double shelfModNoise = net.minecraft.util.Mth.clamp(rawShelfMod / 0.32, -1.0, 1.0);
        return 0.44 + 0.08 * shelfModNoise;
    }

    private static double computeDynamicAbyssEdge(double dynamicShelfEdge) {
        return net.minecraft.util.Mth.clamp(dynamicShelfEdge + 0.36, 0.72, 0.88);
    }

    /**
     * Computes the dynamic continentalness threshold for the deep abyssal floor (Silent Depths),
     * dynamically varying based on regional continental shelf width.
     */
    public double getDynamicAbyssThreshold(int x, int z) {
        double dynamicAbyssEdge = computeDynamicAbyssEdge(computeDynamicShelfEdge(x, z));
        return -(dynamicAbyssEdge * OCEAN_BATHYMETRY_SPAN);
    }

    /**
     * Find a biome holder by resource key from our cached list.
     */
    private Holder<Biome> findBiome(ResourceKey<Biome> key) {
        for (Holder<Biome> holder : biomes) {
            if (holder.is(key)) {
                return holder;
            }
        }
        // Fallback to first biome if not found
        return biomes.get(0);
    }
}
