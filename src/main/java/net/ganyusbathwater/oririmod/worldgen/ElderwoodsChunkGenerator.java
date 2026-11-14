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
    private static BlockState ELDERWOODS_GRASS;
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // ===== SCARLET BIOME BLOCKS =====
    private static BlockState SCARLET_GRASS;
    private static BlockState SCARLET_STONE;
    private static BlockState SCARLET_DEEPSLATE;

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
    private static final BlockState PODZOL_STATE = Blocks.PODZOL.defaultBlockState();
    private static final BlockState SNOW_BLOCK_STATE = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState POWDER_SNOW_STATE = Blocks.POWDER_SNOW.defaultBlockState();
    private static final BlockState MAGMA_STATE = Blocks.MAGMA_BLOCK.defaultBlockState();
    private static BlockState ABYSSAL_BASALT;
    private static BlockState ABYSS_SAND;
    private static BlockState HIGHLANDS_GRASS;
    private static BlockState SOL_SAND;
    private static BlockState SOL_SANDSTONE;
    private static BlockState AETHER_MAGMA;
    private static BlockState FLUORITE_CLUSTER_DOWN;

    private volatile net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator cachedCarvingDummyGen;

    // Biome keys for scarlet biomes
    private static final ResourceKey<Biome> SCARLET_SWAMP_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_swamp"));
    private static final ResourceKey<Biome> SCARLET_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest"));
    private static final ResourceKey<Biome> SCARLET_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_caves"));
    private static final ResourceKey<Biome> CRYSTAL_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "crystal_caves"));
    private static final ResourceKey<Biome> ELYSIAN_ABYSS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));
    private static final ResourceKey<Biome> GOLDEN_DESERT_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "golden_desert"));
    private static final ResourceKey<Biome> ELDER_HIGHLANDS_MEADOW_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highlands_meadow"));
    private static final ResourceKey<Biome> ELDER_HIGHLAND_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_forest"));
    private static final ResourceKey<Biome> ELDER_HIGHLAND_SUMMIT_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_highland_summit"));
    public static final ResourceKey<Biome> ELDER_BEACH_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elder_beach"));
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
                (float) ((z + currentHumOffsetZ) * ElderwoodsBiomeSource.SCARLET_HUMIDITY_NOISE_FREQUENCY), 3);
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
            if (SCARLET_GRASS == null) {
                ELDERWOODS_GRASS = ModBlocks.ELDERWOODS_GRASS_BLOCK.get().defaultBlockState();
                SCARLET_GRASS = ModBlocks.SCARLET_GRASS_BLOCK.get().defaultBlockState();
                SCARLET_STONE = ModBlocks.SCARLET_STONE.get().defaultBlockState();
                SCARLET_DEEPSLATE = ModBlocks.SCARLET_DEEPSLATE.get().defaultBlockState();
                ABYSSAL_BASALT = ModBlocks.ABYSSAL_BASALT.get().defaultBlockState();
                ABYSS_SAND = ModBlocks.ABYSS_SAND.get().defaultBlockState();
                HIGHLANDS_GRASS = ModBlocks.HIGHLANDS_GRASS_BLOCK.get().defaultBlockState();
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
                biomeHolder.is(SCARLET_CAVES_KEY);
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
            (float)(x + currentSeedOffsetCave) * (float)scale,
            (float)(z + currentSeedOffsetCave) * (float)scale,
            3
        );
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

        boolean isMainlandCol = false;
        if (this.biomeSourceReference instanceof ElderwoodsBiomeSource ebs) {
            isMainlandCol = ebs.isMainland(x, z);
        }
        // Under islands, beaches, and oceans, keep ceiling safely below the photic ocean seabed (<= 32)
        // so the cave can never breach the outer flank walls of the island seamount!
        double maxCeiling = isMainlandCol ? ((double) surfaceY - 15.0) : Math.min((double) surfaceY - 25.0, 32.0);
        return Mth.clamp(baseCeiling + ceilVariation, 20.0, maxCeiling);
    }



    private double getAbyssNoise(int x, int z) {
        // Synchronized with ElderwoodsBiomeSource
        float scale = 0.004f;
        return net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
            (float)((x + seedOffsetCave) * scale),
            (float)((z + seedOffsetCave) * scale),
            3
        );
    }

    private double getPillarNoise(int x, int z) {
        double nx = (x + seedOffsetCave + 555) * 0.04;
        double nz = (z + seedOffsetCave + 555) * 0.04;
        return (Math.sin(nx) * Math.cos(nz) + 1.0) / 2.0;
    }

    /**
     * 3D Noise Settings (Density Function) for the Giant Cave Cavern.
     */
    private double getElysianCavernDensity(int x, int y, int z, double caveFloor, double caveCeiling, double abyssIntensity) {
        // Target vertical bounds
        double roomHalfHeight = Math.max(1.0, (caveCeiling - caveFloor) / 2.0);
        double verticalCenter = (caveFloor + caveCeiling) / 2.0;

        double yDist = Math.abs(y - verticalCenter);
        double normalizedY = yDist / roomHalfHeight;

        // Base hollow shape + wall gradient
        double baseDensity = (normalizedY * normalizedY * 3.0) - 1.5;
        double wallGradient = (1.0 - abyssIntensity) * 2.5;
        double deterministicCore = baseDensity + wallGradient;

        // Since |fbm3D| <= 1.0, |noise| <= 2.5; when core > 2.65, final density is strictly > 0.15
        // and above the threshold, allowing an exact skip of 3D noise outside cavern strata.
        if (deterministicCore > 2.65) {
            return deterministicCore;
        }

        double scale = 0.012;
        double noise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
            (float)(x + seedOffsetCave) * (float)scale,
            (float)y * (float)scale,
            (float)(z + seedOffsetCave) * (float)scale,
            2
        ) * 2.5;

        return -noise + deterministicCore;
    }

    public static double getRiverIntensity(int x, int z) {
        // Main winding river
        double riverNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
            (float)(x + currentSeedOffsetCave + 1000) * 0.005f, (float)(z + currentSeedOffsetCave + 1000) * 0.005f, 2);
        double mainRiver = Math.max(0.0, 0.04 - Math.abs(riverNoise));

        // Tributary branches: 3 octaves adds high-frequency organic wiggles to break up straight grid lines
        double branchNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
            (float)(x + currentSeedOffsetCave + 2000) * 0.006f, (float)(z + currentSeedOffsetCave + 2000) * 0.006f, 3);
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

        if (step == GenerationStep.Carving.AIR || step == GenerationStep.Carving.LIQUID) {
            final ElderwoodsCaveAquifer caveAquifer = createCaveAquifer(seed, chunk.getPos());
            ElysianAbyssCarver.CURRENT_AQUIFER.set(caveAquifer);
            try {
                net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator dummyGen = this.cachedCarvingDummyGen;
                if (dummyGen == null) {
                    dummyGen = new net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator(
                            this.biomeSource,
                            level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.NOISE_SETTINGS)
                                    .getHolderOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.CAVES)
                    );
                    this.cachedCarvingDummyGen = dummyGen;
                }
                net.minecraft.world.level.levelgen.carver.CarvingContext context = new net.minecraft.world.level.levelgen.carver.CarvingContext(dummyGen, level.registryAccess(), chunk.getHeightAccessorForGeneration(), null, randomState, null);
                net.minecraft.world.level.chunk.CarvingMask carvingMask = null;
                if (chunk instanceof net.minecraft.world.level.chunk.ProtoChunk protoChunk) {
                    carvingMask = protoChunk.getOrCreateCarvingMask(step);
                }

                final BlockPos.MutableBlockPos carverPos = new BlockPos.MutableBlockPos();
                final net.minecraft.world.level.levelgen.Aquifer aquifer = new net.minecraft.world.level.levelgen.Aquifer() {
                    private boolean scheduleUpdate;

                    @Override
                    public BlockState computeSubstance(net.minecraft.world.level.levelgen.DensityFunction.FunctionContext ctx, double substance) {
                        if (substance > 0.0) {
                            this.scheduleUpdate = false;
                            return null;
                        }
                        int bx = ctx.blockX();
                        int by = ctx.blockY();
                        int bz = ctx.blockZ();
                        // Never overwrite pre-generated cave water/blood-water/lava lakes or breach an adjacent lake wall
                        if (chunk.getPos().getMinBlockX() <= bx && bx <= chunk.getPos().getMaxBlockX()
                                && chunk.getPos().getMinBlockZ() <= bz && bz <= chunk.getPos().getMaxBlockZ()
                                && by >= chunk.getMinBuildHeight() && by < chunk.getMaxBuildHeight()) {
                            carverPos.set(bx, by, bz);
                            if (!chunk.getFluidState(carverPos).isEmpty()) {
                                this.scheduleUpdate = false;
                                return null;
                            }
                        }

                        int surfaceHeight = getSurfaceHeight(bx, bz);
                        Holder<Biome> b = (biomeSourceReference instanceof ElderwoodsBiomeSource ebs)
                                ? ebs.getSurfaceBiome(bx, bz)
                                : biomeSourceReference.getNoiseBiome(bx / 4, by / 4, bz / 4, randomState.sampler());
                        if (b.is(THE_SILENT_DEPTHS_KEY) || b.is(THE_SUNKEN_TRENCHES_KEY)) {
                            this.scheduleUpdate = false;
                            return null; // Preserve water-filled Silent Depths caves and abyssal crust
                        }
                        boolean isOceanOrBeach = b.is(THE_SILENT_OCEANS_KEY) || b.is(ELDER_BEACH_KEY);

                        if (by <= BASE_HEIGHT && by > surfaceHeight && isOceanOrBeach) {
                            this.scheduleUpdate = false;
                            return WATER_STATE;
                        }
                        // Never breach the protective ocean floor basalt crust
                        if (isOceanOrBeach && surfaceHeight < BASE_HEIGHT - 1 && by >= surfaceHeight - 15) {
                            this.scheduleUpdate = false;
                            return null;
                        }
                        // Protect both local and cross-chunk aquifer basin dams from vanilla WorldCarver breaches
                        if (caveAquifer.hasAdjacentOrPredictedFluid(chunk, bx, by, bz, 2, 1)) {
                            this.scheduleUpdate = false;
                            return null;
                        }
                        // Leave surface cave mouths and Scarlet Cave entrances unobstructed air
                        if (by > surfaceHeight - 10) {
                            this.scheduleUpdate = false;
                            return Blocks.AIR.defaultBlockState();
                        }
                        // Keep Elysian Abyss cavern & lower Aether river zone unaffected by cave aquifers
                        if (getAbyssNoise(bx, bz) > 0.14 || (by <= -92 && getRiverIntensity(bx, bz) > 0.0)) {
                            this.scheduleUpdate = false;
                            return Blocks.AIR.defaultBlockState();
                        }

                        // Carvers run after fillFromNoise and must never materialize un-updated suspended fluid volumes in rock
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

                // 1. Vanilla Carvers (Properly loaded per origin chunk to allow biome crossing)
                int vanillaRange = 8;
                for (int rx = -vanillaRange; rx <= vanillaRange; rx++) {
                    for (int rz = -vanillaRange; rz <= vanillaRange; rz++) {
                        net.minecraft.world.level.ChunkPos originPos = new net.minecraft.world.level.ChunkPos(chunk.getPos().x + rx, chunk.getPos().z + rz);
                        net.minecraft.core.BlockPos originCenter = originPos.getMiddleBlockPosition(0);
                        
                        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> originBiome = this.biomeSource.getNoiseBiome(
                                net.minecraft.core.QuartPos.fromBlock(originCenter.getX()), 
                                0, 
                                net.minecraft.core.QuartPos.fromBlock(originCenter.getZ()), 
                                randomState.sampler()
                        );
                        net.minecraft.world.level.biome.BiomeGenerationSettings originSettings = originBiome.value().getGenerationSettings();

                        for (net.minecraft.core.Holder<net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?>> holder : originSettings.getCarvers(step)) {
                            net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?> carver = holder.value();
                            // Run normal biome carvers, excluding the global Abyss Carver
                            if (!(carver.worldCarver() instanceof ElysianAbyssCarver)) {
                                if (carver.worldCarver() instanceof ScarletCaveEntranceCarver) {
                                    if (!originBiome.is(SCARLET_SWAMP_KEY) && !originBiome.is(SCARLET_FOREST_KEY)) {
                                        continue;
                                    }
                                }
                                try {
                                    net.minecraft.util.RandomSource carverRandom = net.minecraft.util.RandomSource.create(seed ^ originPos.toLong());
                                    if (carver.isStartChunk(carverRandom)) {
                                        carver.carve(context, chunk, biomeManager::getBiome, carverRandom, aquifer, originPos, carvingMask);
                                    }
                                } catch (Exception ex) {
                                    OririMod.LOGGER.error("Error running vanilla carver", ex);
                                }
                            }
                        }
                    }
                }
                
                // 2. Run ElysianAbyssCarver globally on AIR step so ravines cross biome boundaries once cleanly
                if (step == GenerationStep.Carving.AIR) {
                    net.minecraft.core.Registry<net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?>> carverRegistry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_CARVER);
                    for (net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?> globalCarver : carverRegistry) {
                        if (globalCarver.worldCarver() instanceof ElysianAbyssCarver) {
                            int range = 12;
                            for (int rx = -range; rx <= range; rx++) {
                                for (int rz = -range; rz <= range; rz++) {
                                    net.minecraft.world.level.ChunkPos originPos = new net.minecraft.world.level.ChunkPos(chunk.getPos().x + rx, chunk.getPos().z + rz);
                                    net.minecraft.util.RandomSource globalRandom = net.minecraft.util.RandomSource.create(seed ^ originPos.toLong());
                                    globalCarver.carve(context, chunk, biomeManager::getBiome, globalRandom, aquifer, originPos, carvingMask);
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

        // Volumetric Hydrostatic Sea Level Water Fill:
        // Any uncarved or carved space below sea level in an ocean/beach column is naturally water!
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                Holder<Biome> colBiome;
                if (this.biomeSourceReference instanceof ElderwoodsBiomeSource ebs) {
                    colBiome = ebs.getSurfaceBiome(worldX, worldZ);
                } else {
                    colBiome = this.biomeSourceReference.getNoiseBiome(
                            net.minecraft.core.QuartPos.fromBlock(worldX),
                            net.minecraft.core.QuartPos.fromBlock(surfaceY),
                            net.minecraft.core.QuartPos.fromBlock(worldZ),
                            randomState.sampler());
                }

                boolean isOceanCol = colBiome.is(THE_SILENT_OCEANS_KEY) || colBiome.is(THE_SILENT_DEPTHS_KEY) || colBiome.is(THE_SUNKEN_TRENCHES_KEY);
                boolean isBeachCol = colBiome.is(ELDER_BEACH_KEY);

                if (isOceanCol || (isBeachCol && surfaceY < BASE_HEIGHT)) {
                    for (int y = BASE_HEIGHT; y > surfaceY; y--) {
                        floodPos.set(worldX, y, worldZ);
                        BlockState bs = chunk.getBlockState(floodPos);
                        if (bs.isAir()) {
                            chunk.setBlockState(floodPos, WATER_STATE, false);
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
                            if (isCrystalCaveBiome(this.biomeSourceReference.getNoiseBiome(worldX / 4, y / 4, worldZ / 4, randomState.sampler()))) {
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
        if (!entranceBiome.is(SCARLET_SWAMP_KEY) && !entranceBiome.is(SCARLET_FOREST_KEY)) {
            return;
        }

        boolean centerInThisChunk = (centerX >> 4) == chunk.getPos().x && (centerZ >> 4) == chunk.getPos().z;

        if (centerInThisChunk) {
            BlockPos.MutableBlockPos surfaceCheckPos = new BlockPos.MutableBlockPos(centerX, surfaceY, centerZ);
            BlockState surfaceBlock = chunk.getBlockState(surfaceCheckPos);
            if (!surfaceBlock.is(Blocks.GRASS_BLOCK) && !surfaceBlock.is(Blocks.DIRT)
                    && !surfaceBlock.is(ModBlocks.SCARLET_GRASS_BLOCK.get())
                    && !surfaceBlock.is(ModBlocks.ELDERWOODS_GRASS_BLOCK.get())) {
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
                        if (ox * ox + oz * oz > radiusSq) continue;
                        int worldX = centerX + ox;
                        int worldZ2 = centerZ + oz;
                        if (chunk.getPos().x != (worldX >> 4) || chunk.getPos().z != (worldZ2 >> 4)) continue;
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

            final ElderwoodsBiomeSource ebs = (this.biomeSourceReference instanceof ElderwoodsBiomeSource src) ? src : null;

            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int worldX = startX + localX;
                    int worldZ = startZ + localZ;
                    int surfaceY = getSurfaceHeight(worldX, worldZ);

                    // Cache the biome for the column surface once
                    Holder<Biome> surfaceBiome = (ebs != null)
                            ? ebs.getSurfaceBiome(worldX, worldZ)
                            : this.biomeSourceReference.getNoiseBiome(
                                    net.minecraft.core.QuartPos.fromBlock(worldX),
                                    net.minecraft.core.QuartPos.fromBlock(surfaceY),
                                    net.minecraft.core.QuartPos.fromBlock(worldZ),
                                    random.sampler());

                    boolean isScarletCurrent = isScarletBiome(surfaceBiome);
                    boolean isDesertCurrent = isGoldenDesertBiome(surfaceBiome);
                    boolean isHighlandCurrent = isHighlandBiome(surfaceBiome);
                    boolean isBeachCurrent = surfaceBiome.is(ELDER_BEACH_KEY);
                    boolean isOceanCurrent = surfaceBiome.is(THE_SILENT_OCEANS_KEY);
                    boolean isDepthsCurrent = surfaceBiome.is(THE_SILENT_DEPTHS_KEY);
                    boolean isTrenchCurrent = surfaceBiome.is(THE_SUNKEN_TRENCHES_KEY);

                    BlockState topState;
                    BlockState subState;
                    int subDepth = 3;

                    // 1. Golden Desert: Sol Sand on top, Sol Sandstone beneath
                    if (isDesertCurrent) {
                        topState = SOL_SAND;
                        subState = SOL_SANDSTONE;
                        subDepth = 4;
                    } else if (isBeachCurrent || (surfaceY >= 58 && surfaceY < BASE_HEIGHT + 2 && !isScarletCurrent && !isHighlandCurrent)) {
                        // 2. Elder Beach: Macro-Geological Shoreline Sediment Provinces + Flat Beach Mangrove Swamps
                        double flatBeachSwampFactor = (isBeachCurrent && ebs != null)
                                ? ebs.getFlatBeachMangroveFactor(worldX, worldZ)
                                : 0.0;
                        boolean placedSwamp = false;
                        if (flatBeachSwampFactor > 0.25) {
                            double swampPatch = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                    (float) (worldX * 0.045f), (float) (worldZ * 0.045f), 2) / 0.32;
                            if ((flatBeachSwampFactor + swampPatch * 0.28) > 0.42) {
                                double rootNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                        (float) (worldX * 0.14f + 137f), (float) (worldZ * 0.14f - 293f), 1) / 0.32;
                                if (surfaceY >= BASE_HEIGHT) {
                                    if (rootNoise > 0.46) {
                                        topState = MUDDY_MANGROVE_ROOTS_STATE;
                                    } else if (surfaceY >= BASE_HEIGHT + 1 && swampPatch > 0.42) {
                                        topState = MOSS_BLOCK_STATE;
                                    } else {
                                        topState = MUD_STATE;
                                    }
                                } else {
                                    topState = (rootNoise > 0.48) ? CLAY_STATE : MUD_STATE;
                                }
                                // Keep top 2 subsurface layers mud and 3rd stone cleanly
                                mutablePos.set(worldX, surfaceY, worldZ);
                                chunk.setBlockState(mutablePos, topState, false);
                                for (int y = surfaceY - 1; y >= Math.max(MIN_Y + 5, surfaceY - 3); y--) {
                                    mutablePos.set(worldX, y, worldZ);
                                    chunk.setBlockState(mutablePos, y >= surfaceY - 2 ? MUD_STATE : STONE, false);
                                }
                                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)
                                        .update(localX, surfaceY, localZ, topState);
                                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING)
                                        .update(localX, surfaceY, localZ, topState);
                                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)
                                        .update(localX, surfaceY, localZ, topState);
                                placedSwamp = true;
                            }
                        }
                        if (placedSwamp) {
                            continue;
                        }

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
                        // Preserve underwater cave mouths carved in fillFromNoise: never seal WATER openings!
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
                    } else {
                        BlockState grassState = isScarletCurrent ? SCARLET_GRASS : (isHighlandCurrent ? HIGHLANDS_GRASS : ELDERWOODS_GRASS);
                        int slopeX = Math.abs(surfaceY - getSurfaceHeight(worldX + 1, worldZ));
                        int slopeZ = Math.abs(surfaceY - getSurfaceHeight(worldX, worldZ + 1));
                        int maxSlope = Math.max(slopeX, slopeZ);
                        if (maxSlope >= 2 && !surfaceBiome.is(SCARLET_SWAMP_KEY)) {
                            topState = STONE;
                            subState = STONE;
                        } else {
                            if (surfaceY < BASE_HEIGHT - 1 && surfaceBiome.is(SCARLET_SWAMP_KEY)) {
                                double mudNoise = Math.sin(worldX * 0.1) * Math.cos(worldZ * 0.1)
                                        + 0.5 * Math.sin(worldX * 0.03 + 2.0) * Math.cos(worldZ * 0.04 - 1.0);
                                topState = (mudNoise > 0.3 || surfaceY < BASE_HEIGHT - 2) ? DIRT : grassState;
                            } else if (surfaceBiome.is(ELDER_HIGHLAND_FOREST_KEY)) {
                                double podzolNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                                        (float) (worldX * 0.03f), (float) (worldZ * 0.03f), 2);
                                topState = podzolNoise > 0.32 ? PODZOL_STATE : grassState;
                            } else {
                                topState = grassState;
                            }
                            subState = DIRT;
                        }
                    }

                    int minSubY = Math.max(MIN_Y + 5, surfaceY - subDepth);
                    for (int y = surfaceY; y >= minSubY; y--) {
                        mutablePos.set(worldX, y, worldZ);
                        if (isDepthsCurrent && y < surfaceY && chunk.getBlockState(mutablePos).is(Blocks.WATER)) {
                            break;
                        }
                        chunk.setBlockState(mutablePos, y == surfaceY ? topState : subState, false);
                    }

                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)
                            .update(localX, surfaceY, localZ, topState);
                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING)
                            .update(localX, surfaceY, localZ, topState);
                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)
                            .update(localX, surfaceY, localZ, topState);
                }
            }
        } catch (Exception e) {
            OririMod.LOGGER.error("CRITICAL ERROR in buildSurface for chunk " + chunk.getPos(), e);
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    private static final int SILENT_CAVE_ENTRANCE_GRID = 112;
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
        // Strictly inside [24..87] of each 112x112 cell so a <= 22-block radius never crosses cell bounds
        int centerX = gx * SILENT_CAVE_ENTRANCE_GRID + 24 + (int) Math.floorMod(cellHash, 64L);
        int centerZ = gz * SILENT_CAVE_ENTRANCE_GRID + 24 + (int) Math.floorMod(cellHash >>> 16, 64L);

        boolean valid = false;
        int centerSurfaceY = BASE_HEIGHT;
        if (ebs != null && ebs.getSurfaceBiome(centerX, centerZ).is(THE_SILENT_DEPTHS_KEY)) {
            centerSurfaceY = ebs.computeSurfaceHeight(centerX, centerZ, null);
            if (centerSurfaceY <= -56 && centerSurfaceY >= -105) {
                double margin = ebs.getDynamicAbyssThreshold(centerX, centerZ) - ebs.sampleContinentalness(centerX, centerZ);
                if (margin >= 0.020) {
                    valid = isValidEntrancePerimeter(ebs, centerX + 14, centerZ)
                            && isValidEntrancePerimeter(ebs, centerX - 14, centerZ)
                            && isValidEntrancePerimeter(ebs, centerX, centerZ + 14)
                            && isValidEntrancePerimeter(ebs, centerX, centerZ - 14);
                }
            }
        }

        entry.seed = syncedSeed;
        entry.gx = gx;
        entry.gz = gz;
        entry.centerX = centerX;
        entry.centerZ = centerZ;
        entry.centerSurfaceY = centerSurfaceY;
        entry.cellHash = cellHash;
        entry.valid = valid;
        entry.occupied = true;
        return entry;
    }

    private static boolean isValidEntrancePerimeter(ElderwoodsBiomeSource ebs, int px, int pz) {
        if (!ebs.getSurfaceBiome(px, pz).is(THE_SILENT_DEPTHS_KEY)) {
            return false;
        }
        int py = ebs.computeSurfaceHeight(px, pz, null);
        if (py > -50 || py < -110) {
            return false;
        }
        return (ebs.getDynamicAbyssThreshold(px, pz) - ebs.sampleContinentalness(px, pz)) >= 0.008;
    }

    /**
     * Evaluates deterministic 3D water-filled caves and flared seafloor entrances inside the_silent_depths.
     * Always fills cavities with Blocks.WATER and pinches shut safely inside the abyssal basalt perimeter,
     * guaranteeing zero dry air pockets and zero hanging non-flowing water seams.
     */
    private boolean isSilentDepthsWaterCave(
            int x, int y, int z, int surfaceY, double depthsCaveMask,
            DepthsEntranceCandidate entrance, int entranceDistSq, double entranceJoinBoost) {
        // 1. Flared seafloor sinkhole entrance shaft + basal vestibule chamber
        if (entrance != null && entranceDistSq <= 20 * 20) {
            int entranceBottomY = MIN_Y + 8;
            int entranceTopY = Math.max(surfaceY, entrance.centerSurfaceY) + 1;
            if (y >= entranceBottomY && y <= entranceTopY) {
                double depthFrac = (double) (entranceTopY - y) / Math.max(1, entranceTopY - entranceBottomY);
                double windPhase = depthFrac * Math.PI * 1.6 + ((entrance.cellHash & 255L) * 0.025);
                double windAmp = 4.5 * Math.sin(depthFrac * Math.PI);
                double wx = entrance.centerX + Math.sin(windPhase) * windAmp;
                double wz = entrance.centerZ + Math.cos(windPhase * 1.15) * windAmp;

                double rimT = Math.max(0.0, 1.0 - (double) (entranceTopY - y) / 7.5);
                double shaftRadius = 5.0 + 3.0 * rimT * rimT;
                if (y <= entranceBottomY + 16) {
                    double bell = Math.sin(Mth.clamp((double) (y - entranceBottomY) / 16.0, 0.0, 1.0) * Math.PI);
                    shaftRadius = Math.max(shaftRadius, 5.2 + 7.2 * bell);
                }

                double wallWobble = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float) (x * 0.085f), (float) (y * 0.085f), (float) (z * 0.085f), 2) * 2.0;
                double radius = shaftRadius + wallWobble;
                double dx = x - wx;
                double dz = z - wz;
                if (radius > 0.0 && (dx * dx + dz * dz) < radius * radius) {
                    return true;
                }
            }
        }

        // 2. Interconnected 3D underwater grottoes and submerged tunnels beneath the basalt seabed
        if (depthsCaveMask <= 0.0) {
            return false;
        }
        int uFloorY = MIN_Y + 8;
        int uMaxY = surfaceY - 5;
        if (uMaxY <= uFloorY + 3 || y < uFloorY || y > uMaxY) {
            return false;
        }

        double roofT = Mth.clamp((double) (y - (surfaceY - 10)) / 5.0, 0.0, 1.0);
        double floorT = Mth.clamp((double) (-115 - y) / 4.0, 0.0, 1.0);
        double taper = Math.max(roofT, floorT);
        // Allow basal entrance vestibules (y <= -104) to break cleanly into neighboring cave halls
        if (entranceJoinBoost > 0.0 && y <= -104 && y > -117) {
            taper *= (1.0 - entranceJoinBoost * 5.0);
            if (taper < 0.0) taper = 0.0;
        }

        double uCheeseThresh = 0.22 + (1.0 - depthsCaveMask) * 0.60 + taper * 1.4 - entranceJoinBoost;
        if (uCheeseThresh >= 1.0 && (taper >= 0.45 || depthsCaveMask <= 0.15)) {
            return false;
        }

        float cx = (float) (x + seedOffsetCave);
        float cy = (float) (y + seedOffsetCave);
        float cz = (float) (z + seedOffsetCave);

        if (uCheeseThresh < 1.0) {
            double uCheese = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (cx + 6100f) * 0.022f, (cy + 6100f) * 0.032f, (cz + 6100f) * 0.022f, 2);
            if (uCheese > uCheeseThresh) {
                return true;
            }
        }

        if (taper < 0.45 && depthsCaveMask > 0.15) {
            double tunHalfWidth = (0.058 + entranceJoinBoost * 0.15) * depthsCaveMask * (1.0 - taper);
            double uTunnel1 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (cx + 7300f) * 0.018f, (cy + 7300f) * 0.026f, (cz + 7300f) * 0.018f, 2);
            if (Math.abs(uTunnel1) < tunHalfWidth) {
                double uTunnel2 = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (cx + 8700f) * 0.018f, (cy + 8700f) * 0.026f, (cz + 8700f) * 0.018f, 2);
                return Math.abs(uTunnel2) < tunHalfWidth;
            }
        }
        return false;
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

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int colIdx = (x << 4) | z;
                    int worldX = startX + x;
                    int worldZ = startZ + z;
                    int surfaceY = getSurfaceHeight(worldX, worldZ);

                    double caveNoise = getAbyssNoise(worldX, worldZ);
                    double abyssIntensity = Mth.clamp((caveNoise - 0.20) / 0.03, 0.0, 1.0);

                    double caveFloorSmooth = getCaveFloorHeightDouble(worldX, worldZ, -115);
                    double caveCeilingSmooth = getCaveCeilingHeightDouble(worldX, worldZ, surfaceY);

                    int maxY = Math.max(surfaceY, BASE_HEIGHT);

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
                    boolean isDepthsColumn = surfaceBiome.is(THE_SILENT_DEPTHS_KEY);
                    boolean isTrenchColumn = surfaceBiome.is(THE_SUNKEN_TRENCHES_KEY);
                    boolean isDepthsOrTrench = isDepthsColumn || isTrenchColumn;
                    boolean isOceanColumn = isDepthsOrTrench || surfaceBiome.is(THE_SILENT_OCEANS_KEY);
                    boolean isBeachColumn = surfaceBiome.is(ELDER_BEACH_KEY);
                    boolean isHighlandColumn = isHighlandBiome(surfaceBiome);
                    boolean isMainland = (ebs == null) || ebs.isMainland(worldX, worldZ);

                    // Silent Depths underwater cave + entrance parameters (hoisted once per column)
                    double depthsCaveMask = 0.0;
                    DepthsEntranceCandidate entrance = null;
                    int entranceDistSq = Integer.MAX_VALUE;
                    double entranceJoinBoost = 0.0;
                    if (isDepthsColumn && ebs != null && surfaceY <= -50 && surfaceY >= -111) {
                        double margin = ebs.getDynamicAbyssThreshold(worldX, worldZ) - ebs.sampleContinentalness(worldX, worldZ);
                        if (margin > 0.014) {
                            depthsCaveMask = Mth.clamp((margin - 0.014) / 0.022, 0.0, 1.0)
                                    * Mth.clamp((surfaceY + 111.0) / 5.0, 0.0, 1.0);
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
                                entranceJoinBoost = Mth.clamp(1.0 - Math.sqrt(entranceDistSq) / 22.0, 0.0, 1.0) * 0.12;
                            }
                        }
                    }

                    // Hoist column-invariant lithology & cave ceiling bounds out of the Y loop
                    int maxAllowedCaveY = isMainland ? (surfaceY - 15) : Math.min(surfaceY - 25, 32);
                    int ceilingTaperStart = isMainland ? (surfaceY - 35) : Math.min(surfaceY - 35, 18);
                    boolean scarletBorderActive = false;
                    boolean scarletSolidBase = isScarletColumn && isMainland && !isHighlandColumn
                            && !isOceanColumn && !isBeachColumn && SCARLET_STONE != null && SCARLET_DEEPSLATE != null;
                    double colHumNoise = -1.0;
                    int colScarletBottomY = Integer.MAX_VALUE;
                    if (isMainland && !isDesertColumn && !isOceanColumn && !isBeachColumn && !isHighlandColumn
                            && SCARLET_STONE != null && SCARLET_DEEPSLATE != null) {
                        colHumNoise = getScarletHumidityNoise(worldX, worldZ);
                        if (colHumNoise > 0.15 && colHumNoise < 0.25) {
                            scarletBorderActive = true;
                            colScarletBottomY = ElderwoodsBiomeSource.getScarletCaveBottomY(worldX, worldZ);
                        } else if (colHumNoise >= 0.25 || scarletSolidBase) {
                            scarletSolidBase = colHumNoise >= 0.25 || isScarletColumn;
                            if (scarletSolidBase) {
                                colScarletBottomY = ElderwoodsBiomeSource.getScarletCaveBottomY(worldX, worldZ);
                            }
                        }
                    } else {
                        scarletSolidBase = false;
                    }

                    for (int y = MIN_Y; y <= maxY; y++) {
                        pos.set(worldX, y, worldZ);

                        if (y <= MIN_Y + 4) {
                            chunk.setBlockState(pos, BEDROCK, false);
                            continue;
                        }

                        if (y > surfaceY) {
                            if (isScarletSwampColumn && isMainland && y <= BASE_HEIGHT - 2) {
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

                        // Silent Depths & Sunken Trenches: purely Abyssal Basalt + WATER-filled underwater caves/entrances.
                        // Never enters dry Elysian caverns or dry cheese caves/aquifers -> zero air pockets possible.
                        if (isDepthsOrTrench) {
                            if (y > MIN_Y + 6 && isDepthsColumn
                                    && (depthsCaveMask > 0.0 || (entrance != null && entranceDistSq <= 20 * 20))
                                    && isSilentDepthsWaterCave(worldX, y, worldZ, surfaceY, depthsCaveMask,
                                            entrance, entranceDistSq, entranceJoinBoost)) {
                                chunk.setBlockState(pos, WATER_STATE, false);
                                continue;
                            }
                            chunk.setBlockState(pos, ABYSSAL_BASALT, false);
                            continue;
                        }

                        // Seabed Crust (Solid Rock Shell protecting Silent Oceans floor from caves)
                        if (isOceanColumn && surfaceY < BASE_HEIGHT - 1 && y >= surfaceY - 14) {
                            chunk.setBlockState(pos, ABYSSAL_BASALT, false);
                            continue;
                        }

                        if (y < MIN_Y + 6) {
                            chunk.setBlockState(pos, isOceanColumn ? ABYSSAL_BASALT : DEEPSLATE, false);
                            continue;
                        }

                        double density = getElysianCavernDensity(worldX, y, worldZ, caveFloorSmooth, caveCeilingSmooth, abyssIntensity);
                        if (density < 0.15) {
                            chunk.setBlockState(pos, AIR, false);
                            if (y <= BASE_HEIGHT - 4 && y <= surfaceY - 12) {
                                if (y < colMinAirY[colIdx]) colMinAirY[colIdx] = y;
                                if (y > colMaxAirY[colIdx]) colMaxAirY[colIdx] = y;
                            }
                            continue;
                        }

                        // General Cave Generation (3D Cheese Caves & Tunnels with Vanilla-Style Aquifers)
                        if (y < maxAllowedCaveY && y >= -108 && (density >= 0.45 || caveNoise <= 0.10)) {
                            double taperThreshold = 0.15;
                            boolean allowTunnel = true;

                            if (y < -94) {
                                double taper = (-94 - y) / 14.0;
                                taperThreshold += taper * 1.5;
                                if (taper > 0.4) allowTunnel = false;
                            } else if (y > ceilingTaperStart) {
                                double taper = (double) (y - ceilingTaperStart) / 14.0;
                                taperThreshold += taper * 1.5;
                                if (taper > 0.4) allowTunnel = false;
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
                                    double openness = Math.max(cheeseGap * 1.8, tunnelMargin > 0.0 ? tunnelMargin * 14.0 : 0.0);
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
                                            if (y < colMinAirY[colIdx]) colMinAirY[colIdx] = y;
                                            if (y > colMaxAirY[colIdx]) colMaxAirY[colIdx] = y;
                                        }
                                        continue;
                                    }
                                }
                            }
                        }

                        BlockState stoneState = STONE;
                        BlockState deepslateState = DEEPSLATE;

                        if (isOceanColumn) {
                            stoneState = ABYSSAL_BASALT;
                            deepslateState = ABYSSAL_BASALT;
                        } else if ((scarletSolidBase || scarletBorderActive) && y >= colScarletBottomY - 3) {
                            boolean placeScarlet = scarletSolidBase;
                            if (scarletBorderActive) {
                                double dither = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                        (float) worldX * 0.1f, (float) y * 0.1f, (float) worldZ * 0.1f, 2) * 0.05;
                                placeScarlet = (colHumNoise + dither) > 0.2;
                            }
                            if (placeScarlet && y < colScarletBottomY + 3) {
                                double floorDither = (net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                        (float) worldX * 0.08f, (float) y * 0.08f, (float) worldZ * 0.08f, 2) / 0.32) * 3.0;
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
     * Fills single-block deep ceiling/underside pinholes (enclosed by 3+ horizontal stone walls
     * with solid rock or fluid directly above) so cave ceilings and aquifer bottoms are smooth.
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
                    boolean solidAbove = above.blocksMotion() && !above.is(Blocks.BEDROCK) && above.getFluidState().isEmpty();
                    boolean fluidAbove = !above.getFluidState().isEmpty();
                    if (!solidAbove && !fluidAbove) {
                        continue;
                    }

                    int solidHoriz = 0;
                    BlockState firstSolidSide = null;
                    BlockState n1 = chunk.getBlockState(check.set(worldX + 1, y, worldZ));
                    if (n1.blocksMotion() && !n1.is(Blocks.BEDROCK) && n1.getFluidState().isEmpty()) { solidHoriz++; firstSolidSide = n1; }
                    BlockState n2 = chunk.getBlockState(check.set(worldX - 1, y, worldZ));
                    if (n2.blocksMotion() && !n2.is(Blocks.BEDROCK) && n2.getFluidState().isEmpty()) { solidHoriz++; if (firstSolidSide == null) firstSolidSide = n2; }
                    BlockState n3 = chunk.getBlockState(check.set(worldX, y, worldZ + 1));
                    if (n3.blocksMotion() && !n3.is(Blocks.BEDROCK) && n3.getFluidState().isEmpty()) { solidHoriz++; if (firstSolidSide == null) firstSolidSide = n3; }
                    BlockState n4 = chunk.getBlockState(check.set(worldX, y, worldZ - 1));
                    if (n4.blocksMotion() && !n4.is(Blocks.BEDROCK) && n4.getFluidState().isEmpty()) { solidHoriz++; if (firstSolidSide == null) firstSolidSide = n4; }
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
     * Marks any underground fluid block bordering open air or a chunk seam for postprocessing
     * fluid ticks so cave water, blood water, and lava always update instead of freezing mid-air.
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
                states[i] = (y == surfaceY) ? ELDERWOODS_GRASS : DIRT;
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
