package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import net.minecraft.world.level.block.LiquidBlock;

public class BloodWaterPondFeature extends Feature<BloodWaterPondConfig> {
    public BloodWaterPondFeature(Codec<BloodWaterPondConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BloodWaterPondConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BloodWaterPondConfig config = context.config();

        // 1. Validate starting position (should be on solid ground)
        if (!level.getBlockState(origin.below()).isSolid()) {
            return false;
        }

        // Only place ponds on grass or dirt surfaces
        BlockState groundBlock = level.getBlockState(origin.below());
        if (!groundBlock.is(Blocks.GRASS_BLOCK) && !groundBlock.is(Blocks.DIRT)
                && !groundBlock.is(ModBlocks.SCARLET_GRASS_BLOCK.get())) {
            return false;
        }

        int radiusBase = config.radius().sample(random);
        int maxDepth = config.depth().sample(random);

        // Aggressive Proximity Check: Manual Block Scan
        // 1. Check for existing liquids to prevent overlapping ponds
        // 2. Verify that the ground actually exists around the pond.
        int scanRadius = radiusBase + 8; // Enlarged radius for liquid check
        int originY = origin.getY();

        for (int x = -scanRadius; x <= scanRadius; x += 2) {
            for (int z = -scanRadius; z <= scanRadius; z += 2) {
                // Check the column at this position
                BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

                // Get surface height for liquid check
                int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX() + x, origin.getZ() + z);

                // Check for liquid at surface (or slightly below/above)
                for (int yOff = -1; yOff <= 1; yOff++) {
                    checkPos.set(origin.getX() + x, surfaceY + yOff, origin.getZ() + z);
                    if (level.getBlockState(checkPos)
                            .getBlock() instanceof LiquidBlock) {
                        return false; // Abort if existing liquid found nearby
                    }
                }

                // Check for profound solid (Void check) - only needed within the actual pond
                // radius + small buffer
                if (x >= -(radiusBase + 3) && x <= (radiusBase + 3) && z >= -(radiusBase + 3)
                        && z <= (radiusBase + 3)) {
                    boolean profoundSolidFound = false;
                    // Scan down 6 blocks from just below surface (using originY as approx reference
                    // or surfaceY)
                    // Using originY is safer for consistency with original logic, but using
                    // surfaceY is more accurate to terrain.
                    // Let's stick to original logic's reference frame for the solid check, but
                    // adapted for the loop

                    int checkY = originY;
                    // To be safe and consistent with previous logic, we use originY.
                    // But we can also use surfaceY if we trust it.
                    // Let's keep the original "void check" logic conceptually similar but
                    // integrated.

                    for (int yOffset = -1; yOffset >= -6; yOffset--) {
                        checkPos.set(origin.getX() + x, originY + yOffset, origin.getZ() + z);
                        if (level.getBlockState(checkPos).isSolid()) {
                            profoundSolidFound = true;
                            break;
                        }
                    }

                    if (!profoundSolidFound) {
                        return false; // Abort
                    }
                }
            }
        }

        BlockState fluidState = ModFluids.BLOOD_WATER_BLOCK.get().defaultBlockState();
        BlockState airState = Blocks.AIR.defaultBlockState();

        boolean placed = false;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // We want the pond surface to be slightly sunken into the ground
        // origin.getY() is usually the top solid block (from OCEAN_FLOOR).

        // Ensure origin is on solid ground (ignoring plants)
        // This fixes inconsistent water levels if the feature starts on a plant
        mutablePos.set(origin);
        int originDescent = 0;
        while (mutablePos.getY() > level.getMinBuildHeight() && !level.getBlockState(mutablePos).isSolid()
                && originDescent < 50) {
            mutablePos.move(Direction.DOWN);
            originDescent++;
        }
        int solidOriginY = mutablePos.getY();

        // Let's set water level two blocks below that to inset it deeper.
        int waterLevelY = solidOriginY - 2;

        // Generate irregular shape
        // We'll iterate a square around the center
        int checkRadius = radiusBase + 2;

