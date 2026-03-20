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


        // ── 2.  SURFACE CANYON ──────────────────────────────────────────────
        // Very rare canyon for occasional lighting
        if (stableRandom.nextFloat() < 0.03f) { // Reduced from 15% to 3%
            int cxWorld = startX + 8;
            int czWorld = startZ + 8;
            boolean xAligned = stableRandom.nextBoolean();

            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int worldX = startX + localX;
                    int worldZ = startZ + localZ;

                    double wobble = Math.sin(worldX * 0.08) * 4.0 + Math.cos(worldZ * 0.08) * 4.0;
                    double distToCenter = xAligned ? Math.abs(worldZ - (czWorld + wobble)) : Math.abs(worldX - (cxWorld + wobble));

                    int canyonStart = 40;
                    int canyonEnd = Mth.clamp(160, minBuildY, maxBuildY);
                    
                    for (int y = canyonStart; y <= canyonEnd; y++) {
                        double progress = (double)(y - canyonStart) / (canyonEnd - canyonStart);
                        double currentWidth = (14.0 + Math.sin(y * 0.04) * 5.0) * Math.sin(progress * Math.PI);
                        
                        if (distToCenter < currentWidth) {
                            mutablePos.set(worldX, y, worldZ);
                            BlockState state = chunk.getBlockState(mutablePos);
                            if (this.canReplaceBlock(config, state)) {
                                chunk.setBlockState(mutablePos, Blocks.CAVE_AIR.defaultBlockState(), false);
                                if (carvingMask != null) {
                                    int maskY = y - minBuildY;
                                    if (maskY >= 0 && maskY < chunk.getHeight()) {
                                        carvingMask.set(localX, maskY, localZ);
                                    }
                                }
                                hit = true;
                            }
                        }
                    }
                }
            }
        }

        return hit;
    }
}
