package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Authentic Coniferous Alpine Pine Tree Generator.
 * Creates majestic tiered conifers with layered needle boughs, tapered conical crowns,
 * weathered trunk curves, and exposed mountain anchoring roots.
 */
public class WindsweptPineTreeFeature extends Feature<NoneFeatureConfiguration> {

    public WindsweptPineTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // 1. Ground Validation
        BlockPos belowPos = origin.below();
        BlockState belowState = level.getBlockState(belowPos);
        boolean validGround = belowState.is(ModBlocks.HIGHLANDS_GRASS_BLOCK.get()) ||
                belowState.is(ModBlocks.ELDERWOODS_GRASS_BLOCK.get()) ||
                belowState.is(Blocks.GRASS_BLOCK) ||
                belowState.is(Blocks.DIRT) ||
                belowState.is(Blocks.PODZOL) ||
                belowState.is(Blocks.COARSE_DIRT) ||
                belowState.is(Blocks.STONE);

        if (!validGround) {
            return false;
        }

        if (!level.isEmptyBlock(origin) && !level.getBlockState(origin).canBeReplaced()) {
            return false;
        }

        // Proximity check: prevent tree clumping so players can freely walk between trunks
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                if (cx == 0 && cz == 0) continue;
                BlockPos checkPos = origin.offset(cx, 1, cz);
                BlockState cs = level.getBlockState(checkPos);
                if (cs.is(ModBlocks.ELDER_PINE_LOG.get()) || cs.is(ModBlocks.ELDER_PINE_WOOD.get())) {
                    return false;
                }
            }
        }

        BlockState logState = ModBlocks.ELDER_PINE_LOG.get().defaultBlockState();
        BlockState woodState = ModBlocks.ELDER_PINE_WOOD.get().defaultBlockState();
        BlockState leavesState = ModBlocks.ELDER_PINE_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 1);

        // 2. Tree Dimensions (12 to 19 blocks tall)
        int height = 12 + random.nextInt(8);
        Direction windLean = Direction.Plane.HORIZONTAL.getRandomDirection(random);

        // 3. Basal Flare Roots (anchor into mountain slopes, all-bark so roots never show raw cut rings)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (random.nextFloat() < 0.75f) {
                BlockPos root = origin.relative(dir);
                if (level.isEmptyBlock(root) || level.getBlockState(root).canBeReplaced()) {
                    setBlock(level, root, woodState.setValue(RotatedPillarBlock.AXIS, dir.getAxis()));
                }
                BlockPos subRoot = root.below();
                if (level.isEmptyBlock(subRoot) || level.getBlockState(subRoot).canBeReplaced()) {
                    setBlock(level, subRoot, woodState.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                }
            }
        }

        // 4. Central Conifer Trunk with Subtle Organic Alpine Curve
        List<BlockPos> trunkLogs = new ArrayList<>();
        BlockPos.MutableBlockPos current = origin.mutable();

        int curX = 0;
        int curZ = 0;

        for (int y = 0; y < height; y++) {
            current.set(origin.getX() + curX, origin.getY() + y, origin.getZ() + curZ);

            if (level.isEmptyBlock(current) || level.getBlockState(current).canBeReplaced()) {
                setBlock(level, current, logState);
                trunkLogs.add(current.immutable());
            }

            // Subtle organic lean at 1/3 and 2/3 height (keeping contiguous trunk)
            if ((y == height / 3 || y == (2 * height) / 3) && random.nextFloat() < 0.65f) {
                curX += windLean.getStepX();
                curZ += windLean.getStepZ();
                // Place connector wood (all-bark) so raw cut rings never face horizontally outwards
                BlockPos conn = new BlockPos(origin.getX() + curX, origin.getY() + y, origin.getZ() + curZ);
                if (level.isEmptyBlock(conn) || level.getBlockState(conn).canBeReplaced()) {
                    setBlock(level, conn, woodState.setValue(RotatedPillarBlock.AXIS, windLean.getAxis()));
                    trunkLogs.add(conn);
                }
            }
        }

        BlockPos apexPos = current.immutable();

        // 5. Weathered Lower Trunk Dead Stubs (alpine character, all-bark wood so no naked cut rings show)
        for (int y = 2; y <= 4; y++) {
            if (random.nextFloat() < 0.40f && y < trunkLogs.size()) {
                BlockPos trunkAtY = trunkLogs.get(y);
                Direction stubDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos stubPos = trunkAtY.relative(stubDir);
                if (level.isEmptyBlock(stubPos)) {
                    setBlock(level, stubPos, woodState.setValue(RotatedPillarBlock.AXIS, stubDir.getAxis()));
                }
            }
        }

        // 6. Tiered Coniferous Needle Whorls (Layered Pine Boughs)
        // Whorls start 4 blocks up and occur every 2 to 3 vertical blocks
        int whorlStart = 4;
        for (int y = whorlStart; y < height - 1; y += 2) {
            // Find trunk position at this height
            BlockPos trunkAtTier = origin.offset(curX, y, curZ);
            for (BlockPos tp : trunkLogs) {
                if (tp.getY() == origin.getY() + y) {
                    trunkAtTier = tp;
                    break;
                }
            }

            // Conical radius calculation: widest at bottom (3-4), tapering to 1 at top
            float heightProgress = (float) (y - whorlStart) / (float) (height - whorlStart);
            int maxRadius;
            if (heightProgress < 0.25f) {
                maxRadius = 3 + (random.nextBoolean() ? 1 : 0);
            } else if (heightProgress < 0.55f) {
                maxRadius = 2 + (random.nextBoolean() ? 1 : 0);
            } else if (heightProgress < 0.85f) {
                maxRadius = 2;
            } else {
                maxRadius = 1;
            }

            // A. Horizontal branch logs extending outward in 4 cardinal directions
            // Using all-bark Elder Pine Wood so branch endpoints never show raw cut rings
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                int branchReach = Math.max(1, maxRadius - 1);
                BlockPos.MutableBlockPos bPos = trunkAtTier.mutable();
                for (int d = 1; d <= branchReach; d++) {
                    bPos.move(dir);
                    if (level.isEmptyBlock(bPos) || level.getBlockState(bPos).canBeReplaced()) {
                        setBlock(level, bPos, woodState.setValue(RotatedPillarBlock.AXIS, dir.getAxis()));
                    }
                }
            }

            // B. Coniferous Needle Cluster (Dense layer at tier height, drooping layer below, smaller layer above)
            placeConiferTier(level, trunkAtTier, maxRadius, leavesState, random);
        }

        // 7. Coniferous Apex Crown (Tapered Sharp Peak)
        // Layer around apex log
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 1) {
                    BlockPos p = apexPos.offset(dx, 0, dz);
                    if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) {
                        setLeaf(level, p, leavesState);
                    }
                }
            }
        }
        // Sharp conifer needle tip: 1 block directly above apex log
        BlockPos tip1 = apexPos.above();
        if (level.isEmptyBlock(tip1) || level.getBlockState(tip1).canBeReplaced()) {
            setLeaf(level, tip1, leavesState);
        }
        if (random.nextBoolean()) {
            BlockPos tip2 = tip1.above();
            if (level.isEmptyBlock(tip2) || level.getBlockState(tip2).canBeReplaced()) {
                setLeaf(level, tip2, leavesState);
            }
        }

        return true;
    }

    private void placeConiferTier(WorldGenLevel level, BlockPos center, int radius, BlockState leaves, RandomSource random) {
        // Main whorl layer at center Y: rounded conifer cross
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distSq = x * x + z * z;
                // Rounded corner clipping
                if (distSq <= radius * radius + 1) {
                    if (Math.abs(x) == radius && Math.abs(z) == radius && random.nextFloat() < 0.65f) {
                        continue;
                    }
                    BlockPos p = center.offset(x, 0, z);
                    if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) {
                        setLeaf(level, p, leaves);
                    }
                }
            }
        }

        // Drooping lower skirt at Y - 1 under outer boughs
        if (radius >= 2) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int taxicab = Math.abs(x) + Math.abs(z);
                    // Only place at the perimeter bough tips
                    if (taxicab == radius || taxicab == radius + 1) {
                        if (random.nextFloat() < 0.55f) {
                            BlockPos p = center.offset(x, -1, z);
                            if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) {
                                setLeaf(level, p, leaves);
                            }
                        }
                    }
                }
            }
        }

        // Upper conical collar at Y + 1 (smaller radius)
        int upperR = Math.max(1, radius - 1);
        for (int x = -upperR; x <= upperR; x++) {
            for (int z = -upperR; z <= upperR; z++) {
                if (Math.abs(x) + Math.abs(z) <= upperR + 1) {
                    BlockPos p = center.offset(x, 1, z);
                    if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) {
                        setLeaf(level, p, leaves);
                    }
                }
            }
        }
    }

    private void setLeaf(WorldGenLevel level, BlockPos pos, BlockState leafState) {
        level.setBlock(pos, leafState, 3);
    }
}
