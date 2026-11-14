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

    private static final net.minecraft.resources.ResourceLocation ELYSIAN_RL =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("oririmod", "elysian_abyss");

    @Override
    public boolean place(FeaturePlaceContext<ScarletDripstoneClusterConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        ScarletDripstoneClusterConfig config = context.config();

        // Hard exclusion: never place in Elysian Abyss.
        // BiomeFilter only checks the origin cell; the feature's ceiling search can
        // reach the abyss ceiling even when the origin is in scarlet_caves.
        if (level.getBiome(origin).is(ELYSIAN_RL)) return false;
        // Also block if the ceiling we'd attach to is inside the abyss Y-range
        // (rough check: abyss ceiling is typically Y -70 to -20)
        if (origin.getY() > -80 && origin.getY() < -15) return false;

        // Determine which blocks to use
        Block baseBlock = config.useScarletBlocks()
                ? ModBlocks.SCARLET_DRIPSTONE_BLOCK.get()
                : Blocks.DRIPSTONE_BLOCK;
        Block pointedBlock = config.useScarletBlocks()
                ? ModBlocks.POINTED_SCARLET_DRIPSTONE.get()
                : Blocks.POINTED_DRIPSTONE;

        int radius = config.clusterRadius().sample(random);
        int radiusSq = radius * radius;
        boolean placedAny = false;
        BlockPos.MutableBlockPos columnPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos patchPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos geodeScratch = new BlockPos.MutableBlockPos();

        // Scan a horizontal area around the origin
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Elliptical check
                int distSq = dx * dx + dz * dz;
                if (distSq > radiusSq) {
                    continue;
                }

                columnPos.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);

                // Try to find floor and ceiling
                BlockPos floorPos = findFloor(level, columnPos, config.floorToCeilingSearchRange(), config);
                BlockPos ceilingPos = findCeiling(level, columnPos, config.floorToCeilingSearchRange(), config);

                // 1. Attempt to place Patch (Base Blocks)
                boolean hasFloorPatch = false;
                boolean hasCeilingPatch = false;

                if (floorPos != null) {
                    patchPos.set(floorPos.getX(), floorPos.getY() - 1, floorPos.getZ());
                    hasFloorPatch = placePatchBlock(level, random, patchPos, Direction.DOWN, baseBlock, geodeScratch);
                }
                if (ceilingPos != null) {
                    patchPos.set(ceilingPos.getX(), ceilingPos.getY() + 1, ceilingPos.getZ());
                    hasCeilingPatch = placePatchBlock(level, random, patchPos, Direction.UP, baseBlock, geodeScratch);
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
    private BlockPos findFloor(WorldGenLevel level, BlockPos pos, int searchRange, ScarletDripstoneClusterConfig config) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        BlockState state = level.getBlockState(mutable);
        int minY = level.getMinBuildHeight();
        for (int i = 0; i < searchRange && mutable.getY() > minY; i++) {
            mutable.move(Direction.DOWN);
            BlockState below = level.getBlockState(mutable);
            if (state.isAir() && below.isSolid() && !isDripstone(below) && isValidAttachmentBlock(below, config)) {
                return mutable.above();
            }
            state = below;
        }
        return null;
    }

    /**
     * Find the ceiling position by searching upward from pos.
     */
    private BlockPos findCeiling(WorldGenLevel level, BlockPos pos, int searchRange, ScarletDripstoneClusterConfig config) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        BlockState state = level.getBlockState(mutable);
        int maxY = level.getMaxBuildHeight() - 1;
        for (int i = 0; i < searchRange && mutable.getY() < maxY; i++) {
            mutable.move(Direction.UP);
            BlockState above = level.getBlockState(mutable);
            if (state.isAir() && above.isSolid() && !isDripstone(above) && isValidAttachmentBlock(above, config)) {
                return mutable.below();
            }
            state = above;
        }
        return null;
    }

    private boolean isValidAttachmentBlock(BlockState state, ScarletDripstoneClusterConfig config) {
        if (config.useScarletBlocks()) {
            return state.is(ModBlocks.SCARLET_STONE.get()) || state.is(ModBlocks.SCARLET_DEEPSLATE.get());
        } else {
            return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE);
        }
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

    private boolean isNearGeode(WorldGenLevel level, int cx, int cy, int cz, BlockPos.MutableBlockPos mutable) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    mutable.set(cx + x, cy + y, cz + z);
                    if (isGeodeBlock(level.getBlockState(mutable))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean placePatchBlock(WorldGenLevel level, RandomSource random, BlockPos.MutableBlockPos pos,
            Direction direction, Block baseBlock, BlockPos.MutableBlockPos geodeScratch) {
        // Height check: Ensure we are well below the surface (dirt layer)
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
                pos.getX(), pos.getZ());
        if (pos.getY() > surfaceY - 8) {
            return false;
        }

        // Random vein length: 2 to 5 blocks extending into the surface
        int veinLength = 2 + random.nextInt(4);
        boolean placedAny = false;
        int sx = pos.getX();
        int sy = pos.getY();
        int sz = pos.getZ();
        int dy = direction.getStepY();
        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < veinLength; i++) {
            int cy = sy + dy * i;
            currentPos.set(sx, cy, sz);
            BlockState state = level.getBlockState(currentPos);

            boolean dripstone = isDripstone(state);
            boolean replaceable = !dripstone && state.is(net.minecraft.tags.BlockTags.DRIPSTONE_REPLACEABLE);
            if (!dripstone && !replaceable) {
                break;
            }

            // Geode proximity check only when block is actually a candidate
            if (isNearGeode(level, sx, cy, sz, geodeScratch)) {
                break;
            }

            if (dripstone) {
                placedAny = true;
            } else {
                level.setBlock(currentPos, baseBlock.defaultBlockState(), 2);
                placedAny = true;
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
