package net.ganyusbathwater.oririmod.worldgen.carver;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Elysian Abyss Carver - The Skylight Ravine.
 *
 * Connects the surface to the Elysian Abyss using a spline-based winding ravine algorithm.
 * Safely loops over neighboring chunks internally to prevent the chunk-clipping square bug.
 */
public class ElysianAbyssCarver extends WorldCarver<CaveCarverConfiguration> {

    private static final ResourceKey<Biome> ELYSIAN_ABYSS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));
    private static final ResourceKey<Biome> SCARLET_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_caves"));
    private static final ResourceKey<Biome> SCARLET_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest"));
    private static final ResourceKey<Biome> SCARLET_SWAMP_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_swamp"));

    public static final ThreadLocal<net.minecraft.world.level.StructureManager> CURRENT_STRUCTURE_MANAGER = new ThreadLocal<>();
    public static final ThreadLocal<net.minecraft.core.RegistryAccess> CURRENT_REGISTRY_ACCESS = new ThreadLocal<>();
    public static final ThreadLocal<net.ganyusbathwater.oririmod.worldgen.ElderwoodsCaveAquifer> CURRENT_AQUIFER = new ThreadLocal<>();

    private static final int ORIGIN_CACHE_SIZE = 512;
    private static final int ORIGIN_CACHE_MASK = ORIGIN_CACHE_SIZE - 1;
    private static final ThreadLocal<OriginThreadState> THREAD_STATE = ThreadLocal.withInitial(OriginThreadState::new);

    private static final class OriginThreadState {
        long worldSeed;
        double seedOffsetCave = Double.NaN;
        final long[] originKeys = new long[ORIGIN_CACHE_SIZE];
        final byte[] originStatus = new byte[ORIGIN_CACHE_SIZE]; // 0 = empty, 1 = inactive, 2 = active
        final double[] colHumCache = new double[256];
    }

    public ElysianAbyssCarver(Codec<CaveCarverConfiguration> codec) {
        super(codec);
    }

    @Override
    public int getRange() {
        return 12;
    }

    @Override
    public boolean isStartChunk(CaveCarverConfiguration config, RandomSource random) {
        // Probability check is now handled per-origin inside carve() to guarantee cross-chunk sync
        return true; 
    }

    @Override
    public boolean carve(CarvingContext context, CaveCarverConfiguration config,
            ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeAccessor,
            RandomSource random, Aquifer aquifer, ChunkPos chunkPos,
            CarvingMask carvingMask) {

        int ox = chunkPos.x;
        int oz = chunkPos.z;
        if (!isOriginActive(ox, oz, biomeAccessor)) {
            return false;
        }

        OriginThreadState state = THREAD_STATE.get();
        double[] colHumCache = state.colHumCache;
        Arrays.fill(colHumCache, Double.NaN);

        long originSeed = (ox * 341873128712L) ^ (oz * 132897987541L);
        RandomSource originRandom = RandomSource.create(originSeed);

        boolean hit = false;
        // Spawn standard winding cave systems (Worms & Rooms)
        hit |= carveWorms(config, chunk, ox, oz, originRandom, biomeAccessor, colHumCache);

        // Spawn massive, localized Cheese Chambers
        hit |= carveCheeseChambers(config, chunk, ox, oz, originRandom, biomeAccessor, colHumCache);

        // Spawn erratic Noodle tunnels for vertical connections
        hit |= carveNoodles(config, chunk, ox, oz, originRandom, biomeAccessor, colHumCache);

        return hit;
    }

    private static boolean isOriginActive(int ox, int oz, Function<BlockPos, Holder<Biome>> biomeAccessor) {
        long worldSeed = net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator.lastSeed;
        double seedOffsetCave = net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator.currentSeedOffsetCave;
        OriginThreadState state = THREAD_STATE.get();
        if (state.worldSeed != worldSeed || Double.compare(state.seedOffsetCave, seedOffsetCave) != 0) {
            state.worldSeed = worldSeed;
            state.seedOffsetCave = seedOffsetCave;
            Arrays.fill(state.originStatus, (byte) 0);
        }

        long packed = ChunkPos.asLong(ox, oz);
        int slot = ((ox * 73856093) ^ (oz * 19349663)) & ORIGIN_CACHE_MASK;
        byte cached = state.originStatus[slot];
        if (cached != 0 && state.originKeys[slot] == packed) {
            return cached == 2;
        }

        boolean active = computeOriginActive(ox, oz, worldSeed, seedOffsetCave, biomeAccessor);
        state.originKeys[slot] = packed;
        state.originStatus[slot] = (byte) (active ? 2 : 1);
        return active;
    }

    private static boolean computeOriginActive(int ox, int oz, long worldSeed, double seedOffsetCave,
            Function<BlockPos, Holder<Biome>> biomeAccessor) {
        // --- Mathematical Abyss Detection (100% synchronized with ElderwoodsBiomeSource & ChunkGenerator) ---
        float scale = net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource.CAVE_NOISE_FREQUENCY;
        double caveNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                (float) ((ox * 16 + seedOffsetCave) * scale),
                (float) ((oz * 16 + seedOffsetCave) * scale),
                3);
        if (caveNoise <= 0.20) {
            return false;
        }

        // --- Global Structure Avoidance ---
        // Mathematically predict where structures will generate using deterministic RandomSpread math.
        // If the carver originates within 8 chunks of a potential Sanctuary, we abort it entirely!
        int spacing = 20;
        int separation = 15;
        int salt = 1234567890; // elderwoods_structures salt
        int regionX = Math.floorDiv(ox, spacing);
        int regionZ = Math.floorDiv(oz, spacing);

        for (int rx = -1; rx <= 1; rx++) {
            for (int rz = -1; rz <= 1; rz++) {
                int rX = regionX + rx;
                int rZ = regionZ + rz;

                long seed1 = worldSeed + (long) rX * 341873128712L + (long) rZ * 132897987541L + (long) salt;
                java.util.Random randomPos = new java.util.Random(seed1);

                int chunkX = rX * spacing + randomPos.nextInt(spacing - separation);
                int chunkZ = rZ * spacing + randomPos.nextInt(spacing - separation);

                int dx = ox - chunkX;
                int dz = oz - chunkZ;

                if (dx * dx + dz * dz < 64) { // 8 chunks radius squared = 64
                    return false; // Abort the entire carver for this origin
                }
            }
        }

        Holder<Biome> originBiome = biomeAccessor.apply(new BlockPos((ox << 4) + 8, 0, (oz << 4) + 8));
        return originBiome.is(ELYSIAN_ABYSS_KEY);
    }

    private static double getCachedColHum(double[] colHumCache, int bx, int bz) {
        int colIdx = ((bx & 15) << 4) | (bz & 15);
        double hum = colHumCache[colIdx];
        if (Double.isNaN(hum)) {
            hum = net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator.getScarletHumidityNoise(bx, bz);
            colHumCache[colIdx] = hum;
        }
        return hum;
    }

    private boolean carveWorms(CaveCarverConfiguration config, ChunkAccess chunk, int originX, int originZ, RandomSource random, Function<BlockPos, Holder<Biome>> biomeAccessor, double[] colHumCache) {
        boolean hit = false;
        
        // Decreased from 1/8 to 1/10
        if (random.nextFloat() > 1.0f / 10.0f) return false;
        
        // 2 to 5 branches per cave system for sprawling networks
        int numWorms = 2 + random.nextInt(4);
        for (int w = 0; w < numWorms; w++) {
            double x = (originX << 4) + 8 + random.nextInt(16);
            // Lowered max height from 140 (Y=80) down to 110 (Y=50) to heavily protect the surface!
            // Increased power bias slightly to 1.7 to keep them deep without destroying the void floor
            double y = -60 + (Math.pow(random.nextFloat(), 1.7f) * 110); 
            double z = (originZ << 4) + 8 + random.nextInt(16);
            
            float yaw = random.nextFloat() * (float) Math.PI * 2.0f;
            float pitch = (random.nextFloat() - 0.5f) * 0.5f; // Mostly horizontal
            
            float baseWidth = 2.0f + random.nextFloat() * 2.5f; // 2 to 4.5 blocks wide
            int length = 60 + random.nextInt(60);           // 60 to 120 blocks long
            
            float currentWidth = baseWidth;
            float targetWidth = baseWidth;
            int roomTimer = 0;
            
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            
            for (int step = 0; step < length; step++) {
                x += Math.cos(yaw) * Math.cos(pitch) * 1.5;
                y += Math.sin(pitch) * 1.5;
                z += Math.sin(yaw) * Math.cos(pitch) * 1.5;
                
                yaw += (random.nextFloat() - 0.5f) * 0.5f; 
                pitch += (random.nextFloat() - 0.5f) * 0.4f;
                pitch *= 0.9f; // Strongly dampen pitch so it stays mostly horizontal
                
                // Smoothly handle bulbous cave rooms
                if (roomTimer > 0) {
                    roomTimer--;
                    if (roomTimer == 0) {
                        targetWidth = baseWidth; // Start shrinking back down
                    }
                } else if (random.nextFloat() < 0.05f) {
                    targetWidth = baseWidth + 3.0f + random.nextFloat() * 4.0f;
                    roomTimer = 4 + random.nextInt(6); // Stay big for a few steps
                }
                
                // Interpolate width so it swells organically instead of instantly jumping
                currentWidth += (targetWidth - currentWidth) * 0.3f;
                
                // Taper the ends of the tunnel so they shrink naturally into the rock
                double stepProgress = (double) step / length;
                double endTaper = 1.0;
                if (stepProgress < 0.1) endTaper = stepProgress / 0.1;
                if (stepProgress > 0.9) endTaper = (1.0 - stepProgress) / 0.1;
                
                double rawWidth = currentWidth * endTaper;
                if (rawWidth < 0.5) continue;

                int chunkMinX = chunk.getPos().getMinBlockX();
                int chunkMaxX = chunk.getPos().getMaxBlockX();
                int chunkMinZ = chunk.getPos().getMinBlockZ();
                int chunkMaxZ = chunk.getPos().getMaxBlockZ();
                if (Mth.floor(x + rawWidth) < chunkMinX || Mth.floor(x - rawWidth) > chunkMaxX
                        || Mth.floor(z + rawWidth) < chunkMinZ || Mth.floor(z - rawWidth) > chunkMaxZ
                        || Mth.floor(y + rawWidth) < -96 || Mth.floor(y - rawWidth) >= 60) {
                    continue;
                }

                double finalWidth = applyScarletEdgeTaper(rawWidth, x, y, z, pos, biomeAccessor);
                if (finalWidth < 0.5) continue;

                int minX = Mth.floor(x - finalWidth);
                int maxX = Mth.floor(x + finalWidth);
                int minZ = Mth.floor(z - finalWidth);
                int maxZ = Mth.floor(z + finalWidth);
                int minY = Math.max(-96, Mth.floor(y - finalWidth));
                int maxY = Math.min(59, Mth.floor(y + finalWidth));
                double finalWidthSq = finalWidth * finalWidth;

                for (int bx = Math.max(minX, chunkMinX); bx <= Math.min(maxX, chunkMaxX); bx++) {
                    for (int bz = Math.max(minZ, chunkMinZ); bz <= Math.min(maxZ, chunkMaxZ); bz++) {
                        double dx = x - bx;
                        double dz = z - bz;
                        double horizSq = dx * dx + dz * dz;
                        if (horizSq >= finalWidthSq) {
                            continue;
                        }
                        for (int by = minY; by <= maxY; by++) {
                            double dy = y - by;
                            if (horizSq + dy * dy < finalWidthSq) {
                                pos.set(bx, by, bz);
                                if (tryCarveSafeCaveAir(config, chunk, pos, bx, by, bz, colHumCache, biomeAccessor)) {
                                    hit = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return hit;
    }

    private boolean carveCheeseChambers(CaveCarverConfiguration config, ChunkAccess chunk, int originX, int originZ, RandomSource random, Function<BlockPos, Holder<Biome>> biomeAccessor, double[] colHumCache) {
        boolean hit = false;
        
        // Decreased from 1/45 to 1/50
        if (random.nextFloat() > 1.0f / 50.0f) return false;
        
        double x = (originX << 4) + 8 + random.nextInt(16);
        // Lowered max height (max Y=30)
        double y = -50 + (Math.pow(random.nextFloat(), 1.7f) * 80); 
        double z = (originZ << 4) + 8 + random.nextInt(16);
        
        // Radius between 15 and 28 blocks (massive room)
        float maxRadius = 15.0f + random.nextFloat() * 13.0f;
        
        int minX = Mth.floor(x - maxRadius);
        int maxX = Mth.floor(x + maxRadius);
        int minZ = Mth.floor(z - maxRadius);
        int maxZ = Mth.floor(z + maxRadius);
        int minY = Math.max(-96, Mth.floor(y - maxRadius));
        int maxY = Math.min(59, Mth.floor(y + maxRadius));
        if (minY > maxY) {
            return false;
        }
        
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();
        
        // Safe bounding box check
        if (maxX >= chunkMinX && minX <= chunkMaxX && maxZ >= chunkMinZ && minZ <= chunkMaxZ) {
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int bx = Math.max(minX, chunkMinX); bx <= Math.min(maxX, chunkMaxX); bx++) {
                for (int bz = Math.max(minZ, chunkMinZ); bz <= Math.min(maxZ, chunkMaxZ); bz++) {
                    double colHum = getCachedColHum(colHumCache, bx, bz);
                    double scarletPinch = 0.0;
                    if (colHum > 0.32) {
                        Holder<Biome> colBiome = biomeAccessor.apply(mutablePos.set(bx, 0, bz));
                        if (!colBiome.is(ELYSIAN_ABYSS_KEY)) {
                            double pt = Mth.clamp((colHum - 0.32) / 0.11, 0.0, 1.0);
                            scarletPinch = pt * pt * (3.0 - 2.0 * pt) * 1.4;
                        }
                    }
                    if (scarletPinch >= 1.35) {
                        continue;
                    }
                    for (int by = minY; by <= maxY; by++) {
                        double dx = x - bx;
                        double dy = y - by;
                        double dz = z - bz;

                        // Squish the Y axis slightly so chambers are wider than they are tall
                        double distanceSq = dx * dx + (dy * dy * 1.5) + dz * dz;

                        if (distanceSq < maxRadius * maxRadius) {

                            // High frequency 3D noise to create massive pillars, stalagmites, and rough floors
                            double noise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                (float)bx * 0.05f, 
                                (float)by * 0.05f, 
                                (float)bz * 0.05f, 
                                3
                            );

                            // Smoothly taper the noise effect toward the edges of the room and before Scarlet borders
                            double normalizedDist = Math.sqrt(distanceSq) / maxRadius;
                            double noiseThreshold = 0.3 - (normalizedDist * 0.2) - scarletPinch;

                            // If noise is very high, leave it as solid rock (creates pillars)
                            if (noise > noiseThreshold) continue;

                            mutablePos.set(bx, by, bz);
                            if (tryCarveSafeCaveAir(config, chunk, mutablePos, bx, by, bz, colHumCache, biomeAccessor)) {
                                hit = true;
                            }
                        }
                    }
                }
            }
        }
        return hit;
    }

    private boolean carveNoodles(CaveCarverConfiguration config, ChunkAccess chunk, int originX, int originZ, RandomSource random, Function<BlockPos, Holder<Biome>> biomeAccessor, double[] colHumCache) {
        boolean hit = false;
        
        // Decreased from 1/25 to 1/30
        if (random.nextFloat() > 1.0f / 30.0f) return false;
        
        int numNoodles = 3 + random.nextInt(5);
        for (int w = 0; w < numNoodles; w++) {
            double x = (originX << 4) + 8 + random.nextInt(16);
            // Lowered max height
            double y = -60 + (Math.pow(random.nextFloat(), 1.7f) * 110); 
            double z = (originZ << 4) + 8 + random.nextInt(16);
            
            float yaw = random.nextFloat() * (float) Math.PI * 2.0f;
            float pitch = (random.nextFloat() - 0.5f) * 2.0f; // Can go straight up or down!
            
            float width = 1.0f + random.nextFloat() * 1.5f; // Very thin, 1 to 2.5 blocks wide
            int length = 80 + random.nextInt(60);           // Long and spindly
            
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            
            for (int step = 0; step < length; step++) {
                x += Math.cos(yaw) * Math.cos(pitch) * 1.5;
                y += Math.sin(pitch) * 1.5;
                z += Math.sin(yaw) * Math.cos(pitch) * 1.5;
                
                // Highly erratic direction changes
                yaw += (random.nextFloat() - 0.5f) * 1.2f; 
                pitch += (random.nextFloat() - 0.5f) * 1.2f;
                // Weak damping allows it to spiral and twist
                pitch *= 0.95f; 
                
                double stepProgress = (double) step / length;
                double endTaper = 1.0;
                if (stepProgress < 0.1) endTaper = stepProgress / 0.1;
                if (stepProgress > 0.9) endTaper = (1.0 - stepProgress) / 0.1;
                
                double rawWidth = width * endTaper;
                if (rawWidth < 0.5) continue;

                int chunkMinX = chunk.getPos().getMinBlockX();
                int chunkMaxX = chunk.getPos().getMaxBlockX();
                int chunkMinZ = chunk.getPos().getMinBlockZ();
                int chunkMaxZ = chunk.getPos().getMaxBlockZ();
                if (Mth.floor(x + rawWidth) < chunkMinX || Mth.floor(x - rawWidth) > chunkMaxX
                        || Mth.floor(z + rawWidth) < chunkMinZ || Mth.floor(z - rawWidth) > chunkMaxZ
                        || Mth.floor(y + rawWidth) < -96 || Mth.floor(y - rawWidth) >= 60) {
                    continue;
                }

                double finalWidth = applyScarletEdgeTaper(rawWidth, x, y, z, pos, biomeAccessor);
                if (finalWidth < 0.5) continue;

                int minX = Mth.floor(x - finalWidth);
                int maxX = Mth.floor(x + finalWidth);
                int minZ = Mth.floor(z - finalWidth);
                int maxZ = Mth.floor(z + finalWidth);
                int minY = Math.max(-96, Mth.floor(y - finalWidth));
                int maxY = Math.min(59, Mth.floor(y + finalWidth));
                double finalWidthSq = finalWidth * finalWidth;

                for (int bx = Math.max(minX, chunkMinX); bx <= Math.min(maxX, chunkMaxX); bx++) {
                    for (int bz = Math.max(minZ, chunkMinZ); bz <= Math.min(maxZ, chunkMaxZ); bz++) {
                        double dx = x - bx;
                        double dz = z - bz;
                        double horizSq = dx * dx + dz * dz;
                        if (horizSq >= finalWidthSq) {
                            continue;
                        }
                        for (int by = minY; by <= maxY; by++) {
                            double dy = y - by;
                            if (horizSq + dy * dy < finalWidthSq) {
                                pos.set(bx, by, bz);
                                if (tryCarveSafeCaveAir(config, chunk, pos, bx, by, bz, colHumCache, biomeAccessor)) {
                                    hit = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return hit;
    }

    /** Tapers carver tunnel radius down smoothly inside grey rock before reaching Scarlet Caves. */
    private static double applyScarletEdgeTaper(double width, double x, double y, double z,
            BlockPos.MutableBlockPos scratchPos, Function<BlockPos, Holder<Biome>> biomeAccessor) {
        if (width < 0.5) {
            return width;
        }
        int sx = Mth.floor(x);
        int sz = Mth.floor(z);
        double stepHum = net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator.getScarletHumidityNoise(sx, sz);
        if (stepHum > 0.34) {
            Holder<Biome> stepBiome = biomeAccessor.apply(scratchPos.set(sx, Mth.floor(y), sz));
            if (!stepBiome.is(ELYSIAN_ABYSS_KEY)) {
                return width * Mth.clamp((0.44 - stepHum) / 0.10, 0.0, 1.0);
            }
        }
        return width;
    }

    private boolean tryCarveSafeCaveAir(CaveCarverConfiguration config, ChunkAccess chunk, BlockPos pos,
            int bx, int by, int bz, double[] colHumCache, Function<BlockPos, Holder<Biome>> biomeAccessor) {
        if (by < -96 || by >= 60) {
            return false; // Protect aether rivers and islands/beaches
        }
        int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bx & 15, bz & 15);
        if (by >= surfaceY - 12) {
            return false; // Protect ocean seabed crust
        }
        BlockState state = chunk.getBlockState(pos);
        if (!state.getFluidState().isEmpty()
                || state.is(net.ganyusbathwater.oririmod.block.ModBlocks.ABYSSAL_BASALT.get())
                || state.is(net.ganyusbathwater.oririmod.block.ModBlocks.ABYSS_SAND.get())
                || state.is(net.ganyusbathwater.oririmod.block.ModBlocks.SCARLET_STONE.get())
                || state.is(net.ganyusbathwater.oririmod.block.ModBlocks.SCARLET_DEEPSLATE.get())
                || !this.canReplaceBlock(config, state)) {
            return false; // Never carve oceanic basalt, Scarlet stone, or leak into underwater caves
        }
        double colHum = getCachedColHum(colHumCache, bx, bz);
        if (colHum > 0.40) {
            Holder<Biome> posBiome = biomeAccessor.apply(pos);
            if (posBiome.is(SCARLET_CAVES_KEY) || posBiome.is(SCARLET_FOREST_KEY) || posBiome.is(SCARLET_SWAMP_KEY)) {
                return false;
            }
            if (!posBiome.is(ELYSIAN_ABYSS_KEY) && colHum > 0.43) {
                return false;
            }
        }
        net.ganyusbathwater.oririmod.worldgen.ElderwoodsCaveAquifer activeAquifer = CURRENT_AQUIFER.get();
        boolean nearFluid = activeAquifer != null
                ? activeAquifer.hasAdjacentOrPredictedFluid(chunk, bx, by, bz, 2, 1)
                : net.ganyusbathwater.oririmod.worldgen.ElderwoodsCaveAquifer
                        .hasAdjacentFluidInChunk(chunk, bx, by, bz, 2, 1);
        if (nearFluid) {
            return false; // Keep aquifer rock basins sealed across both local and neighbor chunks
        }
        chunk.setBlockState(pos, Blocks.CAVE_AIR.defaultBlockState(), false);
        return true;
    }
}
