package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Alpine Rock Spire / Mountain Crag Feature.
 * Generates solid, monolithic stone aiguilles and crags.
 * Strictly uses 100% full solid blocks (Stone, Andesite, Cobblestone, Mossy Cobblestone)
 * with zero internal cavities, zero air pockets, and zero stairs/slabs.
 */
public class HighlandsBoulderFeature extends Feature<NoneFeatureConfiguration> {

    public HighlandsBoulderFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // 1. Valid Ground Check
        BlockPos belowPos = origin.below();
        BlockState belowState = level.getBlockState(belowPos);
        boolean validGround = belowState.is(ModBlocks.HIGHLANDS_GRASS_BLOCK.get()) ||
                belowState.is(ModBlocks.ELDERWOODS_GRASS_BLOCK.get()) ||
                belowState.is(Blocks.GRASS_BLOCK) ||
                belowState.is(Blocks.DIRT) ||
                belowState.is(Blocks.STONE) ||
                belowState.is(Blocks.ANDESITE) ||
                belowState.is(Blocks.SNOW_BLOCK);

        if (!validGround) {
            return false;
        }

        if (!level.getBlockState(origin).isAir() && !level.getBlockState(origin).canBeReplaced()) {
            return false;
        }

        // 2. Mountain Spire Dimensions (8 to 17 blocks tall)
        int height = 8 + random.nextInt(10);
        int baseRadius = height > 12 ? 3 : 2;

        // Slanted angle of the spire (natural alpine needle thrust)
        Direction leanDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        float leanAngle = random.nextFloat() * 0.35f;

        // 3. Anchor Foundation (Bury 3 blocks into mountain so it never floats or leaves hollow roots)
        for (int x = -baseRadius; x <= baseRadius; x++) {
            for (int z = -baseRadius; z <= baseRadius; z++) {
                if (x * x + z * z <= baseRadius * baseRadius) {
                    for (int dy = -3; dy <= 0; dy++) {
                        BlockPos p = origin.offset(x, dy, z);
                        setSolidStone(level, p, random);
                    }
                }
            }
        }

        // 4. Solid Ascending Tapered Shaft
        for (int y = 0; y < height; y++) {
            float progress = (float) y / (float) height;
            // Taper profile: radius shrinks gradually towards the apex
            double currentRadius = Math.max(0.6, baseRadius * Math.pow(1.0f - progress, 0.70));

            // Continuous lateral offset from lean
            int offsetX = (int) Math.round(leanDir.getStepX() * y * leanAngle);
            int offsetZ = (int) Math.round(leanDir.getStepZ() * y * leanAngle);

            int scanR = (int) Math.ceil(currentRadius + 0.6);

            for (int x = -scanR; x <= scanR; x++) {
                for (int z = -scanR; z <= scanR; z++) {
                    double distSq = (x * x + z * z);
                    // Faceted angular needle shape (triangular/hexagonal crag)
                    double angle = Math.atan2(z, x);
                    double facetMod = 1.0 + 0.25 * Math.sin(angle * 3.0);
                    double effectiveRadius = currentRadius * facetMod;

                    if (distSq <= effectiveRadius * effectiveRadius) {
                        BlockPos p = origin.offset(x + offsetX, y, z + offsetZ);
                        // Always place solid stone: fills all internal cavities and replaces any air/foliage/snow
                        setSolidStone(level, p, random);

                        // At high alpine altitudes, dust exposed flat tops with snow
                        if (y == height - 1 && p.getY() >= 130) {
                            BlockPos above = p.above();
                            if (level.isEmptyBlock(above)) {
                                level.setBlock(above, Blocks.SNOW.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }

        // 5. Solid Pinnacle Apex
        int topOffsetX = (int) Math.round(leanDir.getStepX() * height * leanAngle);
        int topOffsetZ = (int) Math.round(leanDir.getStepZ() * height * leanAngle);
        BlockPos tipPos = origin.offset(topOffsetX, height, topOffsetZ);
        setSolidStone(level, tipPos, random);

        return true;
    }

    private void setSolidStone(WorldGenLevel level, BlockPos pos, RandomSource random) {
        float pick = random.nextFloat();
        BlockState state;
        if (pick < 0.55f) {
            state = Blocks.STONE.defaultBlockState();
        } else if (pick < 0.80f) {
            state = Blocks.ANDESITE.defaultBlockState();
        } else if (pick < 0.92f) {
            state = Blocks.COBBLESTONE.defaultBlockState();
        } else {
            state = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        }
        level.setBlock(pos, state, 3);
    }
}
