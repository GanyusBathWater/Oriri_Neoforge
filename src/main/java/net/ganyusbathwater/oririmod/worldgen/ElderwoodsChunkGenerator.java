package net.ganyusbathwater.oririmod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.worldgen.carver.ScarletCaveEntranceCarver;
import net.ganyusbathwater.oririmod.worldgen.carver.ElysianAbyssCarver;

/**
 * Custom ChunkGenerator for the Elderwoods dimension.
 */
public class ElderwoodsChunkGenerator extends ChunkGenerator {

    public static final MapCodec<ElderwoodsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSourceReference))
            .apply(instance, ElderwoodsChunkGenerator::new));

    public static final int BASE_HEIGHT = 72;
    public static final double HILL_AMPLITUDE = 25.0;
    public static final int MIN_Y = -128;
    public static final int MAX_Y = 320;
    public static final int WORLD_HEIGHT = MAX_Y - MIN_Y;

    // ===== ELDERWOODS BLOCKS =====
    private static final BlockState GRASS_BLOCK = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // ===== SCARLET BIOME BLOCKS =====
    private static BlockState SCARLET_STONE;
    private static BlockState SCARLET_DRIPSTONE;
    private static BlockState SCARLET_DEEPSLATE;
    private static BlockState SCARLET_MOSS;
    private static BlockState BLOOD_SLUDGE;

    // ===== ELYSIAN ABYSS BLOCKS =====
    private static BlockState AETHER_LIQUID;
    private static BlockState BLOOD_WATER;

    // ===== CACHED VANILLA / MOD SURFACE & LITHOLOGY BLOCKS (perf) =====
    private static final BlockState WATER_STATE = Blocks.WATER.defaultBlockState();
    private static final BlockState SAND_STATE = Blocks.SAND.defaultBlockState();
    private static final BlockState SANDSTONE_STATE = Blocks.SANDSTONE.defaultBlockState();
    private static final BlockState GRAVEL_STATE = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState ANDESITE_STATE = Blocks.ANDESITE.defaultBlockState();
    private static final BlockState MUD_STATE = Blocks.MUD.defaultBlockState();
    private static final BlockState MUDDY_MANGROVE_ROOTS_STATE = Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState();
    private static final BlockState MOSS_BLOCK_STATE = Blocks.MOSS_BLOCK.defaultBlockState();
    private static final BlockState CLAY_STATE = Blocks.CLAY.defaultBlockState();
    private static final BlockState COARSE_DIRT_STATE = Blocks.COARSE_DIRT.defaultBlockState();
    private static final BlockState PODZOL_STATE = Blocks.PODZOL.defaultBlockState();
    private static final BlockState MOSSY_COBBLESTONE_STATE = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    private static final BlockState SNOW_BLOCK_STATE = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState POWDER_SNOW_STATE = Blocks.POWDER_SNOW.defaultBlockState();
    private static final BlockState MAGMA_STATE = Blocks.MAGMA_BLOCK.defaultBlockState();
    private static final BlockState TERRACOTTA_STATE = Blocks.TERRACOTTA.defaultBlockState();
    private static final BlockState ORANGE_TERRACOTTA_STATE = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
    private static final BlockState YELLOW_TERRACOTTA_STATE = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
    private static BlockState CALICHE;
    private static BlockState ABYSSAL_BASALT;
    private static BlockState ABYSS_SAND;
    private static BlockState SOL_SAND;
    private static BlockState SOL_SANDSTONE;
    private static BlockState AETHER_MAGMA;
    private static BlockState FLUORITE_CLUSTER_DOWN;

    private volatile net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator cachedCarvingDummyGen;
    private volatile Boolean cachedHasLiquidCarvers;

    private boolean hasLiquidCarvers() {
        Boolean cached = this.cachedHasLiquidCarvers;
        if (cached != null) {
            return cached;
        }
        boolean found = false;
        for (Holder<Biome> biome : this.biomeSourceReference.possibleBiomes()) {
            if (biome.value().getGenerationSettings().getCarvers(GenerationStep.Carving.LIQUID).iterator().hasNext()) {
                found = true;
                break;
            }
        }
        this.cachedHasLiquidCarvers = found;
        return found;
    }

    // Biome keys for scarlet biomes
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
    private static final ResourceKey<Biome> ELYSIAN_ABYSS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));
    private static final ResourceKey<Biome> GOLDEN_DESERT_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "golden_desert"));
    public static final ResourceKey<Biome> GOLDEN_SAVANNA_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "golden_savanna"));
    public static final ResourceKey<Biome> GOLDEN_SAVANNA_MESA_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "golden_savanna_mesa"));
    private static final ResourceKey<Biome> ELDER_HIGHLANDS_MEADOW_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highlands_meadow"));
    private static final ResourceKey<Biome> ELDER_HIGHLAND_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_forest"));
    public static final ResourceKey<Biome> ELDER_HIGHLAND_SUMMIT_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_summit"));
    public static final ResourceKey<Biome> ELDER_VALLEY_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_valley"));
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

    private double seedOffsetX = 0;
    private double seedOffsetZ = 0;
    private double seedOffsetCave = 0;
    private volatile boolean seedInitialized = false;
    private volatile long syncedSeed = Long.MIN_VALUE;

    // Cached Biome Holders for performance
    private Holder<Biome> cachedElderwoods;
    private Holder<Biome> cachedScarletSwamp;
    private Holder<Biome> cachedScarletForest;
    private Holder<Biome> cachedScarletCaves;
    private Holder<Biome> cachedCrystalCaves;
    private Holder<Biome> cachedElderwoodsCave;
    private Holder<Biome> cachedElysianAbyss;
    private Holder<Biome> cachedGoldenDesert;

    // Expose seed and noise offsets for mathematical carver calculations
    public static long lastSeed = 0;
    public static double currentSeedOffsetCave = 0;
    public static double currentHumOffsetX = 0;
    public static double currentHumOffsetZ = 0;
    public static double currentTempOffsetX = 0;
    public static double currentTempOffsetZ = 0;

    public static double getScarletHumidityNoise(int x, int z) {
        return net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + currentHumOffsetX) * ElderwoodsBiomeSource.SCARLET_HUMIDITY_NOISE_FREQUENCY),
                (float) ((z + currentHumOffsetZ) * ElderwoodsBiomeSource.SCARLET_HUMIDITY_NOISE_FREQUENCY), 2) / 0.32;
    }

    public static double getDesertTemperatureNoise(int x, int z) {
        return net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + currentTempOffsetX) * ElderwoodsBiomeSource.CLIMATE_NOISE_FREQUENCY),
                (float) ((z + currentTempOffsetZ) * ElderwoodsBiomeSource.CLIMATE_NOISE_FREQUENCY), 3);
    }

    private final BiomeSource biomeSourceReference;

    public ElderwoodsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        this.biomeSourceReference = biomeSource;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private synchronized void initSeed(long seed) {
        if (!seedInitialized || syncedSeed != seed) {
            if (this.biomeSourceReference instanceof ElderwoodsBiomeSource elderwoodsBiomeSource) {
                elderwoodsBiomeSource.syncSeed(seed);
                this.seedOffsetX = elderwoodsBiomeSource.getSeedOffsetX();
                this.seedOffsetZ = elderwoodsBiomeSource.getSeedOffsetZ();
                this.seedOffsetCave = elderwoodsBiomeSource.getSeedOffsetCave();
                currentSeedOffsetCave = this.seedOffsetCave;
                currentHumOffsetX = elderwoodsBiomeSource.getHumOffsetX();
                currentHumOffsetZ = elderwoodsBiomeSource.getHumOffsetZ();
                currentTempOffsetX = elderwoodsBiomeSource.getTempOffsetX();
                currentTempOffsetZ = elderwoodsBiomeSource.getTempOffsetZ();
            } else {
                RandomSource random = RandomSource.create(seed);
                seedOffsetX = random.nextDouble() * 10000.0;
                seedOffsetZ = random.nextDouble() * 10000.0;
                seedOffsetCave = random.nextDouble() * 10000.0;
                currentSeedOffsetCave = seedOffsetCave;
            }

            // Initialize cached block states BEFORE publishing seedInitialized = true
            if (SCARLET_STONE == null) {
                SCARLET_STONE = ModBlocks.SCARLET_STONE.get().defaultBlockState();
                SCARLET_DRIPSTONE = ModBlocks.SCARLET_DRIPSTONE_BLOCK.get().defaultBlockState();
                SCARLET_DEEPSLATE = ModBlocks.SCARLET_DEEPSLATE.get().defaultBlockState();
                SCARLET_MOSS = ModBlocks.SCARLET_MOSS.get().defaultBlockState();
                BLOOD_SLUDGE = ModBlocks.BLOOD_SLUDGE.get().defaultBlockState();
                CALICHE = ModBlocks.CALICHE.get().defaultBlockState();
                ABYSSAL_BASALT = ModBlocks.ABYSSAL_BASALT.get().defaultBlockState();
                ABYSS_SAND = ModBlocks.ABYSS_SAND.get().defaultBlockState();
                SOL_SAND = ModBlocks.SOL_SAND.get().defaultBlockState();
                SOL_SANDSTONE = ModBlocks.SOL_SANDSTONE.get().defaultBlockState();
                AETHER_MAGMA = ModBlocks.AETHER_MAGMA_BLOCK.get().defaultBlockState();
                FLUORITE_CLUSTER_DOWN = ModBlocks.FLUORITE_CLUSTER.get().defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.DOWN);
            }
            if (AETHER_LIQUID == null) {
                AETHER_LIQUID = net.ganyusbathwater.oririmod.fluid.ModFluids.AETHER_BLOCK.get().defaultBlockState();
            }
            if (BLOOD_WATER == null) {
                BLOOD_WATER = net.ganyusbathwater.oririmod.fluid.ModFluids.BLOOD_WATER_BLOCK.get().defaultBlockState();
            }

            syncedSeed = seed;
            seedInitialized = true;
        }
    }

    private void initSeedFromRandomState(RandomState randomState) {
        long targetSeed = OririMod.globalWorldSeed;
        if (!seedInitialized || syncedSeed != targetSeed) {
            initSeed(targetSeed);
        }
    }

    private boolean isScarletBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(SCARLET_SWAMP_KEY) ||
                biomeHolder.is(SCARLET_FOREST_KEY) ||
                biomeHolder.is(SCARLET_KARST_KEY) ||
                biomeHolder.is(SCARLET_CAVES_KEY);
    }

    private boolean isScarletKarstBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(SCARLET_KARST_KEY);
    }

    private boolean isHighlandBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(ELDER_HIGHLANDS_MEADOW_KEY) ||
                biomeHolder.is(ELDER_HIGHLAND_FOREST_KEY) ||
                biomeHolder.is(ELDER_HIGHLAND_SUMMIT_KEY);
    }

    private boolean isElysianAbyssBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(ELYSIAN_ABYSS_KEY);
    }

    private boolean isGoldenDesertBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(GOLDEN_DESERT_KEY);
    }

    private boolean isGoldenSavannaBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(GOLDEN_SAVANNA_KEY);
    }

    private boolean isGoldenMesaBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(GOLDEN_SAVANNA_MESA_KEY);
    }

    private BlockState selectMesaStrataBlock(int worldX, int y, int worldZ) {
        double warp = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) (worldX * 0.012f), (float) (worldZ * 0.012f), 2) * 3.5;
        int band = Math.floorMod((int) Math.floor((y + warp) * 0.45), 11);
        return switch (band) {
            case 3 -> SOL_SANDSTONE != null ? SOL_SANDSTONE : CALICHE;
            case 4 -> YELLOW_TERRACOTTA_STATE;
            case 7 -> SOL_SANDSTONE != null ? SOL_SANDSTONE : CALICHE;
            case 8 -> ORANGE_TERRACOTTA_STATE;
            case 10 -> TERRACOTTA_STATE;
            default -> CALICHE != null ? CALICHE : STONE;
        };
    }

    private int getSurfaceHeight(int x, int z) {
        if (this.biomeSourceReference instanceof ElderwoodsBiomeSource ebs) {
            Holder<Biome> surfaceBiome = ebs.getSurfaceBiome(x, z);
            return ebs.computeSurfaceHeight(x, z, surfaceBiome);
        }
        return BASE_HEIGHT;
    }

    private double getAbyssNoise3D(int x, int y, int z) {
        double nx = (x + seedOffsetX) * 0.02; // Reduced frequency for smoothness
        double ny = y * 0.12;
        double nz = (z + seedOffsetZ) * 0.02;

        // 3D Irrational Noise for micro-surface roughness
        double noise = 0;
        // Octave 1 (Rotated 31 deg)
        double nx1 = nx * 0.857 + nz * 0.515;
        double nz1 = -nx * 0.515 + nz * 0.857;
        noise += Math.sin(nx1) * Math.cos(nz1) * Math.sin(ny);

        return noise / 1.5;
    }

    private int getCaveFloorHeight(int x, int z, int baseFloor) {
        return (int) Math.round(getCaveFloorHeightDouble(x, z, baseFloor));
    }

    public static double getCaveFloorHeightDouble(int x, int z, int baseFloor) {
        double scale = 0.008;
        double noise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) (x + currentSeedOffsetCave) * (float) scale,
                (float) (z + currentSeedOffsetCave) * (float) scale,
                3);
        return (double) baseFloor + (noise * 25.0) + 5.0;
    }

    private int getCaveCeilingHeight(int x, int z, int surfaceY) {
        return (int) Math.round(getCaveCeilingHeightDouble(x, z, surfaceY));
    }

    private double getCaveCeilingHeightDouble(int x, int z, int surfaceY) {
        double nx = (x + seedOffsetCave + 700) * 0.012;
        double nz = (z + seedOffsetCave + 700) * 0.012;

        // Multi-octave IRRATIONAL noise (breaks grid)
        double ceilNoise = 0;
        // Octave 1 (Rotated 15 deg)
        double nx1 = nx * 0.966 + nz * 0.259;
        double nz1 = -nx * 0.259 + nz * 0.966;
        ceilNoise += Math.sin(nx1) * Math.cos(nz1);
        // Octave 2 (Rotated 31 deg)
        double nx2 = nx * 0.857 + nz * 0.515;
        double nz2 = -nx * 0.515 + nz * 0.857;
        ceilNoise += Math.sin(nx2 * 1.93) * Math.cos(nz2 * 1.87) * 0.5;
        // Octave 3 (67 deg)
        double nx3 = nx * 0.390 + nz * 0.921;
        double nz3 = -nx * 0.921 + nz * 0.390;
        ceilNoise += Math.sin(nx3 * 3.31) * Math.cos(nz3 * 3.19) * 0.25;

        double baseCeiling = 60.0;
        double ceilVariation = ceilNoise * 18.0; // Subtle vault variation

        // Smooth coastal transition based on continuous surface height (no hard
        // discontinuity across beaches)
        double coastalSeal = Mth.clamp((73.0 - (double) surfaceY) / 16.0, 0.0, 1.0);
        double maxCeiling = (double) surfaceY - (15.0 + 5.0 * coastalSeal);
        if (maxCeiling <= 20.0) {
            return maxCeiling;
        }
        return Math.min(Math.max(baseCeiling + ceilVariation, 20.0), maxCeiling);
    }

    private double getAbyssNoise(int x, int z) {
        // Synchronized 1:1 with ElderwoodsBiomeSource.sampleCaveNoise &
        // ElysianAbyssCarver
        float scale = ElderwoodsBiomeSource.CAVE_NOISE_FREQUENCY;
        return net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((x + seedOffsetCave) * scale),
                (float) ((z + seedOffsetCave) * scale),
                3);
    }

    private double getPillarNoise(int x, int z) {
        double nx = (x + seedOffsetCave + 555) * 0.04;
        double nz = (z + seedOffsetCave + 555) * 0.04;
        return (Math.sin(nx) * Math.cos(nz) + 1.0) / 2.0;
    }

    /**
     * 3D Noise Settings (Density Function) for the Giant Cave Cavern.
     */
    private double getElysianCavernDensity(int x, int y, int z, double caveFloor, double caveCeiling,
            double abyssIntensity) {
        // Target vertical bounds
        double roomHalfHeight = Math.max(1.0, (caveCeiling - caveFloor) / 2.0);
        double verticalCenter = (caveFloor + caveCeiling) / 2.0;

        double yDist = Math.abs(y - verticalCenter);
        double normalizedY = yDist / roomHalfHeight;

        // Base hollow shape + wall gradient
        double baseDensity = (normalizedY * normalizedY * 3.0) - 1.5;
        double wallGradient = (1.0 - abyssIntensity) * 2.5;
        double deterministicCore = baseDensity + wallGradient;

        // Since |fbm3D| <= 1.0, |noise| <= 2.5; when core > 2.65, final density is
        // strictly > 0.15
        // and above the threshold, allowing an exact skip of 3D noise outside cavern
        // strata.
        if (deterministicCore > 2.65) {
            return deterministicCore;
        }

        double scale = 0.012;
        double noise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                (float) (x + seedOffsetCave) * (float) scale,
                (float) y * (float) scale,
                (float) (z + seedOffsetCave) * (float) scale,
                2) * 2.5;

        return -noise + deterministicCore;
    }

    public static double getRiverIntensity(int x, int z) {
        // Main winding river
        double riverNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) (x + currentSeedOffsetCave + 1000) * 0.005f,
                (float) (z + currentSeedOffsetCave + 1000) * 0.005f, 2);
        double mainRiver = Math.max(0.0, 0.04 - Math.abs(riverNoise));

        // Tributary branches: 3 octaves adds high-frequency organic wiggles to break up
        // straight grid lines
        double branchNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) (x + currentSeedOffsetCave + 2000) * 0.006f,
                (float) (z + currentSeedOffsetCave + 2000) * 0.006f, 3);
        double branchRiver = Math.max(0.0, 0.03 - Math.abs(branchNoise));

        double riverIntensity = (mainRiver + branchRiver) * 30.0;
        return Math.min(1.0, riverIntensity);
    }

    private int[] findNearestEntranceCenter(int x, int z) {
        int gridSize = 180;
        int gridX = Math.floorDiv(x, gridSize);
        int gridZ = Math.floorDiv(z, gridSize);

        int nearestX = 0;
        int nearestZ = 0;
        double nearestDist = Double.MAX_VALUE;

        for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
            for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
                double jitterSeed = gx * 31.0 + gz * 17.0 + seedOffsetCave;
                double jitterX = Math.sin(jitterSeed) * gridSize * 0.4;
                double jitterZ = Math.cos(jitterSeed * 1.3) * gridSize * 0.4;

                int entranceX = gx * gridSize + gridSize / 2 + (int) jitterX;
                int entranceZ = gz * gridSize + gridSize / 2 + (int) jitterZ;

                double dist = Math.sqrt((x - entranceX) * (x - entranceX) + (z - entranceZ) * (z - entranceZ));
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestX = entranceX;
                    nearestZ = entranceZ;
                }
            }
        }

        return new int[] { nearestX, nearestZ, (int) nearestDist };
    }

    private boolean isEntrance(int x, int y, int z, int surfaceY) {
        int[] nearest = findNearestEntranceCenter(x, z);
        int entranceCenterX = nearest[0];
        int entranceCenterZ = nearest[1];

        int maxRadius = 8;
        int minRadius = 4;
        int entranceTop = surfaceY;

        double depthVariation = Math.sin(entranceCenterX * 0.1 + entranceCenterZ * 0.1 + seedOffsetCave);
        int entranceBottom = (int) (depthVariation * 16);

        if (y < entranceBottom || y > entranceTop)
            return false;

        double depthProgress = (double) (entranceTop - y) / (entranceTop - entranceBottom);
        double windingX = Math.sin(depthProgress * 4.0 + entranceCenterX * 0.1) * 6.0;
        double windingZ = Math.cos(depthProgress * 3.5 + entranceCenterZ * 0.1) * 6.0;

        double offsetX = x - entranceCenterX - windingX;
        double offsetZ = z - entranceCenterZ - windingZ;
        double distFromWindingCenter = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);

        double radiusAtY = Mth.lerp(depthProgress, maxRadius, minRadius);

        double wobble = Math.sin(x * 0.2 + y * 0.1 + seedOffsetCave) *
                Math.cos(z * 0.2 + y * 0.1 + seedOffsetCave) * 1.5;

        double extraNoise = Mth.sin((float) (x * 0.05 + y * 0.1)) * Mth.cos((float) (z * 0.05 + y * 0.1)) * 1.5;

        radiusAtY += wobble + extraNoise;

        return distFromWindingCenter < radiusAtY;
    }

    private boolean isCaveLegacy(int x, int y, int z, int surfaceY) {
        if (y <= MIN_Y + 5)
            return false;

        int caveFloor = getCaveFloorHeight(x, z, -115);
        int caveCeiling = getCaveCeilingHeight(x, z, surfaceY);

        if (y < caveFloor || y > caveCeiling)
            return false;
        if (y > surfaceY - 12)
            return false;

        double nx = x + seedOffsetCave;
        double ny = y;
        double nz = z + seedOffsetCave;

        double cheese = 0.0;
        cheese += Math.sin(nx * 0.02 + 17) * Math.sin(ny * 0.025 + 31) * Math.sin(nz * 0.018 + 43);
        cheese += Math.cos(nx * 0.015 + 67) * Math.cos(ny * 0.02 + 89) * Math.cos(nz * 0.012 + 101) * 0.6;
        cheese += Math.sin(nx * 0.035 + 127) * Math.cos(ny * 0.03 + 151) * Math.sin(nz * 0.025 + 173) * 0.35;

        double depthBonus = Math.max(0, (40.0 - y) / 80.0) * 0.15;
        return cheese > 0.5 - depthBonus;
    }

    private static final int CRYSTAL_CAVE_Y_THRESHOLD = -16;
    private static final double CRYSTAL_ZONE_THRESHOLD = 0.82;

    private double getCrystalZoneNoise(int x, int y, int z) {
        if (y > CRYSTAL_CAVE_Y_THRESHOLD)
            return -1;

        double nx = (x + seedOffsetCave) * 0.035;
        double ny = y * 0.045;
        double nz = (z + seedOffsetCave) * 0.035;

        double noise = Mth.sin((float) (nx * 1.5)) * Mth.cos((float) (nz * 1.5));
        noise += Mth.sin((float) (ny * 2.0 + nx)) * 0.5;
        noise += Mth.cos((float) (nx * 3.0 + nz * 2.5)) * 0.3;

        return noise;
    }

    private boolean isCrystalZone(int x, int y, int z) {
        return getCrystalZoneNoise(x, y, z) > CRYSTAL_ZONE_THRESHOLD;
    }

    private int getGeodeLayer(int x, int y, int z) {
        double noise = getCrystalZoneNoise(x, y, z);
        if (noise <= CRYSTAL_ZONE_THRESHOLD - 0.12)
            return 0;
        if (noise <= CRYSTAL_ZONE_THRESHOLD - 0.06)
            return 1;
        if (noise <= CRYSTAL_ZONE_THRESHOLD)
            return 2;
        return 3;
    }

    private int getCrystalType(int x, int y, int z) {
        double noise = Mth.sin((float) ((x + seedOffsetX) * 0.1)) * Mth.cos((float) ((z + seedOffsetZ) * 0.1));
        noise += Mth.sin((float) (y * 0.15)) * 0.5;
        return noise > 0 ? 1 : 0;
    }

    private boolean shouldPlaceCluster(int x, int y, int z, long seed) {
        long hash = ((x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L) ^ seed);
        hash = (hash * 31 + 17) ^ (hash >> 16);
        hash = hash & 0xFFFFFFFFL;
        return (hash % 100) < 4;
    }

    private ElderwoodsCaveAquifer createCaveAquifer(long seed, net.minecraft.world.level.ChunkPos chunkPos) {
        ElderwoodsCaveAquifer.Int3Predicate isScarletCaveFn = (this.biomeSourceReference instanceof ElderwoodsBiomeSource ebs)
                ? ebs::isScarletCaveAt
                : (x, y, z) -> false;
        return new ElderwoodsCaveAquifer(
                seed,
                chunkPos,
                this::getSurfaceHeight,
                this::getAbyssNoise,
                isScarletCaveFn,
                MIN_Y,
                WORLD_HEIGHT);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState, BiomeManager biomeManager,
            StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {

        lastSeed = seed;
        OririMod.globalWorldSeed = seed;
        initSeed(seed);

        if (step == GenerationStep.Carving.AIR || (step == GenerationStep.Carving.LIQUID && hasLiquidCarvers())) {
            final ElderwoodsCaveAquifer caveAquifer = createCaveAquifer(seed, chunk.getPos());
            ElysianAbyssCarver.CURRENT_AQUIFER.set(caveAquifer);
            try {
                net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator dummyGen = this.cachedCarvingDummyGen;
                if (dummyGen == null) {
                    dummyGen = new net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator(
                            this.biomeSource,
                            level.registryAccess()
                                    .registryOrThrow(net.minecraft.core.registries.Registries.NOISE_SETTINGS)
                                    .getHolderOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.CAVES));
                    this.cachedCarvingDummyGen = dummyGen;
                }
                net.minecraft.world.level.levelgen.carver.CarvingContext context = new net.minecraft.world.level.levelgen.carver.CarvingContext(
                        dummyGen, level.registryAccess(), chunk.getHeightAccessorForGeneration(), null, randomState,
                        null);
                net.minecraft.world.level.chunk.CarvingMask carvingMask = null;
                if (chunk instanceof net.minecraft.world.level.chunk.ProtoChunk protoChunk) {
                    carvingMask = protoChunk.getOrCreateCarvingMask(step);
                }

                final int chunkMinX = chunk.getPos().getMinBlockX();
                final int chunkMaxX = chunk.getPos().getMaxBlockX();
                final int chunkMinZ = chunk.getPos().getMinBlockZ();
                final int chunkMaxZ = chunk.getPos().getMaxBlockZ();
                final int chunkMinY = chunk.getMinBuildHeight();
                final int chunkMaxY = chunk.getMaxBuildHeight();
                final ElderwoodsBiomeSource ebs = (biomeSourceReference instanceof ElderwoodsBiomeSource src) ? src
                        : null;

                final int[] colSurfaceHeight = new int[256];
                Arrays.fill(colSurfaceHeight, Integer.MIN_VALUE);
                final byte[] colBiomeFlags = new byte[256]; // bit 0: known, bit 1: silent ocean/trench, bit 2: beach,
                                                            // bit 3: protected river
                final int[] colRiverMinSafeY = new int[256];
                Arrays.fill(colRiverMinSafeY, Integer.MIN_VALUE);
                final double[] colAbyssNoise = new double[256];
                Arrays.fill(colAbyssNoise, Double.NaN);
                final double[] colRiverIntensity = new double[256];
                Arrays.fill(colRiverIntensity, Double.NaN);
                final ElderValleyTerrainHelper.ValleyColumnInfo carverValleyScratch = new ElderValleyTerrainHelper.ValleyColumnInfo();

                final BlockPos.MutableBlockPos carverPos = new BlockPos.MutableBlockPos();
                final net.minecraft.world.level.levelgen.Aquifer aquifer = new net.minecraft.world.level.levelgen.Aquifer() {
                    private boolean scheduleUpdate;

                    @Override
                    public BlockState computeSubstance(
                            net.minecraft.world.level.levelgen.DensityFunction.FunctionContext ctx, double substance) {
                        if (substance > 0.0) {
                            this.scheduleUpdate = false;
                            return null;
                        }
                        int bx = ctx.blockX();
                        int by = ctx.blockY();
                        int bz = ctx.blockZ();
                        boolean inChunk = chunkMinX <= bx && bx <= chunkMaxX && chunkMinZ <= bz && bz <= chunkMaxZ;

                        // Never overwrite pre-generated cave water/blood-water/lava lakes or breach an
                        // adjacent lake wall
                        if (inChunk && by >= chunkMinY && by < chunkMaxY) {
                            carverPos.set(bx, by, bz);
                            if (!chunk.getFluidState(carverPos).isEmpty()) {
                                this.scheduleUpdate = false;
                                return null;
                            }
                        }

                        int colIdx = inChunk ? (((bx - chunkMinX) << 4) | (bz - chunkMinZ)) : -1;

                        int surfaceHeight;
                        if (colIdx >= 0) {
                            surfaceHeight = colSurfaceHeight[colIdx];
                            if (surfaceHeight == Integer.MIN_VALUE) {
                                surfaceHeight = getSurfaceHeight(bx, bz);
                                colSurfaceHeight[colIdx] = surfaceHeight;
                            }
                        } else {
                            surfaceHeight = getSurfaceHeight(bx, bz);
                        }

                        boolean isOceanOrTrench;
                        boolean isBeach;
                        boolean isRiverProtected;
                        int riverProtectMinY = Integer.MIN_VALUE;
                        if (colIdx >= 0 && ebs != null) {
                            byte flags = colBiomeFlags[colIdx];
                            if (flags == 0) {
                                Holder<Biome> b = ebs.getSurfaceBiome(bx, bz);
                                int encoded = 1;
                                if (b.is(THE_SILENT_OCEANS_KEY) || b.is(THE_SILENT_DEPTHS_KEY)
                                        || b.is(THE_SUNKEN_TRENCHES_KEY)) {
                                    encoded |= 2;
                                }
                                if (b.is(ELDER_BEACH_KEY) || b.is(ELDER_LAGOON_KEY)) {
                                    encoded |= 4;
                                }
                                ebs.sampleValleyColumn(bx, bz, carverValleyScratch);
                                if (carverValleyScratch.hasValley && carverValleyScratch.isProtectedRiverbed) {
                                    encoded |= 8;
                                    colRiverMinSafeY[colIdx] = carverValleyScratch.bedY - 12;
                                }
                                flags = (byte) encoded;
                                colBiomeFlags[colIdx] = flags;
                            }
                            isOceanOrTrench = (flags & 2) != 0;
                            isBeach = (flags & 4) != 0;
                            isRiverProtected = (flags & 8) != 0;
                            riverProtectMinY = colRiverMinSafeY[colIdx];
                        } else {
                            Holder<Biome> b = ebs != null
                                    ? ebs.getSurfaceBiome(bx, bz)
                                    : biomeSourceReference.getNoiseBiome(bx / 4, by / 4, bz / 4, randomState.sampler());
                            isOceanOrTrench = b.is(THE_SILENT_OCEANS_KEY) || b.is(THE_SILENT_DEPTHS_KEY)
                                    || b.is(THE_SUNKEN_TRENCHES_KEY);
                            isBeach = b.is(ELDER_BEACH_KEY) || b.is(ELDER_LAGOON_KEY);
                            isRiverProtected = false;
                            if (ebs != null) {
                                ebs.sampleValleyColumn(bx, bz, carverValleyScratch);
                                if (carverValleyScratch.hasValley && carverValleyScratch.isProtectedRiverbed) {
                                    isRiverProtected = true;
                                    riverProtectMinY = carverValleyScratch.bedY - 12;
                                }
                            }
                        }

                        if (isOceanOrTrench) {
                            this.scheduleUpdate = false;
                            return null; // Preserve water-filled Silent Depths caves and solid Silent Oceans basalt
                        }

                        // Shield Elder Valley riverbeds from subterranean cave carvers so stepped pools
                        // never drain
                        if (isRiverProtected && by >= riverProtectMinY) {
                            this.scheduleUpdate = false;
                            return null;
                        }

                        if (by <= BASE_HEIGHT && by > surfaceHeight && isBeach) {
                            this.scheduleUpdate = false;
                            return WATER_STATE;
                        }
                        // Never breach the protective shallow coastal floor under submerged beach
                        // shoals
                        if (isBeach && surfaceHeight < BASE_HEIGHT - 1 && by >= surfaceHeight - 15) {
                            this.scheduleUpdate = false;
                            return null;
                        }
                        // Protect both local and cross-chunk aquifer basin dams from vanilla
                        // WorldCarver breaches
                        if (caveAquifer.hasAdjacentOrPredictedFluid(chunk, bx, by, bz, 2, 1)) {
                            this.scheduleUpdate = false;
                            return null;
                        }
                        // Leave surface cave mouths and Scarlet Cave entrances unobstructed air
                        if (by > surfaceHeight - 10) {
                            this.scheduleUpdate = false;
                            return Blocks.AIR.defaultBlockState();
                        }

                        // Keep Elysian Abyss cavern & lower Aether river zone unaffected by cave
                        // aquifers
                        double abyssNoise;
                        if (colIdx >= 0) {
                            abyssNoise = colAbyssNoise[colIdx];
                            if (Double.isNaN(abyssNoise)) {
                                abyssNoise = getAbyssNoise(bx, bz);
                                colAbyssNoise[colIdx] = abyssNoise;
                            }
                        } else {
                            abyssNoise = getAbyssNoise(bx, bz);
                        }
                        if (abyssNoise > 0.14) {
                            this.scheduleUpdate = false;
                            return Blocks.AIR.defaultBlockState();
                        }
                        if (by <= -92) {
                            double riverIntensity;
                            if (colIdx >= 0) {
                                riverIntensity = colRiverIntensity[colIdx];
                                if (Double.isNaN(riverIntensity)) {
                                    riverIntensity = getRiverIntensity(bx, bz);
                                    colRiverIntensity[colIdx] = riverIntensity;
                                }
                            } else {
                                riverIntensity = getRiverIntensity(bx, bz);
                            }
                            if (riverIntensity > 0.0) {
                                this.scheduleUpdate = false;
                                return Blocks.AIR.defaultBlockState();
                            }
                        }

                        // Carvers run after fillFromNoise and must never materialize un-updated
                        // suspended fluid volumes in rock
                        BlockState aquiferState = caveAquifer.computeSubstance(ctx, substance);
                        if (aquiferState == null || !aquiferState.getFluidState().isEmpty()) {
                            this.scheduleUpdate = false;
                            return null;
                        }
                        this.scheduleUpdate = false;
                        return aquiferState;
                    }

                    @Override
                    public boolean shouldScheduleFluidUpdate() {
                        return this.scheduleUpdate;
                    }
                };

                // Bind BiomeManager to biomeSource directly instead of ServerLevel so
                // background carver workers
                // never trigger synchronous main-thread chunk loads via
                // ServerChunkCache.getChunk().
                final BiomeManager carverBiomeManager = biomeManager.withDifferentSource(
                        (qx, qy, qz) -> this.biomeSource.getNoiseBiome(qx, qy, qz, randomState.sampler()));

                // 1. Vanilla Carvers (Properly loaded per origin chunk to allow biome crossing)
                int vanillaRange = 8;
                for (int rx = -vanillaRange; rx <= vanillaRange; rx++) {
                    for (int rz = -vanillaRange; rz <= vanillaRange; rz++) {
                        net.minecraft.world.level.ChunkPos originPos = new net.minecraft.world.level.ChunkPos(
                                chunk.getPos().x + rx, chunk.getPos().z + rz);
                        net.minecraft.core.BlockPos originCenter = originPos.getMiddleBlockPosition(0);

                        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> originBiome = this.biomeSource
                                .getNoiseBiome(
                                        net.minecraft.core.QuartPos.fromBlock(originCenter.getX()),
                                        0,
                                        net.minecraft.core.QuartPos.fromBlock(originCenter.getZ()),
                                        randomState.sampler());
                        net.minecraft.world.level.biome.BiomeGenerationSettings originSettings = originBiome.value()
                                .getGenerationSettings();

                        for (net.minecraft.core.Holder<net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?>> holder : originSettings
                                .getCarvers(step)) {
                            net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?> carver = holder.value();
                            // Run normal biome carvers, excluding the global Abyss Carver
                            if (!(carver.worldCarver() instanceof ElysianAbyssCarver)) {
                                if (carver.worldCarver() instanceof ScarletCaveEntranceCarver) {
                                    if (!originBiome.is(SCARLET_SWAMP_KEY) && !originBiome.is(SCARLET_FOREST_KEY)) {
                                        continue;
                                    }
                                }
                                try {
                                    net.minecraft.util.RandomSource carverRandom = net.minecraft.util.RandomSource
                                            .create(seed ^ originPos.toLong());
                                    if (carver.isStartChunk(carverRandom)) {
                                        carver.carve(context, chunk, carverBiomeManager::getBiome, carverRandom,
                                                aquifer, originPos, carvingMask);
                                    }
                                } catch (Exception ex) {
                                    OririMod.LOGGER.error("Error running vanilla carver", ex);
                                }
                            }
                        }
                    }
                }

                // 2. Run ElysianAbyssCarver globally on AIR step so ravines cross biome
                // boundaries once cleanly
                if (step == GenerationStep.Carving.AIR) {
                    net.minecraft.core.Registry<net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?>> carverRegistry = level
                            .registryAccess()
                            .registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_CARVER);
                    for (net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?> globalCarver : carverRegistry) {
                        if (globalCarver.worldCarver() instanceof ElysianAbyssCarver) {
                            int range = 12;
                            for (int rx = -range; rx <= range; rx++) {
                                for (int rz = -range; rz <= range; rz++) {
                                    net.minecraft.world.level.ChunkPos originPos = new net.minecraft.world.level.ChunkPos(
                                            chunk.getPos().x + rx, chunk.getPos().z + rz);
                                    net.minecraft.util.RandomSource globalRandom = net.minecraft.util.RandomSource
                                            .create(seed ^ originPos.toLong());
                                    globalCarver.carve(context, chunk, carverBiomeManager::getBiome, globalRandom,
                                            aquifer, originPos, carvingMask);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                OririMod.LOGGER.error("ERROR in applyCarvers: " + e.getMessage(), e);
            } finally {
                ElysianAbyssCarver.CURRENT_AQUIFER.remove();
            }
            if (step == GenerationStep.Carving.AIR) {
                markExposedCaveFluids(chunk, null);
            }
        }

        if (step != GenerationStep.Carving.AIR)
            return;

        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos floodPos = new BlockPos.MutableBlockPos();

        // Volumetric Hydrostatic Sea Level & Stepped River Water Fill:
        // Any uncarved or carved space below sea level (oceans/beaches) or stepped
        // river pool level is naturally water!
        final ElderValleyTerrainHelper.ValleyColumnInfo postCarveValley = new ElderValleyTerrainHelper.ValleyColumnInfo();
        final ElderwoodsBiomeSource ebsPostCarve = (this.biomeSourceReference instanceof ElderwoodsBiomeSource src)
                ? src
                : null;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                Holder<Biome> colBiome;
                if (ebsPostCarve != null) {
                    colBiome = ebsPostCarve.getSurfaceBiome(worldX, worldZ);
                    ebsPostCarve.sampleValleyColumn(worldX, worldZ, postCarveValley);
                } else {
                    colBiome = this.biomeSourceReference.getNoiseBiome(
                            net.minecraft.core.QuartPos.fromBlock(worldX),
                            net.minecraft.core.QuartPos.fromBlock(surfaceY),
                            net.minecraft.core.QuartPos.fromBlock(worldZ),
                            randomState.sampler());
                    postCarveValley.reset(surfaceY);
                }

                boolean isOceanCol = colBiome.is(THE_SILENT_OCEANS_KEY) || colBiome.is(THE_SILENT_DEPTHS_KEY)
                        || colBiome.is(THE_SUNKEN_TRENCHES_KEY);
                boolean isBeachCol = colBiome.is(ELDER_BEACH_KEY) || colBiome.is(ELDER_LAGOON_KEY);
                boolean isRiverWaterCol = postCarveValley.hasValley && postCarveValley.inWaterChannel;

                int targetWaterTop = Integer.MIN_VALUE;
                if (isOceanCol || (isBeachCol && surfaceY < BASE_HEIGHT)) {
                    targetWaterTop = BASE_HEIGHT;
                }
                if (isRiverWaterCol) {
                    targetWaterTop = Math.max(targetWaterTop, postCarveValley.waterY);
                }

                if (targetWaterTop > surfaceY) {
                    for (int y = targetWaterTop; y > surfaceY; y--) {
                        floodPos.set(worldX, y, worldZ);
                        BlockState bs = chunk.getBlockState(floodPos);
                        if (bs.isAir()) {
                            chunk.setBlockState(floodPos, WATER_STATE, false);
                            if (isRiverWaterCol && postCarveValley.isCascadeTickCell && y >= targetWaterTop - 1) {
                                chunk.markPosForPostprocessing(floodPos);
                            }
                        }
                    }
                }
            }
        }

        BlockPos.MutableBlockPos clusterPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos adjacentPos = new BlockPos.MutableBlockPos();

        for (int localX = 2; localX <= 13; localX++) {
            for (int localZ = 2; localZ <= 13; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                for (int y = MIN_Y + 6; y <= surfaceY && y <= CRYSTAL_CAVE_Y_THRESHOLD; y++) {
                    clusterPos.set(worldX, y, worldZ);
                    BlockState currentBlock = chunk.getBlockState(clusterPos);

                    boolean isCrystalBlock = currentBlock.is(Blocks.AMETHYST_BLOCK) ||
                            currentBlock.is(ModBlocks.MANA_CRYSTAL_BLOCK.get());
                    if (!isCrystalBlock)
                        continue;

                    if (!shouldPlaceCluster(worldX, y, worldZ, seed))
                        continue;

                    int crystalType = currentBlock.is(ModBlocks.MANA_CRYSTAL_BLOCK.get()) ? 1 : 0;

                    adjacentPos.set(worldX, y + 1, worldZ);
                    if (chunk.getBlockState(adjacentPos).isAir()) {
                        BlockState cluster = crystalType == 1
                                ? ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.UP)
                                : Blocks.AMETHYST_CLUSTER.defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.UP);
                        chunk.setBlockState(adjacentPos, cluster, false);
                        continue;
                    }

                    adjacentPos.set(worldX, y - 1, worldZ);
                    if (chunk.getBlockState(adjacentPos).isAir()) {
                        BlockState cluster = crystalType == 1
                                ? ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.DOWN)
                                : Blocks.AMETHYST_CLUSTER.defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.DOWN);
                        chunk.setBlockState(adjacentPos, cluster, false);
                        continue;
                    }

                    adjacentPos.set(worldX, y, worldZ - 1);
                    if (chunk.getBlockState(adjacentPos).isAir()) {
                        BlockState cluster = crystalType == 1
                                ? ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
                                : Blocks.AMETHYST_CLUSTER.defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.NORTH);
                        chunk.setBlockState(adjacentPos, cluster, false);
                        continue;
                    }

                    adjacentPos.set(worldX, y, worldZ + 1);
                    if (chunk.getBlockState(adjacentPos).isAir()) {
                        BlockState cluster = crystalType == 1
                                ? ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.SOUTH)
                                : Blocks.AMETHYST_CLUSTER.defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.SOUTH);
                        chunk.setBlockState(adjacentPos, cluster, false);
                        continue;
                    }

                    adjacentPos.set(worldX - 1, y, worldZ);
                    if (chunk.getBlockState(adjacentPos).isAir()) {
                        BlockState cluster = crystalType == 1
                                ? ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.WEST)
                                : Blocks.AMETHYST_CLUSTER.defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.WEST);
                        chunk.setBlockState(adjacentPos, cluster, false);
                        continue;
                    }

                    adjacentPos.set(worldX + 1, y, worldZ);
                    if (chunk.getBlockState(adjacentPos).isAir()) {
                        BlockState cluster = crystalType == 1
                                ? ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.EAST)
                                : Blocks.AMETHYST_CLUSTER.defaultBlockState()
                                        .setValue(BlockStateProperties.FACING, Direction.EAST);
                        chunk.setBlockState(adjacentPos, cluster, false);
                    }
                }
            }
        }

        RandomSource fluoriteRandom = RandomSource.create(seed ^ chunk.getPos().toLong());
        double fluoriteNoiseOffset = seedOffsetCave + 12345.0;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                double patchNoise = Math.sin((worldX + fluoriteNoiseOffset) * 0.02)
                        * Math.cos((worldZ + fluoriteNoiseOffset) * 0.02);
                double detailNoise = Math.sin((worldX - fluoriteNoiseOffset) * 0.07)
                        * Math.cos((worldZ - fluoriteNoiseOffset) * 0.07);

                double combinedNoise = patchNoise * 0.7 + detailNoise * 0.3;

                if (combinedNoise < 0.3 || surfaceY < 58 || getAbyssNoise(worldX, worldZ) > 0.09)
                    continue;

                int maxFluoriteY = Math.min(surfaceY - 17, chunk.getMaxBuildHeight() - 2);
                for (int y = MIN_Y + 6; y <= maxFluoriteY; y++) {
                    clusterPos.set(worldX, y, worldZ);

                    if (chunk.getBlockState(clusterPos).isAir()) {
                        adjacentPos.set(worldX, y + 1, worldZ);
                        BlockState aboveState = chunk.getBlockState(adjacentPos);
                        if (aboveState.isSolid() && !aboveState.is(Blocks.BEDROCK)) {
                            if (isCrystalCaveBiome(this.biomeSourceReference.getNoiseBiome(worldX / 4, y / 4,
                                    worldZ / 4, randomState.sampler()))) {
                                float patchIntensity = (float) ((combinedNoise - 0.3) / 0.7);
                                float chance = 0.05f + (patchIntensity * 0.25f);

                                if (fluoriteRandom.nextFloat() < chance) {
                                    chunk.setBlockState(clusterPos, FLUORITE_CLUSTER_DOWN, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void carveScarletCaveEntrances(WorldGenRegion level, ChunkAccess chunk, int originX, int originZ,
            RandomState randomState) {
        int gridSize = 14;
        int gridX = Math.floorDiv(originX, gridSize);
        int gridZ = Math.floorDiv(originZ, gridSize);

        long gridSeed = level.getSeed() ^ (gridX * 341873128712L) ^ (gridZ * 132897987541L);
        RandomSource gridRandom = RandomSource.create(gridSeed);
        int pickX = gridX * gridSize + gridRandom.nextInt(gridSize);
        int pickZ = gridZ * gridSize + gridRandom.nextInt(gridSize);

        if (originX != pickX || originZ != pickZ)
            return;

        long caveSeed = level.getSeed() ^ (originX * 341873128712L) ^ (originZ * 132897987541L);
        RandomSource random = RandomSource.create(caveSeed);

        int centerX = (originX << 4) + 8;
        int centerZ = (originZ << 4) + 8;

        int surfaceY = getSurfaceHeight(centerX, centerZ);
        if (surfaceY < BASE_HEIGHT)
            return;

        Holder<Biome> entranceBiome;
        if (this.biomeSourceReference instanceof ElderwoodsBiomeSource ebs) {
            entranceBiome = ebs.getSurfaceBiome(centerX, centerZ);
        } else {
            entranceBiome = this.biomeSourceReference.getNoiseBiome(
                    net.minecraft.core.QuartPos.fromBlock(centerX),
                    net.minecraft.core.QuartPos.fromBlock(surfaceY),
                    net.minecraft.core.QuartPos.fromBlock(centerZ),
                    randomState.sampler());
        }
        if (!entranceBiome.is(SCARLET_SWAMP_KEY) && !entranceBiome.is(SCARLET_FOREST_KEY)
                && !entranceBiome.is(SCARLET_KARST_KEY)) {
            return;
        }
        if (this.biomeSourceReference instanceof ElderwoodsBiomeSource ebsCheck
                && ebsCheck.isInRiverChannel(centerX, centerZ)) {
            return;
        }

        boolean centerInThisChunk = (centerX >> 4) == chunk.getPos().x && (centerZ >> 4) == chunk.getPos().z;

        if (centerInThisChunk) {
            BlockPos.MutableBlockPos surfaceCheckPos = new BlockPos.MutableBlockPos(centerX, surfaceY, centerZ);
            BlockState surfaceBlock = chunk.getBlockState(surfaceCheckPos);
            if (!surfaceBlock.is(Blocks.GRASS_BLOCK) && !surfaceBlock.is(Blocks.DIRT)
                    && !surfaceBlock.is(Blocks.COARSE_DIRT)
                    && !surfaceBlock.is(ModBlocks.SCARLET_STONE.get())
                    && !surfaceBlock.is(ModBlocks.SCARLET_DEEPSLATE.get())
                    && !surfaceBlock.is(ModBlocks.SCARLET_MOSS.get())) {
                return;
            }

            int checkRadius = 8;
            BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
            for (int cx = -checkRadius; cx <= checkRadius; cx++) {
                for (int cz = -checkRadius; cz <= checkRadius; cz++) {
                    checkPos.set(centerX + cx, surfaceY, centerZ + cz);
                    if (chunk.getBlockState(checkPos)
                            .getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                        return;
                    }
                    checkPos.set(centerX + cx, surfaceY - 1, centerZ + cz);
                    if (chunk.getBlockState(checkPos)
                            .getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                        return;
                    }
                }
            }
        }

        double x = centerX;
        double y = surfaceY + 2;
        double z = centerZ;

        float yaw = random.nextFloat() * (float) Math.PI * 2.0f;
        float pitch = -((float) Math.PI / 4.0f) - (random.nextFloat() * (float) Math.PI / 4.0f);
        float radius = 3.5f + random.nextFloat() * 2.0f;

        {
            int maxOpeningRadius = (int) radius + 1;
            int openingTop = surfaceY + 3;
            int openingBottom = surfaceY - 5;
            int openingHeight = openingTop - openingBottom;
            BlockPos.MutableBlockPos openPos = new BlockPos.MutableBlockPos();
            for (int oy = openingTop; oy >= openingBottom; oy--) {
                double taperProgress = (double) (openingTop - oy) / openingHeight;
                double currentRadius = Mth.lerp(taperProgress, maxOpeningRadius, maxOpeningRadius * 0.33);
                double radiusSq = currentRadius * currentRadius;
                int checkRad = (int) Math.ceil(currentRadius);
                for (int ox = -checkRad; ox <= checkRad; ox++) {
                    for (int oz = -checkRad; oz <= checkRad; oz++) {
                        if (ox * ox + oz * oz > radiusSq)
                            continue;
                        int worldX = centerX + ox;
                        int worldZ2 = centerZ + oz;
                        if (chunk.getPos().x != (worldX >> 4) || chunk.getPos().z != (worldZ2 >> 4))
                            continue;
                        openPos.set(worldX, oy, worldZ2);
                        BlockState openState = chunk.getBlockState(openPos);
                        if (!openState.is(Blocks.BEDROCK) && !openState.isAir()) {
                            chunk.setBlockState(openPos, Blocks.AIR.defaultBlockState(), false);
                        }
                    }
                }
            }
        }

        carveWorm(level, chunk, random, x, y, z, yaw, pitch, radius, 120, 0, 1.0f, false);

        int numDeepWorms = 5 + random.nextInt(4);
        for (int i = 0; i < numDeepWorms; i++) {
            double dX = centerX + (random.nextInt(160) - 80);
            double dZ = centerZ + (random.nextInt(160) - 80);
            double dY = random.nextInt(118) - 58;

            float dYaw = random.nextFloat() * (float) Math.PI * 2.0f;
            float dPitch = (random.nextFloat() - 0.5f) * (float) Math.PI;
            float dRadius = 3.0f + random.nextFloat() * 3.0f;

            carveWorm(level, chunk, random, dX, dY, dZ, dYaw, dPitch, dRadius, 120, 0, 1.0f, true);
        }
    }

    private void carveWorm(WorldGenRegion level, ChunkAccess chunk, RandomSource random,
            double x, double y, double z, float yaw, float pitch, float radius, int steps, int branchDepth,
            float yScale, boolean shouldProtectSurface) {

        if (steps <= 0)
            return;
        if (branchDepth > 2)
            return;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int originalSurfaceY = getSurfaceHeight((int) x, (int) z);

        for (int i = 0; i < steps; i++) {
            double hDist = Math.cos(pitch);
            double dx = Math.cos(yaw) * hDist;
            double dy = Math.sin(pitch);
            double dz = Math.sin(yaw) * hDist;

            x += dx;
            y += dy;
            z += dz;

            yaw += (random.nextFloat() - 0.5f) * 0.4f;
            pitch += (random.nextFloat() - 0.5f) * 0.2f;

            if (y < originalSurfaceY - 20) {
                pitch *= 0.9f;
                if (Math.abs(pitch) < 0.1f && random.nextFloat() < 0.1f) {
                    pitch = (random.nextFloat() - 0.5f) * 0.5f;
                }
            }

            if (y > originalSurfaceY - 5 && pitch > 0) {
                pitch = -0.5f;
            }

            if (y < -58) {
                pitch = 0.5f;
                y = -58;
            }

            if (random.nextFloat() < 0.02f && steps > 20) {
                float branchYaw = yaw + (random.nextFloat() - 0.5f) * 2.0f;
                float branchPitch = pitch + (random.nextFloat() - 0.5f) * 1.0f;
                carveWorm(level, chunk, random, x, y, z, branchYaw, branchPitch, radius * 0.8f, steps / 2,
                        branchDepth + 1, yScale, shouldProtectSurface);
            }

            radius += (random.nextFloat() - 0.5f) * 0.2f;
            radius = Mth.clamp(radius, 0.5f, 25.0f);

            if (random.nextFloat() < 0.0005f && steps > 10) {
                carveIrregularRoom(level, chunk, random, x, y, z, shouldProtectSurface);
            }

            int checkRad = (int) radius + 2;
            int minX = Mth.floor(x - checkRad);
            int maxX = Mth.floor(x + checkRad);
            int minY = Mth.floor(y - checkRad);
            int maxY = Mth.floor(y + checkRad);
            int minZ = Mth.floor(z - checkRad);
            int maxZ = Mth.floor(z + checkRad);

            int startX_chunk = chunk.getPos().getMinBlockX();
            int endX_chunk = chunk.getPos().getMaxBlockX();
            int startZ_chunk = chunk.getPos().getMinBlockZ();
            int endZ_chunk = chunk.getPos().getMaxBlockZ();

            int minX_clip = Math.max(minX, startX_chunk);
            int maxX_clip = Math.min(maxX, endX_chunk);
            int minZ_clip = Math.max(minZ, startZ_chunk);
            int maxZ_clip = Math.min(maxZ, endZ_chunk);

            if (minX_clip <= maxX_clip && minZ_clip <= maxZ_clip) {
                for (int bx = minX_clip; bx <= maxX_clip; bx++) {
                    for (int bz = minZ_clip; bz <= maxZ_clip; bz++) {
                        for (int by = minY; by <= maxY; by++) {
                            mutablePos.set(bx, by, bz);
                            double dX = x - bx;
                            double dY = (y - by) / yScale;
                            double dZ = z - bz;
                            if (dX * dX + dY * dY + dZ * dZ < radius * radius) {
                                int localSurfaceY = getSurfaceHeight(bx, bz);
                                if (shouldProtectSurface && by >= localSurfaceY - 3)
                                    continue;

                                BlockState state = chunk.getBlockState(mutablePos);
                                if (!state.is(Blocks.BEDROCK) && !state.isAir() && state.getFluidState().isEmpty()
                                        && !ElderwoodsCaveAquifer.hasAdjacentFluidInChunk(chunk, bx, by, bz, 2, 1)) {
                                    chunk.setBlockState(mutablePos, Blocks.AIR.defaultBlockState(), false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void carveIrregularRoom(WorldGenRegion level, ChunkAccess chunk, RandomSource random, double x, double y,
            double z, boolean shouldProtectSurface) {
        int blobs = 3 + random.nextInt(4);
        for (int i = 0; i < blobs; i++) {
            float r = 3.0f + random.nextFloat() * 6.0f;
            double offX = (random.nextFloat() - 0.5f) * r * 1.5f;
            double offY = (random.nextFloat() - 0.5f) * (r * 0.8f);
            double offZ = (random.nextFloat() - 0.5f) * r * 1.5f;

            carveSphere(level, chunk, x + offX, y + offY, z + offZ, r, -64, shouldProtectSurface);
        }
    }

    private void carveSphere(WorldGenRegion level, ChunkAccess chunk, double x, double y, double z, float radius,
            int minY, boolean shouldProtectSurface) {
        int checkRad = (int) radius + 2;
        int minX = Mth.floor(x - checkRad);
        int maxX = Mth.floor(x + checkRad);
        int minY_block = Mth.floor(y - checkRad);
        int maxY_block = Mth.floor(y + checkRad);
        int minZ = Mth.floor(z - checkRad);
        int maxZ = Mth.floor(z + checkRad);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int startX_chunk = chunk.getPos().getMinBlockX();
        int endX_chunk = chunk.getPos().getMaxBlockX();
        int startZ_chunk = chunk.getPos().getMinBlockZ();
        int endZ_chunk = chunk.getPos().getMaxBlockZ();

        int minX_clip = Math.max(minX, startX_chunk);
        int maxX_clip = Math.min(maxX, endX_chunk);
        int minZ_clip = Math.max(minZ, startZ_chunk);
        int maxZ_clip = Math.min(maxZ, endZ_chunk);

        if (minX_clip > maxX_clip || minZ_clip > maxZ_clip) {
            return;
        }

        for (int bx = minX_clip; bx <= maxX_clip; bx++) {
            for (int bz = minZ_clip; bz <= maxZ_clip; bz++) {
                for (int by = minY_block; by <= maxY_block; by++) {
                    mutablePos.set(bx, by, bz);
                    double dX = x - bx;
                    double dY = y - by;
                    double dZ = z - bz;
                    if (dX * dX + dY * dY + dZ * dZ < radius * radius) {
                        int localSurfaceY = getSurfaceHeight(bx, bz);
                        if (shouldProtectSurface && by >= localSurfaceY - 3)
                            continue;

                        BlockState state = chunk.getBlockState(mutablePos);
                        if (!state.is(Blocks.BEDROCK) && !state.isAir() && state.getFluidState().isEmpty()
                                && !ElderwoodsCaveAquifer.hasAdjacentFluidInChunk(chunk, bx, by, bz, 2, 1)) {
                            chunk.setBlockState(mutablePos, Blocks.AIR.defaultBlockState(), false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random,
            ChunkAccess chunk) {
        initSeed(level.getSeed());
        try {
            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            chunk.fillBiomesFromNoise(
                    (bx, by, bz, sampler) -> this.biomeSourceReference.getNoiseBiome(bx, by, bz, sampler),
                    random.sampler());

            final ElderwoodsBiomeSource ebs = (this.biomeSourceReference instanceof ElderwoodsBiomeSource src) ? src
                    : null;
            final ElderValleyTerrainHelper.ValleyColumnInfo valleySurface = new ElderValleyTerrainHelper.ValleyColumnInfo();

            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int worldX = startX + localX;
                    int worldZ = startZ + localZ;
                    int surfaceY = getSurfaceHeight(worldX, worldZ);

                    if (ebs != null) {
                        ebs.sampleValleyColumn(worldX, worldZ, valleySurface);
                    } else {
                        valleySurface.reset(surfaceY);
                    }

                    // Cache the biome for the column surface once
                    Holder<Biome> surfaceBiome = (ebs != null)
                            ? ebs.getSurfaceBiome(worldX, worldZ)
                            : this.biomeSourceReference.getNoiseBiome(
                                    net.minecraft.core.QuartPos.fromBlock(worldX),
                                    net.minecraft.core.QuartPos.fromBlock(surfaceY),
                                    net.minecraft.core.QuartPos.fromBlock(worldZ),
                                    random.sampler());

                    boolean isScarletCurrent = isScarletBiome(surfaceBiome);
                    boolean isKarstCurrent = isScarletKarstBiome(surfaceBiome);
                    boolean isDesertCurrent = isGoldenDesertBiome(surfaceBiome);
                    boolean isSavannaCurrent = isGoldenSavannaBiome(surfaceBiome);
                    boolean isMesaCurrent = isGoldenMesaBiome(surfaceBiome);
                    boolean isHighlandCurrent = isHighlandBiome(surfaceBiome);
                    boolean isValleyCurrent = surfaceBiome.is(ELDER_VALLEY_KEY)
                            || (valleySurface.hasValley && valleySurface.inWaterChannel);
                    boolean isLagoonCurrent = surfaceBiome.is(ELDER_LAGOON_KEY);
                    boolean isBeachCurrent = surfaceBiome.is(ELDER_BEACH_KEY);
                    boolean isOceanCurrent = surfaceBiome.is(THE_SILENT_OCEANS_KEY);
                    boolean isDepthsCurrent = surfaceBiome.is(THE_SILENT_DEPTHS_KEY);
                    boolean isTrenchCurrent = surfaceBiome.is(THE_SUNKEN_TRENCHES_KEY);

                    BlockState topState;
                    BlockState subState;
                    int subDepth = 3;

                    // 1. Golden Desert, Golden Savanna & Golden Savanna Mesa
                    if (isDesertCurrent) {
                        topState = SOL_SAND;
                        subState = SOL_SANDSTONE;
                        subDepth = 4;
                    } else if (isMesaCurrent) {
                        int hPx = getSurfaceHeight(worldX + 1, worldZ);
                        int hNx = getSurfaceHeight(worldX - 1, worldZ);
                        int hPz = getSurfaceHeight(worldX, worldZ + 1);
                        int hNz = getSurfaceHeight(worldX, worldZ - 1);
                        int maxDrop1 = Math.max(0, Math.max(
                                Math.max(surfaceY - hPx, surfaceY - hNx),
                                Math.max(surfaceY - hPz, surfaceY - hNz)));
                        int maxDiff1 = Math.max(
                                Math.max(Math.abs(surfaceY - hPx), Math.abs(surfaceY - hNx)),
                                Math.max(Math.abs(surfaceY - hPz), Math.abs(surfaceY - hNz)));
                        int maxDrop2 = Math.max(0, Math.max(
                                Math.max(surfaceY - getSurfaceHeight(worldX + 2, worldZ),
                                        surfaceY - getSurfaceHeight(worldX - 2, worldZ)),
                                Math.max(surfaceY - getSurfaceHeight(worldX, worldZ + 2),
                                        surfaceY - getSurfaceHeight(worldX, worldZ - 2))));
                        double mesaPatch = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                (float) (worldX * 0.045f + 500.0f), (float) (worldZ * 0.045f - 300.0f), 2) / 0.32;

                        if (maxDrop1 >= 2 || maxDrop2 >= 3) {
                            // Vertical tablemountain cliff walls and steep ledges: stratified Caliche & terracotta bands
                            topState = selectMesaStrataBlock(worldX, surfaceY, worldZ);
                            subState = CALICHE != null ? CALICHE : STONE;
                            subDepth = Math.max(8, Math.min(18, maxDrop1 + 5));
                        } else {
                            // Rocky tablemountain plateau cap and terraced benches (no loose Sol Sand)
                            double mesaRock = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                    (float) (worldX * 0.085f - 190.0f), (float) (worldZ * 0.085f + 410.0f), 2) / 0.32;
                            double rockIndex = mesaPatch * 0.65 + mesaRock * 0.35;
                            if ((rockIndex < -0.04 || maxDiff1 >= 2) && CALICHE != null) {
                                topState = (rockIndex < -0.48 && SOL_SANDSTONE != null) ? SOL_SANDSTONE : CALICHE;
                                subState = CALICHE;
                                subDepth = 5;
                            } else if (rockIndex > 0.30) {
                                topState = COARSE_DIRT_STATE;
                                subState = CALICHE != null ? CALICHE : DIRT;
                                subDepth = 3;
                            } else {
                                topState = GRASS_BLOCK;
                                subState = DIRT;
                                subDepth = 3;
                            }
                        }
                    } else if (isSavannaCurrent) {
                        int maxDrop = Math.max(0, Math.max(
                                Math.max(surfaceY - getSurfaceHeight(worldX + 1, worldZ),
                                        surfaceY - getSurfaceHeight(worldX - 1, worldZ)),
                                Math.max(surfaceY - getSurfaceHeight(worldX, worldZ + 1),
                                        surfaceY - getSurfaceHeight(worldX, worldZ - 1))));
                        double savannaPatch = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                (float) (worldX * 0.032f - 210.0f), (float) (worldZ * 0.032f + 170.0f), 2) / 0.32;
                        if ((maxDrop >= 3 || savannaPatch < -0.52) && CALICHE != null) {
                            topState = CALICHE;
                            subState = CALICHE;
                            subDepth = Math.max(4, Math.min(10, maxDrop + 2));
                        } else if (savannaPatch > 0.38) {
                            topState = COARSE_DIRT_STATE;
                            subState = DIRT;
                        } else {
                            topState = GRASS_BLOCK;
                            subState = DIRT;
                        }
                    } else if (isKarstCurrent) {
                        int hPx = getSurfaceHeight(worldX + 1, worldZ);
                        int hNx = getSurfaceHeight(worldX - 1, worldZ);
                        int hPz = getSurfaceHeight(worldX, worldZ + 1);
                        int hNz = getSurfaceHeight(worldX, worldZ - 1);
                        int maxDrop1 = Math.max(0, Math.max(
                                Math.max(surfaceY - hPx, surfaceY - hNx),
                                Math.max(surfaceY - hPz, surfaceY - hNz)));
                        int maxDiff1 = Math.max(
                                Math.max(Math.abs(surfaceY - hPx), Math.abs(surfaceY - hNx)),
                                Math.max(Math.abs(surfaceY - hPz), Math.abs(surfaceY - hNz)));
                        int maxDrop2 = Math.max(0, Math.max(
                                Math.max(surfaceY - getSurfaceHeight(worldX + 2, worldZ),
                                        surfaceY - getSurfaceHeight(worldX - 2, worldZ)),
                                Math.max(surfaceY - getSurfaceHeight(worldX, worldZ + 2),
                                        surfaceY - getSurfaceHeight(worldX, worldZ - 2))));
                        double karstPatch = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                (float) (worldX * 0.055f - 310.0f), (float) (worldZ * 0.055f + 240.0f), 2) / 0.32;
                        int rockBand = Math.floorMod(surfaceY + (int) Math.round(karstPatch * 3.0), 12);
                        BlockState karstRock = ((rockBand == 2 || rockBand == 6 || rockBand == 9 || karstPatch > 0.25)
                                && SCARLET_DRIPSTONE != null)
                                        ? SCARLET_DRIPSTONE
                                        : (SCARLET_STONE != null ? SCARLET_STONE : STONE);

                        if (maxDrop1 >= 2 || maxDrop2 >= 4) {
                            // Vertical tower karst cliffs and sinkhole walls: Scarlet Stone & Scarlet Dripstone strata
                            topState = karstRock;
                            subState = (karstRock == SCARLET_DRIPSTONE)
                                    ? SCARLET_DRIPSTONE
                                    : (SCARLET_STONE != null ? SCARLET_STONE : STONE);
                            subDepth = Math.max(8, Math.min(18, maxDrop1 + 5));
                        } else if (surfaceY <= BASE_HEIGHT + 3) {
                            // Doline sinkhole floor: Scarlet Dripstone, Scarlet Stone, Coarse Dirt & Grass
                            if (karstPatch < 0.05 && SCARLET_DRIPSTONE != null) {
                                topState = SCARLET_DRIPSTONE;
                                subState = SCARLET_DRIPSTONE;
                            } else if (karstPatch < 0.35 && SCARLET_STONE != null) {
                                topState = SCARLET_STONE;
                                subState = SCARLET_STONE;
                            } else if (karstPatch < 0.55) {
                                topState = COARSE_DIRT_STATE;
                                subState = DIRT;
                            } else {
                                topState = GRASS_BLOCK;
                                subState = DIRT;
                            }
                            subDepth = 4;
                        } else {
                            // Karst tower crowns, ledges & rolling saddles
                            if ((karstPatch < -0.05 || maxDiff1 >= 2) && SCARLET_STONE != null) {
                                topState = karstRock;
                                subState = (karstRock == SCARLET_DRIPSTONE) ? SCARLET_DRIPSTONE : SCARLET_STONE;
                                subDepth = 5;
                            } else if (karstPatch > 0.28) {
                                topState = COARSE_DIRT_STATE;
                                subState = SCARLET_STONE != null ? SCARLET_STONE : DIRT;
                                subDepth = 3;
                            } else {
                                topState = GRASS_BLOCK;
                                subState = DIRT;
                                subDepth = 3;
                            }
                        }
                    } else if (isLagoonCurrent) {
                        // 2. Dedicated Elder Lagoon: sheltered mud/clay channels & mossy mangrove banks
                        // (no underwater stone steps)
                        double lagoonFactor = (ebs != null) ? ebs.getLagoonFeatureFactor(worldX, worldZ) : 0.6;
                        double swampPatch = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                (float) (worldX * 0.045f), (float) (worldZ * 0.045f), 2) / 0.32;
                        double rootNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                (float) (worldX * 0.14f + 137f), (float) (worldZ * 0.14f - 293f), 1) / 0.32;
                        double micro = (net.ganyusbathwater.oririmod.util.FastNoise.noise2D(
                                (float) (worldX * 0.22f), (float) (worldZ * 0.22f)) + 1.0) * 0.5;
                        if (surfaceY < BASE_HEIGHT) {
                            if (lagoonFactor < 0.22 && surfaceY >= BASE_HEIGHT - 2
                                    && (swampPatch + (micro - 0.5) * 0.45) > 0.12) {
                                topState = SAND_STATE;
                            } else if (rootNoise > 0.46 && surfaceY <= BASE_HEIGHT - 2) {
                                topState = CLAY_STATE;
                            } else {
                                topState = MUD_STATE;
                            }
                            subState = MUD_STATE;
                            subDepth = 4;
                        } else {
                            double outerBlend = Mth.clamp((0.28 - lagoonFactor) / 0.28 + swampPatch * 0.26, 0.0, 1.0);
                            if (outerBlend > micro && outerBlend > 0.36) {
                                topState = (surfaceY <= BASE_HEIGHT + 1 && outerBlend > 0.55) ? SAND_STATE
                                        : COARSE_DIRT_STATE;
                                subState = DIRT;
                            } else if (surfaceY == BASE_HEIGHT && rootNoise > 0.68) {
                                topState = MUDDY_MANGROVE_ROOTS_STATE;
                                subState = MUD_STATE;
                            } else if (surfaceY >= BASE_HEIGHT + 1 && swampPatch > 0.34) {
                                topState = MOSS_BLOCK_STATE;
                                subState = MUD_STATE;
                            } else if (surfaceY >= BASE_HEIGHT + 2 && (swampPatch + micro * 0.35) < 0.10) {
                                topState = GRASS_BLOCK;
                                subState = DIRT;
                            } else {
                                topState = MUD_STATE;
                                subState = MUD_STATE;
                            }
                            subDepth = 3;
                        }
                    } else if (isBeachCurrent) {
                        // 3. Elder Beach: Pure Shoreline Sediment Provinces (Sand / Gravel / Underwater Reefs)
                        double sedimentProvince = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                (float) (worldX * 0.0018f), (float) (worldZ * 0.0018f), 2) / 0.32;
                        BlockState beachTop;
                        if (surfaceY >= BASE_HEIGHT && sedimentProvince > -0.58) {
                            beachTop = SAND_STATE;
                        } else if (sedimentProvince > -0.20) {
                            beachTop = SAND_STATE;
                        } else if (sedimentProvince < -0.55) {
                            beachTop = GRAVEL_STATE;
                        } else {
                            double progress = (sedimentProvince - (-0.55)) / 0.35;
                            double wave1 = Math.sin(worldX * 0.08 + worldZ * 0.05) * 0.15;
                            double wave2 = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                    (float) (worldX * 0.025f), (float) (worldZ * 0.025f), 2) * 0.25;
                            double blendFactor = Mth.clamp(progress + wave1 + wave2, 0.0, 1.0);
                            double microDither = (net.ganyusbathwater.oririmod.util.FastNoise.noise2D(
                                    (float) (worldX * 0.12f), (float) (worldZ * 0.12f)) + 1.0) * 0.5;
                            beachTop = (blendFactor > microDither) ? SAND_STATE : GRAVEL_STATE;
                        }

                        if (surfaceY < BASE_HEIGHT) {
                            double reefNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                    (float) (worldX * 0.025f), (float) (worldZ * 0.025f), 2);
                            if (reefNoise > 0.48) {
                                beachTop = (reefNoise > 0.62) ? ANDESITE_STATE : STONE;
                            }
                        }
                        topState = beachTop;
                        subState = beachTop.is(Blocks.SAND) ? SANDSTONE_STATE : STONE;
                    } else if (isOceanCurrent || isDepthsCurrent || isTrenchCurrent) {
                        // Preserve underwater cave mouths carved in fillFromNoise: never seal WATER
                        // openings!
                        mutablePos.set(worldX, surfaceY, worldZ);
                        if (isDepthsCurrent && chunk.getBlockState(mutablePos).is(Blocks.WATER)) {
                            int floorY = surfaceY - 1;
                            BlockState floorState = null;
                            while (floorY >= MIN_Y + 5) {
                                mutablePos.set(worldX, floorY, worldZ);
                                BlockState probe = chunk.getBlockState(mutablePos);
                                if (!probe.is(Blocks.WATER) && !probe.isAir()) {
                                    floorState = probe;
                                    break;
                                }
                                floorY--;
                            }
                            if (floorState != null) {
                                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG)
                                        .update(localX, floorY, localZ, floorState);
                            }
                            continue;
                        }

                        double blendDither = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                (float) worldX * 0.07f, (float) worldZ * 0.07f, 2) * 0.35;
                        double slopeProgress = Mth.clamp((-20.0 - surfaceY) / 65.0, 0.0, 1.0);
                        double effectiveAbyss = slopeProgress + blendDither;

                        if (isTrenchCurrent && surfaceY <= -112) {
                            double magmaNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                    (float) (worldX * 0.08f), (float) (worldZ * 0.08f), 2);
                            topState = (magmaNoise > 0.15) ? AETHER_MAGMA : MAGMA_STATE;
                            subState = ABYSSAL_BASALT;
                        } else if (effectiveAbyss < 0.50) {
                            double sedimentProvince = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                    (float) (worldX * 0.0018f), (float) (worldZ * 0.0018f), 2) / 0.32;
                            if (sedimentProvince > -0.20) {
                                topState = SAND_STATE;
                                subState = SANDSTONE_STATE;
                            } else if (sedimentProvince < -0.55) {
                                topState = GRAVEL_STATE;
                                subState = STONE;
                            } else {
                                double progress = (sedimentProvince - (-0.55)) / 0.35;
                                double wave1 = Math.sin(worldX * 0.08 + worldZ * 0.05) * 0.15;
                                double wave2 = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                        (float) (worldX * 0.025f), (float) (worldZ * 0.025f), 2) * 0.25;
                                double blendFactor = Mth.clamp(progress + wave1 + wave2, 0.0, 1.0);
                                double microDither = (net.ganyusbathwater.oririmod.util.FastNoise.noise2D(
                                        (float) (worldX * 0.12f), (float) (worldZ * 0.12f)) + 1.0) * 0.5;
                                if (blendFactor > microDither) {
                                    topState = SAND_STATE;
                                    subState = SANDSTONE_STATE;
                                } else {
                                    topState = GRAVEL_STATE;
                                    subState = STONE;
                                }
                            }
                        } else {
                            int slopeX = Math.abs(surfaceY - getSurfaceHeight(worldX + 1, worldZ));
                            int slopeZ = Math.abs(surfaceY - getSurfaceHeight(worldX, worldZ + 1));
                            boolean isCrag = Math.max(slopeX, slopeZ) >= 2;
                            if (isCrag || surfaceY > -60) {
                                topState = ABYSSAL_BASALT;
                                subState = ABYSSAL_BASALT;
                            } else {
                                double valleySandNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                        (float) (worldX * 0.03f), (float) (worldZ * 0.03f), 2);
                                if (valleySandNoise > -0.25) {
                                    topState = ABYSS_SAND;
                                    subState = ABYSS_SAND;
                                } else {
                                    topState = ABYSSAL_BASALT;
                                    subState = ABYSSAL_BASALT;
                                }
                            }
                        }
                        subDepth = 4;
                    } else if (surfaceBiome.is(ELDER_HIGHLAND_SUMMIT_KEY)) {
                        double rockNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                (float) (worldX * 0.04f), (float) (worldZ * 0.04f), 2);
                        double snowDriftNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                (float) (worldX * 0.025f), (float) (surfaceY * 0.02f), (float) (worldZ * 0.025f), 3);

                        BlockState rockState = (rockNoise > 0.35)
                                ? ANDESITE_STATE
                                : (Math.abs(rockNoise) < 0.08 ? GRAVEL_STATE : STONE);
                        boolean isSnowCovered;
                        if (surfaceY < 155) {
                            topState = rockState;
                            isSnowCovered = false;
                        } else if (surfaceY < 175) {
                            double snowFactor = (surfaceY - 155.0) / 20.0;
                            if (snowDriftNoise + snowFactor > 0.45) {
                                topState = (snowDriftNoise > 0.35) ? POWDER_SNOW_STATE : SNOW_BLOCK_STATE;
                                isSnowCovered = true;
                            } else {
                                topState = rockState;
                                isSnowCovered = false;
                            }
                        } else {
                            if (rockNoise > 0.48) {
                                topState = rockState;
                                isSnowCovered = false;
                            } else {
                                topState = (snowDriftNoise > 0.30) ? POWDER_SNOW_STATE : SNOW_BLOCK_STATE;
                                isSnowCovered = true;
                            }
                        }
                        subState = isSnowCovered ? SNOW_BLOCK_STATE : rockState;
                    } else if (isValleyCurrent) {
                        int slopeX = Math.abs(surfaceY - getSurfaceHeight(worldX + 1, worldZ));
                        int slopeZ = Math.abs(surfaceY - getSurfaceHeight(worldX, worldZ + 1));
                        int maxSlope = Math.max(slopeX, slopeZ);
                        double fluvialNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                (float) (worldX * 0.045f), (float) (worldZ * 0.045f), 2) / 0.32;

                        if (valleySurface.isWeirCrest || valleySurface.isWaterfallStep) {
                            // Rock boulder weirs and wet cascade steps hold pools without eroding
                            topState = (fluvialNoise > 0.25) ? MOSSY_COBBLESTONE_STATE
                                    : (fluvialNoise < -0.20 ? ANDESITE_STATE : STONE);
                            subState = STONE;
                        } else if (valleySurface.inWaterChannel) {
                            // Sorted alluvial sediment: rocky/gravel in plunge pools/rapids, clay/sand/silt
                            // in calmer pools & estuaries
                            if (valleySurface.isPlungePool) {
                                topState = (fluvialNoise > 0.20) ? MOSSY_COBBLESTONE_STATE : GRAVEL_STATE;
                                subState = STONE;
                            } else if (valleySurface.isEstuary) {
                                if (fluvialNoise > 0.22) {
                                    topState = SAND_STATE;
                                    subState = SANDSTONE_STATE;
                                } else if (fluvialNoise < -0.35) {
                                    topState = MUD_STATE;
                                    subState = CLAY_STATE;
                                } else {
                                    topState = (fluvialNoise > -0.05) ? CLAY_STATE : GRAVEL_STATE;
                                    subState = STONE;
                                }
                            } else if (fluvialNoise > 0.36) {
                                topState = CLAY_STATE;
                                subState = CLAY_STATE;
                            } else if (fluvialNoise < -0.18) {
                                topState = SAND_STATE;
                                subState = SAND_STATE;
                            } else {
                                topState = GRAVEL_STATE;
                                subState = STONE;
                            }
                            subDepth = 2;
                        } else if (valleySurface.distToWaterEdge <= 5.5 && surfaceY <= valleySurface.waterY + 2) {
                            // Wide, beach-like gently sloping riverbank bars transitioning sand/gravel/mud
                            // into moss & grass
                            if (valleySurface.distToWaterEdge <= 2.4 && surfaceY <= valleySurface.waterY) {
                                if (fluvialNoise > 0.18) {
                                    topState = SAND_STATE;
                                    subState = SAND_STATE;
                                } else if (fluvialNoise < -0.22) {
                                    topState = MUD_STATE;
                                    subState = MUD_STATE;
                                } else {
                                    topState = GRAVEL_STATE;
                                    subState = SAND_STATE;
                                }
                            } else if (valleySurface.distToWaterEdge <= 4.0 && surfaceY <= valleySurface.waterY + 1) {
                                if (fluvialNoise > 0.25) {
                                    topState = SAND_STATE;
                                    subState = SAND_STATE;
                                } else if (fluvialNoise < -0.20) {
                                    topState = MUD_STATE;
                                    subState = DIRT;
                                } else {
                                    topState = GRAVEL_STATE;
                                    subState = DIRT;
                                }
                            } else {
                                if (fluvialNoise > 0.32) {
                                    topState = MOSS_BLOCK_STATE;
                                } else if (fluvialNoise < -0.28) {
                                    topState = MUD_STATE;
                                } else {
                                    topState = GRASS_BLOCK;
                                }
                                subState = DIRT;
                            }
                            subDepth = 2;
                        } else {
                            // Cover all dry valley slopes & cliffs naturally with grass top over deep dirt
                            int dropX = Math.max(surfaceY - getSurfaceHeight(worldX + 1, worldZ),
                                    surfaceY - getSurfaceHeight(worldX - 1, worldZ));
                            int dropZ = Math.max(surfaceY - getSurfaceHeight(worldX, worldZ + 1),
                                    surfaceY - getSurfaceHeight(worldX, worldZ - 1));
                            int maxDrop = Math.max(0, Math.max(dropX, dropZ));
                            topState = GRASS_BLOCK;
                            subState = DIRT;
                            subDepth = Math.max(3, Math.min(10, maxDrop + 1));
                        }
                    } else {
                        int dropX = Math.max(surfaceY - getSurfaceHeight(worldX + 1, worldZ),
                                surfaceY - getSurfaceHeight(worldX - 1, worldZ));
                        int dropZ = Math.max(surfaceY - getSurfaceHeight(worldX, worldZ + 1),
                                surfaceY - getSurfaceHeight(worldX, worldZ - 1));
                        int maxDrop = Math.max(0, Math.max(dropX, dropZ));
                        if (!valleySurface.hasValley
                                && maxDrop >= 5 && surfaceY > BASE_HEIGHT + 36
                                && !surfaceBiome.is(SCARLET_SWAMP_KEY)) {
                            topState = STONE;
                            subState = STONE;
                        } else {
                            if (surfaceY < BASE_HEIGHT - 1 && surfaceBiome.is(SCARLET_SWAMP_KEY)) {
                                double mudNoise = Math.sin(worldX * 0.1) * Math.cos(worldZ * 0.1)
                                        + 0.5 * Math.sin(worldX * 0.03 + 2.0) * Math.cos(worldZ * 0.04 - 1.0);
                                topState = (mudNoise > 0.3 || surfaceY < BASE_HEIGHT - 2) ? DIRT : GRASS_BLOCK;
                            } else if (surfaceBiome.is(ELDER_HIGHLAND_FOREST_KEY)) {
                                double podzolNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                        (float) (worldX * 0.03f), (float) (worldZ * 0.03f), 2);
                                topState = podzolNoise > 0.32 ? PODZOL_STATE : GRASS_BLOCK;
                            } else {
                                topState = GRASS_BLOCK;
                            }
                            subState = DIRT;
                            if (maxDrop >= 2) {
                                subDepth = Math.max(subDepth, Math.min(10, maxDrop + 1));
                            }
                        }
                    }

                    int minSubY = Math.max(MIN_Y + 5, surfaceY - subDepth);
                    for (int y = surfaceY; y >= minSubY; y--) {
                        mutablePos.set(worldX, y, worldZ);
                        if (isDepthsCurrent && y < surfaceY && chunk.getBlockState(mutablePos).is(Blocks.WATER)) {
                            break;
                        }
                        BlockState layerState = (y == surfaceY)
                                ? topState
                                : ((isMesaCurrent && subState == CALICHE) ? selectMesaStrataBlock(worldX, y, worldZ) : subState);
                        chunk.setBlockState(mutablePos, layerState, false);
                    }

                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG)
                            .update(localX, surfaceY, localZ, topState);
                    int worldSurfY = (valleySurface.hasValley && valleySurface.inWaterChannel
                            && valleySurface.waterY > surfaceY)
                                    ? valleySurface.waterY
                                    : surfaceY;
                    BlockState worldSurfState = (worldSurfY > surfaceY) ? WATER_STATE : topState;
                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)
                            .update(localX, worldSurfY, localZ, worldSurfState);
                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING)
                            .update(localX, worldSurfY, localZ, worldSurfState);
                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)
                            .update(localX, worldSurfY, localZ, worldSurfState);
                }
            }
        } catch (Exception e) {
            OririMod.LOGGER.error("CRITICAL ERROR in buildSurface for chunk " + chunk.getPos(), e);
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    private static final int SILENT_CAVE_ENTRANCE_GRID = 80;
    private static final int DEPTHS_ENTRANCE_CACHE_SIZE = 256;
    private static final int DEPTHS_ENTRANCE_CACHE_MASK = DEPTHS_ENTRANCE_CACHE_SIZE - 1;

    private static final class DepthsEntranceCandidate {
        long seed = Long.MIN_VALUE;
        int gx;
        int gz;
        boolean occupied;
        boolean valid;
        int centerX;
        int centerZ;
        int centerSurfaceY;
        long cellHash;
    }

    private final ThreadLocal<DepthsEntranceCandidate[]> depthsEntranceCache = ThreadLocal.withInitial(() -> {
        DepthsEntranceCandidate[] arr = new DepthsEntranceCandidate[DEPTHS_ENTRANCE_CACHE_SIZE];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new DepthsEntranceCandidate();
        }
        return arr;
    });

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private DepthsEntranceCandidate getDepthsEntranceForCell(int gx, int gz, ElderwoodsBiomeSource ebs) {
        DepthsEntranceCandidate[] cache = depthsEntranceCache.get();
        int slot = ((gx * 73856093) ^ (gz * 19349663)) & DEPTHS_ENTRANCE_CACHE_MASK;
        DepthsEntranceCandidate entry = cache[slot];
        if (entry.occupied && entry.seed == syncedSeed && entry.gx == gx && entry.gz == gz) {
            return entry;
        }

        long cellHash = mix64(syncedSeed ^ (gx * 0x9E3779B97F4A7C15L) ^ (gz * 0xD6E8FEB86659FD93L));
        int span = SILENT_CAVE_ENTRANCE_GRID - 44; // Strictly [22 .. 57] inside 80x80 cell so <=22 radius never crosses
                                                   // cell bounds
        int bestX = gx * SILENT_CAVE_ENTRANCE_GRID + 22 + (int) Math.floorMod(cellHash, (long) span);
        int bestZ = gz * SILENT_CAVE_ENTRANCE_GRID + 22 + (int) Math.floorMod(cellHash >>> 16, (long) span);
        boolean valid = false;
        int centerSurfaceY = BASE_HEIGHT;

        if (ebs != null) {
            long probeHash = cellHash;
            for (int attempt = 0; attempt < 6; attempt++) {
                int cx = gx * SILENT_CAVE_ENTRANCE_GRID + 22 + (int) Math.floorMod(probeHash, (long) span);
                int cz = gz * SILENT_CAVE_ENTRANCE_GRID + 22 + (int) Math.floorMod(probeHash >>> 16, (long) span);
                if (ebs.getSurfaceBiome(cx, cz).is(THE_SILENT_DEPTHS_KEY)
                        && ebs.sampleSunkenTrenchAbsCrack(cx, cz) >= 0.095) {
                    int cSurfY = ebs.computeSurfaceHeight(cx, cz, null);
                    if (cSurfY <= -42 && cSurfY >= -108) {
                        double margin = ebs.getDynamicAbyssThreshold(cx, cz) - ebs.sampleContinentalness(cx, cz);
                        if (margin >= 0.006
                                && isValidEntrancePerimeter(ebs, cx + 10, cz)
                                && isValidEntrancePerimeter(ebs, cx - 10, cz)
                                && isValidEntrancePerimeter(ebs, cx, cz + 10)
                                && isValidEntrancePerimeter(ebs, cx, cz - 10)) {
                            bestX = cx;
                            bestZ = cz;
                            centerSurfaceY = cSurfY;
                            valid = true;
                            break;
                        }
                    }
                }
                probeHash = mix64(probeHash ^ (0x517CC1B727220A95L * (attempt + 1)));
            }
        }

        entry.seed = syncedSeed;
        entry.gx = gx;
        entry.gz = gz;
        entry.centerX = bestX;
        entry.centerZ = bestZ;
        entry.centerSurfaceY = centerSurfaceY;
        entry.cellHash = cellHash;
        entry.valid = valid;
        entry.occupied = true;
        return entry;
    }

    private static boolean isValidEntrancePerimeter(ElderwoodsBiomeSource ebs, int px, int pz) {
        if (!ebs.getSurfaceBiome(px, pz).is(THE_SILENT_DEPTHS_KEY)
                || ebs.sampleSunkenTrenchAbsCrack(px, pz) < 0.075) {
            return false;
        }
        int py = ebs.computeSurfaceHeight(px, pz, null);
        if (py > -38 || py < -112) {
            return false;
        }
        return (ebs.getDynamicAbyssThreshold(px, pz) - ebs.sampleContinentalness(px, pz)) >= 0.002;
    }

    /**
     * Evaluates deterministic 3D water-filled caves and flared seafloor entrances
     * inside the_silent_depths.
     * Uses continuous C1 floor/roof envelopes, domain-warped 3D caverns, circular
     * L2 tubes, and smoothMax
     * fillets so walls, ceilings, floors, and trench/entrance junctions curve
     * organically without sharp seams.
     */
    private boolean isSilentDepthsWaterCave(
            int x, int y, int z, int surfaceY, double depthsCaveMask,
            double trenchAbsCrack, boolean isTrenchColumn,
            DepthsEntranceCandidate entrance, int entranceDistSq, double entranceJoinBoost) {
        int uFloorY = MIN_Y + 6;
        if (y < uFloorY) {
            return false;
        }

        // 1. Flared seafloor sinkhole entrance shaft + basal vestibule dome (continuous
        // C1 distance field)
        double dEntrance = -1.5;
        double bellJoin = 0.0;
        if (!isTrenchColumn && entrance != null && entranceDistSq <= 22 * 22 && trenchAbsCrack >= 0.072) {
            int entranceBottomY = MIN_Y + 9;
            int entranceTopY = Math.max(surfaceY, entrance.centerSurfaceY) + 1;
            if (y >= entranceBottomY && y <= entranceTopY) {
                double depthFrac = (double) (entranceTopY - y) / Math.max(1, entranceTopY - entranceBottomY);
                double windPhase = depthFrac * Math.PI * 1.5 + ((entrance.cellHash & 255L) * 0.025);
                double windAmp = 4.0 * Math.sin(depthFrac * Math.PI);
                double wx = entrance.centerX + Math.sin(windPhase) * windAmp;
                double wz = entrance.centerZ + Math.cos(windPhase * 1.15) * windAmp;

                double rimT = ElderwoodsBiomeSource.hermite01(1.0 - (double) (entranceTopY - y) / 9.0);
                double flareRadius = 4.4 + 3.6 * rimT;

                double bellTopY = Math.min(entranceTopY - 10.0, -80.0);
                double bellRadius = 0.0;
                if (bellTopY > entranceBottomY + 6.0) {
                    double bellCenterY = (entranceBottomY + bellTopY) * 0.5;
                    double bellHalfH = (bellTopY - entranceBottomY) * 0.52;
                    double bellU = Math.min(1.0, Math.abs(y - bellCenterY) / bellHalfH);
                    bellJoin = 0.5 * (1.0 + Math.cos(Math.PI * bellU));
                    bellRadius = 4.4 + 8.6 * bellJoin;
                }

                double baseRadius = ElderwoodsBiomeSource.smoothMax(flareRadius, bellRadius, 2.2);
                double bottomT = ElderwoodsBiomeSource.hermite01((double) (y - entranceBottomY) / 7.0);
                baseRadius *= bottomT;

                double wallWobble = (net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float) (x * 0.065f), (float) (y * 0.065f), (float) (z * 0.065f), 2) / 0.32) * 1.6 * bottomT;
                double radius = Math.max(0.0, baseRadius + wallWobble);
                double dx = x - wx;
                double dz = z - wz;
                dEntrance = (radius - Math.sqrt(dx * dx + dz * dz)) * 0.065;
            }
        }

        // 2. Shield upper Sunken Trench walls while cleanly curving deep hydrothermal
        // caves into trench floors
        double trenchUpperShield = (trenchAbsCrack < 0.095)
                ? ElderwoodsBiomeSource.hermite01((trenchAbsCrack - 0.048) / 0.047)
                : 1.0;

        double trenchBasalExtend = 0.0;
        if (trenchAbsCrack < 0.092) {
            double dt = ElderwoodsBiomeSource.hermite01((-97.0 - y) / 9.0);
            double et = ElderwoodsBiomeSource.hermite01((0.092 - trenchAbsCrack) / 0.036);
            trenchBasalExtend = 0.62 * dt * et;
        }

        double smoothMask = ElderwoodsBiomeSource.hermite01(
                Math.max(depthsCaveMask * trenchUpperShield, trenchBasalExtend));
        if (smoothMask <= 0.0 && dEntrance <= 0.0) {
            return false;
        }

        // 3. Smooth dome ceiling & undulating bowl floor envelopes (13-14 block C1
        // transitions)
        double openCeilY = (double) surfaceY - 5.5;
        double shieldedCeilY = Math.min(openCeilY, -99.0);
        double ceilLimitY = Mth.lerp(trenchUpperShield, shieldedCeilY, openCeilY);

        double trenchMouthT = (trenchAbsCrack < 0.068)
                ? ElderwoodsBiomeSource.hermite01((0.068 - trenchAbsCrack) / 0.025)
                : 0.0;
        if (trenchMouthT > 0.0) {
            ceilLimitY = Mth.lerp(trenchMouthT, ceilLimitY, -96.0);
        }

        double roofT = ElderwoodsBiomeSource.hermite01((y - (ceilLimitY - 14.0)) / 14.0);
        double floorWave = (net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) (x * 0.014f), (float) (z * 0.014f), 2) / 0.32) * 2.4;
        double floorTopY = -106.5 - trenchMouthT * 4.5 + floorWave;
        double floorT = ElderwoodsBiomeSource.hermite01((floorTopY - y) / 11.5);
        double envTaper = Math.max(roofT, floorT);

        if (dEntrance < -0.35 && (envTaper >= 0.96 || smoothMask <= 0.01)) {
            return false;
        }

        double caveDensity = -1.5;
        if (smoothMask > 0.01) {
            float cx = (float) (x + seedOffsetCave);
            float cy = (float) (y + seedOffsetCave);
            float cz = (float) (z + seedOffsetCave);

            // Light 3D domain warp breaks axis-aligned noise planes into organic billowing
            // vaults
            float warp = net.ganyusbathwater.oririmod.util.FastNoise.noise3D(
                    (cx + 4100f) * 0.011f, (cy + 4100f) * 0.011f, (cz + 4100f) * 0.011f) * 5.0f;

            double uCheese = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (cx + 6100f + warp) * 0.015f,
                    (cy + 6100f + warp * 0.7f) * 0.020f,
                    (cz + 6100f - warp) * 0.015f, 2) / 0.32;

            double cheeseThresh = -0.04 + (1.0 - smoothMask) * 0.62 + roofT * 1.15 + floorT * 1.15;
            double dCheese = uCheese - cheeseThresh;

            // Circular Euclidean (L2) worm tubes with full smooth C1 tapering to zero
            double tunnelRadius = (0.24 + entranceJoinBoost * bellJoin * 2.4) * smoothMask * (1.0 - envTaper);
            double dTunnel = -1.5;
            if (tunnelRadius > 0.015) {
                double t1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (cx + 7300f + warp * 0.8f) * 0.013f,
                        (cy + 7300f) * 0.017f,
                        (cz + 7300f) * 0.013f, 2) / 0.32;
                if (Math.abs(t1) < tunnelRadius + 0.25) {
                    double t2 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                            (cx + 8700f) * 0.013f,
                            (cy + 8700f + warp * 0.8f) * 0.017f,
                            (cz + 8700f - warp * 0.8f) * 0.013f, 2) / 0.32;
                    double tubeDist = Math.sqrt(t1 * t1 + t2 * t2);
                    dTunnel = (tunnelRadius - tubeDist) * 1.15;
                }
            }

            caveDensity = ElderwoodsBiomeSource.smoothMax(dCheese, dTunnel, 0.26);
        }

        if (dEntrance > -0.35) {
            caveDensity = ElderwoodsBiomeSource.smoothMax(caveDensity, dEntrance, 0.32);
        }

        if (dEntrance <= 0.0 && trenchMouthT <= 0.0 && y > surfaceY - 3) {
            return false;
        }

        return caveDensity > 0.0;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
            StructureManager structureManager, ChunkAccess chunk) {
        try {
            initSeedFromRandomState(random);

            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            ElderwoodsCaveAquifer caveAquifer = createCaveAquifer(syncedSeed, chunk.getPos());
            ElderwoodsCaveAquifer.MutablePointContext aquiferPoint = new ElderwoodsCaveAquifer.MutablePointContext();
            ElderwoodsBiomeSource ebs = (this.biomeSourceReference instanceof ElderwoodsBiomeSource src) ? src : null;

            int[] colMinAirY = new int[256];
            int[] colMaxAirY = new int[256];
            java.util.Arrays.fill(colMinAirY, Integer.MAX_VALUE);
            java.util.Arrays.fill(colMaxAirY, Integer.MIN_VALUE);
            boolean[] colHasSubFluid = new boolean[256];

            final ElderValleyTerrainHelper.ValleyColumnInfo fillValley = new ElderValleyTerrainHelper.ValleyColumnInfo();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int colIdx = (x << 4) | z;
                    int worldX = startX + x;
                    int worldZ = startZ + z;
                    int surfaceY = getSurfaceHeight(worldX, worldZ);

                    if (ebs != null) {
                        ebs.sampleValleyColumn(worldX, worldZ, fillValley);
                    } else {
                        fillValley.reset(surfaceY);
                    }

                    double caveNoise = getAbyssNoise(worldX, worldZ);
                    double abyssT = Mth.clamp((caveNoise - 0.15) / 0.07, 0.0, 1.0);

                    double caveFloorSmooth = getCaveFloorHeightDouble(worldX, worldZ, -115);
                    double caveCeilingSmooth = getCaveCeilingHeightDouble(worldX, worldZ, surfaceY);

                    int riverWaterY = (fillValley.hasValley && fillValley.inWaterChannel) ? fillValley.waterY
                            : BASE_HEIGHT;
                    int maxY = Math.max(surfaceY, Math.max(BASE_HEIGHT, riverWaterY));
                    boolean isRiverProtected = fillValley.hasValley && fillValley.isProtectedRiverbed;
                    int riverBedSafeMinY = fillValley.bedY - 12;

                    // Cache column biome properties
                    Holder<Biome> surfaceBiome = (ebs != null)
                            ? ebs.getSurfaceBiome(worldX, worldZ)
                            : this.biomeSourceReference.getNoiseBiome(
                                    net.minecraft.core.QuartPos.fromBlock(worldX),
                                    net.minecraft.core.QuartPos.fromBlock(surfaceY),
                                    net.minecraft.core.QuartPos.fromBlock(worldZ),
                                    random.sampler());

                    boolean isScarletColumn = isScarletBiome(surfaceBiome);
                    boolean isScarletSwampColumn = surfaceBiome.is(SCARLET_SWAMP_KEY);
                    boolean isDesertColumn = isGoldenDesertBiome(surfaceBiome);
                    boolean isSavannaColumn = isGoldenSavannaBiome(surfaceBiome);
                    boolean isMesaColumn = isGoldenMesaBiome(surfaceBiome);
                    boolean isAridColumn = isDesertColumn || isSavannaColumn || isMesaColumn;
                    boolean isDepthsColumn = surfaceBiome.is(THE_SILENT_DEPTHS_KEY);
                    boolean isTrenchColumn = surfaceBiome.is(THE_SUNKEN_TRENCHES_KEY);
                    boolean isDepthsOrTrench = isDepthsColumn || isTrenchColumn;
                    boolean isOceanColumn = isDepthsOrTrench || surfaceBiome.is(THE_SILENT_OCEANS_KEY);
                    boolean isBeachColumn = surfaceBiome.is(ELDER_BEACH_KEY) || surfaceBiome.is(ELDER_LAGOON_KEY);
                    boolean isHighlandColumn = isHighlandBiome(surfaceBiome);
                    boolean isMainland = (ebs == null) || ebs.isMainland(worldX, worldZ);

                    // Evaluate humidity once per eligible column: feeds Scarlet lithology and
                    // tapers Elysian caverns closed
                    // in solid rock BEFORE reaching Scarlet Caves (zero other biomes or sheared
                    // cave cuts inside Scarlet).
                    double colHumNoise = (!isOceanColumn && !isAridColumn && !isHighlandColumn)
                            ? getScarletHumidityNoise(worldX, worldZ)
                            : -2.0;
                    double oceanFloorFactor = Mth.clamp((surfaceY - 58.0) / 12.0, 0.0, 1.0);
                    double abyssOceanFade = oceanFloorFactor * oceanFloorFactor * (3.0 - 2.0 * oceanFloorFactor);
                    double abyssScarletFade = 1.0;
                    if (!isOceanColumn && !isAridColumn && !isHighlandColumn) {
                        double sf = Mth.clamp((0.45 - colHumNoise) / 0.15, 0.0, 1.0);
                        abyssScarletFade = sf * sf * (3.0 - 2.0 * sf);
                    }
                    double abyssIntensity = abyssT * abyssT * (3.0 - 2.0 * abyssT) * abyssScarletFade * abyssOceanFade;

                    // Silent Depths & Sunken Trench underwater cave + entrance parameters (hoisted
                    // once per column)
                    double depthsCaveMask = 0.0;
                    double trenchAbsCrack = 1.0;
                    DepthsEntranceCandidate entrance = null;
                    int entranceDistSq = Integer.MAX_VALUE;
                    double entranceJoinBoost = 0.0;
                    if (isDepthsOrTrench && ebs != null && surfaceY <= -42) {
                        trenchAbsCrack = ebs.sampleSunkenTrenchAbsCrack(worldX, worldZ);
                        if (isDepthsColumn && surfaceY >= -113) {
                            double margin = ebs.getDynamicAbyssThreshold(worldX, worldZ)
                                    - ebs.sampleContinentalness(worldX, worldZ);
                            if (margin > 0.003) {
                                depthsCaveMask = Mth.clamp((margin - 0.003) / 0.017, 0.0, 1.0)
                                        * Mth.clamp((surfaceY + 114.0) / 5.0, 0.0, 1.0);
                            }
                            int gx = Math.floorDiv(worldX, SILENT_CAVE_ENTRANCE_GRID);
                            int gz = Math.floorDiv(worldZ, SILENT_CAVE_ENTRANCE_GRID);
                            DepthsEntranceCandidate candidate = getDepthsEntranceForCell(gx, gz, ebs);
                            if (candidate.valid) {
                                int edx = worldX - candidate.centerX;
                                int edz = worldZ - candidate.centerZ;
                                entranceDistSq = edx * edx + edz * edz;
                                if (entranceDistSq <= 22 * 22) {
                                    entrance = candidate;
                                    entranceJoinBoost = Mth.clamp(1.0 - Math.sqrt(entranceDistSq) / 22.0, 0.0, 1.0)
                                            * 0.12;
                                }
                            }
                        }
                    }

                    // Hoist column-invariant lithology & smooth coastal cave ceiling bounds out of
                    // the Y loop
                    double coastalSeal = Mth.clamp((73.0 - (double) surfaceY) / 16.0, 0.0, 1.0);
                    int maxAllowedCaveY = (int) Math.floor((double) surfaceY - (15.0 + 5.0 * coastalSeal));
                    int ceilingTaperStart = maxAllowedCaveY - 18;
                    boolean scarletBorderActive = false;
                    boolean scarletSolidBase = isScarletColumn && !isHighlandColumn
                            && !isOceanColumn && SCARLET_STONE != null && SCARLET_DEEPSLATE != null;
                    int colScarletBottomY = Integer.MAX_VALUE;
                    if (!isAridColumn && !isOceanColumn && !isHighlandColumn
                            && oceanFloorFactor > 0.35
                            && SCARLET_STONE != null && SCARLET_DEEPSLATE != null) {
                        if (colHumNoise > 0.40 && colHumNoise < 0.54) {
                            scarletBorderActive = true;
                            colScarletBottomY = ElderwoodsBiomeSource.getScarletCaveBottomY(worldX, worldZ);
                        } else if (colHumNoise >= 0.54 || scarletSolidBase) {
                            scarletSolidBase = colHumNoise >= 0.54 || isScarletColumn;
                            if (scarletSolidBase) {
                                colScarletBottomY = ElderwoodsBiomeSource.getScarletCaveBottomY(worldX, worldZ);
                            }
                        }
                    } else {
                        scarletSolidBase = false;
                    }

                    boolean prevWasBasalt = false;
                    for (int y = MIN_Y; y <= maxY; y++) {
                        pos.set(worldX, y, worldZ);

                        if (y <= MIN_Y + 4) {
                            chunk.setBlockState(pos, BEDROCK, false);
                            prevWasBasalt = false;
                            continue;
                        }

                        if (y > surfaceY) {
                            prevWasBasalt = false;
                            if (fillValley.hasValley && fillValley.inWaterChannel && y <= fillValley.waterY) {
                                chunk.setBlockState(pos, WATER_STATE, false);
                                if (fillValley.isCascadeTickCell && y == fillValley.waterY) {
                                    chunk.markPosForPostprocessing(pos);
                                }
                                continue;
                            }
                            if (isScarletSwampColumn && isMainland
                                    && (ebs == null || ebs.sampleContinentalness(worldX, worldZ) >= 0.13)
                                    && y <= BASE_HEIGHT - 2) {
                                chunk.setBlockState(pos, BLOOD_WATER, false);
                                continue;
                            }
                            if (y <= BASE_HEIGHT && (isOceanColumn || (isBeachColumn && surfaceY < BASE_HEIGHT))) {
                                chunk.setBlockState(pos, WATER_STATE, false);
                                continue;
                            }
                            chunk.setBlockState(pos, AIR, false);
                            continue;
                        }

                        // Silent Depths & Sunken Trenches: purely Abyssal Basalt + WATER-filled
                        // underwater caves/entrances.
                        // Hydrothermal caves extending from trenches receive magma/aether-magma floor
                        // veins.
                        if (isDepthsOrTrench) {
                            if (y > MIN_Y + 6
                                    && (depthsCaveMask > 0.0 || trenchAbsCrack < 0.092
                                            || (!isTrenchColumn && entrance != null))
                                    && isSilentDepthsWaterCave(worldX, y, worldZ, surfaceY, depthsCaveMask,
                                            trenchAbsCrack, isTrenchColumn, entrance, entranceDistSq,
                                            entranceJoinBoost)) {
                                if (prevWasBasalt && y <= -96 && trenchAbsCrack < 0.125) {
                                    double trenchT = 1.0 - Mth.clamp((trenchAbsCrack - 0.035) / 0.090, 0.0, 1.0);
                                    double magmaNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                            (float) (worldX * 0.09f), (float) (worldZ * 0.09f), 2) / 0.32;
                                    if ((magmaNoise + (trenchT - 0.5) * 0.9) > 0.05) {
                                        pos.set(worldX, y - 1, worldZ);
                                        BlockState floorMagma = (magmaNoise > 0.35 && AETHER_MAGMA != null)
                                                ? AETHER_MAGMA
                                                : MAGMA_STATE;
                                        chunk.setBlockState(pos, floorMagma, false);
                                        pos.set(worldX, y, worldZ);
                                    }
                                }
                                chunk.setBlockState(pos, WATER_STATE, false);
                                prevWasBasalt = false;
                                continue;
                            }
                            chunk.setBlockState(pos, ABYSSAL_BASALT, false);
                            prevWasBasalt = true;
                            continue;
                        }

                        // Silent Oceans: solid abyssal basalt down to bedrock — zero empty air pockets
                        // below seabed
                        if (isOceanColumn) {
                            chunk.setBlockState(pos, ABYSSAL_BASALT, false);
                            continue;
                        }

                        if (y < MIN_Y + 6) {
                            chunk.setBlockState(pos, DEEPSLATE, false);
                            continue;
                        }

                        boolean allowCaveAtY = !isRiverProtected || y < riverBedSafeMinY;

                        double density = getElysianCavernDensity(worldX, y, worldZ, caveFloorSmooth, caveCeilingSmooth,
                                abyssIntensity);
                        if (allowCaveAtY && density < 0.15) {
                            chunk.setBlockState(pos, AIR, false);
                            if (y <= BASE_HEIGHT - 4 && y <= surfaceY - 12) {
                                if (y < colMinAirY[colIdx])
                                    colMinAirY[colIdx] = y;
                                if (y > colMaxAirY[colIdx])
                                    colMaxAirY[colIdx] = y;
                            }
                            continue;
                        }

                        // General Cave Generation (3D Cheese Caves & Tunnels with Vanilla-Style
                        // Aquifers)
                        if (allowCaveAtY && y < maxAllowedCaveY && y >= -108
                                && (density >= 0.45 || caveNoise <= 0.10)) {
                            double taperThreshold = 0.15 + (1.0 - oceanFloorFactor) * 1.6;
                            boolean allowTunnel = oceanFloorFactor >= 0.35;

                            if (y < -94) {
                                double taper = (-94 - y) / 14.0;
                                taperThreshold += taper * 1.5;
                                if (taper > 0.4)
                                    allowTunnel = false;
                            } else if (y > ceilingTaperStart) {
                                double taper = (double) (y - ceilingTaperStart) / 14.0;
                                taperThreshold += taper * 1.5;
                                if (taper > 0.4)
                                    allowTunnel = false;
                            }

                            if (taperThreshold <= 1.0 || allowTunnel) {
                                double cx = worldX + seedOffsetCave;
                                double cy = y + seedOffsetCave;
                                double cz = worldZ + seedOffsetCave;

                                double cheeseNoise = (taperThreshold <= 1.0)
                                        ? net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                                (float) (cx + 3000) * 0.015f,
                                                (float) (cy + 3000) * 0.015f,
                                                (float) (cz + 3000) * 0.015f, 2)
                                        : -2.0;

                                double cheeseGap = cheeseNoise - taperThreshold;
                                boolean isCheeseOpen = cheeseGap > 0.0;
                                double tunnelMargin = -1.0;
                                boolean isTunnel = false;

                                // Only evaluate tunnel noises if needed to carve a tunnel or win openness
                                if (allowTunnel && (!isCheeseOpen || cheeseGap < 0.39)) {
                                    double tunnelNoise1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                            (float) cx * 0.015f, (float) cy * 0.015f, (float) cz * 0.015f, 2);
                                    double absT1 = Math.abs(tunnelNoise1);
                                    if (absT1 < 0.05) {
                                        double tunnelNoise2 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                                (float) (cx + 1000) * 0.015f, (float) (cy + 1000) * 0.015f,
                                                (float) (cz + 1000) * 0.015f, 2);
                                        double absT2 = Math.abs(tunnelNoise2);
                                        if (absT2 < 0.05) {
                                            isTunnel = true;
                                            tunnelMargin = 0.05 - Math.max(absT1, absT2);
                                        }
                                    }
                                }

                                if (isCheeseOpen || isTunnel) {
                                    double openness = Math.max(cheeseGap * 1.8,
                                            tunnelMargin > 0.0 ? tunnelMargin * 14.0 : 0.0);
                                    double caveSubstance = -Mth.clamp(openness, 0.02, 0.85);
                                    BlockState aquiferBlock = caveAquifer.computeSubstance(
                                            aquiferPoint.set(worldX, y, worldZ),
                                            caveSubstance);
                                    if (aquiferBlock != null) {
                                        chunk.setBlockState(pos, aquiferBlock, false);
                                        if (!aquiferBlock.getFluidState().isEmpty()) {
                                            colHasSubFluid[colIdx] = true;
                                            if (caveAquifer.shouldScheduleFluidUpdate()
                                                    || x == 0 || x == 15 || z == 0 || z == 15) {
                                                chunk.markPosForPostprocessing(pos);
                                            }
                                        } else if (aquiferBlock.isAir() && y <= BASE_HEIGHT - 4 && y <= surfaceY - 12) {
                                            if (y < colMinAirY[colIdx])
                                                colMinAirY[colIdx] = y;
                                            if (y > colMaxAirY[colIdx])
                                                colMaxAirY[colIdx] = y;
                                        }
                                        continue;
                                    }
                                }
                            }
                        }

                        BlockState stoneState = STONE;
                        BlockState deepslateState = DEEPSLATE;

                        if (isMesaColumn && CALICHE != null && y >= BASE_HEIGHT - 6) {
                            stoneState = (y >= BASE_HEIGHT - 2) ? selectMesaStrataBlock(worldX, y, worldZ) : CALICHE;
                        } else if (isSavannaColumn && CALICHE != null && y >= surfaceY - 16 && y >= 0) {
                            stoneState = CALICHE;
                        } else if ((scarletSolidBase || scarletBorderActive) && (!isBeachColumn || y < surfaceY - 12)
                                && y >= colScarletBottomY - 3) {
                            boolean placeScarlet = scarletSolidBase;
                            if (scarletBorderActive) {
                                double dither = (net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                        (float) worldX * 0.1f, (float) y * 0.1f, (float) worldZ * 0.1f, 2) / 0.32)
                                        * 0.08;
                                placeScarlet = (colHumNoise + dither) > 0.48;
                            }
                            if (placeScarlet && y < colScarletBottomY + 3) {
                                double floorDither = (net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                        (float) worldX * 0.08f, (float) y * 0.08f, (float) worldZ * 0.08f, 2) / 0.32)
                                        * 3.0;
                                placeScarlet = y >= (colScarletBottomY + floorDither);
                            }
                            if (placeScarlet) {
                                stoneState = SCARLET_STONE;
                                deepslateState = SCARLET_DEEPSLATE;
                            }
                        }

                        chunk.setBlockState(pos, y < 0 ? deepslateState : stoneState, false);
                    }
                }
            }
            smoothCaveUndersides(chunk, colMinAirY, colMaxAirY);
            markExposedCaveFluids(chunk, colHasSubFluid);
        } catch (Exception e) {
            OririMod.LOGGER.error("CRITICAL: fillFromNoise failed for chunk " + chunk.getPos(), e);
            BlockPos.MutableBlockPos fallbackPos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = chunk.getPos().getMinBlockX() + x;
                    int worldZ = chunk.getPos().getMinBlockZ() + z;
                    for (int y = MIN_Y; y <= 72; y++) {
                        fallbackPos.set(worldX, y, worldZ);
                        chunk.setBlockState(fallbackPos, y <= MIN_Y + 4 ? BEDROCK : (y < 0 ? DEEPSLATE : STONE), false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * Fills single-block deep ceiling/underside pinholes (enclosed by 3+ horizontal
     * stone walls
     * with solid rock or fluid directly above) so cave ceilings and aquifer bottoms
     * are smooth.
     */
    private void smoothCaveUndersides(ChunkAccess chunk, int[] colMinAirY, int[] colMaxAirY) {
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();

        for (int lx = 1; lx < 15; lx++) {
            for (int lz = 1; lz < 15; lz++) {
                int colIdx = (lx << 4) | lz;
                int minAir = colMinAirY[colIdx];
                int maxAir = colMaxAirY[colIdx];
                if (minAir > maxAir) {
                    continue;
                }
                int worldX = startX + lx;
                int worldZ = startZ + lz;
                int surfaceY = getSurfaceHeight(worldX, worldZ);
                int maxY = Math.min(maxAir, Math.min(surfaceY - 12, BASE_HEIGHT - 4));
                int minY = Math.max(minAir, MIN_Y + 6);
                if (maxY < minY) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    pos.set(worldX, y, worldZ);
                    if (!chunk.getBlockState(pos).isAir()) {
                        continue;
                    }
                    BlockState below = chunk.getBlockState(check.set(worldX, y - 1, worldZ));
                    if (!below.isAir()) {
                        continue;
                    }
                    BlockState above = chunk.getBlockState(check.set(worldX, y + 1, worldZ));
                    boolean solidAbove = above.blocksMotion() && !above.is(Blocks.BEDROCK)
                            && above.getFluidState().isEmpty();
                    boolean fluidAbove = !above.getFluidState().isEmpty();
                    if (!solidAbove && !fluidAbove) {
                        continue;
                    }

                    int solidHoriz = 0;
                    BlockState firstSolidSide = null;
                    BlockState n1 = chunk.getBlockState(check.set(worldX + 1, y, worldZ));
                    if (n1.blocksMotion() && !n1.is(Blocks.BEDROCK) && n1.getFluidState().isEmpty()) {
                        solidHoriz++;
                        firstSolidSide = n1;
                    }
                    BlockState n2 = chunk.getBlockState(check.set(worldX - 1, y, worldZ));
                    if (n2.blocksMotion() && !n2.is(Blocks.BEDROCK) && n2.getFluidState().isEmpty()) {
                        solidHoriz++;
                        if (firstSolidSide == null)
                            firstSolidSide = n2;
                    }
                    BlockState n3 = chunk.getBlockState(check.set(worldX, y, worldZ + 1));
                    if (n3.blocksMotion() && !n3.is(Blocks.BEDROCK) && n3.getFluidState().isEmpty()) {
                        solidHoriz++;
                        if (firstSolidSide == null)
                            firstSolidSide = n3;
                    }
                    BlockState n4 = chunk.getBlockState(check.set(worldX, y, worldZ - 1));
                    if (n4.blocksMotion() && !n4.is(Blocks.BEDROCK) && n4.getFluidState().isEmpty()) {
                        solidHoriz++;
                        if (firstSolidSide == null)
                            firstSolidSide = n4;
                    }
                    if (solidHoriz >= 3) {
                        BlockState fillState = solidAbove ? above : firstSolidSide;
                        if (fillState != null) {
                            chunk.setBlockState(pos, fillState, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Marks any underground fluid block bordering open air or a chunk seam for
     * postprocessing
     * fluid ticks so cave water, blood water, and lava always update instead of
     * freezing mid-air.
     */
    private void markExposedCaveFluids(ChunkAccess chunk, boolean[] colHasSubFluid) {
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
        ElderwoodsBiomeSource ebs = (this.biomeSourceReference instanceof ElderwoodsBiomeSource src) ? src : null;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (colHasSubFluid != null && !colHasSubFluid[(lx << 4) | lz]) {
                    continue;
                }
                int worldX = startX + lx;
                int worldZ = startZ + lz;
                if (ebs != null) {
                    Holder<Biome> sb = ebs.getSurfaceBiome(worldX, worldZ);
                    if (sb.is(THE_SILENT_DEPTHS_KEY) || sb.is(THE_SUNKEN_TRENCHES_KEY)) {
                        continue; // Underwater caves in Silent Depths contain zero air blocks
                    }
                }
                boolean onChunkBorder = (lx == 0 || lx == 15 || lz == 0 || lz == 15);
                int surfaceY = getSurfaceHeight(worldX, worldZ);
                int maxY = Math.min(surfaceY - 4, chunk.getMaxBuildHeight() - 2);
                for (int y = MIN_Y + 5; y <= maxY; y++) {
                    pos.set(worldX, y, worldZ);
                    if (chunk.getFluidState(pos).isEmpty()) {
                        continue;
                    }
                    if (onChunkBorder) {
                        chunk.markPosForPostprocessing(pos);
                        continue;
                    }
                    if (chunk.getBlockState(check.set(worldX + 1, y, worldZ)).isAir()
                            || chunk.getBlockState(check.set(worldX - 1, y, worldZ)).isAir()
                            || chunk.getBlockState(check.set(worldX, y, worldZ + 1)).isAir()
                            || chunk.getBlockState(check.set(worldX, y, worldZ - 1)).isAir()
                            || chunk.getBlockState(check.set(worldX, y - 1, worldZ)).isAir()
                            || chunk.getBlockState(check.set(worldX, y + 1, worldZ)).isAir()) {
                        chunk.markPosForPostprocessing(pos);
                    }
                }
            }
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor level, RandomState random) {
        initSeedFromRandomState(random);
        return getSurfaceHeight(x, z);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        initSeedFromRandomState(random);
        int surfaceY = getSurfaceHeight(x, z);
        BlockState[] states = new BlockState[WORLD_HEIGHT];

        for (int i = 0; i < WORLD_HEIGHT; i++) {
            int y = MIN_Y + i;
            if (y <= MIN_Y + 4) {
                states[i] = BEDROCK;
            } else if (y < 0) {
                states[i] = DEEPSLATE;
            } else if (y <= surfaceY - 3) {
                states[i] = STONE;
            } else if (y <= surfaceY) {
                states[i] = (y == surfaceY) ? GRASS_BLOCK : DIRT;
            } else {
                states[i] = AIR;
            }
        }

        return new NoiseColumn(MIN_Y, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        int surfaceY = getSurfaceHeight(pos.getX(), pos.getZ());
        info.add("Elderwoods | Surface: Y=" + surfaceY);
        info.add("Cave Floor: Y=" + getCaveFloorHeight(pos.getX(), pos.getZ(), -115));
        info.add("Cave Ceiling: Y=" + getCaveCeilingHeight(pos.getX(), pos.getZ(), surfaceY));

        net.minecraft.world.level.biome.Climate.Sampler sampler = random.sampler();
        net.minecraft.world.level.biome.Climate.TargetPoint target = sampler.sample(pos.getX(), pos.getY(), pos.getZ());

        info.add(String.format("Biome Noise | T: %.2f H: %.2f C: %.2f E: %.2f W: %.2f D: %.2f",
                (float) net.minecraft.world.level.biome.Climate.quantizeCoord(target.temperature()),
                (float) net.minecraft.world.level.biome.Climate.quantizeCoord(target.humidity()),
                (float) net.minecraft.world.level.biome.Climate.quantizeCoord(target.continentalness()),
                (float) net.minecraft.world.level.biome.Climate.quantizeCoord(target.erosion()),
                (float) net.minecraft.world.level.biome.Climate.quantizeCoord(target.weirdness()),
                (float) net.minecraft.world.level.biome.Climate.quantizeCoord(target.depth())));
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return WORLD_HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return BASE_HEIGHT;
    }

    private boolean isCrystalCaveBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(CRYSTAL_CAVES_KEY);
    }
}
