package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Places dripstone clusters in caves — stalactites from ceilings and
 * stalagmites from floors.
 * Supports both vanilla dripstone and scarlet dripstone variants via
 * configuration.
 *
 * Adapted from vanilla DripstoneClusterFeature logic.
 */
public class ScarletDripstoneClusterFeature extends Feature<ScarletDripstoneClusterConfig> {

    public ScarletDripstoneClusterFeature(Codec<ScarletDripstoneClusterConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ScarletDripstoneClusterConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        ScarletDripstoneClusterConfig config = context.config();

        // Determine which blocks to use
        Block baseBlock = config.useScarletBlocks()
                ? ModBlocks.SCARLET_DRIPSTONE_BLOCK.get()
                : Blocks.DRIPSTONE_BLOCK;
        Block pointedBlock = config.useScarletBlocks()
                ? ModBlocks.POINTED_SCARLET_DRIPSTONE.get()
                : Blocks.POINTED_DRIPSTONE;

        int radius = config.clusterRadius().sample(random);
        boolean placedAny = false;

        // Scan a horizontal area around the origin
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Elliptical check
                double distSq = (double) (dx * dx + dz * dz);
                if (distSq > (double) (radius * radius)) {
                    continue;
                }

                BlockPos columnPos = origin.offset(dx, 0, dz);

                // Try to find floor and ceiling
                BlockPos floorPos = findFloor(level, columnPos, config.floorToCeilingSearchRange());
                BlockPos ceilingPos = findCeiling(level, columnPos, config.floorToCeilingSearchRange());

                // 1. Attempt to place Patch (Base Blocks)
                // We try to place patch blocks more frequently than spikes to create a "patch"
                // effect.
                boolean hasFloorPatch = false;
                boolean hasCeilingPatch = false;

                if (floorPos != null) {
                    // Try to place base block on the floor (below the air)
                    // Vein goes DOWN into the floor
                    hasFloorPatch = placePatchBlock(level, random, floorPos.below(), Direction.DOWN, baseBlock);
                }
                if (ceilingPos != null) {
                    // Try to place base block on the ceiling (above the air)
                    // Vein goes UP into the ceiling
                    hasCeilingPatch = placePatchBlock(level, random, ceilingPos.above(), Direction.UP, baseBlock);
                }

                // 2. Decide if we should place a spike here
                // Skip based on distance (outer ring is sparser)
                double normalizedDist = Math.sqrt(distSq) / radius;
                if (random.nextFloat() > (1.0f - (float) normalizedDist * 0.5f)) {
                    continue;
                }

                // Random density check for spikes
                if (random.nextFloat() < 0.5f) {
                    continue;
                }

                // Place stalagmites (from floor up)
                if (floorPos != null && hasFloorPatch) {
                    int height = 1 + random.nextInt(config.maxStalagmiteHeight());
                    placedAny |= placeSpike(level, floorPos, height, Direction.UP, pointedBlock);
                }

                // Place stalactites (from ceiling down)
                if (ceilingPos != null && hasCeilingPatch) {
                    int height = 1 + random.nextInt(config.maxStalactiteHeight());
                    placedAny |= placeSpike(level, ceilingPos, height, Direction.DOWN, pointedBlock);
                }
            }
        }

        return placedAny;
    }

    private boolean isDripstone(BlockState state) {
        return state.getBlock() instanceof PointedDripstoneBlock
                || state.is(ModBlocks.SCARLET_DRIPSTONE_BLOCK.get())
                || state.is(ModBlocks.POINTED_SCARLET_DRIPSTONE.get())
                || state.is(Blocks.DRIPSTONE_BLOCK);
    }

    /**
     * Find the floor position by searching downward from pos.
     */
    private BlockPos findFloor(WorldGenLevel level, BlockPos pos, int searchRange) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < searchRange; i++) {
            BlockState state = level.getBlockState(mutable);
            BlockState below = level.getBlockState(mutable.below());
            if (state.isAir() && below.isSolid() && !isDripstone(below)) {
                return mutable.immutable();
            }
            mutable.move(Direction.DOWN);
        }
        return null;
    }

    /**
     * Find the ceiling position by searching upward from pos.
     */
    private BlockPos findCeiling(WorldGenLevel level, BlockPos pos, int searchRange) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < searchRange; i++) {
            BlockState state = level.getBlockState(mutable);
            BlockState above = level.getBlockState(mutable.above());
            if (state.isAir() && above.isSolid() && !isDripstone(above)) {
                return mutable.immutable();
            }
            mutable.move(Direction.UP);
        }
        return null;
    }

    private boolean isGeodeBlock(BlockState state) {
        return state.is(Blocks.AMETHYST_BLOCK)
                || state.is(Blocks.BUDDING_AMETHYST)
                || state.is(Blocks.AMETHYST_CLUSTER)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(ModBlocks.MANA_CRYSTAL_BLOCK.get())
                || state.is(ModBlocks.MANA_CRYSTAL_CLUSTER.get());
    }

    private boolean isNearGeode(WorldGenLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        // Check a 2-block radius
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    mutable.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (isGeodeBlock(level.getBlockState(mutable))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean placePatchBlock(WorldGenLevel level, RandomSource random, BlockPos pos, Direction direction,
            Block baseBlock) {
        // Height check: Ensure we are well below the surface (dirt layer)
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
                pos.getX(), pos.getZ());
        if (pos.getY() > surfaceY - 8) {
            return false;
        }

        // Random vein length: 2 to 5 blocks extending into the surface
        int veinLength = 2 + random.nextInt(4);
        boolean placedAny = false;

        for (int i = 0; i < veinLength; i++) {
            BlockPos currentPos = pos.relative(direction, i); // direction is usually DOWN (floor) or UP (ceiling) so we
                                                              // go DEEPER

            // Geode proximity check for every block in the vein
            if (isNearGeode(level, currentPos)) {
                // If we hit geode proximity, stop the vein here
                break;
            }

            BlockState state = level.getBlockState(currentPos);

            // If it's already a dripstone block, count it as a success (we can grow from
            // it)
            if (isDripstone(state)) {
                placedAny = true;
                continue;
            }

            // Only replace valid blocks (e.g. stone), never air
            if (state.is(net.minecraft.tags.BlockTags.DRIPSTONE_REPLACEABLE)) {
                level.setBlock(currentPos, baseBlock.defaultBlockState(), 2);
                placedAny = true;
            } else {
                // Hit something we can't replace (e.g. bedrock, air, ore), stop the vein
                break;
            }
        }

        return placedAny;
    }

    private boolean placeSpike(WorldGenLevel level, BlockPos startPos, int height, Direction growthDir,
            Block pointedBlock) {

        // 1. Calculate actual placeable height by checking for obstructions
        int actualHeight = 0;
        for (int k = 0; k < height; k++) {
            BlockPos checkPos = startPos.relative(growthDir, k);
            BlockState checkState = level.getBlockState(checkPos);
            // Stop if we hit something solid (that isn't liquid)
            if (!checkState.isAir() && checkState.getFluidState().isEmpty()) {
                break;
            }
            actualHeight++;
        }

        if (actualHeight == 0) {
            return false;
        }

        boolean placedAny = false;

        // 2. Place the spike using the adjusted height
        for (int i = 0; i < actualHeight; i++) {
            BlockPos placePos = startPos.relative(growthDir, i);
            BlockState state = level.getBlockState(placePos);

            // Determine thickness based on the *actual* height, ensuring a tip is always
            // formed
            DripstoneThickness thickness;
            if (actualHeight == 1) {
                thickness = DripstoneThickness.TIP;
            } else if (i == 0) {
                thickness = DripstoneThickness.BASE;
            } else if (i == actualHeight - 1) {
                thickness = DripstoneThickness.TIP;
            } else if (i == actualHeight - 2) {
                thickness = DripstoneThickness.FRUSTUM;
            } else {
                thickness = DripstoneThickness.MIDDLE;
            }

            BlockState dripstoneState = pointedBlock.defaultBlockState()
                    .setValue(PointedDripstoneBlock.TIP_DIRECTION, growthDir)
                    .setValue(PointedDripstoneBlock.THICKNESS, thickness);

            // If water, set waterlogged
            if (!state.getFluidState().isEmpty()) {
                dripstoneState = dripstoneState.setValue(PointedDripstoneBlock.WATERLOGGED, true);
            }

            level.setBlock(placePos, dripstoneState, 2);
            placedAny = true;
        }
        return placedAny;
    }
}
