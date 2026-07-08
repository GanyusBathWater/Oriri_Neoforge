package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.ganyusbathwater.oririmod.block.ModBlocks;

public class ElderLeafPileFeature extends Feature<NoneFeatureConfiguration> {

    public ElderLeafPileFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        // 1. Proximity Check: only spawn if an Elder Log is within 5 blocks
        boolean nearLog = false;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -5, -5), origin.offset(5, 5, 5))) {
            if (level.getBlockState(pos).is(ModBlocks.ELDER_LOG_BLOCK.get())) {
                nearLog = true;
                break;
            }
        }

        if (!nearLog) {
            return false;
        }

        // 2. Base placement condition
        if (!level.getBlockState(origin.below()).isSolid() || !level.getBlockState(origin).canBeReplaced()) {
            return false;
        }

        // 3. Generate bush
        level.setBlock(origin, ModBlocks.ELDER_LOG_BLOCK.get().defaultBlockState(), 2);

        int radius = 1 + context.random().nextInt(3); // Random radius between 1 and 3
        int radiusSq = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip log

                    BlockPos pos = origin.offset(x, y, z);
                    int distSq = x * x + y * y + z * z;
                    
                    if (distSq <= radiusSq && level.getBlockState(pos).canBeReplaced()) {
                        // 70% chance to place a leaf block to make it jagged/organic
                        if (context.random().nextFloat() < 0.7f) {
                            BlockState leafState = context.random().nextFloat() < 0.1f ? 
                                ModBlocks.ELDER_LEAVES_FLOWERING.get().defaultBlockState() : 
                                ModBlocks.ELDER_LEAVES.get().defaultBlockState();
                            
                            level.setBlock(pos, leafState, 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}
