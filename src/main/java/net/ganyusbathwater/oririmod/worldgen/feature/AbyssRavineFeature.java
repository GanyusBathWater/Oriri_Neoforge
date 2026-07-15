package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.ganyusbathwater.oririmod.util.FastNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class AbyssRavineFeature extends Feature<NoneFeatureConfiguration> {

    public AbyssRavineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos currentChunk = new ChunkPos(context.origin());
        boolean hit = false;
        
        long worldSeed = level.getSeed();
        RandomSource seedRandom = RandomSource.create(worldSeed);
        seedRandom.nextDouble();
        seedRandom.nextDouble();
        double seedOffsetCave = seedRandom.nextDouble() * 10000.0;
        
        int range = 4;
        
        for (int cx = -range; cx <= range; cx++) {
            for (int cz = -range; cz <= range; cz++) {
                int ox = currentChunk.x + cx;
                int oz = currentChunk.z + cz;
                
                long originSeed = (ox * 341873128712L) ^ (oz * 132897987541L);
                RandomSource originRandom = RandomSource.create(originSeed);
                
                float scale = 0.0015f;
                double caveNoise = FastNoise.fbm3D(
                    (float)((ox * 16 + seedOffsetCave) * scale),
                    0f,
                    (float)((oz * 16 + seedOffsetCave) * scale),
                    4
                );
                
                caveNoise = Math.abs(caveNoise);
                double noiseThreshold = 0.20;
                
                if (caveNoise > noiseThreshold) {
                    float ravineChance = 1.0f / 70.0f;
                    if (caveNoise > 0.25) {
                        ravineChance = 1.0f / 30.0f; 
                    } else if (caveNoise < 0.12) {
                        ravineChance = 1.0f / 120.0f; 
                    }
                    
                    if (originRandom.nextFloat() < ravineChance) {
                        hit |= placeRavine(level, currentChunk, ox, oz, originRandom);
                    }
                }
            }
        }
        
        return hit;
    }

    private boolean placeRavine(WorldGenLevel level, ChunkPos currentChunk, int originX, int originZ, RandomSource random) {
        boolean hit = false;
        
        double x = (originX << 4) + 8 + random.nextInt(16);
        double z = (originZ << 4) + 8 + random.nextInt(16);
        
        float yaw = random.nextFloat() * (float) Math.PI * 2.0f;
        float width = 2.0f + random.nextFloat() * 5.0f;
        int length = 35 + random.nextInt(40);
        
        double topY = 320;
        double bottomY = -60; 
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        int chunkMinX = currentChunk.getMinBlockX();
        int chunkMaxX = currentChunk.getMaxBlockX();
        int chunkMinZ = currentChunk.getMinBlockZ();
        int chunkMaxZ = currentChunk.getMaxBlockZ();
        
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
            
            double maxPossibleRadius = currentWidth + 5.0;
            
            int minX = Mth.floor(x - maxPossibleRadius);
            int maxX = Mth.floor(x + maxPossibleRadius);
            int minZ = Mth.floor(z - maxPossibleRadius);
            int maxZ = Mth.floor(z + maxPossibleRadius);
            
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
                                    // Use a square root curve for a U-shaped floor, with a minimum width
                                    // to act as a middle ground between flat and perfectly sharp.
                                    double curve = Math.sqrt(bottomTaperProgress);
                                    double taperedWidth = currentWidth * (0.4 + 0.6 * curve);
                                    if (dx * dx + dz * dz > taperedWidth * taperedWidth) {
                                        continue;
                                    }
                                }
                                
                                pos.set(bx, by, bz);
                                BlockState state = level.getBlockState(pos);
                                
                                if (isReplaceable(state)) {
                                    if (by < -105) {
                                        continue;
                                    }
                                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
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

    private boolean isReplaceable(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.OVERWORLD_CARVER_REPLACEABLES) || 
               state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) ||
               state.is(net.minecraft.tags.BlockTags.DIRT) ||
               state.is(net.minecraft.tags.BlockTags.LEAVES) ||
               state.is(net.minecraft.tags.BlockTags.LOGS) ||
               state.is(Blocks.WATER) || 
               state.is(Blocks.SAND) || 
               state.is(Blocks.GRAVEL) ||
               state.is(net.ganyusbathwater.oririmod.block.ModBlocks.SCARLET_GRASS_BLOCK.get()) ||
               state.is(net.ganyusbathwater.oririmod.block.ModBlocks.SCARLET_STONE.get()) ||
               state.canBeReplaced() ||
               state.getBlock() instanceof net.minecraft.world.level.block.BushBlock;
    }
}
