package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Natural Underwater Flora Feature for the standard photic ocean (the_silent_oceans).
 * Generates:
 * 1. Seagrass and Tall Seagrass meadows across sand/gravel seabed.
 * 2. Kelp forest columns rising toward the photic ocean surface.
 * 3. Clusters of Sea Pickles on rocky outcroppings.
 */
public class SilentOceanFloraFeature extends Feature<NoneFeatureConfiguration> {

    public SilentOceanFloraFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // 1. Locate the solid ocean floor below water
        BlockPos.MutableBlockPos mPos = origin.mutable();
        while (mPos.getY() > level.getMinBuildHeight() + 5 && level.getBlockState(mPos).is(Blocks.WATER)) {
            mPos.move(Direction.DOWN);
        }

        BlockPos floorPos = mPos.immutable();
        BlockState floorState = level.getBlockState(floorPos);
        BlockPos waterPos = floorPos.above();

        // Must be submerged in water below sea level (72)
        // Barren Continental Descent: Flora only grows in the photic ocean (Y >= 30)
        if (!level.getBlockState(waterPos).is(Blocks.WATER) || floorPos.getY() >= 72 || floorPos.getY() < 30) {
            return false;
        }

        // Must rest on sand, gravel, or stone
        boolean isValidFloor = floorState.is(Blocks.SAND) || floorState.is(Blocks.GRAVEL) || floorState.is(Blocks.STONE);
        if (!isValidFloor) {
            return false;
        }

        int patchRadius = 3 + random.nextInt(4); // 3 to 6 blocks radius
        boolean isKelpForest = random.nextInt(100) < 35; // 35% chance to be a dense kelp forest
        boolean placedAny = false;

        for (int dx = -patchRadius; dx <= patchRadius; dx++) {
            for (int dz = -patchRadius; dz <= patchRadius; dz++) {
                if (dx * dx + dz * dz > patchRadius * patchRadius) {
                    continue;
                }

                BlockPos targetFloor = floorPos.offset(dx, 0, dz);
                // Adjust for slight vertical terrain undulations
                while (level.getBlockState(targetFloor.above()).isSolid() && targetFloor.getY() < 71) {
                    targetFloor = targetFloor.above();
                }
                while (level.getBlockState(targetFloor).is(Blocks.WATER) && targetFloor.getY() > 5) {
                    targetFloor = targetFloor.below();
                }

                BlockPos targetWater = targetFloor.above();
                BlockState tFloorState = level.getBlockState(targetFloor);
                if (!level.getBlockState(targetWater).is(Blocks.WATER) || !tFloorState.isSolid()) {
                    continue;
                }

                if (isKelpForest && random.nextFloat() < 0.65f) {
                    // Place tall kelp column
                    int maxHeight = Math.min(18, 71 - targetWater.getY());
                    if (maxHeight >= 4) {
                        int kelpHeight = 3 + random.nextInt(maxHeight - 2);
                        for (int h = 0; h < kelpHeight; h++) {
                            BlockPos kPos = targetWater.above(h);
                            if (!level.getBlockState(kPos).is(Blocks.WATER)) {
                                break;
                            }
                            if (h == kelpHeight - 1) {
                                level.setBlock(kPos, Blocks.KELP.defaultBlockState(), 2);
                            } else {
                                level.setBlock(kPos, Blocks.KELP_PLANT.defaultBlockState(), 2);
                            }
                            placedAny = true;
                        }
                    }
                } else if (random.nextFloat() < 0.70f) {
                    // Place Seagrass / Tall Seagrass / Sea Pickles
                    float roll = random.nextFloat();
                    if (roll < 0.25f && level.getBlockState(targetWater.above()).is(Blocks.WATER)) {
                        // Tall Seagrass (2 blocks)
                        BlockState lower = Blocks.TALL_SEAGRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
                        BlockState upper = Blocks.TALL_SEAGRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                        level.setBlock(targetWater, lower, 2);
                        level.setBlock(targetWater.above(), upper, 2);
                        placedAny = true;
                    } else if (roll < 0.85f) {
                        // Standard Seagrass
                        level.setBlock(targetWater, Blocks.SEAGRASS.defaultBlockState(), 2);
                        placedAny = true;
                    } else if (tFloorState.is(Blocks.STONE)) {
                        // Sea Pickles on rocky ground
                        int pickles = 1 + random.nextInt(4);
                        level.setBlock(targetWater, Blocks.SEA_PICKLE.defaultBlockState()
                                .setValue(BlockStateProperties.PICKLES, pickles)
                                .setValue(BlockStateProperties.WATERLOGGED, true), 2);
                        placedAny = true;
                    }
                }
            }
        }

        return placedAny;
    }
}

