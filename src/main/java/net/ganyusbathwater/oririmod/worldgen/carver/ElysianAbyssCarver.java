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
 * Elysian Abyss Carver.
 *
 * Carves two things per region:
 * 1. A WIDE elliptical cave room (80-120 blocks tall, very wide horizontally)
 *    centred around Y = -30. The room spans the whole biome chunk area.
 * 2. A narrow CANYON slit from the cave ceiling up to the surface (4-40 blocks wide).
 */
public class ElysianAbyssCarver extends WorldCarver<CaveCarverConfiguration> {

    // Cave room target centre Y


    public ElysianAbyssCarver(Codec<CaveCarverConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean isStartChunk(CaveCarverConfiguration config, RandomSource random) {
        // Each chunk is a candidate — the actual per-region selection happens inside carve()
        return random.nextFloat() <= config.probability;
    }

    @Override
    public boolean carve(CarvingContext context, CaveCarverConfiguration config,
            ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeAccessor,
            RandomSource random, Aquifer aquifer, net.minecraft.world.level.ChunkPos chunkPos,
            CarvingMask carvingMask) {

        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        // Valid Y range for the chunk
        int minBuildY = chunk.getMinBuildHeight();
        int maxBuildY = chunk.getMaxBuildHeight() - 1;

        // Derive a stable seed for this chunk so cave height/shape is deterministic
        long chunkSeed = (long) chunkPos.x * 341873128712L ^ (long) chunkPos.z * 132897987541L;
        RandomSource stableRandom = RandomSource.create(chunkSeed ^ random.nextLong());
        
        // ── 1.  MASSIVE CAVERN LOGIC (MOVED TO fillFromNoise) ──────────────────
        // Caverns are now handled by ElderwoodsChunkGenerator.fillFromNoise for perfect 
        // biome-alignment and monolithic scale. 
        // No room carving needed here anymore.
        
        boolean hit = false;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();


        // ── 2.  SURFACE CRACKS / CEILING RIFTS ───────────────────────────
        // Uses 2-D fBm noise in XZ space so the ceiling opening is a fully
        // irregular blob — no stripes, no rectangles, no 90° angles.
        if (stableRandom.nextFloat() < 0.18f) {
            int numCracks = 1 + (stableRandom.nextFloat() < 0.35f ? 1 : 0);
            for (int crackIdx = 0; crackIdx < numCracks; crackIdx++) {
                // Crack centre
                int cxWorld = startX + 2 + stableRandom.nextInt(12);
                int czWorld = startZ + 2 + stableRandom.nextInt(12);
                double peakRadius = 3.0 + stableRandom.nextDouble() * 9.0;

                // Per-crack phase so each crack has a unique noise field
                long crackSeed = chunkSeed ^ (crackIdx * 0x9E3779B97F4A7C15L);
                double ph = (crackSeed & 0xFFFFL) / 65536.0 * Math.PI * 4.0;

                int canyonTop    = maxBuildY;
                int canyonBottom = Mth.clamp(-60, minBuildY, maxBuildY);

                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldX = startX + localX;
                        int worldZ = startZ + localZ;

                        double rx = worldX - cxWorld;
                        double rz = worldZ - czWorld;

                        // --- 2-D fBm in XZ: 4 octaves, each rotated to avoid axis bias ---
                        double fx = rx * 0.12 + ph;
                        double fz = rz * 0.12;
                        double fbm = 0.0;
                        double amp = 1.0;
                        double freq = 1.0;
                        for (int oct = 0; oct < 4; oct++) {
                            // Rotate each octave by ~37.5° to avoid grid alignment
                            double rfx = fx * freq * 0.809 - fz * freq * 0.588;
                            double rfz = fx * freq * 0.588 + fz * freq * 0.809;
                            fbm += Math.sin(rfx) * Math.cos(rfz) * amp;
                            amp  *= 0.5;
                            freq *= 2.1;
                        }
                        // fbm in [-1, 1]; shift so positive = near centre
                        double centrePull = 1.0 - Math.sqrt(rx * rx + rz * rz) / (peakRadius * 2.5);
                        double openness = fbm * 0.6 + centrePull;

                        for (int y = canyonBottom; y <= canyonTop; y++) {
                            double progress = (double)(y - canyonBottom)
                                    / Math.max(1, canyonTop - canyonBottom);
                            // Bell envelope: zero at top/bottom, max at midpoint
                            double envelope = Math.sin(Mth.clamp(progress, 0.0, 1.0) * Math.PI)
                                    * peakRadius;
                            // Y-local noise modulates the threshold
                            double ynoise = Math.sin(y * 0.11 + ph) * 0.15;
                            double threshold = 0.05 + ynoise;

                            if (openness + envelope * 0.15 > threshold) {
                                mutablePos.set(worldX, y, worldZ);
                                BlockState state = chunk.getBlockState(mutablePos);
                                if (this.canReplaceBlock(config, state)) {
                                    chunk.setBlockState(mutablePos,
                                            Blocks.CAVE_AIR.defaultBlockState(), false);
                                    if (carvingMask != null) {
                                        int maskY = y - minBuildY;
                                        if (maskY >= 0 && maskY < chunk.getHeight())
                                            carvingMask.set(localX, maskY, localZ);
                                    }
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
}
