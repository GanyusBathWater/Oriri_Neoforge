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

public class ScarletBoulderFeature extends Feature<NoneFeatureConfiguration> {
    public ScarletBoulderFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource random = context.random();

        // 1. Valid Ground Check (Avoid Ponds/Caves)
        // Must be on solid Scarlet Grass Block, Stone, or Deepslate
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        boolean validGround = belowState.is(ModBlocks.SCARLET_GRASS_BLOCK.get()) ||
                belowState.is(ModBlocks.SCARLET_STONE.get()) ||
                belowState.is(ModBlocks.SCARLET_DEEPSLATE.get());

        if (!validGround) {
            return false;
        }

        // Also check if current pos is valid (not liquid, not obstructing)
        if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).canBeReplaced()) {
            return false;
        }

        // 2. Size Variant Selection
        // 0=Small (~7 blocks), 1=Medium (~19 blocks), 2=Big (~27 blocks)
        int variant = random.nextInt(3);

        // Radius squared thresholds
        double maxDistSq;
        if (variant == 0) {
            maxDistSq = 1.44; // r=1.2 -> Includes centers (0) and faces (1)
        } else if (variant == 1) {
            maxDistSq = 2.25; // r=1.5 -> Includes edges (2)
        } else {
            maxDistSq = 3.25; // r=1.8 -> Includes corners (3)
        }

        // Add random slight variation to radius to mix it up slightly (rarely change
        // block count)
        maxDistSq += (random.nextFloat() - 0.5f) * 0.2f;

        BlockState stone = ModBlocks.SCARLET_STONE.get().defaultBlockState();

        // 3. Generate Boulder using precise radius with noise
        int radius = 2; // Scans -2 to 2 covers all cases

        boolean placedAny = false;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    // Add per-block noise to the distance check for irregularity
                    // slightly larger noise magnitude for "random placement" look
                    double noise = (random.nextFloat() - 0.5) * 0.8;

                    // Check sphere distance with noise
                    if (x * x + y * y + z * z <= maxDistSq + noise) {
                        BlockPos currentPos = pos.offset(x, y, z);

                        BlockState currentState = level.getBlockState(currentPos);
                        if (currentState.canBeReplaced() || currentState.is(ModBlocks.SCARLET_GRASS_BLOCK.get())
                                || currentState.isAir()) {
                            level.setBlock(currentPos, stone, 3);
                            placedAny = true;
                        }
                    }
                }
            }
        }

        return placedAny;
    }
}
