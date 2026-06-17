package net.ganyusbathwater.oririmod.worldgen.carver;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

import java.util.function.Function;

/**
 * Elysian Abyss Carver - The Skylight Ravine.
 *
 * Connects the surface to the Elysian Abyss using a spline-based winding ravine algorithm.
 * Safely loops over neighboring chunks internally to prevent the chunk-clipping square bug.
 */
public class ElysianAbyssCarver extends WorldCarver<CaveCarverConfiguration> {

    public ElysianAbyssCarver(Codec<CaveCarverConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean isStartChunk(CaveCarverConfiguration config, RandomSource random) {
        // Probability check is now handled per-origin inside carve() to guarantee cross-chunk sync
        return true; 
    }

    @Override
    public boolean carve(CarvingContext context, CaveCarverConfiguration config,
            ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeAccessor,
            RandomSource random, Aquifer aquifer, net.minecraft.world.level.ChunkPos chunkPos,
            CarvingMask carvingMask) {

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        boolean hit = false;

        // Radius of 12 chunks mathematically guarantees we catch worms that started far away and wind into our chunk
        // Max length = 120 steps * 1.5 blocks/step = 180 blocks + 11 width = 191 max blocks = 11.9 chunks
        int radius = 12;
        for (int ox = chunkX - radius; ox <= chunkX + radius; ox++) {
            for (int oz = chunkZ - radius; oz <= chunkZ + radius; oz++) {
                
                // Deterministic seed for the origin chunk
                long originSeed = (ox * 341873128712L) ^ (oz * 132897987541L);
                RandomSource originRandom = RandomSource.create(originSeed);
                
                // --- Mathematical Abyss Detection ---
                double seedOffsetCave = net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator.currentSeedOffsetCave;
                float scale = 0.0015f;
                double caveNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                    (float)((ox * 16 + seedOffsetCave) * scale),
                    0f,
                    (float)((oz * 16 + seedOffsetCave) * scale),
                    3
                );
                double abyssIntensity = Mth.clamp((caveNoise - 0.08) / 0.15, 0.0, 1.0);
                
                float ravineChance = 1.0f / 180.0f; // Toned down surface ravines
                if (abyssIntensity > 0.1) {
                    ravineChance = 1.0f / 60.0f; 
                }
                
                // Spawn Ravine canyon
                if (originRandom.nextFloat() < ravineChance) {
                    hit |= carveRavine(config, chunk, ox, oz, originRandom);
                }
                
                // Spawn standard winding cave systems (Worms & Rooms)
                hit |= carveWorms(config, chunk, ox, oz, originRandom);
                
                // Spawn massive, localized Cheese Chambers
                hit |= carveCheeseChambers(config, chunk, ox, oz, originRandom);
                
                // Spawn erratic Noodle tunnels for vertical connections
                hit |= carveNoodles(config, chunk, ox, oz, originRandom);
            }
        }

        return hit;
    }

