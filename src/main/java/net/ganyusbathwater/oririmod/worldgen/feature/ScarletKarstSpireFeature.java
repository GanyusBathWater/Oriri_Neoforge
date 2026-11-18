package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates anchored vertical Scarlet Stone and Scarlet Deepslate tower karst
 * spires with jagged crowns and mossy caps in the Scarlet Karst biome.
 */
public class ScarletKarstSpireFeature extends Feature<NoneFeatureConfiguration> {

    public ScarletKarstSpireFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockState belowState = level.getBlockState(origin.below());
        BlockState atOrigin = level.getBlockState(origin);
        if (!isValidAnchorGround(belowState) || !atOrigin.getFluidState().isEmpty()
                || (!atOrigin.isAir() && !atOrigin.canBeReplaced())) {
            return false;
        }

        int mainHeight = 9 + random.nextInt(13);
        double mainBaseRadius = 1.8 + random.nextDouble() * 0.9;
        double mainTopRadius = 0.85 + random.nextDouble() * 0.5;
        boolean placedAny = placeSpireColumn(level, random, origin, mainHeight, mainBaseRadius, mainTopRadius);

        // Add 1-2 clustered flanking stone pinnacles around the central spire
        int sideCount = 1 + random.nextInt(2);
        for (int i = 0; i < sideCount; i++) {
            int offsetX = (random.nextBoolean() ? 1 : -1) * random.nextIntBetweenInclusive(2, 3);
            int offsetZ = (random.nextBoolean() ? 1 : -1) * random.nextIntBetweenInclusive(2, 3);
            BlockPos sideOrigin = origin.offset(offsetX, 0, offsetZ);
            int sideHeight = 5 + random.nextInt(7);
            double sideBaseRadius = 1.2 + random.nextDouble() * 0.6;
            double sideTopRadius = 0.7 + random.nextDouble() * 0.35;
            placedAny |= placeSpireColumn(level, random, sideOrigin, sideHeight, sideBaseRadius, sideTopRadius);
        }

        return placedAny;
    }

    private boolean placeSpireColumn(WorldGenLevel level, RandomSource random, BlockPos center,
                                     int height, double baseRadius, double topRadius) {
        int maxHoriz = (int) Math.ceil(baseRadius);
        int maxWorldY = level.getMaxBuildHeight() - 2;
        double radiusDelta = Math.max(0.25, baseRadius - topRadius);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -maxHoriz; dx <= maxHoriz; dx++) {
            for (int dz = -maxHoriz; dz <= maxHoriz; dz++) {
                double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (dist > baseRadius) {
                    continue;
                }

                int colX = center.getX() + dx;
                int colZ = center.getZ() + dz;
                int groundY = findLocalGroundY(level, colX, center.getY(), colZ);
                if (groundY == Integer.MIN_VALUE) {
                    continue;
                }

                double taperT = Mth.clamp((baseRadius - dist) / radiusDelta, 0.0, 1.0);
                int jaggedOffset = random.nextInt(3) - 1;
                int colTopY = Math.min(maxWorldY, center.getY() + (int) Math.round(height * taperT) + jaggedOffset);
                if (colTopY <= groundY) {
                    continue;
                }

                int highestPlacedY = Integer.MIN_VALUE;
                for (int y = groundY; y <= colTopY; y++) {
                    cursor.set(colX, y, colZ);
                    BlockState existing = level.getBlockState(cursor);
                    if (canReplaceForSpire(existing)) {
                        double heightFrac = (double) (y - groundY) / Math.max(1, colTopY - groundY);
                        level.setBlock(cursor, pickKarstStone(random, heightFrac), 2);
                        highestPlacedY = y;
                        placed = true;
                    }
                }

                // Place 1-3 block Pointed Scarlet Dripstone stalagmites on ~38% of pinnacle/ledge column tops
                if (highestPlacedY >= groundY + 2 && dist <= topRadius + 0.6 && random.nextFloat() < 0.38f) {
                    int spikeHeight = 1 + random.nextInt(3);
                    placeUpwardDripstoneSpike(level, colX, highestPlacedY + 1, colZ, Math.min(spikeHeight, maxWorldY - highestPlacedY));
                }
            }
        }

        return placed;
    }

    private void placeUpwardDripstoneSpike(WorldGenLevel level, int x, int startY, int z, int maxHeight) {
        if (maxHeight <= 0) {
            return;
        }
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
        int actualHeight = 0;
        for (int i = 0; i < maxHeight; i++) {
            check.set(x, startY + i, z);
            BlockState state = level.getBlockState(check);
            if (!state.isAir() && !state.canBeReplaced()) {
                break;
            }
            if (!state.getFluidState().isEmpty()) {
                break;
            }
            actualHeight++;
        }
        if (actualHeight == 0) {
            return;
        }

        BlockState pointed = ModBlocks.POINTED_SCARLET_DRIPSTONE.get().defaultBlockState();
        for (int i = 0; i < actualHeight; i++) {
            DripstoneThickness thickness;
            if (actualHeight == 1 || i == actualHeight - 1) {
                thickness = DripstoneThickness.TIP;
            } else if (i == 0) {
                thickness = DripstoneThickness.BASE;
            } else {
                thickness = DripstoneThickness.FRUSTUM;
            }
            check.set(x, startY + i, z);
            level.setBlock(
                    check,
                    pointed.setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
                            .setValue(PointedDripstoneBlock.THICKNESS, thickness),
                    2);
        }
    }

    private int findLocalGroundY(WorldGenLevel level, int x, int baseY, int z) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int y = baseY + 3; y >= baseY - 5; y--) {
            probe.set(x, y, z);
            BlockState state = level.getBlockState(probe);
            if (isValidAnchorGround(state)) {
                return y;
            }
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
                || state.is(ModBlocks.SCARLET_STONE.get())
                || state.is(ModBlocks.SCARLET_DRIPSTONE_BLOCK.get());
    }

    private static boolean canReplaceForSpire(BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
            return false;
        }
        return state.isAir()
                || (state.canBeReplaced() && state.getFluidState().isEmpty())
                || isValidAnchorGround(state);
    }

    private static BlockState pickKarstStone(RandomSource random, double heightFrac) {
        // 65% Scarlet Stone, 35% Scarlet Dripstone Block
        if (random.nextFloat() < 0.35f) {
            return ModBlocks.SCARLET_DRIPSTONE_BLOCK.get().defaultBlockState();
        }
        return ModBlocks.SCARLET_STONE.get().defaultBlockState();
    }
}