        for (int x = -checkRadius; x <= checkRadius; x++) {
            for (int z = -checkRadius; z <= checkRadius; z++) {
                double distSq = x * x + z * z;

                // Noise factor for irregularity
                double noise = random.nextDouble() * 1.5 - 0.75;
                double effectiveRadius = radiusBase + noise;

                if (distSq <= effectiveRadius * effectiveRadius) {
                    // This column is part of the pond

                    // Determine details for this column
                    // Determine details for this column
                    mutablePos.setWithOffset(origin, x, 0, z);

                    // Find the current surface height at this x, z (relative to origin height)
                    // We scan from a bit above to find the ground
                    int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, mutablePos.getX(),
                            mutablePos.getZ());

                    mutablePos.set(origin.getX() + x, surfaceY, origin.getZ() + z);
                    // Descend to find ACTUAL solid ground (ignore plants/leaves)
                    // SAFETY: Added loop limit to prevent infinite loops or hangs
                    int descentSteps = 0;
                    while (mutablePos.getY() > level.getMinBuildHeight() && !level.getBlockState(mutablePos).isSolid()
                            && descentSteps < 50) {
                        mutablePos.move(Direction.DOWN);
                        descentSteps++;
                    }
                    surfaceY = mutablePos.getY();

                    // If the terrain is lower than our water level, we skip placing water
                    // to avoid "floating" puddles.
                    if (surfaceY < waterLevelY) {
                        continue;
                    }

                    // 1. Clear air above pond to remove floating blocks/trees
                    for (int y = surfaceY + 1; y <= waterLevelY + 8; y++) {
                        mutablePos.set(origin.getX() + x, y, origin.getZ() + z);
                        if (!level.getBlockState(mutablePos).is(Blocks.BEDROCK)) {
                            level.setBlock(mutablePos, airState, 3);
                        }
                    }

                    // 2. Flattening logic:
                    if (surfaceY > waterLevelY) {
                        for (int y = surfaceY; y > waterLevelY; y--) {
                            mutablePos.set(origin.getX() + x, y, origin.getZ() + z);
                            if (!level.getBlockState(mutablePos).is(Blocks.BEDROCK)) {
                                level.setBlock(mutablePos, airState, 3);
                            }
                        }
                    }

                    // Carving / Filling
                    // Determine depth at this point
                    double distRatio = Math.sqrt(distSq) / effectiveRadius;
                    int currentDepth = (int) Math.max(1, maxDepth * (1.0 - Math.pow(distRatio, 2.0)));
                    if (currentDepth > maxDepth)
                        currentDepth = maxDepth;

                    for (int y = 0; y >= -currentDepth; y--) {
                        mutablePos.set(origin.getX() + x, waterLevelY + y, origin.getZ() + z);

                        if (level.isOutsideBuildHeight(mutablePos))
                            continue;
                        if (level.getBlockState(mutablePos).is(Blocks.BEDROCK))
                            continue;

                        level.setBlock(mutablePos, fluidState, 3);
                        placed = true;
                    }
                } else if (distSq <= (effectiveRadius + 3.0) * (effectiveRadius + 3.0)) {
                    // Rim decoration (Shoreline)
                    mutablePos.setWithOffset(origin, x, 0, z);
                    int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, mutablePos.getX(),
                            mutablePos.getZ()); // Gets height of solid or liquid (usually top of water or ground)

                    mutablePos.set(origin.getX() + x, surfaceY, origin.getZ() + z);

                    // Descend to find ACTUAL solid ground (ignore plants, leaves, or liquids if
                    // they are not solid)
                    // This fixes the "Stacking" issue where the feature would place a block on top
                    // of a plant
                    // detected by the heightmap.
                    int rimDescent = 0;
                    BlockState currentState = level.getBlockState(mutablePos);
                    while (mutablePos.getY() > level.getMinBuildHeight() &&
                            (!currentState.isSolid() || currentState.is(BlockTags.LOGS)
                                    || currentState.is(BlockTags.LEAVES))
                            && rimDescent < 50) {
                        mutablePos.move(Direction.DOWN);
                        currentState = level.getBlockState(mutablePos);
                        rimDescent++;
                    }

                    BlockState existing = level.getBlockState(mutablePos);

                    // Only decorate if we found solid ground (not bedrock)
                    if (existing.isSolid() && !existing.is(Blocks.BEDROCK) && !level.isOutsideBuildHeight(mutablePos)) {
                        // Place Shoreline Block (Replacing the dirt/stone below the plant)
                        level.setBlock(mutablePos, ModBlocks.SCARLET_GRASS_BLOCK.get().defaultBlockState(), 3);

                        // Place Shoreline Plant (High chance) above the new solid block
                        if (random.nextFloat() < 0.8f) {
                            BlockPos abovePos = mutablePos.above();
                            // Overwrite whatever was above (e.g. the plant) with our new plant
                            if (!level.getBlockState(abovePos).isSolid()) {
                                level.setBlock(abovePos, ModBlocks.SCARLET_GRASS.get().defaultBlockState(), 3);
                            }
                        }
                    }

                    int groundY = mutablePos.getY();

                    // Clear air just in case on the rim too (trees hanging over)
                    // Start clearing from 2 blocks above ground (leaving space for our new plant)
                    for (int y = groundY + 2; y <= groundY + 6; y++) {
                        mutablePos.set(origin.getX() + x, y, origin.getZ() + z);
                        BlockState rimAirState = level.getBlockState(mutablePos);
                        // Clear non-solid, non-air blocks (vegetation) above our new plant
                        if (!rimAirState.isSolid() && !rimAirState.isAir()) {
                            level.setBlock(mutablePos, airState, 3);
                        }
                    }
                }
            }
        }

        return placed;
    }
}