    private boolean carveWorms(CaveCarverConfiguration config, ChunkAccess chunk, int originX, int originZ, RandomSource random) {
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
                
                double finalWidth = currentWidth * endTaper;
                if (finalWidth < 0.5) continue;
                
                int chunkMinX = chunk.getPos().getMinBlockX();
                int chunkMaxX = chunk.getPos().getMaxBlockX();
                int chunkMinZ = chunk.getPos().getMinBlockZ();
                int chunkMaxZ = chunk.getPos().getMaxBlockZ();
                
                int minX = Mth.floor(x - finalWidth);
                int maxX = Mth.floor(x + finalWidth);
                int minZ = Mth.floor(z - finalWidth);
                int maxZ = Mth.floor(z + finalWidth);
                int minY = Mth.floor(y - finalWidth);
                int maxY = Mth.floor(y + finalWidth);
                
                // Check if this cave step overlaps our currently generating chunk
                if (maxX >= chunkMinX && minX <= chunkMaxX && maxZ >= chunkMinZ && minZ <= chunkMaxZ) {
                    for (int bx = Math.max(minX, chunkMinX); bx <= Math.min(maxX, chunkMaxX); bx++) {
                        for (int bz = Math.max(minZ, chunkMinZ); bz <= Math.min(maxZ, chunkMaxZ); bz++) {
                            for (int by = minY; by <= maxY; by++) {
                                double dx = x - bx;
                                double dy = y - by;
                                double dz = z - bz;
                                
                                if (dx * dx + dy * dy + dz * dz < finalWidth * finalWidth) {
                                    pos.set(bx, by, bz);
                                    BlockState state = chunk.getBlockState(pos);
                                    if (this.canReplaceBlock(config, state)) {
                                        // Surface structure protection
                                        if (by > 50 && chunk.hasAnyStructureReferences()) {
                                            continue;
                                        }
                                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                        hit = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return hit;
    }

    private boolean carveRavine(CaveCarverConfiguration config, ChunkAccess chunk, int originX, int originZ, RandomSource random) {
        boolean hit = false;
        
        double x = (originX << 4) + 8 + random.nextInt(16);
        double z = (originZ << 4) + 8 + random.nextInt(16);
        
        float yaw = random.nextFloat() * (float) Math.PI * 2.0f;
        float width = 2.0f + random.nextFloat() * 5.0f; // 2 to 7 blocks wide (radius)
        int length = 35 + random.nextInt(40);           // 35 to 75 blocks long
        
        double topY = chunk.getMaxBuildHeight();
        double bottomY = -60; 
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        for (int step = 0; step < length; step++) {
            x += Math.cos(yaw) * 1.5;
            z += Math.sin(yaw) * 1.5;
            yaw += (random.nextFloat() - 0.5f) * 0.25f; 
            
            width += (random.nextFloat() - 0.5f) * 1.2f; 
            width = Mth.clamp(width, 3.0f, 15.0f);
            
            double stepProgress = (double) step / length;
            double endTaper = 1.0;
            if (stepProgress < 0.1) endTaper = stepProgress / 0.1;
            if (stepProgress > 0.9) endTaper = (1.0 - stepProgress) / 0.1;
            double currentWidth = width * endTaper;
            
            if (currentWidth < 0.5) continue;
            
            // Max noise added to radius is roughly 4.5. 
            // We use 5.0 as an absolute safe margin for the bounding box.
            double maxPossibleRadius = currentWidth + 5.0;
            
            int minX = Mth.floor(x - maxPossibleRadius);
            int maxX = Mth.floor(x + maxPossibleRadius);
            int minZ = Mth.floor(z - maxPossibleRadius);
            int maxZ = Mth.floor(z + maxPossibleRadius);
            
            int chunkMinX = chunk.getPos().getMinBlockX();
            int chunkMaxX = chunk.getPos().getMaxBlockX();
            int chunkMinZ = chunk.getPos().getMinBlockZ();
            int chunkMaxZ = chunk.getPos().getMaxBlockZ();
            
            // Mathematically safe bounding box check prevents chunk clipping
            if (maxX >= chunkMinX && minX <= chunkMaxX && maxZ >= chunkMinZ && minZ <= chunkMaxZ) {
                for (int bx = Math.max(minX, chunkMinX); bx <= Math.min(maxX, chunkMaxX); bx++) {
                    for (int bz = Math.max(minZ, chunkMinZ); bz <= Math.min(maxZ, chunkMaxZ); bz++) {
                        double dx = x - bx;
                        double dz = z - bz;
                        
                        double xzNoise = Math.sin(bx * 0.1) * Math.cos(bz * 0.1) * 2.0;
                        xzNoise += Math.sin(bx * 0.3 + bz * 0.2) * 0.5;
                        
                        for (int by = (int) topY; by >= (int) bottomY; by--) {
                            double yNoise = Math.sin(by * 0.04) * 2.0;
                            double localRadius = currentWidth + xzNoise + yNoise;
                            if (localRadius < 0.0) localRadius = 0.0;
                            
                            if (dx * dx + dz * dz < localRadius * localRadius) {
                                double bottomTaperProgress = (by - bottomY) / 25.0;
                                if (bottomTaperProgress < 1.0) {
                                    double taperedWidth = currentWidth * bottomTaperProgress;
                                    if (dx * dx + dz * dz > taperedWidth * taperedWidth) {
                                        continue;
                                    }
                                }
                                
                                pos.set(bx, by, bz);
                                BlockState state = chunk.getBlockState(pos);
                                if (this.canReplaceBlock(config, state)) {
                                    // Surface structure protection
                                    if (by > 50 && chunk.hasAnyStructureReferences()) {
                                        continue;
                                    }
                                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
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

    private boolean carveCheeseChambers(CaveCarverConfiguration config, ChunkAccess chunk, int originX, int originZ, RandomSource random) {
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
        int minY = Mth.floor(y - maxRadius);
        int maxY = Mth.floor(y + maxRadius);
        
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();
        
        // Safe bounding box check
        if (maxX >= chunkMinX && minX <= chunkMaxX && maxZ >= chunkMinZ && minZ <= chunkMaxZ) {
            for (int bx = Math.max(minX, chunkMinX); bx <= Math.min(maxX, chunkMaxX); bx++) {
                for (int bz = Math.max(minZ, chunkMinZ); bz <= Math.min(maxZ, chunkMaxZ); bz++) {
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
                            
                            // Smoothly taper the noise effect toward the edges of the room
                            double normalizedDist = Math.sqrt(distanceSq) / maxRadius;
                            double noiseThreshold = 0.3 - (normalizedDist * 0.2); // Core is mostly air, edges are rocky
                            
                            // If noise is very high, leave it as solid rock (creates pillars)
                            if (noise > noiseThreshold) continue;
                            
                            BlockPos pos = new BlockPos(bx, by, bz);
                            BlockState state = chunk.getBlockState(pos);
                            
                            if (this.canReplaceBlock(config, state)) {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                hit = true;
                            }
                        }
                    }
                }
            }
        }
        return hit;
    }

    private boolean carveNoodles(CaveCarverConfiguration config, ChunkAccess chunk, int originX, int originZ, RandomSource random) {
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
                
                double finalWidth = width * endTaper;
                if (finalWidth < 0.5) continue;
                
                int chunkMinX = chunk.getPos().getMinBlockX();
                int chunkMaxX = chunk.getPos().getMaxBlockX();
                int chunkMinZ = chunk.getPos().getMinBlockZ();
                int chunkMaxZ = chunk.getPos().getMaxBlockZ();
                
                int minX = Mth.floor(x - finalWidth);
                int maxX = Mth.floor(x + finalWidth);
                int minZ = Mth.floor(z - finalWidth);
                int maxZ = Mth.floor(z + finalWidth);
                int minY = Mth.floor(y - finalWidth);
                int maxY = Mth.floor(y + finalWidth);
                
                if (maxX >= chunkMinX && minX <= chunkMaxX && maxZ >= chunkMinZ && minZ <= chunkMaxZ) {
                    for (int bx = Math.max(minX, chunkMinX); bx <= Math.min(maxX, chunkMaxX); bx++) {
                        for (int bz = Math.max(minZ, chunkMinZ); bz <= Math.min(maxZ, chunkMaxZ); bz++) {
                            for (int by = minY; by <= maxY; by++) {
                                double dx = x - bx;
                                double dy = y - by;
                                double dz = z - bz;
                                
                                if (dx * dx + dy * dy + dz * dz < finalWidth * finalWidth) {
                                    pos.set(bx, by, bz);
                                    BlockState state = chunk.getBlockState(pos);
                                    if (this.canReplaceBlock(config, state)) {
                                        // Surface structure protection
                                        if (by > 50 && chunk.hasAnyStructureReferences()) {
                                            continue;
                                        }
                                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                        hit = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return hit;
    }
}
