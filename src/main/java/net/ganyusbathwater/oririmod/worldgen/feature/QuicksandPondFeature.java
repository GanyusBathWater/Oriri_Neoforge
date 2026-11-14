package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class QuicksandPondFeature extends Feature<NoneFeatureConfiguration> {

    public QuicksandPondFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // Must spawn on Sol Sand
        if (!level.getBlockState(origin.below()).is(ModBlocks.SOL_SAND.get())) {
            return false;
        }

        int radiusX = 4 + random.nextInt(3); // 4 to 6 radius X
        int radiusZ = 4 + random.nextInt(3); // 4 to 6 radius Z
        int depth = 4; // 4 blocks deep at center

        BlockState quicksand = ModBlocks.SOL_QUICKSAND.get().defaultBlockState();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // Carve crater and fill with quicksand (Oval, sloping sides)
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double distanceSq = (double)(x * x) / (radiusX * radiusX) + (double)(z * z) / (radiusZ * radiusZ);
                if (distanceSq <= 1.0) {
                    // Calculate a sloping depth based on distance from center (parabolic)
                    int currentDepth = (int) Math.round(depth * (1.0 - distanceSq));

                    int targetX = origin.getX() + x;
                    int targetZ = origin.getZ() + z;
                    int actualY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, targetX, targetZ);
                    
                    // actualY is now the air block just above the surface
                    for (int d = 1; d <= currentDepth; d++) {
                        mutablePos.set(targetX, actualY - d, targetZ);
                        
                        // Blend the rim
                        if (d == 1 && distanceSq > 0.75 && random.nextBoolean()) {
                            continue;
                        }
                        
                        BlockState existing = level.getBlockState(mutablePos);
                        if (existing.is(ModBlocks.SOL_SAND.get()) || existing.is(ModBlocks.SOL_SANDSTONE.get())) {
                            level.setBlock(mutablePos, quicksand, 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}
