package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places anchored, irregular sun-bleached Caliche boulder outcrops in the
 * Golden Savanna and Golden Savanna Mesa biomes.
 */
public class CalicheBoulderFeature extends Feature<NoneFeatureConfiguration> {

    public CalicheBoulderFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockState ground = level.getBlockState(origin.below());
        if (!isValidAnchorGround(ground)) {
            return false;
        }

        boolean placedAny = placeLobe(
                level,
                random,
                origin,
                1.6 + random.nextDouble() * 1.1,
                1.2 + random.nextDouble() * 0.9,
                1.6 + random.nextDouble() * 1.1
        );

        // Add 1-2 smaller satellite rock outcrops adjacent to the main boulder
        int satellites = 1 + random.nextInt(2);
        for (int i = 0; i < satellites; i++) {
            int offsetX = random.nextIntBetweenInclusive(-2, 2);
            int offsetZ = random.nextIntBetweenInclusive(-2, 2);
            if (offsetX == 0 && offsetZ == 0) {
                offsetX = random.nextBoolean() ? 2 : -2;
            }
            BlockPos satellitePos = origin.offset(offsetX, 0, offsetZ);
            placedAny |= placeLobe(
                    level,
                    random,
                    satellitePos,
                    1.1 + random.nextDouble() * 0.7,
                    0.9 + random.nextDouble() * 0.6,
                    1.1 + random.nextDouble() * 0.7
            );
        }

        return placedAny;
    }

    private boolean placeLobe(WorldGenLevel level, RandomSource random, BlockPos center,
                              double rx, double ry, double rz) {
        int maxDx = (int) Math.ceil(rx);
        int maxDz = (int) Math.ceil(rz);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -maxDx; dx <= maxDx; dx++) {
            for (int dz = -maxDz; dz <= maxDz; dz++) {
                double normX = dx / rx;
                double normZ = dz / rz;
                double horizDistSq = normX * normX + normZ * normZ;
                if (horizDistSq > 1.0) {
                    continue;
                }

                int colX = center.getX() + dx;
                int colZ = center.getZ() + dz;
                int groundY = findLocalGroundY(level, colX, center.getY(), colZ);
                if (groundY == Integer.MIN_VALUE) {
                    continue;
                }

                double domeHeight = ry * Math.sqrt(Math.max(0.0, 1.0 - horizDistSq));
                int topY = center.getY() + (int) Math.floor(domeHeight + random.nextDouble() * 0.45 - 0.15);
                if (topY <= groundY) {
                    continue;
                }

                // Anchor bottom into immediate soil block so rounded rock faces do not hover on slopes
                for (int y = groundY; y <= topY; y++) {
                    cursor.set(colX, y, colZ);
                    BlockState existing = level.getBlockState(cursor);
                    if (canReplaceForBoulder(existing)) {
                        level.setBlock(cursor, pickRockState(random), 2);
                        placed = true;
                    }
                }
            }
        }

        return placed;
    }

    private int findLocalGroundY(WorldGenLevel level, int x, int baseY, int z) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int y = baseY + 3; y >= baseY - 3; y--) {
            probe.set(x, y, z);
            BlockState state = level.getBlockState(probe);
            if (isValidAnchorGround(state) || state.is(ModBlocks.SOL_SANDSTONE.get())) {
                return y;
            }
            // Stop descending if we hit foliage/water/non-soil solid blocks like logs or sandstone cliffs
            if (!state.isAir() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES)) {
                return Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isValidAnchorGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(ModBlocks.CALICHE.get())
                || state.is(ModBlocks.SOL_SAND.get());
    }

    private static boolean canReplaceForBoulder(BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
            return false;
        }
        return state.isAir()
                || (state.canBeReplaced() && state.getFluidState().isEmpty())
                || isValidAnchorGround(state);
    }

    private static BlockState pickRockState(RandomSource random) {
        int roll = random.nextInt(20);
        if (roll < 16) {
            return ModBlocks.CALICHE.get().defaultBlockState();
        }
        return ModBlocks.SOL_SANDSTONE.get().defaultBlockState();
    }
}
