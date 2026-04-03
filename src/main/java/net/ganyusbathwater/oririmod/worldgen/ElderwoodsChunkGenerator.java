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
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // ===== SCARLET BIOME BLOCKS =====
    private static BlockState SCARLET_GRASS;
    private static BlockState SCARLET_STONE;
    private static BlockState SCARLET_DEEPSLATE;

    // Biome keys for scarlet biomes
    private static final ResourceKey<Biome> SCARLET_PLAINS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_plains"));
    private static final ResourceKey<Biome> SCARLET_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest"));
    private static final ResourceKey<Biome> SCARLET_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_caves"));
    private static final ResourceKey<Biome> CRYSTAL_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "crystal_caves"));
    private static final ResourceKey<Biome> ELYSIAN_ABYSS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));

    private double seedOffsetX = 0;
    private double seedOffsetZ = 0;
    private double seedOffsetCave = 0;
    private boolean seedInitialized = false;

    // Cached Biome Holders for performance
    private Holder<Biome> cachedElderwoods;
    private Holder<Biome> cachedScarletPlains;
    private Holder<Biome> cachedScarletForest;
    private Holder<Biome> cachedScarletCaves;
    private Holder<Biome> cachedCrystalCaves;
    private Holder<Biome> cachedElderwoodsCave;
    private Holder<Biome> cachedElysianAbyss;

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
        if (!seedInitialized) {
            RandomSource random = RandomSource.create(seed);
            seedOffsetX = random.nextDouble() * 10000.0;
            seedOffsetZ = random.nextDouble() * 10000.0;
            seedOffsetCave = random.nextDouble() * 10000.0;
            seedInitialized = true;

            // Sync seed to BiomeSource so /locate biome works
            if (this.biomeSourceReference instanceof ElderwoodsBiomeSource elderwoodsBiomeSource) {
                elderwoodsBiomeSource.initSeed(seed);
            }

            // Initialize scarlet block states (lazy init to ensure blocks are registered)
            if (SCARLET_GRASS == null) {
                SCARLET_GRASS = ModBlocks.SCARLET_GRASS_BLOCK.get().defaultBlockState();
                SCARLET_STONE = ModBlocks.SCARLET_STONE.get().defaultBlockState();
                SCARLET_DEEPSLATE = ModBlocks.SCARLET_DEEPSLATE.get().defaultBlockState();
            }
        }
    }

    private void initSeedFromRandomState(RandomState randomState) {
        if (!seedInitialized) {
            net.minecraft.world.level.biome.Climate.TargetPoint sample =
                    randomState.sampler().sample(0, 0, 0);
            long derivedSeed = Double.doubleToRawLongBits(sample.temperature())
                    ^ (Double.doubleToRawLongBits(sample.humidity()) * 6364136223846793005L)
                    ^ (Double.doubleToRawLongBits(sample.continentalness()) * 1442695040888963407L);
            initSeed(derivedSeed);
        }
    }

    private boolean isScarletBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(SCARLET_PLAINS_KEY) ||
                biomeHolder.is(SCARLET_FOREST_KEY) ||
                biomeHolder.is(SCARLET_CAVES_KEY);
    }

    private boolean isElysianAbyssBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(ELYSIAN_ABYSS_KEY);
    }

    private int getSurfaceHeight(int x, int z) {
        // Synchronized with ElderwoodsBiomeSource
        double nx = (x + seedOffsetX) * 0.003;
        double nz = (z + seedOffsetZ) * 0.003;

        double noise = Math.sin(nx) * Math.cos(nz) * 12.0; 
        noise += Math.sin(nx * 0.5 + 2.0) * Math.cos(nz * 0.6 + 1.1) * 6.0;

        return BASE_HEIGHT + (int) Math.round(noise);
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

    private double getCaveFloorHeightDouble(int x, int z, int baseFloor) {
        return (double) baseFloor;
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

        return Mth.clamp(baseCeiling + ceilVariation, 30.0, (double)surfaceY - 10.0);
    }



    private double getAbyssNoise(int x, int z) {
        // Synchronized with ElderwoodsBiomeSource
        double nx = (x + seedOffsetCave) * 0.01;
        double nz = (z + seedOffsetCave) * 0.01;
        return Math.sin(nx) * Math.cos(nz);
    }

    private double getPillarNoise(int x, int z) {
        double nx = (x + seedOffsetCave + 555) * 0.04;
        double nz = (z + seedOffsetCave + 555) * 0.04;
        return (Math.sin(nx) * Math.cos(nz) + 1.0) / 2.0;
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

    private static final int LAVA_LAKE_LEVEL = MIN_Y + 30; 

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState,
            BiomeManager biomeManager, StructureManager structureManager,
            ChunkAccess chunk, GenerationStep.Carving step) {
        if (step == GenerationStep.Carving.AIR || step == GenerationStep.Carving.LIQUID) {
            try {
                net.minecraft.world.level.levelgen.carver.CarvingContext context = null;
                net.minecraft.world.level.chunk.CarvingMask carvingMask = null;
                if (chunk instanceof net.minecraft.world.level.chunk.ProtoChunk protoChunk) {
                    carvingMask = protoChunk.getOrCreateCarvingMask(step);
                }
                net.minecraft.world.level.levelgen.Aquifer aquifer = null;

                net.minecraft.core.BlockPos centerPos = chunk.getPos().getMiddleBlockPosition(0);
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome = biomeManager.getBiome(centerPos);

                net.minecraft.world.level.biome.BiomeGenerationSettings settings = biome.value()
                        .getGenerationSettings();

                for (net.minecraft.core.Holder<net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?>> holder : settings
                        .getCarvers(step)) {
                    net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?> carver = holder.value();

                    net.minecraft.util.RandomSource carverRandom = net.minecraft.util.RandomSource
                            .create(seed ^ chunk.getPos().toLong());
                    if (carver.worldCarver() instanceof ScarletCaveEntranceCarver
                            && carver.isStartChunk(carverRandom)) {
                        carver.carve(context, chunk, biomeManager::getBiome, carverRandom, aquifer, chunk.getPos(),
                                carvingMask);
                    } else if (carver.worldCarver() instanceof ElysianAbyssCarver
                            && isElysianAbyssBiome(biome)) {
                        carver.carve(context, chunk, biomeManager::getBiome, carverRandom, aquifer, chunk.getPos(),
                                carvingMask);
                    }
                }
            } catch (Throwable e) {
                OririMod.LOGGER.error("ERROR in applyCarvers: " + e.getMessage(), e);
            }
        }
        initSeed(seed);

        if (step != GenerationStep.Carving.AIR)
            return;

        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                for (int y = MIN_Y; y <= surfaceY; y++) {
                    pos.set(worldX, y, worldZ);
                    BlockState currentBlock = chunk.getBlockState(pos);
                    if (currentBlock.is(Blocks.WATER) || currentBlock.is(Blocks.LAVA)) {
                        chunk.setBlockState(pos, STONE, false);
                    }
                }

                // Comprehensive wood swap for structures like Mineshafts
                for (int y = MIN_Y; y <= surfaceY; y++) {
                    pos.set(worldX, y, worldZ);
                    BlockState currentBlock = chunk.getBlockState(pos);

                    if (currentBlock.is(Blocks.OAK_PLANKS) || currentBlock.is(Blocks.DARK_OAK_PLANKS)
                            || currentBlock.is(Blocks.SPRUCE_PLANKS) || currentBlock.is(Blocks.BIRCH_PLANKS)
                            || currentBlock.is(Blocks.ACACIA_PLANKS) || currentBlock.is(Blocks.JUNGLE_PLANKS)
                            || currentBlock.is(Blocks.MANGROVE_PLANKS) || currentBlock.is(Blocks.CHERRY_PLANKS)
                            || currentBlock.is(Blocks.BAMBOO_PLANKS)) {
                        chunk.setBlockState(pos, ModBlocks.ELDER_PLANKS.get().defaultBlockState(), false);
                    } else if (currentBlock.is(Blocks.OAK_FENCE) || currentBlock.is(Blocks.DARK_OAK_FENCE)
                            || currentBlock.is(Blocks.SPRUCE_FENCE) || currentBlock.is(Blocks.BIRCH_FENCE)
                            || currentBlock.is(Blocks.ACACIA_FENCE) || currentBlock.is(Blocks.JUNGLE_FENCE)
                            || currentBlock.is(Blocks.MANGROVE_FENCE) || currentBlock.is(Blocks.CHERRY_FENCE)
                            || currentBlock.is(Blocks.BAMBOO_FENCE) || currentBlock.is(Blocks.OAK_FENCE_GATE)
                            || currentBlock.is(Blocks.DARK_OAK_FENCE_GATE) || currentBlock.is(Blocks.SPRUCE_FENCE_GATE)
                            || currentBlock.is(Blocks.BIRCH_FENCE_GATE) || currentBlock.is(Blocks.ACACIA_FENCE_GATE)
                            || currentBlock.is(Blocks.JUNGLE_FENCE_GATE) || currentBlock.is(Blocks.MANGROVE_FENCE_GATE)
                            || currentBlock.is(Blocks.CHERRY_FENCE_GATE) || currentBlock.is(Blocks.BAMBOO_FENCE_GATE)) {
                        chunk.setBlockState(pos, ModBlocks.ELDER_FENCE.get().defaultBlockState(), false);
                    } else if (currentBlock.is(Blocks.OAK_LOG) || currentBlock.is(Blocks.DARK_OAK_LOG)
                            || currentBlock.is(Blocks.SPRUCE_LOG) || currentBlock.is(Blocks.BIRCH_LOG)
                            || currentBlock.is(Blocks.ACACIA_LOG) || currentBlock.is(Blocks.JUNGLE_LOG)
                            || currentBlock.is(Blocks.MANGROVE_LOG) || currentBlock.is(Blocks.CHERRY_LOG)
                            || currentBlock.is(Blocks.OAK_WOOD) || currentBlock.is(Blocks.DARK_OAK_WOOD)
                            || currentBlock.is(Blocks.SPRUCE_WOOD) || currentBlock.is(Blocks.BIRCH_WOOD)
                            || currentBlock.is(Blocks.ACACIA_WOOD) || currentBlock.is(Blocks.JUNGLE_WOOD)
                            || currentBlock.is(Blocks.MANGROVE_WOOD) || currentBlock.is(Blocks.CHERRY_WOOD)) {
                        chunk.setBlockState(pos, ModBlocks.ELDER_LOG_BLOCK.get().defaultBlockState(), false);
                    }
                }
            }
        }

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int originX = chunkX + dx;
                int originZ = chunkZ + dz;
                carveScarletCaveEntrances(level, chunk, originX, originZ, randomState);
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

                if (combinedNoise < 0.3)
                    continue;

                for (int y = MIN_Y + 6; y <= surfaceY - 5; y++) {
                    clusterPos.set(worldX, y, worldZ);

                    if (chunk.getBlockState(clusterPos).isAir()) {
                        adjacentPos.set(worldX, y + 1, worldZ);
                        BlockState aboveState = chunk.getBlockState(adjacentPos);
                        if (aboveState.isSolid() && !aboveState.is(Blocks.BEDROCK)) {
                            // if (isCrystalCaveBiome(getComputedBiome(level, worldX, y, worldZ))) { // Removed biome check
                                float patchIntensity = (float) ((combinedNoise - 0.3) / 0.7); 
                                float chance = 0.05f + (patchIntensity * 0.25f); 

                                if (fluoriteRandom.nextFloat() < chance) {
                                    chunk.setBlockState(clusterPos,
                                            ModBlocks.FLUORITE_CLUSTER.get().defaultBlockState()
                                                    .setValue(BlockStateProperties.FACING, Direction.DOWN),
                                            false);
                                }
                            // }
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
        if (surfaceY < -64)
            return;

        boolean centerInThisChunk = (centerX >> 4) == chunk.getPos().x && (centerZ >> 4) == chunk.getPos().z;

        if (centerInThisChunk) {
            BlockPos.MutableBlockPos surfaceCheckPos = new BlockPos.MutableBlockPos(centerX, surfaceY, centerZ);
            BlockState surfaceBlock = chunk.getBlockState(surfaceCheckPos);
            if (!surfaceBlock.is(Blocks.GRASS_BLOCK) && !surfaceBlock.is(Blocks.DIRT)
                    && !surfaceBlock.is(ModBlocks.SCARLET_GRASS_BLOCK.get())) {
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
                                if (!state.is(Blocks.BEDROCK) && !state.isAir()) {
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
                        if (!state.is(Blocks.BEDROCK) && !state.isAir()) {
                            chunk.setBlockState(mutablePos, Blocks.AIR.defaultBlockState(), false);
                        }
                    }
                }
            }
        }
    }

    private Holder<Biome> getComputedBiome(WorldGenRegion level, int x, int y, int z) {
        if (cachedElysianAbyss == null) {
            synchronized (this) {
                if (cachedElysianAbyss == null) {
                    var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
                    cachedElderwoods = registry.getHolderOrThrow(
                            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods")));
                    cachedScarletPlains = registry.getHolderOrThrow(SCARLET_PLAINS_KEY);
                    cachedScarletForest = registry.getHolderOrThrow(SCARLET_FOREST_KEY);
                    cachedScarletCaves = registry.getHolderOrThrow(SCARLET_CAVES_KEY);
                    cachedCrystalCaves = registry.getHolderOrThrow(CRYSTAL_CAVES_KEY);
                    cachedElderwoodsCave = registry.getHolderOrThrow(
                            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_cave")));
                    cachedElysianAbyss = registry.getHolderOrThrow(ELYSIAN_ABYSS_KEY);
                }
            }
        }

        if (!seedInitialized)
            initSeed(level.getSeed());

        double nx = x + seedOffsetX;
        double nz = z + seedOffsetZ;

        double surfaceNoise = Math.sin(nx * 0.002) * Math.cos(nz * 0.003) +
                0.5 * Math.cos(nx * 0.005 + 2.0) * Math.sin(nz * 0.005 + 1.0);

        double caveNoise = getAbyssNoise(x, z);

        int surfaceY = getSurfaceHeight(x, z);
        boolean isBelowSurfaceLayer = y < surfaceY - 4;

        if (isBelowSurfaceLayer) {
            if (caveNoise > 0.15) {
                return cachedElysianAbyss;
            } else if (caveNoise > 0.0) {
                return cachedScarletCaves;
            } else if (caveNoise < -0.3) {
                return cachedElderwoodsCave;
            } else {
                return cachedCrystalCaves;
            }
        } else {
            if (surfaceNoise > 0.5) {
                return cachedScarletForest;
            } else if (surfaceNoise < -0.5) {
                return cachedScarletPlains;
            } else {
                return cachedElderwoods;
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
                    (bx, by, bz, sampler) -> getComputedBiome(level, bx * 4, by * 4, bz * 4),
                    random.sampler());

            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int worldX = startX + localX;
                    int worldZ = startZ + localZ;
                    int surfaceY = getSurfaceHeight(worldX, worldZ);
                    boolean hasScarletBlocks = SCARLET_STONE != null && SCARLET_DEEPSLATE != null;
                    BlockState heightmapState = null;

                    for (int y = surfaceY; y >= MIN_Y; y--) {
                        mutablePos.set(worldX, y, worldZ);
                        BlockState current = chunk.getBlockState(mutablePos);
                        
                        // Check biome at THIS height to decide painting rules
                        Holder<Biome> currentBiome = getComputedBiome(level, worldX, y, worldZ);
                        boolean isScarletCurrent = isScarletBiome(currentBiome);

                        BlockState grassState = isScarletCurrent ? SCARLET_GRASS : GRASS;

                        // A. Top Surface Painting (Forest Floor)
                        if (y >= surfaceY - 5) {
                            if (y == surfaceY) {
                                chunk.setBlockState(mutablePos, grassState, false);
                                heightmapState = grassState;
                            } else if (y >= surfaceY - 3) {
                                chunk.setBlockState(mutablePos, DIRT, false);
                            }
                            continue;
                        }
                    }

                    if (heightmapState != null) {
                        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)
                                .update(localX, surfaceY, localZ, heightmapState);
                        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING)
                                .update(localX, surfaceY, localZ, heightmapState);
                        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)
                                .update(localX, surfaceY, localZ, heightmapState);
                    }
                }
            }
        } catch (Exception e) {
            OririMod.LOGGER.error("CRITICAL ERROR in buildSurface for chunk " + chunk.getPos(), e);
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
            StructureManager structureManager, ChunkAccess chunk) {
        try {
            initSeedFromRandomState(random);

            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = startX + x;
                    int worldZ = startZ + z;
                    int surfaceY = getSurfaceHeight(worldX, worldZ);

                    double caveNoise = getAbyssNoise(worldX, worldZ);
                    double abyssIntensity = Mth.clamp((caveNoise - 0.08) / 0.15, 0.0, 1.0);

                    double caveFloorSmooth = getCaveFloorHeightDouble(worldX, worldZ, -115);
                    double caveCeilingSmooth = getCaveCeilingHeightDouble(worldX, worldZ, surfaceY);
                    
                    int caveFloor = (int)Math.round(caveFloorSmooth);
                    int caveCeiling = (int)Math.round(caveCeilingSmooth);

                    double verticalCenter = (caveFloorSmooth + caveCeilingSmooth) / 2.0;
                    double roomHalfHeight = Math.max(1.0, (caveCeilingSmooth - caveFloorSmooth) / 2.0);

                    for (int y = MIN_Y; y <= surfaceY; y++) {
                        pos.set(worldX, y, worldZ);

                        if (y <= MIN_Y + 4) {
                            chunk.setBlockState(pos, BEDROCK, false);
                            continue;
                        }

                        boolean isHollow = false;
                        if (abyssIntensity > 0.1) {
                            // EL YSIAN ABYSS: Force perfectly flat floor and ceiling bounds
                            // This ignores 3D noise for the primary cavity to ensure a level floor.
                            if (y > -115 && y < caveCeilingSmooth) {
                                isHollow = true;
                            }
                        } else if (abyssIntensity > 0.0) {
                            double distFromCenter = Math.abs(y - verticalCenter);
                            double verticalFactor = Mth.clamp(1.0 - (distFromCenter / (double)roomHalfHeight), 0.0, 1.0);
                            double hollowingNoise3D = getAbyssNoise3D(worldX, y, worldZ);
                            double hollowingValue = (abyssIntensity * verticalFactor) + (hollowingNoise3D * 0.15 * verticalFactor);

                            if (hollowingValue > 0.35) {
                                isHollow = true;
                            }
                        }

                        if (isHollow) {
                            if (y < MIN_Y + 6) continue;
                            
                            // 3. EXTREME SPARSE PILLARS
                            int pillarGrid = 160;
                            int pGX = Math.floorDiv(worldX, pillarGrid);
                            int pGZ = Math.floorDiv(worldZ, pillarGrid);
                            
                            long pSeed = (long)pGX * 418731287L ^ (long)pGZ * 132897987L + (long)seedOffsetCave;
                            RandomSource pRand = RandomSource.create(pSeed);
                            
                            double pNoise = getPillarNoise(worldX, worldZ);
                            if (pNoise < 0.2) {
                                chunk.setBlockState(pos, AIR, false);
                                continue;
                            }
                            
                            double vP = (double)(y - caveFloor) / Math.max(1, (caveCeiling - caveFloor));
                            double pCenterX = Math.sin(worldX * 0.05 + worldZ * 0.05) * 4.0;
                            double pCenterZ = Math.cos(worldX * 0.05 - worldZ * 0.05) * 4.0;
                            double snakeX = Math.sin(vP * Math.PI) * 12.0;
                            double snakeZ = Math.cos(vP * Math.PI) * 12.0;
                            
                            double yNorm = (vP - 0.5) * 2.0;
                            double baseRad = 1.5 + pRand.nextDouble() * 2.0;
                            double hourglassRad = baseRad + (yNorm * yNorm * 9.5); 
                            double roughness = Math.sin(worldX * 0.2) * Math.sin(y * 0.25) * Math.sin(worldZ * 0.2) * 2.0;
                            
                            double dx = worldX - (pCenterX + snakeX);
                            double dz = worldZ - (pCenterZ + snakeZ);
                            double distSq = dx * dx + dz * dz;
                            
                            if (distSq < (hourglassRad + roughness) * (hourglassRad + roughness)) {
                                chunk.setBlockState(pos, y < 0 ? DEEPSLATE : STONE, false);
                            } else {
                                chunk.setBlockState(pos, AIR, false);
                            }
                        } else {
                            // Not hollow: place base stone/deepslate
                            chunk.setBlockState(pos, y < 0 ? DEEPSLATE : STONE, false);
                        }
                    }
                }
            }
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

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor level, RandomState random) {
        return getSurfaceHeight(x, z);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
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
                states[i] = (y == surfaceY) ? GRASS : DIRT;
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
        return 62;
    }

    private boolean isCrystalCaveBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(CRYSTAL_CAVES_KEY);
    }
}
