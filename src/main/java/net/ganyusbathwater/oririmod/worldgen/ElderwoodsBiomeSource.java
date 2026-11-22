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
    public static final ResourceKey<Biome> SCARLET_KARST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_karst"));
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
    public static final ResourceKey<Biome> GOLDEN_SAVANNA_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "golden_savanna"));
    public static final ResourceKey<Biome> GOLDEN_SAVANNA_MESA_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "golden_savanna_mesa"));

    // Elder Highlands biomes
    public static final ResourceKey<Biome> ELDER_HIGHLANDS_MEADOW_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highlands_meadow"));
    public static final ResourceKey<Biome> ELDER_HIGHLAND_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_forest"));
    public static final ResourceKey<Biome> ELDER_HIGHLAND_SUMMIT_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_summit"));

    // Valley & Riparian Biome
    public static final ResourceKey<Biome> ELDER_VALLEY_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_valley"));

    // Deep Ocean & Coastal Biomes
    public static final ResourceKey<Biome> ELDER_BEACH_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_beach"));
    public static final ResourceKey<Biome> ELDER_LAGOON_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_lagoon"));
    public static final ResourceKey<Biome> THE_SILENT_OCEANS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_oceans"));
    public static final ResourceKey<Biome> THE_SILENT_DEPTHS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_silent_depths"));
    public static final ResourceKey<Biome> THE_SUNKEN_TRENCHES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "the_sunken_trenches"));

    // Vile Plains & Paired Vile Caves Biomes
    public static final ResourceKey<Biome> VILE_PLAINS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vile_plains"));
    public static final ResourceKey<Biome> VILE_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vile_caves"));

    private final List<Holder<Biome>> biomes;

    // Pre-resolved biome holders eliminate O(N) list scans in hot worldgen loops
    private final Holder<Biome> elderwoodsBiome;
    private final Holder<Biome> scarletSwampBiome;
    private final Holder<Biome> scarletForestBiome;
    private final Holder<Biome> scarletKarstBiome;
    private final Holder<Biome> scarletCavesBiome;
    private final Holder<Biome> crystalCavesBiome;
    private final Holder<Biome> elderwoodsCaveBiome;
    private final Holder<Biome> elysianAbyssBiome;
    private final Holder<Biome> goldenDesertBiome;
    private final Holder<Biome> goldenSavannaBiome;
    private final Holder<Biome> goldenSavannaMesaBiome;
    private final Holder<Biome> highlandMeadowBiome;
    private final Holder<Biome> highlandForestBiome;
    private final Holder<Biome> highlandSummitBiome;
    private final Holder<Biome> elderValleyBiome;
    private final Holder<Biome> elderBeachBiome;
    private final Holder<Biome> elderLagoonBiome;
    private final Holder<Biome> silentOceansBiome;
    private final Holder<Biome> silentDepthsBiome;
    private final Holder<Biome> sunkenTrenchesBiome;
    private final Holder<Biome> vilePlainsBiome;
    private final Holder<Biome> vileCavesBiome;

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
        int vileBottomY;
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

    private final ThreadLocal<UndergroundColumnCache> undergroundCache = ThreadLocal
            .withInitial(UndergroundColumnCache::new);

    private static final int FLAT_BEACH_GRID = 16;

    // Morphological opening parameters for filtering tiny Scarlet biome slivers &
    // micro-swamps.
    private static final int MORPH_QUART_STEP = 4;
    private static final int SCARLET_CORE_RADIUS = 26;
    private static final int SCARLET_DILATE_QUARTS = 7; // 7 * 4 = 28 blocks (>= 26 + diagonal grid step)
    private static final int SWAMP_CORE_RADIUS = 14;
    private static final int SWAMP_DILATE_QUARTS = 4; // 4 * 4 = 16 blocks
    private static final int VILE_CORE_RADIUS = 22;
    private static final int VILE_DILATE_QUARTS = 6; // 6 * 4 = 24 blocks
    private static final int ARID_CORE_RADIUS = 24;
    private static final int ARID_DILATE_QUARTS = 7; // 7 * 4 = 28 blocks (>= 24 + diagonal grid step)
    private static final int MORPH_CACHE_SIZE = 8192;
    private static final int MORPH_CACHE_MASK = MORPH_CACHE_SIZE - 1;

    private static final int[][] SCARLET_DILATE_OFFSETS = buildSortedQuartOffsets(SCARLET_DILATE_QUARTS);
    private static final int[][] SWAMP_DILATE_OFFSETS = buildSortedQuartOffsets(SWAMP_DILATE_QUARTS);
    private static final int[][] VILE_DILATE_OFFSETS = buildSortedQuartOffsets(VILE_DILATE_QUARTS);
    private static final int[][] ARID_DILATE_OFFSETS = buildSortedQuartOffsets(ARID_DILATE_QUARTS);
    private static final int[][] SCARLET_CORE_PROBES = buildCoreProbes(SCARLET_CORE_RADIUS, true);
    private static final int[][] SWAMP_CORE_PROBES = buildCoreProbes(SWAMP_CORE_RADIUS, false);
    private static final int[][] VILE_CORE_PROBES = buildCoreProbes(VILE_CORE_RADIUS, true);
    private static final int[][] ARID_CORE_PROBES = buildCoreProbes(ARID_CORE_RADIUS, true);
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
    private final ThreadLocal<MorphCoreCache> vileCoreCache = ThreadLocal.withInitial(MorphCoreCache::new);
    private final ThreadLocal<MorphCoreCache> aridCoreCache = ThreadLocal.withInitial(MorphCoreCache::new);

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

    private static final int VALLEY_CACHE_SIZE = 4096;
    private static final int VALLEY_CACHE_MASK = VALLEY_CACHE_SIZE - 1;

    private static final class ValleyCache {
        long seed = Long.MIN_VALUE;
        final long[] keys = new long[VALLEY_CACHE_SIZE];
        final ElderValleyTerrainHelper.ValleyColumnInfo[] infos = new ElderValleyTerrainHelper.ValleyColumnInfo[VALLEY_CACHE_SIZE];
        final boolean[] occupied = new boolean[VALLEY_CACHE_SIZE];

        ValleyCache() {
            for (int i = 0; i < VALLEY_CACHE_SIZE; i++) {
                infos[i] = new ElderValleyTerrainHelper.ValleyColumnInfo();
            }
        }
    }

    private final ThreadLocal<ValleyCache> valleyColumnCache = ThreadLocal.withInitial(ValleyCache::new);
    private final ElderValleyTerrainHelper valleyTerrainHelper = new ElderValleyTerrainHelper();
    private final ElderValleyTerrainHelper.BaseTerrainLookup valleyTerrainLookup = new ElderValleyTerrainHelper.BaseTerrainLookup() {
        @Override
        public double sampleContinentalness(int x, int z) {
            return ElderwoodsBiomeSource.this.sampleContinentalness(x, z);
        }

        @Override
        public double sampleTemperature(int x, int z) {
            return ElderwoodsBiomeSource.this.sampleTemperatureNoise(x, z);
        }

        @Override
        public double sampleHumidity(int x, int z) {
            return ElderwoodsBiomeSource.this.sampleHumidityNoise(x, z);
        }

        @Override
        public double sampleHighland(int x, int z) {
            double wx = sampleWarpedX(x, z);
            double wz = sampleWarpedZ(x, z);
            return sampleWarpedHighlandNoise(wx, wz);
        }

        @Override
        public int sampleBaseHeight(int x, int z) {
            return getRawSurfaceHeight(x, z);
        }

        @Override
        public double sampleIslandNoise(int x, int z) {
            double wx = sampleWarpedX(x, z);
            double wz = sampleWarpedZ(x, z);
            return computeIslandNoiseValue(x, z, wx, wz);
        }
    };

    public ElderwoodsBiomeSource(List<Holder<Biome>> biomes) {
        this.biomes = biomes;
        this.elderwoodsBiome = findBiome(ELDERWOODS_KEY);
        this.scarletSwampBiome = findBiome(SCARLET_SWAMP_KEY);
        this.scarletForestBiome = findBiome(SCARLET_FOREST_KEY);
        this.scarletKarstBiome = findBiome(SCARLET_KARST_KEY);
        this.scarletCavesBiome = findBiome(SCARLET_CAVES_KEY);
        this.crystalCavesBiome = findBiome(CRYSTAL_CAVES_KEY);
        this.elderwoodsCaveBiome = findBiome(ELDERWOODS_CAVE_KEY);
        this.elysianAbyssBiome = findBiome(ELYSIAN_ABYSS_KEY);
        this.goldenDesertBiome = findBiome(GOLDEN_DESERT_KEY);
        this.goldenSavannaBiome = findBiome(GOLDEN_SAVANNA_KEY);
        this.goldenSavannaMesaBiome = findBiome(GOLDEN_SAVANNA_MESA_KEY);
        this.highlandMeadowBiome = findBiome(ELDER_HIGHLANDS_MEADOW_KEY);
        this.highlandForestBiome = findBiome(ELDER_HIGHLAND_FOREST_KEY);
        this.highlandSummitBiome = findBiome(ELDER_HIGHLAND_SUMMIT_KEY);
        this.elderValleyBiome = findBiome(ELDER_VALLEY_KEY);
        this.elderBeachBiome = findBiome(ELDER_BEACH_KEY);
        this.elderLagoonBiome = findBiome(ELDER_LAGOON_KEY);
        this.silentOceansBiome = findBiome(THE_SILENT_OCEANS_KEY);
        this.silentDepthsBiome = findBiome(THE_SILENT_DEPTHS_KEY);
        this.sunkenTrenchesBiome = findBiome(THE_SUNKEN_TRENCHES_KEY);
        this.vilePlainsBiome = findBiome(VILE_PLAINS_KEY);
        this.vileCavesBiome = findBiome(VILE_CAVES_KEY);
        activeInstance = this;
    }

    /**
     * Static surface-height probe used by cross-chunk carvers (e.g.
     * {@code ScarletCaveEntranceCarver}).
     */
    public static int sampleStaticSurfaceHeight(int x, int z) {
        ElderwoodsBiomeSource instance = activeInstance;
        if (instance != null) {
            return instance.computeSurfaceHeight(x, z, null);
        }
        return ElderwoodsChunkGenerator.BASE_HEIGHT;
    }

    /**
     * Returns the most recently initialized {@link ElderwoodsBiomeSource}, or
     * {@code null} if inactive.
     */
    public static ElderwoodsBiomeSource getActiveInstance() {
        return activeInstance;
    }

    /**
     * Ensures this BiomeSource matches the given seed, re-initializing if not yet
     * initialized
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

            valleyTerrainHelper.resetForSeed(seed);
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

    // Broad continents + stronger positive landmass bias keep oceans smaller and
    // rarer (~18-22% coverage)
    // while macro climate/highland/cave frequencies enlarge all biomes coherently.
    private static final float CONTINENT_FREQUENCY = 0.00042f;
    private static final double CONTINENT_LAND_BIAS = 0.35;
    private static final double OCEAN_BATHYMETRY_SPAN = 0.36;
    public static final float CLIMATE_NOISE_FREQUENCY = 0.0011f;
    // Lower frequency + 2-octave macro lobes form broad, cohesive Scarlet regions
    // instead of thin slivers
    public static final float SCARLET_HUMIDITY_NOISE_FREQUENCY = 0.00034f;
    private static final float HIGHLAND_NOISE_FREQUENCY = 0.00025f;
    public static final float CAVE_NOISE_FREQUENCY = 0.0022f;
    public static final int SCARLET_CAVE_BASE_BOTTOM_Y = -56;
    public static final int VILE_CAVE_BASE_BOTTOM_Y = -68;
    public static final double VILE_TEMP_THRESHOLD = -0.14;

    /**
     * Smoothly undulating lower boundary of Scarlet Caves, keeping them safely
     * above
     * the deep Aether River zone while preventing flat horizontal biome seams.
     */
    public static int getScarletCaveBottomY(int x, int z) {
        double floorWave = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) (x * 0.006f), (float) (z * 0.006f), 2) / 0.32;
        return SCARLET_CAVE_BASE_BOTTOM_Y + (int) Math.round(net.minecraft.util.Mth.clamp(floorWave, -1.0, 1.0) * 6.0);
    }

    /**
     * Smoothly undulating lower boundary of Vile Caves deep inside the deepslate stratum.
     */
    public static int getVileCaveBottomY(int x, int z) {
        double floorWave = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + 713.0f) * 0.006f), (float) ((z - 419.0f) * 0.006f), 2) / 0.32;
        return VILE_CAVE_BASE_BOTTOM_Y + (int) Math.round(net.minecraft.util.Mth.clamp(floorWave, -1.0, 1.0) * 6.0);
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

    public double sampleTemperatureNoise(int x, int z) {
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
     * Smooth inland margin factor [0..1] keeping deep Aether Rivers cleanly inside
     * mainland rock
     * well before reaching coastal beaches or ocean shelves.
     */
    public double getMainlandRiverFade(int x, int z) {
        ensureSeedSynced();
        double cont = sampleContinentalness(x, z);
        double t = net.minecraft.util.Mth.clamp((cont - 0.035) / 0.055, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Returns the unified [0..1] Elder Lagoon mangrove-feature factor, strictly
     * confined inside
     * dedicated {@code elder_lagoon} biomes (well away from Scarlet biomes and open
     * beaches).
     */
    public double getFlatBeachMangroveFactor(int x, int z) {
        ensureSeedSynced();
        if (!getSurfaceBiome(x, z).is(ELDER_LAGOON_KEY)) {
            return 0.0;
        }
        double factor = getLagoonFeatureFactor(x, z);
        if (!Double.isNaN(factor)) {
            return factor;
        }
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
                if (ph < minH)
                    minH = ph;
                if (ph > maxH)
                    maxH = ph;
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

    private double computeEffectiveHighland(double wx, double wz, double contNoise, double tempNoise, double humNoise) {
        double tempFade = hermite01((-0.02 - tempNoise) / 0.12);
        double humFade = hermite01((0.44 - humNoise) / 0.10);
        double contFade = hermite01((contNoise - 0.30) / 0.08);
        double climateSupport = tempFade * humFade * contFade;
        if (climateSupport <= 0.0) {
            return 0.0;
        }
        double highlandNoise = sampleWarpedHighlandNoise(wx, wz);
        return Math.max(0.0, highlandNoise) * climateSupport;
    }

    /**
     * Modulates high-altitude highland massifs by localized 2D peak isolation noise so
     * summits form compact alpine peaks surrounded by wide Forest and Meadow skirts
     * rather than spanning 1000+ block continuous plateaus.
     */
    private double computeSummitPeakIntensity(double wx, double wz, double effectiveHighland) {
        if (effectiveHighland <= 0.66) {
            return 0.0;
        }
        double peakNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((wx + ridgeOffsetX + 731) * 0.0017f),
                (float) ((wz + ridgeOffsetZ - 419) * 0.0017f), 2) / 0.32;
        double peakIsolation = net.minecraft.util.Mth.clamp(0.5 + 0.5 * peakNoise, 0.0, 1.0);
        double summitBase = Math.max(0.0, effectiveHighland - 0.66) / 0.42;
        return summitBase * peakIsolation;
    }

    private boolean isHighlandBiome(Holder<Biome> biome) {
        return biome.is(ELDER_HIGHLANDS_MEADOW_KEY)
                || biome.is(ELDER_HIGHLAND_FOREST_KEY)
                || biome.is(ELDER_HIGHLAND_SUMMIT_KEY);
    }

    public boolean isAridBiome(Holder<Biome> biome) {
        return biome == goldenDesertBiome
                || biome == goldenSavannaBiome
                || biome == goldenSavannaMesaBiome
                || biome.is(GOLDEN_DESERT_KEY)
                || biome.is(GOLDEN_SAVANNA_KEY)
                || biome.is(GOLDEN_SAVANNA_MESA_KEY);
    }

    private double computeEffectiveMesa(double wx, double wz, double contNoise, double tempNoise) {
        double tempFade = hermite01((tempNoise - 0.09) / 0.05);
        double contFade = hermite01((contNoise - 0.11) / 0.06);
        double support = tempFade * contFade;
        if (support <= 0.0) {
            return 0.0;
        }
        double rawMesa = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((wx + highlandOffsetX + 4129) * 0.00052f),
                (float) ((wz + highlandOffsetZ - 2897) * 0.00052f), 2) / 0.24;
        return Math.max(0.0, rawMesa) * support;
    }

    private double computeTablemountainPlateauFactor(double wx, double wz, double effectiveMesa) {
        if (effectiveMesa <= 0.14) {
            return 0.0;
        }
        double lowerCliff = hermite01((effectiveMesa - 0.14) / 0.06);
        double upperCliff = hermite01((effectiveMesa - 0.21) / 0.06);
        return 0.42 * lowerCliff + 0.58 * upperCliff;
    }

    private double computeEffectiveKarst(double wx, double wz, double contNoise, double tempNoise, double humNoise) {
        double humFade = hermite01((humNoise - 0.46) / 0.08);
        double swampExclude = hermite01((0.71 - humNoise) / 0.07);
        double tempFade = hermite01((0.08 - tempNoise) / 0.06);
        double contFade = hermite01((contNoise - 0.14) / 0.06);
        double support = humFade * swampExclude * tempFade * contFade;
        if (support <= 0.0) {
            return 0.0;
        }
        double rawKarst = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((wx + highlandOffsetX - 3187) * 0.00058f),
                (float) ((wz + highlandOffsetZ + 1943) * 0.00058f), 2) / 0.25;
        return Math.max(0.0, rawKarst) * support;
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
     * Returns true for underground points belonging to the Scarlet Caves stratum
     * (including shallow
     * upper aquifer chambers directly beneath mainland Scarlet Forest/Swamp before
     * stone transitions).
     */
    public boolean isScarletCaveAt(int x, int y, int z) {
        ensureSeedSynced();
        if (sampleContinentalness(x, z) < 0.0) {
            return false;
        }
        Holder<Biome> surfaceBiome = getSurfaceBiome(x, z);
        if (isHighlandBiome(surfaceBiome)
                || isAridBiome(surfaceBiome)
                || surfaceBiome == vilePlainsBiome
                || surfaceBiome == silentOceansBiome
                || surfaceBiome == silentDepthsBiome
                || surfaceBiome == sunkenTrenchesBiome) {
            return false;
        }
        int surfaceY = computeSurfaceHeight(x, z, surfaceBiome);
        if (surfaceY < 58 || y >= surfaceY - 10 || y < getScarletCaveBottomY(x, z) - 4) {
            return false;
        }
        boolean isScarletSurface = surfaceBiome == scarletSwampBiome
                || surfaceBiome == scarletForestBiome
                || surfaceBiome == scarletKarstBiome;
        return isScarletSurface || sampleHumidityNoise(x, z) > 0.43;
    }

    /**
     * Returns true strictly for underground points directly beneath Vile Plains
     * down to the undulating Vile Caves deepslate floor.
     */
    public boolean isVileCaveAt(int x, int y, int z) {
        ensureSeedSynced();
        Holder<Biome> surfaceBiome = getSurfaceBiome(x, z);
        if (surfaceBiome != vilePlainsBiome) {
            return false;
        }
        int surfaceY = computeSurfaceHeight(x, z, surfaceBiome);
        if (surfaceY < 62 || y >= surfaceY - 14 || y < getVileCaveBottomY(x, z)) {
            return false;
        }
        return true;
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
                        && !isAridBiome(col.surfaceBiome)
                        && col.surfaceBiome != vilePlainsBiome
                        && col.contNoise >= 0.0;
                if (eligibleScarlet) {
                    col.humNoise = sampleHumidityNoise(x, z);
                    col.scarletBottomY = getScarletCaveBottomY(x, z);
                } else {
                    col.humNoise = -2.0;
                    col.scarletBottomY = Integer.MAX_VALUE;
                }
                col.vileBottomY = (col.surfaceBiome == vilePlainsBiome)
                        ? getVileCaveBottomY(x, z)
                        : Integer.MAX_VALUE;
            } else {
                col.humNoise = -2.0;
                col.scarletBottomY = Integer.MAX_VALUE;
                col.vileBottomY = Integer.MAX_VALUE;
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

        // Vile Plains & Vile Caves are strictly paired 1-to-1: never one without the other.
        if (col.surfaceBiome == vilePlainsBiome && y >= col.vileBottomY) {
            return vileCavesBiome;
        }

        // Strictly zero Elysian Abyss or other biomes inside Scarlet Caves territory.
        // Elysian Abyss caverns taper closed smoothly in rock before humNoise reaches
        // 0.44.
        if (col.humNoise > 0.46 && y >= col.scarletBottomY) {
            return scarletCavesBiome;
        }
        if (col.caveNoise > 0.20 && col.humNoise <= 0.44 && col.surfaceBiome != vilePlainsBiome) {
            return elysianAbyssBiome;
        }
        if (col.caveNoise < -0.15 && col.caveNoise > -0.2 && col.humNoise <= 0.46 && col.surfaceBiome != vilePlainsBiome) {
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

        // Filter out tiny isolated Golden Savanna / Golden Desert micro-blobs so temperature
        // noise ripples never punch small yellow savanna puddles inside Scarlet or Elderwoods.
        if (isAridBiome(raw)) {
            final MorphCoreCache aridCache = aridCoreCache.get();
            if (!ElderCoastalHelper.hasSmoothMorphCore(x, z, MORPH_QUART_STEP, 0.045f,
                    (cx, cz) -> hasAridMorphCoreNear(cx, cz, aridCache))) {
                raw = computeNonAridFallbackBiome(x, z);
            }
        }

        if (raw == scarletSwampBiome) {
            final MorphCoreCache swampCache = swampCoreCache.get();
            if (!ElderCoastalHelper.hasSmoothMorphCore(x, z, MORPH_QUART_STEP, 0.055f,
                    (cx, cz) -> hasMorphCoreNear(cx, cz, true, swampCache, SWAMP_DILATE_OFFSETS, SWAMP_CORE_PROBES))) {
                raw = scarletForestBiome;
            }
        }

        if (raw == scarletForestBiome || raw == scarletKarstBiome || raw == scarletSwampBiome) {
            final MorphCoreCache scarletCache = scarletCoreCache.get();
            if (!ElderCoastalHelper.hasSmoothMorphCore(x, z, MORPH_QUART_STEP, 0.045f,
                    (cx, cz) -> hasMorphCoreNear(cx, cz, false, scarletCache, SCARLET_DILATE_OFFSETS,
                            SCARLET_CORE_PROBES))) {
                int surfaceY = computeSurfaceHeight(x, z, null);
                Holder<Biome> fallback = pickBorderingNonScarletBiome(x, z, surfaceY);
                if (fallback == elderBeachBiome && sampleContinentalness(x, z) >= 0.055) {
                    return elderwoodsBiome;
                }
                return fallback;
            }
        }

        if (raw == vilePlainsBiome) {
            final MorphCoreCache vileCache = vileCoreCache.get();
            if (!ElderCoastalHelper.hasSmoothMorphCore(x, z, MORPH_QUART_STEP, 0.045f,
                    (cx, cz) -> hasVileMorphCoreNear(cx, cz, vileCache))) {
                return elderwoodsBiome;
            }
        }

        return raw;
    }

    private Holder<Biome> computeRawSurfaceBiome(int x, int z) {
        // 0. Master Continentalness Axis: Oceanic Domain vs Coastal Beach vs
        // Continental Landmass
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

        // 0.5 Elder Valley Corridor: check BEFORE coastal beach fallback so inland
        // river channels (Y=67..73)
        // never misclassify as Elder Beach, and estuaries keep elder_valley cleanly
        // until the ocean shelf.
        ElderValleyTerrainHelper.ValleyColumnInfo valleyInfo = getOrEvaluateValley(x, z, Integer.MIN_VALUE);
        if (valleyInfo.hasValley) {
            if (valleyInfo.inWaterChannel && contNoise >= -0.015) {
                return elderValleyBiome;
            }
            if (valleyInfo.biomeMask >= 0.42 && !valleyInfo.isEstuary && contNoise >= 0.015) {
                return elderValleyBiome;
            }
        }

        // 1. Coastal Zone & Dedicated Elder Lagoon (Y=58 to 73/74):
        // Sheltered Lagoons occupy calm, low-humidity basins cleanly separated from
        // Scarlet biomes;
        // outer sandy shores (Y <= 73) remain Elder Beach while inland Scarlet Swamps
        // stay well inland (cont >= 0.14).
        if (surfaceY <= 74 && tempNoise <= 0.08) {
            double humNoiseCheck = sampleHumidityNoise(x, z);
            if (humNoiseCheck < 0.24) {
                double lagoonWeight = computeLagoonBiomeWeight(x, z, contNoise, tempNoise, humNoiseCheck, surfaceY);
                if (lagoonWeight >= 0.40) {
                    return elderLagoonBiome;
                }
            }
        }

        if (surfaceY < 74) {
            double humNoise = sampleHumidityNoise(x, z);
            if (tempNoise <= 0.09) {
                boolean inHighlandFringe = false;
                if (contNoise > 0.26 && tempNoise < -0.02 && humNoise <= 0.46) {
                    double wx = sampleWarpedX(x, z);
                    double wz = sampleWarpedZ(x, z);
                    inHighlandFringe = sampleWarpedHighlandNoise(wx, wz) > 0.04;
                }
                if (!inHighlandFringe && contNoise >= 0.14 && humNoise > 0.72) {
                    return scarletSwampBiome;
                }
                // Only true coastal/island rims (contNoise < 0.055) may form sandy Elder Beach;
                // inland low spots inside continents stay woodland instead of creating sandy
                // inland lakes.
                if (contNoise >= 0.055) {
                    if (!inHighlandFringe && humNoise > 0.46) {
                        if (contNoise >= 0.14) {
                            double wx = sampleWarpedX(x, z);
                            double wz = sampleWarpedZ(x, z);
                            if (computeEffectiveKarst(wx, wz, contNoise, tempNoise, humNoise) > 0.15) {
                                return scarletKarstBiome;
                            }
                        }
                        return scarletForestBiome;
                    }
                    if (!inHighlandFringe && tempNoise < VILE_TEMP_THRESHOLD) {
                        return vilePlainsBiome;
                    }
                    return elderwoodsBiome;
                }
            } else if (contNoise >= 0.055) {
                return resolveAridSurfaceBiome(x, z, contNoise, tempNoise, humNoise, surfaceY);
            }
            return elderBeachBiome;
        }

        double humNoise = sampleHumidityNoise(x, z);

        // 2. Warm / Arid Climate: Golden Savanna, Golden Savanna Mesa & Golden Desert (T > 0.09)
        if (tempNoise > 0.09) {
            return resolveAridSurfaceBiome(x, z, contNoise, tempNoise, humNoise, surfaceY);
        }

        // 3. Elder Highlands Continental System (strictly deep continental interior:
        // contNoise > 0.30, Cold/Temperate: T < -0.02, away from desert buffer and
        // Scarlet macro basins)
        // Use the exact same domain-warped coordinates (wx, wz) as computeSurfaceHeight
        // so
        // physical mountain flanks and surface biomes never drift out of alignment.
        double wx = sampleWarpedX(x, z);
        double wz = sampleWarpedZ(x, z);
        double highlandNoise = sampleWarpedHighlandNoise(wx, wz);
        double effectiveHighland = computeEffectiveHighland(wx, wz, contNoise, tempNoise, humNoise);

        if (effectiveHighland > 0.04) {
            double summitIntensity = computeSummitPeakIntensity(wx, wz, effectiveHighland);
            if (summitIntensity > 0.24 && surfaceY >= 152) {
                return highlandSummitBiome;
            } else if (effectiveHighland > 0.42) {
                return highlandForestBiome;
            } else {
                return highlandMeadowBiome;
            }
        }

        // Thin cold foothill separator ring when humidity is below core Scarlet range
        if (contNoise > 0.26 && tempNoise < -0.02 && highlandNoise > 0.04 && humNoise <= 0.46) {
            return elderwoodsBiome;
        }

        // 4. Lowland Woodland & Cold Vile Plains Biomes (Normalized macro Humidity & Temperature controlled;
        // keep Scarlet Swamp deeply inland)
        if (contNoise >= 0.14 && humNoise > 0.72) {
            return scarletSwampBiome;
        } else if (humNoise > 0.46) {
            if (contNoise >= 0.14 && computeEffectiveKarst(wx, wz, contNoise, tempNoise, humNoise) > 0.15) {
                return scarletKarstBiome;
            }
            return scarletForestBiome;
        } else if (tempNoise < VILE_TEMP_THRESHOLD && contNoise >= 0.055) {
            return vilePlainsBiome;
        } else {
            return elderwoodsBiome;
        }
    }

    private boolean isAridMorphCoreQuart(int qx, int qz, MorphCoreCache cache) {
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
        for (int i = 0; i < ARID_CORE_PROBES.length; i++) {
            if (!isAridBiome(computeRawSurfaceBiome(bx + ARID_CORE_PROBES[i][0], bz + ARID_CORE_PROBES[i][1]))) {
                isCore = false;
                break;
            }
        }
        cache.keys[slot] = key;
        cache.states[slot] = (byte) (isCore ? 2 : 1);
        return isCore;
    }

    private boolean hasAridMorphCoreNear(int x, int z, MorphCoreCache cache) {
        int baseQx = Math.floorDiv(x, MORPH_QUART_STEP);
        int baseQz = Math.floorDiv(z, MORPH_QUART_STEP);
        for (int i = 0; i < ARID_DILATE_OFFSETS.length; i++) {
            int qx = baseQx + ARID_DILATE_OFFSETS[i][0];
            int qz = baseQz + ARID_DILATE_OFFSETS[i][1];
            if (isAridMorphCoreQuart(qx, qz, cache)) {
                int cx = qx * MORPH_QUART_STEP;
                int cz = qz * MORPH_QUART_STEP;
                int dx = cx - x;
                int dz = cz - z;
                if (dx * dx + dz * dz <= 64
                        || isAridBiome(computeRawSurfaceBiome((x + cx) >> 1, (z + cz) >> 1))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolves the surrounding non-arid land biome when an isolated micro-blob of Golden Savanna /
     * Desert is removed by morphological opening, seamlessly matching Scarlet, Highland, Beach, or Elderwoods.
     */
    private Holder<Biome> computeNonAridFallbackBiome(int x, int z) {
        double contNoise = sampleContinentalness(x, z);
        int surfaceY = computeSurfaceHeight(x, z, null);
        double tempNoise = sampleTemperatureNoise(x, z);
        double humNoise = sampleHumidityNoise(x, z);

        if (surfaceY < 74 && contNoise < 0.055) {
            return elderBeachBiome;
        }
        if (contNoise >= 0.14 && humNoise > 0.72) {
            return scarletSwampBiome;
        }
        if (humNoise > 0.46) {
            if (contNoise >= 0.14) {
                double wx = sampleWarpedX(x, z);
                double wz = sampleWarpedZ(x, z);
                if (computeEffectiveKarst(wx, wz, contNoise, Math.min(tempNoise, 0.08), humNoise) > 0.15) {
                    return scarletKarstBiome;
                }
            }
            return scarletForestBiome;
        }
        return elderwoodsBiome;
    }

    private boolean isVileMorphCoreQuart(int qx, int qz, MorphCoreCache cache) {
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
        for (int i = 0; i < VILE_CORE_PROBES.length; i++) {
            if (computeRawSurfaceBiome(bx + VILE_CORE_PROBES[i][0], bz + VILE_CORE_PROBES[i][1]) != vilePlainsBiome) {
                isCore = false;
                break;
            }
        }
        cache.keys[slot] = key;
        cache.states[slot] = (byte) (isCore ? 2 : 1);
        return isCore;
    }

    private boolean hasVileMorphCoreNear(int x, int z, MorphCoreCache cache) {
        int baseQx = Math.floorDiv(x, MORPH_QUART_STEP);
        int baseQz = Math.floorDiv(z, MORPH_QUART_STEP);
        for (int i = 0; i < VILE_DILATE_OFFSETS.length; i++) {
            int qx = baseQx + VILE_DILATE_OFFSETS[i][0];
            int qz = baseQz + VILE_DILATE_OFFSETS[i][1];
            if (isVileMorphCoreQuart(qx, qz, cache)) {
                int cx = qx * MORPH_QUART_STEP;
                int cz = qz * MORPH_QUART_STEP;
                int dx = cx - x;
                int dz = cz - z;
                if (dx * dx + dz * dz <= 64
                        || computeRawSurfaceBiome((x + cx) >> 1, (z + cz) >> 1) == vilePlainsBiome) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesMorphTarget(Holder<Biome> biome, boolean swampOnly) {
        return swampOnly
                ? biome == scarletSwampBiome
                : (biome == scarletForestBiome || biome == scarletKarstBiome || biome == scarletSwampBiome);
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

    private Holder<Biome> resolveAridSurfaceBiome(
            int x, int z, double contNoise, double tempNoise, double humNoise, int surfaceY) {
        double wx = sampleWarpedX(x, z);
        double wz = sampleWarpedZ(x, z);
        double effectiveMesa = computeEffectiveMesa(wx, wz, contNoise, tempNoise);
        if (effectiveMesa >= 0.16 && surfaceY >= 94) {
            return goldenSavannaMesaBiome;
        }
        if (tempNoise <= 0.20 || humNoise > 0.02) {
            return goldenSavannaBiome;
        }
        return goldenDesertBiome;
    }

    private Holder<Biome> pickBorderingNonScarletBiome(int x, int z, int surfaceY) {
        int elderwoodsCount = 0;
        int beachCount = 0;
        int desertCount = 0;
        int savannaCount = 0;
        int mesaCount = 0;
        int meadowCount = 0;
        int forestCount = 0;
        int valleyCount = 0;
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
                if (b == scarletForestBiome || b == scarletKarstBiome || b == scarletSwampBiome
                        || b == silentOceansBiome || b == silentDepthsBiome || b == sunkenTrenchesBiome) {
                    continue;
                }
                if (b == elderwoodsBiome) {
                    elderwoodsCount++;
                } else if (b == elderValleyBiome) {
                    valleyCount++;
                } else if (b == elderBeachBiome) {
                    beachCount++;
                } else if (b == goldenDesertBiome) {
                    desertCount++;
                } else if (b == goldenSavannaBiome) {
                    savannaCount++;
                } else if (b == goldenSavannaMesaBiome) {
                    mesaCount++;
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
        if (valleyCount > elderwoodsCount && valleyCount > 0) {
            return elderValleyBiome;
        }
        if (elderwoodsCount > 0) {
            return elderwoodsBiome;
        }

        Holder<Biome> dominant = nearestLandBiome != null ? nearestLandBiome : elderwoodsBiome;
        int bestCount = 0;
        if (meadowCount > bestCount) {
            bestCount = meadowCount;
            dominant = highlandMeadowBiome;
        }
        if (forestCount > bestCount) {
            bestCount = forestCount;
            dominant = highlandForestBiome;
        }
        if (savannaCount > bestCount) {
            bestCount = savannaCount;
            dominant = goldenSavannaBiome;
        }
        if (mesaCount > bestCount) {
            bestCount = mesaCount;
            dominant = goldenSavannaMesaBiome;
        }
        if (desertCount > bestCount) {
            bestCount = desertCount;
            dominant = goldenDesertBiome;
        }
        if (beachCount > bestCount) {
            bestCount = beachCount;
            dominant = elderBeachBiome;
        }
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
        int preRiverH = computePreRiverSmoothedHeight(x, z);
        ElderValleyTerrainHelper.ValleyColumnInfo valley = getOrEvaluateValley(x, z, preRiverH);
        return valley.hasValley ? valley.surfaceY : preRiverH;
    }

    /**
     * Copies cached or computed Elder Valley hydrological info for the column at
     * {@code (x, z)}.
     */
    public void sampleValleyColumn(int x, int z, ElderValleyTerrainHelper.ValleyColumnInfo out) {
        ensureSeedSynced();
        out.copyFrom(getOrEvaluateValley(x, z, Integer.MIN_VALUE));
    }

    public boolean isInRiverChannel(int x, int z) {
        ensureSeedSynced();
        ElderValleyTerrainHelper.ValleyColumnInfo info = getOrEvaluateValley(x, z, Integer.MIN_VALUE);
        return info.hasValley && info.inWaterChannel;
    }

    private ElderValleyTerrainHelper.ValleyColumnInfo getOrEvaluateValley(int x, int z, int knownPreRiverBaseH) {
        ValleyCache cache = valleyColumnCache.get();
        if (cache.seed != syncedSeed) {
            Arrays.fill(cache.occupied, false);
            cache.seed = syncedSeed;
        }
        long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        int slot = ((x * 73856093) ^ (z * 19349663)) & VALLEY_CACHE_MASK;
        if (cache.occupied[slot] && cache.keys[slot] == key) {
            return cache.infos[slot];
        }

        int baseH = (knownPreRiverBaseH != Integer.MIN_VALUE)
                ? knownPreRiverBaseH
                : computePreRiverSmoothedHeight(x, z);
        double cont = sampleContinentalness(x, z);
        double temp = sampleTemperatureNoise(x, z);
        double hum = sampleHumidityNoise(x, z);
        ElderValleyTerrainHelper.ValleyColumnInfo scratch = valleyTerrainHelper.getScratchInfo();
        valleyTerrainHelper.evaluateColumn(syncedSeed, x, z, baseH, cont, temp, hum, valleyTerrainLookup, scratch);

        cache.keys[slot] = key;
        cache.infos[slot].copyFrom(scratch);
        cache.occupied[slot] = true;
        return cache.infos[slot];
    }

    private int computePreRiverSmoothedHeight(int x, int z) {
        int h = getRawSurfaceHeight(x, z);

        // Relax steep underwater coastal / island aprons using a C1 distance-faded
        // multi-tier cone.
        // Smooth radial and lower-depth attenuation guarantee zero circular edge steps
        // or cutoff cliffs.
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

        // Suppress isolated 1x1 sand block pimples and 1x1 water pinholes along flat
        // beach terraces
        // deterministically across chunk borders without affecting hills, cone ramps,
        // or ocean basins.
        if (!coneApplied && h >= ElderwoodsChunkGenerator.BASE_HEIGHT - 1
                && h <= ElderwoodsChunkGenerator.BASE_HEIGHT + 3) {
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
                // Exact branch-and-bound: even at shoreCap, this point cannot beat current
                // coneH
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

        // Blending weights centered on Biome thresholds; restrict Scarlet Swamp basin
        // depression inland so coastal Scarlet rims form dry Scarlet Forest shelves right
        // onto the beach instead of leaking water steps.
        double warmWeight = net.minecraft.util.Mth.clamp((tempNoise - 0.04) / 0.10, 0.0, 1.0);
        double desertBlend = net.minecraft.util.Mth.clamp((tempNoise - 0.16) / 0.08, 0.0, 1.0)
                * net.minecraft.util.Mth.clamp((0.08 - humNoise) / 0.12, 0.0, 1.0);
        double swampInland = hermite01((contNoise - 0.105) / 0.055);
        double swampWeight = net.minecraft.util.Mth.clamp((humNoise - 0.50) / 0.24, 0.0, 1.0) * swampInland;

        // Domain Warping for organic, non-symmetrical mountain and hill contours
        double wx = sampleWarpedX(x, z);
        double wz = sampleWarpedZ(x, z);

        // 1. Base Rolling Hills (Elderwoods / Scarlet Forest)
        double nx = (wx + seedOffsetX) * 0.0021;
        double nz = (wz + seedOffsetZ) * 0.0021;
        // Elevated baseline (range +1.0..+19.0) keeps inland forests naturally above sea level Y=72
        double baseNoise = 10.0 + Math.sin(nx) * Math.cos(nz) * 6.5;
        baseNoise += Math.sin(nx * 0.5 + 2.0) * Math.cos(nz * 0.6 + 1.1) * 2.5;

        // 2. Swamp Basin
        double bumpyNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + seedOffsetX) * 0.05), (float) ((z + seedOffsetZ) * 0.05), 2) * 3.0;
        double swampHeight = -3.0 + bumpyNoise;

        // Blend Swamp into Base Hills
        double terrainHeight = net.minecraft.util.Mth.lerp(swampWeight, baseNoise, swampHeight);

        // 2b. Scarlet Karst: Steep Tower Karst Spires & Doline Sinkholes
        double effectiveKarst = computeEffectiveKarst(wx, wz, contNoise, tempNoise, humNoise);
        if (effectiveKarst > 0.06) {
            double karstBlend = hermite01((effectiveKarst - 0.06) / 0.12);
            double tA = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + ridgeOffsetX + 911) * 0.014f),
                    (float) ((wz + ridgeOffsetZ - 613) * 0.014f), 2) / 0.30;
            double tB = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((x + seedOffsetX - 407) * 0.026f),
                    (float) ((z + seedOffsetZ + 829) * 0.026f), 2) / 0.30;
            double rK = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + ridgeOffsetX - 521) * 0.0085f),
                    (float) ((wz + ridgeOffsetZ + 373) * 0.0085f), 2)) / 0.30;
            double ridgeSpine = net.minecraft.util.Mth.clamp(rK, 0.0, 1.0);

            double spireA = hermite01((tA - 0.08) / 0.32);
            double spireB = hermite01((tB - 0.14) / 0.34);
            double towerStrength = hermite01((effectiveKarst - 0.10) / 0.22);
            double towerHeight = (spireA * 38.0 + spireB * 18.0 + (ridgeSpine * ridgeSpine) * 14.0) * towerStrength;

            double dolineT = hermite01((-0.22 - tA) / 0.30);
            double sinkholeDrop = dolineT * 13.0 * towerStrength;

            double karstTarget = terrainHeight + 6.0 + towerHeight - sinkholeDrop;
            terrainHeight = net.minecraft.util.Mth.lerp(karstBlend, terrainHeight, karstTarget);
        }

        // 3. Golden Savanna, Dunes & Stratified Mesa / Tablemountains
        double duneNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + seedOffsetX) * 0.03), (float) ((z + seedOffsetZ) * 0.03), 2) * 5.0;
        // Keep interior/coastal arid terrain safely above sea level so troughs never punch saltwater holes
        double savannaBaseHeight = Math.max((terrainHeight * 0.55) + 3.5 + duneNoise * 0.70, 2.6);
        double desertHeight = Math.max((terrainHeight * 0.45) + 2.8 + duneNoise * 0.85, 2.4);
        double aridBaseNoise = net.minecraft.util.Mth.lerp(desertBlend, savannaBaseHeight, desertHeight);

        // Blend Warm/Arid over base terrain
        double finalNoise = net.minecraft.util.Mth.lerp(warmWeight, terrainHeight, aridBaseNoise);

        double effectiveMesa = computeEffectiveMesa(wx, wz, contNoise, tempNoise);
        if (effectiveMesa > 0.08) {
            double talusRamp = hermite01((effectiveMesa - 0.08) / 0.07) * 10.0;
            double plateauFactor = computeTablemountainPlateauFactor(wx, wz, effectiveMesa);
            double rockCrag = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((x + seedOffsetX) * 0.022f), (float) ((z + seedOffsetZ) * 0.022f), 2) / 0.32;
            double innerBench = hermite01((effectiveMesa - 0.38) / 0.06) * 4.5;
            double plateauNoise = 70.0 + innerBench + rockCrag * 2.5;
            double dampedBase = net.minecraft.util.Mth.lerp(0.88 * plateauFactor, finalNoise + talusRamp, plateauNoise);
            finalNoise = dampedBase;
        }

        // 4. Elder Highlands Continental Mountain System (Topological concentric tiers:
        // Wide Meadow Y~84-110 -> Broad Forest Slopes Y~110-150 -> Compact Central Summit Peaks Y~152-220+)
        double effectiveHighland = computeEffectiveHighland(wx, wz, contNoise, tempNoise, humNoise);

        if (effectiveHighland > 0.01) {
            double smoothT = hermite01((effectiveHighland - 0.01) / 0.14);

            // A. Lower Level — Expansive Highland Meadow Belt (effectiveHighland 0.02..0.42, Y ~ 84..110)
            double meadowT = hermite01((effectiveHighland - 0.02) / 0.40);
            double baseLift = meadowT * 24.0;

            // B. Asymmetrical Alpine Slopes & Crag Detail
            double alpineSlope = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + seedOffsetX) * 0.0018f), (float) ((wz + seedOffsetZ) * 0.0018f), 3) * 22.0;
            double cragDetail = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) (x * 0.015f), (float) (z * 0.015f), 2) * 7.0;
            double microRoughness = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) (x * 0.04f), (float) (z * 0.04f), 2) * 3.0;

            // C. Mid Level — Broad Highland Forest Mountain Flanks (effectiveHighland 0.40..0.72, Y ~ 110..150)
            double forestT = hermite01((effectiveHighland - 0.40) / 0.32);
            double forestLift = forestT * 32.0;

            // D. Center Peak — Compact Isolated Summit Peaks (Y~152 to Y~215+)
            double r1 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + ridgeOffsetX) * 0.0011f), (float) ((wz + ridgeOffsetZ) * 0.0011f), 2)) / 0.30;
            double r2 = 1.0 - Math.abs(net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + ridgeOffsetX + 333) * 0.0022f), (float) ((wz + ridgeOffsetZ + 333) * 0.0022f), 2)) / 0.30;
            double ridgeCrest = net.minecraft.util.Mth.clamp(r1 * 0.72 + r2 * 0.28, 0.0, 1.0);

            double summitIntensity = computeSummitPeakIntensity(wx, wz, effectiveHighland);
            double summitNorm = Math.max(0.0, summitIntensity - 0.06) / 0.55;
            double mountainCone = (summitNorm * summitNorm) / (1.0 + 0.25 * summitNorm * summitNorm);
            double peakLift = mountainCone * (88.0 + 42.0 * ridgeCrest * ridgeCrest);

            double summitTransition = hermite01((summitIntensity - 0.04) / 0.22);
            double valleyClampedSlope = net.minecraft.util.Mth.lerp(summitTransition * 0.85, alpineSlope,
                    Math.max(alpineSlope, -2.0));
            double totalHighlandBonus = baseLift + valleyClampedSlope + cragDetail + microRoughness + forestLift
                    + peakLift;
            finalNoise = net.minecraft.util.Mth.lerp(smoothT, finalNoise, terrainHeight + totalHighlandBonus);
        }

        // 5. Continental Protection, Coastline Unification & Bathymetric Carving
        // Sample Island Noise with restrained domain-warped coordinates + multi-scale
        // coastline jaggedness
        // so islands form organic bays without folding contours into shear cliffs.
        double islandNoise = computeIslandNoiseValue(x, z, wx, wz);

        // A. Mainland Coastal Slope & Shoreline (tight natural beaches transitioning
        // crisply into water)
        double mainlandY = ElderwoodsChunkGenerator.BASE_HEIGHT + finalNoise;

        // Inland-to-Coast Smooth Taper (contNoise from 0.050 down to 0.012):
        double landCoastT = net.minecraft.util.Mth.clamp((contNoise - 0.012) / 0.038, 0.0, 1.0);
        double smoothLandCoast = landCoastT * landCoastT * (3.0 - 2.0 * landCoastT);
        double targetCoastY = ElderwoodsChunkGenerator.BASE_HEIGHT
                + net.minecraft.util.Mth.clamp(finalNoise * 0.45 + 1.5, 2.2, 4.2);
        double taperedMainlandY = net.minecraft.util.Mth.lerp(smoothLandCoast, targetCoastY, mainlandY);

        // Crisp Coastal Beach (contNoise from 0.012 down to -0.014):
        // Steps quickly from forest grade across thin sand beach down to submerged
        // shallow water (Y=66.5)
        double beachT = net.minecraft.util.Mth.clamp((0.012 - contNoise) / 0.026, 0.0, 1.0);
        double smoothBeach = beachT * beachT * (3.0 - 2.0 * beachT);
        double mainlandShoreY = net.minecraft.util.Mth.lerp(smoothBeach, taperedMainlandY,
                ElderwoodsChunkGenerator.BASE_HEIGHT - 5.5);

        // B. Ocean Bathymetry (Shallow photic shelf -> Continental slope -> Deep
        // abyssal plains)
        // oceanProgress: 0.0 at coast (contNoise = 0.0), 1.0 at deep abyss (contNoise =
        // -OCEAN_BATHYMETRY_SPAN)
        double oceanProgress = net.minecraft.util.Mth.clamp((-contNoise) / OCEAN_BATHYMETRY_SPAN, 0.0, 1.0);

        // Continental Shelf Dynamic Modulation:
        double dynamicShelfEdge = computeDynamicShelfEdge(x, z);
        double dynamicAbyssEdge = computeDynamicAbyssEdge(dynamicShelfEdge);

        double oceanY;
        if (oceanProgress < dynamicShelfEdge) {
            // Photic Continental Shelf (Silent Oceans): slopes gently from shallow water
            // (Y=58) to shelf floor (Y=42)
            double shelfFrac = oceanProgress / dynamicShelfEdge;
            double s = shelfFrac * shelfFrac * (3.0 - 2.0 * shelfFrac);
            oceanY = net.minecraft.util.Mth.lerp(s, ElderwoodsChunkGenerator.BASE_HEIGHT - 4.0, 42.0);
            // Rolling sandbanks and dunes across the expansive ocean floor (warped organic
            // FBM, zero periodic grids)
            double sandbankNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + islandOffsetX + 1103) * 0.011f), (float) ((wz + islandOffsetZ - 881) * 0.011f), 2);
            oceanY += (sandbankNoise / 0.32) * 2.2 * (1.0 - shelfFrac);
        } else if (oceanProgress < dynamicAbyssEdge) {
            // Continental Slope: gradual descent from photic shelf floor (Y=42) down to
            // abyssal floor (Y=-98)
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

            // Spiked mountains flank the rift valleys and rise in high amounts: up to 72
            // blocks above valley floor!
            double valleyWallBoost = net.minecraft.util.Mth.clamp(valleyAxis / 0.18, 0.25, 1.0);
            double spikedMountainLift = spikeFactor * 72.0 * valleyWallBoost;

            // Gentle floor undulation in deep valley troughs
            double valleyFloorCurve = (net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                    (float) ((wx + trenchOffsetX) * 0.0085f), (float) ((wz + trenchOffsetZ) * 0.0085f), 2) / 0.32)
                    * 3.8;

            // Combine into full abyssal valley topography
            double abyssalTerrainY = valleyBaseY + valleyFloorCurve + spikedMountainLift;
            oceanY = net.minecraft.util.Mth.lerp(smoothAbyssal, -98.0, abyssalTerrainY);

            // Seamounts in the open ocean (Underwater Mountain Ranges rising to photic
            // zone)
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

            // Sunken Trenches / Bedrock fault cracks: smooth Hermite drop down to bedrock
            // layer (-118)
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
        // Samples regional marine shelf noise so every island tapers gradually into
        // surrounding seabed with zero sheer cliffs.
        double rawIslandShelfMod = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + islandOffsetX + 7821) * 0.00065f), (float) ((z + islandOffsetZ + 7821) * 0.00065f), 2);
        double islandShelfMod = net.minecraft.util.Mth.clamp(rawIslandShelfMod / 0.32, -0.75, 1.0);

        double shelfNoiseStart = Math.min(-0.14, -0.34 - 0.24 * islandShelfMod);
        double apronNoiseStart = shelfNoiseStart
                - (0.34 + 0.12 * net.minecraft.util.Mth.clamp(islandShelfMod, -0.6, 1.0));

        double finalTerrainY = baseTerrainY;
        if (islandNoise > apronNoiseStart) {
            double islandTargetY;
            if (islandNoise > 0.50) {
                // Island Interior (Y=74+): wider, softer rolling hills rather than steep
                // pointed cones
                double t = net.minecraft.util.Mth.clamp((islandNoise - 0.50) / 0.42, 0.0, 1.0);
                double smoothT = t * t * (3.0 - 2.0 * t);
                double islandRoll = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                        (float) ((wx + seedOffsetX + 219) * 0.0068f), (float) ((wz + seedOffsetZ - 411) * 0.0068f), 3)
                        / 0.32;
                double organicRelief = Math.max(0.8, 3.2 + Math.max(finalNoise * 0.28, -0.8) + islandRoll * 2.1);
                islandTargetY = ElderwoodsChunkGenerator.BASE_HEIGHT + 2.0 + smoothT * organicRelief;
            } else if (islandNoise > 0.34) {
                // Beach Zone (Y=56 to 74) - broad sandy shore passing smoothly through sea
                // level (Y=72)
                double t = (islandNoise - 0.34) / 0.16;
                double smoothT = t * t * (3.0 - 2.0 * t);
                islandTargetY = net.minecraft.util.Mth.lerp(smoothT, 56.0, 74.0);
            } else if (islandNoise > shelfNoiseStart) {
                // Photic Silent Ocean Shelf (Y=40 to 56) - gentle marine shelf around the
                // island
                double shelfT = (islandNoise - shelfNoiseStart) / (0.34 - shelfNoiseStart);
                double smoothShelf = shelfT * shelfT * (3.0 - 2.0 * shelfT);
                double shelfRollNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                        (float) ((wx + islandOffsetX + 1423) * 0.013f), (float) ((wz + islandOffsetZ - 997) * 0.013f),
                        2) / 0.32;
                double shelfRoll = shelfRollNoise * 2.8 * (shelfT * (1.0 - shelfT));
                islandTargetY = net.minecraft.util.Mth.lerp(smoothShelf, 40.0, 56.0) + shelfRoll;
            } else {
                // Submarine apron / ramp (sloping smoothly from surrounding seafloor
                // baseTerrainY up to Y=40)
                double apronT = (islandNoise - apronNoiseStart) / (shelfNoiseStart - apronNoiseStart);
                double smoothApron = apronT * apronT * (3.0 - 2.0 * apronT);
                islandTargetY = net.minecraft.util.Mth.lerp(smoothApron, baseTerrainY, 40.0);
            }

            // Smooth fillet radius k fades cleanly across the outer submarine apron
            double k = 6.5 * net.minecraft.util.Mth.clamp((islandNoise - apronNoiseStart) / 0.14, 0.0, 1.0);
            finalTerrainY = smoothMax(baseTerrainY, islandTargetY, k);
        }

        // D2. Photic-Shelf Archipelagos, Island Arcs & Atoll Shoals (gated strictly off
        // coasts & abyssal slopes)
        finalTerrainY = ArchipelagoTerrainHelper.applyArchipelagoTerrain(
                x, z, contNoise, oceanProgress, dynamicShelfEdge, finalTerrainY);

        // E. Sheltered Coastal Lagoons (Elder Lagoon): strictly excluded near Scarlet
        // (hum < 0.24)
        // and Golden Desert (temp < 0.06), centered in sheltered nearshore basins so
        // outer sandy bars
        // and wide dry buffers always isolate lagoons from Scarlet Swamps & deep ocean
        // shelves.
        finalTerrainY = ElderCoastalHelper.applyLagoonCarving(
                x, z, wx, wz, contNoise, tempNoise, humNoise,
                islandOffsetX, islandOffsetZ, seedOffsetX, seedOffsetZ, finalTerrainY);

        // --- DYNAMIC SWAMP SHORELINE (SPILL PREVENTION) ---
        // Strictly applies only on deep inland mainland where Scarlet Swamp meets
        // adjacent biomes
        if (contNoise > 0.05) {
            double landFade = net.minecraft.util.Mth.clamp((contNoise - 0.05) / 0.08, 0.0, 1.0);
            double distToHumBorder = Math.abs(swampWeight - 0.5) * 2.0;
            double distToTempBorder = Math.abs(warmWeight - 0.5) * 2.0;

            double ridgeForce = 0.0;
            if (swampWeight > 0.5) {
                double closestEdge = Math.min(distToHumBorder, distToTempBorder);
                ridgeForce = Math.max(0.0, 1.0 - (closestEdge * closestEdge));
            } else {
                ridgeForce = Math.max(0.0, 1.0 - (distToHumBorder * distToHumBorder));
            }

            if (ridgeForce > 0.0) {
                double ridgeMinimum = ElderwoodsChunkGenerator.BASE_HEIGHT + 0.5;
                finalTerrainY = net.minecraft.util.Mth.lerp(ridgeForce * landFade, finalTerrainY,
                        Math.max(finalTerrainY, ridgeMinimum));
            }
        }

        return (int) Math.round(finalTerrainY);
    }

    private double computeLagoonBiomeWeight(int x, int z, double contNoise, double tempNoise, double humNoise,
            int surfaceY) {
        double wx = sampleWarpedX(x, z);
        double wz = sampleWarpedZ(x, z);
        return ElderCoastalHelper.computeLagoonBiomeWeight(
                x, z, wx, wz, contNoise, tempNoise, humNoise,
                islandOffsetX, islandOffsetZ, seedOffsetX, seedOffsetZ, surfaceY);
    }

    public double getLagoonFeatureFactor(int x, int z) {
        ensureSeedSynced();
        double humNoise = sampleHumidityNoise(x, z);
        if (humNoise >= 0.24) {
            return 0.0;
        }
        double tempNoise = sampleTemperatureNoise(x, z);
        if (tempNoise >= 0.06) {
            return 0.0;
        }
        double contNoise = sampleContinentalness(x, z);
        int surfaceY = computeSurfaceHeight(x, z, null);
        double weight = computeLagoonBiomeWeight(x, z, contNoise, tempNoise, humNoise, surfaceY);
        return ElderCoastalHelper.computeLagoonFeatureFactor(weight);
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

    private double computeIslandNoiseValue(int x, int z, double wx, double wz) {
        double islandWarpX = wx + net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + islandOffsetX + 2713) * 0.0018f), (float) ((z + islandOffsetZ - 1831) * 0.0018f), 2)
                * 68.0;
        double islandWarpZ = wz + net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + islandOffsetX - 3917) * 0.0018f), (float) ((z + islandOffsetZ + 2243) * 0.0018f), 2)
                * 68.0;

        double rawIsland = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((islandWarpX + islandOffsetX) * 0.00068f), (float) ((islandWarpZ + islandOffsetZ) * 0.00068f),
                3);
        double coastDetail = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((wx + islandOffsetX + 641) * 0.0032f), (float) ((wz + islandOffsetZ - 487) * 0.0032f), 2)
                * 0.14
                + net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                        (float) ((x + islandOffsetX) * 0.0070f), (float) ((z + islandOffsetZ) * 0.0070f), 1) * 0.04;
        double detailFade = net.minecraft.util.Mth.clamp(((rawIsland / 0.32) + 0.15) / 0.55, 0.25, 1.0);
        return (rawIsland / 0.32) + coastDetail * detailFade;
    }

    /**
     * Computes the dynamic continentalness threshold for the deep abyssal floor
     * (Silent Depths),
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
