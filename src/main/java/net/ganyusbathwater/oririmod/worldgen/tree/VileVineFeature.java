package net.ganyusbathwater.oririmod.worldgen.tree;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.block.custom.UpgradedSaplingBlock;
import net.ganyusbathwater.oririmod.worldgen.feature.VileVineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Tree feature triggered by {@code Vile Sapling} and {@code Upgraded Vile Sapling}.
 * <p>
 * Grows a massive twisting Vile Vine (width 2..6 blocks, random tilt 0..60 degrees) stretching
 * 80..120 blocks into the sky and burrowing deep underground into the deepslate stratum, complete
 * with curling offshoot tendrils.
 * <ul>
 *   <li>Normal Vile Sapling: only pushes through air and normal surface blocks above ground
 *       (and natural stone/deepslate when burrowing underground).</li>
 *   <li>Upgraded Vile Sapling ({@link UpgradedSaplingBlock#IS_FORCING_GROWTH}): pushes through
 *       everything except bedrock / unbreakable blocks.</li>
 * </ul>
 */
public class VileVineFeature extends Feature<NoneFeatureConfiguration> {

    public VileVineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rnd = context.random();

        boolean isSapling = level instanceof ServerLevel;
        int flags = isSapling ? 3 : 2;
        // Detect upgraded sapling both from bonemeal thread-local AND natural random-tick growth
        boolean forcingGrowth = UpgradedSaplingBlock.IS_FORCING_GROWTH.get()
                || level.getBlockState(origin).is(ModBlocks.UPGRADED_VILE_SAPLING.get());

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        // Clear the sapling block itself first
        level.setBlock(origin, Blocks.AIR.defaultBlockState(), flags);

        // Sky reach: 80..120 blocks (clamped safely below world ceiling)
        double heightAbove = Math.min(80.0 + rnd.nextDouble() * 40.0, (maxY - 6) - origin.getY());
        heightAbove = Math.max(24.0, heightAbove);

        // Burrow down into deepslate with a little extra room downwards (target Y = -35 .. -72, above bedrock)
        int targetBottomY = Math.max(minY + 7, -36 - rnd.nextInt(36));
        double depthBelow = Math.max(16.0, origin.getY() - targetBottomY);

        int tendrilCount = 6 + rnd.nextInt(5);
        VileVineGeometry.VineSpec vine = VileVineGeometry.createVine(
                origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5,
                2.0, 6.0,
                heightAbove, depthBelow,
                60.0, Double.NaN,
                tendrilCount, rnd);

        VileVineGeometry.ReplacePredicate predicate = (lvl, pos, existing, aboveBase) -> {
            if (existing.is(Blocks.BEDROCK) || existing.getDestroySpeed(lvl, pos) < 0.0F) {
                return false;
            }
            if (forcingGrowth) {
                // Upgraded Vile Sapling pushes through everything except bedrock / unbreakable blocks
                return true;
            }
            // Normal Vile Sapling: normal surface blocks above ground, natural stone/deepslate below ground
            if (isNormalSurfaceBlock(existing)) {
                return true;
            }
            if (!aboveBase) {
                return existing.is(BlockTags.BASE_STONE_OVERWORLD)
                        || existing.is(Blocks.TUFF)
                        || existing.is(Blocks.COBBLESTONE)
                        || existing.is(Blocks.ANDESITE)
                        || existing.is(Blocks.DIORITE)
                        || existing.is(Blocks.GRANITE);
            }
            return false;
        };

        if (isSapling) {
            VileVineGeometry.placeVine(
                    level, vine,
                    Integer.MIN_VALUE, Integer.MAX_VALUE,
                    Integer.MIN_VALUE, Integer.MAX_VALUE,
                    flags, predicate);
        } else {
            // Safety clip if ever invoked inside chunk-bounded worldgen
            int chunkMinX = (origin.getX() >> 4) * 16 - 16;
            int chunkMaxX = chunkMinX + 47;
            int chunkMinZ = (origin.getZ() >> 4) * 16 - 16;
            int chunkMaxZ = chunkMinZ + 47;
            VileVineGeometry.placeVine(
                    level, vine,
                    chunkMinX, chunkMaxX,
                    chunkMinZ, chunkMaxZ,
                    flags, predicate);
        }

        return true;
    }

    private static boolean isNormalSurfaceBlock(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.WATER)
                || state.is(ModBlocks.VILE_VINE_BLOCK.get());
    }
}
