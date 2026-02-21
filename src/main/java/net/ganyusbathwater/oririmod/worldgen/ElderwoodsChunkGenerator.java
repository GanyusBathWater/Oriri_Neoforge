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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.ganyusbathwater.oririmod.worldgen.carver.ScarletCaveEntranceCarver;

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

    private double seedOffsetX = 0;
    private double seedOffsetZ = 0;
    private double seedOffsetCave = 0;
    private boolean seedInitialized = false;

    private final BiomeSource biomeSourceReference;

    public ElderwoodsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        this.biomeSourceReference = biomeSource;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private void initSeed(long seed) {
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

    /**
     * Check if the given biome holder is a scarlet biome
     */
    private boolean isScarletBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(SCARLET_PLAINS_KEY) ||
                biomeHolder.is(SCARLET_FOREST_KEY) ||
                biomeHolder.is(SCARLET_CAVES_KEY);
    }

    private int getSurfaceHeight(int x, int z) {
        double nx = x + seedOffsetX;
        double nz = z + seedOffsetZ;

        double noise = 0.0;
        noise += Math.sin(nx * 0.0025) * Math.cos(nz * 0.0025) * 10.0;
        noise += Math.cos(nx * 0.002 + 2.0) * Math.sin(nz * 0.003) * 8.0;
        noise += Math.sin(nx * 0.007 + 20) * Math.cos(nz * 0.006 + 20) * 5.0;
        noise += Math.cos(nx * 0.008) * Math.sin(nz * 0.009 + 15) * 4.0;
        noise += Math.sin(nx * 0.02 + 50) * Math.cos(nz * 0.018 + 50) * 2.5;
        noise += Math.sin(nx * 0.05 + 100) * Math.cos(nz * 0.045 + 100) * 1.0;
        noise += Math.sin(nx * 0.12 + 200) * Math.cos(nz * 0.12 + 200) * 0.4;

        noise = Mth.clamp(noise, -HILL_AMPLITUDE, HILL_AMPLITUDE);
        return BASE_HEIGHT + (int) Math.round(noise);
    }

    private int getCaveFloorHeight(int x, int z) {
        double nx = x + seedOffsetCave;
        double nz = z + seedOffsetCave;

        double floorNoise = Math.sin(nx * 0.01) * Math.cos(nz * 0.01);
        floorNoise += Math.sin(nx * 0.03 + 40) * Math.cos(nz * 0.025 + 40) * 0.5;
        floorNoise += Math.cos(nx * 0.05 + 80) * Math.sin(nz * 0.04 + 80) * 0.3;

        int baseFloor = MIN_Y + 35;
        int floorVariation = (int) (floorNoise * 25);

        return Mth.clamp(baseFloor + floorVariation, MIN_Y + 8, MIN_Y + 60);
    }

    private int getCaveCeilingHeight(int x, int z, int surfaceY) {
        double nx = x + seedOffsetCave + 500;
        double nz = z + seedOffsetCave + 500;

        double ceilNoise = 0;
        ceilNoise += Math.sin(nx * 0.008) * Math.cos(nz * 0.008) * 1.0;
        ceilNoise += Math.cos(nx * 0.015 + 30) * Math.sin(nz * 0.012 + 30) * 0.7;
        ceilNoise += Math.sin(nx * 0.025 + 60) * Math.cos(nz * 0.02 + 60) * 0.5;
        ceilNoise += Math.cos(nx * 0.04 + 90) * Math.sin(nz * 0.035 + 90) * 0.3;

        int baseCeiling = 40;
        int ceilVariation = (int) (ceilNoise * 35);

        return Mth.clamp(baseCeiling + ceilVariation, 15, surfaceY - 12);
    }

    private int[] findNearestEntranceCenter(int x, int z) {
        int gridSize = 180; // Reduced frequency (was 120)
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

        // Vary entrance bottom depth from Y=-16 to Y=16 based on entrance location
        double depthVariation = Math.sin(entranceCenterX * 0.1 + entranceCenterZ * 0.1 + seedOffsetCave);
        int entranceBottom = (int) (depthVariation * 16); // Range: -16 to 16

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

        // Add extra irregular noise for more natural shape
        double extraNoise = Mth.sin((float) (x * 0.05 + y * 0.1)) * Mth.cos((float) (z * 0.05 + y * 0.1)) * 1.5;

        radiusAtY += wobble + extraNoise;

        return distFromWindingCenter < radiusAtY;
    }

    // === LEGACY CAVE GENERATION (Preserved for reference) ===
    // This was the old "Cheese Cave" logic using sine waves.
    // Replaced by Giant Room Walkers for more organic shapes.
    private boolean isCaveLegacy(int x, int y, int z, int surfaceY) {
        if (y <= MIN_Y + 5)
            return false;

        double nx = x + seedOffsetCave;
        double ny = y;
        double nz = z + seedOffsetCave;

        int caveFloor = getCaveFloorHeight(x, z);
        int caveCeiling = getCaveCeilingHeight(x, z, surfaceY);

        if (y < caveFloor || y > caveCeiling)
            return false;
        if (y > surfaceY - 12)
            return false;

        // === BIG CAVE ROOMS (cheese caves) ===
        double cheese = 0.0;
        cheese += Math.sin(nx * 0.02 + 17) * Math.sin(ny * 0.025 + 31) * Math.sin(nz * 0.018 + 43);
        cheese += Math.cos(nx * 0.015 + 67) * Math.cos(ny * 0.02 + 89) * Math.cos(nz * 0.012 + 101) * 0.6;
        cheese += Math.sin(nx * 0.035 + 127) * Math.cos(ny * 0.03 + 151) * Math.sin(nz * 0.025 + 173) * 0.35;

        double depthBonus = Math.max(0, (40.0 - y) / 80.0) * 0.15;
        if (cheese > 0.5 - depthBonus)
            return true;

        return false;
    }

    // Crystal cave zone detection (below Y=-16)
    private static final int CRYSTAL_CAVE_Y_THRESHOLD = -16;
    private static final double CRYSTAL_ZONE_THRESHOLD = 0.82; // Higher = rarer, smaller geodes

    // Returns noise value for geode layering (higher = closer to center)
    private double getCrystalZoneNoise(int x, int y, int z) {
        if (y > CRYSTAL_CAVE_Y_THRESHOLD)
            return -1;

        double nx = (x + seedOffsetCave) * 0.035; // Higher frequency = smaller zones
        double ny = y * 0.045;
        double nz = (z + seedOffsetCave) * 0.035;

        // 3D noise for crystal zones
        double noise = Mth.sin((float) (nx * 1.5)) * Mth.cos((float) (nz * 1.5));
        noise += Mth.sin((float) (ny * 2.0 + nx)) * 0.5;
        noise += Mth.cos((float) (nx * 3.0 + nz * 2.5)) * 0.3;

        return noise;
    }

    private boolean isCrystalZone(int x, int y, int z) {
        return getCrystalZoneNoise(x, y, z) > CRYSTAL_ZONE_THRESHOLD;
    }

    // Get geode layer: 0=none, 1=outer (smooth basalt), 2=middle (calcite), 3=inner
    // (crystal)
    private int getGeodeLayer(int x, int y, int z) {
        double noise = getCrystalZoneNoise(x, y, z);
        if (noise <= CRYSTAL_ZONE_THRESHOLD - 0.12)
            return 0; // Not in geode
        if (noise <= CRYSTAL_ZONE_THRESHOLD - 0.06)
            return 1; // Outer layer - smooth basalt
        if (noise <= CRYSTAL_ZONE_THRESHOLD)
            return 2; // Middle layer - calcite
        return 3; // Inner - crystal blocks
    }

    // Determine which crystal type (0 = amethyst, 1 = mana crystal)
    private int getCrystalType(int x, int y, int z) {
        double noise = Mth.sin((float) ((x + seedOffsetX) * 0.1)) * Mth.cos((float) ((z + seedOffsetZ) * 0.1));
        noise += Mth.sin((float) (y * 0.15)) * 0.5;
        return noise > 0 ? 1 : 0; // 1 = mana, 0 = amethyst
    }

    // Check if cluster should spawn with more randomness
    private boolean shouldPlaceCluster(int x, int y, int z, long seed) {
        // Use more varied hash with multiple primes and XOR for less linear patterns
        long hash = ((x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L) ^ seed);
        hash = (hash * 31 + 17) ^ (hash >> 16);
        hash = hash & 0xFFFFFFFFL;
        return (hash % 100) < 4; // ~4% chance for clusters on exposed faces
    }

    // Single consistent lava lake level for the entire dimension
    private static final int LAVA_LAKE_LEVEL = MIN_Y + 30; // Y = -98

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState,
            BiomeManager biomeManager, StructureManager structureManager,
            ChunkAccess chunk, GenerationStep.Carving step) {
        if (step == GenerationStep.Carving.AIR || step == GenerationStep.Carving.LIQUID) {
            // Manual Carver Application Logic (since super.applyCarvers is abstract)
            try {
                OririMod.LOGGER.info("DEBUG_TRACE: applyCarvers started for step " + step);
                // Manual Carver Application Logic
                // We cannot instantiate a valid CarvingContext because we don't have a
                // NoiseBasedChunkGenerator.
                // Our custom carver (ScarletCaveEntranceCarver) is designed to handle null
                // context.
                net.minecraft.world.level.levelgen.carver.CarvingContext context = null;

                OririMod.LOGGER.info("DEBUG_TRACE: Context created");

                net.minecraft.world.level.chunk.CarvingMask carvingMask = ((net.minecraft.world.level.chunk.ProtoChunk) chunk)
                        .getOrCreateCarvingMask(step);
                net.minecraft.world.level.levelgen.Aquifer aquifer = null; // ScarletCaveEntranceCarver doesn't use
                                                                           // aquifer

                net.minecraft.core.BlockPos centerPos = chunk.getPos().getMiddleBlockPosition(0);
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(centerPos);
                OririMod.LOGGER.info("DEBUG_TRACE: Got biome "
                        + biome.unwrapKey().map(k -> k.location().toString()).orElse("unknown"));

                net.minecraft.world.level.biome.BiomeGenerationSettings settings = biome.value()
                        .getGenerationSettings();

                for (net.minecraft.core.Holder<net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?>> holder : settings
                        .getCarvers(step)) {
                    net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?> carver = holder.value();
                    OririMod.LOGGER.info("DEBUG_TRACE: Carving with "
                            + holder.unwrapKey().map(k -> k.location().toString()).orElse("unknown"));

                    // Create a deterministic random source for this chunk's carver
                    net.minecraft.util.RandomSource carverRandom = net.minecraft.util.RandomSource
                            .create(seed ^ chunk.getPos().toLong());
                    if (carver.worldCarver() instanceof ScarletCaveEntranceCarver
                            && carver.isStartChunk(carverRandom)) {
                        carver.carve(context, chunk, biomeManager::getBiome, carverRandom, aquifer, chunk.getPos(),
                                carvingMask);
                        OririMod.LOGGER.info("DEBUG_TRACE: Finished carving with "
                                + holder.unwrapKey().map(k -> k.location().toString()).orElse("unknown"));
                    } else if (!(carver.worldCarver() instanceof ScarletCaveEntranceCarver)) {
                        OririMod.LOGGER.warn("Skipping incompatible carver "
                                + holder.unwrapKey().map(k -> k.location().toString()).orElse("unknown")
                                + " in ElderwoodsChunkGenerator");
                    }
                }
            } catch (Throwable e) {
                OririMod.LOGGER.error("CRITICAL ERROR in applyCarvers: " + e.getMessage(), e);
                e.printStackTrace();
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

                // First pass: Remove ALL water and lava blocks in the entire column
                for (int y = MIN_Y; y <= surfaceY; y++) {
                    pos.set(worldX, y, worldZ);
                    BlockState currentBlock = chunk.getBlockState(pos);
                    if (currentBlock.is(Blocks.WATER) || currentBlock.is(Blocks.LAVA)) {
                        chunk.setBlockState(pos, STONE, false);
                    }
                }

                // Second pass: Carve caves and place lava lakes
                for (int y = MIN_Y + 6; y <= surfaceY; y++) {
                    pos.set(worldX, y, worldZ);
                    BlockState currentBlock = chunk.getBlockState(pos);

                    // Swap mineshaft wood blocks to elder wood
                    if (currentBlock.is(Blocks.OAK_PLANKS) || currentBlock.is(Blocks.DARK_OAK_PLANKS)
                            || currentBlock.is(Blocks.SPRUCE_PLANKS)) {
                        chunk.setBlockState(pos, ModBlocks.ELDER_PLANKS.get().defaultBlockState(), false);
                    } else if (currentBlock.is(Blocks.OAK_FENCE) || currentBlock.is(Blocks.DARK_OAK_FENCE)
                            || currentBlock.is(Blocks.SPRUCE_FENCE)) {
                        chunk.setBlockState(pos, ModBlocks.ELDER_FENCE.get().defaultBlockState(), false);
                    }

                    /*
                     * // === LEGACY CAVE GENERATION (DISABLED) ===
                     * // Replaced by Giant Room Walkers.
                     * // This block used the old 'isCave' method which is now renamed to
                     * 'isCaveLegacy'.
                     * 
                     * if (isCaveLegacy(worldX, y, worldZ, surfaceY)) {
                     * // Lava at consistent level in caves
                     * if (y <= LAVA_LAKE_LEVEL) {
                     * chunk.setBlockState(pos, Blocks.LAVA.defaultBlockState(), false);
                     * } else {
                     * chunk.setBlockState(pos, AIR, false);
                     * }
                     * }
                     */
                }
            }
        }

        // Manual Carver Application
        // Manual Carver Application
        BlockPos center = chunk.getPos().getMiddleBlockPosition(0);

        // 3x3 Neighbor Check: Carve caves from this chunk AND neighbors into this chunk
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                int originX = chunkX + dx;
                int originZ = chunkZ + dz;
                carveScarletCaveEntrances(level, chunk, originX, originZ, randomState);
            }
        }

        // Crystal Cluster Placement - Option 4: Avoid chunk borders
        // Only place clusters 2+ blocks from chunk edges to prevent floating from
        // adjacent chunk carving. Interior blocks (localX 2-13, localZ 2-13) are safe.
        BlockPos.MutableBlockPos clusterPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos adjacentPos = new BlockPos.MutableBlockPos();

        // Only process interior of chunk (2 blocks away from edges)
        for (int localX = 2; localX <= 13; localX++) {
            for (int localZ = 2; localZ <= 13; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                for (int y = MIN_Y + 6; y <= surfaceY && y <= CRYSTAL_CAVE_Y_THRESHOLD; y++) {
                    clusterPos.set(worldX, y, worldZ);
                    BlockState currentBlock = chunk.getBlockState(clusterPos);

                    // Only process crystal blocks
                    boolean isCrystalBlock = currentBlock.is(Blocks.AMETHYST_BLOCK) ||
                            currentBlock.is(ModBlocks.MANA_CRYSTAL_BLOCK.get());
                    if (!isCrystalBlock)
                        continue;

                    // Random check - ~4% chance per crystal block
                    if (!shouldPlaceCluster(worldX, y, worldZ, seed))
                        continue;

                    // Get crystal type from the block we're attaching to
                    int crystalType = currentBlock.is(ModBlocks.MANA_CRYSTAL_BLOCK.get()) ? 1 : 0;

                    // Check each face for air and place cluster at FIRST available face only
                    // UP
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

                    // DOWN
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

                    // NORTH
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

                    // SOUTH
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

                    // WEST
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

                    // EAST
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

        // Fluorite Generation (Crystal Caves Only)
        // Iterate interior of chunk with a slightly expanded range to allow patterns to
        // flow naturally
        // Create a deterministic random source for Fluorite generation
        RandomSource fluoriteRandom = RandomSource.create(seed ^ chunk.getPos().toLong());
        // Use a different seed offset for fluorite noise so it doesn't align with caves
        double fluoriteNoiseOffset = seedOffsetCave + 12345.0;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                // Fluorite cluster patch noise - lower frequency for larger natural patches
                // Using 0.02 frequency creates patches ~50 blocks wide
                double patchNoise = Math.sin((worldX + fluoriteNoiseOffset) * 0.02)
                        * Math.cos((worldZ + fluoriteNoiseOffset) * 0.02);
                // Secondary noise to break it up and make the edges irregular
                double detailNoise = Math.sin((worldX - fluoriteNoiseOffset) * 0.07)
                        * Math.cos((worldZ - fluoriteNoiseOffset) * 0.07);

                // Combine noises: main patch shape + irregular details
                double combinedNoise = patchNoise * 0.7 + detailNoise * 0.3;

                // Threshold for patch: > 0.3 means we are inside a patch
                if (combinedNoise < 0.3)
                    continue;

                for (int y = MIN_Y + 6; y <= surfaceY - 5; y++) {
                    clusterPos.set(worldX, y, worldZ);

                    // 1. Ceiling Check (Fluorite Cluster hanging)
                    // Condition: Air here, Solid above
                    if (chunk.getBlockState(clusterPos).isAir()) {
                        adjacentPos.set(worldX, y + 1, worldZ);
                        BlockState aboveState = chunk.getBlockState(adjacentPos);
                        if (aboveState.isSolid() && !aboveState.is(Blocks.BEDROCK)) {
                            // Check Biome at this specific position
                            if (isCrystalCaveBiome(getComputedBiome(level, worldX, y, worldZ))) {

                                // Base chance inside a patch, higher towards the center of the patch
                                float patchIntensity = (float) ((combinedNoise - 0.3) / 0.7); // 0.0 at edge, 1.0 at
                                                                                              // center
                                float chance = 0.05f + (patchIntensity * 0.25f); // 5% to 30% chance based on patch
                                                                                 // density

                                if (fluoriteRandom.nextFloat() < chance) {
                                    // Tip: Fluorite Cluster directly attached to the ceiling
                                    chunk.setBlockState(clusterPos,
                                            ModBlocks.FLUORITE_CLUSTER.get().defaultBlockState()
                                                    .setValue(BlockStateProperties.FACING, Direction.DOWN),
                                            false);
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
        // Optimization: Grid-Based Distribution (1 cave per 14x14 chunk grid = ~1/196
        // chunks)
        // Slightly rarer to account for massive worm caves
        int gridSize = 14;
        int gridX = Math.floorDiv(originX, gridSize);
        int gridZ = Math.floorDiv(originZ, gridSize);

        // Deterministically pick ONE chunk within this grid cell
        long gridSeed = level.getSeed() ^ (gridX * 341873128712L) ^ (gridZ * 132897987541L);
        RandomSource gridRandom = RandomSource.create(gridSeed);
        int pickX = gridX * gridSize + gridRandom.nextInt(gridSize);
        int pickZ = gridZ * gridSize + gridRandom.nextInt(gridSize);

        // Only carve if the current origin matches the picked chunk
        if (originX != pickX || originZ != pickZ)
            return;

        // Use a stable random for the cave shape itself
        long caveSeed = level.getSeed() ^ (originX * 341873128712L) ^ (originZ * 132897987541L);
        RandomSource random = RandomSource.create(caveSeed);

        // Calculate center of the ORIGIN chunk
        int centerX = (originX << 4) + 8;
        int centerZ = (originZ << 4) + 8;

        // Get surface height at the ORIGIN center
        int surfaceY = getSurfaceHeight(centerX, centerZ);
        if (surfaceY < -64)
            return;

        // Only do block-reading checks (surface type, liquid) when the center is in
        // THIS chunk.
        // For cross-chunk origins, the checks already passed when the origin's own
        // chunk was processed.
        boolean centerInThisChunk = (centerX >> 4) == chunk.getPos().x && (centerZ >> 4) == chunk.getPos().z;

        if (centerInThisChunk) {
            // Only carve entrances on grass/dirt surfaces
            BlockPos.MutableBlockPos surfaceCheckPos = new BlockPos.MutableBlockPos(centerX, surfaceY, centerZ);
            BlockState surfaceBlock = chunk.getBlockState(surfaceCheckPos);
            if (!surfaceBlock.is(Blocks.GRASS_BLOCK) && !surfaceBlock.is(Blocks.DIRT)
                    && !surfaceBlock.is(ModBlocks.SCARLET_GRASS_BLOCK.get())) {
                return;
            }

            // Check for liquids (Water, Lava, etc.) in the entrance area to avoid cutting
            // into ponds
            int checkRadius = 8;
            BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
            for (int cx = -checkRadius; cx <= checkRadius; cx++) {
                for (int cz = -checkRadius; cz <= checkRadius; cz++) {
                    checkPos.set(centerX + cx, surfaceY, centerZ + cz);
                    if (chunk.getBlockState(checkPos)
                            .getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                        // Abort if liquid found
                        return;
                    }
                    // Check one block below
                    checkPos.set(centerX + cx, surfaceY - 1, centerZ + cz);
                    if (chunk.getBlockState(checkPos)
                            .getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                        return;
                    }
                }
            }
        }

        // WORM STARTS HERE
        // Initial Position
        double x = centerX;
        double y = surfaceY + 2; // Start slightly above ground
        double z = centerZ;

        // Initial Velocity / Direction
        // Yaw: Random direction (0-360)
        float yaw = random.nextFloat() * (float) Math.PI * 2.0f;
        // Pitch: Pointing DOWN (-45 to -90 degrees) to ensure we dig into the ground
        float pitch = -((float) Math.PI / 4.0f) - (random.nextFloat() * (float) Math.PI / 4.0f);

        // Initial Size
        float radius = 3.5f + random.nextFloat() * 2.0f; // 3.5 to 5.5

        // EXPLICIT ENTRANCE OPENING:
        // Carve a tapered funnel at the surface to guarantee the entrance is always
        // open.
        // Radius shrinks as we go deeper for a smooth, natural look.
        {
            int maxOpeningRadius = (int) radius + 1;
            int openingTop = surfaceY + 3;
            int openingBottom = surfaceY - 5;
            int openingHeight = openingTop - openingBottom;
            BlockPos.MutableBlockPos openPos = new BlockPos.MutableBlockPos();
            for (int oy = openingTop; oy >= openingBottom; oy--) {
                // Radius tapers from full at the top to ~1/3 at the bottom
                double taperProgress = (double) (openingTop - oy) / openingHeight;
                double currentRadius = Mth.lerp(taperProgress, maxOpeningRadius, maxOpeningRadius * 0.33);
                double radiusSq = currentRadius * currentRadius;
                int checkRad = (int) Math.ceil(currentRadius);
                for (int ox = -checkRad; ox <= checkRad; ox++) {
                    for (int oz = -checkRad; oz <= checkRad; oz++) {
                        if (ox * ox + oz * oz > radiusSq)
                            continue;
                        int worldX = centerX + ox;
                        int worldZ = centerZ + oz;
                        // Only carve blocks within THIS chunk
                        if (chunk.getPos().x != (worldX >> 4) || chunk.getPos().z != (worldZ >> 4))
                            continue;
                        openPos.set(worldX, oy, worldZ);
                        BlockState openState = chunk.getBlockState(openPos);
                        if (!openState.is(Blocks.BEDROCK) && !openState.isAir()) {
                            chunk.setBlockState(openPos, Blocks.AIR.defaultBlockState(), false);
                        }
                    }
                }
            }
        }

        // Recursively carve the worm
        // Max steps ~150 (enough to go deep and wind around)
        // shouldProtectSurface is FALSE for the entrance worm to allow it to break
        // through the surface
        carveWorm(level, chunk, random, x, y, z, yaw, pitch, radius, 150, 0, 1.0f, false);

        // EXTRA: Deep Underground Worms (No surface connection)
        // Spawn 8-16 extra worms deep down per grid cell, spread out significantly
        int numDeepWorms = 12 + random.nextInt(7); // 12 to 18
        for (int i = 0; i < numDeepWorms; i++) {
            // Spread over +/- 80 blocks (5 chunks) to cover the grid area
            double dX = centerX + (random.nextInt(160) - 80);
            double dZ = centerZ + (random.nextInt(160) - 80);
            double dY = random.nextInt(118) - 58; // Y -58 to 60

            float dYaw = random.nextFloat() * (float) Math.PI * 2.0f;
            float dPitch = (random.nextFloat() - 0.5f) * (float) Math.PI; // Any direction
            float dRadius = 3.0f + random.nextFloat() * 3.0f;

            // shouldProtectSurface is TRUE for deep worms to avoid accidental surface holes
            carveWorm(level, chunk, random, dX, dY, dZ, dYaw, dPitch, dRadius, 120, 0, 1.0f, true);
        }

        // TITAN CAVE ROOMS (Replaces Cheese Caves and Giant Worms)
        // Spawn 1 TITAN cavern worm per grid (50% chance)
        // These are massive, flattened worms that create vast underground halls.
        if (random.nextFloat() < 0.33f) { // 33% chance per grid
            double dX = centerX + (random.nextInt(40) - 20);
            double dZ = centerZ + (random.nextInt(40) - 20);
            double dY = random.nextInt(100) - 50; // Deep underground (-50 to 50)

            float dYaw = random.nextFloat() * (float) Math.PI * 2.0f;
            float dPitch = (random.nextFloat() - 0.5f) * 0.2f; // Very flat pitch
            // Radius 35-70 (Diameter 70-140)
            float dRadius = 35.0f + random.nextFloat() * 35.0f;

            // Run for fewer steps (creating a "Hall"), flattened vertically (0.6 scale -
            // Taller)
            // shouldProtectSurface is TRUE for Titan worms to avoid accidental surface
            // holes
            carveWorm(level, chunk, random, dX, dY, dZ, dYaw, dPitch, dRadius, 60, 0, 0.6f, true);
        }
    }

    private void carveWorm(WorldGenRegion level, ChunkAccess chunk, RandomSource random,
            double x, double y, double z, float yaw, float pitch, float radius, int steps, int branchDepth,
            float yScale, boolean shouldProtectSurface) {

        if (steps <= 0)
            return;
        if (branchDepth > 2)
            return; // Prevent infinite recursion

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        // Removed rimBlockState and rimUnderBlockState

        int originalSurfaceY = getSurfaceHeight((int) x, (int) z);

        for (int i = 0; i < steps; i++) {
            // 1. Move
            double hDist = Math.cos(pitch);
            double dx = Math.cos(yaw) * hDist;
            double dy = Math.sin(pitch);
            double dz = Math.sin(yaw) * hDist;

            x += dx;
            y += dy;
            z += dz;

            // 2. Adjust Direction (Simulate Brownian Motion / Noise)
            yaw += (random.nextFloat() - 0.5f) * 0.4f; // Turn
            pitch += (random.nextFloat() - 0.5f) * 0.2f; // Slight pitch change

            // Flatten pitch if we are deep, to make tunnels instead of just a pit
            if (y < originalSurfaceY - 20) {
                pitch *= 0.9f;
                // If pitch gets too flat near bedrock, maybe angle up/down slightly
                if (Math.abs(pitch) < 0.1f && random.nextFloat() < 0.1f) {
                    pitch = (random.nextFloat() - 0.5f) * 0.5f;
                }
            }

            // Avoid going too high (back out of ground)
            if (y > originalSurfaceY - 5 && pitch > 0) {
                pitch = -0.5f; // Push back down
            }

            // Bedrock Limit
            if (y < -58) {
                pitch = 0.5f; // Push up
                y = -58;
            }

            // 4. Branching
            if (random.nextFloat() < 0.02f && steps > 20) { // 2% chance per step (Reduced from 5%)
                float branchYaw = yaw + (random.nextFloat() - 0.5f) * 2.0f; // Big turn
                float branchPitch = pitch + (random.nextFloat() - 0.5f) * 1.0f;
                carveWorm(level, chunk, random, x, y, z, branchYaw, branchPitch, radius * 0.8f, steps / 2,
                        branchDepth + 1, yScale, shouldProtectSurface);
            }

            // Vary Radius
            radius += (random.nextFloat() - 0.5f) * 0.2f;
            radius = Mth.clamp(radius, 0.5f, 80.0f); // Allow HYPER-TITAN worms (up to 80 radius = 160 width)

            // Random Room Swellings (Cave Rooms along the tunnel)
            // 0.05% chance per step to swell into a room (Much RAREr)
            // Use irregular shape instead of perfect sphere
            if (random.nextFloat() < 0.0005f && steps > 10) {
                carveIrregularRoom(level, chunk, random, x, y, z, shouldProtectSurface);
            }

            // 3. Carve Sphere
            // Only affect blocks within the CURRENT chunk to allow multithreading safety
            // (We iterate radius + padding)
            int checkRad = (int) radius + 2;
            int minX = Mth.floor(x - checkRad);
            int maxX = Mth.floor(x + checkRad);
            int minY = Mth.floor(y - checkRad);
            int maxY = Mth.floor(y + checkRad);
            int minZ = Mth.floor(z - checkRad);
            int maxZ = Mth.floor(z + checkRad);

            // Optimization: Quick check if this sphere even touches the chunk
            if (minX > chunk.getPos().getMaxBlockX() || maxX < chunk.getPos().getMinBlockX() ||
                    minZ > chunk.getPos().getMaxBlockZ() || maxZ < chunk.getPos().getMinBlockZ()) {
                // Continue moving worm simulation, but don't carve blocks
                // Branching Logic still runs!
            } else {
                // Carve blocks
                for (int bx = minX; bx <= maxX; bx++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        // Must be in THIS chunk
                        if (chunk.getPos().x != (bx >> 4) || chunk.getPos().z != (bz >> 4))
                            continue;

                        for (int by = minY; by <= maxY; by++) {
                            mutablePos.set(bx, by, bz);
                            double dX = x - bx;
                            double dY = (y - by) / yScale; // Apply vertical scale (flattening)
                            double dZ = z - bz;
                            // Ellipsoid stretch logic
                            if (dX * dX + dY * dY + dZ * dZ < radius * radius) {
                                // Surface protection: never carve within 4 blocks of the local surface if flag
                                // is set
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
        int blobs = 3 + random.nextInt(4); // 3-6 overlapping blobs
        for (int i = 0; i < blobs; i++) {
            float r = 3.0f + random.nextFloat() * 6.0f; // 3-9 radius
            // Offset blobs to create irregular shape
            double offX = (random.nextFloat() - 0.5f) * r * 1.5f;
            double offY = (random.nextFloat() - 0.5f) * (r * 0.8f); // Slightly flatter vertically
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

        if (minX > chunk.getPos().getMaxBlockX() || maxX < chunk.getPos().getMinBlockX() ||
                minZ > chunk.getPos().getMaxBlockZ() || maxZ < chunk.getPos().getMinBlockZ()) {
            return;
        }

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                if (chunk.getPos().x != (bx >> 4) || chunk.getPos().z != (bz >> 4))
                    continue;
                for (int by = minY_block; by <= maxY_block; by++) {
                    mutablePos.set(bx, by, bz);
                    double dX = x - bx;
                    double dY = y - by;
                    double dZ = z - bz;
                    if (dX * dX + dY * dY + dZ * dZ < radius * radius) {
                        // Surface protection: never carve within 4 blocks of the local surface if flag
                        // is set
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

    // Manual Biome Selection Logic (Bypassing broken RandomState/BiomeSource)
    private Holder<Biome> getComputedBiome(WorldGenRegion level, int x, int y, int z) {
        // Initialize if needed (though initSeed should have run)
        if (!seedInitialized)
            initSeed(level.getSeed());

        double nx = x + seedOffsetX;
        double nz = z + seedOffsetZ;

        // 1. Surface Noise (Temperature/Humidity equivalent)
        // Scale: Large regions (e.g. 500-1000 blocks)
        double surfaceNoise = Math.sin(nx * 0.002) * Math.cos(nz * 0.003) +
                0.5 * Math.cos(nx * 0.005 + 2.0) * Math.sin(nz * 0.005 + 1.0);

        // 2. Cave Noise (Weirdness equivalent)
        // Scale: Medium regions (e.g. 200 blocks)
        double caveNoise = Math.sin(nx * 0.01 + seedOffsetCave) * Math.cos(nz * 0.01 + seedOffsetCave);

        // Determine surface height at this XZ position for depth-relative biome
        // transition
        int surfaceY = getSurfaceHeight(x, z);
        // Surface biomes only apply within 4 blocks of the surface (grass + dirt layer)
        // Below that = cave biomes
        boolean isBelowSurfaceLayer = y < surfaceY - 4;

        // Biome Logic
        if (isBelowSurfaceLayer) {
            // Underground: Cave Biomes
            // Check cave noise to split between Scarlet Caves, Elderwoods Cave, and Crystal
            // Caves
            if (caveNoise > 0.3) {
                return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(SCARLET_CAVES_KEY);
            } else if (caveNoise < -0.3) {
                // Elderwoods Cave (with its own particles/effects)
                return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                        ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods_cave")));
            } else {
                // Crystal Caves
                return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                        ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "crystal_caves")));
            }
        } else {
            // Surface: Forest vs Plains vs Standard
            if (surfaceNoise > 0.5) {
                return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(SCARLET_FOREST_KEY);
            } else if (surfaceNoise < -0.5) {
                return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(SCARLET_PLAINS_KEY);
            } else {
                // Standard Elderwoods
                return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                        ResourceKey.create(Registries.BIOME,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods")));
            }
        }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
            RandomState random, ChunkAccess chunk) {
        initSeed(level.getSeed());

        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        RandomSource bedrockRandom = RandomSource.create(level.getSeed() + chunk.getPos().toLong());

        // POPULATE BIOMES MANUALLY IN THE CHUNK
        // This ensures F3, fog, particles, and structure generation see the correct
        // biomes
        chunk.fillBiomesFromNoise(
                (x, y, z, sampler) -> getComputedBiome(level, x * 4, y * 4, z * 4),
                random.sampler());

        // SURFACE BUILDING LOGIC
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                // Get biome at SURFACE level for heightmaps and top layer
                Holder<Biome> surfaceBiomeHolder = getComputedBiome(level, worldX, surfaceY, worldZ);
                boolean isSurfaceScarlet = isScarletBiome(surfaceBiomeHolder);
                BlockState surfaceGrass = isSurfaceScarlet ? SCARLET_GRASS : GRASS;

                for (int y = MIN_Y; y <= surfaceY; y++) {
                    mutablePos.set(worldX, y, worldZ);

                    // Get biome at current Y level
                    Holder<Biome> biomeHolder = getComputedBiome(level, worldX, y, worldZ);
                    boolean scarletBiome = isScarletBiome(biomeHolder);
                    BlockState stoneBlock = scarletBiome ? SCARLET_STONE : STONE;
                    BlockState deepslateBlock = scarletBiome ? SCARLET_DEEPSLATE : DEEPSLATE;
                    BlockState currentGrass = scarletBiome ? SCARLET_GRASS : GRASS;

                    BlockState blockState;
                    if (y <= MIN_Y + 4) {
                        blockState = (y == MIN_Y || bedrockRandom.nextFloat() < (1.0f - (y - MIN_Y) * 0.2f))
                                ? BEDROCK
                                : deepslateBlock;
                    } else if (y < 0) {
                        // Check for geode layers and place appropriate blocks
                        // Check for geode layers and place appropriate blocks
                        // GEODES ARE DISABLED IN SCARLET BIOMES AND RESTRICTED TO CRYSTAL CAVES
                        if (!scarletBiome && isCrystalCaveBiome(biomeHolder)) {
                            int geodeLayer = getGeodeLayer(worldX, y, worldZ);
                            if (geodeLayer == 3) {
                                // Inner - crystal blocks
                                int crystalType = getCrystalType(worldX, y, worldZ);
                                blockState = crystalType == 1
                                        ? ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState()
                                        : Blocks.AMETHYST_BLOCK.defaultBlockState();
                            } else if (geodeLayer == 2) {
                                // Middle - calcite
                                blockState = Blocks.CALCITE.defaultBlockState();
                            } else if (geodeLayer == 1) {
                                // Outer - smooth basalt
                                blockState = Blocks.SMOOTH_BASALT.defaultBlockState();
                            } else {
                                blockState = deepslateBlock;
                            }
                        } else {
                            // In Scarlet Biomes, just place deepslate (no geodes)
                            blockState = deepslateBlock;
                        }
                    } else if (y == surfaceY) {
                        blockState = currentGrass;
                    } else if (y >= surfaceY - 3) {
                        blockState = DIRT;
                    } else {
                        blockState = stoneBlock;
                    }

                    chunk.setBlockState(mutablePos, blockState, false);
                }

                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)
                        .update(localX, surfaceY, localZ, surfaceGrass);
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING)
                        .update(localX, surfaceY, localZ, surfaceGrass);
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)
                        .update(localX, surfaceY, localZ, surfaceGrass);
            }
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
            StructureManager structureManager, ChunkAccess chunk) {
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
        info.add("Cave Floor: Y=" + getCaveFloorHeight(pos.getX(), pos.getZ()));
        info.add("Cave Ceiling: Y=" + getCaveCeilingHeight(pos.getX(), pos.getZ(), surfaceY));

        // Biome Noise Debug
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
